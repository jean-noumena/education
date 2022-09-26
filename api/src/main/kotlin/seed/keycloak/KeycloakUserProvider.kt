package keycloak

import com.noumenadigital.platform.engine.client.Authorization
import com.noumenadigital.platform.engine.client.AuthorizationProvider
import org.http4k.client.ApacheClient
import seed.keycloak.KeycloakClient
import java.net.URL

class KeycloakUserProvider(
    private val keycloakURL: URL,
    private val keycloakHost: String?,
    private val username: String,
    private val password: String
) : AuthorizationProvider {
    override fun invoke(): Authorization? {
        val keycloakClient = KeycloakClient(keycloakURL, keycloakHost, ApacheClient())
        val kcToken = keycloakClient.login(username, password)
        return Authorization("Bearer", kcToken.accessToken, kcToken.refreshToken)
    }
}
