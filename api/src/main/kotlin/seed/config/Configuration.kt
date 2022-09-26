package seed.config

import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.net.URL
import java.time.Duration

private val logger = KotlinLogging.logger { }

data class Configuration(
    val allowedOrigins: List<String> = (System.getenv("CORS_ALLOWED_ORIGINS") ?: "").split(","),
    val keycloakURL: URL = URL(System.getenv("KEYCLOAK_URL") ?: "http://localhost:11000"),
    val engineURL: String = System.getenv("ENGINE_URL") ?: "http://localhost:12000",
    val systemUser: String = System.getenv("SEED_SYSTEM_USER") ?: "system",
    val engineMaxRetries: Int = (System.getenv("SEED_MAX_ENGINE_RETRIES") ?: "20").toInt(),
    val engineRetryPeriodSeconds: Duration = Duration.ofSeconds(
        (System.getenv("SEED_ENGINE_RETRY_PERIOD_SECONDS") ?: "5").toLong()
    ),
    val readStreamsMaxReconnections: Int = (System.getenv("SEED_READ_STREAMS_MAX_RECONNECTIONS") ?: "30").toInt(),
    val readStreamsReconnectionPeriodSeconds: Duration = Duration.ofSeconds(
        (System.getenv("SEED_READ_STREAMS_RECONNECTION_PERIOD_SECONDS") ?: "10").toLong()
    ),
    val debug: Boolean = (System.getenv("DEBUG_REQUEST_RESPONSE") ?: "false").toBoolean(),

    // special case for development from outside docker
    val keycloakHost: String? = if (keycloakURL.host == "localhost") "keycloak:${keycloakURL.port}" else null
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
