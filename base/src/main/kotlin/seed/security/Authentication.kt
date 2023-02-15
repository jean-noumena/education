package seed.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.noumenadigital.platform.engine.client.Authorization
import com.noumenadigital.platform.engine.client.AuthorizationProvider
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.with
import seed.config.IConfiguration
import seed.config.JSON.auto
import seed.filter.errorResponse
import seed.filter.logger
import seed.keycloak.KeycloakClient
import seed.keycloak.KeycloakClientImpl
import seed.keycloak.KeycloakServerException
import seed.keycloak.KeycloakToken
import seed.keycloak.KeycloakUnauthorizedException

data class LoginResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("refresh_token") val refreshToken: String,
) {
    companion object {
        fun fromKeycloak(t: KeycloakToken): LoginResponse = LoginResponse(
            accessToken = t.accessToken,
            expiresIn = t.expiresIn,
            refreshToken = t.refreshToken,
        )
    }
}

private val loginTokenLens = Body.auto<LoginResponse>().toLens()

internal data class OAuthError(
    val error: String,
    @JsonProperty("error_description") val errorDescription: String,
)

private val oAuthErrorLens = Body.auto<OAuthError>().toLens()

private fun oAuthError(status: Status, error: String, description: String = ""): Response =
    Response(status).with(oAuthErrorLens.of(OAuthError(error, description)))

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
data class LoginRequest(
    val username: String?,
    val password: String?,
    @JsonProperty("grant_type") val grantType: String?,
)

private val loginRequestLens = Body.auto<LoginRequest>().toLens()

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
data class RefreshRequest(
    @JsonProperty("refresh_token") val refreshToken: String?,
    @JsonProperty("grant_type") val grantType: String?,
)

private val refreshRequestLens = Body.auto<RefreshRequest>().toLens()

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
data class LogoutRequest(
    @JsonProperty("refresh_token") val refreshToken: String?,
)

private val logoutRequestLens = Body.auto<LogoutRequest>().toLens()

interface AuthHandler {
    fun login(): HttpHandler
    fun refresh(): HttpHandler
    fun logout(): HttpHandler
}

abstract class KeycloakAuthHandler(
    config: IConfiguration,
    client: HttpHandler = ApacheClient(),
    private val loginRequestConverter: (Request) -> LoginRequest,
    private val refreshRequestConverter: (Request) -> RefreshRequest,
    private val logoutRequestConverter: (Request) -> LogoutRequest,
) : AuthHandler {
    private val keycloakClient: KeycloakClient = KeycloakClientImpl(config, client)

    override fun login(): HttpHandler = {
        val loginRequest = loginRequestConverter(it)

        val username = loginRequest.username
        val password = loginRequest.password
        val grantType = loginRequest.grantType

        when {
            username == null -> {
                oAuthError(Status.BAD_REQUEST, "invalid_request")
            }

            password == null -> {
                oAuthError(Status.BAD_REQUEST, "invalid_request")
            }

            grantType == null -> {
                oAuthError(Status.BAD_REQUEST, "invalid_request")
            }

            grantType != "password" -> {
                oAuthError(Status.BAD_REQUEST, "unsupported_grant_type")
            }

            else -> try {
                val kcToken = keycloakClient.login(username, password)
                val responseBody = LoginResponse.fromKeycloak(kcToken)
                Response(Status.OK).with(loginTokenLens of responseBody)
            } catch (e: KeycloakUnauthorizedException) {
                oAuthError(Status.UNAUTHORIZED, "invalid_grant", e.message ?: "")
            } catch (e: KeycloakServerException) {
                oAuthError(Status.INTERNAL_SERVER_ERROR, "server_error", e.message ?: "")
            }
        }
    }

    override fun refresh(): HttpHandler = {
        val refreshRequest = refreshRequestConverter(it)

        val token = refreshRequest.refreshToken
        val grantType = refreshRequest.grantType

        when {
            token == null -> {
                oAuthError(Status.BAD_REQUEST, "invalid_request")
            }

            grantType != "refresh_token" -> {
                oAuthError(Status.BAD_REQUEST, "invalid_request")
            }

            else -> try {
                val kcToken = keycloakClient.refresh(token)
                val responseBody = LoginResponse.fromKeycloak(kcToken)
                Response(Status.OK).with(loginTokenLens of responseBody)
            } catch (e: KeycloakUnauthorizedException) {
                errorResponse(Status.UNAUTHORIZED, e.code)
            } catch (e: KeycloakServerException) {
                errorResponse(Status.INTERNAL_SERVER_ERROR, e.code)
            }
        }
    }

    override fun logout(): HttpHandler = {
        val logoutRequest = logoutRequestConverter(it)
        val refreshToken = logoutRequest.refreshToken ?: ""

        val bearerToken = (it.header("Authorization") ?: "").removePrefix("Bearer ")

        try {
            keycloakClient.logout(bearerToken, refreshToken)
            Response(Status.OK)
        } catch (e: KeycloakServerException) {
            logger.error(e) { "logout failed" }
            errorResponse(Status.INTERNAL_SERVER_ERROR, e.code)
        }
    }
}

class JsonKeycloakAuthHandler(
    config: IConfiguration,
    client: HttpHandler = ApacheClient(),
    loginRequestConverter: (Request) -> LoginRequest = ::jsonLoginRequestConverter,
    refreshRequestConverter: (Request) -> RefreshRequest = ::jsonRefreshRequestConverter,
    logoutRequestConverter: (Request) -> LogoutRequest = ::jsonLogoutRequestConverter,
) : KeycloakAuthHandler(
    config,
    client,
    loginRequestConverter,
    refreshRequestConverter,
    logoutRequestConverter
)

class FormKeycloakAuthHandler(
    config: IConfiguration,
    client: HttpHandler = ApacheClient(),
    loginRequestConverter: (Request) -> LoginRequest = ::formLoginRequestConverter,
    refreshRequestConverter: (Request) -> RefreshRequest = ::formRefreshRequestConverter,
    logoutRequestConverter: (Request) -> LogoutRequest = ::formLogoutRequestConverter,
) : KeycloakAuthHandler(
    config,
    client,
    loginRequestConverter,
    refreshRequestConverter,
    logoutRequestConverter
)

class KeycloakAuthorizationProvider(private val req: Request) : AuthorizationProvider {
    override fun invoke(): Authorization? {
        val authHeader = req.header("Authorization") ?: return null
        val parts = authHeader.split(" ", limit = 2)
        if (parts.size != 2) {
            logger.debug { "invalid Authorization header: $authHeader" }
            return null
        }
        return Authorization(parts[0], parts[1])
    }
}

internal fun jsonLoginRequestConverter(req: Request): LoginRequest {
    val loginRequest = loginRequestLens(req)

    val username = loginRequest.username
    val password = loginRequest.password
    val grantType = loginRequest.grantType

    return LoginRequest(username, password, grantType)
}

internal fun formLoginRequestConverter(req: Request): LoginRequest {
    val username = req.form("username")
    val password = req.form("password")
    val grantType = req.form("grant_type")

    return LoginRequest(username, password, grantType)
}

internal fun jsonRefreshRequestConverter(req: Request): RefreshRequest {
    val refreshRequest = refreshRequestLens(req)

    val token = refreshRequest.refreshToken
    val grantType = refreshRequest.grantType

    return RefreshRequest(token, grantType)
}

internal fun formRefreshRequestConverter(req: Request): RefreshRequest {
    val token = req.form("refresh_token")
    val grantType = req.form("grant_type")

    return RefreshRequest(token, grantType)
}

internal fun jsonLogoutRequestConverter(req: Request): LogoutRequest {
    val logoutRequest = logoutRequestLens(req)
    val refreshToken = logoutRequest.refreshToken ?: ""

    return LogoutRequest(refreshToken)
}

internal fun formLogoutRequestConverter(req: Request): LogoutRequest {
    val refreshToken = req.form("refresh_token") ?: ""

    return LogoutRequest(refreshToken)
}
