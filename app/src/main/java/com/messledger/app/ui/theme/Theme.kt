package com.messledger.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class MessColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val rule: Color,
    val ruleDark: Color,
    val accentRed: Color,
    // Specialized
    val bark: Color = Bark,
    val leaf: Color = Leaf,
    val fruit: Color = Fruit,
    val astrolabeRing: Color = AstrolabeRing
)

val LocalMessColors = staticCompositionLocalOf {
    MessColors(
        background = Color.Unspecified,
        surface = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textTertiary = Color.Unspecified,
        rule = Color.Unspecified,
        ruleDark = Color.Unspecified,
        accentRed = Color.Unspecified
    )
}

private val LightMessColors = MessColors(
    background = Paper,
    surface = CardLight,
    textPrimary = Ink,
    textSecondary = InkSoft,
    textTertiary = Pencil,
    rule = Rule,
    ruleDark = RuleDk,
    accentRed = StampRed
)

private val DarkMessColors = MessColors(
    background = VoidColor,
    surface = CardDark,
    textPrimary = TextLight,
    textSecondary = TextLight,
    textTertiary = TextDim,
    rule = EtchedRule,
    ruleDark = EtchedRule,
    accentRed = NeonEmber
)

private val DarkColorScheme = darkColorScheme(
    primary = TextLight,
    background = VoidColor,
    surface = CardDark,
    onPrimary = VoidColor,
    onBackground = TextLight,
    onSurface = TextLight,
    error = NeonEmber
)

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    background = Paper,
    surface = CardLight,
    onPrimary = Paper,
    onBackground = Ink,
    onSurface = Ink,
    error = StampRed
)

@Composable
fun FoodJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) DarkMessColors else LightMessColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.value.toInt()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMessColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
