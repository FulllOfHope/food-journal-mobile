package com.messledger.app.domain

import com.messledger.app.data.Config
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import java.time.LocalDate

/** How one meal is presented in a ledger cell. */
enum class MealDisplayStatus { EATEN, UNTOUCHED, SKIPPED, CLOSED }

/**
 * A pure view model for one calendar cell. No Compose, no resources, no rule
 * math of its own: "served" comes from [BillingEngine] and the rest is read
 * straight from the records.
 */
data class CalendarDayModel(
    val date: LocalDate,
    val inMonth: Boolean,
    val servedMeals: List<MealType>,
    val statuses: Map<MealType, MealDisplayStatus>,
    val guestCount: Int,
    val revised: Boolean,
    val inRange: Boolean
)

/**
 * Six Sunday-first weeks around [month]. Adjacent-month days are included so
 * the grid keeps its seven-column ledger ruling every month.
 */
fun mapCalendarMonth(
    month: LocalDate,
    recordsByDate: Map<LocalDate, List<MealRecord>>,
    config: Config,
    range: Pair<LocalDate, LocalDate>? = null
): List<CalendarDayModel> {
    val first = month.withDayOfMonth(1)
    // java.time uses Monday = 1..7 (Sunday = 7); the ledger starts on Sunday = 0.
    val leading = first.dayOfWeek.value % 7
    val gridStart = first.minusDays(leading.toLong())

    return (0 until 42).map { index ->
        val date = gridStart.plusDays(index.toLong())
        val rule = BillingEngine.ruleFor(date, config)
        val served = buildList {
            if (rule.breakfast) add(MealType.BREAKFAST)
            if (rule.lunchPortions > 0) add(MealType.LUNCH)
            if (rule.dinner) add(MealType.DINNER)
        }
        val records = recordsByDate[date].orEmpty()
        val byMeal = records.associateBy { it.mealType }

        val statuses = MealType.values().associateWith { meal ->
            when {
                meal !in served -> MealDisplayStatus.CLOSED
                byMeal[meal] == null -> MealDisplayStatus.UNTOUCHED
                byMeal[meal]!!.status -> MealDisplayStatus.EATEN
                else -> MealDisplayStatus.SKIPPED
            }
        }

        CalendarDayModel(
            date = date,
            inMonth = date.month == first.month && date.year == first.year,
            servedMeals = served,
            statuses = statuses,
            guestCount = records.sumOf { it.guestCount.coerceAtLeast(0) },
            revised = records.any { it.previousStatus != null },
            inRange = range?.let { (start, end) ->
                val from = if (start <= end) start else end
                val to = if (start <= end) end else start
                !date.isBefore(from) && !date.isAfter(to)
            } ?: false
        )
    }
}
