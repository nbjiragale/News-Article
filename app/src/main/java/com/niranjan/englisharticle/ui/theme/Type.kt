package com.niranjan.englisharticle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.niranjan.englisharticle.R

// ── Two voices ─────────────────────────────────────────────────
// Content is serif; chrome is sans. An article title belongs to the article, so it
// is set in the same face as the prose underneath it. Buttons, labels and bars are
// the app talking, and they stay in the sans.
//
// Only Normal and Bold are declared for the sans: those are the two cuts actually
// bundled in res/font. Asking for Medium or SemiBold would silently snap to one of
// them anyway, so the scale earns its hierarchy from size and spacing instead.

/** Chrome. Atkinson Hyperlegible — drawn for low-vision readers, unusually legible small. */
val AppFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

/** Content. The platform serif — the reading voice, and the voice of article titles. */
val ArticleReadingFontFamily = FontFamily.Serif

/**
 * Article prose. Long-form reading on a phone, by learners who also tap individual
 * words, so the line is generous and the words are large enough to hit accurately.
 */
val ArticleBodyTextStyle = TextStyle(
    fontFamily = ArticleReadingFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 19.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.sp
)

private fun content(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Bold,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = ArticleReadingFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

private fun chrome(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

val AppTypography = Typography(
    // ── Content voice (serif) ──────────────────────────────────
    displayLarge  = content(40, 46, tracking = -0.6),
    displayMedium = content(33, 40, tracking = -0.4),
    displaySmall  = content(27, 34, tracking = -0.2),

    // Article titles. headlineMedium is the fallback for long headlines.
    headlineLarge  = content(30, 38, tracking = -0.3),
    headlineMedium = content(25, 32, tracking = -0.2),
    headlineSmall  = content(21, 28, tracking = -0.1),

    // ── Chrome voice (sans) ────────────────────────────────────
    titleLarge  = chrome(19, 25, FontWeight.Bold),
    titleMedium = chrome(16, 22, FontWeight.Bold),
    titleSmall  = chrome(14, 20, FontWeight.Bold),

    bodyLarge  = chrome(16, 26),
    bodyMedium = chrome(15, 23),
    bodySmall  = chrome(13, 19),

    // Sentence case throughout — no tracked-out capitals.
    labelLarge  = chrome(14, 18, FontWeight.Bold, 0.1),
    labelMedium = chrome(13, 17, FontWeight.Normal, 0.15),
    labelSmall  = chrome(11, 15, FontWeight.Normal, 0.25),
)
