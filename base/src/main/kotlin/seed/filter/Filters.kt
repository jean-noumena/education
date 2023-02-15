package seed.filter

import com.fasterxml.jackson.annotation.JsonProperty
import com.noumenadigital.platform.engine.values.ClientException
import mu.KotlinLogging
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AllowAllOriginPolicy
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import seed.config.IConfiguration
import seed.config.JSON.auto
import seed.keycloak.KeycloakClient
import seed.keycloak.KeycloakClientImpl
import seed.keycloak.KeycloakUnauthorizedException
import java.net.URLDecoder.decode
import java.nio.charset.Charset
import java.util.UUID

internal val logger = KotlinLogging.logger {}

fun loginRequired(config: IConfiguration, client: HttpHandler = ApacheClient()): Filter {
    val keycloakClient: KeycloakClient = KeycloakClientImpl(config, client)
    return Filter { next ->
        { req ->
            try {
                req.header("Authorization")?.let {
                    keycloakClient.authorize(it)
                    next(req)
                } ?: errorResponse(Status.UNAUTHORIZED, ErrorCode.LoginRequired)
            } catch (e: KeycloakUnauthorizedException) {
                logger.error { "KeycloakUnauthorizedException: $e, request: $req" }
                errorResponse(Status.UNAUTHORIZED, ErrorCode.LoginRequired)
            }
        }
    }
}

fun corsFilter(config: IConfiguration): Filter =
    /**
     * We're not using org.http4k.filter.ServerFilters.Cors because it returns an "access-control-allow-origin" response
     * header of null if the origin request header is missing or originPolicy returns false, which is not the correct
     * behaviour. This would allow access from non-whitelisted origins and missing origins.
     * We should return the server hostname in these cases.
     */
    Cors(
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
        ),
        config.apiServerUrl
    )

object Cors {
    private fun List<String>.joined() = joinToString(", ")

    operator fun invoke(policy: CorsPolicy, fallbackAllowedOrigin: String) = Filter { next ->
        {
            val response = if (it.method == Method.OPTIONS) Response(Status.OK) else next(it)

            val origin = it.header("Origin")
            val allowedOrigin = when {
                policy.originPolicy is AllowAllOriginPolicy -> "*"
                origin != null && policy.originPolicy(origin) -> origin
                else -> fallbackAllowedOrigin
            }

            response.with(
                Header.required("access-control-allow-origin") of allowedOrigin,
                Header.required("access-control-allow-headers") of policy.headers.joined(),
                Header.required("access-control-allow-methods") of policy.methods.map { method -> method.name }
                    .joined(),
                { res -> if (policy.credentials) res.header("access-control-allow-credentials", "true") else res }
            )
        }
    }
}

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
            } catch (e: LensFailure) {
                val traceID = UUID.randomUUID()
                if (debug) {
                    logger.warn(e) {
                        "Encode/Decode failure $traceID, failures ${e.failures}, stacktrace ${e.stackTrace}"
                    }
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
            } catch (e: ClientException.AuthorizationException) {
                logger.error { "ClientException.AuthorizationException: $e, request: $it" }
                errorResponse(Status.UNAUTHORIZED, ErrorCode.LoginRequired)
            }
        }
    }

fun catchNoSuchItemException() =
    Filter { next ->
        {
            try {
                next(it)
            } catch (e: ClientException.NoSuchItemException) {
                logger.error { "ClientException.NoSuchItemException: $e, request: $it" }
                errorResponse(Status.NOT_FOUND, ErrorCode.ItemNotFound)
            }
        }
    }

fun catchPlatformRuntimeException() =
    Filter { next ->
        {
            try {
                next(it)
            } catch (e: ClientException.PlatformRuntimeException) {
                logger.error { "ClientException.PlatformRuntimeException: $e, request: $it" }
                when (e.origin.code) {
                    37 -> errorResponse(Status.FORBIDDEN, ErrorCode.InvalidClaim)
                    else -> throw e
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
                    Status.NOT_FOUND -> {
                        logger.error { "404 Not Found: request: $it, response: $res" }
                        errorResponse(Status.NOT_FOUND, ErrorCode.RouteNotFound)
                    }

                    else -> res
                }
            }
        }
    }
