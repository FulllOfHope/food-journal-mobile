package com.messledger.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Config(
    val priceBreakfast: Float,
    val priceLunch: Float,
    val priceDinner: Float,
    val taxPercent: Float,
    val allowedMisses: Int
)

class ConfigDataStore(private val context: Context) {
    companion object {
        val PRICE_BREAKFAST = floatPreferencesKey("price_breakfast")
        val PRICE_LUNCH = floatPreferencesKey("price_lunch")
        val PRICE_DINNER = floatPreferencesKey("price_dinner")
        val TAX_PERCENT = floatPreferencesKey("tax_percent")
        val ALLOWED_MISSES = intPreferencesKey("allowed_misses")
    }

    val configFlow: Flow<Config> = context.dataStore.data
        .map { preferences ->
            Config(
                priceBreakfast = preferences[PRICE_BREAKFAST] ?: 27.61f,
                priceLunch = preferences[PRICE_LUNCH] ?: 50.48f,
                priceDinner = preferences[PRICE_DINNER] ?: 50.48f,
                taxPercent = preferences[TAX_PERCENT] ?: 5.0f,
                allowedMisses = preferences[ALLOWED_MISSES] ?: 25
            )
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
}
