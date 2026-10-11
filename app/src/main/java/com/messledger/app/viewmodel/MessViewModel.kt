package com.messledger.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.Config
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.data.MessLedgerDao
import com.messledger.app.domain.BillingEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class MessState(
    val selectedDate: LocalDate = LocalDate.now(),
    val mealsForDate: Map<MealType, MealRecord> = emptyMap(),
    val cycleSkippedCount: Double = 0.0,
    val dailyLiability: Float = 0.0f,
    val cycleMeals: Map<LocalDate, List<MealRecord>> = emptyMap(),
    val batch1Liability: Float = 0.0f,
    val batch2Liability: Float = 0.0f,
    val config: Config = Config(27.61f, 50.48f, 50.48f, 5.0f, 25)
)

class MessViewModel(
    private val dao: MessLedgerDao,
    private val configDataStore: ConfigDataStore
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())

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
            val mealsByDate = cycleMeals.groupBy { LocalDate.parse(it.date) }
            
            var curr = cycleStart
            val endCalc = if (LocalDate.now() < cycleEnd) LocalDate.now() else cycleEnd
            
            while (curr <= endCalc) {
                val dayRecords = mealsByDate[curr] ?: emptyList()
                val dayMap = dayRecords.associateBy { it.mealType }
                
                if (dayMap[MealType.BREAKFAST]?.status == false) skippedCount += 0.5
                if (dayMap[MealType.LUNCH]?.status == false) skippedCount += if (curr.dayOfWeek == DayOfWeek.SATURDAY) 2.0 else 1.0
                if (dayMap[MealType.DINNER]?.status == false && curr.dayOfWeek != DayOfWeek.SATURDAY) skippedCount += 1.0
                
                curr = curr.plusDays(1)
            }

            var subtotal = 0f
            if (dailyMap[MealType.BREAKFAST]?.status != false) subtotal += config.priceBreakfast
            if (dailyMap[MealType.LUNCH]?.status != false) {
                subtotal += if (date.dayOfWeek == DayOfWeek.SATURDAY) config.priceLunch * 2 else config.priceLunch
            }
            if (dailyMap[MealType.DINNER]?.status != false && date.dayOfWeek != DayOfWeek.SATURDAY) {
                subtotal += config.priceDinner
            }
            subtotal *= (1.0f + (config.taxPercent / 100f))

            var batch1Liability = 0f
            var batch2Liability = 0f
            
            mealsByDate.forEach { (d, records) ->
                var daySubtotal = 0f
                val map = records.associateBy { it.mealType }
                if (map[MealType.BREAKFAST]?.status != false) daySubtotal += config.priceBreakfast
                if (map[MealType.LUNCH]?.status != false) {
                    daySubtotal += if (d.dayOfWeek == DayOfWeek.SATURDAY) config.priceLunch * 2 else config.priceLunch
                }
                if (map[MealType.DINNER]?.status != false && d.dayOfWeek != DayOfWeek.SATURDAY) {
                    daySubtotal += config.priceDinner
                }
                daySubtotal *= (1.0f + (config.taxPercent / 100f))
                
                if (d.dayOfMonth in 3..17) {
                    batch1Liability += daySubtotal
                } else {
                    batch2Liability += daySubtotal
                }
            }

            MessState(
                selectedDate = date,
                mealsForDate = dailyMap,
                cycleSkippedCount = skippedCount,
                dailyLiability = subtotal,
                cycleMeals = mealsByDate,
                batch1Liability = batch1Liability,
                batch2Liability = batch2Liability,
                config = config
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MessState())

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleMeal(mealType: MealType) {
        val date = _selectedDate.value
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
            dao.logMeal(date.toString(), MealType.BREAKFAST, status, System.currentTimeMillis())
            dao.logMeal(date.toString(), MealType.LUNCH, status, System.currentTimeMillis())
            if (date.dayOfWeek != DayOfWeek.SATURDAY) {
                dao.logMeal(date.toString(), MealType.DINNER, status, System.currentTimeMillis())
            }
        }
    }

    fun updateConfig(config: Config) {
        viewModelScope.launch {
            configDataStore.updateConfig(config)
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
