package com.example.mathwake.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = NavyText,
    secondary = MintSuccess,
    onSecondary = Color.White,
    secondaryContainer = MintBgLight,
    onSecondaryContainer = NavyText,
    tertiary = HardBlue,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralBgLight,
    onErrorContainer = CoralError,
    background = CreamBackground,
    onBackground = NavyText,
    surface = CardWhite,
    onSurface = NavyText,
    surfaceVariant = Color(0xFFF3EFEA),
    onSurfaceVariant = SubTextLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = Color(0xFF1F160C),
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = DarkText,
    secondary = MintSuccess,
    onSecondary = Color(0xFF0A2115),
    secondaryContainer = MintBgDark,
    onSecondaryContainer = DarkText,
    tertiary = HardBlue,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralBgDark,
    onErrorContainer = Color(0xFFFFB4B4),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SubTextDark,
    outline = OutlineDark
)

@Composable
fun MathWakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}