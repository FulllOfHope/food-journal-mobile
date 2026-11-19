package com.messledger.app.export

import com.messledger.app.data.DayRule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Custom per-date meal rules travel with the CSV backup as a single tagged
 * line appended after the meal rows:
 *
 * ```
 * #FoodJournalDayRules:{"2026-10-18":{"breakfast":false,"lunchPortions":2,"dinner":false}}
 * ```
 *
 * The tag keeps the line out of the CSV table itself: older versions simply
 * skip it as an unparseable row, while this version restores every override —
 * including dates that have no meal records at all.
 */
internal object DayRulesBackup {
    const val PREFIX = "#FoodJournalDayRules:"

    private val json = Json { ignoreUnknownKeys = true }

    /** Null when there is nothing to carry, so plain exports stay unchanged. */
    fun lineFor(rules: Map<String, DayRule>): String? =
        if (rules.isEmpty()) null else PREFIX + json.encodeToString(rules.toSortedMap().toMap())

    /** Null when the line is absent or corrupt; a bad tag never fails an import. */
    fun fromLine(line: String): Map<String, DayRule>? {
        if (!line.startsWith(PREFIX)) return null
        return runCatching {
            json.decodeFromString<Map<String, DayRule>>(line.removePrefix(PREFIX))
        }.getOrNull()
    }
}
