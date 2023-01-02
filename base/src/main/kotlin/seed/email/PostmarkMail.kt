package seed.email

import com.postmarkapp.postmark.Postmark
import com.postmarkapp.postmark.client.data.model.message.Message
import mu.KotlinLogging
import seed.config.Configuration

private val logger = KotlinLogging.logger {}

class PostmarkMail(val config: Configuration) : Email {

    override fun send(message: String, to: String): Boolean {
        val email = Message()
        email.from = config.postmarkSenderEmail
        email.to = to
        email.subject = "Upload to NXTLOG failed"
        email.htmlBody = message

        return try {
            val response = Postmark.getApiClient(config.postmarkServerToken).deliverMessage(email)
            response.errorCode == 0
        } catch (e: Exception) {
            logger.error(e) { "Email Service failed" }
            false
        }
    }
}
