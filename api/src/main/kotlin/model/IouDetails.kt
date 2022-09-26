package model

data class IouDetails(
    val id: java.util.UUID,
    val payee: String,
    val issuer: String,
    val amount: Double
)
