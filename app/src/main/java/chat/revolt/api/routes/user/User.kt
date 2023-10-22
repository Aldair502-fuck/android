package chat.revolt.api.routes.user

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.Profile
import chat.revolt.api.schemas.Status
import chat.revolt.api.schemas.User
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

suspend fun fetchSelf(): User {
    val response = RevoltHttp.get("/users/@me")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltJson.decodeFromString(User.serializer(), response)

    if (user.id == null) {
        throw Error("Self user ID is null")
    }

    RevoltAPI.userCache[user.id] = user
    RevoltAPI.selfId = user.id

    return user
}

suspend fun patchSelf(
    status: Status? = null,
    pure: Boolean = false,
) {
    val body = mutableMapOf<String, JsonElement>()
    if (status != null) {
        body["status"] = RevoltJson.encodeToJsonElement(Status.serializer(), status)
    }

    val response = RevoltHttp.patch("/users/@me") {
        contentType(ContentType.Application.Json)
        setBody(
            RevoltJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ), body
            )
        )
    }
        .bodyAsText()

    if (RevoltAPI.selfId == null) {
        throw Error("Self ID is null")
    }

    val currentUser = RevoltAPI.userCache[RevoltAPI.selfId] ?: fetchSelf()
    val newUserKeys = RevoltJson.decodeFromString(User.serializer(), response)
    val mergedUser = currentUser.mergeWithPartial(newUserKeys)

    if (!pure) {
        RevoltAPI.userCache[RevoltAPI.selfId!!] = mergedUser
    }
}

suspend fun fetchUser(id: String): User {
    val res = RevoltHttp.get("/users/$id")

    if (res.status.value == 404) {
        return User.getPlaceholder(id)
    }

    val response = res.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltJson.decodeFromString(User.serializer(), response)

    user.id?.let {
        RevoltAPI.userCache[it] = user
    }

    return user
}

suspend fun getOrFetchUser(id: String): User {
    return RevoltAPI.userCache[id] ?: fetchUser(id)
}

suspend fun addUserIfUnknown(id: String) {
    if (RevoltAPI.userCache[id] == null) {
        RevoltAPI.userCache[id] = fetchUser(id)
    }
}

suspend fun fetchUserProfile(id: String): Profile {
    val res = RevoltHttp.get("/users/$id/profile")

    val response = res.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RevoltJson.decodeFromString(Profile.serializer(), response)
}