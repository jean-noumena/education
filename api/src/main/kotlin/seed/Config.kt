package seed

import com.noumenadigital.platform.engine.client.AuthConfig
import com.noumenadigital.platform.engine.client.TokenAuthorizationProvider
import com.noumenadigital.platform.engine.client.UserConfig

const val ISSUER = "seeduser1"
const val PAYEE = "seeduser2"

val engineURL: String = System.getenv("ENGINE_URL") ?: "http://localhost:12000"
val keycloakURL: String = System.getenv("KEYCLOAK_URL") ?: "http://localhost:11000"

private val authConfig = AuthConfig(
    realm = "noumena",
    authUrl = keycloakURL,
    clientId = "nm-platform-service-client",
    clientSecret = "87ff12ca-cf29-4719-bda8-c92faa78e3c4"
)

fun authProvider(username: String = ISSUER) =
    TokenAuthorizationProvider(
        UserConfig(username, "welcome"),
        authConfig
    )
