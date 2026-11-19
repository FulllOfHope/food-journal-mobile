package com.messledger.app.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SunlitScrapbookColors = lightColorScheme(
    primary = StampRed,
    background = PaperCream,
    surface = CardBgLight,
    surfaceVariant = EatenCardBg,
    onPrimary = PaperCream,
    onBackground = InkBlack,
    onSurface = InkBlack,
    outline = LedgerRule,
    outlineVariant = PencilFaint
)

private val AstrolabeColors = darkColorScheme(
    primary = NeonEmber,
    background = VoidCharcoal,
    surface = DeepSpaceCard,
    surfaceVariant = NebulaGlow,
    onPrimary = VoidCharcoal,
    onBackground = StarlightWhite,
    onSurface = StarlightWhite,
    outline = EtchedGlass,
    outlineVariant = EtchedGlass
)

@Composable
fun FoodJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AstrolabeColors else SunlitScrapbookColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (darkTheme) AstrolabeTypography else ScrapbookTypography,
        content = content
    )
}
