package com.messledger.app

import com.messledger.app.data.MealType
import com.messledger.app.export.DataImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Import must round-trip what the app exports, and must not choke on the
 * desktop export's lower-case headers and True/False cells.
 */
class DataImporterTest {

    private fun statusOf(import: DataImporter.Result, date: String, meal: MealType): Boolean? =
        import.records.firstOrNull { it.date == date && it.mealType == meal }?.status

    @Test
    fun readsTheAndroidExport() {
        val csv = """
            Date,Weekday,Breakfast,Lunch,Dinner,Meals Eaten,Missed,Bill
            2026-10-14,WEDNESDAY,Eaten,Missed,Eaten,2,1,108.62
            2026-10-17,SATURDAY,Eaten,Eaten,N/A,2,0,135.00
        """.trimIndent()

        val result = DataImporter.parseCsv(csv)

        assertNull(result.error)
        assertEquals(2, result.days)
        assertEquals(5, result.records.size)
        assertEquals(true, statusOf(result, "2026-10-14", MealType.BREAKFAST))
        assertEquals(false, statusOf(result, "2026-10-14", MealType.LUNCH))
        assertEquals(true, statusOf(result, "2026-10-14", MealType.DINNER))
        assertEquals(true, statusOf(result, "2026-10-17", MealType.LUNCH))
        // Saturday dinner is closed, so no record is invented for it.
        assertNull(statusOf(result, "2026-10-17", MealType.DINNER))
    }

    @Test
    fun readsTheDesktopExport() {
        val csv = """
            date,weekday,breakfast,lunch,dinner,meals_eaten,meals_expected,missed,bill
            2026-10-14,Wednesday,True,False,True,1.5,2.5,1.0,108.62
        """.trimIndent()

        val result = DataImporter.parseCsv(csv)

        assertNull(result.error)
        assertEquals(1, result.days)
        assertEquals(true, statusOf(result, "2026-10-14", MealType.BREAKFAST))
        assertEquals(false, statusOf(result, "2026-10-14", MealType.LUNCH))
        assertEquals(true, statusOf(result, "2026-10-14", MealType.DINNER))
    }

    @Test
    fun blankMealCellsKeepTheDefault() {
        val csv = """
            Date,Breakfast,Lunch,Dinner
            2026-10-14,,Missed,
        """.trimIndent()

        val result = DataImporter.parseCsv(csv)

        assertNull(result.error)
        assertEquals(1, result.records.size)
        assertEquals(false, statusOf(result, "2026-10-14", MealType.LUNCH))
    }

    @Test
    fun malformedRowsAreSkippedNotFatal() {
        val csv = """
            Date,Breakfast,Lunch,Dinner
            not-a-date,Eaten,Eaten,Eaten
            2026-10-14,Eaten,Missed,Eaten
        """.trimIndent()

        val result = DataImporter.parseCsv(csv)

        assertNull(result.error)
        assertEquals(1, result.days)
        assertEquals(1, result.skippedRows)
    }

    @Test
    fun rejectsAFileThatIsNotALedger() {
        val result = DataImporter.parseCsv("name,amount\ncoffee,20")
        assertNotNull(result.error)
        assertEquals(0, result.records.size)
    }

    @Test
    fun handlesAQuotedHeaderAndBom() {
        val csv = "\uFEFF\"Date\",\"Breakfast\",\"Lunch\",\"Dinner\"\n" +
            "\"2026-10-14\",\"Eaten\",\"Eaten\",\"Missed\"\n"

        val result = DataImporter.parseCsv(csv)

        assertNull(result.error)
        assertEquals(3, result.records.size)
        assertEquals(false, statusOf(result, "2026-10-14", MealType.DINNER))
    }
}
