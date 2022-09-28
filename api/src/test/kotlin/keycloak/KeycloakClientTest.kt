package keycloak

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import seed.keycloak.KeycloakClient
import seed.keycloak.KeycloakToken
import seed.keycloak.KeycloakUnauthorizedException
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

const val VALID_USERNAME = "username"
const val VALID_PASSWORD = "password"
const val ACCESS_TOKEN_INITIAL = "123.access.456"
const val ACCESS_TOKEN_REFRESHED = "123.access.789"
const val REFRESH_TOKEN = "123.refresh.456"
const val EXPIRY = 300

val TOKEN_INITIAL = KeycloakToken(
    accessToken = ACCESS_TOKEN_INITIAL,
    expiresIn = EXPIRY,
    refreshToken = REFRESH_TOKEN,
)
val TOKEN_REFRESHED = KeycloakToken(
    accessToken = ACCESS_TOKEN_REFRESHED,
    expiresIn = EXPIRY,
    refreshToken = REFRESH_TOKEN,
)

val keycloakLoginMock: HttpHandler = { req ->
    val grantType = req.form("grant_type")
    val username = req.form("username")
    val password = req.form("password")
    val refresh = req.form("refresh_token")

    when {
        // require correct encoding
        req.header("Content-Type") != ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue() -> {
            Response(Status.INTERNAL_SERVER_ERROR)
        }
        // require client ID
        req.form("client_id") == null -> {
            Response(Status.INTERNAL_SERVER_ERROR)
        }

        // happy path for login
        grantType == "password" && username == VALID_USERNAME && password == VALID_PASSWORD -> {
            Response(Status.OK).body(
                """
                        {
                          "access_token": "$ACCESS_TOKEN_INITIAL",
                          "expires_in": $EXPIRY,
                          "refresh_token": "$REFRESH_TOKEN",
                          "refresh_expires_in": 1800,
                          "token_type": "Bearer",
                          "not-before-policy": 0
                        }
                """.trimIndent()
            )
        }

        // invalid login
        grantType == "password" -> {
            Response(Status.UNAUTHORIZED)
        }

        // valid refresh
        grantType == "refresh_token" && refresh == REFRESH_TOKEN -> {
            Response(Status.OK).body(
                """
                        {
                          "access_token": "$ACCESS_TOKEN_REFRESHED",
                          "expires_in": $EXPIRY,
                          "refresh_token": "$REFRESH_TOKEN",
                          "refresh_expires_in": 1800,
                          "token_type": "Bearer",
                          "not-before-policy": 0
                        }
                """.trimIndent()
            )
        }

        // invalid refresh
        grantType == "refresh_token" -> {
            Response(Status.BAD_REQUEST)
        }

        else -> {
            Response(Status.INTERNAL_SERVER_ERROR)
        }
    }
}

val keycloakAuthorizeMock: HttpHandler = { req ->
    val auth = req.header("Authorization") ?: ""
    val isBearerAuth = auth.startsWith("Bearer ")

    if (isBearerAuth) {
        Response(Status.OK)
    } else {
        Response(Status.UNAUTHORIZED)
    }
}

internal class KeycloakClientTest {
    private val client = KeycloakClient(URL("http://example.com"), "example.com", keycloakLoginMock)

    @Test
    fun `correct login`() {
        val got = client.login(VALID_USERNAME, VALID_PASSWORD)
        assertEquals(TOKEN_INITIAL, got)
    }

    @Test
    fun `incorrect login`() {
        assertFailsWith(KeycloakUnauthorizedException::class) {
            client.login(
                "very wrong username",
                "very wrong password"
            )
        }
    }

    @Test
    fun `valid refresh`() {
        val got = client.refresh(REFRESH_TOKEN)
        assertEquals(TOKEN_REFRESHED, got)
    }

    @Test
    fun `invalid refresh`() {
        assertFailsWith(KeycloakUnauthorizedException::class) { client.refresh("invalid token") }
    }
}