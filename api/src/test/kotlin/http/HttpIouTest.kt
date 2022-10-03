package http

import arrow.core.Either
import com.noumenadigital.codegen.Party
import com.noumenadigital.npl.api.generated.seed.IouFacade
import com.noumenadigital.platform.engine.client.EngineClientApi
import com.noumenadigital.platform.engine.values.ClientNumberValue
import com.noumenadigital.platform.engine.values.ClientPartyValue
import com.noumenadigital.platform.engine.values.ClientProtocolReferenceValue
import com.noumenadigital.platform.engine.values.ClientUnitValue
import com.noumenadigital.platform.engine.values.ClientValueResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import iou.http.AmountResponse
import iou.http.CreateResponse
import iou.http.Gen
import iou.http.Raw
import iou.http.partyPerson
import iou.http.userParty
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
import seed.keycloak.KeycloakForwardProvider
import seed.security.ForwardAuthorization
import seed.utilities.validProtocolState
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
        val keycloakForwardProviderMock = mock<KeycloakForwardProvider>()
        val forwardAuthorizationMock = mock<ForwardAuthorization>()
        val engineClient = mock<EngineClientApi>()

        afterTest {
            // in Kotest, mocks declared outside of a single test() {} block need to be reset after each test
            reset(engineClient, keycloakForwardProviderMock)
        }

        // the JWT token data was extracted from a swagger call to /auth/login

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
            val party = ClientPartyValue(
                entity = mapOf("party" to setOf("issuer"), "preferred_username" to setOf("issuer1")),
                access = mapOf()
            )

            mapOf(
                "raw" to Raw(engineClient, forwardAuthorizationMock),
                "gen" to Gen(engineClient, forwardAuthorizationMock),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.party(any())).thenReturn(party)
                    whenever(forwardAuthorizationMock.forward(any())).thenReturn(keycloakForwardProviderMock)
                    whenever(keycloakForwardProviderMock.bearerToken()).thenReturn("anIssuer")
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
                    verify(engineClient, times(1)).createProtocol(
                        eq(IouFacade.prototypeId), any(), any(), any(), any()
                    )
                    verify(engineClient, times(1)).getProtocolStateById(protocolId, keycloakForwardProviderMock, true)
                    verifyNoMoreInteractions(keycloakForwardProviderMock)
                    verifyNoMoreInteractions(engineClient)
                }
            }
        }

        context("fetch amount still owed") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorizationMock),
                "gen" to Gen(engineClient, forwardAuthorizationMock),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken(any())).thenReturn("anIssuer")
                    whenever(forwardAuthorizationMock.forward(any())).thenReturn(keycloakForwardProviderMock)
                    whenever(keycloakForwardProviderMock.bearerToken()).thenReturn("aPayee")
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
                        authorizationProvider = eq(keycloakForwardProviderMock),
                        caller = eq(null)
                    )
                    verifyNoMoreInteractions(keycloakForwardProviderMock)
                    verifyNoMoreInteractions(engineClient)
                }
            }
        }

        context("pay off an amount") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorizationMock),
                "gen" to Gen(engineClient, forwardAuthorizationMock),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken(any())).thenReturn("anIssuer")
                    whenever(forwardAuthorizationMock.forward(any())).thenReturn(keycloakForwardProviderMock)
                    whenever(keycloakForwardProviderMock.bearerToken()).thenReturn("anIssuer")
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
                        authorizationProvider = eq(keycloakForwardProviderMock),
                        caller = eq(null)
                    )
                    verifyNoMoreInteractions(keycloakForwardProviderMock)
                    verifyNoMoreInteractions(engineClient)
                }
            }
        }

        context("forgive") {
            mapOf(
                "raw" to Raw(engineClient, forwardAuthorizationMock),
                "gen" to Gen(engineClient, forwardAuthorizationMock),
            ).forEach { (name, iouNpl) ->
                test("using $name") {
                    whenever(forwardAuthorizationMock.bearerToken(any())).thenReturn("anIssuer")
                    whenever(forwardAuthorizationMock.forward(any())).thenReturn(keycloakForwardProviderMock)
                    whenever(keycloakForwardProviderMock.bearerToken()).thenReturn("aPayee")
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
                        authorizationProvider = eq(keycloakForwardProviderMock),
                        caller = eq(null)
                    )
                    verifyNoMoreInteractions(keycloakForwardProviderMock)
                }
            }
        }
    }
})
