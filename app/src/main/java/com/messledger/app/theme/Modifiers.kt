package com.messledger.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.CardDark
import com.messledger.app.ui.theme.Ember
import com.messledger.app.ui.theme.EtchedRuleStrong
import com.messledger.app.ui.theme.NightSurfaceVariant
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
 * Adds a static paper grain. On cream stock it is black ink dust; on the night
 * ledger it is a barely-there chalk dust. Cached, so the cost is one draw.
 */
fun Modifier.paperGrain(
    color: Color = Color.Black,
    alpha: Float = 0.025f
): Modifier = this.drawWithCache {
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
            color = color.copy(alpha = alpha),
            style = Stroke(width = 2f)
        )
    }
}

/** Chalk dust on the Night Ledger. Subtle enough to read as paper, not noise. */
fun Modifier.nightGrain(): Modifier = paperGrain(color = Color.White, alpha = 0.016f)

/**
 * The night card: a filled ink panel with one hairline rule around it.
 * No glow, just the same paper idea with the lights off. Active cards get a
 * muted copper rule instead of the neutral one.
 */
private fun Modifier.etchedPanel(isActive: Boolean): Modifier = this.drawBehind {
    val corner = CornerRadius(6.dp.toPx())

    drawRoundRect(
        color = if (isActive) NightSurfaceVariant else CardDark,
        cornerRadius = corner
    )

    if (isActive) {
        // A whisper of copper in the stock, not a glow.
        drawRoundRect(
            color = Ember.copy(alpha = 0.05f),
            cornerRadius = corner
        )
    }

    drawRoundRect(
        color = if (isActive) Ember.copy(alpha = 0.7f) else EtchedRuleStrong,
        cornerRadius = corner,
        style = Stroke(width = 1.dp.toPx())
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
 */
fun Modifier.messLedgerBackground(): Modifier = composed {
    if (isSystemInDarkTheme()) {
        this.nightGrain()
    } else {
        this.paperGrain()
    }
}

/**
 * The Master Adaptive Border Modifier.
 */
fun Modifier.messLedgerBorder(isActive: Boolean = false): Modifier = composed {
    if (isSystemInDarkTheme()) {
        this.etchedPanel(isActive)
    } else {
        this.deckledEdge(color = if (isActive) StampRed else LedgerRule)
    }
}

