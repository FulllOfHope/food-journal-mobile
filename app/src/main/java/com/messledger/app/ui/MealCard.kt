package com.messledger.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.theme.messLedgerBackground
import com.messledger.app.theme.messLedgerBorder
import com.messledger.app.ui.theme.LocalMessColors

@Composable
fun MealCard(
    mealType: MealType,
    value: Double,
    status: Boolean,
    previousStatus: Boolean? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = LocalMessColors.current
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
                color = if (disabled) colors.textTertiary.copy(alpha = 0.65f) else colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // THE AUDIT TRAIL: Render prior value with a hand-drawn diagonal pencil strike-through
                if (previousStatus != null && previousStatus != status) {
                    val prevText = if (previousStatus) "Eaten" else "Skipped"
                    Text(
                        text = prevText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                        modifier = Modifier.drawBehind {
                            drawLine(
                                color = colors.textTertiary,
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
                    color = if (disabled) colors.textTertiary.copy(alpha = 0.65f) else colors.textPrimary
                )
            }
        }

        // TACTILE ANIMATION: a red stamp by day, a copper seal by night.
        if (!disabled && scale.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale.value)
                    .rotate(if (isDark) 0f else -15f),
                contentAlignment = Alignment.Center
            ) {
                if (isDark) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val hairline = 1.dp.toPx()

                        drawCircle(
                            color = colors.brass.copy(alpha = 0.45f),
                            radius = size.minDimension / 2f - hairline,
                            center = center,
                            style = Stroke(width = hairline)
                        )
                        drawCircle(
                            color = colors.brass.copy(alpha = 0.55f),
                            radius = size.minDimension * 0.34f,
                            center = center,
                            style = Stroke(width = hairline)
                        )
                        drawCircle(
                            color = colors.accentRed,
                            radius = size.minDimension * 0.21f,
                            center = center
                        )
                    }
                } else {
                    // Stamp seal for Light Mode Scrapbook
                    Text(
                        text = "✔",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.accentRed
                    )
                }
            }
        }
    }
}
