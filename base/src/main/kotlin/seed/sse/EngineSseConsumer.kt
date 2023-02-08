package seed.sse

import com.noumenadigital.platform.engine.client.AuthorizationProvider
import com.noumenadigital.platform.engine.values.ClientFluxNotification
import com.noumenadigital.platform.read.streams.client.SseReaderClient
import org.http4k.sse.Sse
import org.http4k.sse.SseConsumer
import seed.security.ForwardAuthorization
import java.util.function.Consumer

data class Event(val flux: ClientFluxNotification, val auth: AuthorizationProvider)

class EngineSseConsumer(
    private val sseClient: SseReaderClient,
    private val forwardAuth: ForwardAuthorization,
    private val consumers: List<Consumer<Event>>,
) : SseConsumer {
    override fun invoke(sse: Sse) {
        val auth = forwardAuth.forward(sse.connectRequest)

        sseClient.notifications(-1, auth, false)
            .subscribe { clientFluxNotification ->
                consumers.forEach { it.accept(Event(clientFluxNotification, auth)) }
            }
    }
}
