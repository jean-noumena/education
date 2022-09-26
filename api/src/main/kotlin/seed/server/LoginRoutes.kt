package seed.server

import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.then
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import seed.config.Configuration
import seed.security.defaultFilter
import seed.security.loginHandler
import seed.security.logoutHandler
import seed.security.metricsFilter
import seed.security.refreshHandler

fun loginRoutes(
    config: Configuration,
): RoutingHttpHandler = routes(
    "/openapi" bind GET to static(Classpath("/"), "yml" to ContentType.TEXT_YAML),
    "/swagger" bind GET to static(Classpath("/swagger-ui-3.52.3/dist")),

    "/auth/login" bind POST to metricsFilter(config).then(
        loginHandler(config.keycloakURL, config.keycloakHost)
    ),
    "/auth/refresh" bind POST to metricsFilter(config).then(
        refreshHandler(config.keycloakURL, config.keycloakHost)
    ),
    "/auth/logout" bind POST to defaultFilter(config).then(
        logoutHandler(config.keycloakURL, config.keycloakHost)
    ),
)
