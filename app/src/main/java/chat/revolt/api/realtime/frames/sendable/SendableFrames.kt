package chat.revolt.api.realtime.frames.sendable

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizationFrame(
    val type: String,
    val token: String
)

@Serializable
data class PingFrame(
    val type: String,
    val data: Int
)

@Serializable
data class BeginTypingFrame(
    val type: String,
    val channel: String
)

@Serializable
data class EndTypingFrame(
    val type: String,
    val channel: String
)
