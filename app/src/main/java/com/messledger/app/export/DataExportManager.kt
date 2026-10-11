package com.messledger.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import java.io.OutputStreamWriter
import java.time.DayOfWeek
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

    fun writeDataToUri(context: Context, uri: Uri, records: List<MealRecord>) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                // Write Header
                writer.write("Date,Weekday,Breakfast,Lunch,Dinner,Meals Eaten,Missed,Bill\n")

                // Group by date
                val grouped = records.groupBy { it.date }.toSortedMap()

                for ((dateStr, dayRecords) in grouped) {
                    val date = LocalDate.parse(dateStr)
                    val dayMap = dayRecords.associateBy { it.mealType }

                    val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY

                    val bStatus = dayMap[MealType.BREAKFAST]?.status ?: true
                    val lStatus = dayMap[MealType.LUNCH]?.status ?: true
                    val dStatus = if (isSaturday) true else dayMap[MealType.DINNER]?.status ?: true // Sat dinner is NA

                    var eaten = 0
                    var missed = 0
                    var bill = 0f

                    if (bStatus) { eaten++; bill += 27.61f } else missed++
                    if (lStatus) { eaten++; bill += if (isSaturday) 50.48f * 2 else 50.48f } else missed++
                    if (dStatus && !isSaturday) { eaten++; bill += 50.48f }
                    if (!dStatus && !isSaturday) { missed++ }
                    
                    bill *= 1.05f // Add tax

                    writer.write(
                        "$dateStr,${date.dayOfWeek.name}," +
                        "${if (bStatus) "Eaten" else "Missed"}," +
                        "${if (lStatus) "Eaten" else "Missed"}," +
                        "${if (isSaturday) "N/A" else if (dStatus) "Eaten" else "Missed"}," +
                        "$eaten,$missed,%.2f\n".format(bill)
                    )
                }
            }
        }
    }
}
