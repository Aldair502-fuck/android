package chat.revolt.api.internals

import android.util.Log
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color

// color is spelled american because Color from compose is spelled american
fun Brush.Companion.solidColor(colour: Color) = linearGradient(
    colorStops = arrayOf(
        0f to colour,
        1f to colour
    )
)

// Some colours that are not present in Android's built-in list.
// not exhaustive, but covers most of the ones I've seen in the wild
// for the sake of all of us, please just use hex codes
// reference: https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
private val ADDITIONAL_WEB_COLOURS = mapOf(
    "orange" to Color(0xFFFFA500),
    "rebeccapurple" to Color(0xFF663399),
    "transparent" to Color.Transparent,
    "inherit" to Color.Unspecified,
    "initial" to Color.Unspecified,
    "unset" to Color.Unspecified,
)

object WebCompat {
    @Composable
    private fun parseLinearGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float, Color>>()

        val parts = mutableListOf<String>()
        var startIndex = 0
        var openParenthesesCount = 0

        for (i in gradient.indices) {
            when (gradient[i]) {
                '(' -> openParenthesesCount++
                ')' -> openParenthesesCount--
                ',' -> {
                    if (openParenthesesCount == 0) {
                        val part = gradient.substring(startIndex, i).trim()
                        parts.add(part)
                        startIndex = i + 1
                    }
                }
            }
        }

        val lastPart = gradient.substring(startIndex).trim()
        if (lastPart.isNotEmpty()) {
            parts.add(lastPart)
        }

        parts.forEachIndexed { index, part ->
            if (part.startsWith("to") || part.endsWith("deg")) {
                // we don't support any other direction / blocked on compose supporting them
                // TODO could probably emulate this by swapping the values around
            } else {
                val splitPart = part.split(" ")

                val colourPart = splitPart[0]
                val colour = when {
                    colourPart.startsWith("var(") -> {
                        parseVarToColour(
                            colourPart.substringAfter("var(").substringBeforeLast(")")
                        )
                    }

                    else -> parseFunctionColour(colourPart) ?: parseColourName(colourPart)
                }

                val stop = if (splitPart.size == 2) {
                    splitPart[1].removeSuffix("%").toFloat() / 100f
                } else {
                    index.toFloat() / (parts.size - 1)
                }

                stops.add(stop to colour)
            }
        }

        return linearGradient(
            colorStops = stops.toTypedArray()
        )
    }

    @Composable
    private fun parseFunctionColour(colourString: String): Color? {
        val cleanedString = colourString.trim()

        return try {
            if (cleanedString.startsWith("rgb(")) {
                parseRGBColour(cleanedString)
            } else if (cleanedString.startsWith("rgba(")) {
                parseRGBAColour(cleanedString)
            } else {
                throw IllegalArgumentException("Invalid colour format: $colourString")
            }
        } catch (e: Exception) {
            Log.d(
                "WebCompat",
                "Failed to parse colour $colourString, falling back to LocalContentColor.current"
            )
            null
        }
    }

    private fun parseRGBColour(rgbString: String): Color {
        val colourParts = rgbString.removePrefix("rgb(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim().toInt() }

        val red = colourParts[0] / 255.0f
        val green = colourParts[1] / 255.0f
        val blue = colourParts[2] / 255.0f

        return Color(red, green, blue)
    }

    private fun parseRGBAColour(rgbaString: String): Color {
        val colourParts = rgbaString.removePrefix("rgba(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim() }

        val red = colourParts[0].toInt() / 255.0f
        val green = colourParts[1].toInt() / 255.0f
        val blue = colourParts[2].toInt() / 255.0f
        val alpha = colourParts[3].removeSuffix("%").toFloat() / 100.0f

        return Color(red, green, blue, alpha)
    }

    @Composable
    private fun parseVarToColour(varName: String): Color {
        return when (varName) {
            "--accent" -> MaterialTheme.colorScheme.primary
            "--foreground" -> MaterialTheme.colorScheme.onBackground
            "--background" -> MaterialTheme.colorScheme.background
            "--error" -> MaterialTheme.colorScheme.error
            else -> LocalContentColor.current
        }
    }

    @Composable
    private fun parseVar(varName: String): Brush {
        return Brush.solidColor(parseVarToColour(varName))
    }

    @Composable
    private fun parseColourName(colour: String): Color {
        return try {
            val additionalWebColour = ADDITIONAL_WEB_COLOURS[colour]
            if (additionalWebColour != null) {
                Log.d(
                    "WebCompat",
                    "Parsed additional web colour $colour to $additionalWebColour"
                )
                return additionalWebColour
            }

            Color(android.graphics.Color.parseColor(colour))
        } catch (e: IllegalArgumentException) {
            Log.d(
                "WebCompat",
                "Failed to parse colour $colour, falling back to LocalContentColor.current"
            )
            LocalContentColor.current
        }
    }

    @Composable
    fun parseColour(colour: String): Brush {
        when {
            colour.startsWith("var(") -> {
                Log.d(
                    "WebCompat",
                    "Parsing variable $colour"
                )
                return parseVar(
                    colour.substringAfter("var(").substringBeforeLast(")")
                )
            }

            colour.startsWith("linear-gradient(") || colour.startsWith("repeating-linear-gradient(") -> {
                return parseLinearGradient(
                    colour
                        .substringAfter("repeating-")
                        .substringAfter("linear-gradient(")
                        .substringBeforeLast(")")
                )
            }

            else -> {
                return Brush.solidColor(parseColourName(colour))
            }
        }
    }
}