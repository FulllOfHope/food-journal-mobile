package com.messledger.app.domain

import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * The only thing the update check needs to remember about a release.
 * [versionName] is the release tag without a leading "v"; [notes] is the raw
 * Markdown body, stripped only when it is rendered.
 */
@Serializable
data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val releaseUrl: String
)

/**
 * One GET, one host. Injectable so the checker (and everything above it) can
 * be tested without touching the network.
 */
fun interface ReleaseFetcher {
    fun fetch(url: String): String
}

/** Production fetcher: plain JDK/Android APIs, no HTTP client dependency. */
class JdkReleaseFetcher : ReleaseFetcher {
    override fun fetch(url: String): String {
        val target = runCatching { URL(url) }
            .getOrElse { throw IllegalArgumentException("Invalid update URL", it) }
        // Defence in depth: even a future caller cannot turn this into a
        // general-purpose network client.
        require(target.protocol == "https" && target.host == "api.github.com") {
            "Update checks may only reach api.github.com over HTTPS"
        }

        val connection = target.openConnection() as? HttpsURLConnection
            ?: throw IllegalStateException("Update check requires HTTPS")
        connection.apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Update check returned $code")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val USER_AGENT = "FoodJournal-UpdateChecker/1.0"
    }
}

/**
 * Pure-ish release lookup. Network I/O is behind [ReleaseFetcher], so the
 * comparison, parsing and notification policy can be unit tested directly.
 * [checkForUpdate] performs blocking I/O and must be called from Dispatchers.IO.
 */
class GitHubUpdateChecker(
    private val currentVersion: String,
    private val fetcher: ReleaseFetcher = JdkReleaseFetcher()
) {
    fun checkForUpdate(): UpdateInfo? = runCatching {
        val payload = fetcher.fetch(LATEST_RELEASE_URL)
        parseRelease(payload)?.takeIf { isNewer(it.versionName, currentVersion) }
    }.getOrNull()

    companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/FulllOfHope/food-journal-mobile/releases/latest"

        /** Automatic checks are at most this far apart. */
        const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L

        /** "v1.6.0" and "1.6.0" are the same release. */
        fun normalizeVersion(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.startsWith('v', ignoreCase = true)) trimmed.drop(1) else trimmed
        }

        /**
         * Numeric, segment-by-segment comparison: 1.10.0 beats 1.9.0, and a
         * missing trailing segment counts as zero. Non-numeric suffixes such
         * as "-beta" are ignored rather than crashing the check.
         */
        fun compareVersions(left: String, right: String): Int {
            val a = normalizeVersion(left).split('.')
            val b = normalizeVersion(right).split('.')
            for (index in 0 until maxOf(a.size, b.size)) {
                val av = a.getOrNull(index).orEmpty().takeWhile(Char::isDigit).toLongOrNull() ?: 0L
                val bv = b.getOrNull(index).orEmpty().takeWhile(Char::isDigit).toLongOrNull() ?: 0L
                if (av != bv) return av.compareTo(bv)
            }
            return 0
        }

        fun isNewer(release: String, current: String): Boolean =
            compareVersions(release, current) > 0

        /** Parse the fields we display. Returns null for unusable payloads. */
        fun parseRelease(payload: String): UpdateInfo? {
            val release = runCatching { JSONObject(payload) }.getOrNull() ?: return null
            val tag = release.optString("tag_name").orEmpty().trim()
            if (tag.isEmpty()) return null

            val assets = release.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name").orEmpty()
                    if (!name.endsWith(".apk", ignoreCase = true)) continue
                    val download = asset.optString("browser_download_url").orEmpty()
                    if (download.isNotEmpty()) {
                        apkUrl = download
                        break
                    }
                }
            }
            if (apkUrl.isNullOrEmpty()) return null

            return UpdateInfo(
                versionName = normalizeVersion(tag),
                notes = release.optString("body").orEmpty(),
                apkUrl = apkUrl,
                releaseUrl = release.optString("html_url").orEmpty()
            )
        }

        /** Plain text for the bottom sheet; keeps the news, drops the markup. */
        fun stripMarkdown(raw: String): String {
            if (raw.isBlank()) return ""
            val cleaned = raw
                .replace(Regex("```[\\s\\S]*?```")) { it.value.removePrefix("```").removeSuffix("```").trim() }
                .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
                .replace(Regex("(?m)^\\s{0,3}>\\s?"), "")
                .replace(Regex("(?m)^\\s*([-*+]|\\d+\\.)\\s+"), "")
                .replace(Regex("(?m)^\\s*(-{3,}|\\*{3,})\\s*$"), "")
                .replace(Regex("\\*\\*|__|~~|`"), "")

            val lines = mutableListOf<String>()
            var previousBlank = false
            cleaned.lines().forEach { line ->
                val trimmed = line.trimEnd()
                when {
                    trimmed.isBlank() -> {
                        if (!previousBlank && lines.isNotEmpty()) lines += ""
                        previousBlank = true
                    }
                    else -> {
                        lines += trimmed
                        previousBlank = false
                    }
                }
            }
            return lines.joinToString("\n").trim()
        }

        /**
         * Notification policy: only a genuinely newer, unskipped version,
         * and never more often than the six-hour gate unless the user asked
         * for a manual refresh.
         */
        fun shouldNotify(
            update: UpdateInfo?,
            currentVersion: String,
            dismissedVersion: String = "",
            lastCheckAt: Long = 0L,
            now: Long = 0L,
            checksEnabled: Boolean = true,
            bypassTimeGate: Boolean = false
        ): Boolean {
            if (!checksEnabled) return false
            val info = update ?: return false
            if (!isNewer(info.versionName, currentVersion)) return false
            if (normalizeVersion(dismissedVersion) == normalizeVersion(info.versionName)) return false
            if (!bypassTimeGate && now - lastCheckAt < CHECK_INTERVAL_MS) return false
            return true
        }
    }
}
