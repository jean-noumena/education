package seed.server

import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import seed.config.adminPort
import seed.config.httpPort

val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting server on :$httpPort" }
    logger.info { "Starting admin server on :$adminPort" }

    DefaultExports.initialize()

    val adminServer = admin().asServer(SunHttp(adminPort))
    val appServer = app().asServer(SunHttp(httpPort))

    adminServer.start()
    try {
        appServer.start().block()
    } finally {
        appServer.stop()
    }
}
