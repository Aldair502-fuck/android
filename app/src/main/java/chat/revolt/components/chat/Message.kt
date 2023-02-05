package chat.revolt.components.chat

import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.markdown.Renderer
import chat.revolt.api.schemas.Message as MessageSchema

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(
        ctx,
        Uri.parse("$REVOLT_FILES/attachments/${attachment.id}/${attachment.filename}")
    )
}


fun formatLongAsTime(time: Long): String {
    val date = java.util.Date(time)
    val format =
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())

    // EQUIVALENT CODE WITH kotlinx.datetime:

    // val date = Instant.fromEpochMilliseconds(time)
    // val format = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")


    return format.format(date)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Message(
    message: MessageSchema
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column {
        if (message.tail == false) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        message.replies?.forEach { reply ->
            val replyMessage = RevoltAPI.messageCache[reply] ?: return@forEach

            InReplyTo(
                messageId = reply,
                withMention = message.mentions?.contains(replyMessage.author) == true
            ) {
                // TODO Add jump to message
            }
        }

        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (message.content != null && message.content.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(message.content))

                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.copied),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                )
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
        ) {
            if (message.tail == false) {
                UserAvatar(
                    username = author.username ?: "",
                    userId = author.id!!,
                    avatar = author.avatar
                )
            } else {
                UserAvatarWidthPlaceholder()
            }

            Column(modifier = Modifier.padding(start = 10.dp)) {
                if (message.tail == false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author.username ?: "",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = formatLongAsTime(ULID.asTimestamp(message.id!!)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                message.content?.let {
                    Text(
                        text = Renderer.annotateMarkdown(it),
                    )
                }

                message.attachments?.let {
                    if (message.attachments.isNotEmpty()) {
                        message.attachments.forEach { attachment ->
                            if (attachment.metadata?.type == "Image") {
                                RemoteImage(
                                    url = "$REVOLT_FILES/attachments/${attachment.id}/image.png",
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .clickable {
                                            viewAttachmentInBrowser(context, attachment)
                                        },
                                    width = attachment.metadata.width?.toInt() ?: 0,
                                    height = attachment.metadata.height?.toInt() ?: 0,
                                    contentScale = ContentScale.Fit,
                                    description = "Attached image ${attachment.filename}"
                                )
                            } else {
                                Text(
                                    text = attachment.filename ?: "Attachment",
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            viewAttachmentInBrowser(context, attachment)
                                        }
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}