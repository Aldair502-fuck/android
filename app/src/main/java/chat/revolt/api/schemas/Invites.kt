package chat.revolt.api.schemas

import android.net.Uri
import androidx.core.net.toUri
import chat.revolt.api.REVOLT_APP
import chat.revolt.api.REVOLT_INVITES
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Invite(
    val type: String? = null,
    val code: String? = null,

    @SerialName("server_id")
    val serverId: String? = null,

    @SerialName("server_name")
    val serverName: String? = null,

    @SerialName("server_icon")
    val serverIcon: AutumnResource? = null,

    @SerialName("server_banner")
    val serverBanner: AutumnResource? = null,

    @SerialName("server_flags")
    val serverFlags: Long? = null,

    @SerialName("channel_id")
    val channelId: String? = null,

    @SerialName("channel_name")
    val channelName: String? = null,

    @SerialName("user_name")
    val userName: String? = null,

    @SerialName("user_avatar")
    val userAvatar: AutumnResource? = null,

    @SerialName("member_count")
    val memberCount: Long? = null
)

@Serializable
data class InviteJoined(
    val type: String? = null,
    val channels: List<Channel>? = null,
    val server: Server? = null
)

fun Uri.isInviteUri(): Boolean {
    val firstPathSegmentIsInvite = this.pathSegments.firstOrNull() == "invite"
    val isAppRevoltChat = this.host == REVOLT_APP.toUri().host
    val matchRvltGG = this.host == REVOLT_INVITES.toUri().host

    val matchApp = isAppRevoltChat && firstPathSegmentIsInvite

    val hasEnoughSegments =
        if (matchApp) this.pathSegments.size == 2 else this.pathSegments.size == 1

    return (matchApp || matchRvltGG) && hasEnoughSegments
}