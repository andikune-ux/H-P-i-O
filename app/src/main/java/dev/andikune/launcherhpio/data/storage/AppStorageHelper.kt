package dev.andikune.launcherhpio.data.storage

import android.content.Context
import android.os.StatFs
import dev.andikune.launcherhpio.domain.model.StorageSummary
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Manages structured private application storage under:
 * `filesDir/H P i O/`
 *   ├── Pembaruan Aplikasi/
 *   ├── Log Crash/
 *   └── Personalization/
 */
class AppStorageHelper(private val context: Context) {

    companion object {
        const val APP_ROOT_FOLDER = "H P i O"
        const val UPDATES_FOLDER = "Pembaruan Aplikasi"
        const val CRASH_LOGS_FOLDER = "Log Crash"
        const val PERSONALIZATION_FOLDER = "Personalization"

        const val MAX_CRASH_FILES = 10
        const val MAX_CRASH_FILE_BYTES = 256 * 1024L
        const val MAX_UPDATE_APK_FILES = 2
        const val MIN_FREE_SPACE_BUFFER_BYTES = 25 * 1024 * 1024L
    }

    val rootDir: File
        get() = File(context.filesDir, APP_ROOT_FOLDER).also { ensureDirectory(it) }

    val updatesDir: File
        get() = File(rootDir, UPDATES_FOLDER).also { ensureDirectory(it) }

    val crashLogsDir: File
        get() = File(rootDir, CRASH_LOGS_FOLDER).also { ensureDirectory(it) }

    val personalizationDir: File
        get() = File(rootDir, PERSONALIZATION_FOLDER).also { ensureDirectory(it) }

    fun ensureStructure() {
        ensureDirectory(rootDir)
        ensureDirectory(updatesDir)
        ensureDirectory(crashLogsDir)
        ensureDirectory(personalizationDir)
    }

    fun sanitizeFileName(rawName: String, fallback: String = "artifact"): String {
        val trimmed = rawName.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trimStart('.')
        return if (trimmed.isEmpty() || trimmed == "." || trimmed == "..") {
            fallback
        } else {
            trimmed.take(120)
        }
    }

    fun resolveSafeChild(parentDir: File, fileName: String): File {
        val cleanName = sanitizeFileName(fileName)
        val candidate = File(parentDir, cleanName).canonicalFile
        val parentCanonical = parentDir.canonicalFile
        if (!candidate.path.startsWith(parentCanonical.path + File.separator)) {
            throw SecurityException("Invalid file path outside structured app directory")
        }
        return candidate
    }

    @Throws(IOException::class)
    fun writeTextAtomically(targetFile: File, content: String, maxBytes: Long = Long.MAX_VALUE) {
        ensureDirectory(targetFile.parentFile ?: rootDir)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        val boundedBytes = if (bytes.size > maxBytes) {
            bytes.copyOf(maxBytes.toInt())
        } else {
            bytes
        }
        val tempFile = File(targetFile.parentFile, ".tmp_${targetFile.name}_${System.nanoTime()}")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(boundedBytes)
                fos.fd.sync()
            }
            if (targetFile.exists() && !targetFile.delete()) {
                throw IOException("Failed to replace existing file: ${targetFile.name}")
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun getAvailableFreeSpaceBytes(): Long {
        return try {
            ensureStructure()
            val stat = StatFs(rootDir.absolutePath)
            stat.availableBytes
        } catch (_: Exception) {
            0L
        }
    }

    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val available = getAvailableFreeSpaceBytes()
        return available >= (requiredBytes + MIN_FREE_SPACE_BUFFER_BYTES)
    }

    fun enforceCrashLogRetention(maxFiles: Int = MAX_CRASH_FILES) {
        val logFiles = crashLogsDir.listFiles { file ->
            file.isFile && file.name.startsWith("crash-") && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: return

        if (logFiles.size > maxFiles) {
            logFiles.drop(maxFiles).forEach { it.delete() }
        }
    }

    fun enforceUpdateApkRetention(maxApks: Int = MAX_UPDATE_APK_FILES, keepFileName: String? = null) {
        val apkFiles = updatesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".apk")
        }?.sortedByDescending { it.lastModified() } ?: return

        var kept = 0
        for (file in apkFiles) {
            if (keepFileName != null && file.name == keepFileName) {
                kept++
                continue
            }
            if (kept < maxApks) {
                kept++
            } else {
                file.delete()
            }
        }
    }

    fun clearDownloadedUpdates(): Int {
        var deletedCount = 0
        updatesDir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                deletedCount++
            }
        }
        return deletedCount
    }

    fun clearCrashLogs(): Int {
        var deletedCount = 0
        crashLogsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                deletedCount++
            }
        }
        return deletedCount
    }

    fun getSummary(): StorageSummary {
        ensureStructure()
        val updateFiles = updatesDir.listFiles()?.filter { it.isFile }.orEmpty()
        val crashFiles = crashLogsDir.listFiles()?.filter { it.isFile }.orEmpty()
        return StorageSummary(
            rootFolderPath = rootDir.absolutePath,
            updatesFolderPath = updatesDir.absolutePath,
            crashFolderPath = crashLogsDir.absolutePath,
            updatesFileCount = updateFiles.size,
            updatesBytes = updateFiles.sumOf { it.length() },
            crashFileCount = crashFiles.size,
            crashBytes = crashFiles.sumOf { it.length() },
            freeSpaceBytes = getAvailableFreeSpaceBytes()
        )
    }

    private fun ensureDirectory(dir: File) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
}
