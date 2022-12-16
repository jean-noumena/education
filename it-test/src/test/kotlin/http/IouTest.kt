package http

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.openapitools.client.apis.IOUApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.models.IouIou
import java.util.UUID

class IouTest : FunSpec({
    val iouApi = IOUApi(basePath)

    test("Create IOU") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 10.0
        val want = IouIou(id = UUID.randomUUID(), payee = testPayee2, issuer = testIssuer, amount = amount)
        val response = iouApi.createIouGen(amount, testPayee2)
        response.iou shouldBe want.copy(id = response.iou.id)
    }

    test("Get outstanding amount") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        val response = iouApi.getAmountOwedGen(iou.iou.id)
        (response as Map<*, *>)["amount"] shouldBe 5.0
    }

    test("Pay IOU") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        val response = iouApi.payGen(iou.iou.id, amount)
        (response as Map<*, *>)["amount"] shouldBe 0.0
    }

    test("forgive IOU") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        ApiClient.accessToken = loginPayee2().accessToken
        iouApi.forgiveGen(iou.iou.id)
    }

    test("Different payee can't check amount") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        ApiClient.accessToken = loginPayee1().accessToken
        val e = shouldThrow<ClientException> {
            iouApi.getAmountOwedGen(iou.iou.id)
        }
        e.statusCode shouldBe 403
    }

    test("Payee can't pay") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        ApiClient.accessToken = loginPayee2().accessToken
        val e = shouldThrow<ClientException> {
            iouApi.payGen(iou.iou.id, 5.0)
        }
        e.statusCode shouldBe 403
    }

    test("Issuer can't forgive") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        val e = shouldThrow<ClientException> {
            iouApi.forgiveGen(iou.iou.id)
        }
        e.statusCode shouldBe 403
    }

    test("Different payee can't forgive") {
        ApiClient.accessToken = loginIssuer().accessToken
        val amount = 5.0
        val iou = iouApi.createIouGen(amount, testPayee2)
        ApiClient.accessToken = loginPayee1().accessToken
        val e = shouldThrow<ClientException> {
            iouApi.forgiveGen(iou.iou.id)
        }
        e.statusCode shouldBe 403
    }
})
