package http

import io.kotest.core.spec.style.FunSpec
import org.openapitools.client.infrastructure.ApiClient

class LoginTest : FunSpec({

    test("Login using test payee") {
        ApiClient.accessToken = loginPayee2().accessToken
    }

    test("Login using test issuer") {
        ApiClient.accessToken = loginIssuer().accessToken
    }
})
