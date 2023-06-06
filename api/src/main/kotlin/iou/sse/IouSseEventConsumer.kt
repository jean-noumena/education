package iou.sse

import com.noumenadigital.npl.api.generated.seed.EventFacade
import com.noumenadigital.npl.api.generated.seed.EventTypeEnum
import com.noumenadigital.npl.api.generated.seed.IouCompleteFacade
import com.noumenadigital.npl.api.generated.seed.IouProxy
import com.noumenadigital.npl.api.generated.seed.PaymentFacade
import com.noumenadigital.platform.client.engine.ApplicationClient
import com.noumenadigital.platform.engine.values.ClientNumberValue
import com.noumenadigital.platform.engine.values.ClientProtocolReferenceValue
import mu.KotlinLogging
import org.http4k.sse.SseConsumer
import seed.security.ForwardAuthorization
import seed.sse.EngineSseConsumer
import seed.sse.Event
import java.math.BigDecimal
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}

fun iouSseEventConsumers(
    engineClient: ApplicationClient,
    forwardAuth: ForwardAuthorization,
): SseConsumer =
    EngineSseConsumer(
        engineClient,
        forwardAuth,
        listOf(
            IouCompleteEventConsumer(engineClient),
            PaymentEventConsumer(engineClient),
        ),
    )

class IouCompleteEventConsumer(val client: ApplicationClient) : Consumer<Event> {

    private val proxy = IouProxy(client)

    override fun accept(event: Event) {
        val payload = event.flux.payload
        if (payload.name == IouCompleteFacade.typeName && payload.arguments.isNotEmpty()) {
            val iouArg = payload.arguments[0]
            val iouId = (iouArg as ClientProtocolReferenceValue).value

            logger.info { "payload=$payload, iouId=$iouId" }

            proxy.registerEvent(
                protocolId = iouId,
                event = EventFacade(EventTypeEnum.IouComplete, BigDecimal.ZERO, BigDecimal.ZERO),
                authorizationProvider = event.auth,
            )

            // further process the event here
        }
    }
}

class PaymentEventConsumer(val client: ApplicationClient) : Consumer<Event> {

    private val proxy = IouProxy(client)

    override fun accept(event: Event) {
        val payload = event.flux.payload
        if (payload.name == PaymentFacade.typeName && payload.arguments.isNotEmpty()) {
            val iouArg = payload.arguments[0]
            val amountArg = payload.arguments[1]
            val remainingArg = payload.arguments[2]

            val iouId = (iouArg as ClientProtocolReferenceValue).value
            val amount = (amountArg as ClientNumberValue).value.toInt()
            val remaining = (remainingArg as ClientNumberValue).value.toInt()

            logger.info { "payload=$payload, iouId=$iouId, amount=$amount, remaining=$remaining" }

            proxy.registerEvent(
                protocolId = iouId,
                event = EventFacade(
                    EventTypeEnum.Payment,
                    amount.toBigDecimal(),
                    remaining.toBigDecimal(),
                ),
                authorizationProvider = event.auth,
            )

            // further process the event here
        }
    }
}
