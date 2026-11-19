package com.messledger.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import com.messledger.app.ui.MealCard
import com.messledger.app.ui.theme.LocalIsNight
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessState
import com.messledger.app.viewmodel.MessViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TodayScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val isDark = LocalIsNight.current
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
        // Tablets and landscape phones get the day in two columns: the page
        // mark on the left, the three meal rows on the right.
        val twoPane = maxWidth >= 720.dp || (maxWidth >= 600.dp && maxWidth > maxHeight)
        val narrow = maxWidth < 360.dp
        val visualHeight = when {
            maxHeight < 420.dp -> 150.dp
            twoPane -> 230.dp
            else -> 190.dp
        }

        if (twoPane) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
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
                        DayVisual(state = state, isDark = isDark, height = visualHeight)
                        Spacer(modifier = Modifier.height(12.dp))
                        DailySubtotal(state = state)
                        LedgerMasterBadge(state = state)
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
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
                    DayVisual(state = state, isDark = isDark, height = visualHeight)
                    Spacer(modifier = Modifier.height(12.dp))
                    DailySubtotal(state = state)
                    LedgerMasterBadge(state = state)
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

        androidx.compose.animation.AnimatedContent(
            targetState = date,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }.using(SizeTransform(clip = false))
            },
            label = "date_anim"
        ) { targetDate ->
            Text(
                text = targetDate.toString(),
                style = if (narrow) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
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

    Text(
        text = "Daily Subtotal: ₹%.2f".format(state.dailyLiability),
        style = MaterialTheme.typography.titleMedium,
        color = colors.textPrimary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LedgerMasterBadge(state: MessState) {
    val allowed = state.config.allowedMisses
    if (allowed <= 0 || state.cycleSkippedCount < allowed) return

    val colors = LocalMessColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .background(colors.brass.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, colors.brass.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = colors.brass,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = if (state.cycleSkippedCount > allowed) {
                "Allowance fully used — every extra skip is billed."
            } else {
                "Ledger Master — all $allowed rebates used."
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textPrimary
        )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.setAllMealsStatus(false) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = colors.accentRed
            ),
            border = BorderStroke(1.dp, colors.ruleDark)
        ) {
            Text("Clear All", maxLines = 1)
        }
        OutlinedButton(
            onClick = { viewModel.setAllMealsStatus(true) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = colors.textPrimary
            ),
            border = BorderStroke(1.dp, colors.ruleDark)
        ) {
            Text("Mark All Eaten", maxLines = 1)
        }
    }
}

/** A small ledger footnote only shown when there is room for it. */
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

@Composable
private fun DayVisual(state: MessState, isDark: Boolean, height: Dp) {
    val colors = LocalMessColors.current
    val rule = BillingEngine.ruleFor(state.selectedDate, state.config)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // The same Daily 3-Tangerine Stick in both themes. Night simply
            // uses warmer ink on the black ledger; no dial or moon any more.
            val scale = size.height / 180.dp.toPx()
            val trunkTop = 80f * scale
            val trunkBottom = size.height - 80f * scale
            val trunkX = center.x

            val barkColor = if (isDark) colors.bark else Color(0xFF4A3B32)
            val leafColor = if (isDark) colors.leaf else Color(0xFF5A7A2A)
            val tangerineColor = if (isDark) colors.tangerine else Color(0xFFD97020)

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
                color = barkColor,
                style = Stroke(width = 8f * scale, cap = StrokeCap.Round)
            )

            val dailyMeals = buildList {
                if (rule.breakfast) add(state.mealsForDate[MealType.BREAKFAST]?.status ?: true)
                if (rule.lunchPortions > 0) add(state.mealsForDate[MealType.LUNCH]?.status ?: true)
                if (rule.dinner) add(state.mealsForDate[MealType.DINNER]?.status ?: true)
            }

            val spacing = (trunkBottom - trunkTop - 60f * scale) / 5f

            dailyMeals.forEachIndexed { index, isEaten ->
                val xDir = if (index % 2 == 0) -1f else 1f

                if (isEaten) {
                    val fruitY = trunkTop + 30f * scale + index * spacing
                    val fruitX = trunkX + xDir * 45f * scale
                    val bend = if (fruitY > size.height * 0.3f && fruitY < size.height * 0.7f) {
                        -6f * scale
                    } else {
                        0f
                    }
                    drawLine(
                        color = barkColor,
                        start = Offset(trunkX + bend, fruitY - 5f * scale),
                        end = Offset(fruitX, fruitY - 18f * scale),
                        strokeWidth = 3.5f * scale,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = tangerineColor,
                        radius = 35f * scale,
                        center = Offset(fruitX, fruitY)
                    )
                    drawOval(
                        color = leafColor,
                        topLeft = Offset(fruitX - 10f * scale, fruitY - 28f * scale),
                        size = Size(18f * scale, 12f * scale)
                    )
                } else {
                    val dropX = trunkX + xDir * (40f * scale + index * 25f * scale)
                    val dropY = trunkBottom - 5f * scale + (if (xDir < 0f) 6f * scale else -4f * scale)
                    drawCircle(
                        color = tangerineColor,
                        radius = 35f * scale,
                        center = Offset(dropX, dropY)
                    )
                    drawOval(
                        color = leafColor,
                        topLeft = Offset(dropX - 14f * scale, dropY - 22f * scale),
                        size = Size(14f * scale, 10f * scale)
                    )
                }
            }
        }
    }
}
