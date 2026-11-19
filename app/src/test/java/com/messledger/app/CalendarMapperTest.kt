package com.messledger.app

import com.messledger.app.data.Config
import com.messledger.app.data.DayRule
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import com.messledger.app.domain.CalendarDayModel
import com.messledger.app.domain.MealDisplayStatus
import com.messledger.app.domain.mapCalendarMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The ledger grid is pure data, so its shape and flags can be pinned without a
 * device. These tests guard the 42-cell Sunday-first contract the UI relies on.
 */
class CalendarMapperTest {

    private val config = Config(
        priceBreakfast = 10f,
        priceLunch = 20f,
        priceDinner = 30f,
        taxPercent = 0f,
        allowedMisses = 25
    )

    private fun map(
        month: LocalDate,
        records: Map<LocalDate, List<MealRecord>> = emptyMap(),
        range: Pair<LocalDate, LocalDate>? = null
    ): List<CalendarDayModel> = mapCalendarMonth(month, records, config, range)

    private fun record(
        date: LocalDate,
        meal: MealType,
        status: Boolean,
        guests: Int = 0,
        previousStatus: Boolean? = null
    ) = MealRecord(date.toString(), meal, status, previousStatus, 0L, guests)

    @Test
    fun everyMonthLengthStillProducesFortyTwoCells() {
        listOf(
            LocalDate.of(2025, 2, 1) to 28,
            LocalDate.of(2024, 2, 1) to 29,
            LocalDate.of(2025, 4, 1) to 30,
            LocalDate.of(2025, 3, 1) to 31
        ).forEach { (month, expectedDays) ->
            val cells = map(month)
            assertEquals("42 cells for $month", 42, cells.size)
            assertEquals("in-month days for $month", expectedDays, cells.count { it.inMonth })
            assertEquals(
                "last in-month date for $month",
                expectedDays,
                cells.last { it.inMonth }.date.dayOfMonth
            )
        }
    }

    @Test
    fun gridStartsOnSundayAndCrossesYearBoundary() {
        // 1 April 2026 is a Wednesday, so the Sunday-first grid starts on 29 March.
        val april = map(LocalDate.of(2026, 4, 1))
        assertEquals(LocalDate.of(2026, 3, 29), april.first().date)
        assertEquals(java.time.DayOfWeek.SUNDAY, april.first().date.dayOfWeek)
        assertFalse(april.first().inMonth)

        // 1 March 2026 is itself a Sunday: no leading days from February.
        assertEquals(LocalDate.of(2026, 3, 1), map(LocalDate.of(2026, 3, 1)).first().date)

        val january = map(LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2025, 12, 28), january.first().date)
        assertFalse(january.first().inMonth)
        assertEquals(LocalDate.of(2026, 1, 31), january.last { it.inMonth }.date)
    }

    @Test
    fun billingCycleRunsFromThirdToSecondIncludingYearRollover() {
        assertEquals(
            LocalDate.of(2026, 2, 3) to LocalDate.of(2026, 3, 2),
            BillingEngine.cycleRange(LocalDate.of(2026, 3, 2))
        )
        assertEquals(
            LocalDate.of(2026, 3, 3) to LocalDate.of(2026, 4, 2),
            BillingEngine.cycleRange(LocalDate.of(2026, 3, 3))
        )
        assertEquals(
            LocalDate.of(2025, 12, 3) to LocalDate.of(2026, 1, 2),
            BillingEngine.cycleRange(LocalDate.of(2026, 1, 2))
        )
    }

    @Test
    fun rangePreviewSpansMonthEnd() {
        val cells = map(
            LocalDate.of(2026, 4, 1),
            range = LocalDate.of(2026, 4, 29) to LocalDate.of(2026, 5, 2)
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2)
            ),
            cells.filter { it.inRange }.map { it.date }
        )

        // Reversed edges are normalised by the mapper, not by the caller.
        val reversed = map(
            LocalDate.of(2026, 4, 1),
            range = LocalDate.of(2026, 5, 2) to LocalDate.of(2026, 4, 29)
        )
        assertEquals(cells.filter { it.inRange }.map { it.date }, reversed.filter { it.inRange }.map { it.date })
    }

    @Test
    fun guestCountAndRevisionAreReadFromRecords() {
        val saturday = LocalDate.of(2026, 10, 17)
        val records = mapOf(
            saturday to listOf(
                record(saturday, MealType.BREAKFAST, true),
                record(saturday, MealType.LUNCH, false, guests = 2, previousStatus = true),
                // Saturday dinner is closed by rule; a stray record must not revive it.
                record(saturday, MealType.DINNER, true)
            )
        )

        val cell = map(LocalDate.of(2026, 10, 1), records).first { it.date == saturday }

        assertEquals(2, cell.guestCount)
        assertTrue(cell.revised)

        assertEquals(MealDisplayStatus.EATEN, cell.statuses[MealType.BREAKFAST])
        assertEquals(MealDisplayStatus.SKIPPED, cell.statuses[MealType.LUNCH])
        assertEquals(MealDisplayStatus.CLOSED, cell.statuses[MealType.DINNER])
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH),
            cell.servedMeals
        )

        val monday = LocalDate.of(2026, 10, 19)
        val untouched = map(LocalDate.of(2026, 10, 1), records).first { it.date == monday }
        assertFalse(untouched.revised)
        assertEquals(0, untouched.guestCount)
        assertEquals(MealDisplayStatus.UNTOUCHED, untouched.statuses[MealType.LUNCH])
    }

    @Test
    fun savedDayRuleOverridesTheWeekdayDefault() {
        val sunday = LocalDate.of(2026, 10, 18)
        val withRule = config.copy(
            dayRules = mapOf(sunday.toString() to DayRule(breakfast = false, lunchPortions = 2, dinner = false))
        )

        val cell = mapCalendarMonth(LocalDate.of(2026, 10, 1), emptyMap(), withRule)
            .first { it.date == sunday }

        assertEquals(
            listOf(MealType.LUNCH),
            cell.servedMeals
        )
        assertEquals(MealDisplayStatus.CLOSED, cell.statuses[MealType.BREAKFAST])
        assertEquals(MealDisplayStatus.CLOSED, cell.statuses[MealType.DINNER])
    }
}
