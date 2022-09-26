package keycloak

import com.noumenadigital.platform.engine.client.Authorization
import com.noumenadigital.platform.engine.client.AuthorizationProvider
import com.noumenadigital.platform.engine.values.ClientProtocolState
import org.http4k.core.Request
import seed.config.Configuration
import seed.keycloak.KeycloakServerException
import seed.keycloak.KeycloakUnauthorizedException
import seed.security.ErrorCode

data class KeycloakForwardProvider(
    private val bearerToken: String,
) : AuthorizationProvider {
    override fun invoke(): Authorization? {
        return Authorization("Bearer", bearerToken.split(' ')[1])
    }

    fun bearerToken() = bearerToken

    internal fun <T> authorizeState(state: ClientProtocolState, f: () -> T): T {
        val authorized = party(bearerToken())
        for (p in state.parties) {
            if (authorized.entity.keys.containsAll(p.value.entity.keys) &&
                authorized.entity.values.containsAll(p.value.entity.values)
            ) {
                return f()
            }
        }
        throw KeycloakUnauthorizedException(ErrorCode.LoginRequired)
    }
}

typealias ForwardAuthorization = (Request) -> KeycloakForwardProvider

val forwardAuthorization = fun(request: Request): KeycloakForwardProvider =
    Configuration().run {
        KeycloakForwardProvider(
            request.header("Authorization") ?: throw KeycloakServerException(ErrorCode.InvalidBearerToken)
        )
    }
