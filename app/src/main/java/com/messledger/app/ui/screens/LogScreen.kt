package com.messledger.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.ui.MealCard
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessViewModel
import java.time.DayOfWeek
import java.time.LocalDate

import androidx.compose.foundation.lazy.rememberLazyListState
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

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = {
                val current = state.selectedDate
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        // DatePickerDialog month is 0-indexed
                        val targetDate = LocalDate.of(year, month + 1, dayOfMonth)
                        viewModel.setSelectedDate(targetDate)
                        expandedDate = targetDate
                        val index = sortedDates.indexOf(targetDate)
                        if (index != -1) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                    },
                    current.year,
                    current.monthValue - 1,
                    current.dayOfMonth
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colors.textPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.ruleDark)
        ) {
            Text("Jump to Date")
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(sortedDates) { date ->
                val records = state.cycleMeals[date] ?: emptyList()
                val isExpanded = expandedDate == date
                val eatenCount = records.count { it.status }
                val maxEaten = if (date.dayOfWeek == DayOfWeek.SATURDAY) 2 else 3
                val fullyEaten = eatenCount == maxEaten
                
                Row(modifier = Modifier
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
                            .clickable { expandedDate = if (isExpanded) null else date }
                    ) {
                        Text(
                            text = date.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        
                        Text(
                            text = "$eatenCount/$maxEaten meals eaten",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary
                        )
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                    val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
                                    val dayMap = records.associateBy { it.mealType }
                                    
                                    val bRecord = dayMap[MealType.BREAKFAST]
                                    MealCard(
                                        mealType = MealType.BREAKFAST,
                                        value = 0.5,
                                        status = bRecord?.status ?: true,
                                        previousStatus = bRecord?.previousStatus,
                                        onClick = { 
                                            viewModel.setSelectedDate(date)
                                            viewModel.toggleMeal(MealType.BREAKFAST) 
                                        }
                                    )
                                    
                                    val lRecord = dayMap[MealType.LUNCH]
                                    MealCard(
                                        mealType = MealType.LUNCH,
                                        value = if (isSaturday) 2.0 else 1.0,
                                        status = lRecord?.status ?: true,
                                        previousStatus = lRecord?.previousStatus,
                                        onClick = { 
                                            viewModel.setSelectedDate(date)
                                            viewModel.toggleMeal(MealType.LUNCH) 
                                        }
                                    )
                                    
                                    val dRecord = dayMap[MealType.DINNER]
                                    MealCard(
                                        mealType = MealType.DINNER,
                                        value = if (isSaturday) 0.0 else 1.0,
                                        status = dRecord?.status ?: true,
                                        previousStatus = dRecord?.previousStatus,
                                        onClick = { 
                                            if (!isSaturday) {
                                                viewModel.setSelectedDate(date)
                                                viewModel.toggleMeal(MealType.DINNER)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}