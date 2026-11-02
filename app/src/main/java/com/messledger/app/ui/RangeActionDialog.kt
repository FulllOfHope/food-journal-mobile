package com.messledger.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.LocalMessColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val rangeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

/**
 * A range editor for holidays and exam breaks. It is a deliberate two-step
 * action: choose the dates, then say whether the mess was eaten or skipped.
 * Nothing changes until one of the two apply buttons is tapped.
 */
@Composable
fun RangeActionDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate = initialStart,
    onDismiss: () -> Unit,
    onApply: (start: LocalDate, end: LocalDate, eaten: Boolean) -> Unit
) {
    var start by remember(initialStart, initialEnd) { mutableStateOf(initialStart) }
    var end by remember(initialStart, initialEnd) { mutableStateOf(initialEnd) }

    val context = LocalContext.current
    val colors = LocalMessColors.current
    val haptics = LocalHapticFeedback.current

    fun pick(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth -> onPicked(LocalDate.of(year, month + 1, dayOfMonth)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    val dayCount = ChronoUnit.DAYS.between(start, end) + 1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("Mark a range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set every meal in this span at once. Saturday still counts lunch twice and closes dinner.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                RangeDateField(
                    label = "From",
                    date = start,
                    onClick = {
                        pick(start) { picked ->
                            start = picked
                            if (picked.isAfter(end)) end = picked
                        }
                    }
                )
                RangeDateField(
                    label = "To",
                    date = end,
                    onClick = {
                        pick(end) { picked ->
                            end = picked
                            if (picked.isBefore(start)) start = picked
                        }
                    }
                )

                Text(
                    text = if (dayCount == 1L) "1 day included" else "$dayCount days included",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brass
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onApply(start, end, false)
                    }
                ) {
                    Text("Mark skipped", color = colors.missed)
                }
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onApply(start, end, true)
                    }
                ) {
                    Text("Mark eaten", color = colors.eaten)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun RangeDateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit
) {
    val colors = LocalMessColors.current

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, colors.ruleDark)
    ) {
        Icon(
            imageVector = Icons.Rounded.EditCalendar,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = date.format(rangeFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
