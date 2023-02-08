package iou.model

data class IouDetails(
    val id: java.util.UUID,
    val payee: String,
    val issuer: String,
    val amount: Double,
)

enum class EventType { IouComplete, Payment }

data class Event(
    val type: EventType,
    val amount: Double,
    val remaining: Double,
)
