package chat.revolt.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.api.internals.BrushCompat
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.microservices.january.asJanuaryProxyUrl
import chat.revolt.api.schemas.Embed
import chat.revolt.api.schemas.Embed as EmbedSchema
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UIMarkdown

@Composable
fun RegularEmbed(
    embed: EmbedSchema,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Stripe at the left side of the embed
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    embed.colour?.let { BrushCompat.parseColour(it) }
                        ?: Brush.solidColor(MaterialTheme.colorScheme.primary)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column {
                // Title row (icon + title)
                Row(
                    modifier = Modifier
                        .then(
                            if (embed.originalURL != null) {
                                Modifier
                                    .clickable {
                                        onLinkClick(embed.originalURL)
                                    }
                            } else {
                                Modifier
                            }
                        )
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    embed.iconURL?.let {
                        if (it.endsWith(".svg")) return@let

                        RemoteImage(
                            url = asJanuaryProxyUrl(it),
                            width = 48,
                            height = 48,
                            contentScale = ContentScale.Crop,
                            description = null // decorative
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    embed.title?.let {
                        Text(
                            text = it,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Description
                embed.description?.let {
                    UIMarkdown(
                        text = it,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Image
                embed.image?.let {
                    if (it.url == null || it.url.endsWith(".svg")) return@let

                    Spacer(modifier = Modifier.height(8.dp))
                    RemoteImage(
                        url = asJanuaryProxyUrl(it.url),
                        width = (it.width ?: 48).toInt(),
                        height = (it.height ?: 48).toInt(),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .then(
                                if (embed.originalURL != null) {
                                    Modifier.clickable {
                                        onLinkClick(embed.originalURL)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .aspectRatio(
                                (it.width?.toFloat() ?: 0f) / (it.height?.toFloat() ?: 0f)
                            ),
                        contentScale = ContentScale.Crop,
                        description = null // decorative
                    )
                }
            }
        }
    }
}

@Composable
fun Embed(embed: Embed, modifier: Modifier = Modifier, onLinkClick: (String) -> Unit) {
    Column {
        when (embed.type) {
            else -> RegularEmbed(embed = embed, modifier = modifier, onLinkClick = onLinkClick)
        }
    }
}
