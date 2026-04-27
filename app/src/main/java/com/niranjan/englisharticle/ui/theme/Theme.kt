package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppDarkText,
    onPrimary = AppOnSurface,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    secondary = AppDarkMuted,
    onSecondary = AppOnSurface,
    secondaryContainer = AppSecondaryContainer,
    onSecondaryContainer = AppOnSecondaryContainer,
    background = AppDarkBackground,
    onBackground = AppDarkText,
    surface = AppDarkSurface,
    onSurface = AppDarkText,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = AppDarkMuted,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
    error = AppError,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    secondary = AppSecondary,
    onSecondary = AppOnPrimary,
    secondaryContainer = AppSecondaryContainer,
    onSecondaryContainer = AppOnSecondaryContainer,
    tertiary = AppTertiary,
    background = AppSurface,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
    error = AppError,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppOnErrorContainer
)

@Composable
fun EnglishArticleTheme(
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
