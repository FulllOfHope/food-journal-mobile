package com.messledger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messledger.app.export.ReceiptData
import com.messledger.app.export.ReceiptSharer
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessState
import java.time.LocalDate

/**
 * The bill as the warden would hand it over: a compact, printable summary of
 * the cycle with a shareable PNG and a plain-text fallback.
 */
@Composable
fun ReceiptDialog(
    state: MessState,
    onDismiss: () -> Unit
) {
    val colors = LocalMessColors.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val data = remember(state) {
        ReceiptData(
            cycleStart = state.cycleStart,
            cycleEnd = state.cycleEnd,
            daysElapsed = state.cycleDaysElapsed,
            mealsBilled = state.cycleMealsBilled,
            guestPortions = state.cycleGuests,
            rebatesUsed = state.cycleSkippedCount,
            allowance = state.config.allowedMisses,
            subtotal = state.cycleSubtotal,
            tax = state.cycleTax,
            total = state.cycleTotal
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Column {
                Text("Bill receipt", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${formatRange(data.cycleStart)} — ${formatRange(data.cycleEnd)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ReceiptRow("Days so far", data.daysElapsed.toString())
                ReceiptRow("Meal portions billed", formatUnits(data.mealsBilled))
                ReceiptRow("Guest portions", data.guestPortions.toString())
                ReceiptRow("Rebates used", "${formatUnits(data.rebatesUsed)} of ${data.allowance}")

                ReceiptRule()
                ReceiptRow("Subtotal", "₹%.2f".format(data.subtotal))
                ReceiptRow("GST ${"%.1f".format(state.config.taxPercent)}%", "₹%.2f".format(data.tax))

                ReceiptRule(double = true)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "TOTAL DUE",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.brass,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "₹%.2f".format(data.total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = "Includes every elapsed day of the cycle. Portions follow each date's rules; by default Saturday lunch is billed as two and Saturday dinner is closed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    ReceiptSharer.shareImage(context, data, state.config)
                }
            ) {
                Text("Share PNG", color = colors.brass)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        ReceiptSharer.shareText(context, data)
                    }
                ) {
                    Text("Share text", color = colors.textSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = colors.textSecondary)
                }
            }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    val colors = LocalMessColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ReceiptRule(double: Boolean = false) {
    val colors = LocalMessColors.current

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.ruleDark)
        )
        if (double) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.rule)
            )
        }
    }
}

private fun formatRange(date: LocalDate): String =
    date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))

private fun formatUnits(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
