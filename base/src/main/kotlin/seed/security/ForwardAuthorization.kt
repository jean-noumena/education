package seed.security

import com.noumenadigital.platform.engine.client.AuthorizationProvider
import com.noumenadigital.platform.engine.values.ClientPartyValue
import org.http4k.core.Request

interface ForwardAuthorization {
    fun party(request: Request): ClientPartyValue
    fun forward(request: Request): AuthorizationProvider
    fun bearerToken(request: Request): String
}
