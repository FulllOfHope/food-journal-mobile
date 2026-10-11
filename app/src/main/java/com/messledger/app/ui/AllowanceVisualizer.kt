package com.messledger.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.LocalMessColors
import kotlin.math.min

@Composable
fun AllowanceVisualizer(
    allowanceTotal: Int = 25,
    consumed: Double,
    modifier: Modifier = Modifier
) {
    val rows = 5
    val cols = 5

    // Separate the full misses from a potential 0.5 breakfast miss
    val fullMisses = consumed.toInt()
    val hasHalfMiss = (consumed - fullMisses) >= 0.5

    val colors = LocalMessColors.current

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
    ) {
        val cellWidth = size.width / cols
        val cellHeight = size.height / rows
        val cellSize = min(cellWidth, cellHeight)

        // Center the grid
        val startX = (size.width - cellSize * cols) / 2f
        val startY = (size.height - cellSize * rows) / 2f

        val stampRed = colors.accentRed
        val borderColor = colors.rule

        for (i in 0 until allowanceTotal) {
            val row = i / cols
            val col = i % cols

            val x = startX + col * cellSize
            val y = startY + row * cellSize

            // Draw crisp 1dp square border
            drawRect(
                color = borderColor,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
                style = Stroke(width = 1.dp.toPx())
            )

            val padding = cellSize * 0.2f

            // Draw full hand-drawn style red 'X'
            if (i < fullMisses) {
                // Top-left to bottom-right
                drawLine(
                    color = stampRed,
                    start = Offset(x + padding, y + padding),
                    end = Offset(x + cellSize - padding, y + cellSize - padding),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Bottom-left to top-right
                drawLine(
                    color = stampRed,
                    start = Offset(x + padding, y + cellSize - padding),
                    end = Offset(x + cellSize - padding, y + padding),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            // Draw half cut (single slash) for a 0.5 miss
            else if (i == fullMisses && hasHalfMiss) {
                // Bottom-left to top-right only
                drawLine(
                    color = stampRed,
                    start = Offset(x + padding, y + cellSize - padding),
                    end = Offset(x + cellSize - padding, y + padding),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}