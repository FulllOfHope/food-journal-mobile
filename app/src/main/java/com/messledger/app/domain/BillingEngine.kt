package com.messledger.app.domain

import com.messledger.app.data.Config
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

object BillingEngine {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates total liability based on exact meals eaten, applying 5% tax.
     * Saturday Lunch = 2 portions. Saturday Dinner = 0 portions.
     */
    fun calculateLiability(records: List<MealRecord>, config: Config): Float {
        var subtotal = 0.0f
        for (record in records) {
            if (!record.status) continue // Only bill if eaten

            val date = LocalDate.parse(record.date, dateFormatter)
            val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY

            when (record.mealType) {
                MealType.BREAKFAST -> subtotal += config.priceBreakfast
                MealType.LUNCH -> {
                    subtotal += if (isSaturday) {
                        config.priceLunch * 2
                    } else {
                        config.priceLunch
                    }
                }
                MealType.DINNER -> {
                    subtotal += if (isSaturday) {
                        0.0f
                    } else {
                        config.priceDinner
                    }
                }
            }
        }

        val taxMultiplier = 1.0f + (config.taxPercent / 100.0f)
        return subtotal * taxMultiplier
    }

    /**
     * Group records by cycle and batch.
     * Cycle runs from 3rd of current month to 2nd of next month.
     * Batch 1: 3rd to 17th
     * Batch 2: 18th to 2nd
     */
    fun filterForCycle(records: List<MealRecord>, cycleYear: Int, cycleMonth: Int): List<MealRecord> {
        val startDate = LocalDate.of(cycleYear, cycleMonth, 3)
        val endDate = startDate.plusMonths(1).minusDays(1) // Up to 2nd of next month

        return records.filter {
            val d = LocalDate.parse(it.date, dateFormatter)
            !d.isBefore(startDate) && !d.isAfter(endDate)
        }
    }
    
    fun filterForBatch1(cycleRecords: List<MealRecord>): List<MealRecord> {
        return cycleRecords.filter {
            val d = LocalDate.parse(it.date, dateFormatter)
            d.dayOfMonth in 3..17
        }
    }

    fun filterForBatch2(cycleRecords: List<MealRecord>): List<MealRecord> {
        return cycleRecords.filter {
            val d = LocalDate.parse(it.date, dateFormatter)
            d.dayOfMonth >= 18 || d.dayOfMonth <= 2
        }
    }
}
