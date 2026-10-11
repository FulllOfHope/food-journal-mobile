package com.messledger.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * 🌞 Simulates a physically torn or uneven paper edge using a perturbed path.
 */
fun Modifier.deckledEdge(color: Color = LedgerRule): Modifier = this.drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    val path = Path()
    // Using a fixed seed based on size so the deckle doesn't jump/animate on recompose
    val random = Random((size.width + size.height).toInt())
    
    path.moveTo(0f, 0f)
    
    // Top edge
    var x = 0f
    while (x < size.width) {
        x += 8f
        val y = (random.nextFloat() - 0.5f) * 3f
        path.lineTo(x, y)
    }
    
    // Right edge
    var y = 0f
    while (y < size.height) {
        y += 8f
        val nx = size.width + (random.nextFloat() - 0.5f) * 3f
        path.lineTo(nx, y)
    }
    
    // Bottom edge
    while (x > 0f) {
        x -= 8f
        val ny = size.height + (random.nextFloat() - 0.5f) * 3f
        path.lineTo(x, ny)
    }
    
    // Left edge
    while (y > 0f) {
        y -= 8f
        val nx = (random.nextFloat() - 0.5f) * 3f
        path.lineTo(nx, y)
    }
    
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}

/**
 * 🌞 Adds a subtle paper grain effect by drawing thousands of faint static points.
 * Cached to ensure 0% battery drain after the initial draw calculation.
 */
fun Modifier.paperGrain(): Modifier = this.drawWithCache {
    val width = size.width.toInt()
    val height = size.height.toInt()
    
    val noisePath = Path()
    val random = Random(42)
    val density = (width * height) / 250 // Extremely lightweight static noise
    
    for (i in 0 until density) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        noisePath.moveTo(x, y)
        noisePath.lineTo(x + 1f, y + 1f)
    }
    
    onDrawBehind {
        drawPath(
            path = noisePath,
            color = Color.Black.copy(alpha = 0.025f),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * 🌙 Draws crisp, laser-cut glowing etched lines for Dark Mode.
 * We use an inner bright core and a wider, translucent glow effect.
 */
fun Modifier.glowingEtchedBorder(
    color: Color = EtchedGlass,
    glowColor: Color = EtchedGlass.copy(alpha = 0.4f)
): Modifier = this.drawBehind {
    val strokeWidth = 1.dp.toPx()
    val cornerRadius = 4.dp.toPx() // Crisp but slightly rounded edges

    // Outer Glow
    drawRoundRect(
        color = glowColor,
        style = Stroke(width = strokeWidth * 4),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
    )
    
    // Inner crisp line
    drawRoundRect(
        color = color,
        style = Stroke(width = strokeWidth),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
    )
}

/**
 * ✏️ Draws a faint pencil divider line instead of a solid UI separator
 */
fun Modifier.pencilDivider(color: Color = PencilFaint): Modifier = this.drawBehind {
    val strokeWidth = 1.dp.toPx()
    val path = Path()
    val random = Random((size.width).toInt())
    
    path.moveTo(0f, size.height / 2f)
    var x = 0f
    while (x < size.width) {
        x += 6f
        val y = (size.height / 2f) + (random.nextFloat() - 0.5f) * 1.5f
        path.lineTo(x, y)
    }
    
    drawPath(path = path, color = color.copy(alpha = 0.5f), style = Stroke(width = strokeWidth))
}

/**
 * The Master Adaptive Container Modifier.
 * Evaluates current theme and applies the exact physics described in the prompt.
 */
fun Modifier.messLedgerBackground(): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    if (isDark) {
        this // VoidCharcoal is applied via MaterialTheme color scheme, no grain needed in space
    } else {
        this.paperGrain()
    }
}

/**
 * The Master Adaptive Border Modifier.
 */
fun Modifier.messLedgerBorder(isActive: Boolean = false): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    if (isDark) {
        this.glowingEtchedBorder(
            color = if (isActive) NeonEmber else EtchedGlass,
            glowColor = if (isActive) NeonEmber.copy(alpha = 0.3f) else EtchedGlass.copy(alpha = 0.1f)
        )
    } else {
        this.deckledEdge(color = if (isActive) StampRed else LedgerRule)
    }
}
