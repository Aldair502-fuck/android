package chat.revolt.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

val RevoltColorScheme = darkColorScheme(
    primary = Color(0xfffe4654),
    onPrimary = Color(0xffffffff),
    secondary = Color(0xfffd6671),
    onSecondary = Color(0xffffffff),
    tertiary = Color(0xffff6667),
    onTertiary = Color(0xffffffff),
    background = Color(0xff101823),
    onBackground = Color(0xffffffff),
    surfaceVariant = Color(0xff172333),
    onSurfaceVariant = Color(0xffffffff),
    surface = Color(0xff111a26),
    onSurface = Color(0xffffffff),
)

val AmoledColorScheme = RevoltColorScheme.copy(
    background = Color(0xff000000),
    onBackground = Color(0xffffffff),
    surfaceVariant = Color(0xff131313),
    onSurfaceVariant = Color(0xffffffff),
    surface = Color(0xff212121),
    onSurface = Color(0xffffffff),
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xfffe4654),
    onPrimary = Color(0xffffffff),
    secondary = Color(0xfffd6671),
    onSecondary = Color(0xffffffff),
    tertiary = Color(0xffff6667),
    onTertiary = Color(0xffffffff),
    background = Color(0xffffffff),
    onBackground = Color(0xff000000),
    surfaceVariant = Color(0xffe6e6e6),
    onSurfaceVariant = Color(0xff000000),
    surface = Color(0xffdddddd),
    onSurface = Color(0xff000000),
)

enum class Theme {
    None,
    Revolt,
    Light,
    M3Dynamic,
    Amoled,
}

@Composable
fun RevoltTheme(
    requestedTheme: Theme,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val systemInDarkTheme = isSystemInDarkTheme()
    val m3Supported = systemSupportsDynamicColors()

    val colorScheme = when {
        m3Supported && requestedTheme == Theme.M3Dynamic && systemInDarkTheme -> dynamicDarkColorScheme(
            context
        )
        m3Supported && requestedTheme == Theme.M3Dynamic && !systemInDarkTheme -> dynamicLightColorScheme(
            context
        )
        requestedTheme == Theme.Revolt -> RevoltColorScheme
        requestedTheme == Theme.Light -> LightColorScheme
        requestedTheme == Theme.Amoled -> AmoledColorScheme
        requestedTheme == Theme.None && systemInDarkTheme -> RevoltColorScheme
        requestedTheme == Theme.None && !systemInDarkTheme -> LightColorScheme
        else -> RevoltColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RevoltTypography,
        content = content
    )
}

fun systemSupportsDynamicColors(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}