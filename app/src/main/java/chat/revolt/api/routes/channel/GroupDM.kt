package chat.revolt.api.routes.channel

import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.Channel
import chat.revolt.screens.create.MAX_ADDABLE_PEOPLE_IN_GROUP
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class CreateGroupDMBody(
    val name: String,
    val users: List<String>
)

suspend fun createGroupDM(name: String, members: List<String>): Channel {
    if (members.size > MAX_ADDABLE_PEOPLE_IN_GROUP) {
        throw Exception("Too many members, maximum is $MAX_ADDABLE_PEOPLE_IN_GROUP")
    }

    val response = RevoltHttp.post("/channels/create") {
        contentType(ContentType.Application.Json)
        setBody(CreateGroupDMBody(name, members))
    }.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RevoltJson.decodeFromString(Channel.serializer(), response)
}