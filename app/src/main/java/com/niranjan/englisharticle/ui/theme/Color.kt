package com.niranjan.englisharticle.ui.theme

import androidx.compose.ui.graphics.Color

// ── Reading paper palette ────────────────────────────────────────────────────
// Quiet, warm-leaning neutrals (inspired by editorial readers like Instapaper /
// Readwise) with a sage-green primary, sepia secondary, and dusty-plum
// tertiary accent. The whole scheme is tuned for long reading sessions:
// ample contrast on body text, low contrast on chrome, no harsh saturation.

// Paper neutrals (background → containers)
val Paper50 = Color(0xFFFBF8F3)
val Paper100 = Color(0xFFF6F1E7)
val Paper200 = Color(0xFFEFE9DB)
val Paper300 = Color(0xFFE8E1D0)
val Paper400 = Color(0xFFE1D9C6)

// Warm charcoal ink (text + strong chrome)
val Charcoal900 = Color(0xFF1F1D19)
val Charcoal800 = Color(0xFF26241F)
val Charcoal700 = Color(0xFF34302A)
val Charcoal600 = Color(0xFF4A443B)
val Charcoal500 = Color(0xFF6E665B)
val Charcoal400 = Color(0xFF8E8676)
val Charcoal300 = Color(0xFFA89F8E)
val Charcoal200 = Color(0xFFDBD3C3)
val Charcoal100 = Color(0xFFEDE7DB)

// Sage primary
val Sage100 = Color(0xFFD9E5D2)
val Sage300 = Color(0xFF9CC1A0)
val Sage500 = Color(0xFF4F6E58)
val Sage700 = Color(0xFF2F4737)
val Sage900 = Color(0xFF1A2C20)

// Sepia secondary
val Sepia100 = Color(0xFFEBDDC4)
val Sepia300 = Color(0xFFD0AE7A)
val Sepia500 = Color(0xFF8C6B45)
val Sepia700 = Color(0xFF5A4128)

// Plum tertiary
val Plum100 = Color(0xFFEBDCF2)
val Plum300 = Color(0xFFB99AC7)
val Plum500 = Color(0xFF7C5A88)
val Plum700 = Color(0xFF4D3457)

// Semantic
val SuccessGreen500 = Color(0xFF3F7A5C)
val SuccessGreen100 = Color(0xFFD9ECDF)
val WarnAmber500 = Color(0xFFB47B1F)
val WarnAmber100 = Color(0xFFF4E4C2)
val DangerRed500 = Color(0xFFB14C42)
val DangerRed100 = Color(0xFFF4DAD4)

// ── Backwards-compatible aliases ─────────────────────────────────────────────
// Existing screens reference these names directly. They now resolve to the
// new reading-paper tokens so the whole app picks up the redesign without
// touching every call site.

// Legacy "indigo" → sage primary
val BrandIndigo50 = Sage100
val BrandIndigo100 = Sage100
val BrandIndigo200 = Sage300
val BrandIndigo300 = Sage300
val BrandIndigo400 = Sage500
val BrandIndigo500 = Sage500
val BrandIndigo600 = Sage500
val BrandIndigo700 = Sage700
val BrandIndigo800 = Sage700
val BrandIndigo900 = Sage900

// Legacy "coral" → sepia secondary accent
val BrandCoral50 = Sepia100
val BrandCoral100 = Sepia100
val BrandCoral300 = Sepia300
val BrandCoral500 = Sepia500
val BrandCoral700 = Sepia700

// Legacy "plum" → plum tertiary
val BrandPlum100 = Plum100
val BrandPlum300 = Plum300
val BrandPlum500 = Plum500
val BrandPlum700 = Plum700

// Legacy cream neutrals → paper neutrals
val Cream50 = Paper50
val Cream100 = Paper100
val Cream200 = Paper200
val Cream300 = Paper300

// Legacy ink → warm charcoal
val Ink900 = Charcoal900
val Ink800 = Charcoal800
val Ink700 = Charcoal700
val Ink600 = Charcoal600
val Ink500 = Charcoal500
val Ink400 = Charcoal400
val Ink300 = Charcoal300
val Ink200 = Charcoal200
val Ink100 = Charcoal100

// Legacy semantic role aliases
val AppPrimary = Sage500
val AppOnPrimary = Color.White
val AppPrimaryContainer = Sage100
val AppOnPrimaryContainer = Sage700
val AppSecondary = Sepia500
val AppSecondaryContainer = Sepia100
val AppOnSecondaryContainer = Sepia700
val AppTertiary = Plum500
val AppSurface = Paper50
val AppTopBar = Paper50
val AppSurfaceContainerLowest = Color.White
val AppSurfaceContainerLow = Paper100
val AppSurfaceContainer = Paper200
val AppSurfaceContainerHigh = Paper300
val AppSurfaceVariant = Paper200
val AppOnSurface = Charcoal900
val AppOnSurfaceVariant = Charcoal500
val AppOutline = Charcoal300
val AppOutlineVariant = Charcoal200
val AppError = DangerRed500
val AppErrorContainer = DangerRed100
val AppOnErrorContainer = Color(0xFF5A1610)

val AppDarkBackground = Color(0xFF14130F)
val AppDarkSurface = Color(0xFF1A1814)
val AppDarkSurfaceVariant = Color(0xFF26241F)
val AppDarkText = Color(0xFFEDE7DB)
val AppDarkMuted = Charcoal300
