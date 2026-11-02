package com.messledger.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.theme.messLedgerBackground
import com.messledger.app.theme.messLedgerBorder
import com.messledger.app.ui.theme.LocalIsNight
import com.messledger.app.ui.theme.LocalMessColors

@Composable
fun MealCard(
    mealType: MealType,
    value: Double,
    status: Boolean,
    previousStatus: Boolean? = null,
    guestCount: Int = 0,
    isActive: Boolean = false,
    feast: Boolean = false,
    onClick: () -> Unit,
    onGuestChange: ((Int) -> Unit)? = null
) {
    val isDark = LocalIsNight.current
    val colors = LocalMessColors.current
    val disabled = value == 0.0
    val haptics = LocalHapticFeedback.current

    // Spring animation for the stamp.
    val scale = animateFloatAsState(
        targetValue = if (status) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "stamp_scale"
    )

    // A single gold pulse when Saturday lunch is marked eaten. It is quiet,
    // one-shot, and never loops.
    val feastPulse = remember { Animatable(0f) }
    var wasEaten by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (feast && status && !wasEaten) {
            feastPulse.snapTo(0f)
            feastPulse.animateTo(1f, tween(360))
            feastPulse.animateTo(0f, tween(420))
        }
        wasEaten = status
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .messLedgerBorder(isActive = status)
            .messLedgerBackground()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !disabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mealType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (disabled) colors.textTertiary.copy(alpha = 0.65f) else colors.textPrimary
                    )
                    if (feast && !disabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        MealBadge(
                            text = "Feast",
                            color = colors.brass,
                            modifier = Modifier.scale(1f + feastPulse.value * 0.12f)
                        )
                    }
                    if (isActive && !disabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        MealBadge(text = "Now", color = colors.eaten)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // THE AUDIT TRAIL: Render prior value with a hand-drawn
                    // diagonal pencil strike-through.
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
                    if (guestCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $guestCount guest${if (guestCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.brass
                        )
                    }
                }
            }

            // TACTILE ANIMATION: a red stamp by day, a mint seal by night.
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
                                color = colors.eaten.copy(alpha = 0.5f),
                                radius = size.minDimension / 2f - hairline,
                                center = center,
                                style = Stroke(width = hairline)
                            )
                            drawCircle(
                                color = colors.eaten.copy(alpha = 0.6f),
                                radius = size.minDimension * 0.34f,
                                center = center,
                                style = Stroke(width = hairline)
                            )
                            drawCircle(
                                color = colors.eaten,
                                radius = size.minDimension * 0.21f,
                                center = center
                            )
                        }
                    } else {
                        Text(
                            text = "✔",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.accentRed
                        )
                    }
                }
            }
        }

        if (!disabled && onGuestChange != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isDark) colors.rule else colors.rule.copy(alpha = 0.7f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (guestCount == 0) "Bringing a guest?" else "Guest portions",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.weight(1f))

                if (guestCount > 0) {
                    IconButton(
                        onClick = { onGuestChange(-1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = "Remove guest portion",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = guestCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.brass
                    )
                }

                IconButton(
                    onClick = { onGuestChange(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add guest portion",
                        tint = colors.brass,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MealBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
