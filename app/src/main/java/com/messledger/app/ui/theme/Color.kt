package com.messledger.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The v1.5.1 palette — "Sunlit Scrapbook" by day, "The Night Ledger" by night.
 *
 * Light is warm analog paper with serif ink and pencil rules. Dark is a
 * near-black canvas with orange accents instead of the old beige-brown ink.
 * The canonical role names introduced for the v2 feature work are retained so
 * the date picker, range picker, update sheet and day-rules dialog keep
 * compiling.
 */

// ── LIGHT — Sunlit Scrapbook ───────────────────────────────────────────────
val PageLight = Color(0xFFF4F1EA)
val SurfaceLight = Color(0xFFFAF8F2)
val ElevatedLight = Color(0xFFFAF8F2)
val BorderLight = Color(0xFFDDD5C2)
val BorderStrongLight = Color(0xFFC0B797)
val TextPrimaryLight = Color(0xFF212121)
val TextSecondaryLight = Color(0xFF4A4843)
val TextTertiaryLight = Color(0xFF8A867E)
val TangerineLight = Color(0xFFC96A24)
val LeafLight = Color(0xFF5A7A2A)
val BrassLight = Color(0xFFC8A46B)
val WarningLight = Color(0xFFB3402E)
val SuccessLight = Color(0xFF5A7A2A)
val BarkLight = Color(0xFF6B4A2F)

// ── NIGHT — near-black with orange ink ─────────────────────────────────────
val PageDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF0E0B09)
val ElevatedDark = Color(0xFF18120E)
val BorderDark = Color(0xFF2B221C)
val BorderStrongDark = Color(0xFF423228)
val TextPrimaryDark = Color(0xFFF7F1E8)
val TextSecondaryDark = Color(0xFFD8CFC4)
val TextTertiaryDark = Color(0xFF9C9185)
val TangerineDark = Color(0xFFFF8A3D)
val LeafDark = Color(0xFFA8C080)
val BrassDark = Color(0xFFEFA963)
val WarningDark = Color(0xFFFF7043)
val SuccessDark = Color(0xFF93B56D)
val BarkDark = Color(0xFF8A6A50)

// Strict AMOLED is the default night canvas again; this opt-in is retained for
// callers that still pass `amoled = true`.
val PageAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF070504)

val OnTangerineLight = Color(0xFFFFFDF7)
val OnTangerineDark = Color(0xFF000000)

// ── Original names, preserved for the restored journal components ─────────
val Paper = PageLight
val CardLight = SurfaceLight
val Ink = TextPrimaryLight
val InkSoft = TextSecondaryLight
val Pencil = TextTertiaryLight
val Rule = BorderLight
val RuleDk = BorderStrongLight
val StampRed = WarningLight
val Bark = BarkLight
val Leaf = LeafLight
val Fruit = TangerineLight

val NightCanvasTop = PageDark
val NightCanvasBottom = PageDark
val VoidColor = PageDark
val CardDark = SurfaceDark
val NightSurfaceVariant = ElevatedDark
val TextLight = TextPrimaryDark
val TextSoft = TextSecondaryDark
val TextDim = TextTertiaryDark
val EtchedRule = BorderDark
val EtchedRuleStrong = BorderStrongDark
val AstrolabeRing = BorderStrongDark
val Ember = WarningDark
val Brass = BrassDark
val Sage = SuccessDark
val NightBark = BarkDark
val NightLeaf = LeafDark
val NightFruit = TangerineDark
