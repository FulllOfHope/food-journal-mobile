package com.messledger.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.LocalIsNight
import com.messledger.app.ui.theme.LocalMessColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

/**
 * A single horizontal safety net instead of a ring: twenty-five crisp
 * segments, grouped in fives so the eye can count without hunting. Spent
 * rebates fill with the skip colour; unused ones stay as quiet slate.
 */
@Composable
fun AllowanceVisualizer(
    allowanceTotal: Int = 25,
    consumed: Double,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val colors = LocalMessColors.current
    val isDark = LocalIsNight.current
    val used = consumed.coerceIn(0.0, allowanceTotal.toDouble())
    val remaining = (allowanceTotal - used).coerceAtLeast(0.0)

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (remaining % 1.0 == 0.0) remaining.toInt().toString()
                        else "%.1f".format(remaining),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = " of $allowanceTotal left",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Text(
                    text = if (used % 1.0 == 0.0) "${used.toInt()} used" else "%.1f used".format(used),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val gap = 2.dp.toPx()
            val groupGap = 5.dp.toPx()
            val slots = allowanceTotal.coerceAtLeast(1)
            val groupBreaks = (slots - 1) / 5
            val totalGaps = gap * (slots - 1) + groupGap * groupBreaks
            val slotWidth = ((size.width - totalGaps) / slots).coerceAtLeast(1f)
            val radius = CornerRadius(2.dp.toPx())
            val emptyColor = if (isDark) colors.ruleDark else colors.rule
            val fillColor = colors.missed

            var x = 0f
            for (i in 0 until slots) {
                drawRoundRect(
                    color = emptyColor,
                    topLeft = Offset(x, 0f),
                    size = Size(slotWidth, size.height),
                    cornerRadius = radius
                )

                val fraction = (used - i).coerceIn(0.0, 1.0).toFloat()
                if (fraction > 0f) {
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(x, 0f),
                        size = Size(slotWidth * fraction, size.height),
                        cornerRadius = radius
                    )
                }

                x += slotWidth
                if (i < slots - 1) {
                    x += if ((i + 1) % 5 == 0) groupGap else gap
                }
            }
        }
    }
}
