package seed.keycloak

import com.noumenadigital.platform.engine.client.Authorization
import com.noumenadigital.platform.engine.client.AuthorizationProvider
import com.noumenadigital.platform.engine.values.ClientPartyValue
import org.http4k.core.Request
import seed.filter.ErrorCode
import seed.security.ForwardAuthorization

data class KeycloakForwardProvider(
    private val bearerToken: String,
) : AuthorizationProvider {
    override fun invoke(): Authorization? {
        return Authorization("Bearer", bearerToken.split(' ')[1])
    }

    fun bearerToken() = bearerToken
}

class KeycloakForwardAuthorization(val client: KeycloakClient) : ForwardAuthorization {
    override fun party(request: Request): ClientPartyValue = client.party(bearerToken(request))
    override fun forward(request: Request): AuthorizationProvider = KeycloakForwardProvider(bearerToken(request))
    override fun bearerToken(request: Request): String =
        request.header("Authorization") ?: throw KeycloakServerException(ErrorCode.InvalidBearerToken)
}
