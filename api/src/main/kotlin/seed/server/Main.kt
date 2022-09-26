package seed.server

import com.noumenadigital.platform.engine.client.EngineClientApi
import io.prometheus.client.hotspot.DefaultExports
import keycloak.forwardAuthorization
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.http4k.core.then
import org.http4k.routing.routes
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import seed.config.Configuration
import seed.http.Gen
import seed.http.Raw
import seed.security.corsFilter
import seed.security.errorFilter
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(): Unit = runBlocking {
    logger.info { "Starting WP-PLUGIN-SERVICE" }

    DefaultExports.initialize()

    val httpPort = (System.getenv("HTTP_PORT") ?: "8080").toInt()
    val adminPort = (System.getenv("HTTP_ADMIN_PORT") ?: "8000").toInt()
    val config = Configuration()

    val adminServer = admin(config).asServer(KtorCIO(adminPort))
    adminServer.start()
    val engineClient = EngineClientApi(config.engineURL)

    logger.info { "Request logging is ${if (config.debug) "on" else "off"}" }
    exitProcess(
        try {
            corsFilter(config).then(
                errorFilter().then(
                    routes(
                        loginRoutes(config),
                        rawRoutes(config, Raw(engineClient, forwardAuthorization)),
                        genRoutes(config, Gen(engineClient, forwardAuthorization))
                    )
                )
            ).asServer(KtorCIO(httpPort)).start().block()
            0
        } catch (e: Throwable) {
            logger.error(e) {}
            1
        } finally {
            adminServer.stop()
        }
    )
}
