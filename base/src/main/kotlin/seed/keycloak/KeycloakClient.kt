package seed.keycloak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.noumenadigital.platform.engine.values.ClientPartyValue
import io.prometheus.client.Summary
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import seed.config.IConfiguration
import seed.config.SnakeCaseJsonConfiguration.auto
import seed.filter.ErrorCode
import seed.filter.logger
import seed.metrics.record
import java.io.IOException
import java.net.URL

private const val CONTENT_TYPE = "Content-Type"

@JsonIgnoreProperties(ignoreUnknown = true)
data class KeycloakToken(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfo(
    val preferredUsername: String,
    val party: List<String>,
)

private val keycloakTokenLens = Body.auto<KeycloakToken>().toLens()
private val keycloakUserInfoLens = Body.auto<UserInfo>().toLens()

class KeycloakUnauthorizedException(val code: ErrorCode) : Exception(code.name)
class KeycloakServerException(val code: ErrorCode) : Exception(code.name)

private fun Request.optionalHeader(name: String, value: String?): Request {
    if (value == null) {
        return this
    }
    return this.header(name, value)
}

interface PartyProvider {
    fun party(response: Response): ClientPartyValue
}

class SeedPartyProvider : PartyProvider {
    override fun party(response: Response): ClientPartyValue {
        val userInfo = keycloakUserInfoLens.extract(response)

        return ClientPartyValue(
            entity = mapOf(
                "party" to userInfo.party.toSet(),
                "preferred_username" to setOf(userInfo.preferredUsername)
            ),
            access = mapOf()
        )
    }
}

interface KeycloakClient {
    fun ready(): Boolean
    fun login(username: String, password: String): KeycloakToken
    fun refresh(refreshToken: String): KeycloakToken
    fun logout(bearerToken: String, refreshToken: String)
    fun authorize(bearerToken: String)
    fun party(bearerToken: String): ClientPartyValue
}

class KeycloakClientImpl(
    config: IConfiguration,
    val client: HttpHandler = ApacheClient(),
    private val partyProvider: PartyProvider = SeedPartyProvider(),
) : KeycloakClient {
    private val base = config.keycloakURL
    private val host = config.keycloakHost
    private val realm = config.keycloakRealm
    private val clientID = config.keycloakClientId

    private val endpointHealth = URL(base, "/health").toExternalForm()
    private val endpointRealmConfiguration =
        URL(base, "/realms/$realm/.well-known/openid-configuration").toExternalForm()
    private val endpointToken = URL(base, "/realms/$realm/protocol/openid-connect/token").toExternalForm()
    private val endpointLogout = URL(base, "/realms/$realm/protocol/openid-connect/logout").toExternalForm()
    private val endpointUserInfo = URL(base, "/realms/$realm/protocol/openid-connect/userinfo").toExternalForm()

    override fun ready(): Boolean {
        try {
            val healthReq = Request(Method.GET, endpointHealth)
            val healthRes = client(healthReq)
            if (healthRes.status != Status.OK) {
                return false
            }
        } catch (e: IOException) {
            return false
        }

        val realmReq = Request(Method.GET, endpointRealmConfiguration)
        val realmRes = client(realmReq)
        return realmRes.status == Status.OK
    }

    override fun login(username: String, password: String): KeycloakToken {
        return keycloakTimer.labels("login").record {
            val req = Request(Method.POST, endpointToken)
                .header(CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
                .form("client_id", clientID)
                .form("grant_type", "password")
                .form("username", username)
                .form("password", password)
                .optionalHeader("Host", host)
            val res = client(req)

            when (res.status) {
                Status.OK -> keycloakTokenLens.extract(res)
                Status.UNAUTHORIZED -> throw KeycloakUnauthorizedException(ErrorCode.InvalidLogin)
                else -> {
                    logger.error("Unexpected Keycloak response: ${res.status} - ${res.bodyString()}")
                    throw KeycloakServerException(ErrorCode.InternalServerError)
                }
            }
        }
    }

    override fun refresh(refreshToken: String): KeycloakToken {
        return keycloakTimer.labels("refresh").record {
            val req = Request(Method.POST, endpointToken)
                .header(CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
                .form("client_id", clientID)
                .form("grant_type", "refresh_token")
                .form("refresh_token", refreshToken)
                .optionalHeader("Host", host)
            val res = client(req)

            when (res.status) {
                Status.OK -> keycloakTokenLens.extract(res)
                Status.BAD_REQUEST -> throw KeycloakUnauthorizedException(ErrorCode.InvalidRefreshToken)
                else -> {
                    logger.error("Unexpected Keycloak response: ${res.status} - ${res.bodyString()}")
                    throw KeycloakServerException(ErrorCode.InternalServerError)
                }
            }
        }
    }

    override fun logout(bearerToken: String, refreshToken: String) {
        keycloakTimer.labels("logout").record {
            val req = Request(Method.POST, endpointLogout)
                .header(CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
                .form("client_id", clientID)
                .form("refresh_token", refreshToken)
                .header("Authorization", "Bearer $bearerToken")
                .optionalHeader("Host", host)
            val res = client(req)

            if (res.status != Status.NO_CONTENT) {
                logger.error { "Unexpected Keycloak response: ${res.status} - ${res.bodyString()}" }
                throw KeycloakServerException(ErrorCode.InternalServerError)
            }
        }
    }

    override fun authorize(bearerToken: String) {
        keycloakTimer.labels("authorize").record {
            val res = userInfoResponse(bearerToken)
            when (res.status) {
                Status.OK -> {
                    logger.debug { "authorized: $bearerToken" }
                }

                Status.UNAUTHORIZED -> throw KeycloakUnauthorizedException(ErrorCode.InvalidBearerToken)
                else -> {
                    logger.error { "Unexpected Keycloak response: ${res.status} - ${res.bodyString()}" }
                    throw KeycloakServerException(ErrorCode.InternalServerError)
                }
            }
        }
    }

    private fun userInfoResponse(bearerToken: String): Response {
        val req = Request(Method.GET, endpointUserInfo)
            .header("Authorization", bearerToken)
        return try {
            client(req)
        } catch (e: Throwable) {
            logger.error(e) { "Unexpected exception checking access token" }
            throw KeycloakServerException(ErrorCode.InternalServerError)
        }
    }

    override fun party(bearerToken: String): ClientPartyValue {
        val response = userInfoResponse(bearerToken)
        if (response.status != Status.OK) {
            throw KeycloakServerException(ErrorCode.InvalidBearerToken)
        }
        return partyProvider.party(response)
    }

    companion object {
        private val keycloakTimer = Summary.build()
            .name("keycloak_security_functions_seconds")
            .help("record time taken for invoking keycloak security functions")
            .labelNames("function")
            .quantile(0.5, 0.01).quantile(0.9, 0.01).quantile(0.99, 0.005)
            .register()
    }
}
