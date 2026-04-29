package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Sage500,
    onPrimary = Color.White,
    primaryContainer = Sage100,
    onPrimaryContainer = Sage700,
    secondary = Sepia500,
    onSecondary = Color.White,
    secondaryContainer = Sepia100,
    onSecondaryContainer = Sepia700,
    tertiary = Plum500,
    onTertiary = Color.White,
    tertiaryContainer = Plum100,
    onTertiaryContainer = Plum700,
    background = Paper50,
    onBackground = Charcoal900,
    surface = Paper50,
    onSurface = Charcoal900,
    surfaceVariant = Paper200,
    onSurfaceVariant = Charcoal500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Paper100,
    surfaceContainer = Paper200,
    surfaceContainerHigh = Paper300,
    surfaceContainerHighest = Paper400,
    outline = Charcoal300,
    outlineVariant = Charcoal200,
    error = DangerRed500,
    onError = Color.White,
    errorContainer = DangerRed100,
    onErrorContainer = Color(0xFF5A1610)
)

private val DarkColorScheme = darkColorScheme(
    primary = Sage300,
    onPrimary = Sage900,
    primaryContainer = Sage700,
    onPrimaryContainer = Sage100,
    secondary = Sepia300,
    onSecondary = Sepia700,
    secondaryContainer = Sepia700,
    onSecondaryContainer = Sepia100,
    tertiary = Plum300,
    onTertiary = Plum700,
    tertiaryContainer = Plum700,
    onTertiaryContainer = Plum100,
    background = AppDarkBackground,
    onBackground = AppDarkText,
    surface = AppDarkBackground,
    onSurface = AppDarkText,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = Charcoal300,
    surfaceContainerLowest = AppDarkBackground,
    surfaceContainerLow = AppDarkSurface,
    surfaceContainer = AppDarkSurface,
    surfaceContainerHigh = AppDarkSurfaceVariant,
    surfaceContainerHighest = Charcoal700,
    outline = Charcoal400,
    outlineVariant = Charcoal700,
    error = DangerRed500,
    onError = Color.White,
    errorContainer = Color(0xFF6B221A),
    onErrorContainer = DangerRed100
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
        shapes = AppShapes,
        content = content
    )
}
