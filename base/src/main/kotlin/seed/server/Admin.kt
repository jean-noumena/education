package seed.server

import com.noumenadigital.platform.engine.client.EngineClientApi
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.routes
import seed.config.IConfiguration
import seed.keycloak.KeycloakClient
import seed.keycloak.KeycloakClientImpl
import seed.metrics.handler
import seed.metrics.measure

fun admin(config: IConfiguration): HttpHandler {
    return routes(
        "/health" bind Method.GET to measure().then(healthHandler(config)),
        "/metrics" bind Method.GET to measure().then(handler())
    )
}

fun healthHandler(config: IConfiguration): HttpHandler {
    val httpClient = ApacheClient()
    val keycloakClient: KeycloakClient = KeycloakClientImpl(config, httpClient)
    val engineClient = EngineClientApi(config.engineURL)

    return {
        when {
            !keycloakClient.ready() -> Response(Status.SERVICE_UNAVAILABLE).body("Waiting for Keycloak on ${config.keycloakURL}")
            !engineClient.ready() -> Response(Status.SERVICE_UNAVAILABLE).body("Waiting for engine on ${config.engineURL}")
            else -> Response(Status.OK).body("OK")
        }
    }
}
