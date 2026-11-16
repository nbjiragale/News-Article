package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Role mapping, so the rule in Color.kt survives contact with Material:
//
//   primary   → Indigo.  Actions, navigation, and idiomatic phrases.
//   tertiary  → Rubric.  Meaning, and nothing else. A looked-up word turns red;
//                        the gloss that opens for it is red. Buttons never are.
//   secondary → ink-on-recessed-paper. Quiet chips that should not compete.
//
// Every role is set explicitly. Any role left out falls back to Material's
// purple-grey baseline, which is how the dark scheme used to go wrong.

private val LightColorScheme = lightColorScheme(
    primary                 = Indigo,
    onPrimary               = PaperRaised,
    primaryContainer        = IndigoTint,
    onPrimaryContainer      = OnIndigoTint,

    secondary               = InkMuted,
    onSecondary             = PaperRaised,
    secondaryContainer      = PaperRecess,
    onSecondaryContainer    = Ink,

    tertiary                = Rubric,
    onTertiary              = PaperRaised,
    tertiaryContainer       = RubricTint,
    onTertiaryContainer     = OnRubricTint,

    background              = Paper,
    onBackground            = Ink,
    surface                 = Paper,
    onSurface               = Ink,
    surfaceVariant          = PaperRecess,
    onSurfaceVariant        = InkMuted,

    surfaceContainerLowest  = PaperRaised,
    surfaceContainerLow     = Paper,
    surfaceContainer        = PaperRecess,
    surfaceContainerHigh    = PaperRecessHigh,
    surfaceContainerHighest = Color(0xFFDBDBD4),

    inverseSurface          = Ink,
    inverseOnSurface        = Paper,
    inversePrimary          = IndigoDark,

    outline                 = RuleStrong,
    outlineVariant          = Rule,

    error                   = ErrorInk,
    onError                 = PaperRaised,
    errorContainer          = ErrorTint,
    onErrorContainer        = OnErrorTint,

    scrim                   = Scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary                 = IndigoDark,
    onPrimary               = Color(0xFF14203F),
    primaryContainer        = IndigoTintDark,
    onPrimaryContainer      = OnIndigoTintDark,

    secondary               = InkMutedDark,
    onSecondary             = InkSurfaceLow,
    secondaryContainer      = InkSurfaceHigh,
    onSecondaryContainer    = PaperOnDark,

    tertiary                = RubricDark,
    onTertiary              = Color(0xFF3B0D0A),
    tertiaryContainer       = RubricTintDark,
    onTertiaryContainer     = OnRubricTintDark,

    background              = InkSurface,
    onBackground            = PaperOnDark,
    surface                 = InkSurface,
    onSurface               = PaperOnDark,
    surfaceVariant          = InkSurfaceHigh,
    onSurfaceVariant        = InkMutedDark,

    surfaceContainerLowest  = InkSurfaceLow,
    surfaceContainerLow     = InkSurfaceRaise,
    surfaceContainer        = InkSurfaceHigh,
    surfaceContainerHigh    = InkSurfaceTop,
    surfaceContainerHighest = Color(0xFF3A4150),

    inverseSurface          = PaperOnDark,
    inverseOnSurface        = InkSurface,
    inversePrimary          = Indigo,

    outline                 = Color(0xFF5B6273),
    outlineVariant          = RuleDark,

    error                   = ErrorInkDark,
    onError                 = Color(0xFF4A0A0C),
    errorContainer          = ErrorTintDark,
    onErrorContainer        = OnErrorTintDark,

    scrim                   = Color.Black,
)

/** Colours Material has no role for. Read them through [AppTheme.extendedColors]. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

private val LightExtendedColors = ExtendedColors(
    success = SuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight
)

private val DarkExtendedColors = ExtendedColors(
    success = SuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object AppTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun EnglishArticleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
