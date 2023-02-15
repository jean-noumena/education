package seed.config

import kotlinx.coroutines.delay
import mu.KotlinLogging
import seed.filter.AccessLogVerbosity
import java.net.URL
import java.time.Duration

private val logger = KotlinLogging.logger { }

interface IPostMarkEmailConfiguration : IConfiguration {
    // Postmark email service
    val postmarkServerToken: String
    val postmarkSenderEmail: String
    val postmarkReceiverEmail: String
}

data class PostMarkEmailConfiguration(
    // Postmark email service
    override val postmarkServerToken: String = System.getenv("POSTMARK_SERVER_TOKEN") ?: "POSTMARK_API_TEST",
    override val postmarkSenderEmail: String = System.getenv("POSTMARK_SENDER_EMAIL") ?: "",
    override val postmarkReceiverEmail: String = System.getenv("POSTMARK_RECEIVER_EMAIL") ?: postmarkSenderEmail,

    private val baseConfig: IConfiguration,
) : IPostMarkEmailConfiguration, IConfiguration by baseConfig

interface IJavaxEmailConfiguration : IConfiguration {
    // javax.mail service
    val javaxEmailFrom: String
    val javaxEmailPassword: String
    val javaxSmtpHost: String
    val javaxSmtpPort: Int
    val javaxRequireTls: Boolean
}

data class JavaxEmailConfiguration(
    // javax.mail service
    override val javaxEmailFrom: String = System.getenv("JAVAX_EMAIL_FROM") ?: "do_not_reply@noumenadigital.com",
    override val javaxEmailPassword: String = System.getenv("JAVAX_EMAIL_PASSWORD")
        ?: "unused", // Mailhog can still work with full security, it just ignores the values. Value still can't be null.
    override val javaxSmtpHost: String = System.getenv("JAVAX_SMTP_HOST") ?: "mailhog",
    override val javaxSmtpPort: Int = System.getenv("JAVAX_SMTP_PORT")?.toInt() ?: 1025,
    override val javaxRequireTls: Boolean = System.getenv("JAVAX_REQUIRE_TLS")?.toBoolean() ?: true,

    private val baseConfig: IConfiguration,
) : IJavaxEmailConfiguration, IConfiguration by baseConfig

interface IConfiguration {
    val keycloakRealm: String
    val keycloakClientId: String
    val allowedOrigins: List<String>
    val keycloakURL: URL
    val engineURL: String
    val readModelURL: String
    val accessLogVerbosity: AccessLogVerbosity
    val apiServerUrl: String

    val keycloakHost: String?
}

data class Configuration(
    override val keycloakRealm: String,
    override val keycloakClientId: String,
    override val allowedOrigins: List<String> = (System.getenv("CORS_ALLOWED_ORIGINS") ?: "").split(","),
    override val keycloakURL: URL = URL(System.getenv("KEYCLOAK_URL") ?: "http://localhost:11000"),
    override val engineURL: String = System.getenv("ENGINE_URL") ?: "http://localhost:12000",
    override val readModelURL: String = System.getenv("READ_MODEL_URL") ?: "http://localhost:15000",
    override val accessLogVerbosity: AccessLogVerbosity =
        AccessLogVerbosity.valueOf(System.getenv("ACCESS_LOG_VERBOSITY") ?: "NONE"),
    override val apiServerUrl: String = System.getenv("API_SERVER_URL") ?: "http://localhost:8080",

    // special case for development from outside docker
    override val keycloakHost: String? = if (keycloakURL.host == "localhost") "keycloak:${keycloakURL.port}" else null,
) : IConfiguration

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
