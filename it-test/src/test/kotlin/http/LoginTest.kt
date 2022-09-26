package http

import io.kotest.core.spec.style.FunSpec
import mu.KotlinLogging
import org.openapitools.client.apis.AuthenticationApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.AccessToken
import java.lang.System.getenv
import java.time.Duration

val basePath = getenv("BASE_PATH") ?: "http://localhost:8080"
val authenticationApi = AuthenticationApi(basePath)
val testPayee1 = getenv("SEED_TEST_PAYEE_1") ?: "payee1"
val testPayee1Password = getenv("SEED_TEST_PAYEE_PASSWORD_1") ?: "welcome1"
val testPayee2 = getenv("SEED_TEST_PAYEE_2") ?: "payee2"
val testPayee2Password = getenv("SEED_TEST_PAYEE_PASSWORD_2") ?: "welcome2"
val testIssuer = getenv("SEED_TEST_ISSUER") ?: "issuer1"
val testIssuerPassword = getenv("SEED_TEST_ISSUER_PASSWORD") ?: "welcome3"
private val logger = KotlinLogging.logger { }

fun <T> retry(retries: Int, retryPeriod: Duration, f: () -> T): T {
    for (i in 1..retries) {
        runCatching {
            return f()
        }.onFailure {
            if (i == retries) {
                logger.error(it) {}
                throw it
            }
            Thread.sleep(retryPeriod.toMillis())
            logger.warn { "Retrying $i of $retries after error: ${it.message}" }
        }
    }
    throw IllegalStateException("Unreachable")
}

fun loginPayee1(): AccessToken = retry(10, Duration.ofSeconds(2)) {
    try {
        authenticationApi.login(grantType = "password", username = testPayee1, password = testPayee1Password)
    } catch (e: Throwable) {
        logger.error(e) { "Login failed" }
        throw e
    }
}

fun loginPayee2(): AccessToken = retry(10, Duration.ofSeconds(2)) {
    try {
        authenticationApi.login(grantType = "password", username = testPayee2, password = testPayee2Password)
    } catch (e: Throwable) {
        logger.error(e) { "Login failed" }
        throw e
    }
}

fun loginIssuer(): AccessToken = retry(10, Duration.ofSeconds(2)) {
    try {
        authenticationApi.login(grantType = "password", username = testIssuer, password = testIssuerPassword)
    } catch (e: Throwable) {
        logger.error(e) { "Login failed" }
        throw e
    }
}

class LoginTest : FunSpec({

    test("Login using test payee") {
        ApiClient.Companion.accessToken = loginPayee2().accessToken
    }

    test("Login using test issuer") {
        ApiClient.Companion.accessToken = loginIssuer().accessToken
    }
})
