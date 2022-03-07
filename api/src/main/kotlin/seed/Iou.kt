package seed

import arrow.core.getOrHandle
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.noumenadigital.codegen.Party
import com.noumenadigital.npl.api.generated.seed.IouProxy
import com.noumenadigital.platform.engine.client.EngineClientWriter
import com.noumenadigital.platform.engine.values.ClientNumberValue
import com.noumenadigital.platform.engine.values.ClientPartyValue
import com.noumenadigital.platform.engine.values.ClientProtocolReferenceValue
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.lens.uuid
import java.math.BigDecimal
import java.util.UUID

interface Iou {
    fun create(): HttpHandler
    fun amountOwed(): HttpHandler
    fun pay(): HttpHandler
    fun forgive(): HttpHandler
}

val pathAmount = Path.int().of("amount")
val pathProtocolId = Path.uuid().of("protocolId")

@JsonInclude(Include.NON_NULL)
data class Payload(
    val protocolId: UUID?,
    val value: String? = null
)

val lens = Body.auto<Payload>().toLens()

class Raw(private val client: EngineClientWriter) : Iou {

    override fun create(): HttpHandler {
        return { req ->
            val result = client.createProtocol(
                prototypeId = "/seed-1.0.0?/seed/Iou",
                parties = listOf(ClientPartyValue(ISSUER), ClientPartyValue(PAYEE)),
                arguments = listOf(ClientNumberValue(BigDecimal(pathAmount(req)))),
                authorizationProvider = authProvider()
            )

            val id = result.getOrHandle { throw it }.result as ClientProtocolReferenceValue
            val response = Payload(id.value)
            Response(Status.OK).with(lens of response)
        }
    }

    override fun amountOwed(): HttpHandler {
        return { req ->
            val iouProtocolId = pathProtocolId(req)

            val result = client.selectAction(
                protocolId = pathProtocolId(req),
                action = "getAmountOwed",
                arguments = listOf(),
                authorizationProvider = authProvider()
            )

            val amountOwed = result.getOrHandle { throw it }.result as ClientNumberValue
            val response = Payload(iouProtocolId, amountOwed.value.toPlainString())
            Response(Status.OK).with(lens of response)
        }
    }

    override fun pay(): HttpHandler {
        return { req ->
            val request = lens.extract(req)

            val iouProtocolId = pathProtocolId(req)
            val amountToPay = BigDecimal(request.value)

            client.selectAction(
                protocolId = iouProtocolId,
                action = "pay",
                arguments = listOf(ClientNumberValue(amountToPay)),
                authorizationProvider = authProvider()
            )

            val response = Payload(iouProtocolId, amountToPay.toPlainString())
            Response(Status.OK).with(lens of response)
        }
    }

    override fun forgive(): HttpHandler {
        return { req ->
            val iouProtocolId = pathProtocolId(req)

            client.selectAction(
                protocolId = iouProtocolId,
                action = "forgive",
                arguments = listOf(),
                authorizationProvider = authProvider(PAYEE)
            )

            val response = Payload(iouProtocolId, "forgiven")
            Response(Status.OK).with(lens of response)
        }
    }
}

class Gen(client: EngineClientWriter) : Iou {

    private val proxy = IouProxy(client)

    override fun create(): HttpHandler {
        return { req ->
            val result = proxy.create(
                issuer = Party(ISSUER),
                payee = Party(PAYEE),
                forAmount = BigDecimal(pathAmount(req)),
                authorizationProvider = authProvider()
            )

            val id = result.getOrHandle { throw it }.result

            val response = Payload(id.id)
            Response(Status.OK).with(lens of response)
        }
    }

    override fun amountOwed(): HttpHandler {
        return { req ->
            val iouProtocolId = pathProtocolId(req)

            val result = proxy.getAmountOwed(
                protocolId = iouProtocolId,
                authorizationProvider = authProvider()
            )

            val amountOwed = result.getOrHandle { throw it }

            val response = Payload(iouProtocolId, amountOwed.result.toPlainString())
            Response(Status.OK).with(lens of response)
        }
    }

    override fun pay(): HttpHandler {
        return { req ->
            val request = lens.extract(req)

            val iouProtocolId = pathProtocolId(req)
            val amountToPay = BigDecimal(request.value)

            proxy.pay(
                protocolId = iouProtocolId,
                amount = amountToPay,
                authorizationProvider = authProvider()
            )

            val response = Payload(iouProtocolId, amountToPay.toPlainString())
            Response(Status.OK).with(lens of response)
        }
    }

    override fun forgive(): HttpHandler {
        return { req ->
            val iouProtocolId = pathProtocolId(req)

            proxy.forgive(
                protocolId = iouProtocolId,
                authorizationProvider = authProvider(PAYEE)
            )

            val response = Payload(iouProtocolId, "forgiven")
            Response(Status.OK).with(lens of response)
        }
    }
}
