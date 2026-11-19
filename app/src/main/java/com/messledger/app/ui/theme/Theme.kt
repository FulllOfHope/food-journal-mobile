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
 * Every semantic colour a component needs. The short legacy names are kept as
 * read-only aliases so older call sites keep compiling, but new code should use
 * the semantic roles (border, tangerine, warning, …).
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
    val bark: Color
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
    background = PageLight,
    surface = SurfaceLight,
    elevated = ElevatedLight,
    border = BorderLight,
    borderStrong = BorderStrongLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    tangerine = TangerineLight,
    onTangerine = OnTangerineLight,
    leaf = LeafLight,
    brass = BrassLight,
    warning = WarningLight,
    success = SuccessLight,
    bark = BarkLight
)

val DarkMessColors = MessColors(
    background = PageDark,
    surface = SurfaceDark,
    elevated = ElevatedDark,
    border = BorderDark,
    borderStrong = BorderStrongDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    tangerine = TangerineDark,
    onTangerine = OnTangerineDark,
    leaf = LeafDark,
    brass = BrassDark,
    warning = WarningDark,
    success = SuccessDark,
    bark = BarkDark
)

/** Strict AMOLED: only when [FoodJournalTheme] is explicitly asked for it. */
val AmoledMessColors = DarkMessColors.copy(
    background = PageAmoled,
    surface = SurfaceAmoled,
    elevated = SurfaceAmoled
)

val LocalMessColors = staticCompositionLocalOf { LightMessColors }

private val LightColorScheme = lightColorScheme(
    primary = TangerineLight,
    onPrimary = OnTangerineLight,
    secondary = BrassLight,
    onSecondary = SurfaceLight,
    tertiary = LeafLight,
    onTertiary = SurfaceLight,
    background = PageLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderStrongLight,
    error = WarningLight,
    onError = SurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TangerineDark,
    onPrimary = PageDark,
    secondary = BrassDark,
    onSecondary = PageDark,
    tertiary = LeafDark,
    onTertiary = PageDark,
    background = PageDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderStrongDark,
    error = WarningDark,
    onError = PageDark
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = PageAmoled,
    surface = SurfaceAmoled
)

/**
 * @param amoled opt-in strict AMOLED canvas (#000000 / #14110E). The default
 * dark canvas stays the warmer #0F0D0B / #171411 pair.
 */
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
