package com.messledger.app

import androidx.compose.ui.graphics.Color
import com.messledger.app.ui.theme.AmoledMessColors
import com.messledger.app.ui.theme.BrassLight
import com.messledger.app.ui.theme.CardDark
import com.messledger.app.ui.theme.CardLight
import com.messledger.app.ui.theme.DarkMessColors
import com.messledger.app.ui.theme.Ember
import com.messledger.app.ui.theme.Ink
import com.messledger.app.ui.theme.LightMessColors
import com.messledger.app.ui.theme.NightBark
import com.messledger.app.ui.theme.NightLeaf
import com.messledger.app.ui.theme.PageAmoled
import com.messledger.app.ui.theme.PageDark
import com.messledger.app.ui.theme.Paper
import com.messledger.app.ui.theme.Rule
import com.messledger.app.ui.theme.RuleDk
import com.messledger.app.ui.theme.Sage
import com.messledger.app.ui.theme.StampRed
import com.messledger.app.ui.theme.TangerineDark
import com.messledger.app.ui.theme.TextLight
import com.messledger.app.ui.theme.VoidColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Pins the restored paper journal by day and the near-black, orange-inked
 * ledger by night. These assertions stop either palette from drifting back.
 */
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
    fun sunlitScrapbookPaletteIsRestored() {
        assertEquals(Color(0xFFF4F1EA), Paper)
        assertEquals(Color(0xFFFAF8F2), CardLight)
        assertEquals(Color(0xFF212121), Ink)
        assertEquals(Color(0xFFDDD5C2), Rule)
        assertEquals(Color(0xFFC0B797), RuleDk)
        assertEquals(Color(0xFFB3402E), StampRed)
        assertEquals(Color(0xFFC8A46B), BrassLight)

        assertEquals(Paper, LightMessColors.background)
        assertEquals(CardLight, LightMessColors.surface)
        assertEquals(Ink, LightMessColors.textPrimary)
        assertEquals(Rule, LightMessColors.rule)
        assertEquals(RuleDk, LightMessColors.ruleDark)
        assertEquals(StampRed, LightMessColors.accentRed)
    }

    @Test
    fun nightPaletteIsBlackAndOrange() {
        assertEquals(Color(0xFF000000), VoidColor)
        assertEquals(Color(0xFF0E0B09), CardDark)
        assertEquals(Color(0xFFF7F1E8), TextLight)
        assertEquals(Color(0xFFFF7043), Ember)
        assertEquals(Color(0xFF93B56D), Sage)
        assertEquals(Color(0xFFA8C080), NightLeaf)
        assertEquals(Color(0xFF8A6A50), NightBark)
        assertEquals(Color(0xFFFF8A3D), TangerineDark)

        assertEquals(VoidColor, DarkMessColors.background)
        assertEquals(CardDark, DarkMessColors.surface)
        assertEquals(TextLight, DarkMessColors.textPrimary)
        assertEquals(Ember, DarkMessColors.accentRed)
        assertEquals(Sage, DarkMessColors.eaten)
        assertEquals(TangerineDark, DarkMessColors.tangerine)
    }

    @Test
    fun nightCanvasIsGenuinelyBlackAgain() {
        assertEquals(Color(0xFF000000), PageDark)
        assertEquals(Color(0xFF000000), PageAmoled)
        assertEquals(PageDark, DarkMessColors.background)
        assertEquals(PageDark, AmoledMessColors.background)
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
        }
    }
}
