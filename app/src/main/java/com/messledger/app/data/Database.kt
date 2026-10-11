package com.messledger.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class MealType { BREAKFAST, LUNCH, DINNER }

@Entity(tableName = "meal_records", primaryKeys = ["date", "mealType"])
data class MealRecord(
    val date: String,
    val mealType: MealType,
    val status: Boolean, // true = eaten, false = skipped
    val previousStatus: Boolean?,
    val timestamp: Long
)

@Entity(tableName = "sync_queue")
data class SyncDelta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val mealType: MealType,
    val status: Boolean,
    val timestamp: Long
)

class MessTypeConverters {
    @TypeConverter fun fromMealType(value: MealType): String = value.name
    @TypeConverter fun toMealType(value: String): MealType = MealType.valueOf(value)
}

@Dao
interface MessLedgerDao {
    @Query("SELECT * FROM meal_records WHERE date = :date")
    fun getMealsForDate(date: String): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date = :date AND mealType = :mealType")
    suspend fun getMeal(date: String, mealType: MealType): MealRecord?

    @Query("SELECT * FROM meal_records WHERE date BETWEEN :startDate AND :endDate")
    fun getMealsBetween(startDate: String, endDate: String): Flow<List<MealRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(record: MealRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncDelta(delta: SyncDelta)

    @Transaction
    suspend fun logMeal(date: String, mealType: MealType, status: Boolean, timestamp: Long) {
        val existing = getMeal(date, mealType)
        // Last Write Wins
        if (existing != null && existing.timestamp >= timestamp) return

        val isPastDay = date < LocalDate.now().toString()
        val prevStatus = if (isPastDay && existing != null && existing.status != status) existing.status else existing?.previousStatus

        insertMeal(MealRecord(date, mealType, status, prevStatus, timestamp))
        insertSyncDelta(SyncDelta(date = date, mealType = mealType, status = status, timestamp = timestamp))
    }
}

@Database(entities = [MealRecord::class, SyncDelta::class], version = 1, exportSchema = false)
@TypeConverters(MessTypeConverters::class)
abstract class MessLedgerDatabase : RoomDatabase() {
    abstract fun messLedgerDao(): MessLedgerDao

    companion object {
        @Volatile private var INSTANCE: MessLedgerDatabase? = null
        fun getDatabase(context: Context): MessLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessLedgerDatabase::class.java,
                    "mess_ledger.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
