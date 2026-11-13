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

private val LightColorScheme = lightColorScheme(
    // ── Primary (emerald) ──────────────────────────────────────
    primary                = Emerald700,
    onPrimary              = SurfaceWhite,
    primaryContainer       = Emerald50,
    onPrimaryContainer     = Emerald800,

    // ── Secondary (soft emerald tint — cards, chips, toggles) ─
    secondary              = Emerald600,
    onSecondary            = SurfaceWhite,
    secondaryContainer     = SecondaryContainer,
    onSecondaryContainer   = OnSecondaryContainer,

    // ── Tertiary (plum — idiomatic phrase highlights) ──────────
    tertiary               = Plum700,
    onTertiary             = SurfaceWhite,
    tertiaryContainer      = Plum100,
    onTertiaryContainer    = Plum700,

    // ── Background / Surface ───────────────────────────────────
    background             = SurfaceWhite,
    onBackground           = Stone900,
    surface                = SurfaceWhite,
    onSurface              = Stone900,
    surfaceVariant         = SurfaceContainerLow,
    onSurfaceVariant       = Stone400,

    // ── Surface container tiers ────────────────────────────────
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceContainerHigh,
    surfaceContainerHighest = Stone100,

    // ── Inverse ───────────────────────────────────────────────
    inverseSurface         = Stone900,
    inverseOnSurface       = Stone50,
    inversePrimary         = Emerald200,

    // ── Outline ───────────────────────────────────────────────
    outline                = Outline,
    outlineVariant         = OutlineVariant,

    // ── Error ─────────────────────────────────────────────────
    error                  = ErrorRed,
    onError                = OnError,
    errorContainer         = ErrorContainer,
    onErrorContainer       = OnErrorContainer,

    // ── Scrim ─────────────────────────────────────────────────
    scrim                  = Scrim,
)

// Dark scheme: the same emerald logic on emerald-tinted near-black surfaces. Every role is
// set explicitly; any role left out falls back to Material's purple-grey baseline, which is
// what the app used to show in dark mode.
private val DarkColorScheme = darkColorScheme(
    primary                = Emerald400,
    onPrimary              = Emerald900,
    primaryContainer       = Emerald800,
    onPrimaryContainer     = Emerald50,

    secondary              = Emerald200,
    onSecondary            = Emerald900,
    secondaryContainer     = Color(0xFF233A30),
    onSecondaryContainer   = Emerald100,

    tertiary               = Plum200,
    onTertiary             = Stone950,
    tertiaryContainer      = Color(0xFF4E3858),
    onTertiaryContainer    = Plum100,

    background             = DarkSurface,
    onBackground           = Stone50,
    surface                = DarkSurface,
    onSurface              = Stone50,
    surfaceVariant         = DarkSurfaceContainerHigh,
    onSurfaceVariant       = Stone200,

    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow    = DarkSurfaceContainerLow,
    surfaceContainer       = DarkSurfaceContainer,
    surfaceContainerHigh   = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,

    inverseSurface         = Stone50,
    inverseOnSurface       = Stone900,
    inversePrimary         = Emerald700,

    outline                = Stone400,
    outlineVariant         = Color(0xFF2E3833),

    error                  = ErrorDark,
    onError                = OnErrorDark,
    errorContainer         = ErrorContainerDark,
    onErrorContainer       = OnErrorContainerDark,

    scrim                  = Color.Black,
)

/** Colors Material's scheme has no role for. Read them through [AppTheme.extendedColors]. */
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
            typography   = AppTypography,
            shapes       = AppShapes,
            content      = content
        )
    }
}
