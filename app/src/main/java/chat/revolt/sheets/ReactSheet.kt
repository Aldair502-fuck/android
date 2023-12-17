package chat.revolt.sheets

import androidx.compose.runtime.Composable
import chat.revolt.api.RevoltAPI
import chat.revolt.components.emoji.EmojiPicker

@Composable
fun ReactSheet(messageId: String, onSelect: (String?) -> Unit) {
    val message = RevoltAPI.messageCache[messageId]

    if (message == null) {
        onSelect(null)
        return
    }

    EmojiPicker {
        onSelect(it.removeSurrounding(":"))
    }
}