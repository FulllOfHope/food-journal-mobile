package com.messledger.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Semantic colours for the journal. The short legacy names stay as read-only
 * aliases, and the role names added for the newer dialogs/calendar remain, but
 * every value comes from the v1.5.1 paper/ink palette.
 */
@Immutable
data class MessColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val tangerine: Color,
    val onTangerine: Color,
    val leaf: Color,
    val brass: Color,
    val warning: Color,
    val success: Color,
    val bark: Color,
    val astrolabeRing: Color = borderStrong
) {
    val rule: Color get() = border
    val ruleDark: Color get() = borderStrong
    val accentRed: Color get() = warning
    val missed: Color get() = warning
    val eaten: Color get() = success
    val fruit: Color get() = tangerine
}

/** Resolved after the saved preference is applied, so overrides never leave half the app behind. */
val LocalIsNight = staticCompositionLocalOf { false }

val LightMessColors = MessColors(
    background = Paper,
    surface = CardLight,
    elevated = CardLight,
    border = Rule,
    borderStrong = RuleDk,
    textPrimary = Ink,
    textSecondary = InkSoft,
    textTertiary = Pencil,
    tangerine = Fruit,
    onTangerine = OnTangerineLight,
    leaf = LeafLight,
    brass = Brass,
    warning = StampRed,
    success = SuccessLight,
    bark = BarkLight,
    astrolabeRing = RuleDk
)

val DarkMessColors = MessColors(
    background = VoidColor,
    surface = CardDark,
    elevated = NightSurfaceVariant,
    border = EtchedRule,
    borderStrong = EtchedRuleStrong,
    textPrimary = TextLight,
    textSecondary = TextSoft,
    textTertiary = TextDim,
    tangerine = TangerineDark,
    onTangerine = OnTangerineDark,
    leaf = NightLeaf,
    brass = Brass,
    warning = Ember,
    success = Sage,
    bark = NightBark,
    astrolabeRing = EtchedRuleStrong
)

/** Kept for callers that explicitly opt into the strict AMOLED surfaces. */
val AmoledMessColors = DarkMessColors.copy(
    background = PageAmoled,
    surface = SurfaceAmoled,
    elevated = SurfaceAmoled
)

val LocalMessColors = staticCompositionLocalOf { LightMessColors }

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Brass,
    onSecondary = Paper,
    tertiary = StampRed,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = CardLight,
    onSurfaceVariant = InkSoft,
    outline = RuleDk,
    outlineVariant = Rule,
    error = StampRed,
    onError = Paper
)

private val DarkColorScheme = darkColorScheme(
    primary = TextLight,
    onPrimary = VoidColor,
    secondary = Brass,
    onSecondary = VoidColor,
    tertiary = Ember,
    onTertiary = VoidColor,
    background = VoidColor,
    onBackground = TextLight,
    surface = CardDark,
    onSurface = TextLight,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = TextSoft,
    outline = EtchedRuleStrong,
    outlineVariant = EtchedRule,
    error = Ember,
    onError = VoidColor
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = PageAmoled,
    surface = SurfaceAmoled
)

@Composable
fun FoodJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme && amoled -> AmoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val customColors = when {
        darkTheme && amoled -> AmoledMessColors
        darkTheme -> DarkMessColors
        else -> LightMessColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = customColors.background.toArgb()
            window.navigationBarColor = customColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalMessColors provides customColors,
        LocalIsNight provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
