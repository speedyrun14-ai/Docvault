package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DocuBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DocuBlueDark,
    onPrimaryContainer = DocuCyanSecondary,
    secondary = DocuCyanSecondary,
    onSecondary = DocuNavyDark,
    background = DocuNavyDark,
    onBackground = DocuTextPrimaryDark,
    surface = DocuCardDark,
    onSurface = DocuTextPrimaryDark,
    surfaceVariant = DocuSecondaryBgDark,
    onSurfaceVariant = DocuTextSecondaryDark,
    outline = DocuBorderDark,
    outlineVariant = DocuBorderDark.copy(alpha = 0.5f),
    error = DocuError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DocuBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = DocuBlueDark,
    secondary = DocuCyanSecondary,
    onSecondary = Color.Black,
    background = DocuBgLight,
    onBackground = DocuTextPrimaryLight,
    surface = DocuCardLight,
    onSurface = DocuTextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = DocuTextSecondaryLight,
    outline = DocuBorderLight,
    outlineVariant = DocuBorderLight.copy(alpha = 0.5f),
    error = DocuError,
    onError = Color.White
)

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

@Composable
fun DocuVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun DocuVaultTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }
    DocuVaultTheme(darkTheme = isDark, content = content)
}
