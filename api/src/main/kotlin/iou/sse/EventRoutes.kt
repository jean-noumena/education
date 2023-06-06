package iou.sse

import com.noumenadigital.platform.client.engine.ApplicationClient
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.bind
import org.http4k.routing.sse
import seed.security.ForwardAuthorization

fun sseRoutes(
    engineClient: ApplicationClient,
    forwardAuth: ForwardAuthorization,
): RoutingSseHandler =
    sse(
        "/iou/sse" bind iouSseEventConsumers(engineClient, forwardAuth),
    )
