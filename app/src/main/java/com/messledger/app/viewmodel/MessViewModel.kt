package com.messledger.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.Config
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.DayRule
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.data.MessLedgerDao
import com.messledger.app.data.ThemeMode
import com.messledger.app.domain.BillingEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MessState(
    val selectedDate: LocalDate = LocalDate.now(),
    val mealsForDate: Map<MealType, MealRecord> = emptyMap(),
    val cycleSkippedCount: Double = 0.0,
    val dailyLiability: Float = 0.0f,
    val cycleMeals: Map<LocalDate, List<MealRecord>> = emptyMap(),
    val batch1Liability: Float = 0.0f,
    val batch2Liability: Float = 0.0f,
    val config: Config = Config(27.61f, 50.48f, 50.48f, 5.0f, 25),
    val cycleStart: LocalDate = LocalDate.now(),
    val cycleEnd: LocalDate = LocalDate.now(),
    val cycleDaysElapsed: Int = 0,
    val cycleMealsBilled: Double = 0.0,
    val cycleGuests: Int = 0,
    val cycleSubtotal: Float = 0.0f,
    val cycleTax: Float = 0.0f,
    val cycleTotal: Float = 0.0f
)

class MessViewModel(
    private val dao: MessLedgerDao,
    private val configDataStore: ConfigDataStore
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    /** The saved theme choice: follow the phone until the reader overrides it. */
    val themeMode: StateFlow<ThemeMode> = configDataStore.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val state: StateFlow<MessState> = combine(
        _selectedDate,
        configDataStore.configFlow
    ) { date, config ->
        date to config
    }.flatMapLatest { (date, config) ->
        val (cycleStart, cycleEnd) = getCycleRange(date)
        
        combine(
            dao.getMealsForDate(date.toString()),
            dao.getMealsBetween(cycleStart.toString(), cycleEnd.toString())
        ) { dailyMeals, cycleMeals ->
            val dailyMap = dailyMeals.associateBy { it.mealType }

            var skippedCount = 0.0
            var billedUnits = 0.0
            var guestCount = 0
            var subtotal = 0f
            var elapsedDays = 0
            val mealsByDate = cycleMeals.groupBy { LocalDate.parse(it.date) }

            var curr = cycleStart
            val endCalc = if (LocalDate.now() < cycleEnd) LocalDate.now() else cycleEnd

            while (curr <= endCalc) {
                val dayRecords = mealsByDate[curr] ?: emptyList()
                val dayMap = dayRecords.associateBy { it.mealType }
                val rule = BillingEngine.ruleFor(curr, config)

                if (rule.breakfast && dayMap[MealType.BREAKFAST]?.status == false) skippedCount += 0.5
                if (rule.lunchPortions > 0 && dayMap[MealType.LUNCH]?.status == false) {
                    skippedCount += rule.lunchPortions
                }
                if (rule.dinner && dayMap[MealType.DINNER]?.status == false) skippedCount += 1.0

                if (rule.breakfast && dayMap[MealType.BREAKFAST]?.status != false) billedUnits += 0.5
                if (rule.lunchPortions > 0 && dayMap[MealType.LUNCH]?.status != false) {
                    billedUnits += rule.lunchPortions
                }
                if (rule.dinner && dayMap[MealType.DINNER]?.status != false) billedUnits += 1.0

                guestCount += dayRecords.sumOf { it.guestCount }
                subtotal += BillingEngine.daySubtotal(curr, dayMap, config)

                elapsedDays++
                curr = curr.plusDays(1)
            }

            val dailyLiability = BillingEngine.dayLiability(date, dailyMap, config)

            // Bill every elapsed day of the cycle, not only the days that
            // happen to have a record. A day nobody touched is still a day the
            // mess served: its meals default to eaten, so skipping it
            // understated the total until each meal was tapped by hand.
            var batch1Liability = 0f
            var batch2Liability = 0f
            var billingDay = cycleStart
            while (!billingDay.isAfter(endCalc)) {
                val dayMap = mealsByDate[billingDay].orEmpty().associateBy { it.mealType }
                val dayLiability = BillingEngine.dayLiability(billingDay, dayMap, config)

                if (billingDay.dayOfMonth in 3..17) {
                    batch1Liability += dayLiability
                } else {
                    batch2Liability += dayLiability
                }
                billingDay = billingDay.plusDays(1)
            }

            val total = batch1Liability + batch2Liability
            MessState(
                selectedDate = date,
                mealsForDate = dailyMap,
                cycleSkippedCount = skippedCount,
                dailyLiability = dailyLiability,
                cycleMeals = mealsByDate,
                batch1Liability = batch1Liability,
                batch2Liability = batch2Liability,
                config = config,
                cycleStart = cycleStart,
                cycleEnd = cycleEnd,
                cycleDaysElapsed = elapsedDays,
                cycleMealsBilled = billedUnits,
                cycleGuests = guestCount,
                cycleSubtotal = subtotal,
                cycleTax = (total - subtotal).coerceAtLeast(0f),
                cycleTotal = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MessState())

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleMeal(mealType: MealType) {
        val date = _selectedDate.value
        val rule = BillingEngine.ruleFor(date, state.value.config)
        val served = when (mealType) {
            MealType.BREAKFAST -> rule.breakfast
            MealType.LUNCH -> rule.lunchPortions > 0
            MealType.DINNER -> rule.dinner
        }
        if (!served) return
        viewModelScope.launch {
            val existing = dao.getMeal(date.toString(), mealType)
            val currentStatus = existing?.status ?: true // Default is true (eaten)
            dao.logMeal(
                date = date.toString(),
                mealType = mealType,
                status = !currentStatus,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    fun setAllMealsStatus(status: Boolean) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val rule = BillingEngine.ruleFor(date, state.value.config)
            if (rule.breakfast) {
                dao.logMeal(date.toString(), MealType.BREAKFAST, status, System.currentTimeMillis())
            }
            if (rule.lunchPortions > 0) {
                dao.logMeal(date.toString(), MealType.LUNCH, status, System.currentTimeMillis())
            }
            if (rule.dinner) {
                dao.logMeal(date.toString(), MealType.DINNER, status, System.currentTimeMillis())
            }
        }
    }

    /** Add or remove one guest portion for a meal on the selected date. */
    fun changeGuestCount(mealType: MealType, delta: Int) {
        changeGuestCount(_selectedDate.value, mealType, delta)
    }

    fun changeGuestCount(date: LocalDate, mealType: MealType, delta: Int) {
        viewModelScope.launch {
            val existing = dao.getMeal(date.toString(), mealType)
            val next = ((existing?.guestCount ?: 0) + delta).coerceIn(0, 20)
            dao.setGuestCount(
                date = date.toString(),
                mealType = mealType,
                guestCount = next,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Mark every meal in a range. Used for holidays and exam breaks so a week
     * away does not need twenty-one individual taps.
     */
    fun setRangeStatus(start: LocalDate, end: LocalDate, status: Boolean) {
        val first = if (start.isAfter(end)) end else start
        val last = if (start.isAfter(end)) start else end
        val config = state.value.config
        val closedDinners = buildSet {
            var day = first
            while (!day.isAfter(last)) {
                if (!BillingEngine.ruleFor(day, config).dinner) add(day.toString())
                day = day.plusDays(1)
            }
        }
        viewModelScope.launch {
            dao.setMealsStatusForRange(
                startDate = first.toString(),
                endDate = last.toString(),
                status = status,
                timestamp = System.currentTimeMillis(),
                dinnerClosedDates = closedDinners
            )
        }
    }

    /** Save a rule for one date; null restores the weekday default. */
    fun setDayRule(date: LocalDate, rule: DayRule?) {
        viewModelScope.launch {
            configDataStore.setDayRule(date, rule)
        }
    }

    fun updateConfig(config: Config) {
        viewModelScope.launch {
            configDataStore.updateConfig(config)
        }
    }

    /**
     * Tap the masthead switch: lock the mode opposite to what is on screen.
     * [currentlyDark] is the resolved theme, so a tap from system-dark lands
     * on light rather than guessing at the saved preference.
     */
    fun toggleTheme(currentlyDark: Boolean) {
        setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            configDataStore.setThemeMode(mode)
        }
    }

    /** Every record, not just the visible cycle, for a complete backup. */
    suspend fun allRecords(): List<MealRecord> = dao.getAllMeals()

    /**
     * Restore records from an imported ledger. Existing entries for the same
     * date and meal are overwritten by the file.
     */
    fun importMeals(records: List<MealRecord>) {
        if (records.isEmpty()) return
        viewModelScope.launch {
            dao.insertMeals(records)
        }
    }

    private fun getCycleRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val startMonth = if (date.dayOfMonth < 3) date.minusMonths(1) else date
        val start = startMonth.withDayOfMonth(3)
        val end = start.plusMonths(1).minusDays(1)
        return start to end
    }
}

class MessViewModelFactory(
    private val dao: MessLedgerDao,
    private val configDataStore: ConfigDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessViewModel(dao, configDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
