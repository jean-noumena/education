package http

import arrow.core.Either
import arrow.core.mapOf
import com.noumenadigital.codegen.Party
import com.noumenadigital.npl.api.generated.seed.IouFacade
import com.noumenadigital.platform.engine.client.EngineClientApi
import com.noumenadigital.platform.engine.values.ClientNumberValue
import com.noumenadigital.platform.engine.values.ClientPartyValue
import com.noumenadigital.platform.engine.values.ClientProtocolReferenceValue
import com.noumenadigital.platform.engine.values.ClientUnitValue
import com.noumenadigital.platform.engine.values.ClientValueResponse
import io.jsonwebtoken.impl.Base64Codec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import keycloak.ForwardAuthorization
import keycloak.KeycloakForwardProvider
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import seed.config.SnakeCaseJsonConfiguration.auto
import seed.http.AmountResponse
import seed.http.CreateResponse
import seed.http.Gen
import seed.http.Raw
import seed.http.partyPerson
import seed.http.userParty
import utilities.validProtocolState
import java.util.UUID.randomUUID

class HttpIouTest : FunSpec({

    test("payee party") {
        val username = "payee1"
        val want = Party(
            entity = mapOf("party" to setOf("payee"), "preferred_username" to setOf(username)),
            access = mapOf()
        )
        val got = userParty("payee", username)
        got shouldBe want
    }

    context("party person from protocol parties") {
        val payee = ClientPartyValue(
            entity = mapOf("party" to setOf("payee"), "preferred_username" to setOf("payee1")),
            access = mapOf()
        )
        val issuer = ClientPartyValue(
            entity = mapOf("party" to setOf("issuer"), "preferred_username" to setOf("issuer1")),
            access = mapOf()
        )
        val parties = mapOf("payee" to payee, "issuer" to issuer)
        mapOf("payee" to "payee1", "issuer" to "issuer1").forEach { (party, person) ->
            test(party) {
                partyPerson(parties, party) shouldBe person
            }
        }
    }

    context("npl operations") {
        val forwardAuthorizationMock = mock<KeycloakForwardProvider>()
        val forwardAuthorization: ForwardAuthorization = fun(_: Request) = forwardAuthorizationMock
        val engineClient = mock<EngineClientApi>()

        afterTest {
            // in Kotest, mocks declared outside of a single test() {} block need to be reset after each test
            reset(engineClient, forwardAuthorizationMock)
        }

        // the JWT token data was extracted from a swagger call to /auth/login

        val payeeToken = """$algo.$payeePayload.$payeeSignature"""
        val issuerToken = """$algo.$issuerPayload.$issuerSignature"""

        val payee = "payee1"
        val issuer = "issuer1"

        val payeeParty = userParty("payee", payee)
        val issuerParty = userParty("issuer", issuer)

        val protocolId = randomUUID()
        val amount = 10.0

        val issuerClientParty = ClientPartyValue(entity = issuerParty.entity, access = mapOf())
        val payeeClientParty = ClientPartyValue(entity = payeeParty.entity, access = mapOf())

        val iouState = validProtocolState(
            id = protocolId,
            prototypeId = IouFacade.prototypeId,
            currentState = IouFacade.StatesEnum.unpaid.name,
            party = mapOf("issuer" to issuerClientParty, "payee" to payeeClientParty),
            fields = mapOf("forAmount" to ClientNumberValue(amount.toBigDecimal()))
        )
        val iouFacade = IouFacade(iouState)

        context("creation of iou") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorization),
                "gen" to Gen(engineClient, forwardAuthorization),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken()).thenReturn("Bearer $issuerToken")
                    whenever(engineClient.createProtocol(eq(IouFacade.prototypeId), any(), any(), any(), any()))
                        .thenReturn(
                            Either.Right(
                                ClientValueResponse(
                                    ClientProtocolReferenceValue(
                                        protocolId,
                                        IouFacade.prototypeId,
                                    ),
                                    randomUUID(),
                                    listOf()
                                )
                            )
                        )
                    whenever(engineClient.getProtocolStateById(eq(iouFacade.id.id), any(), any()))
                        .thenReturn(Either.Right(iouState))
                    val request = Request(Method.POST, "http://somewhere/$name/iou/$payee/$amount")
                    val handler = routes(
                        "/$name/iou/{payee}/{amount}" bind Method.POST to iouNpl.create()
                    )
                    val response = handler(request)
                    response.status shouldBe Status.CREATED
                    val got = Body.auto<CreateResponse>().toLens().extract(response)
                    got.iou.id shouldBe protocolId
                    got.iou.payee shouldBe payee
                    got.iou.issuer shouldBe issuer
                    verify(forwardAuthorizationMock, times(1)).bearerToken()
                    verify(engineClient, times(1)).createProtocol(
                        eq(IouFacade.prototypeId), any(), any(), any(), any()
                    )
                    verify(engineClient, times(1)).getProtocolStateById(protocolId, forwardAuthorizationMock, true)
                    verifyNoMoreInteractions(forwardAuthorizationMock)
                    verifyNoMoreInteractions(engineClient)
                }
            }
        }

        context("fetch amount still owed") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorization),
                "gen" to Gen(engineClient, forwardAuthorization),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken()).thenReturn("Bearer $issuerToken")
                    whenever(
                        engineClient.selectAction(
                            protocolId = eq(protocolId),
                            action = anyString(),
                            arguments = any(),
                            authorizationProvider = any(),
                            caller = eq(null)
                        )
                    )
                        .thenReturn(
                            Either.Right(
                                ClientValueResponse(
                                    ClientNumberValue(amount.toBigDecimal()),
                                    randomUUID(),
                                    listOf()
                                )
                            )
                        )
                    val request = Request(Method.GET, "http://somewhere/$name/iou/$protocolId/amountOwed")
                    val handler = routes(
                        "/$name/iou/{iouId}/amountOwed" bind Method.GET to iouNpl.amountOwed()
                    )
                    val response = handler(request)
                    response.status shouldBe Status.OK
                    val got = Body.auto<AmountResponse>().toLens().extract(response)
                    got.amount shouldBe amount
                    verify(engineClient, times(1)).selectAction(
                        protocolId = eq(protocolId),
                        action = eq("getAmountOwed"),
                        arguments = eq(listOf()),
                        authorizationProvider = eq(forwardAuthorizationMock),
                        caller = eq(null)
                    )
                    verifyNoMoreInteractions(forwardAuthorizationMock)
                    verifyNoMoreInteractions(engineClient)
                }
            }
        }

        context("pay off an amount") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorization),
                "gen" to Gen(engineClient, forwardAuthorization),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken()).thenReturn("Bearer $issuerToken")
                    whenever(
                        engineClient.selectAction(
                            protocolId = eq(protocolId),
                            action = anyString(),
                            arguments = any(),
                            authorizationProvider = any(),
                            caller = eq(null)
                        )
                    )
                        .thenReturn(
                            Either.Right(
                                ClientValueResponse(
                                    ClientNumberValue(amount.toBigDecimal()),
                                    randomUUID(),
                                    listOf()
                                )
                            )
                        )
                    val request = Request(Method.PATCH, "http://somewhere/$name/iou/$protocolId/pay/$amount")
                    val handler = routes(
                        "/$name/iou/{iouId}/pay/{amount}" bind Method.PATCH to iouNpl.pay()
                    )
                    val response = handler(request)
                    response.status shouldBe Status.OK
                    val got = Body.auto<AmountResponse>().toLens().extract(response)
                    got.amount shouldBe amount
                    verify(engineClient, times(1)).selectAction(
                        protocolId = eq(protocolId),
                        action = eq("pay"),
                        arguments = eq(listOf(ClientNumberValue(amount.toBigDecimal()))),
                        authorizationProvider = eq(forwardAuthorizationMock),
                        caller = eq(null)
                    )
                    verifyNoMoreInteractions(forwardAuthorizationMock)
                    verifyNoMoreInteractions(engineClient)
                }
            }
        }

        context("forgive") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorization),
                "gen" to Gen(engineClient, forwardAuthorization),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken()).thenReturn("Bearer $payeeToken")
                    whenever(
                        engineClient.selectAction(
                            protocolId = eq(protocolId),
                            action = anyString(),
                            arguments = any(),
                            authorizationProvider = any(),
                            caller = eq(null)
                        )
                    )
                        .thenReturn(
                            Either.Right(
                                ClientValueResponse(
                                    ClientUnitValue,
                                    randomUUID(),
                                    listOf()
                                )
                            )
                        )
                    val request = Request(Method.PUT, "http://somewhere/$name/iou/$protocolId/forgive")
                    val handler = routes(
                        "/$name/iou/{iouId}/forgive" bind Method.PUT to iouNpl.forgive()
                    )
                    val response = handler(request)
                    response.status shouldBe Status.NO_CONTENT
                    verify(engineClient, times(1)).selectAction(
                        protocolId = eq(protocolId),
                        action = eq("forgive"),
                        arguments = eq(listOf()),
                        authorizationProvider = eq(forwardAuthorizationMock),
                        caller = eq(null)
                    )
                    verifyNoMoreInteractions(forwardAuthorizationMock)
                }
            }
        }
    }
})

private val algo = Base64Codec().encode(
    """
        {
            "alg": "RS256",
            "typ": "JWT",
            "kid": "vIdfLCQ5xGpkVz2TuulL57uJhGdvDLxyQsnXLV5kYkU"
        }
    """.trimIndent().replace("\n", "")
)

private val payeeSignature =
    """rVjfadFxF5c5gm3YkCK068Zob2YBXlu4DYk72XY7gPJv7ssdX883Q7KUflzVA1bnnhqtzQPARHehlyWA-pPb5QkLgC0YfRNfvMxX0dCbcd8yvGp6VE1QNPg6D1HFhINX1NDJEWbcijstIVOcxvI0tmagmvs7G1H_66tY89II0XGp3UP7dHZzTJYxileXlI_tZqtBb3y5nzcdk7CFBx_QOw3n_dt5COOZUWL9Eprblgi45smbkw-rXZmlCAIhEz_cJTDPrX0NQ8d1fn8VZYNcIp9k0B3gST9B89R2Hk6ImwNHGmf-goODWYSib2zM95tDy3ni2H7lFlI-iUSJSDZGGw"""
private val payeePayload = Base64Codec().encode(
    """
        {
          "exp": 1663842138,
          "iat": 1663841838,
          "jti": "b28083ac-84b2-4915-8cc7-e600ea88104b",
          "iss": "http://keycloak:11000/realms/seed",
          "aud": "account",
          "sub": "89ccbbba-b2ae-4c83-8a13-40f98ef232fc",
          "typ": "Bearer",
          "azp": "seed",
          "session_state": "16cbd671-ab0e-4f17-9eb4-e4e470f6e5ed",
          "acr": "1",
          "realm_access": {
            "roles": [
              "NM_USER",
              "default-roles-seed",
              "offline_access",
              "uma_authorization"
            ]
          },
          "resource_access": {
            "account": {
              "roles": [
                "manage-account",
                "manage-account-links",
                "view-profile"
              ]
            }
          },
          "scope": "email profile",
          "sid": "16cbd671-ab0e-4f17-9eb4-e4e470f6e5ed",
          "email_verified": false,
          "preferred_username": "payee2",
          "given_name": "",
          "family_name": "",
          "party": [
            "payee"
          ]
        }
    """.trimIndent().replace("\n", "")
)

private val issuerSignature =
    """FYSXmAfFTmeiBbjjycjDFMkECOMpBcMVKOOuu7618b74M0WLV8YD26XZRasiXhxWOwH7fKV0Ejv1BVBgwmSZw_KhzP16BqDr9vSISWUJr48w77Pihbu1wZpCfk-U8tjtqUoGWjdDL5OS2rDNOtKAJOCbkHd1KvijWCaJ0IpAh57jA9B8jySnJRgvxyw3aYPhtgL67x2JYzfAy3HPB30FvdTgiotWIU7BClEzdFGSmxjjp_ocrifRUBY5U37lyIKW2BJpt_8Dlpn2kA-zjRPoFo10elm15Pdq_ih3ciZSKGWqx1_Uaw8Zeao3dtQddaCohKBx2Fb61TEfYYuiuRMtPg"""
private val issuerPayload = Base64Codec().encode(
    """
        {
          "exp": 1663844981,
          "iat": 1663844681,
          "jti": "854a0f3e-08bb-4d45-80e6-fd5356aa9809",
          "iss": "http://keycloak:11000/realms/seed",
          "aud": "account",
          "sub": "8290e0a0-273e-42b8-82bd-7166a5322743",
          "typ": "Bearer",
          "azp": "seed",
          "session_state": "012aaaba-51ca-449e-a08e-a67df6e29634",
          "acr": "1",
          "realm_access": {
            "roles": [
              "NM_USER",
              "default-roles-seed",
              "offline_access",
              "uma_authorization"
            ]
          },
          "resource_access": {
            "account": {
              "roles": [
                "manage-account",
                "manage-account-links",
                "view-profile"
              ]
            }
          },
          "scope": "email profile",
          "sid": "012aaaba-51ca-449e-a08e-a67df6e29634",
          "email_verified": false,
          "preferred_username": "issuer1",
          "given_name": "",
          "family_name": "",
          "party": [
            "issuer"
          ]
        }
    """.trimIndent().replace("\n", "")
)
