package com.messledger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PinThreadChart(
    dataPoints: List<Int>, // Y values (e.g., meals eaten: 0, 1, 2, 3)
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val maxMeals = 3f
        val pointsCount = dataPoints.size
        val xStep = size.width / (if (pointsCount > 1) pointsCount - 1 else 1).toFloat()
        
        val points = dataPoints.mapIndexed { index, mealsEaten ->
            val x = index * xStep
            // Y is inverted (0 is at the bottom, 3 is at the top)
            val y = size.height - (mealsEaten / maxMeals) * size.height
            Offset(x, y)
        }
        
        // Draw Thread (Path)
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val start = points[i]
                val end = points[i + 1]
                
                // Control points for a "sagging thread" effect (cubic bezier)
                val cx1 = start.x + (end.x - start.x) / 2
                val cy1 = start.y + 20f // Sag down
                val cx2 = start.x + (end.x - start.x) / 2
                val cy2 = end.y + 20f   // Sag down
                
                path.cubicTo(cx1, cy1, cx2, cy2, end.x, end.y)
            }
            
            drawPath(
                path = path,
                color = Color.Gray,
                style = Stroke(width = 4f)
            )
        }
        
        // Draw Pins (Red circles)
        for (point in points) {
            drawCircle(
                color = Color.Red,
                radius = 8f,
                center = point
            )
        }
    }
}
