package seed.security

import com.fasterxml.jackson.annotation.JsonProperty
import com.noumenadigital.platform.engine.client.Authorization
import com.noumenadigital.platform.engine.client.AuthorizationProvider
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.with
import seed.config.Configuration
import seed.config.JSON.auto
import seed.keycloak.KeycloakClient
import seed.keycloak.KeycloakServerException
import seed.keycloak.KeycloakToken
import seed.keycloak.KeycloakUnauthorizedException

fun loginRequired(config: Configuration, client: HttpHandler = ApacheClient()): Filter {
    val keycloakClient = KeycloakClient(config, client)
    return Filter { next ->
        { req ->
            try {
                req.header("Authorization")?.let {
                    keycloakClient.authorize(it)
                    next(req)
                } ?: errorResponse(Status.UNAUTHORIZED, ErrorCode.LoginRequired)
            } catch (e: KeycloakUnauthorizedException) {
                errorResponse(Status.UNAUTHORIZED, ErrorCode.LoginRequired)
            }
        }
    }
}

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

fun loginHandler(config: Configuration, client: HttpHandler = ApacheClient()): HttpHandler {
    val keycloakClient = KeycloakClient(config, client)

    return { req ->
        val username = req.form("username")
        val password = req.form("password")
        val grantType = req.form("grant_type")

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
}

fun refreshHandler(config: Configuration, client: HttpHandler = ApacheClient()): HttpHandler {
    val keycloakClient = KeycloakClient(config, client)

    return { req ->
        val token = req.form("refresh_token")
        val grantType = req.form("grant_type")

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
}

fun logoutHandler(config: Configuration, client: HttpHandler = ApacheClient()): HttpHandler {
    val keycloakClient = KeycloakClient(config, client)

    return { req ->
        val refreshToken = req.form("refresh_token") ?: ""
        val bearerToken = (req.header("Authorization") ?: "").removePrefix("Bearer ")

        try {
            keycloakClient.logout(bearerToken, refreshToken)
            Response(Status.OK)
        } catch (e: KeycloakServerException) {
            errorResponse(Status.INTERNAL_SERVER_ERROR, e.code)
        }
    }
}

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
