package seed.security

import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class KeycloakAuthorizationProviderTest {

    @Test
    fun `bearer token is forwarded`() {
        val given = Request(Method.GET, "https://example.com")
            .header("Authorization", "Bearer MyToken")

        val provider = KeycloakAuthorizationProvider(given)
        val got = provider()

        assertNotNull(got, "no authorization created")
        assertEquals("MyToken", got.token)
    }

    @Test
    fun `no token means no authorization`() {
        val given = Request(Method.GET, "https://example.com")
        val provider = KeycloakAuthorizationProvider(given)
        val got = provider()

        assertNull(got, "unexpected authorization")
    }
}
