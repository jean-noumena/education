package seed.config

import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.net.URL
import java.time.Duration

private val logger = KotlinLogging.logger { }

data class Configuration(
    val keycloakRealm: String,
    val keycloakClientId: String,
    val allowedOrigins: List<String> = (System.getenv("CORS_ALLOWED_ORIGINS") ?: "").split(","),
    val keycloakURL: URL = URL(System.getenv("KEYCLOAK_URL") ?: "http://localhost:11000"),
    val engineURL: String = System.getenv("ENGINE_URL") ?: "http://localhost:12000",
    val debug: Boolean = (System.getenv("DEBUG_REQUEST_RESPONSE") ?: "false").toBoolean(),
    val apiServerUrl: String = System.getenv("API_SERVER_URL") ?: "http://localhost:8080",

    // special case for development from outside docker
    val keycloakHost: String? = if (keycloakURL.host == "localhost") "keycloak:${keycloakURL.port}" else null,

    // Postmark email service
    val postmarkServerToken: String = System.getenv("POSTMARK_SERVER_TOKEN") ?: "POSTMARK_API_TEST",
    val postmarkSenderEmail: String = System.getenv("POSTMARK_SENDER_EMAIL") ?: "",
    val postmarkReceiverEmail: String = System.getenv("POSTMARK_RECEIVER_EMAIL") ?: postmarkSenderEmail,

    // javax.mail service
    val javaxEmailFrom: String = System.getenv("JAVAX_EMAIL_FROM") ?: "do_not_reply@noumenadigital.com",
    val javaxEmailPassword: String = System.getenv("JAVAX_EMAIL_PASSWORD") ?: "unused", // Mailhog can still work with full security, it just ignores the values. Value still can't be null.
    val javaxSmtpHost: String = System.getenv("JAVAX_SMTP_HOST") ?: "mailhog",
    val javaxSmtpPort: Int = System.getenv("JAVAX_SMTP_PORT")?.toInt() ?: 1025,
    val javaxRequireTls: Boolean = System.getenv("JAVAX_REQUIRE_TLS")?.toBoolean() ?: true,
)

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
            logger.warn { "Retrying after $it.message" }
        }
    }
    throw IllegalStateException("Unreachable")
}

suspend fun <T> suspendRetry(retries: Int, retryPeriod: Duration, f: suspend () -> T): T {
    for (i in 1..retries) {
        runCatching {
            return f()
        }.onFailure {
            if (i == retries) {
                logger.error(it) {}
                throw it
            }
            delay(retryPeriod.toMillis())
            logger.warn { "Retrying after $it.message" }
        }
    }
    throw IllegalStateException("Unreachable")
}
