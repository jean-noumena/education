package seed.email

import mu.KotlinLogging
import seed.config.Configuration
import java.time.Clock
import java.util.Date
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

private val logger = KotlinLogging.logger {}

class JavaxMail(
    val config: Configuration,
    private val clock: Clock = Clock.systemDefaultZone()
) : Email {
    override fun send(message: String, to: String): Boolean {
        val session = Session.getInstance(properties, passwordAuthenticator)
        val msg = MimeMessage(session)

        msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
        msg.addHeader("format", "flowed")
        msg.addHeader("Content-Transfer-Encoding", "8bit")

        msg.setFrom(InternetAddress(config.javaxEmailFrom))
        msg.setSubject("Subject", "UTF-8")
        msg.setContent(message, "text/html; charset=utf-8")
        msg.sentDate = Date.from(clock.instant())
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false))

        return try {
            Transport.send(msg)
            true
        } catch (e: Exception) {
            logger.error(e) { "Email Service failed" }
            false
        }
    }

    private val properties = System.getProperties().apply {
        this["mail.smtp.host"] = config.javaxSmtpHost
        this["mail.smtp.port"] = config.javaxSmtpPort
        this["mail.transport.protocol"] = "smtp"
        this["mail.smtp.auth"] = true
        this["mail.smtp.starttls.enable"] = true
        this["mail.smtp.starttls.required"] = config.javaxRequireTls
    }

    private val passwordAuthenticator = object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(config.javaxEmailFrom, config.javaxEmailPassword)
        }
    }
}
