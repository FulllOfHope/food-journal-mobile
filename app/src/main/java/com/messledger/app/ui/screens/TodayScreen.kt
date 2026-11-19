package com.messledger.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import com.messledger.app.ui.AllowanceVisualizer
import com.messledger.app.ui.MealCard
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.rememberMotionDuration
import com.messledger.app.ui.theme.tabularNumbers
import com.messledger.app.viewmodel.MessState
import com.messledger.app.viewmodel.MessViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@Composable
fun TodayScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 64.dp.toPx() }

    val now by produceState(initialValue = LocalTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalTime.now()
        }
    }

    val activeMeal = if (state.selectedDate == LocalDate.now()) {
        when (now.hour) {
            in 7..9 -> MealType.BREAKFAST
            in 12..14 -> MealType.LUNCH
            in 19..21 -> MealType.DINNER
            else -> null
        }
    } else {
        null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.selectedDate) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag > swipeThreshold) {
                            viewModel.setSelectedDate(state.selectedDate.minusDays(1))
                        } else if (totalDrag < -swipeThreshold) {
                            viewModel.setSelectedDate(state.selectedDate.plusDays(1))
                        }
                    }
                )
            }
    ) {
        val twoPane = maxWidth >= 720.dp || (maxWidth >= 600.dp && maxWidth > maxHeight)
        val narrow = maxWidth < 360.dp
        val visualHeight = when {
            maxHeight < 420.dp -> 150.dp
            twoPane -> 230.dp
            else -> 190.dp
        }

        if (twoPane) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1180.dp)
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.92f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        DateNavigator(
                            date = state.selectedDate,
                            narrow = false,
                            onPrevious = { viewModel.setSelectedDate(state.selectedDate.minusDays(1)) },
                            onNext = { viewModel.setSelectedDate(state.selectedDate.plusDays(1)) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TangerineTree(state = state, height = visualHeight)
                        Spacer(modifier = Modifier.height(12.dp))
                        DailySubtotal(state = state)
                        Spacer(modifier = Modifier.height(16.dp))
                        DaySummaryNote(state = state)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.08f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        MealRows(state = state, viewModel = viewModel, activeMeal = activeMeal)
                        Spacer(modifier = Modifier.height(20.dp))
                        ActionButtons(viewModel = viewModel)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    DateNavigator(
                        date = state.selectedDate,
                        narrow = narrow,
                        onPrevious = { viewModel.setSelectedDate(state.selectedDate.minusDays(1)) },
                        onNext = { viewModel.setSelectedDate(state.selectedDate.plusDays(1)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TangerineTree(state = state, height = visualHeight)
                    Spacer(modifier = Modifier.height(12.dp))
                    DailySubtotal(state = state)
                    Spacer(modifier = Modifier.height(16.dp))
                    MealRows(state = state, viewModel = viewModel, activeMeal = activeMeal)
                    Spacer(modifier = Modifier.height(20.dp))
                    ActionButtons(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DateNavigator(
    date: LocalDate,
    narrow: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalMessColors.current
    val duration = rememberMotionDuration(120)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPrevious,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(if (narrow) "◀" else "◀ Prev", color = colors.textPrimary)
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (date == LocalDate.now()) "TODAY" else "SELECTED DAY",
                style = MaterialTheme.typography.labelSmall,
                color = colors.tangerine,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            AnimatedContent(
                targetState = date,
                transitionSpec = {
                    fadeIn(androidx.compose.animation.core.tween(duration)) togetherWith
                        fadeOut(androidx.compose.animation.core.tween(duration))
                },
                label = "date_crossfade"
            ) { targetDate ->
                Text(
                    text = targetDate.format(longDateFormatter),
                    style = if (narrow) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    }.tabularNumbers(),
                    color = colors.textPrimary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }

        TextButton(
            onClick = onNext,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(if (narrow) "▶" else "Next ▶", color = colors.textPrimary)
        }
    }
}

@Composable
private fun DailySubtotal(state: MessState) {
    val colors = LocalMessColors.current
    var expanded by remember { mutableStateOf(false) }
    val duration = rememberMotionDuration(120)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Daily subtotal  ·  allowance",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Hide allowance" else "Show allowance",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "₹%.2f".format(state.dailyLiability),
                style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                color = colors.textPrimary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(androidx.compose.animation.core.tween(duration)),
            exit = fadeOut(androidx.compose.animation.core.tween(duration))
        ) {
            AllowanceVisualizer(
                allowanceTotal = state.config.allowedMisses,
                consumed = state.cycleSkippedCount,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MealRows(
    state: MessState,
    viewModel: MessViewModel,
    activeMeal: MealType?
) {
    val rule = BillingEngine.ruleFor(state.selectedDate, state.config)
    val isServingNow = state.selectedDate == LocalDate.now()

    val breakfastRecord = state.mealsForDate[MealType.BREAKFAST]
    MealCard(
        mealType = MealType.BREAKFAST,
        value = if (rule.breakfast) 0.5 else 0.0,
        status = breakfastRecord?.status ?: true,
        previousStatus = breakfastRecord?.previousStatus,
        guestCount = breakfastRecord?.guestCount ?: 0,
        recorded = breakfastRecord != null,
        isActive = isServingNow && activeMeal == MealType.BREAKFAST,
        onClick = { viewModel.toggleMeal(MealType.BREAKFAST) },
        onGuestChange = { viewModel.changeGuestCount(MealType.BREAKFAST, it) }
    )

    val lunchRecord = state.mealsForDate[MealType.LUNCH]
    MealCard(
        mealType = MealType.LUNCH,
        value = rule.lunchPortions.toDouble(),
        status = lunchRecord?.status ?: true,
        previousStatus = lunchRecord?.previousStatus,
        guestCount = lunchRecord?.guestCount ?: 0,
        recorded = lunchRecord != null,
        isActive = isServingNow && activeMeal == MealType.LUNCH,
        feast = rule.lunchPortions >= 2,
        onClick = { viewModel.toggleMeal(MealType.LUNCH) },
        onGuestChange = { viewModel.changeGuestCount(MealType.LUNCH, it) }
    )

    val dinnerRecord = state.mealsForDate[MealType.DINNER]
    MealCard(
        mealType = MealType.DINNER,
        value = if (rule.dinner) 1.0 else 0.0,
        status = dinnerRecord?.status ?: true,
        previousStatus = dinnerRecord?.previousStatus,
        guestCount = dinnerRecord?.guestCount ?: 0,
        recorded = dinnerRecord != null,
        isActive = isServingNow && rule.dinner && activeMeal == MealType.DINNER,
        onClick = { if (rule.dinner) viewModel.toggleMeal(MealType.DINNER) },
        onGuestChange = if (rule.dinner) {
            { viewModel.changeGuestCount(MealType.DINNER, it) }
        } else null
    )
}

@Composable
private fun ActionButtons(viewModel: MessViewModel) {
    val colors = LocalMessColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { viewModel.setAllMealsStatus(true) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.tangerine,
                contentColor = colors.onTangerine
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("Mark All Eaten", maxLines = 1)
        }
        TextButton(
            onClick = { viewModel.setAllMealsStatus(false) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text("Clear All", color = colors.textSecondary, maxLines = 1)
        }
    }
}

/** A quiet footnote only shown when there is room for it. */
@Composable
private fun DaySummaryNote(state: MessState) {
    val colors = LocalMessColors.current
    val rule = BillingEngine.ruleFor(state.selectedDate, state.config)
    val closed = when {
        !rule.breakfast && !rule.dinner -> "Breakfast and dinner are closed on this date."
        !rule.dinner -> "Dinner is closed on this date."
        !rule.breakfast -> "Breakfast is closed on this date."
        rule.lunchPortions == 0 -> "Lunch is closed on this date."
        else -> "All served meals are billed unless marked skipped."
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Today's allowance",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${state.cycleSkippedCount.toInt()} of ${state.config.allowedMisses} rebates used this cycle.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = closed,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary
        )
    }
}

/**
 * ONE botanical page mark, shared by light and night. Only the token values
 * change. Skipped fruit is a hollow ring; nothing glows or pulses.
 */
@Composable
fun TangerineTree(state: MessState, height: Dp, modifier: Modifier = Modifier) {
    val colors = LocalMessColors.current
    val rule = BillingEngine.ruleFor(state.selectedDate, state.config)

    val dailyMeals = buildList {
        if (rule.breakfast) add(state.mealsForDate[MealType.BREAKFAST]?.status ?: true)
        if (rule.lunchPortions > 0) add(state.mealsForDate[MealType.LUNCH]?.status ?: true)
        if (rule.dinner) add(state.mealsForDate[MealType.DINNER]?.status ?: true)
        if (isEmpty()) add(true)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val scale = size.height / 180.dp.toPx()

            val trunkTop = 80f * scale
            val trunkBottom = size.height - 80f * scale
            val trunkX = center.x

            val bark = colors.bark
            val leaf = colors.leaf
            val tangerine = colors.tangerine

            val trunkPath = Path().apply {
                moveTo(trunkX, trunkBottom)
                quadraticBezierTo(
                    x1 = trunkX - 12f * scale,
                    y1 = size.height / 2f,
                    x2 = trunkX,
                    y2 = trunkTop
                )
            }
            drawPath(
                path = trunkPath,
                color = bark,
                style = Stroke(width = 8f * scale, cap = StrokeCap.Round)
            )

            val spacing = (trunkBottom - trunkTop - 60f * scale) / 5f

            dailyMeals.forEachIndexed { index, isEaten ->
                val isLeft = index % 2 == 0
                val xDir = if (isLeft) -1f else 1f
                val fruitY = trunkTop + 30f * scale + index * spacing
                val fruitX = trunkX + xDir * 45f * scale
                val bend = if (fruitY > size.height * 0.3f && fruitY < size.height * 0.7f) {
                    -6f * scale
                } else 0f

                drawLine(
                    color = bark,
                    start = Offset(trunkX + bend, fruitY - 5f * scale),
                    end = Offset(fruitX, fruitY - 18f * scale),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )

                val radius = 35f * scale
                if (isEaten) {
                    drawCircle(color = tangerine, radius = radius, center = Offset(fruitX, fruitY))
                } else {
                    drawCircle(
                        color = tangerine,
                        radius = radius,
                        center = Offset(fruitX, fruitY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                drawOval(
                    color = leaf,
                    topLeft = Offset(fruitX - 10f * scale, fruitY - 28f * scale),
                    size = Size(18f * scale, 12f * scale)
                )
            }
        }
    }
}
