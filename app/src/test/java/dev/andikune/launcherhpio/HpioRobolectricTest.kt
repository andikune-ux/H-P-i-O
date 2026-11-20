package dev.andikune.launcherhpio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.andikune.launcherhpio.data.crash.CrashRecoveryHelper
import dev.andikune.launcherhpio.data.permissions.PermissionCapabilityHelper
import dev.andikune.launcherhpio.data.storage.AppStorageHelper
import dev.andikune.launcherhpio.data.update.GitHubReleaseUpdateHelper
import dev.andikune.launcherhpio.domain.model.CapabilityAvailability
import dev.andikune.launcherhpio.domain.model.ReleaseUpdateInfo
import dev.andikune.launcherhpio.domain.model.UpdateDownloadState
import dev.andikune.launcherhpio.ui.dialogs.CrashRecoveryDialog
import dev.andikune.launcherhpio.ui.dialogs.UpdateAvailableDialog
import dev.andikune.launcherhpio.ui.theme.HpioTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HpioRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun manifestAndIdentity_supportHomeLauncherRole() {
        assertEquals("H P i O", context.getString(R.string.app_name))
        assertEquals("dev.andikune.launcherhpio", context.packageName)

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            setPackage(context.packageName)
        }
        val resolved = context.packageManager.queryIntentActivities(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        assertTrue(resolved.any { it.activityInfo.packageName == "dev.andikune.launcherhpio" })
    }

    @Test
    fun structuredStorage_createsMandatoryFoldersAndPreventsTraversal() {
        val storage = AppStorageHelper(context)
        storage.ensureStructure()

        assertTrue(storage.rootDir.exists())
        assertEquals("H P i O", storage.rootDir.name)
        assertTrue(storage.updatesDir.exists())
        assertEquals("Pembaruan Aplikasi", storage.updatesDir.name)
        assertTrue(storage.crashLogsDir.exists())
        assertEquals("Log Crash", storage.crashLogsDir.name)

        val safeFile = storage.resolveSafeChild(storage.crashLogsDir, "../../etc/passwd")
        assertTrue(safeFile.canonicalPath.startsWith(storage.crashLogsDir.canonicalPath + File.separator))

        storage.writeTextAtomically(safeFile, "atomic-ok")
        assertEquals("atomic-ok", safeFile.readText())
    }

    @Test
    fun crashRecovery_redactsSecretsAndPersistsPendingCrash() {
        val storage = AppStorageHelper(context)
        val crashHelper = CrashRecoveryHelper(context, storage)

        val record = crashHelper.recordException(
            threadName = "main",
            throwable = IllegalStateException("Bearer secretToken12345 failed for user@example.org")
        )
        assertNotNull(record)
        assertFalse(record!!.message.contains("secretToken12345"))
        assertFalse(record.message.contains("user@example.org"))
        assertTrue(record.message.contains("[REDACTED_TOKEN]"))
        assertTrue(record.message.contains("[REDACTED_EMAIL]"))

        val pending = crashHelper.getPendingCrashReport()
        assertNotNull(pending)
        assertEquals(record.id, pending?.id)

        crashHelper.acknowledgePendingCrash()
        assertNull(crashHelper.getPendingCrashReport())
    }

    @Test
    fun updateHelper_enforcesSemverOrderingTrustedUrlsAndSkipSemantics() {
        val storage = AppStorageHelper(context)
        val updater = GitHubReleaseUpdateHelper(context, storage)

        assertTrue(updater.compareVersions("1.0.10", "1.0.2") > 0)
        assertTrue(updater.compareVersions("1.0.0", "1.2.0") < 0)
        assertTrue(updater.isTrustedHttpsUrl("https://github.com/andikune-ux/H-P-i-O/releases/download/v1.0.1/hpio.apk"))
        assertFalse(updater.isTrustedHttpsUrl("http://github.com/andikune-ux/H-P-i-O/releases/download/v1.0.1/hpio.apk"))
        assertFalse(updater.isTrustedHttpsUrl("https://evil.example.net/hpio.apk"))

        val releases = JSONArray().apply {
            put(JSONObject().apply {
                put("tag_name", "v9.9.9")
                put("draft", false)
                put("prerelease", true)
                put("assets", JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "hpio-9.9.9.apk")
                        put("browser_download_url", "https://github.com/andikune-ux/H-P-i-O/releases/download/v9.9.9/hpio.apk")
                    })
                })
            })
            put(JSONObject().apply {
                put("tag_name", "v2.1.0")
                put("draft", false)
                put("prerelease", false)
                put("body", "Pembaruan stabil")
                put("assets", JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "hpio-release.apk")
                        put("browser_download_url", "https://github.com/andikune-ux/H-P-i-O/releases/download/v2.1.0/hpio-release.apk")
                        put("size", 1024L)
                    })
                })
            })
        }

        val selected = updater.selectBestStableRelease(releases, currentVersion = "1.0.0")
        assertNotNull(selected)
        assertEquals("2.1.0", selected?.versionName)

        updater.skipVersion("2.1.0")
        assertEquals("2.1.0", updater.getSkippedVersion())
        updater.clearSkippedVersion()
        assertNull(updater.getSkippedVersion())
    }

    @Test
    fun permissionsHelper_reportsCapabilitiesWithoutCrashingWhenDeniedOrUnavailable() {
        val permHelper = PermissionCapabilityHelper(context)
        val statuses = permHelper.collectCapabilityStatuses(
            activity = null,
            bluetoothRequestedBefore = false
        )
        assertTrue(statuses.isNotEmpty())
        val flashStatus = statuses.first { it.id == "camera_flash" }
        assertTrue(
            flashStatus.availability == CapabilityAvailability.GRANTED ||
                flashStatus.availability == CapabilityAvailability.UNAVAILABLE
        )
    }

    @Test
    fun crashRecoveryDialog_displaysCopyAndBackActions() {
        val storage = AppStorageHelper(context)
        val crashHelper = CrashRecoveryHelper(context, storage)
        val record = crashHelper.recordException("main", RuntimeException("Simulated UI crash"))!!
        var backClicked = false

        composeTestRule.setContent {
            HpioTheme {
                CrashRecoveryDialog(
                    report = record,
                    formattedShareText = crashHelper.formatShareableReport(record),
                    safeModeActive = false,
                    onDismissBack = { backClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("crash_copy_log_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("crash_back_button").assertIsDisplayed().performClick()
        assertTrue(backClicked)
    }

    @Test
    fun updateAvailableDialog_displaysSkipLaterAndUpdateActions() {
        val update = ReleaseUpdateInfo(
            tagName = "v1.1.0",
            versionName = "1.1.0",
            publishedAt = "2026-11-20",
            releaseNotes = "- Liquid Glass refinement",
            apkDownloadUrl = "https://github.com/andikune-ux/H-P-i-O/releases/download/v1.1.0/hpio.apk",
            apkFileName = "hpio-1.1.0.apk",
            apkSizeBytes = 12_500_000L,
            expectedSha256 = null
        )
        var skipped: String? = null
        var laterClicked = false

        composeTestRule.setContent {
            HpioTheme {
                UpdateAvailableDialog(
                    updateInfo = update,
                    downloadState = UpdateDownloadState.Idle,
                    onSkip = { skipped = it },
                    onLater = { laterClicked = true },
                    onUpdate = {},
                    onRequestInstallPermission = {},
                    onInstallReadyApk = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("update_skip_button").assertIsDisplayed().performClick()
        assertEquals("1.1.0", skipped)

        composeTestRule.onNodeWithTag("update_later_button").assertIsDisplayed().performClick()
        assertTrue(laterClicked)
        composeTestRule.onNodeWithTag("update_confirm_button").assertIsDisplayed()
    }
}
