package main

import com.noumenadigital.platform.engine.client.EngineClientApi
import io.prometheus.client.hotspot.DefaultExports
import iou.http.Gen
import iou.http.Raw
import iou.http.genRoutes
import iou.http.rawRoutes
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.http4k.client.ApacheClient
import org.http4k.core.then
import org.http4k.routing.routes
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import seed.config.Configuration
import seed.keycloak.KeycloakClient
import seed.keycloak.KeycloakForwardAuthorization
import seed.security.corsFilter
import seed.security.errorFilter
import seed.server.admin
import seed.server.loginRoutes
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(): Unit = runBlocking {
    logger.info { "Starting WP-PLUGIN-SERVICE" }

    DefaultExports.initialize()

    val httpPort = (System.getenv("HTTP_PORT") ?: "8080").toInt()
    val adminPort = (System.getenv("HTTP_ADMIN_PORT") ?: "8000").toInt()
    val config = Configuration(
        keycloakRealm = System.getenv("KEYCLOAK_REALM") ?: "seed",
        keycloakClientId = System.getenv("KEYCLOAK_CLIENT_ID") ?: "seed",
    )

    val adminServer = admin(config).asServer(KtorCIO(adminPort))
    adminServer.start()
    val engineClient = EngineClientApi(config.engineURL)
    val keycloakClient = KeycloakClient(config, ApacheClient())
    val forwardAuthorization = KeycloakForwardAuthorization(keycloakClient)

    logger.info { "Request logging is ${if (config.debug) "on" else "off"}" }
    exitProcess(
        try {
            corsFilter(config).then(
                errorFilter(config.debug).then(
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
