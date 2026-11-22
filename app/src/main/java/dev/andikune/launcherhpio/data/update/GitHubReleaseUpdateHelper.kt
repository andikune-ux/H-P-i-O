package dev.andikune.launcherhpio.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.andikune.launcherhpio.BuildConfig
import dev.andikune.launcherhpio.data.storage.AppStorageHelper
import dev.andikune.launcherhpio.domain.model.ReleaseUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class NoCompatibleRelease(val message: String) : UpdateCheckResult
    data class OfflineOrSkipped(val reason: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface DownloadOutcome {
    data class Success(val apkFile: File) : DownloadOutcome
    data class Failure(val message: String) : DownloadOutcome
}

/**
 * Checks official GitHub Releases for `andikune-ux/H-P-i-O` asynchronously without any embedded token,
 * verifies release version ordering, host allowlist, APK integrity/checksum, and triggers official
 * Android package installation consent via FileProvider.
 */
class GitHubReleaseUpdateHelper(
    private val context: Context,
    private val storageHelper: AppStorageHelper,
    private val baseApiUrl: String = OFFICIAL_RELEASES_URL
) {
    companion object {
        const val REPO_OWNER = "andikune-ux"
        const val REPO_NAME = "H-P-i-O"
        const val OFFICIAL_RELEASES_URL =
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases?per_page=10"

        private const val PREFS_NAME = "hpio_update_prefs"
        private const val KEY_SKIPPED_VERSION = "skipped_version"
        private const val KEY_LAST_CHECK_EPOCH = "last_check_epoch"
        private const val KEY_LATER_SESSION_SUPPRESSED = "later_suppressed"
        private const val METADATA_FILE = "metadata.json"

        private val TRUSTED_HOSTS = setOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSkippedVersion(): String? = prefs.getString(KEY_SKIPPED_VERSION, null)

    fun skipVersion(versionName: String) {
        prefs.edit().putString(KEY_SKIPPED_VERSION, normalizeVersion(versionName)).apply()
    }

    fun clearSkippedVersion() {
        prefs.edit().remove(KEY_SKIPPED_VERSION).apply()
    }

    /**
     * Marks "Nanti" in-memory/per-launch; cleared automatically when a new launcher process starts
     * or when manual check is invoked.
     */
    private var laterDismissedThisLaunch = false

    fun dismissForCurrentLaunch() {
        laterDismissedThisLaunch = true
    }

    fun resetLaunchDismissal() {
        laterDismissedThisLaunch = false
    }

    suspend fun checkForUpdates(forceManual: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!forceManual && laterDismissedThisLaunch) {
            return@withContext UpdateCheckResult.OfflineOrSkipped("Ditunda untuk sesi peluncuran ini")
        }

        if (!isNetworkConnected()) {
            return@withContext UpdateCheckResult.OfflineOrSkipped("Perangkat sedang offline")
        }

        if (!isTrustedHttpsUrl(baseApiUrl)) {
            return@withContext UpdateCheckResult.Error("Sumber pembaruan tidak tepercaya")
        }

        try {
            val request = Request.Builder()
                .url(baseApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "HPIO-Launcher/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    404 -> return@withContext UpdateCheckResult.NoCompatibleRelease(
                        "Belum ada release resmi yang dipublikasikan di $REPO_OWNER/$REPO_NAME."
                    )
                    403, 429 -> return@withContext UpdateCheckResult.Error(
                        "Batas permintaan GitHub API tercapai. Coba lagi nanti."
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("Gagal memeriksa pembaruan (HTTP ${response.code}).")
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext UpdateCheckResult.NoCompatibleRelease("Respons release kosong.")
                }

                prefs.edit().putLong(KEY_LAST_CHECK_EPOCH, System.currentTimeMillis()).apply()
                val releasesArray = JSONArray(body)
                val parsed = selectBestStableRelease(
                    releasesArray = releasesArray,
                    currentVersion = BuildConfig.VERSION_NAME
                )

                return@withContext when (parsed) {
                    null -> UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                    else -> {
                        val skipped = getSkippedVersion()
                        if (!forceManual && skipped != null && compareVersions(parsed.versionName, skipped) <= 0) {
                            UpdateCheckResult.OfflineOrSkipped("Versi ${parsed.versionName} dilewati oleh pengguna")
                        } else {
                            UpdateCheckResult.UpdateAvailable(parsed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Tidak dapat menghubungi GitHub Releases: ${e.message ?: "kesalahan jaringan"}")
        }
    }

    internal fun selectBestStableRelease(
        releasesArray: JSONArray,
        currentVersion: String
    ): ReleaseUpdateInfo? {
        var best: ReleaseUpdateInfo? = null
        for (i in 0 until releasesArray.length()) {
            val rel = releasesArray.optJSONObject(i) ?: continue
            if (rel.optBoolean("draft", false)) continue
            if (rel.optBoolean("prerelease", false)) continue

            val tagName = rel.optString("tag_name").trim()
            val candidateVersion = normalizeVersion(tagName)
            if (parseVersionTuple(candidateVersion) == null) continue
            if (compareVersions(candidateVersion, currentVersion) <= 0) continue

            val assets = rel.optJSONArray("assets") ?: continue
            val apkAsset = findCompatibleApkAsset(assets) ?: continue
            val downloadUrl = apkAsset.optString("browser_download_url")
            if (!isTrustedHttpsUrl(downloadUrl)) continue

            val releaseNotes = rel.optString("body").ifBlank {
                "Pembaruan stabil H P i O versi $candidateVersion."
            }
            val expectedSha = extractSha256(releaseNotes, apkAsset.optString("digest"))
            val info = ReleaseUpdateInfo(
                tagName = tagName,
                versionName = candidateVersion,
                publishedAt = rel.optString("published_at").take(10),
                releaseNotes = releaseNotes,
                apkDownloadUrl = downloadUrl,
                apkFileName = storageHelper.sanitizeFileName(apkAsset.optString("name"), "hpio-$candidateVersion.apk"),
                apkSizeBytes = apkAsset.optLong("size", 0L),
                expectedSha256 = expectedSha
            )

            if (best == null || compareVersions(info.versionName, best.versionName) > 0) {
                best = info
            }
        }
        return best
    }

    private fun findCompatibleApkAsset(assets: JSONArray): JSONObject? {
        val candidates = mutableListOf<JSONObject>()
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name").lowercase()
            if (name.endsWith(".apk") && !name.contains("unsigned")) {
                candidates.add(asset)
            }
        }
        if (candidates.isEmpty()) return null
        val deviceAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }
        // Prefer universal or ABI-matching APK
        return candidates.firstOrNull { asset ->
            val n = asset.optString("name").lowercase()
            deviceAbis.any { abi -> n.contains(abi) }
        } ?: candidates.firstOrNull { asset ->
            val n = asset.optString("name").lowercase()
            n.contains("universal") || n.contains("release")
        } ?: candidates.first()
    }

    suspend fun downloadReleaseApk(
        info: ReleaseUpdateInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        if (!isTrustedHttpsUrl(info.apkDownloadUrl)) {
            return@withContext DownloadOutcome.Failure("URL unduhan ditolak karena tidak berasal dari domain GitHub resmi.")
        }

        val requiredSize = if (info.apkSizeBytes > 0L) info.apkSizeBytes else 20 * 1024 * 1024L
        if (!storageHelper.hasEnoughSpace(requiredSize)) {
            return@withContext DownloadOutcome.Failure(
                "Ruang penyimpanan internal tidak mencukupi untuk mengunduh pembaruan."
            )
        }

        val safeApkName = storageHelper.sanitizeFileName("hpio-${info.versionName}.apk")
        val targetFile = storageHelper.resolveSafeChild(storageHelper.updatesDir, safeApkName)
        val tempFile = storageHelper.resolveSafeChild(storageHelper.updatesDir, ".downloading_$safeApkName")

        try {
            val request = Request.Builder()
                .url(info.apkDownloadUrl)
                .header("User-Agent", "HPIO-Launcher/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadOutcome.Failure("Gagal mengunduh APK (HTTP ${response.code}).")
                }
                val body = response.body ?: return@withContext DownloadOutcome.Failure("Berkas APK kosong.")
                val total = if (body.contentLength() > 0) body.contentLength() else info.apkSizeBytes

                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                val buffer = ByteArray(16 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            val pct = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
                            onProgress(pct, downloaded, total)
                        }
                        output.fd.sync()
                    }
                }

                val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (!info.expectedSha256.isNullOrBlank() &&
                    !actualSha256.equals(info.expectedSha256, ignoreCase = true)
                ) {
                    tempFile.delete()
                    return@withContext DownloadOutcome.Failure(
                        "Verifikasi integritas SHA-256 gagal. Berkas unduhan dibatalkan demi keamanan."
                    )
                }

                if (!verifyDownloadedApkIdentity(tempFile)) {
                    tempFile.delete()
                    return@withContext DownloadOutcome.Failure(
                        "Paket APK tidak cocok dengan identitas package ${BuildConfig.APPLICATION_ID}."
                    )
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                saveUpdateMetadata(info, safeApkName, actualSha256)
                storageHelper.enforceUpdateApkRetention(keepFileName = safeApkName)
                DownloadOutcome.Success(targetFile)
            }
        } catch (e: Exception) {
            tempFile.delete()
            DownloadOutcome.Failure("Unduhan terputus: ${e.message ?: "kesalahan tidak diketahui"}")
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createManageUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createInstallApkIntent(apkFile: File): Intent {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    internal fun verifyDownloadedApkIdentity(apkFile: File): Boolean {
        return try {
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
            archiveInfo != null && archiveInfo.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    private fun saveUpdateMetadata(info: ReleaseUpdateInfo, fileName: String, sha256: String) {
        val meta = JSONObject().apply {
            put("tagName", info.tagName)
            put("versionName", info.versionName)
            put("apkFileName", fileName)
            put("sha256", sha256)
            put("downloadedAtEpoch", System.currentTimeMillis())
        }
        val metaFile = storageHelper.resolveSafeChild(storageHelper.updatesDir, METADATA_FILE)
        storageHelper.writeTextAtomically(metaFile, meta.toString(2))
    }

    private fun extractSha256(body: String, digestField: String): String? {
        if (digestField.startsWith("sha256:", ignoreCase = true)) {
            val candidate = digestField.substringAfter(":").trim()
            if (candidate.matches(Regex("[0-9a-fA-F]{64}"))) return candidate
        }
        val match = Regex("\\b([0-9a-fA-F]{64})\\b").find(body)
        return match?.groupValues?.getOrNull(1)
    }

    private fun isNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    internal fun isTrustedHttpsUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return scheme == "https" && host in TRUSTED_HOSTS
    }

    internal fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v.").removePrefix("v").removePrefix("V")
    }

    internal fun parseVersionTuple(raw: String): List<Int>? {
        val cleaned = normalizeVersion(raw).substringBefore("-").substringBefore("+")
        val parts = cleaned.split(".")
        if (parts.isEmpty()) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        return nums
    }

    internal fun compareVersions(a: String, b: String): Int {
        val pa = parseVersionTuple(a) ?: return 0
        val pb = parseVersionTuple(b) ?: return 0
        val maxLen = maxOf(pa.size, pb.size)
        for (i in 0 until maxLen) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }
}
