package seed.keycloak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.noumenadigital.platform.engine.values.ClientPartyValue
import io.prometheus.client.Summary
import metrics.record
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import seed.config.Configuration
import seed.config.SnakeCaseJsonConfiguration.auto
import seed.security.ErrorCode
import seed.security.logger
import java.io.IOException
import java.net.URL

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

class KeycloakClient(config: Configuration, val client: HttpHandler) {

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

    fun ready(): Boolean {
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

    fun login(username: String, password: String): KeycloakToken {
        return keycloakTimer.labels("login").record {
            val req = Request(Method.POST, endpointToken)
                .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
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

    fun refresh(refreshToken: String): KeycloakToken {
        return keycloakTimer.labels("refresh").record {
            val req = Request(Method.POST, endpointToken)
                .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
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

    fun logout(bearerToken: String, refreshToken: String) {
        keycloakTimer.labels("logout").record {
            val req = Request(Method.POST, endpointLogout)
                .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
                .form("client_id", clientID)
                .form("refresh_token", refreshToken)
                .header("Authorization", "Bearer $bearerToken")
                .optionalHeader("Host", host)
            val res = client(req)

            if (res.status != Status.OK) {
                logger.error("Unexpected Keycloak response: ${res.status} - ${res.bodyString()}")
                throw KeycloakServerException(ErrorCode.InternalServerError)
            }
        }
    }

    fun authorize(bearerToken: String) {
        keycloakTimer.labels("authorize").record {
            val res = userInfoResponse(bearerToken)
            when (res.status) {
                Status.OK -> {
                }
                Status.UNAUTHORIZED -> throw KeycloakUnauthorizedException(ErrorCode.InvalidBearerToken)
                else -> {
                    logger.error { "Unexpected Keycloak response: ${res.status} - ${res.bodyString()}" }
                    throw KeycloakServerException(ErrorCode.InternalServerError)
                }
            }
        }
    }

    fun userInfoResponse(bearerToken: String): Response {
        val req = Request(Method.GET, endpointUserInfo)
            .header("Authorization", bearerToken)
        return try {
            client(req)
        } catch (e: Throwable) {
            logger.error(e) { "Unexpected exception checking access token" }
            throw KeycloakServerException(ErrorCode.InternalServerError)
        }
    }

    fun party(userInfo: UserInfo): ClientPartyValue {
        return ClientPartyValue(
            entity = mapOf(
                "party" to userInfo.party.toSet(),
                "preferred_username" to setOf(userInfo.preferredUsername)
            ),
            access = mapOf()
        )
    }

    private fun userInfo(bearerToken: String): UserInfo {
        val response = userInfoResponse(bearerToken)
        if (response.status != Status.OK) {
            throw KeycloakServerException(ErrorCode.InvalidBearerToken)
        }
        return keycloakUserInfoLens.extract(response)
    }

    fun party(bearerToken: String): ClientPartyValue = party(userInfo(bearerToken))

    companion object {
        private val keycloakTimer = Summary.build()
            .name("keycloak_security_functions_seconds")
            .help("record time taken for invoking keycloak security functions")
            .labelNames("function")
            .quantile(0.5, 0.01).quantile(0.9, 0.01).quantile(0.99, 0.005)
            .register()
    }
}
