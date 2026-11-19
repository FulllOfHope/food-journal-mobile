package com.messledger.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tangerine Grove at Night — the one canonical palette.
 *
 * Light is warm paper; the default night is a warm near-black, not pure black.
 * A strict AMOLED variant exists as an explicit opt-in and is never applied
 * automatically. No gradients, no glow: surfaces are flat and roles are
 * semantic so a component never has to ask which mode it is in.
 */

// ── LIGHT ────────────────────────────────────────────────────────────────
val PageLight = Color(0xFFF4F1EA)
val SurfaceLight = Color(0xFFFAF8F2)
val ElevatedLight = Color(0xFFFFFDF7)
val BorderLight = Color(0xFFDDD5C2)
val BorderStrongLight = Color(0xFFC0B797)
val TextPrimaryLight = Color(0xFF212121)
val TextSecondaryLight = Color(0xFF4A4843)
val TextTertiaryLight = Color(0xFF8A867E)
val TangerineLight = Color(0xFFC96A24)
val LeafLight = Color(0xFF5A7A2A)
val BrassLight = Color(0xFF8A6A2F) // money only
val WarningLight = Color(0xFFB3402E)
val SuccessLight = Color(0xFF5A7A2A)
val BarkLight = Color(0xFF6B4A2F)

// ── NIGHT (default dark) ─────────────────────────────────────────────────
val PageDark = Color(0xFF0F0D0B)
val SurfaceDark = Color(0xFF171411)
val ElevatedDark = Color(0xFF1D1915)
val BorderDark = Color(0xFF2E2823)
val BorderStrongDark = Color(0xFF453A31)
val TextPrimaryDark = Color(0xFFEFE7D9)
val TextSecondaryDark = Color(0xFFBCB0A0)
val TextTertiaryDark = Color(0xFF8A7D6D)
val TangerineDark = Color(0xFFE08A3C)
val LeafDark = Color(0xFF9DAF7C)
val BrassDark = Color(0xFFC8A46B) // money only
val WarningDark = Color(0xFFE2785E)
val SuccessDark = Color(0xFFA9BE86)
val BarkDark = Color(0xFF6F5A46)

// ── STRICT AMOLED (opt-in only) ──────────────────────────────────────────
val PageAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF14110E)

// Text painted on top of a tangerine fill.
val OnTangerineLight = Color(0xFFFFFDF7)
val OnTangerineDark = Color(0xFF171411)

@Deprecated("Use PageLight.", ReplaceWith("PageLight"))
val Paper = PageLight
@Deprecated("Use SurfaceLight.", ReplaceWith("SurfaceLight"))
val CardLight = SurfaceLight
@Deprecated("Use TextPrimaryLight.", ReplaceWith("TextPrimaryLight"))
val Ink = TextPrimaryLight
@Deprecated("Use TextSecondaryLight.", ReplaceWith("TextSecondaryLight"))
val InkSoft = TextSecondaryLight
@Deprecated("Use TextTertiaryLight.", ReplaceWith("TextTertiaryLight"))
val Pencil = TextTertiaryLight
@Deprecated("Use BorderLight.", ReplaceWith("BorderLight"))
val Rule = BorderLight
@Deprecated("Use BorderStrongLight.", ReplaceWith("BorderStrongLight"))
val RuleDk = BorderStrongLight
@Deprecated("Use WarningLight.", ReplaceWith("WarningLight"))
val StampRed = WarningLight
@Deprecated("Use BarkLight.", ReplaceWith("BarkLight"))
val Bark = BarkLight
@Deprecated("Use LeafLight.", ReplaceWith("LeafLight"))
val Leaf = LeafLight
@Deprecated("Use TangerineLight.", ReplaceWith("TangerineLight"))
val Fruit = TangerineLight
@Deprecated("Use PageDark.", ReplaceWith("PageDark"))
val VoidColor = PageDark
@Deprecated("Use SurfaceDark.", ReplaceWith("SurfaceDark"))
val CardDark = SurfaceDark
@Deprecated("Use ElevatedDark.", ReplaceWith("ElevatedDark"))
val NightSurfaceVariant = ElevatedDark
@Deprecated("Use TextPrimaryDark.", ReplaceWith("TextPrimaryDark"))
val TextLight = TextPrimaryDark
@Deprecated("Use TextSecondaryDark.", ReplaceWith("TextSecondaryDark"))
val TextSoft = TextSecondaryDark
@Deprecated("Use TextTertiaryDark.", ReplaceWith("TextTertiaryDark"))
val TextDim = TextTertiaryDark
@Deprecated("Use BorderDark.", ReplaceWith("BorderDark"))
val EtchedRule = BorderDark
@Deprecated("Use BorderStrongDark.", ReplaceWith("BorderStrongDark"))
val EtchedRuleStrong = BorderStrongDark
@Deprecated("Removed with the astrolabe hero.", ReplaceWith("BorderStrongDark"))
val AstrolabeRing = BorderStrongDark
@Deprecated("Use WarningDark.", ReplaceWith("WarningDark"))
val Ember = WarningDark
@Deprecated("Use BrassDark.", ReplaceWith("BrassDark"))
val Brass = BrassDark
@Deprecated("Use PageDark.", ReplaceWith("PageDark"))
val NightCanvasTop = PageDark
@Deprecated("Use PageDark.", ReplaceWith("PageDark"))
val NightCanvasBottom = PageDark
@Deprecated("Use SuccessDark.", ReplaceWith("SuccessDark"))
val Sage = SuccessDark
@Deprecated("Use BarkDark.", ReplaceWith("BarkDark"))
val NightBark = BarkDark
@Deprecated("Use LeafDark.", ReplaceWith("LeafDark"))
val NightLeaf = LeafDark
@Deprecated("Use TangerineDark.", ReplaceWith("TangerineDark"))
val NightFruit = TangerineDark
