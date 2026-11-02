package com.messledger.app.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.ui.MealCard
import com.messledger.app.ui.theme.LocalIsNight
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessState
import com.messledger.app.viewmodel.MessViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TodayScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val isDark = LocalIsNight.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Tablets and landscape phones get the day in two columns: the page
        // mark on the left, the three meal rows on the right.
        val twoPane = maxWidth >= 720.dp || (maxWidth >= 600.dp && maxWidth > maxHeight)
        val narrow = maxWidth < 360.dp
        val visualHeight = when {
            maxHeight < 420.dp -> 140.dp
            twoPane -> 240.dp
            else -> 180.dp
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
                        Spacer(modifier = Modifier.height(16.dp))
                        DaySummaryNote(state = state)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.08f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        MealRows(state = state, viewModel = viewModel)
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
                    Spacer(modifier = Modifier.height(16.dp))
                    MealRows(state = state, viewModel = viewModel)
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

        AnimatedContent(
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
private fun MealRows(state: MessState, viewModel: MessViewModel) {
    val isSaturday = state.selectedDate.dayOfWeek == DayOfWeek.SATURDAY

    val breakfastRecord = state.mealsForDate[MealType.BREAKFAST]
    MealCard(
        mealType = MealType.BREAKFAST,
        value = 0.5,
        status = breakfastRecord?.status ?: true,
        previousStatus = breakfastRecord?.previousStatus,
        onClick = { viewModel.toggleMeal(MealType.BREAKFAST) }
    )

    val lunchRecord = state.mealsForDate[MealType.LUNCH]
    MealCard(
        mealType = MealType.LUNCH,
        value = if (isSaturday) 2.0 else 1.0,
        status = lunchRecord?.status ?: true,
        previousStatus = lunchRecord?.previousStatus,
        onClick = { viewModel.toggleMeal(MealType.LUNCH) }
    )

    val dinnerRecord = state.mealsForDate[MealType.DINNER]
    MealCard(
        mealType = MealType.DINNER,
        value = if (isSaturday) 0.0 else 1.0,
        status = dinnerRecord?.status ?: true,
        previousStatus = dinnerRecord?.previousStatus,
        onClick = { if (!isSaturday) viewModel.toggleMeal(MealType.DINNER) }
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
    val isSaturday = state.selectedDate.dayOfWeek == DayOfWeek.SATURDAY
    val closed = if (isSaturday) "Dinner is closed on Saturdays." else "All three meals are billed unless marked skipped."

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
    val selectedDate = state.selectedDate
    val isSaturday = selectedDate.dayOfWeek == DayOfWeek.SATURDAY

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            if (isDark) {
                // ----------------------------------------------------
                // THE NIGHT LEDGER (a quiet 25-bead cycle dial)
                // ----------------------------------------------------
                val outerRadius = min(size.width, size.height) / 2f * 0.86f
                val innerRadius = outerRadius * 0.76f
                val hairline = 1.dp.toPx()
                val beadRadius = 3.dp.toPx()
                val tickInset = 5.dp.toPx()

                drawCircle(
                    color = colors.ruleDark,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = hairline)
                )
                drawCircle(
                    color = colors.rule.copy(alpha = 0.85f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = hairline)
                )

                val missedMeals = state.cycleSkippedCount.toInt()
                for (i in 0 until 25) {
                    val angle = (i.toFloat() / 25f) * 2f * PI - PI / 2f
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()

                    if (i < missedMeals) {
                        // A copper bead for each rebate spent.
                        val beadCenter = Offset(
                            center.x + outerRadius * cosA,
                            center.y + outerRadius * sinA
                        )
                        drawCircle(
                            color = colors.accentRed.copy(alpha = 0.16f),
                            radius = beadRadius * 2f,
                            center = beadCenter
                        )
                        drawCircle(
                            color = colors.accentRed,
                            radius = beadRadius,
                            center = beadCenter
                        )
                    } else {
                        // Plain hairline ticks for the untouched allowance.
                        drawLine(
                            color = colors.rule.copy(alpha = 0.75f),
                            start = Offset(
                                center.x + (outerRadius - tickInset) * cosA,
                                center.y + (outerRadius - tickInset) * sinA
                            ),
                            end = Offset(
                                center.x + (outerRadius + 2.dp.toPx()) * cosA,
                                center.y + (outerRadius + 2.dp.toPx()) * sinA
                            ),
                            strokeWidth = hairline,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // The noon mark, so the dial has an orientation.
                drawLine(
                    color = colors.brass.copy(alpha = 0.85f),
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y - innerRadius),
                    strokeWidth = hairline * 1.5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = colors.brass,
                    radius = 1.8.dp.toPx(),
                    center = Offset(center.x, center.y - innerRadius)
                )

                // The moon reports the day rather than decorating it:
                // new moon at none, crescent at one, gibbous at two, full at all.
                val meals = listOf(MealType.BREAKFAST, MealType.LUNCH) +
                    if (!isSaturday) listOf(MealType.DINNER) else emptyList()
                val eatenCount = meals.count { state.mealsForDate[it]?.status ?: true }
                val fullDay = meals.isNotEmpty() && eatenCount == meals.size

                val moonRadius = outerRadius * 0.30f
                val moonCenter = Offset(
                    center.x - moonRadius * 0.08f,
                    center.y + moonRadius * 0.02f
                )

                if (eatenCount == 0) {
                    // New moon: the page is unmarked.
                    drawCircle(
                        color = colors.textPrimary.copy(alpha = 0.55f),
                        radius = moonRadius,
                        center = moonCenter,
                        style = Stroke(width = hairline)
                    )
                } else if (!fullDay) {
                    val shadowDistance = when {
                        eatenCount == 1 -> 0.55f
                        meals.size == 2 -> 2.2f
                        else -> 1.25f
                    }

                    // Cut the crescent out of the lit disc instead of painting
                    // an opaque shadow, so the lamp wash below stays visible.
                    val lit = Path().apply {
                        addOval(
                            Rect(
                                left = moonCenter.x - moonRadius,
                                top = moonCenter.y - moonRadius,
                                right = moonCenter.x + moonRadius,
                                bottom = moonCenter.y + moonRadius
                            )
                        )
                    }
                    val shadowCenter = Offset(
                        moonCenter.x + moonRadius * shadowDistance,
                        moonCenter.y - moonRadius * 0.18f
                    )
                    val shadowRadius = moonRadius * 0.93f
                    val shadow = Path().apply {
                        addOval(
                            Rect(
                                left = shadowCenter.x - shadowRadius,
                                top = shadowCenter.y - shadowRadius,
                                right = shadowCenter.x + shadowRadius,
                                bottom = shadowCenter.y + shadowRadius
                            )
                        )
                    }
                    val moon = Path().apply {
                        op(lit, shadow, PathOperation.Difference)
                    }
                    drawPath(path = moon, color = colors.textPrimary)
                } else {
                    drawCircle(color = colors.textPrimary, radius = moonRadius, center = moonCenter)
                }

                if (fullDay) {
                    // A perfect day earns the only star in the book.
                    val starCenter = Offset(
                        center.x + outerRadius * 0.36f,
                        center.y - outerRadius * 0.30f
                    )
                    val starArm = 4.5.dp.toPx()
                    drawLine(
                        color = colors.brass,
                        start = Offset(starCenter.x - starArm, starCenter.y),
                        end = Offset(starCenter.x + starArm, starCenter.y),
                        strokeWidth = hairline,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = colors.brass,
                        start = Offset(starCenter.x, starCenter.y - starArm),
                        end = Offset(starCenter.x, starCenter.y + starArm),
                        strokeWidth = hairline,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                // ----------------------------------------------------
                // SUNLIT SCRAPBOOK (The Daily 3-Tangerine Stick)
                // ----------------------------------------------------
                // Scale from the original 180dp canvas so the drawing keeps
                // its proportions on small phones, tablets and in landscape.
                val scale = size.height / 180.dp.toPx()

                val trunkTop = 80f * scale
                val trunkBottom = size.height - 80f * scale
                val trunkX = center.x

                val barkColor = Color(0xFF4A3B32)
                val leafColor = Color(0xFF5A7A2A)
                val tangerineColor = Color(0xFFD97020)

                // 1. Draw the Single Elegant Stem
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

                // 2. Evaluate Today's Meals
                val dailyMeals = listOfNotNull(
                    state.mealsForDate[MealType.BREAKFAST]?.status ?: true,
                    state.mealsForDate[MealType.LUNCH]?.status ?: true,
                    if (!isSaturday) state.mealsForDate[MealType.DINNER]?.status ?: true else null
                )

                // 3. Draw the 3 Daily Fruits
                val spacing = (trunkBottom - trunkTop - 60f * scale) / 5f

                dailyMeals.forEachIndexed { index, isEaten ->
                    val isLeft = index % 2 == 0
                    val xDir = if (isLeft) -1f else 1f

                    if (isEaten) {
                        // HANGING FRUIT
                        val fruitY = trunkTop + 30f * scale + (index * spacing)
                        val fruitX = trunkX + (xDir * 45f * scale)

                        val bendFactor = if (fruitY > size.height * 0.3f && fruitY < size.height * 0.7f) {
                            -6f * scale
                        } else {
                            0f
                        }
                        val attachX = trunkX + bendFactor

                        drawLine(
                            color = barkColor,
                            start = Offset(attachX, fruitY - 5f * scale),
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
                        // FALLEN FRUIT (Spread out on the ground)
                        val dropX = trunkX + (xDir * (40f * scale + index * 25f * scale))
                        val dropY = trunkBottom - 5f * scale + (if (isLeft) 6f * scale else -4f * scale)

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
}
