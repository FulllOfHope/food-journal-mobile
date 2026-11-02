package com.messledger.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
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
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun LogScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }
    val colors = LocalMessColors.current

    // Sort dates descending
    val sortedDates = state.cycleMeals.keys.sortedDescending()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 840.dp

        val onPickDate: (LocalDate) -> Unit = { targetDate ->
            viewModel.setSelectedDate(targetDate)
            expandedDate = targetDate
            val index = sortedDates.indexOf(targetDate)
            if (index != -1) {
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            }
        }

        val jumpButton: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedButton(
                onClick = {
                    val current = state.selectedDate
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            // DatePickerDialog month is 0-indexed
                            onPickDate(LocalDate.of(year, month + 1, dayOfMonth))
                        },
                        current.year,
                        current.monthValue - 1,
                        current.dayOfMonth
                    ).show()
                },
                modifier = modifier,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Jump to Date")
            }
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
                            .weight(1.1f)
                            .fillMaxHeight()
                    ) {
                        jumpButton(Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        TimelineList(
                            dates = sortedDates,
                            state = state,
                            expandedDate = expandedDate,
                            twoPane = true,
                            listState = listState,
                            onRowClick = { date ->
                                expandedDate = date
                                viewModel.setSelectedDate(date)
                            },
                            onToggleMeal = { date, meal ->
                                viewModel.setSelectedDate(date)
                                viewModel.toggleMeal(meal)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DayDetailPane(
                        date = expandedDate ?: state.selectedDate,
                        state = state,
                        onToggleMeal = { date, meal ->
                            viewModel.setSelectedDate(date)
                            viewModel.toggleMeal(meal)
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                ) {
                    jumpButton(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                    TimelineList(
                        dates = sortedDates,
                        state = state,
                        expandedDate = expandedDate,
                        twoPane = false,
                        listState = listState,
                        onRowClick = { date ->
                            expandedDate = if (expandedDate == date) null else date
                        },
                        onToggleMeal = { date, meal ->
                            viewModel.setSelectedDate(date)
                            viewModel.toggleMeal(meal)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineList(
    dates: List<LocalDate>,
    state: MessState,
    expandedDate: LocalDate?,
    twoPane: Boolean,
    listState: LazyListState,
    onRowClick: (LocalDate) -> Unit,
    onToggleMeal: (LocalDate, MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = if (twoPane) 16.dp else 24.dp)
    ) {
        items(dates) { date ->
            val records = state.cycleMeals[date] ?: emptyList()
            val isExpanded = expandedDate == date
            val eatenCount = records.count { it.status }
            val maxEaten = if (date.dayOfWeek == DayOfWeek.SATURDAY) 2 else 3
            val fullyEaten = eatenCount == maxEaten
            val colors = LocalMessColors.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp)
            ) {
                // Timeline Gutter
                Canvas(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                ) {
                    val centerX = size.width / 2
                    // Thread
                    drawLine(
                        color = colors.rule,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    // Knot
                    drawCircle(
                        color = colors.ruleDark,
                        radius = 8f,
                        center = Offset(centerX, 24.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = if (fullyEaten) colors.textPrimary else colors.surface,
                        radius = 6f,
                        center = Offset(centerX, 24.dp.toPx())
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Row content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable { onRowClick(date) }
                ) {
                    Text(
                        text = date.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isExpanded && twoPane) colors.brass else colors.textPrimary
                    )

                    Text(
                        text = "$eatenCount/$maxEaten meals eaten",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )

                    AnimatedVisibility(visible = isExpanded && !twoPane) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            LogMealCards(
                                date = date,
                                state = state,
                                onToggleMeal = onToggleMeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogMealCards(
    date: LocalDate,
    state: MessState,
    onToggleMeal: (LocalDate, MealType) -> Unit
) {
    val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
    val records = state.cycleMeals[date] ?: emptyList()
    val dayMap = records.associateBy { it.mealType }

    val bRecord = dayMap[MealType.BREAKFAST]
    MealCard(
        mealType = MealType.BREAKFAST,
        value = 0.5,
        status = bRecord?.status ?: true,
        previousStatus = bRecord?.previousStatus,
        onClick = { onToggleMeal(date, MealType.BREAKFAST) }
    )

    val lRecord = dayMap[MealType.LUNCH]
    MealCard(
        mealType = MealType.LUNCH,
        value = if (isSaturday) 2.0 else 1.0,
        status = lRecord?.status ?: true,
        previousStatus = lRecord?.previousStatus,
        onClick = { onToggleMeal(date, MealType.LUNCH) }
    )

    val dRecord = dayMap[MealType.DINNER]
    MealCard(
        mealType = MealType.DINNER,
        value = if (isSaturday) 0.0 else 1.0,
        status = dRecord?.status ?: true,
        previousStatus = dRecord?.previousStatus,
        onClick = {
            if (!isSaturday) {
                onToggleMeal(date, MealType.DINNER)
            }
        }
    )
}

@Composable
private fun DayDetailPane(
    date: LocalDate,
    state: MessState,
    onToggleMeal: (LocalDate, MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMessColors.current
    val isDark = LocalIsNight.current
    val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
    val records = state.cycleMeals[date] ?: emptyList()
    val dayMap = records.associateBy { it.mealType }
    val meals = listOf(MealType.BREAKFAST, MealType.LUNCH) +
        if (!isSaturday) listOf(MealType.DINNER) else emptyList()
    val eatenCount = meals.count { dayMap[it]?.status ?: true }
    val dailyTotal = BillingEngine.dayLiability(date, dayMap, state.config)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = if (isDark) BorderStroke(1.dp, colors.rule) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Day detail",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textTertiary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Text(
                text = date.year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$eatenCount / ${meals.size} meals eaten",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LogMealCards(
                date = date,
                state = state,
                onToggleMeal = onToggleMeal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Daily subtotal",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary
                )
                Text(
                    text = "₹%.2f".format(dailyTotal),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
            }
        }
    }
}
