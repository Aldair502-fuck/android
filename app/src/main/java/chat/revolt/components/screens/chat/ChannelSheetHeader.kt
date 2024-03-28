package chat.revolt.components.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.markdown.MarkdownTree
import chat.revolt.ndk.AstNode
import chat.revolt.ndk.Stendal

@Composable
fun ChannelSheetHeader(
    channelName: String,
    channelIcon: AutumnResource? = null,
    channelType: ChannelType,
    channelDescription: String? = null,
    dmPartner: User? = null
) {
    var renderedChannelDescription by remember { mutableStateOf<AstNode?>(null) }

    LaunchedEffect(channelDescription) {
        if (channelDescription != null) {
            renderedChannelDescription = Stendal.render(channelDescription)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (channelType == ChannelType.DirectMessage) CircleShape else MaterialTheme.shapes.medium)
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channelIcon != null) {
                RemoteImage(
                    url = "$REVOLT_FILES/icons/${channelIcon.id ?: ""}?max_side=48",
                    description = null, // decorative
                    contentScale = ContentScale.Crop,
                    height = 48,
                    width = 48,
                    modifier = Modifier
                        .size(48.dp)
                )
            } else if (dmPartner != null) {
                UserAvatar(
                    username = User.resolveDefaultName(dmPartner),
                    userId = dmPartner.id ?: "",
                    avatar = dmPartner.avatar,
                    presence = null,
                    size = 48.dp,
                    modifier = Modifier
                        .size(48.dp)
                )
            } else {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                    ChannelIcon(channelType = channelType)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = channelName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (renderedChannelDescription != null && channelDescription?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                MarkdownTree(node = renderedChannelDescription!!)
            }
        }
    }
}
