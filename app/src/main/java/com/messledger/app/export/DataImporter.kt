package com.messledger.app.export

import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Reads a ledger back in from a CSV export.
 *
 * Two column layouts are accepted, because the Android export and the
 * desktop export differ slightly:
 *
 *   Date,Weekday,Breakfast,Lunch,Dinner,Meals Eaten,Missed,Bill
 *   date,weekday,breakfast,lunch,dinner,meals_eaten,meals_expected,missed,bill
 *
 * Meal cells may say Eaten/Missed (Android) or True/False (desktop). Anything
 * else — blank, N/A, a dash — is treated as "no record", which leaves the
 * default in place rather than inventing a skip.
 *
 * Nothing is written to the database here; this only turns text into records.
 */
object DataImporter {

    data class Result(
        val records: List<MealRecord>,
        val days: Int,
        val skippedRows: Int,
        val error: String? = null
    )

    fun parseCsv(text: String, now: Long = System.currentTimeMillis()): Result {
        val lines = text.removePrefix("\uFEFF").lineSequence()
            .filter { it.isNotBlank() }
            .toList()

        if (lines.isEmpty()) {
            return Result(emptyList(), 0, 0, "That file is empty.")
        }

        val header = splitCsvLine(lines.first()).map(::normalise)
        val dateCol = header.indexOf("date")
        val breakfastCol = header.indexOf("breakfast")
        val lunchCol = header.indexOf("lunch")
        val dinnerCol = header.indexOf("dinner")

        if (dateCol < 0 || breakfastCol < 0 || lunchCol < 0 || dinnerCol < 0) {
            return Result(
                emptyList(), 0, 0,
                "This is not a meal ledger. Expected columns: Date, Breakfast, Lunch, Dinner."
            )
        }

        val records = mutableListOf<MealRecord>()
        val dates = mutableSetOf<String>()
        var skippedRows = 0

        for (line in lines.drop(1)) {
            val cells = splitCsvLine(line)
            if (cells.size <= maxOf(dateCol, breakfastCol, lunchCol, dinnerCol)) {
                skippedRows++
                continue
            }

            val date = try {
                LocalDate.parse(cells[dateCol].trim().trim('"'))
            } catch (_: DateTimeParseException) {
                skippedRows++
                continue
            }

            val statuses = listOf(
                MealType.BREAKFAST to parseEaten(cells[breakfastCol]),
                MealType.LUNCH to parseEaten(cells[lunchCol]),
                MealType.DINNER to parseEaten(cells[dinnerCol])
            )

            var found = false
            statuses.forEachIndexed { index, (meal, status) ->
                // Saturday dinner is closed, and the app never stores it.
                if (meal == MealType.DINNER && date.dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                    return@forEachIndexed
                }
                if (status == null) return@forEachIndexed

                records += MealRecord(
                    date = date.toString(),
                    mealType = meal,
                    status = status,
                    previousStatus = null,
                    timestamp = now + index
                )
                found = true
            }

            if (found) dates += date.toString() else skippedRows++
        }

        if (records.isEmpty()) {
            return Result(
                emptyList(), 0, skippedRows,
                "No meal entries could be read from that file."
            )
        }

        return Result(records, dates.size, skippedRows)
    }

    /** True = eaten, false = skipped, null = leave the default alone. */
    private fun parseEaten(raw: String): Boolean? = when (raw.trim().trim('"').lowercase()) {
        "eaten", "true", "1", "yes", "y", "t" -> true
        "missed", "false", "0", "no", "n", "f" -> false
        else -> null
    }

    private fun normalise(header: String): String =
        header.trim().trim('"').lowercase().replace("_", "").replace(" ", "")

    /** Minimal CSV reader: handles quoted cells and doubled quotes. */
    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> {
                    cells += cell.toString()
                    cell.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        cells += cell.toString()
        return cells
    }
}
