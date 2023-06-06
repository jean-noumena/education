package iou.http

import org.http4k.core.Method
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun iouRoutes(iou: Iou): RoutingHttpHandler =
    routes(
        "/iou" bind routes(
            "/{amount}/{payee}" bind Method.POST to iou.create(),
            "/{iouId}/amountOwed" bind Method.GET to iou.amountOwed(),
            "/{iouId}/pay/{amount}" bind Method.PATCH to iou.pay(),
            "/{iouId}/forgive" bind Method.PUT to iou.forgive(),
        ),
    )
