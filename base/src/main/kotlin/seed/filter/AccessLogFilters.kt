package seed.filter

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.MemoryBody
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.RequestFilters
import seed.config.IConfiguration

enum class AccessLogVerbosity {
    NONE, MIN, MAX
}

fun accessLogFilter(config: IConfiguration): Filter = when (config.accessLogVerbosity) {
    AccessLogVerbosity.NONE -> Filter.NoOp
    AccessLogVerbosity.MIN -> MinAccessLogFilters.PrintRequestAndResponse()
    AccessLogVerbosity.MAX -> MaxAccessLogFilters.PrintRequestAndResponse()
}

object MinAccessLogFilters {
    object PrintRequest {
        operator fun invoke(): Filter =
            RequestFilters.Tap { req ->
                logger.info { "REQUEST: ${req.method}: ${req.uri}" }
            }
    }

    object PrintResponse {
        operator fun invoke(): Filter =
            Filter { next ->
                {
                    try {
                        next(it).let { response ->
                            logger.info { "RESPONSE ${response.status.code} to ${it.method}: ${it.uri}" }
                            response
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "RESPONSE FAILED to ${it.method}: ${it.uri}" }
                        throw e
                    }
                }
            }
    }

    object PrintRequestAndResponse {
        operator fun invoke() = PrintRequest().then(PrintResponse())
    }
}

object MaxAccessLogFilters {
    private const val defaultDebugStream = false

    object PrintRequest {
        operator fun invoke(debugStream: Boolean = defaultDebugStream): Filter =
            RequestFilters.Tap { req ->
                logger.info {
                    listOf(
                        "REQUEST: ${req.method}: ${req.uri}",
                        req.printable(debugStream)
                    ).joinToString("\n")
                }
            }
    }

    object PrintResponse {
        operator fun invoke(debugStream: Boolean = defaultDebugStream): Filter =
            Filter { next ->
                {
                    try {
                        next(it).let { response ->
                            logger.info {
                                listOf(
                                    "RESPONSE ${response.status.code} to ${it.method}: ${it.uri}",
                                    response.printable(debugStream)
                                ).joinToString("\n")
                            }
                            response
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "RESPONSE FAILED to ${it.method}: ${it.uri}" }
                        throw e
                    }
                }
            }
    }

    private fun HttpMessage.printable(debugStream: Boolean) =
        if (debugStream || body is MemoryBody) this else body("<<stream>>")

    object PrintRequestAndResponse {
        operator fun invoke(debugStream: Boolean = defaultDebugStream) =
            PrintRequest(debugStream).then(PrintResponse(debugStream))
    }
}
