package com.messledger.app

import com.messledger.app.domain.GitHubUpdateChecker
import com.messledger.app.domain.ReleaseFetcher
import com.messledger.app.domain.UpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** All checks are pure or use a fake fetcher — no test ever opens a socket. */
class GitHubUpdateCheckerTest {

    private val payload = """
        {
          "tag_name": "v1.6.0",
          "name": "Tangerine Grove at Night",
          "body": "## What's new\n- **Faster** startup\n- Fixed the [bill receipt](https://example.com/receipt) layout\n\nInstall `food-journal.apk`.\n",
          "html_url": "https://github.com/FulllOfHope/food-journal-mobile/releases/tag/v1.6.0",
          "assets": [
            {
              "name": "release-notes.txt",
              "browser_download_url": "https://github.com/FulllOfHope/food-journal-mobile/releases/download/v1.6.0/release-notes.txt"
            },
            {
              "name": "food-journal.apk",
              "browser_download_url": "https://github.com/FulllOfHope/food-journal-mobile/releases/download/v1.6.0/food-journal.apk"
            }
          ]
        }
    """.trimIndent()

    private val update = UpdateInfo(
        versionName = "1.6.0",
        notes = "## Notes",
        apkUrl = "https://example.com/food-journal.apk",
        releaseUrl = "https://example.com/release"
    )

    @Test
    fun versionComparisonIsNumeric() {
        assertTrue(GitHubUpdateChecker.isNewer("1.6.0", "1.5.1"))
        assertFalse(GitHubUpdateChecker.isNewer("1.5.1", "1.6.0"))
        assertTrue(GitHubUpdateChecker.isNewer("1.10.0", "1.9.0"))
        assertFalse(GitHubUpdateChecker.isNewer("1.9.0", "1.10.0"))
        assertEquals(0, GitHubUpdateChecker.compareVersions("1.5.1", "1.5.1"))
    }

    @Test
    fun versionComparisonHandlesTagsAndUnevenSegments() {
        assertTrue(GitHubUpdateChecker.isNewer("v1.6.0", "1.5.1"))
        assertTrue(GitHubUpdateChecker.isNewer("V1.6.0", "v1.5.9"))
        assertEquals("1.6.0", GitHubUpdateChecker.normalizeVersion("v1.6.0"))
        assertEquals(0, GitHubUpdateChecker.compareVersions("1.6", "1.6.0"))
        assertTrue(GitHubUpdateChecker.isNewer("1.6.1", "1.6"))
        // A pre-release suffix must not break the numeric comparison.
        assertEquals(0, GitHubUpdateChecker.compareVersions("1.6.0-beta", "1.6.0"))
    }

    @Test
    fun parsesTagNotesReleaseUrlAndFirstApk() {
        val parsed = GitHubUpdateChecker.parseRelease(payload)
        assertNotNull(parsed)
        assertEquals("1.6.0", parsed!!.versionName)
        assertEquals("https://github.com/FulllOfHope/food-journal-mobile/releases/tag/v1.6.0", parsed.releaseUrl)
        assertEquals(
            "https://github.com/FulllOfHope/food-journal-mobile/releases/download/v1.6.0/food-journal.apk",
            parsed.apkUrl
        )
        assertTrue(parsed.notes.contains("## What's new"))
        assertTrue(parsed.notes.contains("**Faster**"))
    }

    @Test
    fun stripsMarkdownForTheSheet() {
        val parsed = GitHubUpdateChecker.parseRelease(payload)!!
        val plain = GitHubUpdateChecker.stripMarkdown(parsed.notes)
        assertTrue(plain.contains("What's new"))
        assertTrue(plain.contains("Faster startup"))
        assertTrue(plain.contains("Fixed the bill receipt layout"))
        assertFalse(plain.contains("##"))
        assertFalse(plain.contains("**"))
        assertFalse(plain.contains("]("))
        assertFalse(plain.contains("`"))
    }

    @Test
    fun missingApkOrTagProducesNoUpdate() {
        assertNull(GitHubUpdateChecker.parseRelease("""{"tag_name": "v1.6.0", "assets": []}"""))
        assertNull(GitHubUpdateChecker.parseRelease("""{"body": "no tag"}"""))
        assertNull(GitHubUpdateChecker.parseRelease("not json"))
    }

    @Test
    fun injectedFetcherReturnsOnlyNewerReleases() {
        var calls = 0
        val fetcher = ReleaseFetcher {
            calls++
            payload
        }
        val checker = GitHubUpdateChecker(currentVersion = "1.5.1", fetcher = fetcher)
        val found = checker.checkForUpdate()

        assertNotNull(found)
        assertEquals("1.6.0", found!!.versionName)
        assertEquals(1, calls)
        assertEquals(GitHubUpdateChecker.LATEST_RELEASE_URL, "https://api.github.com/repos/FulllOfHope/food-journal-mobile/releases/latest")

        val upToDate = GitHubUpdateChecker(
            currentVersion = "1.6.0",
            fetcher = ReleaseFetcher { payload }
        )
        assertNull(upToDate.checkForUpdate())
    }

    @Test
    fun fetchFailureIsSilent() {
        val failing = ReleaseFetcher { throw IllegalStateException("offline") }
        assertNull(GitHubUpdateChecker(currentVersion = "1.5.1", fetcher = failing).checkForUpdate())
    }

    @Test
    fun shouldNotifyRequiresANewerVersion() {
        assertTrue(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.5.1",
                now = GitHubUpdateChecker.CHECK_INTERVAL_MS
            )
        )
        assertFalse(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.6.0",
                now = GitHubUpdateChecker.CHECK_INTERVAL_MS
            )
        )
        assertFalse(
            GitHubUpdateChecker.shouldNotify(
                update = null,
                currentVersion = "1.5.1",
                now = GitHubUpdateChecker.CHECK_INTERVAL_MS
            )
        )
    }

    @Test
    fun shouldNotifyRespectsSkippedVersion() {
        assertFalse(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.5.1",
                dismissedVersion = "v1.6.0",
                now = GitHubUpdateChecker.CHECK_INTERVAL_MS
            )
        )
    }

    @Test
    fun shouldNotifyUsesTheSixHourGate() {
        assertFalse(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.5.1",
                lastCheckAt = 1_000L,
                now = 1_000L + GitHubUpdateChecker.CHECK_INTERVAL_MS - 1
            )
        )
        assertTrue(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.5.1",
                lastCheckAt = 1_000L,
                now = 1_000L + GitHubUpdateChecker.CHECK_INTERVAL_MS
            )
        )
        assertTrue(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.5.1",
                lastCheckAt = 1_000L,
                now = 2_000L,
                bypassTimeGate = true
            )
        )
    }

    @Test
    fun shouldNotifyRespectsTheSwitch() {
        assertFalse(
            GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = "1.5.1",
                now = GitHubUpdateChecker.CHECK_INTERVAL_MS,
                checksEnabled = false
            )
        )
    }
}
