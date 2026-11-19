package com.messledger.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.LocalMessColors

/** Leaf dot = eaten, hollow leaf = untouched, hollow warning ring = skipped, dash = closed. */
@Composable
fun MealStateDot(
    status: Boolean,
    recorded: Boolean,
    closed: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color? = null
) {
    val colors = LocalMessColors.current
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(radius, radius)
        when {
            closed -> drawLine(
                color = colors.textTertiary.copy(alpha = 0.6f),
                start = Offset(0f, radius),
                end = Offset(size.width, radius),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            !recorded -> drawCircle(
                color = colors.leaf,
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            status -> drawCircle(color = colors.leaf, radius = radius, center = center)
            else -> {
                // The slash's cut is painted in the resolved container colour so
                // it works on calendar range cells as well as plain surfaces.
                drawCircle(
                    color = colors.warning,
                    radius = radius - 1.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                val start = Offset(size.width * 0.22f, size.height * 0.78f)
                val end = Offset(size.width * 0.78f, size.height * 0.22f)
                if (containerColor != null) {
                    drawLine(containerColor, start, end, strokeWidth = 3.dp.toPx())
                }
                drawLine(
                    color = colors.warning,
                    start = start,
                    end = end,
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}
