package com.messledger.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import com.messledger.app.ui.RangeActionDialog
import com.messledger.app.ui.theme.LocalIsNight
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessState
import com.messledger.app.viewmodel.MessViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MonthScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalMessColors.current
    val isDark = LocalIsNight.current
    var rangeStart by remember { mutableStateOf<LocalDate?>(null) }

    val currentMonth = state.selectedDate.withDayOfMonth(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.dayOfWeek.value % 7 // Sunday = 0

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 840.dp
        val narrow = maxWidth < 360.dp

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
                            .weight(1.55f)
                            .fillMaxHeight()
                    ) {
                        MonthHeader(
                            month = currentMonth,
                            narrow = narrow,
                            onPrevious = { viewModel.setSelectedDate(state.selectedDate.minusMonths(1)) },
                            onNext = { viewModel.setSelectedDate(state.selectedDate.plusMonths(1)) },
                            onOpenRange = { rangeStart = state.selectedDate }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        WeekdayHeader(isDark = isDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        MonthGrid(
                            currentMonth = currentMonth,
                            daysInMonth = daysInMonth,
                            firstDayOfWeek = firstDayOfWeek,
                            state = state,
                            isDark = isDark,
                            onSelect = { viewModel.setSelectedDate(it) },
                            onLongPress = { rangeStart = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SelectedDayPanel(
                        state = state,
                        isDark = isDark,
                        onGuestChange = { meal, delta ->
                            viewModel.changeGuestCount(state.selectedDate, meal, delta)
                        },
                        modifier = Modifier
                            .weight(1f)
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
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    MonthHeader(
                        month = currentMonth,
                        narrow = narrow,
                        onPrevious = { viewModel.setSelectedDate(state.selectedDate.minusMonths(1)) },
                        onNext = { viewModel.setSelectedDate(state.selectedDate.plusMonths(1)) },
                        onOpenRange = { rangeStart = state.selectedDate }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    WeekdayHeader(isDark = isDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    MonthGrid(
                        currentMonth = currentMonth,
                        daysInMonth = daysInMonth,
                        firstDayOfWeek = firstDayOfWeek,
                        state = state,
                        isDark = isDark,
                        onSelect = { viewModel.setSelectedDate(it) },
                        onLongPress = { rangeStart = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    rangeStart?.let { start ->
        RangeActionDialog(
            initialStart = start,
            onDismiss = { rangeStart = null },
            onApply = { from, to, eaten ->
                viewModel.setRangeStatus(from, to, eaten)
                viewModel.setSelectedDate(from)
                rangeStart = null
            }
        )
    }
}

@Composable
private fun MonthHeader(
    month: LocalDate,
    narrow: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenRange: () -> Unit
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
        Text(
            text = "${month.month} ${month.year}",
            style = if (narrow) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        TextButton(
            onClick = onNext,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(if (narrow) "▶" else "Next ▶", color = colors.textPrimary)
        }
        TextButton(
            onClick = onOpenRange,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(if (narrow) "⇔" else "Range", color = colors.textSecondary)
        }
    }
}

@Composable
private fun WeekdayHeader(isDark: Boolean) {
    val colors = LocalMessColors.current

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
            Text(
                text = it,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) colors.textTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthGrid(
    currentMonth: LocalDate,
    daysInMonth: Int,
    firstDayOfWeek: Int,
    state: MessState,
    isDark: Boolean,
    onSelect: (LocalDate) -> Unit,
    onLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMessColors.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(firstDayOfWeek) {
            Spacer(modifier = Modifier.aspectRatio(1f))
        }

        items(daysInMonth) { dayIndex ->
            val date = currentMonth.plusDays(dayIndex.toLong())
            val records = state.cycleMeals[date] ?: emptyList()
            val dayMap = records.associateBy { it.mealType }
            val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
            val isSelected = date == state.selectedDate

            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .combinedClickable(
                        onClick = { onSelect(date) },
                        onLongClick = { onLongPress(date) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Ticks and type grow with the cell, so a 320dp phone and a
                // 1280dp tablet both read as the same calendar.
                val cellWidth = maxWidth
                val tickSize = (cellWidth * 0.17f).coerceIn(6.dp, 11.dp)
                val numberStyle = when {
                    cellWidth < 42.dp -> MaterialTheme.typography.bodySmall
                    cellWidth > 62.dp -> MaterialTheme.typography.bodyLarge
                    else -> MaterialTheme.typography.bodyMedium
                }

                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSelected && isDark -> colors.rule.copy(alpha = 0.35f)
                            isDark -> colors.surface
                            else -> CardDefaults.cardColors().containerColor
                        }
                    ),
                    border = when {
                        isSelected -> BorderStroke(1.dp, if (isDark) colors.brass else colors.accentRed)
                        isDark -> BorderStroke(1.dp, colors.rule)
                        else -> null
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${dayIndex + 1}",
                            style = numberStyle,
                            color = colors.textPrimary,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(if (cellWidth < 44.dp) 2.dp else 4.dp))

                        // Visual ticks for B L D
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MealTick(
                                status = dayMap[MealType.BREAKFAST]?.status ?: true,
                                isClosed = false,
                                tickSize = tickSize
                            )
                            MealTick(
                                status = dayMap[MealType.LUNCH]?.status ?: true,
                                isClosed = false,
                                tickSize = tickSize
                            )
                            MealTick(
                                status = dayMap[MealType.DINNER]?.status ?: true,
                                isClosed = isSaturday,
                                tickSize = tickSize
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayPanel(
    state: MessState,
    isDark: Boolean,
    onGuestChange: (MealType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMessColors.current
    val date = state.selectedDate
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
                text = "Selected day",
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$eatenCount / ${meals.size} meals eaten",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            meals.forEach { meal ->
                val eaten = dayMap[meal]?.status ?: true
                val guests = dayMap[meal]?.guestCount ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealTick(status = eaten, isClosed = false, tickSize = 10.dp)
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                        if (guests > 0) {
                            Text(
                                text = "$guests guest portion${if (guests == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.brass
                            )
                        }
                    }

                    if (guests > 0) {
                        IconButton(
                            onClick = { onGuestChange(meal, -1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Remove guest portion",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onGuestChange(meal, 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add guest portion",
                            tint = colors.brass,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = if (eaten) "Eaten" else "Skipped",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (eaten) colors.eaten else colors.missed
                    )
                }
            }

            if (isSaturday) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealTick(status = true, isClosed = true, tickSize = 10.dp)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "Dinner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Closed",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Daily subtotal",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary
            )
            Text(
                text = "₹%.2f".format(dailyTotal),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
fun MealTick(
    status: Boolean,
    isClosed: Boolean,
    tickSize: Dp = 8.dp
) {
    val colors = LocalMessColors.current
    Canvas(modifier = Modifier.size(tickSize)) {
        if (isClosed) {
            drawLine(
                color = colors.textTertiary.copy(alpha = 0.5f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f
            )
        } else if (status) {
            drawCircle(color = colors.eaten, radius = size.minDimension / 2)
        } else {
            // A missed meal: a filled dot cut by the card's own paper.
            drawCircle(color = colors.missed, radius = size.minDimension / 2)
            drawLine(
                color = colors.surface,
                start = Offset(size.width * 0.15f, size.height * 0.15f),
                end = Offset(size.width * 0.85f, size.height * 0.85f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
