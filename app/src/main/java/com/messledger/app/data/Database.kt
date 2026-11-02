package com.messledger.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class MealType { BREAKFAST, LUNCH, DINNER }

@Entity(tableName = "meal_records", primaryKeys = ["date", "mealType"])
data class MealRecord(
    val date: String,
    val mealType: MealType,
    val status: Boolean, // true = eaten, false = skipped
    val previousStatus: Boolean?,
    val timestamp: Long,
    /** Extra eaters charged for this meal, independent of the rebate allowance. */
    val guestCount: Int = 0
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

    @Query("SELECT * FROM meal_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMealsBetweenOnce(startDate: String, endDate: String): List<MealRecord>

    /** Every record, oldest first. Used for a complete backup, not just a cycle. */
    @Query("SELECT * FROM meal_records ORDER BY date, mealType")
    suspend fun getAllMeals(): List<MealRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(record: MealRecord)

    /** Bulk restore from an export. Same date + meal is overwritten. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(records: List<MealRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncDelta(delta: SyncDelta)

    @Transaction
    suspend fun logMeal(date: String, mealType: MealType, status: Boolean, timestamp: Long) {
        val existing = getMeal(date, mealType)
        // Last Write Wins
        if (existing != null && existing.timestamp >= timestamp) return

        val isPastDay = date < LocalDate.now().toString()
        val prevStatus = if (isPastDay && existing != null && existing.status != status) existing.status else existing?.previousStatus

        insertMeal(
            MealRecord(
                date = date,
                mealType = mealType,
                status = status,
                previousStatus = prevStatus,
                timestamp = timestamp,
                guestCount = existing?.guestCount ?: 0
            )
        )
        insertSyncDelta(SyncDelta(date = date, mealType = mealType, status = status, timestamp = timestamp))
    }

    /**
     * Change the number of guests for one meal. A guest is an extra portion:
     * it costs money, but it never spends a rebate. Keeping the existing
     * status means adding a guest cannot accidentally un-skip a meal.
     */
    @Transaction
    suspend fun setGuestCount(date: String, mealType: MealType, guestCount: Int, timestamp: Long) {
        val existing = getMeal(date, mealType)
        insertMeal(
            MealRecord(
                date = date,
                mealType = mealType,
                status = existing?.status ?: true,
                previousStatus = existing?.previousStatus,
                timestamp = timestamp,
                guestCount = guestCount.coerceAtLeast(0)
            )
        )
    }

    /**
     * Mark every meal in an inclusive date range eaten or skipped. Saturday
     * dinner is never written because the mess is closed that night.
     */
    @Transaction
    suspend fun setMealsStatusForRange(
        startDate: String,
        endDate: String,
        status: Boolean,
        timestamp: Long
    ) {
        var day = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        var offset = 0L

        while (!day.isAfter(end)) {
            val stamp = timestamp + offset
            logMeal(day.toString(), MealType.BREAKFAST, status, stamp)
            logMeal(day.toString(), MealType.LUNCH, status, stamp)
            if (day.dayOfWeek != java.time.DayOfWeek.SATURDAY) {
                logMeal(day.toString(), MealType.DINNER, status, stamp)
            }
            day = day.plusDays(1)
            offset++
        }
    }
}

@Database(entities = [MealRecord::class, SyncDelta::class], version = 2, exportSchema = false)
@TypeConverters(MessTypeConverters::class)
abstract class MessLedgerDatabase : RoomDatabase() {
    abstract fun messLedgerDao(): MessLedgerDao

    companion object {
        @Volatile private var INSTANCE: MessLedgerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE meal_records ADD COLUMN guestCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): MessLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessLedgerDatabase::class.java,
                    "mess_ledger.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
