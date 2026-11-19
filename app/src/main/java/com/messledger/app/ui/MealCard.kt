package com.messledger.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.messSurface
import com.messledger.app.ui.theme.rememberMotionDuration

/**
 * A flat meal row: 12dp radius, one hairline border, no corner ticks, no
 * status chips and no spring stamp. The row's only state marker is a small
 * ledger dot that crossfades in 120ms.
 */
@Composable
fun MealCard(
    mealType: MealType,
    value: Double,
    status: Boolean,
    previousStatus: Boolean? = null,
    guestCount: Int = 0,
    recorded: Boolean = true,
    isActive: Boolean = false,
    feast: Boolean = false,
    onClick: () -> Unit,
    onGuestChange: ((Int) -> Unit)? = null
) {
    val colors = LocalMessColors.current
    val haptics = LocalHapticFeedback.current
    val duration = rememberMotionDuration(120)
    val disabled = value == 0.0

    val markerScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(duration),
        label = "meal_marker_scale"
    )
    val markerAlpha by animateFloatAsState(
        targetValue = if (disabled) 0.5f else 1f,
        animationSpec = tween(duration),
        label = "meal_marker_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .messSurface(isActive = isActive && !disabled)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !disabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MealStateDot(
                        status = status,
                        recorded = recorded,
                        closed = disabled,
                        modifier = Modifier
                            .size(14.dp)
                            .alpha(markerAlpha)
                            .scale(markerScale)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = mealType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (disabled) colors.textTertiary else colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val currentText = when {
                        disabled -> "Closed"
                        !recorded -> "Untouched"
                        status -> "Eaten"
                        else -> "Skipped"
                    }
                    Text(
                        text = "$currentText ($value)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (disabled) colors.textTertiary else colors.textSecondary
                    )
                    if (guestCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+$guestCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary
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
                    .background(colors.border)
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
                    IconButton(onClick = { onGuestChange(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = "Remove guest portion",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "+$guestCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }

                IconButton(onClick = { onGuestChange(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add guest portion",
                        tint = colors.tangerine,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Leaf dot = eaten, hollow leaf = untouched, hollow tangerine ring = skipped, dash = closed. */
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
                // Hollow tangerine ring with a slash. The slash's cut is painted
                // in the resolved container colour so it works on range cells.
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
