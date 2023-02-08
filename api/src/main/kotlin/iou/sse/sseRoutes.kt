package iou.sse

import com.noumenadigital.platform.engine.client.EngineClientApi
import com.noumenadigital.platform.read.streams.client.SseReaderClient
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.bind
import org.http4k.routing.sse
import seed.security.ForwardAuthorization

fun sseRoutes(
    sseClient: SseReaderClient,
    engineClient: EngineClientApi,
    forwardAuth: ForwardAuthorization
): RoutingSseHandler =
    sse(
        "/iou/sse" bind iouSseEventConsumers(sseClient, engineClient, forwardAuth)
    )
