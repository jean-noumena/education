package seed.email

interface Email {
    fun send(message: String, to: String): Boolean
}
