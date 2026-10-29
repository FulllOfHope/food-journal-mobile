package com.messledger.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.messledger.app.data.MealType
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessViewModel
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun MonthScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalMessColors.current
    val isDark = isSystemInDarkTheme()
    
    val currentMonth = state.selectedDate.withDayOfMonth(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.dayOfWeek.value % 7 // Sunday = 0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        // Month Navigator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.setSelectedDate(state.selectedDate.minusMonths(1)) }) {
                Text("◀ Prev", color = colors.textPrimary)
            }
            Text(
                text = "${currentMonth.month} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            TextButton(onClick = { viewModel.setSelectedDate(state.selectedDate.plusMonths(1)) }) {
                Text("Next ▶", color = colors.textPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) colors.textTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            items(firstDayOfWeek) {
                Spacer(modifier = Modifier.padding(8.dp))
            }
            
            items(daysInMonth) { dayIndex ->
                val date = currentMonth.plusDays(dayIndex.toLong())
                val records = state.cycleMeals[date] ?: emptyList()
                val dayMap = records.associateBy { it.mealType }
                val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) colors.surface
                            else CardDefaults.cardColors().containerColor
                        ),
                        border = if (isDark) BorderStroke(1.dp, colors.rule) else null
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${dayIndex + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Visual ticks for B L D
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                MealTick(
                                    status = dayMap[MealType.BREAKFAST]?.status ?: true,
                                    isClosed = false
                                )
                                MealTick(
                                    status = dayMap[MealType.LUNCH]?.status ?: true,
                                    isClosed = false
                                )
                                MealTick(
                                    status = dayMap[MealType.DINNER]?.status ?: true,
                                    isClosed = isSaturday
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealTick(status: Boolean, isClosed: Boolean) {
    val colors = LocalMessColors.current
    Canvas(modifier = Modifier.size(8.dp)) {
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
