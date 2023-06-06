package seed.server

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import seed.config.IConfiguration
import seed.filter.loginRequired
import seed.security.AuthHandler

fun loginRoutes(
    config: IConfiguration,
    authHandler: AuthHandler,
): RoutingHttpHandler = routes(
    "/openapi/openapi.yml" bind GET to openApi(config),
    "/swagger" bind GET to static(Classpath("/swagger-ui-3.52.3/dist")),

    "/auth/login" bind POST to authHandler.login(),
    "/auth/refresh" bind POST to authHandler.refresh(),
    "/auth/logout" bind POST to loginRequired(config).then(authHandler.logout()),
)

fun openApi(config: IConfiguration): HttpHandler {
    return { _ ->
        val resourceLoader = Classpath("/")
        val openApiUrl = resourceLoader.load("openapi.yml")
        val contents = openApiUrl?.readText()
        val responseBody = contents!!.replace(
            "- url: http://localhost:8080",
            "- url: ${config.apiServerUrl}",
        )

        Response(Status.OK)
            .header("Content-Type", ContentType.TEXT_YAML.toHeaderValue())
            .body(Body(responseBody))
    }
}
