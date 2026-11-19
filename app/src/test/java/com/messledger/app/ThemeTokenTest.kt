package com.messledger.app

import androidx.compose.ui.graphics.Color
import com.messledger.app.ui.theme.AmoledMessColors
import com.messledger.app.ui.theme.DarkMessColors
import com.messledger.app.ui.theme.LightMessColors
import com.messledger.app.ui.theme.PageAmoled
import com.messledger.app.ui.theme.PageDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Practical, non-pixel-perfect contrast floors for the semantic palette. */
class ThemeTokenTest {

    private fun channel(value: Float): Double {
        val c = value.toDouble().coerceIn(0.0, 1.0)
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val first = luminance(a)
        val second = luminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    @Test
    fun textRolesStayLegibleOnBothSurfaces() {
        listOf("light" to LightMessColors, "dark" to DarkMessColors).forEach { (name, colors) ->
            assertTrue(
                "$name primary text on surface",
                contrast(colors.textPrimary, colors.surface) >= 4.5
            )
            assertTrue(
                "$name secondary text on surface",
                contrast(colors.textSecondary, colors.surface) >= 4.5
            )
            assertTrue(
                "$name tertiary text on surface",
                contrast(colors.textTertiary, colors.surface) >= 3.0
            )
            assertTrue(
                "$name primary text on elevated surface",
                contrast(colors.textPrimary, colors.elevated) >= 4.5
            )
        }
    }

    @Test
    fun actionAccentsMeetThreeToOneFloor() {
        listOf("light" to LightMessColors, "dark" to DarkMessColors).forEach { (name, colors) ->
            assertTrue(
                "$name tangerine against surface",
                contrast(colors.tangerine, colors.surface) >= 3.0
            )
            assertTrue(
                "$name on-tangerine label",
                contrast(colors.onTangerine, colors.tangerine) >= 3.0
            )
            assertTrue(
                "$name brass money figure",
                contrast(colors.brass, colors.surface) >= 4.5
            )
            assertTrue(
                "$name warning status",
                contrast(colors.warning, colors.surface) >= 3.0
            )
            assertTrue(
                "$name success status",
                contrast(colors.success, colors.surface) >= 3.0
            )
        }
    }

    @Test
    fun bordersAreVisibleButNotLoud() {
        listOf("light" to LightMessColors, "dark" to DarkMessColors).forEach { (name, colors) ->
            val ratio = contrast(colors.border, colors.surface)
            assertTrue("$name border is distinguishable", ratio >= 1.1)
            assertTrue("$name border stays quiet", ratio < 2.0)
        }
    }

    @Test
    fun strictAmoledIsOptInOnly() {
        assertEquals(Color(0xFF000000), PageAmoled)
        assertEquals(Color(0xFF000000), AmoledMessColors.background)
        assertEquals(Color(0xFF14110E), AmoledMessColors.surface)
        // The default night palette is the warmer near-black, never pure black.
        assertEquals(PageDark, DarkMessColors.background)
        assertTrue(PageDark != PageAmoled)
    }
}
