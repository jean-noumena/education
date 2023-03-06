package http

import mu.KotlinLogging
import org.openapitools.client.apis.AuthenticationApi
import org.openapitools.client.models.AccessToken
import org.openapitools.client.models.Login
import org.openapitools.client.models.Logout
import java.time.Duration

val basePath = System.getenv("BASE_PATH") ?: "http://localhost:8080"
val authenticationApi = AuthenticationApi(basePath)

val testPayee1 = System.getenv("SEED_TEST_PAYEE_1") ?: "payee1"
val testPayee1Password = System.getenv("SEED_TEST_USERS_PASSWORD") ?: "welcome"
val testPayee2 = System.getenv("SEED_TEST_PAYEE_2") ?: "payee2"
val testPayee2Password = System.getenv("SEED_TEST_USERS_PASSWORD") ?: "welcome"
val testIssuer = System.getenv("SEED_TEST_ISSUER") ?: "issuer1"
val testIssuerPassword = System.getenv("SEED_TEST_USERS_PASSWORD") ?: "welcome"
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

fun loginPayee1() = login(testPayee1, testPayee1Password)
fun loginPayee2() = login(testPayee2, testPayee2Password)
fun loginIssuer() = login(testIssuer, testIssuerPassword)

private fun login(username: String, password: String): AccessToken = retry(10, Duration.ofSeconds(2)) {
    try {
        val loginCargo = Login(
            grantType = Login.GrantType.password,
            username = username,
            password = password
        )
        authenticationApi.login(loginCargo)
    } catch (e: Throwable) {
        logger.error(e) { "Login failed" }
        throw e
    }
}

fun logout(refreshToken: String) = retry(10, Duration.ofSeconds(2)) {
    try {
        val logoutCargo = Logout(refreshToken = refreshToken)
        authenticationApi.logout(logoutCargo)
    } catch (e: Throwable) {
        logger.error(e) { "Logout failed" }
        throw e
    }
}
