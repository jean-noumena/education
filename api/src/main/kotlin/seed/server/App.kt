package seed.server

import com.noumenadigital.platform.engine.client.EngineClientApi
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes
import seed.app.Gen
import seed.app.Iou
import seed.app.Raw
import seed.config.engineURL

val engineClient: EngineClientApi = EngineClientApi(engineURL)
val rawIou: Iou = Raw(engineClient)
val genIou: Iou = Gen(engineClient)

fun app(): HttpHandler {
    return routes(
        "/raw/iou" bind routes(
            "/{amount}" bind Method.POST to rawIou.create(),
            "/amountOwed/{protocolId}" bind Method.GET to rawIou.amountOwed(),
            "/pay/{protocolId}" bind Method.POST to rawIou.pay(),
            "/forgive/{protocolId}" bind Method.POST to rawIou.forgive(),
        ),
        "/gen/iou" bind routes(
            "/{amount}" bind Method.POST to genIou.create(),
            "/amountOwed/{protocolId}" bind Method.GET to genIou.amountOwed(),
            "/pay/{protocolId}" bind Method.POST to genIou.pay(),
            "/forgive/{protocolId}" bind Method.POST to genIou.forgive(),
        )
    )
}
