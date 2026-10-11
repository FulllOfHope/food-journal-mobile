package com.messledger.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.ui.MealCard
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessViewModel
import java.time.DayOfWeek
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TodayScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val isDark = isSystemInDarkTheme()
    val colors = LocalMessColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        // Date Navigator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.setSelectedDate(state.selectedDate.minusDays(1)) }) {
                Text("◀ Prev", color = MaterialTheme.colorScheme.onSurface)
            }

            AnimatedContent(
                targetState = state.selectedDate,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "date_anim"
            ) { date ->
                Text(
                    text = date.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(onClick = { viewModel.setSelectedDate(state.selectedDate.plusDays(1)) }) {
                Text("Next ▶", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Astrolabe / Tangerine Tree Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            val missedMeals = state.cycleSkippedCount.toInt()

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)

                if (isDark) {
                    // ----------------------------------------------------
                    // STARGAZER'S ASTROLABE (Dark Mode - Cycle Logic)
                    // ----------------------------------------------------
                    val radius = size.height / 2 * 0.8f
                    drawCircle(color = colors.astrolabeRing, radius = radius, style = Stroke(width = 2.dp.toPx()))

                    for (i in 0 until 25) {
                        val angle = (i.toFloat() / 25f) * 2 * PI - PI / 2
                        val cx = center.x + radius * cos(angle).toFloat()
                        val cy = center.y + radius * sin(angle).toFloat()

                        if (i < missedMeals) {
                            drawCircle(color = colors.accentRed.copy(alpha = 0.4f), radius = 8f, center = Offset(cx, cy))
                            drawCircle(color = colors.accentRed, radius = 4f, center = Offset(cx, cy))
                        } else {
                            drawCircle(color = colors.rule, radius = 4f, center = Offset(cx, cy))
                        }
                    }

                    drawCircle(color = colors.textPrimary.copy(alpha = 0.2f), radius = 24f, center = center)
                    drawCircle(color = colors.textPrimary, radius = 8f, center = center)

                    if (missedMeals > 25) {
                        val random = Random(42)
                        for (i in 0 until 3) {
                            val angle = random.nextFloat() * 2 * PI
                            val startX = center.x + radius * cos(angle).toFloat()
                            val startY = center.y + radius * sin(angle).toFloat()
                            val path = Path()
                            path.moveTo(startX, startY)
                            var currX = startX
                            var currY = startY
                            for (step in 0 until 5) {
                                currX += (random.nextFloat() - 0.5f) * 60f
                                currY += (random.nextFloat() - 0.5f) * 60f
                                path.lineTo(currX, currY)
                            }
                            drawPath(
                                path = path,
                                color = colors.accentRed,
                                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
                            )
                        }
                    }
                } else {
                    // ----------------------------------------------------
                    // SUNLIT SCRAPBOOK (The Daily 3-Tangerine Stick)
                    // ----------------------------------------------------
                    val trunkTop = 80f
                    val trunkBottom = size.height - 80f
                    val trunkX = center.x

                    val barkColor = Color(0xFF4A3B32)
                    val leafColor = Color(0xFF5A7A2A)
                    val tangerineColor = Color(0xFFD97020)

                    // 1. Draw the Single Elegant Stem
                    val trunkPath = Path().apply {
                        moveTo(trunkX, trunkBottom)
                        // A very subtle, organic curve
                        quadraticBezierTo(
                            x1 = trunkX - 12f, y1 = size.height / 2f,
                            x2 = trunkX, y2 = trunkTop
                        )
                    }
                    drawPath(
                        path = trunkPath,
                        color = barkColor,
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )

                    // 2. Evaluate Today's Meals
                    val isSaturday = state.selectedDate.dayOfWeek == DayOfWeek.SATURDAY
                    val dailyMeals = listOfNotNull(
                        state.mealsForDate[MealType.BREAKFAST]?.status ?: true,
                        state.mealsForDate[MealType.LUNCH]?.status ?: true,
                        if (!isSaturday) state.mealsForDate[MealType.DINNER]?.status ?: true else null
                    )

                    // 3. Draw the 3 Daily Fruits (Huge size)
                    val spacing = (trunkBottom - trunkTop - 60f) / 5f

                    dailyMeals.forEachIndexed { index, isEaten ->
                        val isLeft = index % 2 == 0
                        val xDir = if (isLeft) -1f else 1f

                        if (isEaten) {
                            // HANGING FRUIT
                            val fruitY = trunkTop + 30f + (index * spacing)
                            val fruitX = trunkX + (xDir * 45f)

                            // Calculate where the stem attaches to the slightly curved trunk
                            val bendFactor = if (fruitY > size.height * 0.3f && fruitY < size.height * 0.7f) -6f else 0f
                            val attachX = trunkX + bendFactor

                            // Tiny connecting stem
                            drawLine(
                                color = barkColor,
                                start = Offset(attachX, fruitY - 5f),
                                end = Offset(fruitX, fruitY - 18f),
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )

                            // Giant Tangerine
                            drawCircle(color = tangerineColor, radius = 35f, center = Offset(fruitX, fruitY))

                            // Little green leaf on top
                            drawOval(
                                color = leafColor,
                                topLeft = Offset(fruitX - 10f, fruitY - 28f),
                                size = Size(18f, 12f)
                            )
                        } else {
                            // FALLEN FRUIT (Spread out on the ground)
                            val dropX = trunkX + (xDir * (40f + index * 25f))
                            val dropY = trunkBottom - 5f + (if (isLeft) 6f else -4f)

                            // Giant Tangerine on the floor
                            drawCircle(color = tangerineColor, radius = 35f, center = Offset(dropX, dropY))

                            // Little green leaf, rotated slightly for the fallen state
                            drawOval(
                                color = leafColor,
                                topLeft = Offset(dropX - 14f, dropY - 22f),
                                size = Size(14f, 10f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Financial Subtotal
        Text(
            text = "Daily Subtotal: ₹%.2f".format(state.dailyLiability),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Meal Cards
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

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { viewModel.setAllMealsStatus(false) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = colors.accentRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Clear All")
            }
            OutlinedButton(
                onClick = { viewModel.setAllMealsStatus(true) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = colors.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Mark All Eaten")
            }
        }
    }
}