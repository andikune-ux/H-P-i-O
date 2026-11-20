package dev.andikune.launcherhpio.ui

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.andikune.launcherhpio.HpioApplication
import dev.andikune.launcherhpio.data.launcher.ControlCenterSystemHelper
import dev.andikune.launcherhpio.data.launcher.InstalledAppsRepository
import dev.andikune.launcherhpio.data.launcher.LauncherAppActionsHelper
import dev.andikune.launcherhpio.data.launcher.LauncherPreferencesRepository
import dev.andikune.launcherhpio.data.launcher.LauncherWidgetHelper
import dev.andikune.launcherhpio.data.launcher.PersistedLayout
import dev.andikune.launcherhpio.data.permissions.PermissionCapabilityHelper
import dev.andikune.launcherhpio.data.update.DownloadOutcome
import dev.andikune.launcherhpio.data.update.GitHubReleaseUpdateHelper
import dev.andikune.launcherhpio.data.update.UpdateCheckResult
import dev.andikune.launcherhpio.domain.model.AppCategory
import dev.andikune.launcherhpio.domain.model.BuiltInWallpaper
import dev.andikune.launcherhpio.domain.model.BuiltInWidgetType
import dev.andikune.launcherhpio.domain.model.CapabilityStatusItem
import dev.andikune.launcherhpio.domain.model.ControlCenterState
import dev.andikune.launcherhpio.domain.model.CrashReportRecord
import dev.andikune.launcherhpio.domain.model.DrawerSortOrder
import dev.andikune.launcherhpio.domain.model.HomeGridItem
import dev.andikune.launcherhpio.domain.model.HomePageData
import dev.andikune.launcherhpio.domain.model.InstalledApp
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.domain.model.ReleaseUpdateInfo
import dev.andikune.launcherhpio.domain.model.StorageSummary
import dev.andikune.launcherhpio.domain.model.UpdateDownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ActiveOverlay {
    NONE,
    APP_DRAWER,
    CONTROL_CENTER,
    SEARCH,
    PERSONALIZATION,
    SYSTEM_HUB
}

data class LauncherUiState(
    val isLoadingApps: Boolean = true,
    val installedApps: List<InstalledApp> = emptyList(),
    val pages: List<HomePageData> = listOf(HomePageData(0, emptyList())),
    val dockApps: List<HomeGridItem.AppShortcut> = emptyList(),
    val currentHomePage: Int = 0,
    val preferences: LauncherPreferences = LauncherPreferences(),
    val activeOverlay: ActiveOverlay = ActiveOverlay.NONE,
    val openFolderId: String? = null,
    val selectedAppForActions: InstalledApp? = null,
    val showWidgetPickerSheet: Boolean = false,
    val drawerSearchQuery: String = "",
    val drawerSelectedCategory: AppCategory = AppCategory.ALL,
    val universalSearchQuery: String = "",
    val controlCenterState: ControlCenterState = ControlCenterState(),
    val pendingCrashReport: CrashReportRecord? = null,
    val crashHistory: List<CrashReportRecord> = emptyList(),
    val safeModeActive: Boolean = false,
    val availableUpdate: ReleaseUpdateInfo? = null,
    val updateDownloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val lastUpdateStatusMessage: String? = null,
    val skippedVersion: String? = null,
    val capabilityStatuses: List<CapabilityStatusItem> = emptyList(),
    val storageSummary: StorageSummary? = null,
    val snackbarMessage: String? = null,
    val pendingInstallApkPath: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HpioApplication
    val storageHelper = app.storageHelper
    val crashRecoveryHelper = app.crashRecoveryHelper
    val permissionHelper = PermissionCapabilityHelper(application)
    val updateHelper = GitHubReleaseUpdateHelper(application, storageHelper)
    val appsRepository = InstalledAppsRepository(application)
    val preferencesRepository = LauncherPreferencesRepository(application, storageHelper)
    val appActionsHelper = LauncherAppActionsHelper(application)
    val controlCenterHelper = ControlCenterSystemHelper(application, permissionHelper)
    val widgetHelper = LauncherWidgetHelper(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var bluetoothPermissionRequestedBefore = false

    init {
        observePreferences()
        loadInitialState()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val safeMode = crashRecoveryHelper.isSafeModeTriggered()
            if (safeMode) {
                preferencesRepository.updatePreferences {
                    it.copy(
                        safeModeEnabled = true,
                        blurIntensityDp = 0f
                    )
                }
            }
            val pendingCrash = crashRecoveryHelper.getPendingCrashReport()
            val crashHistory = crashRecoveryHelper.loadAllCrashReports()

            val apps = appsRepository.loadInstalledApps()
            val layout = preferencesRepository.loadOrSeedHomeLayout(apps)
            val ccState = controlCenterHelper.readState()
            val storageSummary = storageHelper.getSummary()

            _uiState.update {
                it.copy(
                    isLoadingApps = false,
                    installedApps = apps,
                    pages = layout.pages,
                    dockApps = layout.dockApps,
                    controlCenterState = ccState,
                    pendingCrashReport = pendingCrash,
                    crashHistory = crashHistory,
                    safeModeActive = safeMode,
                    skippedVersion = updateHelper.getSkippedVersion(),
                    storageSummary = storageSummary
                )
            }

            // Asynchronous non-blocking startup check for official GitHub Releases
            checkForUpdates(forceManual = false)
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = appsRepository.loadInstalledApps()
            val existingKeys = apps.map { it.componentKey }.toSet()

            val cleanedPages = _uiState.value.pages.map { page ->
                page.copy(
                    items = page.items.mapNotNull { item ->
                        when (item) {
                            is HomeGridItem.AppShortcut ->
                                if ("${item.packageName}/${item.activityName}" in existingKeys) item else null
                            is HomeGridItem.FolderItem -> {
                                val validApps = item.apps.filter {
                                    "${it.packageName}/${it.activityName}" in existingKeys
                                }
                                item.copy(apps = validApps)
                            }
                            else -> item
                        }
                    }
                )
            }
            val cleanedDock = _uiState.value.dockApps.filter {
                "${it.packageName}/${it.activityName}" in existingKeys
            }

            preferencesRepository.saveHomeLayout(PersistedLayout(cleanedPages, cleanedDock))
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    pages = cleanedPages,
                    dockApps = cleanedDock
                )
            }
        }
    }

    fun refreshSystemDiagnostics(activity: Activity?) {
        val ccState = controlCenterHelper.readState()
        val capabilities = permissionHelper.collectCapabilityStatuses(
            activity = activity,
            bluetoothRequestedBefore = bluetoothPermissionRequestedBefore
        )
        val summary = storageHelper.getSummary()
        val crashes = crashRecoveryHelper.loadAllCrashReports()
        _uiState.update {
            it.copy(
                controlCenterState = ccState,
                capabilityStatuses = capabilities,
                storageSummary = summary,
                crashHistory = crashes,
                skippedVersion = updateHelper.getSkippedVersion()
            )
        }
    }

    fun markBluetoothPermissionRequested() {
        bluetoothPermissionRequestedBefore = true
    }

    fun acknowledgeCrashReport() {
        crashRecoveryHelper.acknowledgePendingCrash()
        crashRecoveryHelper.resetSafeModeCounter()
        viewModelScope.launch {
            preferencesRepository.updatePreferences { it.copy(safeModeEnabled = false) }
        }
        _uiState.update { it.copy(pendingCrashReport = null, safeModeActive = false) }
    }

    fun openOverlay(overlay: ActiveOverlay) {
        _uiState.update {
            it.copy(
                activeOverlay = overlay,
                openFolderId = null,
                selectedAppForActions = null
            )
        }
        if (overlay == ActiveOverlay.CONTROL_CENTER || overlay == ActiveOverlay.SYSTEM_HUB) {
            _uiState.update { it.copy(controlCenterState = controlCenterHelper.readState()) }
        }
    }

    fun closeOverlay() {
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.NONE,
                openFolderId = null,
                selectedAppForActions = null,
                showWidgetPickerSheet = false
            )
        }
    }

    fun navigateHome() {
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.NONE,
                openFolderId = null,
                selectedAppForActions = null,
                showWidgetPickerSheet = false,
                currentHomePage = 0
            )
        }
    }

    fun setCurrentHomePage(pageIndex: Int) {
        val clamped = pageIndex.coerceIn(0, (_uiState.value.pages.size - 1).coerceAtLeast(0))
        _uiState.update { it.copy(currentHomePage = clamped) }
    }

    fun openFolder(folderId: String) {
        _uiState.update { it.copy(openFolderId = folderId) }
    }

    fun closeFolder() {
        _uiState.update { it.copy(openFolderId = null) }
    }

    fun openAppActions(appEntry: InstalledApp) {
        _uiState.update { it.copy(selectedAppForActions = appEntry) }
    }

    fun closeAppActions() {
        _uiState.update { it.copy(selectedAppForActions = null) }
    }

    fun setWidgetPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showWidgetPickerSheet = visible) }
    }

    fun launchApp(packageName: String, activityName: String, label: String) {
        val ok = appActionsHelper.launchApp(packageName, activityName)
        if (!ok) {
            showSnackbar("Tidak dapat membuka $label")
        } else {
            _uiState.update {
                it.copy(
                    activeOverlay = ActiveOverlay.NONE,
                    openFolderId = null,
                    selectedAppForActions = null
                )
            }
        }
    }

    fun pinAppToCurrentHomePage(appEntry: InstalledApp) {
        val shortcut = HomeGridItem.AppShortcut(
            packageName = appEntry.packageName,
            activityName = appEntry.activityName,
            fallbackLabel = appEntry.label
        )
        val targetPage = _uiState.value.currentHomePage
        val updatedPages = _uiState.value.pages.map { page ->
            if (page.pageIndex == targetPage) {
                page.copy(items = page.items + shortcut)
            } else {
                page
            }
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        closeAppActions()
        showSnackbar("${appEntry.label} ditambahkan ke Layar Utama")
    }

    fun removeItemFromHomePage(pageIndex: Int, itemId: String) {
        val targetItem = _uiState.value.pages.getOrNull(pageIndex)?.items?.firstOrNull { it.id == itemId }
        if (targetItem is HomeGridItem.SystemWidgetItem) {
            widgetHelper.deleteWidgetId(targetItem.appWidgetId)
        }
        val updatedPages = _uiState.value.pages.map { page ->
            if (page.pageIndex == pageIndex) {
                page.copy(items = page.items.filterNot { it.id == itemId })
            } else {
                page
            }
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        showSnackbar("Item dihapus dari Layar Utama")
    }

    fun pinAppToDock(appEntry: InstalledApp) {
        val currentDock = _uiState.value.dockApps
        if (currentDock.any { it.packageName == appEntry.packageName && it.activityName == appEntry.activityName }) {
            showSnackbar("${appEntry.label} sudah ada di Dock")
            closeAppActions()
            return
        }
        if (currentDock.size >= 5) {
            showSnackbar("Dock maksimal 5 aplikasi. Lepas salah satu terlebih dahulu.")
            closeAppActions()
            return
        }
        val updatedDock = currentDock + HomeGridItem.AppShortcut(
            packageName = appEntry.packageName,
            activityName = appEntry.activityName,
            fallbackLabel = appEntry.label
        )
        persistLayout(_uiState.value.pages, updatedDock)
        closeAppActions()
        showSnackbar("${appEntry.label} disematkan ke Dock")
    }

    fun removeAppFromDock(shortcutId: String) {
        val updatedDock = _uiState.value.dockApps.filterNot { it.id == shortcutId }
        persistLayout(_uiState.value.pages, updatedDock)
        showSnackbar("Aplikasi dilepas dari Dock")
    }

    fun createFolderWithApp(appEntry: InstalledApp, folderName: String = "Folder Baru") {
        val shortcut = HomeGridItem.AppShortcut(
            packageName = appEntry.packageName,
            activityName = appEntry.activityName,
            fallbackLabel = appEntry.label
        )
        val folder = HomeGridItem.FolderItem(
            name = folderName.trim().ifEmpty { "Folder Baru" },
            apps = listOf(shortcut)
        )
        val targetPage = _uiState.value.currentHomePage
        val updatedPages = _uiState.value.pages.map { page ->
            if (page.pageIndex == targetPage) {
                page.copy(items = page.items + folder)
            } else {
                page
            }
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        closeAppActions()
        showSnackbar("Folder '${folder.name}' dibuat")
    }

    fun addAppToFolder(folderId: String, appEntry: InstalledApp) {
        val shortcut = HomeGridItem.AppShortcut(
            packageName = appEntry.packageName,
            activityName = appEntry.activityName,
            fallbackLabel = appEntry.label
        )
        val updatedPages = _uiState.value.pages.map { page ->
            page.copy(
                items = page.items.map { item ->
                    if (item is HomeGridItem.FolderItem && item.id == folderId) {
                        val exists = item.apps.any {
                            it.packageName == shortcut.packageName && it.activityName == shortcut.activityName
                        }
                        if (exists) item else item.copy(apps = item.apps + shortcut)
                    } else {
                        item
                    }
                }
            )
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        closeAppActions()
        showSnackbar("${appEntry.label} ditambahkan ke folder")
    }

    fun renameFolder(folderId: String, newName: String) {
        val clean = newName.trim().ifEmpty { return }
        val updatedPages = _uiState.value.pages.map { page ->
            page.copy(
                items = page.items.map { item ->
                    if (item is HomeGridItem.FolderItem && item.id == folderId) {
                        item.copy(name = clean.take(24))
                    } else {
                        item
                    }
                }
            )
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
    }

    fun removeAppFromFolder(folderId: String, shortcutId: String) {
        val updatedPages = _uiState.value.pages.map { page ->
            page.copy(
                items = page.items.mapNotNull { item ->
                    if (item is HomeGridItem.FolderItem && item.id == folderId) {
                        val remaining = item.apps.filterNot { it.id == shortcutId }
                        if (remaining.isEmpty()) null else item.copy(apps = remaining)
                    } else {
                        item
                    }
                }
            )
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        val stillExists = updatedPages.any { p -> p.items.any { it.id == folderId } }
        if (!stillExists) closeFolder()
    }

    fun addHomePage() {
        val nextIndex = _uiState.value.pages.size
        if (nextIndex >= 6) {
            showSnackbar("Maksimal 6 halaman Layar Utama")
            return
        }
        val updated = _uiState.value.pages + HomePageData(pageIndex = nextIndex, items = emptyList())
        persistLayout(updated, _uiState.value.dockApps)
        _uiState.update { it.copy(currentHomePage = nextIndex) }
    }

    fun removeEmptyLastPage() {
        val pages = _uiState.value.pages
        if (pages.size <= 1) return
        val last = pages.last()
        if (last.items.isNotEmpty()) {
            showSnackbar("Kosongkan halaman terakhir terlebih dahulu sebelum menghapusnya")
            return
        }
        val updated = pages.dropLast(1)
        val newCurrent = _uiState.value.currentHomePage.coerceAtMost(updated.lastIndex)
        persistLayout(updated, _uiState.value.dockApps)
        _uiState.update { it.copy(currentHomePage = newCurrent) }
    }

    fun addBuiltInWidget(widgetType: BuiltInWidgetType) {
        val targetPage = _uiState.value.currentHomePage
        val widgetItem = HomeGridItem.BuiltInWidgetItem(widgetType = widgetType)
        val updatedPages = _uiState.value.pages.map { page ->
            if (page.pageIndex == targetPage) {
                page.copy(items = listOf(widgetItem) + page.items)
            } else {
                page
            }
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        setWidgetPickerVisible(false)
        showSnackbar("Widget ${widgetType.title} ditambahkan")
    }

    fun addSystemAppWidget(appWidgetId: Int, label: String) {
        if (appWidgetId == -1) return
        val targetPage = _uiState.value.currentHomePage
        val widgetItem = HomeGridItem.SystemWidgetItem(appWidgetId = appWidgetId, providerLabel = label)
        val updatedPages = _uiState.value.pages.map { page ->
            if (page.pageIndex == targetPage) {
                page.copy(items = listOf(widgetItem) + page.items)
            } else {
                page
            }
        }
        persistLayout(updatedPages, _uiState.value.dockApps)
        setWidgetPickerVisible(false)
        showSnackbar("Widget '$label' dipasang")
    }

    fun updateDrawerSearch(query: String) {
        _uiState.update { it.copy(drawerSearchQuery = query) }
    }

    fun updateDrawerCategory(category: AppCategory) {
        _uiState.update { it.copy(drawerSelectedCategory = category) }
    }

    fun updateDrawerSort(order: DrawerSortOrder) {
        updatePreferences { it.copy(drawerSortOrder = order) }
    }

    fun updateUniversalSearch(query: String) {
        _uiState.update { it.copy(universalSearchQuery = query) }
    }

    fun updatePreferences(transform: (LauncherPreferences) -> LauncherPreferences) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences(transform)
        }
    }

    fun importCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val target = storageHelper.resolveSafeChild(
                        storageHelper.personalizationDir,
                        "custom_wallpaper.jpg"
                    )
                    val temp = storageHelper.resolveSafeChild(
                        storageHelper.personalizationDir,
                        ".tmp_wallpaper_${System.nanoTime()}"
                    )
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                            output.fd.sync()
                        }
                    } ?: return@runCatching null
                    if (target.exists()) target.delete()
                    if (!temp.renameTo(target)) {
                        temp.copyTo(target, overwrite = true)
                        temp.delete()
                    }
                    target.absolutePath
                }.getOrNull()
            }

            if (savedPath != null) {
                updatePreferences {
                    it.copy(
                        selectedWallpaper = BuiltInWallpaper.CUSTOM_IMAGE,
                        customWallpaperPath = savedPath
                    )
                }
                showSnackbar("Wallpaper kustom disimpan di penyimpanan privat H P i O")
            } else {
                showSnackbar("Gagal memuat gambar yang dipilih")
            }
        }
    }

    fun toggleTorch() {
        val result = controlCenterHelper.toggleTorch()
        result.onSuccess { enabled ->
            _uiState.update {
                it.copy(controlCenterState = it.controlCenterState.copy(torchEnabled = enabled))
            }
        }.onFailure { err ->
            showSnackbar(err.message ?: "Senter tidak tersedia")
        }
    }

    fun setBrightnessFraction(fraction: Float, activity: Activity?) {
        val clamped = fraction.coerceIn(0.05f, 1.0f)
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = clamped
            win.attributes = lp
        }
        controlCenterHelper.applyBrightnessFraction(clamped)
        _uiState.update {
            it.copy(
                controlCenterState = it.controlCenterState.copy(
                    brightnessFraction = clamped,
                    canWriteSystemSettings = permissionHelper.canWriteSystemSettings()
                )
            )
        }
    }

    fun setMediaVolume(level: Int) {
        controlCenterHelper.setMediaVolume(level)
        _uiState.update {
            it.copy(controlCenterState = it.controlCenterState.copy(mediaVolume = level))
        }
    }

    fun setRingVolume(level: Int) {
        controlCenterHelper.setRingVolume(level)
        _uiState.update {
            it.copy(controlCenterState = it.controlCenterState.copy(ringVolume = level))
        }
    }

    fun checkForUpdates(forceManual: Boolean) {
        viewModelScope.launch {
            if (forceManual) {
                _uiState.update { it.copy(lastUpdateStatusMessage = "Memeriksa GitHub Releases…") }
            }
            when (val result = updateHelper.checkForUpdates(forceManual)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(
                            availableUpdate = result.info,
                            updateDownloadState = UpdateDownloadState.Idle,
                            lastUpdateStatusMessage = "Tersedia versi ${result.info.versionName}"
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            lastUpdateStatusMessage = "Sudah menggunakan versi terbaru (${result.currentVersion})"
                        )
                    }
                    if (forceManual) {
                        showSnackbar("H P i O sudah versi terbaru (${result.currentVersion})")
                    }
                }
                is UpdateCheckResult.NoCompatibleRelease -> {
                    _uiState.update { it.copy(lastUpdateStatusMessage = result.message) }
                    if (forceManual) showSnackbar(result.message)
                }
                is UpdateCheckResult.OfflineOrSkipped -> {
                    _uiState.update { it.copy(lastUpdateStatusMessage = result.reason) }
                    if (forceManual) showSnackbar(result.reason)
                }
                is UpdateCheckResult.Error -> {
                    _uiState.update { it.copy(lastUpdateStatusMessage = result.message) }
                    if (forceManual) showSnackbar(result.message)
                }
            }
        }
    }

    fun onUpdateSkip(versionName: String) {
        updateHelper.skipVersion(versionName)
        _uiState.update {
            it.copy(
                availableUpdate = null,
                updateDownloadState = UpdateDownloadState.Idle,
                skippedVersion = updateHelper.normalizeVersion(versionName)
            )
        }
        showSnackbar("Versi $versionName akan dilewati sampai ada rilis lebih baru")
    }

    fun onUpdateLater() {
        updateHelper.dismissForCurrentLaunch()
        _uiState.update {
            it.copy(
                availableUpdate = null,
                updateDownloadState = UpdateDownloadState.Idle
            )
        }
    }

    fun onStartUpdateDownload(info: ReleaseUpdateInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(updateDownloadState = UpdateDownloadState.Downloading(0, 0L, info.apkSizeBytes))
            }
            when (
                val outcome = updateHelper.downloadReleaseApk(info) { pct, down, total ->
                    _uiState.update {
                        it.copy(updateDownloadState = UpdateDownloadState.Downloading(pct, down, total))
                    }
                }
            ) {
                is DownloadOutcome.Success -> {
                    _uiState.update {
                        it.copy(storageSummary = storageHelper.getSummary())
                    }
                    if (updateHelper.canRequestPackageInstalls()) {
                        _uiState.update {
                            it.copy(
                                updateDownloadState = UpdateDownloadState.ReadyToInstall(
                                    apkFilePath = outcome.apkFile.absolutePath,
                                    versionName = info.versionName
                                ),
                                pendingInstallApkPath = outcome.apkFile.absolutePath
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                updateDownloadState = UpdateDownloadState.RequiresInstallPermission(
                                    apkFilePath = outcome.apkFile.absolutePath,
                                    versionName = info.versionName
                                ),
                                pendingInstallApkPath = outcome.apkFile.absolutePath
                            )
                        }
                    }
                }
                is DownloadOutcome.Failure -> {
                    _uiState.update {
                        it.copy(updateDownloadState = UpdateDownloadState.Error(outcome.message))
                    }
                }
            }
        }
    }

    fun launchPendingApkInstall(): Boolean {
        val path = _uiState.value.pendingInstallApkPath ?: return false
        val file = File(path)
        if (!file.exists()) {
            showSnackbar("File APK pembaruan tidak ditemukan")
            return false
        }
        if (!updateHelper.canRequestPackageInstalls()) {
            return false
        }
        val intent = updateHelper.createInstallApkIntent(file)
        return runCatching {
            app.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun clearSkippedVersion() {
        updateHelper.clearSkippedVersion()
        _uiState.update { it.copy(skippedVersion = null) }
        showSnackbar("Daftar versi yang dilewati dibersihkan")
    }

    fun clearDownloadedUpdates() {
        val count = storageHelper.clearDownloadedUpdates()
        _uiState.update { it.copy(storageSummary = storageHelper.getSummary()) }
        showSnackbar("$count berkas pembaruan dibersihkan")
    }

    fun clearCrashLogs() {
        val count = storageHelper.clearCrashLogs()
        _uiState.update {
            it.copy(
                crashHistory = emptyList(),
                storageSummary = storageHelper.getSummary()
            )
        }
        showSnackbar("$count log crash dibersihkan")
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun persistLayout(pages: List<HomePageData>, dock: List<HomeGridItem.AppShortcut>) {
        _uiState.update { it.copy(pages = pages, dockApps = dock) }
        viewModelScope.launch {
            preferencesRepository.saveHomeLayout(PersistedLayout(pages, dock))
        }
    }

    override fun onCleared() {
        super.onCleared()
        widgetHelper.stopListening()
    }
}
