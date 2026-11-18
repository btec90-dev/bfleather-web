package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton / Manager responsible for theme state management.
 * Defaults to CARAMEL as requested.
 */
object ThemeManager {
  private val _currentPalette = MutableStateFlow(AppThemePalette.CARAMEL)
  val currentPalette: StateFlow<AppThemePalette> = _currentPalette.asStateFlow()

  val activeThemeColors: ThemeColors
    get() = getThemeColors(_currentPalette.value)

  fun setPalette(palette: AppThemePalette) {
    _currentPalette.value = palette
  }
}

val LocalThemeColors = staticCompositionLocalOf {
  getThemeColors(AppThemePalette.CARAMEL)
}

val LocalAppThemePalette = staticCompositionLocalOf {
  AppThemePalette.CARAMEL
}

@Composable
fun AppThemeProvider(
  themeManager: ThemeManager = ThemeManager,
  content: @Composable () -> Unit
) {
  val selectedPalette by themeManager.currentPalette.collectAsState()
  val themeColors = remember(selectedPalette) { getThemeColors(selectedPalette) }

  val dynamicColorScheme = remember(themeColors) {
    lightColorScheme(
      primary = themeColors.primaryText,
      secondary = themeColors.mutedText,
      tertiary = themeColors.accent,
      background = themeColors.background,
      surface = themeColors.cardSurface,
      surfaceVariant = themeColors.border,
      outline = themeColors.border,
      outlineVariant = themeColors.border.copy(alpha = 0.6f),
      onPrimary = Color.White,
      onSecondary = Color.White,
      onTertiary = Color.White,
      onBackground = themeColors.primaryText,
      onSurface = themeColors.primaryText,
      onSurfaceVariant = themeColors.mutedText
    )
  }

  CompositionLocalProvider(
    LocalThemeColors provides themeColors,
    LocalAppThemePalette provides selectedPalette
  ) {
    MaterialTheme(
      colorScheme = dynamicColorScheme,
      typography = Typography,
      content = content
    )
  }
}

object AppTheme {
  val colors: ThemeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeColors.current

  val palette: AppThemePalette
    @Composable
    @ReadOnlyComposable
    get() = LocalAppThemePalette.current
}
