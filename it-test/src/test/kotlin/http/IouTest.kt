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
    context("Create IOU") {
        test(" raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 10.0
            val wanted = IouIou(id = UUID.randomUUID(), payee = testPayee2, issuer = testIssuer, amount = amount)
            val response = iouApi.createIouRaw(amount, testPayee2)
            response.iou shouldBe wanted.copy(id = response.iou.id)
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 10.0
            val wanted = IouIou(id = UUID.randomUUID(), payee = testPayee2, issuer = testIssuer, amount = amount)
            val response = iouApi.createIouGen(amount, testPayee2)
            response.iou shouldBe wanted.copy(id = response.iou.id)
        }
    }

    context("Get outstanding amount") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            val response = iouApi.getAmountOwedRaw(iou.iou.id)
            (response as Map<*, *>)["amount"] shouldBe 5.0
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            val response = iouApi.getAmountOwedGen(iou.iou.id)
            (response as Map<*, *>)["amount"] shouldBe 5.0
        }
    }

    context("Pay IOU") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            val response = iouApi.payGen(iou.iou.id, amount)
            (response as Map<*, *>)["amount"] shouldBe 0.0
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            val response = iouApi.payRaw(iou.iou.id, amount)
            (response as Map<*, *>)["amount"] shouldBe 0.0
        }
    }

    context("forgive IOU") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouRaw(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee2().accessToken
            iouApi.forgiveRaw(iou.iou.id)
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee2().accessToken
            iouApi.forgiveGen(iou.iou.id)
        }
    }

    context("Different payee can't check amount") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee1().accessToken
            val e = shouldThrow<ClientException> {
                iouApi.getAmountOwedRaw(iou.iou.id)
            }
            e.statusCode shouldBe 403
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee1().accessToken
            val e = shouldThrow<ClientException> {
                iouApi.getAmountOwedRaw(iou.iou.id)
            }
            e.statusCode shouldBe 403
        }
    }

    context("Payee can't pay") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee2().accessToken
            val e = shouldThrow<ClientException> {
                iouApi.payRaw(iou.iou.id, 5.0)
            }
            e.statusCode shouldBe 403
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee2().accessToken
            val e = shouldThrow<ClientException> {
                iouApi.payGen(iou.iou.id, 5.0)
            }
            e.statusCode shouldBe 403
        }
    }

    context("Issuer can't forgive") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            val e = shouldThrow<ClientException> {
                iouApi.forgiveRaw(iou.iou.id)
            }
            e.statusCode shouldBe 403
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            val e = shouldThrow<ClientException> {
                iouApi.forgiveGen(iou.iou.id)
            }
            e.statusCode shouldBe 403
        }
    }

    context("Different payee can't forgive") {
        test("raw") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee1().accessToken
            val e = shouldThrow<ClientException> {
                iouApi.forgiveRaw(iou.iou.id)
            }
            e.statusCode shouldBe 403
        }
        test("gen") {
            ApiClient.Companion.accessToken = loginIssuer().accessToken
            val amount = 5.0
            val iou = iouApi.createIouGen(amount, testPayee2)
            ApiClient.Companion.accessToken = loginPayee1().accessToken
            val e = shouldThrow<ClientException> {
                iouApi.forgiveGen(iou.iou.id)
            }
            e.statusCode shouldBe 403
        }
    }
})
