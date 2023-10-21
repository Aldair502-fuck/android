package chat.revolt.internals.markdown

import android.text.format.DateUtils
import android.util.Log
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant

fun resolveTimestamp(timestamp: Long, modifier: String? = null): String {
    val normalisedModifier = modifier.orEmpty().removePrefix(":")

    val instant = Instant.fromEpochSeconds(timestamp)
    val javaInstant = instant.toJavaInstant()

    try {
        if (timestamp < 0) {
            return "<invalid timestamp>"
        }

        val outString = when (normalisedModifier) {
            // 22:22
            "t" -> DateTimeFormatter.ofPattern("HH:mm")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(javaInstant)

            // 22:22:22
            "T" -> DateTimeFormatter.ofPattern("HH:mm:ss")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(javaInstant)

            // 22 September 2022
            "D" -> DateTimeFormatter.ofPattern("dd MMMM yyyy")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(javaInstant)

            // 22 September 2022 22:22
            "f" -> DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(javaInstant)

            // Thursday, 22 September 2022 22:22
            "F" -> DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(javaInstant)

            // 9 months ago
            "R" -> DateUtils.getRelativeTimeSpanString(
                timestamp * 1000,
                Clock.System.now().toEpochMilliseconds(),
                DateUtils.MINUTE_IN_MILLIS
            )

            // Fallback. Shouldn't happen, regex already checks for this
            else -> timestamp.toString()
        }

        return outString.toString()
    } catch (e: Exception) {
        Log.e("Timestamp", "Failed to parse timestamp", e)
        return "<invalid timestamp>"
    }
}
