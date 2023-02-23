package chat.revolt.api.schemas

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
enum class ContentReportReason(val value: String) {
    NoneSpecified("NoneSpecified"),
    Illegal("Illegal"),
    PromotesHarm("PromotesHarm"),
    SpamAbuse("SpamAbuse"),
    Malware("Malware"),
    Harassment("Harassment");

    companion object : KSerializer<ContentReportReason> {
        override val descriptor: SerialDescriptor
            get() {
                return PrimitiveSerialDescriptor(
                    "chat.revolt.api.schemas.ContentReportReason",
                    PrimitiveKind.STRING
                )
            }

        override fun deserialize(decoder: Decoder): ContentReportReason =
            when (val value = decoder.decodeString()) {
                "NoneSpecified" -> NoneSpecified
                "Illegal" -> Illegal
                "PromotesHarm" -> PromotesHarm
                "SpamAbuse" -> SpamAbuse
                "Malware" -> Malware
                "Harassment" -> Harassment
                else -> throw IllegalArgumentException("Unknown ContentReportReason: $value")
            }

        override fun serialize(encoder: Encoder, value: ContentReportReason) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
enum class UserReportReason(val value: String) {
    NoneSpecified("NoneSpecified"),
    SpamAbuse("SpamAbuse"),
    InappropriateProfile("InappropriateProfile"),
    Impersonation("Impersonation"),
    BanEvasion("BanEvasion"),
    Underage("Underage");

    companion object : KSerializer<UserReportReason> {
        override val descriptor: SerialDescriptor
            get() {
                return PrimitiveSerialDescriptor(
                    "chat.revolt.api.schemas.UserReportReason",
                    PrimitiveKind.STRING
                )
            }

        override fun deserialize(decoder: Decoder): UserReportReason =
            when (val value = decoder.decodeString()) {
                "NoneSpecified" -> NoneSpecified
                "SpamAbuse" -> SpamAbuse
                "InappropriateProfile" -> InappropriateProfile
                "Impersonation" -> Impersonation
                "BanEvasion" -> BanEvasion
                "Underage" -> Underage
                else -> throw IllegalArgumentException("Unknown UserReportReason: $value")
            }

        override fun serialize(encoder: Encoder, value: UserReportReason) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class MessageReport(
    val type: String = "Message",
    val id: String,
    val report_reason: ContentReportReason,
)

@Serializable
data class FullMessageReport(
    val content: MessageReport,
    val additional_context: String? = null,
)

@Serializable
data class ServerReport(
    val type: String = "Server",
    val id: String,
    val report_reason: ContentReportReason,
)

@Serializable
data class FullServerReport(
    val content: ServerReport,
    val additional_context: String? = null,
)

@Serializable
data class UserReport(
    val type: String = "User",
    val id: String,
    val report_reason: UserReportReason,
)

@Serializable
data class FullUserReport(
    val content: UserReport,
    val additional_context: String? = null,
)