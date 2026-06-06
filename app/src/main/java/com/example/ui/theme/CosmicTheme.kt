package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView

// Palette Design Tokens
val CosmicDeepNavy = Color(0xFF030A16)
val CosmicMidnightSlate = Color(0xFF0D1726)
val CosmicElectricBlue = Color(0xFF00B7FF)
val CosmicNeonCyan = Color(0xFF00F0FF)
val CosmicRoyalGold = Color(0xFFFFD54A)
val CosmicTextSilver = Color(0xFFC9D1D9)
val CosmicPurpleAccent = Color(0xFF6D4CFF)
val CosmicSuccessGreen = Color(0xFF00D68F)
val CosmicAlertRed = Color(0xFFFF5A5A)

private val CosmicThemeColorScheme = darkColorScheme(
    primary = CosmicElectricBlue,
    secondary = CosmicPurpleAccent,
    tertiary = CosmicRoyalGold,
    background = CosmicDeepNavy,
    surface = CosmicMidnightSlate,
    error = CosmicAlertRed,
    onPrimary = CosmicDeepNavy,
    onSecondary = CosmicDeepNavy,
    onTertiary = CosmicDeepNavy,
    onBackground = CosmicTextSilver,
    onSurface = CosmicTextSilver,
    surfaceVariant = Color(0xFF162235),
    onSurfaceVariant = CosmicTextSilver,
    outline = Color(0xFF1E2D4A)
)

val CosmicTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
)

@Composable
fun CosmicTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CosmicDeepNavy.toArgb()
            window.navigationBarColor = CosmicMidnightSlate.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = CosmicThemeColorScheme,
        typography = CosmicTypography,
        content = content
    )
}
