package chat.revolt.components.generic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun CountableListHeader(
    text: String,
    count: Int,
    backgroundColor: Color = Color.Transparent
) {
    ListHeader(backgroundColor = backgroundColor) {
        Text(
            text = AnnotatedString.Builder().apply {
                append(text)

                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = LocalTextStyle.current.fontSize * 0.8,
                        color = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                )
                append("—$count")
                pop()
            }.toAnnotatedString()
        )
    }
}
