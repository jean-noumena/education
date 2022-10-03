package iou.http

import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import seed.config.Configuration
import seed.security.defaultFilter

fun rawRoutes(config: Configuration, iou: Raw): RoutingHttpHandler =
    routes(
        "/raw/iou/{amount}/{payee}" bind Method.POST to defaultFilter(config).then(iou.create()),
        "/raw/iou/{iouId}/amountOwed" bind Method.GET to defaultFilter(config).then(iou.amountOwed()),
        "/raw/iou/{iouId}/pay/{amount}" bind Method.PATCH to defaultFilter(config).then(iou.pay()),
        "/raw/iou/{iouId}/forgive" bind Method.PUT to defaultFilter(config).then(iou.forgive()),
    )

fun genRoutes(config: Configuration, iou: Gen): RoutingHttpHandler =
    routes(
        "/gen/iou/{amount}/{payee}" bind Method.POST to defaultFilter(config).then(iou.create()),
        "/gen/iou/{iouId}/amountOwed" bind Method.GET to defaultFilter(config).then(iou.amountOwed()),
        "/gen/iou/{iouId}/pay/{amount}" bind Method.PATCH to defaultFilter(config).then(iou.pay()),
        "/gen/iou/{iouId}/forgive" bind Method.PUT to defaultFilter(config).then(iou.forgive()),
    )
