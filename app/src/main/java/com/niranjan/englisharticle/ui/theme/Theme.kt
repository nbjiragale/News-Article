package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandIndigo600,
    onPrimary = AppOnPrimary,
    primaryContainer = BrandIndigo100,
    onPrimaryContainer = BrandIndigo900,
    secondary = BrandPlum500,
    onSecondary = AppOnPrimary,
    secondaryContainer = BrandPlum100,
    onSecondaryContainer = BrandPlum700,
    tertiary = BrandCoral500,
    onTertiary = AppOnPrimary,
    tertiaryContainer = BrandCoral100,
    onTertiaryContainer = BrandCoral700,
    background = Cream50,
    onBackground = Ink900,
    surface = Cream50,
    onSurface = Ink900,
    surfaceVariant = Cream200,
    onSurfaceVariant = Ink500,
    surfaceContainerLowest = AppSurfaceContainerLowest,
    surfaceContainerLow = Cream100,
    surfaceContainer = Cream200,
    surfaceContainerHigh = Cream300,
    surfaceContainerHighest = Cream300,
    outline = Ink400,
    outlineVariant = Ink200,
    error = DangerRed500,
    onError = AppOnPrimary,
    errorContainer = DangerRed100,
    onErrorContainer = AppOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandIndigo300,
    onPrimary = BrandIndigo900,
    primaryContainer = BrandIndigo700,
    onPrimaryContainer = BrandIndigo100,
    secondary = BrandPlum300,
    onSecondary = BrandPlum700,
    secondaryContainer = BrandPlum700,
    onSecondaryContainer = BrandPlum100,
    tertiary = BrandCoral300,
    onTertiary = BrandCoral700,
    tertiaryContainer = BrandCoral700,
    onTertiaryContainer = BrandCoral100,
    background = Ink900,
    onBackground = AppDarkText,
    surface = Ink900,
    onSurface = AppDarkText,
    surfaceVariant = Ink700,
    onSurfaceVariant = Ink300,
    surfaceContainerLowest = Ink900,
    surfaceContainerLow = Ink800,
    surfaceContainer = Ink800,
    surfaceContainerHigh = Ink700,
    surfaceContainerHighest = Ink600,
    outline = Ink400,
    outlineVariant = Ink600,
    error = DangerRed500,
    onError = AppOnPrimary,
    errorContainer = androidx.compose.ui.graphics.Color(0xFF7B0000),
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
