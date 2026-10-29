package com.messledger.app

import com.messledger.app.data.Config
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.domain.BillingEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The meals are eaten by default; tapping one records a skip. These tests pin
 * that rule because getting it backwards silently drops whole days from the
 * bill.
 */
class BillingEngineTest {

    // Distinct prices so the Saturday rules cannot accidentally look correct.
    private val config = Config(
        priceBreakfast = 10f,
        priceLunch = 20f,
        priceDinner = 30f,
        taxPercent = 0f,
        allowedMisses = 25
    )

    private val wednesday = LocalDate.of(2026, 10, 14)
    private val saturday = LocalDate.of(2026, 10, 17)

    private fun record(meal: MealType, eaten: Boolean, date: LocalDate = wednesday) =
        MealRecord(date.toString(), meal, eaten, null, 0L)

    @Test
    fun untouchedDayIsBilledInFull() {
        assertEquals(60f, BillingEngine.dayLiability(wednesday, emptyMap(), config), 0.001f)
    }

    @Test
    fun untouchedSaturdayUsesSaturdayPortions() {
        // breakfast 10 + double lunch 40, dinner is closed
        assertEquals(50f, BillingEngine.dayLiability(saturday, emptyMap(), config), 0.001f)
    }

    @Test
    fun skippedMealIsNotBilled() {
        val records = mapOf(MealType.BREAKFAST to record(MealType.BREAKFAST, eaten = false))
        assertEquals(50f, BillingEngine.dayLiability(wednesday, records, config), 0.001f)
    }

    @Test
    fun everythingSkippedCostsNothing() {
        val records = MealType.values().associateWith { record(it, eaten = false) }
        assertEquals(0f, BillingEngine.dayLiability(wednesday, records, config), 0.001f)
    }

    @Test
    fun taxAppliesToTheDay() {
        val taxed = config.copy(taxPercent = 5f)
        // 60 * 1.05
        assertEquals(63f, BillingEngine.dayLiability(wednesday, emptyMap(), taxed), 0.001f)
    }

    @Test
    fun aRecordedMealSurvivesADeletedDefault() {
        // Sparse records must not resurrect the other meals: missing still
        // means eaten, but an explicit skip must win.
        val records = mapOf(
            MealType.LUNCH to record(MealType.LUNCH, eaten = true),
            MealType.DINNER to record(MealType.DINNER, eaten = false)
        )
        assertEquals(30f, BillingEngine.dayLiability(wednesday, records, config), 0.001f)
    }
}
