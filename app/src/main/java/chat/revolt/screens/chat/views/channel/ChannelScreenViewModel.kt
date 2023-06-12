package chat.revolt.screens.chat.views.channel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltJson
import chat.revolt.api.internals.ULID
import chat.revolt.api.realtime.RealtimeSocketFrames
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.MessageDeleteFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.realtime.frames.receivable.MessageUpdateFrame
import chat.revolt.api.routes.channel.SendMessageReply
import chat.revolt.api.routes.channel.ackChannel
import chat.revolt.api.routes.channel.fetchMessagesFromChannel
import chat.revolt.api.routes.channel.fetchSingleChannel
import chat.revolt.api.routes.channel.sendMessage
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.api.routes.microservices.autumn.MAX_ATTACHMENTS_PER_MESSAGE
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.routes.user.addUserIfUnknown
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message
import chat.revolt.callbacks.UiCallback
import chat.revolt.callbacks.UiCallbacks
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class ChannelScreenViewModel : ViewModel() {
    var activeChannel by mutableStateOf<Channel?>(null)

    var renderableMessages = mutableStateListOf<Message>()

    private fun setRenderableMessages(messages: List<Message>) {
        renderableMessages.clear()
        renderableMessages.addAll(messages)
    }

    var typingUsers = mutableStateListOf<String>()

    var messageContent by mutableStateOf("")

    var pendingAttachments = mutableStateListOf<FileArgs>()

    private fun popAttachmentBatch() {
        pendingAttachments =
            pendingAttachments.drop(MAX_ATTACHMENTS_PER_MESSAGE) as SnapshotStateList<FileArgs>
    }

    var sendingMessage by mutableStateOf(false)

    var replies = mutableStateListOf<SendMessageReply>()

    private fun addReply(reply: SendMessageReply) {
        if (replies.any { it.id == reply.id }) return
        replies.add(reply)
    }

    fun toggleReplyMentionFor(reply: SendMessageReply) {
        val index = replies.indexOf(reply)
        val newReply = SendMessageReply(
            reply.id,
            !reply.mention
        )

        replies[index] = newReply
    }

    private fun clearInReplyTo() {
        replies.clear()
    }

    var noMoreMessages by mutableStateOf(false)

    fun fetchOlderMessages() {
        if (activeChannel == null) {
            return
        }

        viewModelScope.launch {
            val messages = arrayListOf<Message>()

            fetchMessagesFromChannel(
                activeChannel!!.id!!,
                limit = 50,
                true,
                before = if (renderableMessages.isNotEmpty()) {
                    renderableMessages.last().id
                } else {
                    null
                }
            ).let {
                if (it.messages.isNullOrEmpty() || it.messages.size < 50) {
                    noMoreMessages = true
                }

                it.messages?.forEach { message ->
                    addUserIfUnknown(message.author ?: return@forEach)
                    if (!RevoltAPI.messageCache.containsKey(message.id)) {
                        RevoltAPI.messageCache[message.id!!] = message
                    }
                    messages.add(message)
                }
            }

            regroupMessages(renderableMessages + messages)
        }
    }

    fun fetchChannel(id: String) {
        viewModelScope.launch {
            if (id !in RevoltAPI.channelCache) {
                val channel = fetchSingleChannel(id)
                activeChannel = channel
                RevoltAPI.channelCache[id] = channel
            } else {
                activeChannel = RevoltAPI.channelCache[id]
            }

            if (activeChannel?.lastMessageID != null) {
                ackNewest()
            } else {
                Log.d("ChannelScreen", "No last message ID, not acking.")
            }
        }
    }

    fun sendPendingMessage() {
        sendingMessage = true

        viewModelScope.launch {
            val attachmentIds = arrayListOf<String>()

            pendingAttachments.take(MAX_ATTACHMENTS_PER_MESSAGE).forEach {
                try {
                    val id = uploadToAutumn(
                        it.file,
                        it.filename,
                        "attachments",
                        ContentType.parse(it.contentType)
                    )
                    Log.d("ChannelScreen", "Uploaded attachment with id $id")
                    attachmentIds.add(id)
                } catch (e: Exception) {
                    Log.e("ChannelScreen", "Failed to upload attachment", e)
                    return@launch
                }
            }

            sendMessage(
                activeChannel!!.id!!,
                messageContent.trimIndent(),
                attachments = if (attachmentIds.isEmpty()) null else attachmentIds,
                replies = replies
            )

            messageContent = ""
            noMoreMessages = false
            popAttachmentBatch()
            clearInReplyTo()
        }
    }

    private suspend fun regroupMessages(newMessages: List<Message> = renderableMessages) {
        val groupedMessages = mutableMapOf<String, Message>()

        // Verbatim implementation of https://wiki.rvlt.gg/index.php/Text_Channel_(UI)#Message_Grouping_Algorithm
        // The exception is the date variable being pushed into cache, we don't need that here.
        // Keep in mind: Recomposing UI is incredibly cheap in Jetpack Compose.
        newMessages.forEach { message ->
            var tail = true

            val next = newMessages.getOrNull(newMessages.indexOf(message) + 1)
            if (next != null) {
                val dateA = Instant.fromEpochMilliseconds(ULID.asTimestamp(message.id!!))
                val dateB = Instant.fromEpochMilliseconds(ULID.asTimestamp(next.id!!))

                val minuteDifference = (dateA - dateB).inWholeMinutes

                if (
                    message.author != next.author ||
                    minuteDifference >= 7 ||
                    message.masquerade != next.masquerade ||
                    message.system != null || next.system != null ||
                    message.replies != null
                ) {
                    tail = false
                }
            } else {
                tail = false
            }

            if (groupedMessages.containsKey(message.id!!)) return@forEach

            groupedMessages[message.id] = message.copy(tail = tail)
        }

        withContext(Dispatchers.Main) {
            setRenderableMessages(groupedMessages.values.toList())
        }
    }

    suspend fun listenForWsFrame() {
        withContext(RevoltAPI.realtimeContext) {
            flow {
                while (true) {
                    emit(RevoltAPI.wsFrameChannel.receive())
                }
            }.onEach {
                when (it) {
                    is MessageFrame -> {
                        if (it.channel != activeChannel?.id) return@onEach

                        addUserIfUnknown(it.author!!)
                        regroupMessages(listOf(it) + renderableMessages)
                        ackNewest()
                    }

                    is MessageUpdateFrame -> {
                        if (it.channel != activeChannel?.id) return@onEach

                        val messageFrame =
                            RevoltJson.decodeFromJsonElement(MessageFrame.serializer(), it.data)

                        renderableMessages.find { currentMsg ->
                            currentMsg.id == it.id
                        } ?: return@onEach // Message not found, ignore.

                        if (messageFrame.author != null)
                            addUserIfUnknown(messageFrame.author)

                        regroupMessages(renderableMessages.map { currentMsg ->
                            if (currentMsg.id == it.id) {
                                currentMsg.mergeWithPartial(messageFrame)
                            } else {
                                currentMsg
                            }
                        })
                    }

                    is MessageDeleteFrame -> {
                        if (it.channel != activeChannel?.id) return@onEach

                        val newRenderableMessages = renderableMessages.filter { currentMsg ->
                            currentMsg.id != it.id
                        }
                        regroupMessages(newRenderableMessages)
                    }

                    is ChannelStartTypingFrame -> {
                        if (it.id != activeChannel?.id) return@onEach
                        if (typingUsers.contains(it.user)) return@onEach

                        addUserIfUnknown(it.user)
                        typingUsers.add(it.user)
                    }

                    is ChannelStopTypingFrame -> {
                        if (it.id != activeChannel?.id) return@onEach
                        if (!typingUsers.contains(it.user)) return@onEach

                        typingUsers.remove(it.user)
                    }

                    is RealtimeSocketFrames.Reconnected -> {
                        Log.d("ChannelScreen", "Reconnected to WS.")
                        listenForWsFrame()
                    }
                }
            }.catch {
                Log.e("ChannelScreen", "Failed to receive WS frame", it)
            }.launchIn(this)
        }
    }

    suspend fun listenForUiCallbacks() {
        withContext(Dispatchers.Main) {
            UiCallbacks.uiCallbackFlow.onEach {
                when (it) {
                    is UiCallback.ReplyToMessage -> {
                        addReply(
                            SendMessageReply(
                                id = it.messageId,
                                mention = false
                            )
                        )
                    }
                }
            }.catch {
                Log.e("ChannelScreen", "Failed to receive UI callback", it)
            }.launchIn(this)
        }
    }

    private var debouncedChannelAck: Job? = null
    private fun ackNewest() {
        if (debouncedChannelAck?.isActive == true) {
            debouncedChannelAck?.cancel()

            Log.d("ChannelScreen", "Cancelling channel ack")
        }

        if (activeChannel?.lastMessageID == null) return

        RevoltAPI.unreads.processExternalAck(
            activeChannel!!.id!!,
            activeChannel!!.lastMessageID!!
        )

        debouncedChannelAck = viewModelScope.launch {
            delay(1000)
            if (activeChannel?.lastMessageID == null) return@launch
            ackChannel(activeChannel!!.id!!, activeChannel!!.lastMessageID!!)

            Log.d("ChannelScreen", "Acking channel")
        }
    }

    fun replyToMessage(message: Message) {
        addReply(
            SendMessageReply(
                id = message.id!!,
                mention = false
            )
        )
    }
}
