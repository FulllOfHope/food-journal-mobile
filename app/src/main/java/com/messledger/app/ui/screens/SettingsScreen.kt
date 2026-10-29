package com.messledger.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.messledger.app.data.Config
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import com.messledger.app.export.DataExportManager
import com.messledger.app.export.DataImporter
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.AllowanceVisualizer
import com.messledger.app.viewmodel.MessViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

@Composable
fun SettingsScreen(viewModel: MessViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalMessColors.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Insights & Analytics", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { DataExportManager.triggerExportIntent(exportLauncher) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Export CSV")
            }
            OutlinedButton(
                onClick = {
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
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.ruleDark)
            ) {
                Text("Import CSV")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Liability to Date", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                Text(
                    text = "₹%.2f".format(state.batch1Liability + state.batch2Liability),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Batch 1 (3rd-17th)", style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                        Text("₹%.2f".format(state.batch1Liability), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = androidx.compose.ui.Alignment.End
                    ) {
                        Text("Batch 2 (18th-2nd)", style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                        Text("₹%.2f".format(state.batch2Liability), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Allowance Box", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        
        AllowanceVisualizer(
            allowanceTotal = state.config.allowedMisses,
            consumed = state.cycleSkippedCount
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Billing Settings", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.ruleDark,
            unfocusedBorderColor = colors.rule,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.textSecondary,
            unfocusedLabelColor = colors.textTertiary
        )
        
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

        OutlinedTextField(value = bPrice, onValueChange = { bPrice = it; onSave() }, label = { Text("Breakfast Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = lPrice, onValueChange = { lPrice = it; onSave() }, label = { Text("Lunch Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = dPrice, onValueChange = { dPrice = it; onSave() }, label = { Text("Dinner Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = tax, onValueChange = { tax = it; onSave() }, label = { Text("Tax (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = allowance, onValueChange = { allowance = it; onSave() }, label = { Text("Allowed Misses") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = textFieldColors, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(80.dp))
    }
}
