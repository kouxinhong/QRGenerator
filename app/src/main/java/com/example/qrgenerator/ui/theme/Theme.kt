package com.example.qrgenerator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = MimoDarkText,
    onSecondary = MimoBlack,
    background = MimoBlack,
    onBackground = MimoDarkText,
    surface = MimoDarkSurface,
    onSurface = MimoDarkText,
    surfaceVariant = MimoDarkCard,
    onSurfaceVariant = MimoDarkTextSecondary,
    outline = MimoDarkDivider,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeLight,
    onPrimaryContainer = Color.White,
    secondary = MimoLightText,
    onSecondary = MimoWhite,
    background = MimoWhite,
    onBackground = MimoLightText,
    surface = MimoWhite,
    onSurface = MimoLightText,
    surfaceVariant = MimoLightSurface,
    onSurfaceVariant = MimoLightTextSecondary,
    outline = MimoLightDivider,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun QRGeneratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
