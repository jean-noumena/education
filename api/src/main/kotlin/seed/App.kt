package seed

import com.noumenadigital.platform.engine.client.EngineClientApi
import com.noumenadigital.platform.engine.client.EngineClientWriter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val engineClient: EngineClientWriter = EngineClientApi(engineURL)
val rawIou: Iou = Raw(engineClient)
val genIou: Iou = Gen(engineClient)

val app: HttpHandler = routes(
    "/raw/iou" bind routes(
        "/{amount}" bind Method.POST to rawIou.create(),
        "/amountOwed/{protocolId}" bind Method.GET to rawIou.amountOwed(),
        "/pay/{protocolId}" bind Method.POST to rawIou.pay(),
        "/forgive/{protocolId}" bind Method.POST to rawIou.forgive()
    ),
    "/gen/iou" bind routes(
        "/{amount}" bind Method.POST to genIou.create(),
        "/amountOwed/{protocolId}" bind Method.GET to genIou.amountOwed(),
        "/pay/{protocolId}" bind Method.POST to genIou.pay(),
        "/forgive/{protocolId}" bind Method.POST to genIou.forgive()
    )
)

fun main() {
    val printingApp: HttpHandler = DebuggingFilters.PrintRequest().then(app)

    val server = printingApp.asServer(SunHttp(9000)).start()

    println("Server started on " + server.port())
}
