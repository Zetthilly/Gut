package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val CosmicColorScheme = darkColorScheme(
  primary = ElectricBlue,
  secondary = CosmicPurple,
  tertiary = RoyalGold,
  background = DeepNavy,
  surface = MidnightSlate,
  onPrimary = DeepNavy,
  onSecondary = TextSilver,
  onTertiary = DeepNavy,
  onBackground = TextSilver,
  onSurface = TextSilver,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark creative studio environment by default
  dynamicColor: Boolean = false, // Set to false to preserve our signature Cosmic Audio visual workspace branding
  content: @Composable () -> Unit,
) {
  val colorScheme = CosmicColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = DeepNavy.toArgb()
      window.navigationBarColor = MidnightSlate.toArgb()
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

