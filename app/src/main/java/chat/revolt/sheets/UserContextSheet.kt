package chat.revolt.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.components.chat.RoleChip
import chat.revolt.components.generic.NonIdealState
import chat.revolt.components.generic.WebMarkdown
import chat.revolt.components.screens.settings.RawUserOverview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserContextSheet(
    userId: String,
    serverId: String? = null
) {
    val user = RevoltAPI.userCache[userId]

    val member = serverId?.let { RevoltAPI.members.getMember(it, userId) }

    val server = RevoltAPI.serverCache[serverId]

    var profile by remember { mutableStateOf<Profile?>(null) }
    var profileNotFound by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        try {
            user?.id?.let { fetchUserProfile(it) }?.let { profile = it }
        } catch (e: Error) {
            if (e.message == "NotFound") {
                profileNotFound = true
            }
            e.printStackTrace()
        }
    }

    if (user == null) {
        // TODO fetch user in this scenario
        NonIdealState(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_alert_decagram_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(it)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.user_context_sheet_user_not_found)
                )
            },
            description = {
                Text(
                    text = stringResource(R.string.user_context_sheet_user_not_found_description)
                )
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        RawUserOverview(user, profile)

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
        ) {
            member?.roles?.let {
                Text(
                    text = stringResource(id = R.string.user_context_sheet_category_roles),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    it
                        .map { roleId -> server?.roles?.get(roleId) }
                        .sortedBy { it?.rank ?: 0.0 }
                        .forEach { role ->
                            role?.let {
                                RoleChip(
                                    label = role.name ?: "null",
                                    brush = role.colour?.let { WebCompat.parseColour(it) }
                                        ?: Brush.solidColor(LocalContentColor.current)
                                )
                            }
                        }
                }
            }

            Text(
                text = stringResource(id = R.string.user_context_sheet_category_bio),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            if (profile?.content.isNullOrBlank().not()) {
                WebMarkdown(
                    text = profile!!.content!!,
                    maskLoading = true
                )
            } else if (profile != null) {
                Text(
                    text = stringResource(id = R.string.user_context_sheet_bio_empty),
                    color = LocalContentColor.current.copy(alpha = 0.6f)
                )
            } else if (profileNotFound) {
                Text(
                    text = stringResource(id = R.string.user_context_sheet_bio_not_found),
                    color = LocalContentColor.current.copy(alpha = 0.6f)
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}