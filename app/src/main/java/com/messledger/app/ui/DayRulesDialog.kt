package com.messledger.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.messledger.app.data.DayRule
import com.messledger.app.domain.BillingEngine
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayRuleFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

/**
 * A single-date rules editor, kept out of the day, month and log pages. The
 * weekday defaults are shown as a starting point but are not enforced: any
 * date can close a meal or turn lunch into a feast portion.
 */
@Composable
fun DayRulesDialog(
    state: MessState,
    onDismiss: () -> Unit,
    onSave: (LocalDate, DayRule?) -> Unit
) {
    val colors = LocalMessColors.current
    val context = LocalContext.current
    var date by remember { mutableStateOf(state.selectedDate) }

    val defaultRule = BillingEngine.defaultRule(date)
    val savedRule = state.config.dayRules[date.toString()]

    var breakfast by remember(date, savedRule) {
        mutableStateOf(savedRule?.breakfast ?: defaultRule.breakfast)
    }
    var lunchPortions by remember(date, savedRule) {
        mutableStateOf(savedRule?.lunchPortions ?: defaultRule.lunchPortions)
    }
    var dinner by remember(date, savedRule) {
        mutableStateOf(savedRule?.dinner ?: defaultRule.dinner)
    }

    fun pickDate() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth -> date = LocalDate.of(year, month + 1, dayOfMonth) },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        ).show()
    }

    fun summary(rule: DayRule): String {
        val meals = buildList {
            add(if (rule.breakfast) "breakfast" else "no breakfast")
            add(
                when (rule.lunchPortions) {
                    0 -> "no lunch"
                    2 -> "feast lunch"
                    else -> "lunch"
                }
            )
            add(if (rule.dinner) "dinner" else "no dinner")
        }
        return meals.joinToString(" · ")
    }

    val savedDates = state.config.dayRules.keys.sortedDescending()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("Day rules") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "The mess usually follows the weekday, but a festival or holiday can move a feast lunch or close a meal on any date. Saved rules apply to that date only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = date.format(dayRuleFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Default: ${summary(defaultRule)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary
                        )
                    }
                    IconButton(onClick = { pickDate() }) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = "Choose a date",
                            tint = colors.brass
                        )
                    }
                }

                RuleSwitch(
                    label = "Breakfast served",
                    checked = breakfast,
                    onCheckedChange = { breakfast = it }
                )

                Text(
                    text = "Lunch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Closed", 1 to "Single", 2 to "Feast").forEach { (value, label) ->
                        FilterChip(
                            selected = lunchPortions == value,
                            onClick = { lunchPortions = value },
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    text = "A single portion is billed once; a feast lunch is billed as two portions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )

                RuleSwitch(
                    label = "Dinner served",
                    checked = dinner,
                    onCheckedChange = { dinner = it }
                )

                if (savedDates.isNotEmpty()) {
                    HorizontalDivider(color = colors.rule)
                    Text(
                        text = "Saved dates",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textTertiary
                    )
                    savedDates.forEach { key ->
                        val rule = state.config.dayRules[key] ?: return@forEach
                        val savedDate = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@forEach
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = savedDate.format(dayRuleFormatter),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = summary(rule),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textTertiary
                                )
                            }
                            IconButton(onClick = { onSave(savedDate, null) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove rule for $savedDate",
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(date, DayRule(breakfast, lunchPortions, dinner))
                    onDismiss()
                }
            ) {
                Text("Save for this day", color = colors.brass)
            }
        },
        dismissButton = {
            Row {
                if (savedRule != null) {
                    TextButton(onClick = { onSave(date, null) }) {
                        Text("Use default", color = colors.textSecondary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = colors.textSecondary)
                }
            }
        }
    )
}

@Composable
private fun RuleSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalMessColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
