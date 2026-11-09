package com.messledger.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.messledger.app.data.Config
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import java.io.OutputStreamWriter
import java.time.LocalDate

object DataExportManager {

    fun triggerExportIntent(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "mess_ledger_export_${LocalDate.now()}.csv")
        }
        launcher.launch(intent)
    }

    fun writeDataToUri(context: Context, uri: Uri, records: List<MealRecord>, config: Config) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                // Write Header
                writer.write("Date,Weekday,Breakfast,Lunch,Dinner,Breakfast Guests,Lunch Guests,Dinner Guests,Meals Eaten,Missed,Bill\n")

                // Group by date
                val grouped = records.groupBy { it.date }.toSortedMap()

                for ((dateStr, dayRecords) in grouped) {
                    val date = LocalDate.parse(dateStr)
                    val dayMap = dayRecords.associateBy { it.mealType }

                    val rule = BillingEngine.ruleFor(date, config)

                    val bStatus = dayMap[MealType.BREAKFAST]?.status ?: true
                    val lStatus = dayMap[MealType.LUNCH]?.status ?: true
                    val dStatus = dayMap[MealType.DINNER]?.status ?: true
                    val bGuests = if (rule.breakfast) dayMap[MealType.BREAKFAST]?.guestCount ?: 0 else 0
                    val lGuests = if (rule.lunchPortions > 0) dayMap[MealType.LUNCH]?.guestCount ?: 0 else 0
                    val dGuests = if (rule.dinner) dayMap[MealType.DINNER]?.guestCount ?: 0 else 0

                    var eaten = 0
                    var missed = 0

                    if (rule.breakfast) { if (bStatus) eaten++ else missed++ }
                    if (rule.lunchPortions > 0) { if (lStatus) eaten++ else missed++ }
                    if (rule.dinner) { if (dStatus) eaten++ else missed++ }

                    val bill = BillingEngine.dayLiability(date, dayMap, config)

                    fun cell(served: Boolean, status: Boolean) = when {
                        !served -> "N/A"
                        status -> "Eaten"
                        else -> "Missed"
                    }

                    writer.write(
                        "$dateStr,${date.dayOfWeek.name}," +
                        "${cell(rule.breakfast, bStatus)}," +
                        "${cell(rule.lunchPortions > 0, lStatus)}," +
                        "${cell(rule.dinner, dStatus)}," +
                        "$bGuests,$lGuests,$dGuests,$eaten,$missed,%.2f\n".format(bill)
                    )
                }
            }
        }
    }
}
