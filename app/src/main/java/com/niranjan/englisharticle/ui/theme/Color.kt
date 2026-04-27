package com.niranjan.englisharticle.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ────────────────────────────────────────────────────────────────────
// Primary: vibrant indigo. Tertiary: warm coral. Secondary: muted plum.
// Picked to feel editorial yet "expressive" — bigger, more confident than the
// previous near-navy palette while keeping a Kannada-friendly literary tone.

val BrandIndigo50 = Color(0xFFF1F2FF)
val BrandIndigo100 = Color(0xFFE0E2FF)
val BrandIndigo200 = Color(0xFFC2C6FF)
val BrandIndigo300 = Color(0xFF9CA1FF)
val BrandIndigo400 = Color(0xFF7479F5)
val BrandIndigo500 = Color(0xFF4F4DE0)
val BrandIndigo600 = Color(0xFF3C39C7)
val BrandIndigo700 = Color(0xFF2C2A99)
val BrandIndigo800 = Color(0xFF1F1D70)
val BrandIndigo900 = Color(0xFF15144F)

val BrandCoral50 = Color(0xFFFFF1ED)
val BrandCoral100 = Color(0xFFFFD9CC)
val BrandCoral300 = Color(0xFFFF9F84)
val BrandCoral500 = Color(0xFFE56A4C)
val BrandCoral700 = Color(0xFFA84229)

val BrandPlum100 = Color(0xFFEDDCEB)
val BrandPlum300 = Color(0xFFB48AB1)
val BrandPlum500 = Color(0xFF7E4F7C)
val BrandPlum700 = Color(0xFF4F2A4D)

// ── Neutrals ─────────────────────────────────────────────────────────────────
val Cream50 = Color(0xFFFBF8F2)
val Cream100 = Color(0xFFF5F1E8)
val Cream200 = Color(0xFFEDE8DD)
val Cream300 = Color(0xFFDFD9CB)

val Ink900 = Color(0xFF14141A)
val Ink800 = Color(0xFF1F1F28)
val Ink700 = Color(0xFF2A2A36)
val Ink600 = Color(0xFF3D3D4D)
val Ink500 = Color(0xFF55556B)
val Ink400 = Color(0xFF7A7A92)
val Ink300 = Color(0xFFAFAEC2)
val Ink200 = Color(0xFFD8D6E2)
val Ink100 = Color(0xFFEDECF3)

// ── Semantic ─────────────────────────────────────────────────────────────────
val SuccessGreen500 = Color(0xFF1F9D6F)
val SuccessGreen100 = Color(0xFFD3F1E2)
val WarnAmber500 = Color(0xFFD78B16)
val WarnAmber100 = Color(0xFFFCEAC2)
val DangerRed500 = Color(0xFFC83434)
val DangerRed100 = Color(0xFFFFD9D6)

// ── Legacy aliases (kept so existing direct references compile) ──────────────
val AppPrimary = BrandIndigo600
val AppOnPrimary = Color.White
val AppPrimaryContainer = BrandIndigo100
val AppOnPrimaryContainer = BrandIndigo900
val AppSecondary = BrandPlum500
val AppSecondaryContainer = BrandPlum100
val AppOnSecondaryContainer = BrandPlum700
val AppTertiary = BrandCoral500
val AppSurface = Cream50
val AppTopBar = Cream100
val AppSurfaceContainerLowest = Color.White
val AppSurfaceContainerLow = Cream100
val AppSurfaceContainer = Cream200
val AppSurfaceContainerHigh = Cream300
val AppSurfaceVariant = Cream200
val AppOnSurface = Ink900
val AppOnSurfaceVariant = Ink500
val AppOutline = Ink400
val AppOutlineVariant = Ink200
val AppError = DangerRed500
val AppErrorContainer = DangerRed100
val AppOnErrorContainer = Color(0xFF6B0F0F)

val AppDarkBackground = Ink900
val AppDarkSurface = Ink800
val AppDarkSurfaceVariant = Ink700
val AppDarkText = Color(0xFFF2F0F8)
val AppDarkMuted = Ink300
