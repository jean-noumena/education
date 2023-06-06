package seed.sse

import com.noumenadigital.platform.client.engine.ApplicationClient
import com.noumenadigital.platform.engine.client.AuthorizationProvider
import com.noumenadigital.platform.engine.values.ClientStreamNotification
import org.http4k.sse.Sse
import org.http4k.sse.SseConsumer
import seed.security.ForwardAuthorization
import java.util.function.Consumer

data class Event(val flux: ClientStreamNotification, val auth: AuthorizationProvider)

class EngineSseConsumer(
    private val engineClient: ApplicationClient,
    private val forwardAuth: ForwardAuthorization,
    private val consumers: List<Consumer<Event>>,
) : SseConsumer {
    override fun invoke(sse: Sse) {
        val auth = forwardAuth.forward(sse.connectRequest)

        engineClient.notifications(-1, auth).forEach { n ->
            consumers.forEach { it.accept(Event(n, auth)) }
        }
    }
}
