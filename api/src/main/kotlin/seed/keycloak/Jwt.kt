package keycloak

import com.github.scribejava.core.java8.Base64
import com.noumenadigital.codegen.Party
import seed.config.SnakeCaseJsonConfiguration
import java.net.URI
import java.net.URL
import java.util.UUID

data class Jwt(val header: Header, val payload: Payload)
data class Header(val alg: String, val typ: String, val kid: String)
data class Payload(
    val exp: Long,
    val iat: Long,
    val jti: UUID,
    val iss: URL,
    val aud: String,
    val sub: UUID,
    val typ: String,
    val azp: String,
    val sessionState: UUID,
    val acr: String,
    val realmAccess: RealmAccess,
    val resourceAccess: ResourceAccess,
    val scope: String,
    val sid: UUID,
    val emailVerified: URI,
    val preferredUsername: String,
    val givenName: String,
    val familyName: String,
    val party: List<String>
)

data class RealmAccess(
    val roles: List<String>,
)

data class ResourceAccess(
    val account: Account,
)

data class Account(
    val roles: List<String>,
)

/**
 * Decode a Keycloak JWT to constiuent parts
 *
 * We assume that the token has been validated, by a call to keycloak.KeycloakClient.authorize
 */
fun decodeJwt(auth: String): Jwt {
    val parts = auth.split(' ')
    if (parts.size != 2) {
        throw IllegalArgumentException("Authorization header must have two parts Bearer: base64-encoded-token")
    }
    if (parts[0] != "Bearer") {
        throw IllegalArgumentException("Authorization header must have two parts Bearer: base64-encoded-token")
    }
    val decoder = Base64.getUrlDecoder()
    val chunks = parts[1].trim().split('.')
    return when (chunks.size) {
        2 -> throw IllegalArgumentException("Jwt is not signed ${parts[1]}")
        3 -> Jwt(
            SnakeCaseJsonConfiguration.asA(String(decoder.decode(chunks[0]))),
            SnakeCaseJsonConfiguration.asA(String(decoder.decode(chunks[1])))
        )
        else -> throw IllegalArgumentException("Not a valid JWT ${parts[1]}")
    }
}

fun party(bearerToken: String): Party {
    val jwt = decodeJwt(bearerToken)
    return Party(
        entity = mapOf(
            "party" to jwt.payload.party.toSet(),
            "preferred_username" to setOf(jwt.payload.preferredUsername)
        ),
        access = mapOf()
    )
}
