package seed.email

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import seed.config.IPostMarkEmailConfiguration
import seed.config.PostMarkEmailConfiguration
import seed.utilities.testConfig

private const val POSTMARK_TEST_API_TOKEN = "POSTMARK_API_TEST"

private const val TEST_MESSAGE = """
<body>Here is a 
<a href="https://api.seed-dev.noumenadigital.com/swagger/#/Authentication/login">blank link</a> 
from the PostmarkEmailTest.
</body>
"""

class PostmarkMailTest {

    @Test
    fun `can send mail via PostmarkApp`() {
        val config: IPostMarkEmailConfiguration =
            (testConfig as PostMarkEmailConfiguration).copy(postmarkServerToken = POSTMARK_TEST_API_TOKEN)

        val mailer: Email = PostmarkMail(config)
        val wasSent = mailer.send(TEST_MESSAGE, config.postmarkSenderEmail)
        assertTrue(wasSent)
    }

    @Test
    fun `can't send mail via PostmarkApp with wrong credentials`() {
        val config: IPostMarkEmailConfiguration =
            (testConfig as PostMarkEmailConfiguration).copy(postmarkServerToken = "bad token")

        val mailer: Email = PostmarkMail(config)
        val wasSent = mailer.send(TEST_MESSAGE, config.postmarkSenderEmail)
        assertFalse(wasSent)
    }
}
