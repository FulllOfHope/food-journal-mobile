package com.messledger.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.messledger.app.theme.*
import com.messledger.app.data.MealType

@Composable
fun MealCard(
    mealType: MealType,
    value: Double,
    status: Boolean,
    previousStatus: Boolean? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val disabled = value == 0.0
    
    // Spring animation for stamp/node
    val scale = animateFloatAsState(
        targetValue = if (status) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "stamp_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .messLedgerBorder(isActive = status)
            .messLedgerBackground()
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = mealType.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else if (status) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // THE AUDIT TRAIL: Render prior value with a hand-drawn diagonal pencil strike-through
                if (previousStatus != null && previousStatus != status) {
                    val prevText = if (previousStatus) "Eaten" else "Skipped"
                    Text(
                        text = prevText,
                        style = MaterialTheme.typography.labelSmall,
                        color = PencilFaint,
                        modifier = Modifier.drawBehind {
                            drawLine(
                                color = PencilFaint,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                val currentText = if (disabled) "Closed" else if (status) "Eaten" else "Skipped"
                Text(
                    text = "$currentText ($value)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // TACTILE ANIMATION: Stamp Seal or Neon Ember Node
        if (!disabled && scale.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale.value)
                    .rotate(if (isDark) 0f else -15f), // Hand-stamped rotation in light mode
                contentAlignment = Alignment.Center
            ) {
                if (isDark) {
                    // Pulse animation for Dark Mode Astrolabe Node
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_alpha"
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = NeonEmber.copy(alpha = pulseAlpha * 0.3f),
                            radius = size.width / 2f
                        )
                        drawCircle(
                            color = NeonEmber,
                            radius = size.width / 6f
                        )
                    }
                } else {
                    // Stamp seal for Light Mode Scrapbook
                    Text(
                        text = "✔",
                        style = MaterialTheme.typography.titleLarge,
                        color = StampRed
                    )
                }
            }
        }
    }
}
