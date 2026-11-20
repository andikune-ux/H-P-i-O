package dev.andikune.launcherhpio.data.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.andikune.launcherhpio.data.storage.AppStorageHelper
import dev.andikune.launcherhpio.domain.model.BuiltInWallpaper
import dev.andikune.launcherhpio.domain.model.BuiltInWidgetType
import dev.andikune.launcherhpio.domain.model.DrawerAnimationStyle
import dev.andikune.launcherhpio.domain.model.DrawerSortOrder
import dev.andikune.launcherhpio.domain.model.GridPreset
import dev.andikune.launcherhpio.domain.model.HomeGridItem
import dev.andikune.launcherhpio.domain.model.HomePageData
import dev.andikune.launcherhpio.domain.model.IconSizePreset
import dev.andikune.launcherhpio.domain.model.InstalledApp
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "hpio_launcher_prefs")

data class PersistedLayout(
    val pages: List<HomePageData>,
    val dockApps: List<HomeGridItem.AppShortcut>
)

/**
 * Persists personalization settings in Jetpack DataStore and Home Screen pages/folders/widgets/dock
 * using atomic JSON writes inside H P i O's private personalization storage folder.
 */
class LauncherPreferencesRepository(
    private val context: Context,
    private val storageHelper: AppStorageHelper
) {
    companion object {
        private const val LAYOUT_FILE = "home_layout_v1.json"

        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_WALLPAPER = stringPreferencesKey("selected_wallpaper")
        private val KEY_CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
        private val KEY_GLASS_ALPHA = floatPreferencesKey("glass_transparency")
        private val KEY_BLUR_DP = floatPreferencesKey("blur_intensity_dp")
        private val KEY_HOME_GRID = stringPreferencesKey("home_grid_preset")
        private val KEY_DRAWER_COLUMNS = intPreferencesKey("drawer_columns")
        private val KEY_ICON_SIZE = stringPreferencesKey("icon_size_preset")
        private val KEY_SHOW_DOCK = booleanPreferencesKey("show_dock")
        private val KEY_SHOW_DRAWER_BTN = booleanPreferencesKey("show_app_drawer_button")
        private val KEY_SWIPE_UP_DRAWER = booleanPreferencesKey("swipe_up_for_drawer")
        private val KEY_SWIPE_DOWN_CC = booleanPreferencesKey("swipe_down_for_control_center")
        private val KEY_SHOW_CLOCK_HERO = booleanPreferencesKey("show_built_in_clock_hero")
        private val KEY_DRAWER_SORT = stringPreferencesKey("drawer_sort_order")
        private val KEY_DRAWER_ANIM = stringPreferencesKey("drawer_animation_style")
        private val KEY_QUICK_NOTE = stringPreferencesKey("quick_note_text")
        private val KEY_SAFE_MODE = booleanPreferencesKey("safe_mode_enabled")
    }

    val preferencesFlow: Flow<LauncherPreferences> = context.launcherDataStore.data.map { prefs ->
        LauncherPreferences(
            themeMode = enumOrDefault(prefs[KEY_THEME_MODE], ThemeMode.SYSTEM),
            selectedWallpaper = enumOrDefault(prefs[KEY_WALLPAPER], BuiltInWallpaper.AURORA_GLASS),
            customWallpaperPath = prefs[KEY_CUSTOM_WALLPAPER_PATH],
            glassTransparency = (prefs[KEY_GLASS_ALPHA] ?: 0.34f).coerceIn(0.15f, 0.85f),
            blurIntensityDp = (prefs[KEY_BLUR_DP] ?: 14f).coerceIn(0f, 24f),
            homeGridPreset = enumOrDefault(prefs[KEY_HOME_GRID], GridPreset.FOUR_BY_FIVE),
            drawerColumns = (prefs[KEY_DRAWER_COLUMNS] ?: 4).coerceIn(4, 5),
            iconSizePreset = enumOrDefault(prefs[KEY_ICON_SIZE], IconSizePreset.STANDARD),
            showDock = prefs[KEY_SHOW_DOCK] ?: true,
            showAppDrawerButton = prefs[KEY_SHOW_DRAWER_BTN] ?: true,
            swipeUpForDrawer = prefs[KEY_SWIPE_UP_DRAWER] ?: true,
            swipeDownForControlCenter = prefs[KEY_SWIPE_DOWN_CC] ?: true,
            showBuiltInClockHero = prefs[KEY_SHOW_CLOCK_HERO] ?: true,
            drawerSortOrder = enumOrDefault(prefs[KEY_DRAWER_SORT], DrawerSortOrder.NAME_ASC),
            drawerAnimationStyle = enumOrDefault(prefs[KEY_DRAWER_ANIM], DrawerAnimationStyle.SPRING_GLASS),
            quickNoteText = prefs[KEY_QUICK_NOTE]
                ?: "Ketuk untuk menulis catatan cepat di Layar Utama H P i O.",
            safeModeEnabled = prefs[KEY_SAFE_MODE] ?: false
        )
    }

    suspend fun updatePreferences(transform: (LauncherPreferences) -> LauncherPreferences) {
        context.launcherDataStore.edit { prefs ->
            val current = LauncherPreferences(
                themeMode = enumOrDefault(prefs[KEY_THEME_MODE], ThemeMode.SYSTEM),
                selectedWallpaper = enumOrDefault(prefs[KEY_WALLPAPER], BuiltInWallpaper.AURORA_GLASS),
                customWallpaperPath = prefs[KEY_CUSTOM_WALLPAPER_PATH],
                glassTransparency = prefs[KEY_GLASS_ALPHA] ?: 0.34f,
                blurIntensityDp = prefs[KEY_BLUR_DP] ?: 14f,
                homeGridPreset = enumOrDefault(prefs[KEY_HOME_GRID], GridPreset.FOUR_BY_FIVE),
                drawerColumns = prefs[KEY_DRAWER_COLUMNS] ?: 4,
                iconSizePreset = enumOrDefault(prefs[KEY_ICON_SIZE], IconSizePreset.STANDARD),
                showDock = prefs[KEY_SHOW_DOCK] ?: true,
                showAppDrawerButton = prefs[KEY_SHOW_DRAWER_BTN] ?: true,
                swipeUpForDrawer = prefs[KEY_SWIPE_UP_DRAWER] ?: true,
                swipeDownForControlCenter = prefs[KEY_SWIPE_DOWN_CC] ?: true,
                showBuiltInClockHero = prefs[KEY_SHOW_CLOCK_HERO] ?: true,
                drawerSortOrder = enumOrDefault(prefs[KEY_DRAWER_SORT], DrawerSortOrder.NAME_ASC),
                drawerAnimationStyle = enumOrDefault(prefs[KEY_DRAWER_ANIM], DrawerAnimationStyle.SPRING_GLASS),
                quickNoteText = prefs[KEY_QUICK_NOTE]
                    ?: "Ketuk untuk menulis catatan cepat di Layar Utama H P i O.",
                safeModeEnabled = prefs[KEY_SAFE_MODE] ?: false
            )
            val updated = transform(current)
            prefs[KEY_THEME_MODE] = updated.themeMode.name
            prefs[KEY_WALLPAPER] = updated.selectedWallpaper.name
            if (updated.customWallpaperPath != null) {
                prefs[KEY_CUSTOM_WALLPAPER_PATH] = updated.customWallpaperPath
            } else {
                prefs.remove(KEY_CUSTOM_WALLPAPER_PATH)
            }
            prefs[KEY_GLASS_ALPHA] = updated.glassTransparency.coerceIn(0.15f, 0.85f)
            prefs[KEY_BLUR_DP] = updated.blurIntensityDp.coerceIn(0f, 24f)
            prefs[KEY_HOME_GRID] = updated.homeGridPreset.name
            prefs[KEY_DRAWER_COLUMNS] = updated.drawerColumns.coerceIn(4, 5)
            prefs[KEY_ICON_SIZE] = updated.iconSizePreset.name
            prefs[KEY_SHOW_DOCK] = updated.showDock
            prefs[KEY_SHOW_DRAWER_BTN] = updated.showAppDrawerButton
            prefs[KEY_SWIPE_UP_DRAWER] = updated.swipeUpForDrawer
            prefs[KEY_SWIPE_DOWN_CC] = updated.swipeDownForControlCenter
            prefs[KEY_SHOW_CLOCK_HERO] = updated.showBuiltInClockHero
            prefs[KEY_DRAWER_SORT] = updated.drawerSortOrder.name
            prefs[KEY_DRAWER_ANIM] = updated.drawerAnimationStyle.name
            prefs[KEY_QUICK_NOTE] = updated.quickNoteText.take(240)
            prefs[KEY_SAFE_MODE] = updated.safeModeEnabled
        }
    }

    suspend fun loadOrSeedHomeLayout(installedApps: List<InstalledApp>): PersistedLayout =
        withContext(Dispatchers.IO) {
            val file = storageHelper.resolveSafeChild(storageHelper.personalizationDir, LAYOUT_FILE)
            if (file.exists()) {
                val parsed = runCatching { parseLayoutJson(file.readText()) }.getOrNull()
                if (parsed != null && parsed.pages.isNotEmpty()) {
                    return@withContext parsed
                }
            }
            val seeded = seedDefaultLayout(installedApps)
            saveHomeLayout(seeded)
            seeded
        }

    suspend fun saveHomeLayout(layout: PersistedLayout) = withContext(Dispatchers.IO) {
        val root = JSONObject().apply {
            put("version", 1)
            put("pages", JSONArray().apply {
                layout.pages.forEach { page ->
                    put(JSONObject().apply {
                        put("pageIndex", page.pageIndex)
                        put("items", JSONArray().apply {
                            page.items.forEach { item -> put(serializeHomeItem(item)) }
                        })
                    })
                }
            })
            put("dockApps", JSONArray().apply {
                layout.dockApps.forEach { shortcut -> put(serializeHomeItem(shortcut)) }
            })
        }
        val file = storageHelper.resolveSafeChild(storageHelper.personalizationDir, LAYOUT_FILE)
        storageHelper.writeTextAtomically(file, root.toString(2))
    }

    private fun seedDefaultLayout(installedApps: List<InstalledApp>): PersistedLayout {
        val dockApps = installedApps.take(4).map {
            HomeGridItem.AppShortcut(
                packageName = it.packageName,
                activityName = it.activityName,
                fallbackLabel = it.label
            )
        }
        val remaining = installedApps.drop(4)
        val page1Apps = remaining.take(8).map {
            HomeGridItem.AppShortcut(
                packageName = it.packageName,
                activityName = it.activityName,
                fallbackLabel = it.label
            )
        }
        val folderSeed = remaining.drop(8).take(4).map {
            HomeGridItem.AppShortcut(
                packageName = it.packageName,
                activityName = it.activityName,
                fallbackLabel = it.label
            )
        }

        val page1Items = mutableListOf<HomeGridItem>(
            HomeGridItem.BuiltInWidgetItem(widgetType = BuiltInWidgetType.SYSTEM_TELEMETRY)
        )
        page1Items.addAll(page1Apps)
        if (folderSeed.isNotEmpty()) {
            page1Items.add(HomeGridItem.FolderItem(name = "Koleksi Inti", apps = folderSeed))
        }

        val page2Items = mutableListOf<HomeGridItem>(
            HomeGridItem.BuiltInWidgetItem(widgetType = BuiltInWidgetType.QUICK_NOTES)
        )
        page2Items.addAll(
            remaining.drop(12).take(8).map {
                HomeGridItem.AppShortcut(
                    packageName = it.packageName,
                    activityName = it.activityName,
                    fallbackLabel = it.label
                )
            }
        )

        return PersistedLayout(
            pages = listOf(
                HomePageData(pageIndex = 0, items = page1Items),
                HomePageData(pageIndex = 1, items = page2Items)
            ),
            dockApps = dockApps
        )
    }

    private fun serializeHomeItem(item: HomeGridItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            when (item) {
                is HomeGridItem.AppShortcut -> {
                    put("type", "app")
                    put("packageName", item.packageName)
                    put("activityName", item.activityName)
                    put("fallbackLabel", item.fallbackLabel)
                }
                is HomeGridItem.FolderItem -> {
                    put("type", "folder")
                    put("name", item.name)
                    put("apps", JSONArray().apply {
                        item.apps.forEach { put(serializeHomeItem(it)) }
                    })
                }
                is HomeGridItem.BuiltInWidgetItem -> {
                    put("type", "builtin_widget")
                    put("widgetType", item.widgetType.name)
                }
                is HomeGridItem.SystemWidgetItem -> {
                    put("type", "system_widget")
                    put("appWidgetId", item.appWidgetId)
                    put("providerLabel", item.providerLabel)
                }
            }
        }
    }

    private fun parseLayoutJson(raw: String): PersistedLayout {
        val root = JSONObject(raw)
        val pagesArray = root.optJSONArray("pages") ?: JSONArray()
        val pages = mutableListOf<HomePageData>()
        for (i in 0 until pagesArray.length()) {
            val pageObj = pagesArray.optJSONObject(i) ?: continue
            val itemsArray = pageObj.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<HomeGridItem>()
            for (j in 0 until itemsArray.length()) {
                val itemObj = itemsArray.optJSONObject(j) ?: continue
                parseHomeItem(itemObj)?.let { items.add(it) }
            }
            pages.add(HomePageData(pageIndex = i, items = items))
        }
        val dockArray = root.optJSONArray("dockApps") ?: JSONArray()
        val dockApps = mutableListOf<HomeGridItem.AppShortcut>()
        for (i in 0 until dockArray.length()) {
            val itemObj = dockArray.optJSONObject(i) ?: continue
            val parsed = parseHomeItem(itemObj)
            if (parsed is HomeGridItem.AppShortcut) {
                dockApps.add(parsed)
            }
        }
        return PersistedLayout(pages = pages, dockApps = dockApps)
    }

    private fun parseHomeItem(obj: JSONObject): HomeGridItem? {
        val id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }
        return when (obj.optString("type")) {
            "app" -> HomeGridItem.AppShortcut(
                id = id,
                packageName = obj.optString("packageName"),
                activityName = obj.optString("activityName"),
                fallbackLabel = obj.optString("fallbackLabel")
            )
            "folder" -> {
                val appsArr = obj.optJSONArray("apps") ?: JSONArray()
                val apps = mutableListOf<HomeGridItem.AppShortcut>()
                for (i in 0 until appsArr.length()) {
                    val child = appsArr.optJSONObject(i)?.let { parseHomeItem(it) }
                    if (child is HomeGridItem.AppShortcut) apps.add(child)
                }
                HomeGridItem.FolderItem(
                    id = id,
                    name = obj.optString("name", "Folder"),
                    apps = apps
                )
            }
            "builtin_widget" -> {
                val widgetType = enumOrDefault(obj.optString("widgetType"), BuiltInWidgetType.SYSTEM_TELEMETRY)
                HomeGridItem.BuiltInWidgetItem(id = id, widgetType = widgetType)
            }
            "system_widget" -> HomeGridItem.SystemWidgetItem(
                id = id,
                appWidgetId = obj.optInt("appWidgetId", -1),
                providerLabel = obj.optString("providerLabel", "Widget Sistem")
            )
            else -> null
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
