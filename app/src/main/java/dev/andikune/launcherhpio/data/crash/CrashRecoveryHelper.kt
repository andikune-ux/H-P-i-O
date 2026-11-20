package dev.andikune.launcherhpio.data.crash

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.andikune.launcherhpio.data.storage.AppStorageHelper
import dev.andikune.launcherhpio.domain.model.CrashReportRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Captures uncaught JVM exceptions safely without obstructing Android's default handler,
 * redacts sensitive secrets/PII, prevents crash loops, and stores crash reports in
 * `filesDir/H P i O/Log Crash/`.
 */
class CrashRecoveryHelper(
    private val context: Context,
    private val storageHelper: AppStorageHelper
) {
    companion object {
        private const val PREFS_NAME = "hpio_crash_guard_prefs"
        private const val KEY_PENDING_CRASH_ID = "pending_crash_id"
        private const val KEY_CONSECUTIVE_FAST_CRASHES = "consecutive_fast_crashes"
        private const val KEY_LAST_CRASH_EPOCH = "last_crash_epoch"
        private const val KEY_LAST_LAUNCH_EPOCH = "last_launch_epoch"
        private const val FAST_CRASH_WINDOW_MS = 12_000L
        private const val CRASH_LOOP_THRESHOLD = 3
        private const val INDEX_FILE_NAME = "crash-index.json"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun installUncaughtExceptionHandler() {
        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (currentHandler is HpioUncaughtExceptionHandler) {
            return
        }
        prefs.edit().putLong(KEY_LAST_LAUNCH_EPOCH, System.currentTimeMillis()).apply()
        Thread.setDefaultUncaughtExceptionHandler(HpioUncaughtExceptionHandler(currentHandler))
    }

    fun isSafeModeTriggered(): Boolean {
        return prefs.getInt(KEY_CONSECUTIVE_FAST_CRASHES, 0) >= CRASH_LOOP_THRESHOLD
    }

    fun resetSafeModeCounter() {
        prefs.edit().putInt(KEY_CONSECUTIVE_FAST_CRASHES, 0).apply()
    }

    fun getPendingCrashReport(): CrashReportRecord? {
        val pendingId = prefs.getString(KEY_PENDING_CRASH_ID, null) ?: return null
        return loadAllCrashReports().firstOrNull { it.id == pendingId }
            ?: loadAllCrashReports().firstOrNull()
    }

    fun acknowledgePendingCrash() {
        prefs.edit().remove(KEY_PENDING_CRASH_ID).apply()
    }

    fun loadAllCrashReports(): List<CrashReportRecord> {
        return try {
            val indexFile = storageHelper.resolveSafeChild(storageHelper.crashLogsDir, INDEX_FILE_NAME)
            if (!indexFile.exists()) return emptyList()
            val raw = indexFile.readText()
            val array = JSONArray(raw)
            val result = mutableListOf<CrashReportRecord>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val filePath = obj.optString("filePath")
                val stackTrace = runCatching {
                    val file = File(filePath)
                    if (file.exists() && file.canonicalPath.startsWith(storageHelper.crashLogsDir.canonicalPath)) {
                        file.readText()
                    } else {
                        obj.optString("summary", "")
                    }
                }.getOrDefault("")

                result.add(
                    CrashReportRecord(
                        id = obj.optString("id"),
                        timestampIso = obj.optString("timestampIso"),
                        epochMillis = obj.optLong("epochMillis"),
                        appVersion = obj.optString("appVersion"),
                        versionCode = obj.optInt("versionCode", 1),
                        androidVersion = obj.optString("androidVersion"),
                        deviceModel = obj.optString("deviceModel"),
                        exceptionType = obj.optString("exceptionType"),
                        message = obj.optString("message"),
                        stackTrace = stackTrace,
                        filePath = filePath
                    )
                )
            }
            result.sortedByDescending { it.epochMillis }
        } catch (_: Exception) {
            // Never allow corrupted crash log index to trigger a recursive crash loop
            emptyList()
        }
    }

    fun recordException(threadName: String, throwable: Throwable): CrashReportRecord? {
        return try {
            val now = System.currentTimeMillis()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val fileStamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(now))

            val reportId = "crash-$fileStamp"
            val (versionName, versionCode) = readAppVersion()
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val redactedMessage = redactSensitiveData(throwable.message ?: "Tidak ada pesan exception")
            val redactedTrace = redactSensitiveData(sw.toString())

            val fullLog = buildString {
                appendLine("=== H P i O Crash Report ===")
                appendLine("Report ID      : $reportId")
                appendLine("Timestamp (UTC): ${isoFormat.format(Date(now))}")
                appendLine("App Version    : $versionName ($versionCode)")
                appendLine("Platform       : $androidVersion")
                appendLine("Device         : $deviceModel")
                appendLine("Thread         : ${redactSensitiveData(threadName)}")
                appendLine("Exception      : ${throwable::class.java.name}")
                appendLine("Message        : $redactedMessage")
                appendLine()
                appendLine("--- Stack Trace ---")
                appendLine(redactedTrace)
            }

            val logFile = storageHelper.resolveSafeChild(storageHelper.crashLogsDir, "$reportId.txt")
            storageHelper.writeTextAtomically(
                logFile,
                fullLog,
                maxBytes = AppStorageHelper.MAX_CRASH_FILE_BYTES
            )
            storageHelper.enforceCrashLogRetention()

            val record = CrashReportRecord(
                id = reportId,
                timestampIso = isoFormat.format(Date(now)),
                epochMillis = now,
                appVersion = versionName,
                versionCode = versionCode,
                androidVersion = androidVersion,
                deviceModel = deviceModel,
                exceptionType = throwable::class.java.simpleName.ifEmpty { throwable::class.java.name },
                message = redactedMessage,
                stackTrace = fullLog,
                filePath = logFile.absolutePath
            )

            updateIndexFile(record)
            updateCrashLoopCounters(now, reportId)
            record
        } catch (_: Exception) {
            null
        }
    }

    fun formatShareableReport(record: CrashReportRecord): String {
        return buildString {
            appendLine("H P i O — Laporan Crash")
            appendLine("Waktu: ${record.timestampIso}")
            appendLine("Versi: ${record.appVersion} (${record.versionCode})")
            appendLine("Perangkat: ${record.deviceModel} · ${record.androidVersion}")
            appendLine("Exception: ${record.exceptionType}")
            appendLine("Pesan: ${record.message}")
            appendLine()
            appendLine(record.stackTrace)
        }
    }

    fun redactSensitiveData(input: String): String {
        if (input.isEmpty()) return input
        var sanitized = input
        // Redact Bearer / Authorization tokens
        sanitized = sanitized.replace(
            Regex("(?i)(bearer\\s+)[A-Za-z0-9._\\-~+/]+=*"),
            "$1[REDACTED_TOKEN]"
        )
        // Redact key/token/password/secret key=value pairs
        sanitized = sanitized.replace(
            Regex("(?i)(token|secret|password|passwd|api_?key|auth|authorization)\\s*[:=]\\s*[^\\s,;\"']+"),
            "$1=[REDACTED]"
        )
        // Redact GitHub PATs or classic tokens
        sanitized = sanitized.replace(
            Regex("\\b(gh[pousr]_[A-Za-z0-9_]{12,}|github_pat_[A-Za-z0-9_]{12,})\\b"),
            "[REDACTED_GITHUB_TOKEN]"
        )
        // Redact email addresses
        sanitized = sanitized.replace(
            Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
            "[REDACTED_EMAIL]"
        )
        return sanitized
    }

    private fun updateIndexFile(newRecord: CrashReportRecord) {
        val indexFile = storageHelper.resolveSafeChild(storageHelper.crashLogsDir, INDEX_FILE_NAME)
        val existing = loadAllCrashReports().filter { it.id != newRecord.id }
        val updated = (listOf(newRecord) + existing).take(AppStorageHelper.MAX_CRASH_FILES)
        val jsonArray = JSONArray()
        updated.forEach { item ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("timestampIso", item.timestampIso)
                    put("epochMillis", item.epochMillis)
                    put("appVersion", item.appVersion)
                    put("versionCode", item.versionCode)
                    put("androidVersion", item.androidVersion)
                    put("deviceModel", item.deviceModel)
                    put("exceptionType", item.exceptionType)
                    put("message", item.message)
                    put("filePath", item.filePath)
                }
            )
        }
        storageHelper.writeTextAtomically(indexFile, jsonArray.toString(2))
    }

    private fun updateCrashLoopCounters(now: Long, reportId: String) {
        val lastLaunch = prefs.getLong(KEY_LAST_LAUNCH_EPOCH, 0L)
        val prevCount = prefs.getInt(KEY_CONSECUTIVE_FAST_CRASHES, 0)
        val isFastCrash = (now - lastLaunch) in 0..FAST_CRASH_WINDOW_MS
        val nextCount = if (isFastCrash) prevCount + 1 else 1
        prefs.edit()
            .putString(KEY_PENDING_CRASH_ID, reportId)
            .putInt(KEY_CONSECUTIVE_FAST_CRASHES, nextCount)
            .putLong(KEY_LAST_CRASH_EPOCH, now)
            .commit()
    }

    private fun readAppVersion(): Pair<String, Int> {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
            (info.versionName ?: "1.0.0") to code
        } catch (_: Exception) {
            "1.0.0" to 1
        }
    }

    private inner class HpioUncaughtExceptionHandler(
        private val defaultHandler: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                recordException(thread.name, throwable)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
