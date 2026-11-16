package com.niranjan.englisharticle.ui.theme

import androidx.compose.ui.graphics.Color

// ── ArthaReader — "Rubric" ─────────────────────────────────────
// The app is named for artha (ಅರ್ಥ, "meaning"), and what it does — explaining a
// text in the margin — is the old manuscript practice of rubrication: the body
// in ink, the gloss in red.
//
// So red is a rule here, not a decoration:
//   Rubric  — appears ONLY where meaning appears (a looked-up word, an open gloss).
//   Indigo  — structure and action, and idiomatic phrases, so a phrase reads
//             differently from a single word.
// Everything else is ink on paper. Nothing else gets a hue.

// ── Ink (text) ─────────────────────────────────────────────────
// A real colour rather than a tinted black: deep indigo, the colour of iron-gall ink.
val Ink        = Color(0xFF1C2230)
val InkMuted   = Color(0xFF5B6475)
val InkFaint   = Color(0xFF8A91A0)

// ── Paper (surfaces) ───────────────────────────────────────────
// Cool near-white, not parchment cream. Recessed tiers are where text goes IN
// (the paste area, the summary) rather than where cards sit on top.
val Paper           = Color(0xFFF7F7F4)
val PaperRaised     = Color(0xFFFFFFFF)
val PaperRecess     = Color(0xFFEDEDE8)
val PaperRecessHigh = Color(0xFFE4E4DE)

// ── Rules ──────────────────────────────────────────────────────
val Rule       = Color(0xFFDCDCD5)
val RuleStrong = Color(0xFFC3C3BA)

// ── Rubric (meaning) ───────────────────────────────────────────
val Rubric        = Color(0xFFA4161A)
val RubricTint    = Color(0xFFF7E4E2)
val OnRubricTint  = Color(0xFF6E0D10)

// ── Indigo (structure, action, idiomatic phrases) ──────────────
val Indigo        = Color(0xFF2B3A67)
val IndigoTint    = Color(0xFFE2E6F1)
val OnIndigoTint  = Color(0xFF1A2444)

// ── Success (a correct practice answer) ────────────────────────
val SuccessLight            = Color(0xFF2F6B4F)
val SuccessContainerLight   = Color(0xFFDCEDE3)
val OnSuccessContainerLight = Color(0xFF16402D)

// ── Error ──────────────────────────────────────────────────────
// Darker and browner than Rubric so a failure never reads as a gloss.
val ErrorInk         = Color(0xFF7E1416)
val ErrorTint        = Color(0xFFF3DEDC)
val OnErrorTint      = Color(0xFF4A0A0C)

val Scrim = Color(0xFF11141C)

// ── Dark ───────────────────────────────────────────────────────
// Night reading: the paper darkens, the ink lightens, the rules stay quiet.
val InkSurface      = Color(0xFF15171E)
val InkSurfaceLow   = Color(0xFF101219)
val InkSurfaceRaise = Color(0xFF1C1F28)
val InkSurfaceHigh  = Color(0xFF252935)
val InkSurfaceTop   = Color(0xFF2F3440)

val PaperOnDark   = Color(0xFFE9E8E2)
val InkMutedDark  = Color(0xFFA0A6B4)
val RuleDark      = Color(0xFF2E3340)

val RubricDark       = Color(0xFFEB7A6E)
val RubricTintDark   = Color(0xFF4C1512)
val OnRubricTintDark = Color(0xFFFFDAD4)

val IndigoDark       = Color(0xFF93A7D9)
val IndigoTintDark   = Color(0xFF243154)
val OnIndigoTintDark = Color(0xFFD7E0F4)

val SuccessDark            = Color(0xFF7FC9A4)
val SuccessContainerDark   = Color(0xFF1B4634)
val OnSuccessContainerDark = Color(0xFFCDE9DA)

val ErrorInkDark    = Color(0xFFFFB4A9)
val ErrorTintDark   = Color(0xFF5F1A17)
val OnErrorTintDark = Color(0xFFFFDAD4)
