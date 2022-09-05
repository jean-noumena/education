package seed.server

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import seed.config.engineURL
import seed.config.keycloakURL
import java.io.StringWriter

fun admin(): HttpHandler {
    return routes(
        "/health" bind Method.GET to health(),
        "/metrics" bind Method.GET to metrics(),
    )
}

fun health(): HttpHandler {
    return { _ ->
        when {
            ! engineClient.ready() -> Response(Status.INTERNAL_SERVER_ERROR).body("cannot reach engine on $engineURL")
            ! keycloakReady() -> Response(Status.INTERNAL_SERVER_ERROR).body("cannot reach keycloak on $keycloakURL")
            else -> Response(Status.OK).body("OK")
        }
    }
}

private fun keycloakReady(): Boolean {
    try {
        val healthReq = Request(Method.GET, "$keycloakURL/health")
        val response = ApacheClient().invoke(healthReq)
        return response.status == Status.OK
    } catch (e: Exception) {
        logger.error(e) { "while checking keycloak health " }
    }
    return false
}

fun metrics(): HttpHandler {
    return {
        val s = StringWriter()
        TextFormat.write004(s, CollectorRegistry.defaultRegistry.metricFamilySamples())
        Response(Status.OK).body(s.buffer.toString().byteInputStream())
    }
}
