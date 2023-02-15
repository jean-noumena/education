package seed.security

import com.noumenadigital.platform.engine.values.ClientException
import com.noumenadigital.platform.engine.values.ClientIdType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import seed.config.Configuration
import seed.config.IConfiguration
import seed.filter.Error
import seed.filter.ErrorCode
import seed.filter.corsFilter
import seed.filter.errorFilter
import seed.filter.errorResponse
import kotlin.test.assertEquals

internal class CORSTest {

    @Test
    fun `test CORS allowed`() {
        val baseHandler: HttpHandler = { _ -> Response(OK) }
        val config: IConfiguration =
            Configuration(
                allowedOrigins = listOf("localhost:4040"),
                keycloakRealm = "seed",
                keycloakClientId = "seed",
                apiServerUrl = "localhost:8080"
            )
        val handler = corsFilter(config).then(baseHandler)

        fun assertResponse(origin: String, want: String) {
            val preflight = Request(Method.OPTIONS, "/")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
            val res = handler(preflight)
            assertEquals(
                want,
                res.header("Access-Control-Allow-Origin"),
                "invalid CORS response headers: ${res.headers}"
            )
        }

        assertResponse(origin = "localhost:4040", want = "localhost:4040")
        assertResponse(origin = "localhost:5050", want = "localhost:8080")
    }
}

internal class ErrorTest {

    val app = errorFilter().then(
        routes(
            "/hello" bind Method.GET to { _ ->
                Response(OK)
                    .header("Content-Type", "application/custom")
                    .body("OK")
            },
            "/404-explicit" bind Method.GET to {
                errorResponse(Status.NOT_FOUND, ErrorCode.PropertyNotFound)
            },
            "/400-explicit" bind Method.GET to {
                errorResponse(Status.BAD_REQUEST, ErrorCode.InvalidTimestamp)
            },
            "/bind-explicit-int/{id:\\d+}" bind Method.GET to {
                Response(OK).body("OK")
            },
            "/bind-implicit-int/{id}" bind Method.GET to {
                Response(OK).body(Path.int().of("id")(it).toString())
            },
            "/500" bind Method.GET to {
                throw Exception("oops")
            },
            "/NoSuchItemException" bind Method.GET to {
                throw ClientException.NoSuchItemException(idType = ClientIdType.PrototypeId, id = "Id")
            }
        )
    )

    private fun assertError(res: Response, status: Int, code: ErrorCode) {
        assertEquals(status, res.status.code)
        assertEquals("application/json; charset=utf-8", res.header("Content-Type"))

        val error = seed.config.JSON.asA(res.body.stream, Error::class)
        assertEquals(code, error.code)
    }

    @Test
    fun `return 200 on correctly routed URL`() {
        val req = Request(Method.GET, "/hello")
        val res = app(req)
        assertEquals(res.status.code, 200)
        assertEquals(res.header("Content-Type"), "application/custom")
        assertEquals(res.bodyString(), "OK")
    }

    @Test
    fun `return 404 on non-routed URLs`() {
        val req = Request(Method.GET, "/404")
        val res = app(req)
        assertError(res, 404, ErrorCode.RouteNotFound)
    }

    @Test
    fun `return 404 on explicitly 404-returning URLs`() {
        val req = Request(Method.GET, "/404-explicit")
        val res = app(req)
        assertError(res, 404, ErrorCode.PropertyNotFound)
    }

    @Test
    fun `return 400 on explicitly 400-returning URLs - Invalid Timestamp`() {
        val req = Request(Method.GET, "/400-explicit")
        val res = app(req)
        assertError(res, 400, ErrorCode.InvalidTimestamp)
    }

    @Test
    fun `return 400 on regex-based invalid parameters`() {
        val req = Request(Method.GET, "/bind-explicit-int/non-int")
        val res = app(req)
        assertError(res, 404, ErrorCode.RouteNotFound)
    }

    @Test
    fun `return 400 on parsing-based invalid parameters`() {
        val req = Request(Method.GET, "/bind-implicit-int/non-int")
        val res = app(req)
        assertError(res, 400, ErrorCode.InvalidParameter)
    }

    @Test
    fun `return 500 on unknown errors`() {
        val req = Request(Method.GET, "/500")
        val res = app(req)
        assertError(res, 500, ErrorCode.InternalServerError)
    }

    @Test
    fun `return 404 on NoSuchItemException errors`() {
        val req = Request(Method.GET, "/NoSuchItemException")
        val res = app(req)
        assertError(res, 404, ErrorCode.ItemNotFound)
    }
}
