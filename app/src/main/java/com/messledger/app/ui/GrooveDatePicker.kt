package com.messledger.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.LocalMessColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material 3 date picker, recoloured from the same tokens as the rest of the grove. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrooveDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val colors = LocalMessColors.current
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        colors = DatePickerDefaults.colors(
            containerColor = colors.elevated,
            titleContentColor = colors.textPrimary,
            headlineContentColor = colors.textPrimary,
            weekdayContentColor = colors.textTertiary,
            subheadContentColor = colors.textSecondary,
            navigationContentColor = colors.textSecondary,
            yearContentColor = colors.textSecondary,
            currentYearContentColor = colors.tangerine,
            selectedYearContentColor = colors.onTangerine,
            selectedYearContainerColor = colors.tangerine,
            dayContentColor = colors.textPrimary,
            selectedDayContentColor = colors.onTangerine,
            selectedDayContainerColor = colors.tangerine,
            todayContentColor = colors.tangerine,
            todayDateBorderColor = colors.tangerine,
            dividerColor = colors.border
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = colors.tangerine)
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = state)
    }
}
