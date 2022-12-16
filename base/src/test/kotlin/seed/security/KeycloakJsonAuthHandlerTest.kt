package seed.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import seed.config.JSON
import seed.keycloak.ACCESS_TOKEN_INITIAL
import seed.keycloak.ACCESS_TOKEN_REFRESHED
import seed.keycloak.EXPIRY
import seed.keycloak.REFRESH_TOKEN
import seed.keycloak.VALID_PASSWORD
import seed.keycloak.VALID_USERNAME
import seed.keycloak.keycloakLoginMock
import seed.testing.OpenAPI
import seed.testing.baseAuthJsonTestValidator

class KeycloakJsonAuthHandlerTest : FunSpec({

    val authHandler: AuthHandler = JsonKeycloakAuthHandler(config, keycloakLoginMock)

    context("login") {
        val bareHandler = authHandler.login()
        val handler = OpenAPI(baseAuthJsonTestValidator).validate(bareHandler)

        test("happy path") {
            // given
            val body = LoginRequest(
                username = VALID_USERNAME,
                password = VALID_PASSWORD,
                grantType = "password"
            )

            val jsonBody = JSON.mapper.writeValueAsString(body)

            val req = Request(Method.POST, "/auth/login")
                .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                .body(jsonBody)

            // when
            val got = handler(req)

            // then
            val gotBody = JSON.asA<LoginResponse>(got.body.stream)

            val wantBody = LoginResponse(
                accessToken = ACCESS_TOKEN_INITIAL,
                refreshToken = REFRESH_TOKEN,
                expiresIn = EXPIRY
            )

            got.status shouldBe Status.OK
            got.header("Content-Type") shouldBe ContentType.APPLICATION_JSON.toHeaderValue()
            gotBody shouldBe wantBody
        }

        context("username") {
            test("invalid") {
                // given
                val body = LoginRequest(
                    username = "not valid at all",
                    password = VALID_PASSWORD,
                    grantType = "password"
                )

                val jsonBody = JSON.mapper.writeValueAsString(body)

                val req = Request(Method.POST, "/auth/login")
                    .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(jsonBody)

                // when
                val got = handler(req)

                // then
                got.status shouldBe Status.UNAUTHORIZED
            }

            test("missing") {
                // given
                val body = LoginRequest(
                    username = null,
                    password = VALID_PASSWORD,
                    grantType = "password"
                )

                val jsonBody = JSON.mapper.writeValueAsString(body)

                val req = Request(Method.POST, "/auth/login")
                    .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(jsonBody)

                // when
                val got = bareHandler(req) // bypassing OpenAPI validator because request is invalid

                // then
                got.status shouldBe Status.BAD_REQUEST
            }
        }

        context("password") {
            test("invalid") {
                // given
                val body = LoginRequest(
                    username = VALID_USERNAME,
                    password = "not valid at all",
                    grantType = "password"
                )

                val jsonBody = JSON.mapper.writeValueAsString(body)

                val req = Request(Method.POST, "/auth/login")
                    .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(jsonBody)

                // when
                val got = handler(req)

                // then
                got.status shouldBe Status.UNAUTHORIZED
            }

            test("missing") {
                // given
                val body = LoginRequest(
                    username = VALID_USERNAME,
                    password = null,
                    grantType = "password"
                )

                val jsonBody = JSON.mapper.writeValueAsString(body)

                val req = Request(Method.POST, "/auth/login")
                    .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(jsonBody)

                // when
                val got = bareHandler(req) // bypassing OpenAPI validator because request is invalid

                // then
                got.status shouldBe Status.BAD_REQUEST
            }
        }

        context("grant type for oAuth") {
            test("invalid") {
                // given
                val body = LoginRequest(
                    username = VALID_USERNAME,
                    password = VALID_PASSWORD,
                    grantType = "implicit"
                )

                val jsonBody = JSON.mapper.writeValueAsString(body)

                val req = Request(Method.POST, "/auth/login")
                    .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(jsonBody)

                // when
                val got = bareHandler(req) // bypassing OpenAPI validator because request is invalid

                // then
                got.status shouldBe Status.BAD_REQUEST
            }

            test("missing") {
                // given
                val body = LoginRequest(
                    username = VALID_USERNAME,
                    password = VALID_PASSWORD,
                    grantType = null
                )

                val jsonBody = JSON.mapper.writeValueAsString(body)

                val req = Request(Method.POST, "/auth/login")
                    .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(jsonBody)

                // when
                val got = bareHandler(req) // bypassing OpenAPI validator because request is invalid

                // then
                got.status shouldBe Status.BAD_REQUEST
            }
        }
    }

    context("refresh") {
        val bareHandler = authHandler.refresh()
        val handler = OpenAPI(baseAuthJsonTestValidator).validate(bareHandler)

        test("happy path") {
            // given
            val body = RefreshRequest(
                refreshToken = REFRESH_TOKEN,
                grantType = "refresh_token"
            )

            val jsonBody = JSON.mapper.writeValueAsString(body)

            val req = Request(Method.POST, "/auth/refresh")
                .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                .body(jsonBody)

            // when
            val got = handler(req)

            // then
            val gotBody = JSON.asA<LoginResponse>(got.body.stream)

            val wantBody = LoginResponse(
                accessToken = ACCESS_TOKEN_REFRESHED,
                refreshToken = REFRESH_TOKEN,
                expiresIn = EXPIRY
            )

            got.status shouldBe Status.OK
            got.header("Content-Type") shouldBe ContentType.APPLICATION_JSON.toHeaderValue()
            gotBody shouldBe wantBody
        }
    }

    context("logout test") {
        val bareHandler = authHandler.logout()
        val handler = OpenAPI(baseAuthJsonTestValidator).validate(bareHandler)

        test("happy path") {
            // given
            val body = LogoutRequest(
                refreshToken = REFRESH_TOKEN
            )

            val jsonBody = JSON.mapper.writeValueAsString(body)

            val req = Request(Method.POST, "/auth/logout")
                .header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                .body(jsonBody)

            // when
            val got = handler(req)

            // then
            got.status shouldBe Status.OK
        }
    }
})
