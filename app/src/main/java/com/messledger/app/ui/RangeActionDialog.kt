package com.messledger.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
    var picking by remember { mutableStateOf<RangePicker?>(null) }

    val colors = LocalMessColors.current
    val haptics = LocalHapticFeedback.current
    val dayCount = ChronoUnit.DAYS.between(start, end) + 1

    GrooveBottomSheet(
        title = "Mark a range",
        copy = "Set every meal in this span at once. Each date follows its saved day rules; by default Saturday counts lunch twice and closes dinner.",
        onDismiss = onDismiss,
        primaryLabel = "Mark eaten",
        onPrimary = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onApply(start, end, true)
        },
        secondaryLabel = "Mark skipped",
        onSecondary = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onApply(start, end, false)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RangeDateField(
                label = "From",
                date = start,
                onClick = { picking = RangePicker.START }
            )
            RangeDateField(
                label = "To",
                date = end,
                onClick = { picking = RangePicker.END }
            )

            Text(
                text = if (dayCount == 1L) "1 day included" else "$dayCount days included",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }
    }

    picking?.let { target ->
        GrooveDatePickerDialog(
            initialDate = if (target == RangePicker.START) start else end,
            onDismiss = { picking = null },
            onDateSelected = { picked ->
                when (target) {
                    RangePicker.START -> {
                        start = picked
                        if (picked.isAfter(end)) end = picked
                    }
                    RangePicker.END -> {
                        end = picked
                        if (picked.isBefore(start)) start = picked
                    }
                }
            }
        )
    }
}

private enum class RangePicker { START, END }

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
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.borderStrong)
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
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1
        )
    }
}
