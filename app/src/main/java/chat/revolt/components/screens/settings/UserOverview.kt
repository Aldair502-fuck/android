package chat.revolt.components.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus

@Composable
fun SelfUserOverview() {
    val selfUser = RevoltAPI.userCache[RevoltAPI.selfId] ?: return

    UserOverview(selfUser)
}

@Composable
fun UserOverview(user: User) {
    var profile by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(user) {
        try {
            if (profile == null) {
                profile = fetchUserProfile(user.id ?: ULID.makeSpecial(0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    RawUserOverview(user, profile)
}

@Composable
fun RawUserOverview(user: User, profile: Profile? = null) {
    Box(
        contentAlignment = Alignment.BottomStart,
    ) {
        profile?.background?.let { background ->
            RemoteImage(
                url = "$REVOLT_FILES/backgrounds/${background.id}",
                description = null,
                modifier = Modifier
                    .height(128.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )

            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .height(128.dp)
                    .fillMaxWidth()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            UserAvatar(
                username = user.displayName ?: stringResource(id = R.string.unknown),
                userId = user.id ?: ULID.makeSpecial(0),
                avatar = user.avatar,
                size = 48.dp,
                presence = presenceFromStatus(user.status?.presence ?: "Offline"),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = AnnotatedString.Builder().apply {
                    if (user.displayName != null) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(user.displayName)
                        pop()
                        append("\n")
                    }
                    append("${user.username}")
                    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraLight))
                    append("#${user.discriminator}")
                    pop()
                }.toAnnotatedString(),
                color = if (profile?.background != null) Color.White else LocalContentColor.current,
            )
        }
    }
}