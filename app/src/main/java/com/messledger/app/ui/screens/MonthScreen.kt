package com.messledger.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import com.messledger.app.domain.CalendarDayModel
import com.messledger.app.domain.MealDisplayStatus
import com.messledger.app.domain.mapCalendarMonth
import com.messledger.app.ui.MealStateDot
import com.messledger.app.ui.RangeActionDialog
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.rememberMotionDuration
import com.messledger.app.ui.theme.tabularNumbers
import com.messledger.app.viewmodel.MessState
import com.messledger.app.viewmodel.MessViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM uuuu")
private val cycleFormatter = DateTimeFormatter.ofPattern("d MMM")
private val semanticFormatter = DateTimeFormatter.ofPattern("d MMMM")

@Composable
fun MonthScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalMessColors.current
    val duration = rememberMotionDuration(120)
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }

    var rangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var rangeEnd by remember { mutableStateOf<LocalDate?>(null) }
    var showRangeSheet by remember { mutableStateOf(false) }

    val currentMonth = state.selectedDate.withDayOfMonth(1)
    val today = LocalDate.now()
    val (cycleStart, cycleEnd) = BillingEngine.cycleRange(state.selectedDate)

    val range = if (rangeStart != null) {
        (rangeStart!!) to (rangeEnd ?: rangeStart!!)
    } else null
    val daysFor: (LocalDate) -> List<CalendarDayModel> = { targetMonth ->
        mapCalendarMonth(
            month = targetMonth,
            recordsByDate = state.cycleMeals,
            config = state.config,
            range = range
        )
    }
    val rangeEdges = range?.let { (start, end) ->
        if (start <= end) start to end else end to start
    }

    fun openRange(anchor: LocalDate, end: LocalDate? = null) {
        rangeStart = anchor
        rangeEnd = end ?: anchor
        showRangeSheet = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft -> viewModel.setSelectedDate(state.selectedDate.minusDays(1))
                    Key.DirectionRight -> viewModel.setSelectedDate(state.selectedDate.plusDays(1))
                    Key.DirectionUp -> viewModel.setSelectedDate(state.selectedDate.minusDays(7))
                    Key.DirectionDown -> viewModel.setSelectedDate(state.selectedDate.plusDays(7))
                    else -> return@onKeyEvent false
                }
                true
            }
            .pointerInput(rangeStart) {
                if (rangeStart != null) return@pointerInput
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        totalDrag += amount
                    },
                    onDragEnd = {
                        if (totalDrag > swipeThreshold) {
                            viewModel.setSelectedDate(state.selectedDate.minusMonths(1))
                        } else if (totalDrag < -swipeThreshold) {
                            viewModel.setSelectedDate(state.selectedDate.plusMonths(1))
                        }
                    }
                )
            }
    ) {
        val twoPane = maxWidth >= 840.dp

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
                            .weight(1.55f)
                            .fillMaxHeight()
                    ) {
                        MonthHeader(
                            month = currentMonth,
                            cycleStart = cycleStart,
                            cycleEnd = cycleEnd,
                            onPrevious = { viewModel.setSelectedDate(state.selectedDate.minusMonths(1)) },
                            onNext = { viewModel.setSelectedDate(state.selectedDate.plusMonths(1)) },
                            onToday = { viewModel.setSelectedDate(today) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        WeekdayHeader()
                        Spacer(modifier = Modifier.height(8.dp))
                        MonthPager(
                            daysFor = daysFor,
                            month = currentMonth,
                            selectedDate = state.selectedDate,
                            today = today,
                            rangeEdges = rangeEdges,
                            duration = duration,
                            onSelect = { viewModel.setSelectedDate(it) },
                            onAnchor = { rangeStart = it; rangeEnd = it },
                            onPreview = { rangeEnd = it },
                            onRelease = { showRangeSheet = rangeStart != null },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SelectedDayPanel(
                        state = state,
                        onOpenRange = { openRange(state.selectedDate) },
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    MonthHeader(
                        month = currentMonth,
                        cycleStart = cycleStart,
                        cycleEnd = cycleEnd,
                        onPrevious = { viewModel.setSelectedDate(state.selectedDate.minusMonths(1)) },
                        onNext = { viewModel.setSelectedDate(state.selectedDate.plusMonths(1)) },
                        onToday = { viewModel.setSelectedDate(today) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    WeekdayHeader()
                    Spacer(modifier = Modifier.height(8.dp))
                    MonthPager(
                        daysFor = daysFor,
                        month = currentMonth,
                        selectedDate = state.selectedDate,
                        today = today,
                        rangeEdges = rangeEdges,
                        duration = duration,
                        onSelect = { viewModel.setSelectedDate(it) },
                        onAnchor = { rangeStart = it; rangeEnd = it },
                        onPreview = { rangeEnd = it },
                        onRelease = { showRangeSheet = rangeStart != null },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showRangeSheet && rangeStart != null) {
        RangeActionDialog(
            initialStart = rangeStart!!,
            initialEnd = rangeEnd ?: rangeStart!!,
            onDismiss = {
                showRangeSheet = false
                rangeStart = null
                rangeEnd = null
            },
            onApply = { from, to, eaten ->
                viewModel.setRangeStatus(from, to, eaten)
                viewModel.setSelectedDate(from)
                showRangeSheet = false
                rangeStart = null
                rangeEnd = null
            }
        )
    }
}

@Composable
private fun MonthHeader(
    month: LocalDate,
    cycleStart: LocalDate,
    cycleEnd: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val colors = LocalMessColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(icon = Icons.Rounded.ChevronLeft, label = "Previous month", onClick = onPrevious)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = month.format(monthFormatter),
                style = MaterialTheme.typography.headlineSmall.tabularNumbers(),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "Cycle ${cycleStart.format(cycleFormatter)} – ${cycleEnd.format(cycleFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        RoundIconButton(icon = Icons.Rounded.ChevronRight, label = "Next month", onClick = onNext)
        TextButton(
            onClick = onToday,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text("Today", color = colors.tangerine, maxLines = 1)
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalMessColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, colors.border, CircleShape)
    ) {
        Icon(icon, contentDescription = label, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WeekdayHeader() {
    val colors = LocalMessColors.current
    val style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = style,
                color = colors.textTertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthPager(
    daysFor: (LocalDate) -> List<CalendarDayModel>,
    month: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    rangeEdges: Pair<LocalDate, LocalDate>?,
    duration: Int,
    onSelect: (LocalDate) -> Unit,
    onAnchor: (LocalDate) -> Unit,
    onPreview: (LocalDate) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = month,
        transitionSpec = {
            fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
        },
        label = "month_crossfade",
        modifier = modifier
    ) { targetMonth ->
        GrooveMonthGrid(
            days = daysFor(targetMonth),
            selectedDate = selectedDate,
            today = today,
            rangeEdges = rangeEdges,
            onSelect = onSelect,
            onAnchor = onAnchor,
            onPreview = onPreview,
            onRelease = onRelease
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrooveMonthGrid(
    days: List<CalendarDayModel>,
    selectedDate: LocalDate,
    today: LocalDate,
    rangeEdges: Pair<LocalDate, LocalDate>?,
    onSelect: (LocalDate) -> Unit,
    onAnchor: (LocalDate) -> Unit,
    onPreview: (LocalDate) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val model = days.getOrNull(index) ?: return@Row
                    GrooveDayCell(
                        model = model,
                        index = index,
                        isSelected = model.date == selectedDate,
                        isToday = model.date == today,
                        isRangeStart = rangeEdges?.first == model.date,
                        isRangeEnd = rangeEdges?.second == model.date,
                        onSelect = onSelect,
                        onAnchor = onAnchor,
                        onPreview = onPreview,
                        onRelease = onRelease,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .heightIn(min = 44.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrooveDayCell(
    model: CalendarDayModel,
    index: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isRangeStart: Boolean,
    isRangeEnd: Boolean,
    onSelect: (LocalDate) -> Unit,
    onAnchor: (LocalDate) -> Unit,
    onPreview: (LocalDate) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMessColors.current

    val container = when {
        model.inRange -> colors.tangerine.copy(alpha = 0.12f)
        isSelected -> colors.elevated
        else -> colors.surface
    }
    val borderColor = when {
        isSelected || isToday || isRangeStart || isRangeEnd -> colors.tangerine
        model.inRange -> colors.tangerine.copy(alpha = 0.35f)
        else -> colors.border
    }

    val description = buildString {
        append(model.date.format(semanticFormatter))
        append(", ")
        append(
            MealType.values().joinToString(", ") { meal ->
                "${meal.name.lowercase()} ${model.statuses[meal]?.name?.lowercase() ?: "closed"}"
            }
        )
        if (model.guestCount > 0) {
            append(", ${model.guestCount} guest${if (model.guestCount == 1) "" else "s"}")
        }
        if (model.revised) append(", revised")
    }

    Box(
        modifier = modifier
            .background(container)
            .border(1.dp, borderColor)
            .combinedClickable(
                onClick = { onSelect(model.date) },
                onLongClick = { onAnchor(model.date) }
            )
            .pointerInput(model.date, index) {
                var start = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        start = offset
                        onAnchor(model.date)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val cellWidth = size.width.toFloat().coerceAtLeast(1f)
                        val cellHeight = size.height.toFloat().coerceAtLeast(1f)
                        val colDelta = ((change.position.x - start.x) / cellWidth).roundToInt()
                        val rowDelta = ((change.position.y - start.y) / cellHeight).roundToInt()
                        val target = (index + rowDelta * 7 + colDelta).coerceIn(0, 41)
                        onPreview(model.date.plusDays((target - index).toLong()))
                    },
                    onDragEnd = { onRelease() },
                    onDragCancel = { onRelease() }
                )
            }
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${model.date.dayOfMonth}",
                    style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                    color = if (model.inMonth) colors.textPrimary else colors.textTertiary.copy(alpha = 0.35f),
                    maxLines = 1
                )
                if (model.inMonth && model.guestCount > 0) {
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "+${model.guestCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                }
            }

            if (model.inMonth) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MealType.values().forEach { meal ->
                        val status = model.statuses[meal] ?: MealDisplayStatus.CLOSED
                        MealTick(
                            status = status == MealDisplayStatus.EATEN,
                            isClosed = status == MealDisplayStatus.CLOSED,
                            recorded = status != MealDisplayStatus.UNTOUCHED,
                            tickSize = 9.dp,
                            containerColor = container
                        )
                    }
                }
            }
        }

        if (isToday && model.inMonth) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(3.dp)
                    .background(colors.tangerine, CircleShape)
            )
        }

        if (model.inMonth && model.revised) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(5.dp)
                    .drawBehind {
                        drawLine(
                            color = colors.tangerine,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            )
        }
    }
}

/** The skipped slash is cut with the resolved cell container colour. */
@Composable
fun MealTick(
    status: Boolean,
    isClosed: Boolean,
    tickSize: Dp = 8.dp,
    recorded: Boolean = true,
    containerColor: Color? = null
) {
    MealStateDot(
        status = status,
        recorded = recorded,
        closed = isClosed,
        modifier = Modifier.size(tickSize),
        containerColor = containerColor
    )
}

@Composable
private fun SelectedDayPanel(
    state: MessState,
    onOpenRange: () -> Unit,
    onGuestChange: (MealType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMessColors.current
    val date = state.selectedDate
    val rule = BillingEngine.ruleFor(date, state.config)
    val records = state.cycleMeals[date] ?: emptyList()
    val dayMap = records.associateBy { it.mealType }

    val meals = buildList {
        if (rule.breakfast) add(MealType.BREAKFAST)
        if (rule.lunchPortions > 0) add(MealType.LUNCH)
        if (rule.dinner) add(MealType.DINNER)
    }
    val closedMeals = MealType.values().filter { it !in meals }
    val eatenCount = meals.count { dayMap[it]?.status ?: true }
    val dailyTotal = BillingEngine.dayLiability(date, dayMap, state.config)

    Box(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
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
                text = date.format(semanticFormatter),
                style = MaterialTheme.typography.titleLarge.tabularNumbers(),
                color = colors.textPrimary
            )
            Text(
                text = date.year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onOpenRange,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Mark a range", color = colors.tangerine)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$eatenCount / ${meals.size} meals eaten",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            meals.forEach { meal ->
                val record = dayMap[meal]
                val eaten = record?.status ?: true
                val guests = record?.guestCount ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealTick(
                        status = eaten,
                        isClosed = false,
                        recorded = record != null,
                        tickSize = 10.dp,
                        containerColor = colors.surface
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                        if (guests > 0) {
                            Text(
                                text = "+$guests guest portion${if (guests == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )
                        }
                    }

                    if (guests > 0) {
                        IconButton(onClick = { onGuestChange(meal, -1) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Rounded.Remove,
                                contentDescription = "Remove guest portion",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = { onGuestChange(meal, 1) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add guest portion",
                            tint = colors.tangerine,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = when {
                            record == null -> "Untouched"
                            eaten -> "Eaten"
                            else -> "Skipped"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            record == null -> colors.textTertiary
                            eaten -> colors.success
                            else -> colors.warning
                        }
                    )
                }
            }

            closedMeals.forEach { meal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealTick(status = true, isClosed = true, tickSize = 10.dp, containerColor = colors.surface)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = meal.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Closed", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
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
                style = MaterialTheme.typography.headlineMedium.tabularNumbers(),
                color = colors.textPrimary
            )
        }
    }
}
