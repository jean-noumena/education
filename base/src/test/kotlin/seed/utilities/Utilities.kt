package seed.utilities

import com.noumenadigital.platform.engine.values.ClientMapValue
import com.noumenadigital.platform.engine.values.ClientPartyValue
import com.noumenadigital.platform.engine.values.ClientProtocolReferenceValue
import com.noumenadigital.platform.engine.values.ClientProtocolState
import com.noumenadigital.platform.engine.values.ClientSetValue
import com.noumenadigital.platform.engine.values.ClientTextValue
import com.noumenadigital.platform.engine.values.ClientValue
import java.time.Instant
import java.util.UUID

private const val issuerName = "broke"
private const val payeeName = "bff"

class PartyMap private constructor(
    val delegate: Map<String, ClientPartyValue>
) : Map<String, ClientPartyValue> by delegate {

    class Builder() {
        private val builderInternalMap: MutableMap<String, ClientPartyValue> = mutableMapOf()

        fun pPayee(userName: String): Builder {
            builderInternalMap["payee"] =
                ClientPartyValue(
                    entity = mapOf(
                        "party" to setOf("payee"),
                        "preferred_username" to setOf(userName)
                    ),
                    access = mapOf()
                )
            return this
        }

        fun pIssuer(userName: String): Builder {
            builderInternalMap["issuer"] =
                ClientPartyValue(
                    entity = mapOf(
                        "party" to setOf("issuer"),
                        "preferred_username" to setOf(userName)
                    ),
                    access = mapOf()
                )
            return this
        }

        fun build(): PartyMap = PartyMap(builderInternalMap)
    }
}

fun validProtocolState(
    prototypeId: String,
    id: UUID,
    currentState: String,
    fields: Map<String, ClientValue> = emptyMap(),
    party: Map<String, ClientPartyValue> = PartyMap.Builder().pPayee(payeeName).pIssuer(issuerName).build()
) = ClientProtocolState(
    id = id,
    created = Instant.now(),
    prototypeId = prototypeId,
    parties = party,
    observers = mapOf(),
    currentState = currentState,
    version = 0L,
    fields = fields,
    signatures = mapOf()
)

fun setToField(name: String, set: Set<ClientProtocolReferenceValue>): Pair<String, ClientValue> =
    name to ClientSetValue(set.map { ClientProtocolReferenceValue(it.value, null) }.toSet())

fun mapToField(name: String, map: Map<String, ClientProtocolReferenceValue>): Pair<String, ClientValue> =
    name to ClientMapValue(
        LinkedHashMap(
            map.map { (k, v) ->
                ClientTextValue(k) to ClientProtocolReferenceValue(
                    v.value,
                    null
                )
            }.toMap()
        )
    )
