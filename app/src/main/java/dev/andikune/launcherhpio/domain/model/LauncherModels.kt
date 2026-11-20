package dev.andikune.launcherhpio.domain.model

import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap")
}

enum class AppCategory(val label: String) {
    ALL("Semua"),
    COMMUNICATION("Komunikasi"),
    MEDIA("Media"),
    PRODUCTIVITY("Produktivitas"),
    TOOLS("Utilitas"),
    GAMES("Gim"),
    SYSTEM("Sistem"),
    OTHER("Lainnya")
}

enum class DrawerSortOrder(val label: String) {
    NAME_ASC("Nama (A–Z)"),
    NAME_DESC("Nama (Z–A)"),
    RECENT_INSTALL("Baru Diinstal"),
    RECENT_UPDATE("Baru Diperbarui")
}

enum class DrawerAnimationStyle(val label: String) {
    SPRING_GLASS("Spring Glass"),
    SLIDE_UP("Slide Up"),
    FADE_SCALE("Fade Scale")
}

enum class IconSizePreset(val label: String, val iconDp: Int, val cellPaddingDp: Int) {
    COMPACT("Ringkas", 46, 6),
    STANDARD("Standar", 54, 8),
    LARGE("Besar", 62, 10)
}

enum class GridPreset(val label: String, val columns: Int, val rows: Int) {
    FOUR_BY_FIVE("4 × 5", 4, 5),
    FOUR_BY_SIX("4 × 6", 4, 6),
    FIVE_BY_SIX("5 × 6", 5, 6)
}

enum class BuiltInWallpaper(val id: String, val title: String, val subtitle: String, val isLightBackdrop: Boolean) {
    AURORA_GLASS("aurora_glass", "Aurora Glass", "Refleksi biru kobalt & cyan obsidian", false),
    OBSIDIAN_WAVE("obsidian_wave", "Obsidian Wave", "Gelombang gelap zamrud & perak", false),
    PRISM_SILK("prism_silk", "Prism Silk", "Sutra kaca mutiara cerah hangat", true),
    CUSTOM_IMAGE("custom_image", "Gambar Pilihan", "Wallpaper pribadi dari perangkat", false)
}

enum class BuiltInWidgetType(val title: String, val description: String) {
    CLOCK_STATUS("Jam & Status Perangkat", "Jam digital Hybrid OS, tanggal, baterai & jaringan"),
    SYSTEM_TELEMETRY("Monitor Performa", "Pemakaian RAM, penyimpanan & status sistem"),
    QUICK_NOTES("Catatan Cepat", "Catatan kaca instan di Layar Utama")
}

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: AppCategory,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val icon: ImageBitmap? = null
) {
    val componentKey: String
        get() = "$packageName/$activityName"
}

sealed interface HomeGridItem {
    val id: String

    data class AppShortcut(
        override val id: String = UUID.randomUUID().toString(),
        val packageName: String,
        val activityName: String,
        val fallbackLabel: String
    ) : HomeGridItem

    data class FolderItem(
        override val id: String = UUID.randomUUID().toString(),
        val name: String,
        val apps: List<AppShortcut>
    ) : HomeGridItem

    data class BuiltInWidgetItem(
        override val id: String = UUID.randomUUID().toString(),
        val widgetType: BuiltInWidgetType
    ) : HomeGridItem

    data class SystemWidgetItem(
        override val id: String = UUID.randomUUID().toString(),
        val appWidgetId: Int,
        val providerLabel: String
    ) : HomeGridItem
}

data class HomePageData(
    val pageIndex: Int,
    val items: List<HomeGridItem>
)

data class LauncherPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedWallpaper: BuiltInWallpaper = BuiltInWallpaper.AURORA_GLASS,
    val customWallpaperPath: String? = null,
    val glassTransparency: Float = 0.34f,
    val blurIntensityDp: Float = 14f,
    val homeGridPreset: GridPreset = GridPreset.FOUR_BY_FIVE,
    val drawerColumns: Int = 4,
    val iconSizePreset: IconSizePreset = IconSizePreset.STANDARD,
    val showDock: Boolean = true,
    val showAppDrawerButton: Boolean = true,
    val swipeUpForDrawer: Boolean = true,
    val swipeDownForControlCenter: Boolean = true,
    val showBuiltInClockHero: Boolean = true,
    val drawerSortOrder: DrawerSortOrder = DrawerSortOrder.NAME_ASC,
    val drawerAnimationStyle: DrawerAnimationStyle = DrawerAnimationStyle.SPRING_GLASS,
    val quickNoteText: String = "Ketuk untuk menulis catatan cepat di Layar Utama H P i O.",
    val safeModeEnabled: Boolean = false
)

data class ControlCenterState(
    val wifiEnabled: Boolean = false,
    val wifiLabel: String = "Terputus",
    val bluetoothEnabled: Boolean = false,
    val bluetoothPermissionMissing: Boolean = false,
    val torchEnabled: Boolean = false,
    val torchAvailable: Boolean = true,
    val brightnessFraction: Float = 0.65f,
    val canWriteSystemSettings: Boolean = false,
    val mediaVolume: Int = 5,
    val mediaMaxVolume: Int = 15,
    val ringVolume: Int = 5,
    val ringMaxVolume: Int = 15,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isDefaultLauncher: Boolean = false
)

enum class CapabilityAvailability {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    REVOKED,
    UNAVAILABLE
}

data class CapabilityStatusItem(
    val id: String,
    val title: String,
    val purpose: String,
    val availability: CapabilityAvailability,
    val isOptional: Boolean,
    val actionLabel: String
)

data class CrashReportRecord(
    val id: String,
    val timestampIso: String,
    val epochMillis: Long,
    val appVersion: String,
    val versionCode: Int,
    val androidVersion: String,
    val deviceModel: String,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val filePath: String
)

data class ReleaseUpdateInfo(
    val tagName: String,
    val versionName: String,
    val publishedAt: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val expectedSha256: String?
)

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val apkFilePath: String, val versionName: String) : UpdateDownloadState
    data class RequiresInstallPermission(val apkFilePath: String, val versionName: String) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

data class StorageSummary(
    val rootFolderPath: String,
    val updatesFolderPath: String,
    val crashFolderPath: String,
    val updatesFileCount: Int,
    val updatesBytes: Long,
    val crashFileCount: Int,
    val crashBytes: Long,
    val freeSpaceBytes: Long
)
