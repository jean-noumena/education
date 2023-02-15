package seed.security

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.then
import org.junit.jupiter.api.Test
import seed.config.Configuration
import seed.config.IConfiguration
import seed.config.JSON
import seed.filter.loginRequired
import seed.keycloak.ACCESS_TOKEN_INITIAL
import seed.keycloak.EXPIRY
import seed.keycloak.REFRESH_TOKEN
import seed.keycloak.VALID_PASSWORD
import seed.keycloak.VALID_USERNAME
import seed.keycloak.keycloakAuthorizeMock
import seed.keycloak.keycloakLoginMock
import seed.testing.OpenAPI
import seed.testing.baseTestValidator
import kotlin.test.assertEquals

val config: IConfiguration = Configuration(keycloakRealm = "seed", keycloakClientId = "seed")

internal class LoginHandlerTest {

    private val authHandler: AuthHandler = FormKeycloakAuthHandler(config, keycloakLoginMock)

    val bareHandler = authHandler.login()
    val handler = OpenAPI(baseTestValidator).validate(bareHandler)

    @Test
    fun `happy path`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("username", VALID_USERNAME)
            .form("password", VALID_PASSWORD)
            .form("grant_type", "password")
        val res = handler(req)

        assertEquals(Status.OK, res.status, "non-200 response for happy path")
        assertEquals(ContentType.APPLICATION_JSON.toHeaderValue(), res.header("Content-Type"), "wrong content type")

        val gotBody = JSON.asA<LoginResponse>(res.body.stream)
        val wantBody =
            LoginResponse(accessToken = ACCESS_TOKEN_INITIAL, refreshToken = REFRESH_TOKEN, expiresIn = EXPIRY)
        assertEquals(wantBody, gotBody, "wrong body")
    }

    @Test
    fun `invalid username`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("username", "not valid at all")
            .form("password", VALID_PASSWORD)
            .form("grant_type", "password")
        val res = handler(req)

        assertEquals(Status.UNAUTHORIZED, res.status, "expected 401 on wrong username")
    }

    @Test
    fun `missing username`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("password", VALID_PASSWORD)
            .form("grant_type", "password")
        val res = bareHandler(req)

        assertEquals(Status.BAD_REQUEST, res.status, "expected 400 on missing username")
    }

    @Test
    fun `missing password`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("username", VALID_USERNAME)
            .form("grant_type", "password")
        val res = bareHandler(req)

        assertEquals(Status.BAD_REQUEST, res.status, "expected 400 on missing password")
    }

    @Test
    fun `missing grant type for oAuth`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("username", VALID_USERNAME)
            .form("password", VALID_PASSWORD)
        val res = bareHandler(req)

        assertEquals(Status.BAD_REQUEST, res.status, "expected 400 on missing grant type")
    }

    @Test
    fun `invalid grant type for oAuth`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("username", VALID_USERNAME)
            .form("password", VALID_PASSWORD)
            .form("grant_type", "implicit")
        val res = bareHandler(req)

        assertEquals(Status.BAD_REQUEST, res.status, "expected 400 on invalid grant type")
    }

    @Test
    fun `invalid password`() {
        val req = Request(Method.POST, "/auth/login")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("username", VALID_USERNAME)
            .form("password", "still not valid")
            .form("grant_type", "password")
        val res = handler(req)

        assertEquals(Status.UNAUTHORIZED, res.status, "expected 401 on wrong password")
    }
}

internal class RefreshHandlerTest {

    private val authHandler: AuthHandler = FormKeycloakAuthHandler(config, keycloakLoginMock)
    val handler = OpenAPI(baseTestValidator).validate(authHandler.refresh())

    @Test
    fun `happy path`() {
        val req = Request(Method.POST, "/auth/refresh")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
            .form("refresh_token", REFRESH_TOKEN)
            .form("grant_type", "refresh_token")
        val res = handler(req)

        assertEquals(Status.OK, res.status, "expected 200 after token refresh")
    }
}

internal class LoginRequiredTest {
    private val underlying: HttpHandler = { Response(Status.I_M_A_TEAPOT) }
    val handler = loginRequired(config, keycloakAuthorizeMock).then(underlying)

    @Test
    fun `loginRequired requires an Authorization header`() {
        val req = Request(Method.GET, "")
        val res = handler(req)
        assertEquals(Status.UNAUTHORIZED, res.status)
    }

    @Test
    fun `loginRequired requires a Bearer token`() {
        val req = Request(Method.GET, "").header("Authorization", "BASIC-AUTH")
        val res = handler(req)
        assertEquals(Status.UNAUTHORIZED, res.status)
    }

    @Test
    fun `underlying is called when a valid-looking bearer token is present`() {
        val req = Request(Method.GET, "").header("Authorization", "Bearer eyJhb")
        val res = handler(req)
        assertEquals(Status.I_M_A_TEAPOT, res.status)
    }
}
