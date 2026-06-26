package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberEmerald,
    secondary = CyberCyan,
    tertiary = CyberGray,
    background = CyberBlack,
    surface = CyberCard,
    onPrimary = CyberBlack,
    onSecondary = CyberBlack,
    onBackground = CyberWhite,
    onSurface = CyberWhite
  )

private val LightColorScheme = DarkColorScheme // Stealth app stays dark-theme only for cyber aesthetics

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for stealth cybersecurity vibe
  dynamicColor: Boolean = false, // Use our brand colors for maximum punch
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
