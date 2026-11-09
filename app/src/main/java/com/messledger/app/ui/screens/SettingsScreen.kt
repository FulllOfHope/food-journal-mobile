package com.messledger.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messledger.app.data.Config
import com.messledger.app.export.DataExportManager
import com.messledger.app.export.DataImporter
import com.messledger.app.ui.AllowanceVisualizer
import com.messledger.app.ui.DayRulesDialog
import com.messledger.app.ui.ReceiptDialog
import com.messledger.app.ui.theme.LocalIsNight
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.MessColors
import com.messledger.app.viewmodel.MessState
import com.messledger.app.viewmodel.MessViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalMessColors.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReceipt by remember { mutableStateOf(false) }
    var showDayRules by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    val records = viewModel.allRecords()
                    withContext(Dispatchers.IO) {
                        DataExportManager.writeDataToUri(context, uri, records, state.config)
                    }
                    Toast.makeText(
                        context,
                        "Exported ${records.size} meal entries",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not open that file")
                }.mapCatching { DataImporter.parseCsv(it) }
            }

            parsed.onSuccess { result ->
                if (result.error != null) {
                    Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                } else {
                    viewModel.importMeals(result.records)
                    Toast.makeText(
                        context,
                        "Imported ${result.days} days (${result.records.size} meals)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure {
                Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var bPrice by remember(state.config.priceBreakfast) { mutableStateOf(state.config.priceBreakfast.toString()) }
    var lPrice by remember(state.config.priceLunch) { mutableStateOf(state.config.priceLunch.toString()) }
    var dPrice by remember(state.config.priceDinner) { mutableStateOf(state.config.priceDinner.toString()) }
    var tax by remember(state.config.taxPercent) { mutableStateOf(state.config.taxPercent.toString()) }
    var allowance by remember(state.config.allowedMisses) { mutableStateOf(state.config.allowedMisses.toString()) }

    val onSave = {
        viewModel.updateConfig(
            Config(
                priceBreakfast = bPrice.toFloatOrNull() ?: state.config.priceBreakfast,
                priceLunch = lPrice.toFloatOrNull() ?: state.config.priceLunch,
                priceDinner = dPrice.toFloatOrNull() ?: state.config.priceDinner,
                taxPercent = tax.toFloatOrNull() ?: state.config.taxPercent,
                allowedMisses = allowance.toIntOrNull() ?: state.config.allowedMisses
            )
        )
    }

    val onExport = { DataExportManager.triggerExportIntent(exportLauncher) }
    val onImport = {
        importLauncher.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/plain",
                "application/csv",
                "application/vnd.ms-excel",
                "application/octet-stream"
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 900.dp
        val scroll = rememberScrollState()

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
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                    ) {
                        InsightsSection(
                            state = state,
                            onExport = onExport,
                            onImport = onImport,
                            onReceipt = { showReceipt = true }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        DayRulesSection(
                            state = state,
                            onOpen = { showDayRules = true }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        AllowanceSection(state = state)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                    ) {
                        BillingSection(
                            bPrice = bPrice,
                            lPrice = lPrice,
                            dPrice = dPrice,
                            tax = tax,
                            allowance = allowance,
                            colors = colors,
                            onBreakfastChange = { bPrice = it; onSave() },
                            onLunchChange = { lPrice = it; onSave() },
                            onDinnerChange = { dPrice = it; onSave() },
                            onTaxChange = { tax = it; onSave() },
                            onAllowanceChange = { allowance = it; onSave() }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        AboutSection()
                    }
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
                        .verticalScroll(scroll)
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    InsightsSection(
                        state = state,
                        onExport = onExport,
                        onImport = onImport,
                        onReceipt = { showReceipt = true }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    DayRulesSection(
                        state = state,
                        onOpen = { showDayRules = true }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    AllowanceSection(state = state)
                    Spacer(modifier = Modifier.height(28.dp))
                    BillingSection(
                        bPrice = bPrice,
                        lPrice = lPrice,
                        dPrice = dPrice,
                        tax = tax,
                        allowance = allowance,
                        colors = colors,
                        onBreakfastChange = { bPrice = it; onSave() },
                        onLunchChange = { lPrice = it; onSave() },
                        onDinnerChange = { dPrice = it; onSave() },
                        onTaxChange = { tax = it; onSave() },
                        onAllowanceChange = { allowance = it; onSave() }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    AboutSection()
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showReceipt) {
        ReceiptDialog(state = state, onDismiss = { showReceipt = false })
    }

    if (showDayRules) {
        DayRulesDialog(
            state = state,
            onDismiss = { showDayRules = false },
            onSave = { date, rule -> viewModel.setDayRule(date, rule) }
        )
    }
}

@Composable
private fun DayRulesSection(
    state: MessState,
    onOpen: () -> Unit
) {
    val colors = LocalMessColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        LedgerHeading(
            text = "Day Rules",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The weekday rules stay the default, but one date can differ: move the feast lunch to a Sunday, close dinner for a festival, or restore the default at any time.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpen,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = colors.textPrimary
            ),
            border = BorderStroke(1.dp, colors.ruleDark)
        ) {
            Text("Custom day rules")
        }
        if (state.config.dayRules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.config.dayRules.size} date${if (state.config.dayRules.size == 1) "" else "s"} overridden",
                style = MaterialTheme.typography.labelSmall,
                color = colors.brass
            )
        }
    }
}

@Composable
private fun InsightsSection(
    state: MessState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReceipt: () -> Unit
) {
    val colors = LocalMessColors.current
    val isDark = LocalIsNight.current

    Column(modifier = Modifier.fillMaxWidth()) {
        LedgerHeading(
            text = "Insights & Analytics",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Export CSV", maxLines = 1)
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Import CSV", maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onReceipt,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = colors.brass
            ),
            border = BorderStroke(1.dp, colors.ruleDark)
        ) {
            Text("Bill Receipt & Share")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = if (isDark) BorderStroke(1.dp, colors.rule) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Liability to Date",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = "₹%.2f".format(state.batch1Liability + state.batch2Liability),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batch 1 (3rd-17th)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary
                        )
                        Text(
                            text = "₹%.2f".format(state.batch1Liability),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Batch 2 (18th-2nd)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary
                        )
                        Text(
                            text = "₹%.2f".format(state.batch2Liability),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllowanceSection(state: MessState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LedgerHeading(
            text = "Allowance Box",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AllowanceVisualizer(
                allowanceTotal = state.config.allowedMisses,
                consumed = state.cycleSkippedCount,
                modifier = Modifier.widthIn(max = 420.dp)
            )
        }
    }
}

@Composable
private fun BillingSection(
    bPrice: String,
    lPrice: String,
    dPrice: String,
    tax: String,
    allowance: String,
    colors: MessColors,
    onBreakfastChange: (String) -> Unit,
    onLunchChange: (String) -> Unit,
    onDinnerChange: (String) -> Unit,
    onTaxChange: (String) -> Unit,
    onAllowanceChange: (String) -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.ruleDark,
        unfocusedBorderColor = colors.rule,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedLabelColor = colors.textSecondary,
        unfocusedLabelColor = colors.textTertiary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        LedgerHeading(
            text = "Billing Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bPrice,
            onValueChange = onBreakfastChange,
            label = { Text("Breakfast Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = lPrice,
            onValueChange = onLunchChange,
            label = { Text("Lunch Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dPrice,
            onValueChange = onDinnerChange,
            label = { Text("Dinner Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = tax,
            onValueChange = onTaxChange,
            label = { Text("Tax (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = allowance,
            onValueChange = onAllowanceChange,
            label = { Text("Allowed Misses") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Prices apply to every day in the cycle, including days already past.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary
        )
    }
}

/** Serif heading with a double hairline rule on the night ledger. */
@Composable
private fun LedgerHeading(text: String, style: TextStyle) {
    val colors = LocalMessColors.current
    val isDark = LocalIsNight.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = style,
            color = colors.textPrimary
        )
        if (isDark) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .drawBehind {
                        drawLine(
                            color = colors.ruleDark,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = colors.rule.copy(alpha = 0.7f),
                            start = Offset(0f, 2.dp.toPx()),
                            end = Offset(size.width, 2.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            )
        }
    }
}

/**
 * Version, a short description, and one very small easter egg: the warden's
 * seal only approves the ledger after five taps.
 */
@Composable
private fun AboutSection() {
    val colors = LocalMessColors.current
    val isDark = LocalIsNight.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.5.1"
    }

    var taps by remember { mutableIntStateOf(0) }
    val stamped = taps >= 5

    Column(modifier = Modifier.fillMaxWidth()) {
        LedgerHeading(
            text = "About",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = if (isDark) BorderStroke(1.dp, colors.rule) else null
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mess Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "v$version",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A quiet ledger for meals, rebates, and the 3rd-of-the-month bill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = if (stamped) colors.brass else colors.ruleDark,
                                cornerRadius = CornerRadius(8.dp.toPx()),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(10f, 8f)
                                    )
                                )
                            )
                        }
                        .clickable {
                            if (!stamped) {
                                taps++
                                if (taps == 5) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (stamped) {
                        Row(
                            modifier = Modifier.rotate(-6f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = colors.brass,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "WARDEN APPROVED",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 2.sp,
                                color = colors.brass
                            )
                        }
                    } else {
                        Text(
                            text = "The warden's stamp",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (stamped) {
                        "The warden has looked the other way."
                    } else {
                        "Tap the seal five times."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary
                )
            }
        }
    }
}
