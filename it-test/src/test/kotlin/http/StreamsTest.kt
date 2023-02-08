package http

import arrow.core.getOrHandle
import com.noumenadigital.platform.engine.client.EngineClientApi
import com.noumenadigital.platform.engine.values.ClientEnumValue
import com.noumenadigital.platform.engine.values.ClientListValue
import com.noumenadigital.platform.engine.values.ClientStructValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured
import io.restassured.http.Header
import org.openapitools.client.apis.IOUApi
import org.openapitools.client.infrastructure.ApiClient
import seed.config.Configuration
import seed.keycloak.KeycloakForwardProvider
import java.time.Duration

class StreamsTest : FunSpec({
    val iouApi = IOUApi(basePath)
    val config = Configuration(
        keycloakRealm = System.getenv("KEYCLOAK_REALM") ?: "seed",
        keycloakClientId = System.getenv("KEYCLOAK_CLIENT_ID") ?: "seed",
    )
    val engineClient = EngineClientApi(config.engineURL)

    test("IouComplete notification") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 10.0
        val iou = iouApi.createIouGen(amount, testPayee1)

        // initialise SSE listener
        RestAssured.given()
            .header(Header("Accept", "text/event-stream"))
            .auth().oauth2(loginIssuer().accessToken)
            .get("/iou/sse")

        // trigger IouComplete notification
        val response = iouApi.payGen(iou.iou.id, 10.0)
        (response as Map<*, *>)["amount"] shouldBe 0.0

        // We should get a notification of this format:
        //
        // ClientStructValue(
        //    prototypeId=/seed-1.0.0?/seed/Notification,
        //    value={
        //        type=ClientEnumValue(prototypeId=/seed-1.0.0?/seed/EventType, variant=IouComplete),
        //        amount=ClientNumberValue(value=0),
        //        remaining=ClientNumberValue(value=0)
        //    }
        // )

        val eventTypes = retry(10, Duration.ofSeconds(2)) {
            val iouState = engineClient.getProtocolStateById(
                iou.iou.id,
                KeycloakForwardProvider("Bearer ${ApiClient.accessToken}"),
                false
            ).getOrHandle { throw it }

            val events = iouState.fields.getValue("events") as ClientListValue<*>
            val eventTypes = events.value.map {
                val eventStruct = it as ClientStructValue
                val type = eventStruct.value.getValue("type") as ClientEnumValue
                type.variant
            }

            if (eventTypes.isEmpty()) {
                throw AssertionError("eventTypes was empty")
            }

            eventTypes
        }

        eventTypes shouldContain "IouComplete"
    }
})
