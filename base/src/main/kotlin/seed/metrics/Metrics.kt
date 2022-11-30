package seed.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Summary
import io.prometheus.client.exporter.common.TextFormat
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RequestWithRoute
import java.io.StringWriter
import java.time.Duration.between
import java.time.Instant

fun handler(): HttpHandler {
    return {
        val s = StringWriter()
        TextFormat.write004(s, CollectorRegistry.defaultRegistry.metricFamilySamples())
        Response(Status.OK).body(s.buffer.toString().byteInputStream())
    }
}

private val incomingTimer = Summary.build()
    .name("http_incoming_request_duration_seconds")
    .help("Duration of HTTP request in seconds")
    .labelNames("path")
    .quantile(.5, .05)
    .quantile(.9, .01)
    .quantile(.99, .001)
    .register()

fun measure(): Filter {
    return Filter { next ->
        { req: Request ->
            when (req) {
                is RequestWithRoute -> {
                    val label = req.xUriTemplate.toString()
                    incomingTimer.labels(label).startTimer().use {
                        next(req)
                    }
                }
                else -> next(req)
            }
        }
    }
}

/**
 * Does the same as Summary.Child.time, using Kotlin lambda rather than Runnable.
 */
fun <T> Summary.Child.record(f: () -> T): T {
    val start = Instant.now()
    try {
        return f()
    } finally {
        observe(between(start, Instant.now()).toNanos().toDouble() / 1e9)
    }
}
