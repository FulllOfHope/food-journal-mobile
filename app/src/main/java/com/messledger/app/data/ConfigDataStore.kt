package com.messledger.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The mess rules for one specific date. A date with a saved rule ignores the
 * weekday defaults entirely, which is how a special Saturday lunch served on
 * Sunday (or a dinner cancelled for a festival) is recorded.
 *
 * [lunchPortions] is 0 for a closed lunch, 1 for a normal portion and 2 for a
 * feast portion.
 */
@Serializable
data class DayRule(
    val breakfast: Boolean = true,
    val lunchPortions: Int = 1,
    val dinner: Boolean = true
)

data class Config(
    val priceBreakfast: Float,
    val priceLunch: Float,
    val priceDinner: Float,
    val taxPercent: Float,
    val allowedMisses: Int,
    /** Overrides keyed by ISO date; dates without an entry use the weekday default. */
    val dayRules: Map<String, DayRule> = emptyMap()
)

class ConfigDataStore(private val context: Context) {
    companion object {
        val PRICE_BREAKFAST = floatPreferencesKey("price_breakfast")
        val PRICE_LUNCH = floatPreferencesKey("price_lunch")
        val PRICE_DINNER = floatPreferencesKey("price_dinner")
        val TAX_PERCENT = floatPreferencesKey("tax_percent")
        val ALLOWED_MISSES = intPreferencesKey("allowed_misses")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAY_RULES = stringPreferencesKey("day_rules")

        private val json = Json { ignoreUnknownKeys = true }

        fun decodeRules(raw: String?): Map<String, DayRule> {
            if (raw.isNullOrBlank()) return emptyMap()
            return runCatching {
                json.decodeFromString<Map<String, DayRule>>(raw)
            }.getOrDefault(emptyMap())
        }
    }

    val configFlow: Flow<Config> = context.dataStore.data
        .map { preferences ->
            Config(
                priceBreakfast = preferences[PRICE_BREAKFAST] ?: 27.61f,
                priceLunch = preferences[PRICE_LUNCH] ?: 50.48f,
                priceDinner = preferences[PRICE_DINNER] ?: 50.48f,
                taxPercent = preferences[TAX_PERCENT] ?: 5.0f,
                allowedMisses = preferences[ALLOWED_MISSES] ?: 25,
                dayRules = decodeRules(preferences[DAY_RULES])
            )
        }

    /** Save a rule for one date, or pass null to restore the weekday default. */
    suspend fun setDayRule(date: LocalDate, rule: DayRule?) {
        context.dataStore.edit { preferences ->
            val rules = decodeRules(preferences[DAY_RULES]).toMutableMap()
            if (rule == null) rules.remove(date.toString()) else rules[date.toString()] = rule
            preferences[DAY_RULES] = json.encodeToString(rules)
        }
    }

    suspend fun updateConfig(config: Config) {
        context.dataStore.edit { preferences ->
            preferences[PRICE_BREAKFAST] = config.priceBreakfast
            preferences[PRICE_LUNCH] = config.priceLunch
            preferences[PRICE_DINNER] = config.priceDinner
            preferences[TAX_PERCENT] = config.taxPercent
            preferences[ALLOWED_MISSES] = config.allowedMisses
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val saved = preferences[THEME_MODE]
            ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
}
