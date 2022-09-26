package seed.http

import arrow.core.getOrHandle
import com.noumenadigital.codegen.Party
import com.noumenadigital.npl.api.generated.seed.IouFacade
import com.noumenadigital.npl.api.generated.seed.IouProxy
import com.noumenadigital.platform.engine.client.EngineClientApi
import com.noumenadigital.platform.engine.values.ClientNumberValue
import com.noumenadigital.platform.engine.values.ClientPartyValue
import com.noumenadigital.platform.engine.values.ClientProtocolReferenceValue
import keycloak.ForwardAuthorization
import keycloak.party
import model.IouDetails
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.double
import org.http4k.lens.string
import org.http4k.lens.uuid
import java.math.BigDecimal

interface Iou {
    fun create(): HttpHandler
    fun amountOwed(): HttpHandler
    fun pay(): HttpHandler
    fun forgive(): HttpHandler
}

internal val pathPayee = Path.string().of("payee")
internal val pathAmount = Path.double().of("amount")
internal val pathIouId = Path.uuid().of("iouId")

internal data class CreateResponse(
    val iou: IouDetails
)

internal val createResponseLens = Body.auto<CreateResponse>().toLens()

internal data class AmountResponse(
    val amount: Double
)

internal val amountResponseLens = Body.auto<AmountResponse>().toLens()

private const val userNameClaim = "preferred_username"

internal fun userParty(partyName: String, userName: String): Party {
    return Party(
        entity = mapOf(
            "party" to setOf(partyName),
            userNameClaim to setOf(userName)
        ),
        access = mapOf()
    )
}

internal fun partyPerson(parties: Map<String, ClientPartyValue>, partyName: String): String =
    parties[partyName]?.let { party ->
        party.entity["party"]?.single()
            ?: throw IllegalArgumentException("no entity \"party\" in protocol parties $parties")
        party.entity[userNameClaim]?.single()
            ?: throw IllegalArgumentException("no entity \"preferred_username\" in protocol parties $parties")
    } ?: throw IllegalArgumentException("no party $partyName in protocol parties $parties")

internal fun partyPerson(party: Party) = party.entity[userNameClaim]?.single()
    ?: throw IllegalStateException("userNameClaim not found in parties $party")

/**
 * The Raw class illustrates why you don't want to do this without generated code
 */
class Raw(private val client: EngineClientApi, private val authorizationProvider: ForwardAuthorization) : Iou {

    override fun create(): HttpHandler {
        return { req ->
            val auth = authorizationProvider(req)
            val amount = pathAmount(req).toBigDecimal()
            val payeeName = pathPayee(req)
            val issuer = party(auth.bearerToken())
            val payee = userParty("payee", payeeName)
            val result = client.createProtocol(
                prototypeId = IouFacade.prototypeId,
                parties = listOf(
                    ClientPartyValue(issuer.entity, issuer.access), ClientPartyValue(payee.entity, payee.access)
                ),
                arguments = listOf(ClientNumberValue(amount)),
                authorizationProvider = auth
            )
            val id = result.getOrHandle { throw it }.result as ClientProtocolReferenceValue
            val iouProtocol = client.getProtocolStateById(id.value, auth).getOrHandle { throw it }
            val iouDetails = IouDetails(
                id = iouProtocol.id,
                payee = partyPerson(iouProtocol.parties, "payee"),
                issuer = partyPerson(iouProtocol.parties, "issuer"),
                amount = (iouProtocol.fields["forAmount"] as ClientNumberValue).value.toDouble()
            )
            Response(Status.CREATED).with(createResponseLens of CreateResponse(iouDetails))
        }
    }

    override fun amountOwed(): HttpHandler {
        return { req ->
            val result = client.selectAction(
                protocolId = pathIouId(req),
                action = "getAmountOwed",
                arguments = listOf(),
                authorizationProvider = authorizationProvider(req),
                caller = null
            ).getOrHandle { throw it }.result as ClientNumberValue
            Response(Status.OK).with(amountResponseLens of AmountResponse(result.value.toDouble()))
        }
    }

    override fun pay(): HttpHandler {
        return { req ->
            val iouProtocolId = pathIouId(req)
            val amountToPay = pathAmount(req).toBigDecimal()

            val result = client.selectAction(
                protocolId = iouProtocolId,
                action = "pay",
                arguments = listOf(ClientNumberValue(amountToPay)),
                authorizationProvider = authorizationProvider(req)
            ).getOrHandle { throw it }.result as ClientNumberValue
            Response(Status.OK).with(amountResponseLens of AmountResponse(result.value.toDouble()))
        }
    }

    override fun forgive(): HttpHandler {
        return { req ->
            val iouProtocolId = pathIouId(req)

            client.selectAction(
                protocolId = iouProtocolId,
                action = "forgive",
                arguments = listOf(),
                authorizationProvider = authorizationProvider(req)
            ).getOrHandle { throw it }

            Response(Status.NO_CONTENT)
        }
    }
}

class Gen(val client: EngineClientApi, private val authorizationProvider: ForwardAuthorization) : Iou {

    private val proxy = IouProxy(client)

    override fun create(): HttpHandler {
        return { req ->
            val auth = authorizationProvider(req)
            val payeeName = pathPayee(req)
            val result = proxy.create(
                issuer = party(auth.bearerToken()),
                payee = userParty("payee", payeeName),
                forAmount = BigDecimal(pathAmount(req)),
                authorizationProvider = authorizationProvider(req)
            )

            val id = result.getOrHandle { throw it }.result
            val iouProtocol = client.getProtocolStateById(id.id, auth).getOrHandle { throw it }
            val iouFacade = IouFacade(iouProtocol)
            val iouDetails = IouDetails(
                id = iouProtocol.id,
                payee = partyPerson(iouFacade.parties.payee),
                issuer = partyPerson(iouFacade.parties.issuer),
                amount = iouFacade.fields.forAmount.toDouble()
            )
            Response(Status.CREATED).with(createResponseLens of CreateResponse(iouDetails))
        }
    }

    override fun amountOwed(): HttpHandler {
        return { req ->
            val iouProtocolId = pathIouId(req)

            val result = proxy.getAmountOwed(
                protocolId = iouProtocolId,
                authorizationProvider = authorizationProvider(req)
            ).getOrHandle { throw it }
            Response(Status.OK).with(amountResponseLens of AmountResponse(result.result.toDouble()))
        }
    }

    override fun pay(): HttpHandler {
        return { req ->
            val amountToPay = pathAmount(req)
            val iouProtocolId = pathIouId(req)

            val result = proxy.pay(
                protocolId = iouProtocolId,
                amount = amountToPay.toBigDecimal(),
                authorizationProvider = authorizationProvider(req)
            ).getOrHandle { throw it }
            Response(Status.OK).with(amountResponseLens of AmountResponse(result.result.toDouble()))
        }
    }

    override fun forgive(): HttpHandler {
        return { req ->
            val iouProtocolId = pathIouId(req)

            proxy.forgive(
                protocolId = iouProtocolId,
                authorizationProvider = authorizationProvider(req)
            ).getOrHandle { throw it }

            Response(Status.NO_CONTENT)
        }
    }
}
