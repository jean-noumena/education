package seed.security

import com.fasterxml.jackson.annotation.JsonProperty
import com.noumenadigital.platform.engine.values.ClientException
import mu.KotlinLogging
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.LensFailure
import seed.config.Configuration
import seed.config.JSON.auto
import java.net.URLDecoder.decode
import java.nio.charset.Charset
import java.util.UUID

internal val logger = KotlinLogging.logger {}

fun metricsFilter(config: Configuration): Filter = if (config.debug) {
    DebuggingFilters.PrintResponse().then(metrics.measure())
} else {
    metrics.measure()
}

fun defaultFilter(config: Configuration): Filter =
    metricsFilter(config).then(loginRequired(config))

fun corsFilter(config: Configuration): Filter =
    ServerFilters.Cors(
        CorsPolicy(
            originPolicy = OriginPolicy.AnyOf(config.allowedOrigins),
            methods = Method.values().asList(),
            headers = listOf(
                "DNT",
                "Keep-Alive",
                "User-Agent",
                "X-Requested-With",
                "If-Modified-Since",
                "Cache-Control",
                "Content-Type",
                "Content-Range",
                "Range",
                "Authorization"
            ),
            credentials = true,
        )
    )

fun errorFilter(debug: Boolean = false) =
    // filtering should go from least to most specific
    httpStatusFilter()
        .then(catchUnhandled())
        .then(catchLensFailure(debug))
        .then(catchAuthorizationException())
        .then(catchNoSuchItemException())
        .then(catchPlatformRuntimeException())

enum class ErrorCode {
    // general HTTP codes
    InternalServerError,
    InvalidParameter,
    MissingParameter,
    RouteNotFound,
    BadGateway,
    BadRequest,
    Conflict,

    // app-specific HTTP codes
    PropertyNotFound,
    InvalidTimestamp,
    InvalidLogin,
    InvalidRefreshToken,
    InvalidBearerToken,
    InvalidClaim,
    LoginRequired,
    ItemNotFound
}

data class Error(
    val code: ErrorCode,
    @JsonProperty("trace") val traceID: UUID,
)

private val errorLens = Body.auto<Error>().toLens()

fun errorResponse(status: Status, code: ErrorCode, traceID: UUID = UUID.randomUUID()) =
    Response(status).with(errorLens.of(Error(code, traceID)))

fun catchLensFailure(debug: Boolean = false) =
    Filter { next ->
        {
            try {
                next(it)
            } catch (t: LensFailure) {
                val traceID = UUID.randomUUID()
                if (debug) {
                    logger.error(t) { "Encode/Decode failure $traceID" }
                }
                errorResponse(Status.BAD_REQUEST, ErrorCode.InvalidParameter, traceID)
            }
        }
    }

fun catchAuthorizationException() =
    Filter { next ->
        {
            try {
                next(it)
            } catch (t: ClientException.AuthorizationException) {
                errorResponse(Status.UNAUTHORIZED, ErrorCode.LoginRequired)
            }
        }
    }

fun catchNoSuchItemException() =
    Filter { next ->
        {
            try {
                next(it)
            } catch (t: ClientException.NoSuchItemException) {
                errorResponse(Status.NOT_FOUND, ErrorCode.ItemNotFound)
            }
        }
    }

fun catchPlatformRuntimeException() =
    Filter { next ->
        {
            try {
                next(it)
            } catch (t: ClientException.PlatformRuntimeException) {
                if (t.origin.code == 37) {
                    errorResponse(Status.FORBIDDEN, ErrorCode.InvalidClaim)
                } else {
                    throw t
                }
            }
        }
    }

fun catchUnhandled() =
    Filter { next ->
        {
            try {
                next(it)
            } catch (t: Throwable) {
                val traceID = UUID.randomUUID()
                logger.error(t) {
                    "$traceID: uncaught exception when accessing ${decode(it.uri.path, Charset.defaultCharset())}"
                }
                errorResponse(Status.INTERNAL_SERVER_ERROR, ErrorCode.InternalServerError, traceID)
            }
        }
    }

fun httpStatusFilter() =
    Filter { next ->
        {
            val res = next(it)

            if (res.header("Content-Type")?.startsWith("application/json") == true) {
                // assume handled by application
                res
            } else {
                when (res.status) {
                    Status.NOT_FOUND -> errorResponse(Status.NOT_FOUND, ErrorCode.RouteNotFound)
                    else -> res
                }
            }
        }
    }
