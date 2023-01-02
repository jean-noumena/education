package seed.email

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import seed.config.Configuration

private const val TEST_MESSAGE = """
<body>Here is a 
<a href="https://api.seed-dev.noumenadigital.com/swagger/#/Authentication/login">blank link</a> 
from the PostmarkEmailTest.
</body>
"""

class PostmarkMailTest {

    @Test
    fun `can send mail via PostmarkApp`() {
        val config = Configuration(
            "", "", // We don't need keycloak for testing mail
            postmarkSenderEmail = "unknown-user@noumenadigital.com"
        )
        val mailer: Email = PostmarkMail(config)
        val wasSent = mailer.send(TEST_MESSAGE, config.postmarkSenderEmail)
        assertTrue(wasSent)
    }

    @Test
    fun `can't send mail via PostmarkApp with wrong credentials`() {
        val config = Configuration(
            "", "", // We don't need keycloak for testing mail
            postmarkSenderEmail = "unknown-user@noumenadigital.com",
            postmarkServerToken = "bad token"
        )
        val mailer: Email = PostmarkMail(config)
        val wasSent = mailer.send(TEST_MESSAGE, config.postmarkSenderEmail)
        assertFalse(wasSent)
    }
}
