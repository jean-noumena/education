package seed.graphql

import com.noumenadigital.platform.engine.client.AuthorizationProvider
import mu.KotlinLogging
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import seed.config.SnakeCaseJsonConfiguration.auto
import java.net.URL

private val logger = KotlinLogging.logger {}

internal data class Query(val query: String)

internal val queryLens = Body.auto<Query>().toLens()

class GraphQLException(msg: String) : Exception(msg)
class GraphQLUnauthorizedException : Exception()

class GraphQLClient(baseURL: URL, val client: HttpHandler = ApacheClient()) {

    private val queryEndpoint = URL(baseURL, "/graphql").toExternalForm()

    fun handle(query: String, auth: AuthorizationProvider? = null): String {
        var req = Request(Method.POST, queryEndpoint).with(queryLens of Query(query))
        if (auth != null) {
            req = req.header("Authorization", "Bearer ${auth()?.token}")
        }
        val res = client(req)
        return when (res.status) {
            Status.OK -> res.body.toString()
            Status.UNAUTHORIZED -> throw GraphQLUnauthorizedException()
            Status.FORBIDDEN -> throw GraphQLUnauthorizedException()
            else -> {
                logger.error { "$queryEndpoint returned ${res.status} when executing\n$query" }
                throw GraphQLException(res.status.toString())
            }
        }
    }

    fun ready(): Boolean {
        val query = "{ protocolStates(first: 1) { nodes { protocolId } } }"

        return try {
            handle(query)
            true
        } catch (e: Throwable) {
            false
        }
    }
}
