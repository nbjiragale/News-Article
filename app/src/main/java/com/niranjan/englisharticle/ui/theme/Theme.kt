package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    surfaceContainerHighest = Stone50,

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

// Dark scheme keeps the same emerald logic on dark stone surfaces.
// Currently ArthaReader ships light-only; dark kept for system compat.
private val DarkColorScheme = darkColorScheme(
    primary                = Emerald400,
    onPrimary              = Emerald900,
    primaryContainer       = Emerald800,
    onPrimaryContainer     = Emerald50,

    secondary              = Emerald200,
    onSecondary            = Emerald900,
    secondaryContainer     = Emerald800,
    onSecondaryContainer   = Emerald100,

    tertiary               = Plum200,
    onTertiary             = Stone950,
    tertiaryContainer      = Plum700,
    onTertiaryContainer    = Plum100,

    background             = Stone950,
    onBackground           = Stone50,
    surface                = Stone950,
    onSurface              = Stone50,
    surfaceVariant         = Stone900,
    onSurfaceVariant       = Stone200,

    outline                = Stone500,
    outlineVariant         = Stone700,

    error                  = ErrorRed,
    onError                = OnError,
    errorContainer         = Color(0xFF5C0B0B),
    onErrorContainer       = ErrorContainer,
)

@Composable
fun EnglishArticleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = AppTypography,
        shapes       = AppShapes,
        content      = content
    )
}
