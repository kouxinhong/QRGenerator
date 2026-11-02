package com.example.qrgenerator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = ActionGreen, onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F3E9), onPrimaryContainer = Color(0xFF065333),
    secondary = ActionGreen, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F3E9), onSecondaryContainer = Color(0xFF065333),
    background = PageGray, onBackground = TextBlack,
    surface = Color.White, onSurface = TextBlack,
    surfaceVariant = Color(0xFFF5F6F7), onSurfaceVariant = TextGray,
    outline = Color(0xFF92979E), outlineVariant = DividerGray,
    error = Color(0xFFC83C32), onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF69D6A2), onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF164B34), onPrimaryContainer = Color(0xFFB6F0D1),
    secondary = Color(0xFF69D6A2), onSecondary = Color(0xFF003820),
    secondaryContainer = Color(0xFF164B34), onSecondaryContainer = Color(0xFFB6F0D1),
    background = Color(0xFF111111), onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF202020), onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF292929), onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF858585), outlineVariant = Color(0xFF393939),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
)

@Composable
fun QRGeneratorTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors, typography = Typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(20.dp)),
        content = content
    )
}
