package seed.testing

import com.atlassian.oai.validator.OpenApiInteractionValidator
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.queries
import java.util.Optional
import kotlin.test.junit5.JUnit5Asserter.fail

val baseTestValidator = OpenApiInteractionValidator.createFor("src/test/resources/test-openapi.yml").build()!!
val baseAuthJsonTestValidator = OpenApiInteractionValidator.createFor("src/test/resources/test-auth-json-openapi.yml").build()!!

class OpenAPI(val validator: OpenApiInteractionValidator) {

    fun validate(handler: HttpHandler): HttpHandler {
        return { req ->
            val res = handler(req)
            assertValid(req, res)
            res
        }
    }

    private fun assertValid(req: Request, res: Response) {
        val report = validator.validate(RequestWrapper(req), ResponseWrapper(res))
        for (err in report.messages) {
            fail(err.message)
        }
    }
}

internal class RequestWrapper(val req: Request) : com.atlassian.oai.validator.model.Request {
    override fun getPath(): String = req.uri.path

    override fun getMethod(): com.atlassian.oai.validator.model.Request.Method {
        return when (req.method) {
            Method.GET -> com.atlassian.oai.validator.model.Request.Method.GET
            Method.POST -> com.atlassian.oai.validator.model.Request.Method.POST
            Method.PUT -> com.atlassian.oai.validator.model.Request.Method.PUT
            Method.DELETE -> com.atlassian.oai.validator.model.Request.Method.DELETE
            Method.OPTIONS -> com.atlassian.oai.validator.model.Request.Method.OPTIONS
            Method.TRACE -> com.atlassian.oai.validator.model.Request.Method.TRACE
            Method.PATCH -> com.atlassian.oai.validator.model.Request.Method.PATCH
            Method.HEAD -> com.atlassian.oai.validator.model.Request.Method.HEAD
            else -> throw IllegalArgumentException("unsupported method: ${req.method}")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun getBody(): Optional<String> {
        val bodyString = req.bodyString()
        if (bodyString.isEmpty()) {
            return Optional.empty()
        }
        return Optional.of(bodyString)
    }

    override fun getQueryParameters(): MutableCollection<String> =
        req.uri.queries().map { it.first }.toMutableList()

    override fun getQueryParameterValues(name: String): MutableCollection<String> =
        req.queries(name).map { it ?: "" }.toMutableList()

    override fun getHeaders(): MutableMap<String, MutableCollection<String>> {
        val res = HashMap<String, MutableCollection<String>>()
        for ((header, _) in req.headers) {
            res[header] = getHeaderValues(header)
        }
        return res
    }

    override fun getHeaderValues(name: String): MutableCollection<String> =
        req.headerValues(name).map { it ?: "" }.toMutableList()
}

internal class ResponseWrapper(val res: Response) : com.atlassian.oai.validator.model.Response {
    override fun getStatus(): Int = res.status.code

    @Deprecated("Deprecated in Java")
    override fun getBody(): Optional<String> {
        val bodyString = res.bodyString()
        if (bodyString.isEmpty()) {
            return Optional.empty()
        }
        return Optional.of(bodyString)
    }

    override fun getHeaderValues(name: String): MutableCollection<String> =
        res.headerValues(name).map { it ?: "" }.toMutableList()
}
