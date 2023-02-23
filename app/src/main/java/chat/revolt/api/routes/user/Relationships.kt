package chat.revolt.api.routes.user

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerializationException

suspend fun blockUser(userId: String) {
    val response = RevoltHttp.put("/users/$userId/block") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltAPI.userCache[userId] ?: return
    RevoltAPI.userCache[userId] = user.copy(relationship = "Blocked")
}

suspend fun unblockUser(userId: String) {
    val response = RevoltHttp.delete("/users/$userId/block") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltAPI.userCache[userId] ?: return
    RevoltAPI.userCache[userId] = user.copy(relationship = "None")
}