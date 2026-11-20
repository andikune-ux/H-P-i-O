package dev.andikune.launcherhpio.data.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.andikune.launcherhpio.domain.model.AppCategory
import dev.andikune.launcherhpio.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Queries real launchable Android applications via PackageManager using least-privilege
 * `<queries>` intent resolution and caches decoded icon bitmaps cleanly.
 */
class InstalledAppsRepository(private val context: Context) {

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        resolveInfos
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                val activityName = activityInfo.name
                val label = runCatching { info.loadLabel(pm).toString().trim() }
                    .getOrDefault(packageName)
                    .ifEmpty { packageName }

                val appInfo = activityInfo.applicationInfo
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val pkgInfo = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(packageName, 0)
                    }
                }.getOrNull()

                val iconBitmap = runCatching {
                    drawableToImageBitmap(info.loadIcon(pm))
                }.getOrNull()

                InstalledApp(
                    packageName = packageName,
                    activityName = activityName,
                    label = label,
                    category = mapCategory(appInfo, isSystem),
                    isSystemApp = isSystem,
                    firstInstallTime = pkgInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = pkgInfo?.lastUpdateTime ?: 0L,
                    icon = iconBitmap
                )
            }
            .distinctBy { it.componentKey }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun mapCategory(appInfo: ApplicationInfo, isSystem: Boolean): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> AppCategory.MEDIA
                ApplicationInfo.CATEGORY_SOCIAL,
                ApplicationInfo.CATEGORY_NEWS -> AppCategory.COMMUNICATION
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_MAPS,
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.TOOLS
                else -> if (isSystem) AppCategory.SYSTEM else AppCategory.OTHER
            }
        }
        return if (isSystem) AppCategory.SYSTEM else AppCategory.OTHER
    }

    private fun drawableToImageBitmap(drawable: Drawable, targetSizePx: Int = 144): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            if (bmp.width <= targetSizePx && bmp.height <= targetSizePx) {
                return bmp.asImageBitmap()
            }
            return Bitmap.createScaledBitmap(bmp, targetSizePx, targetSizePx, true).asImageBitmap()
        }
        val width = drawable.intrinsicWidth.takeIf { it in 1..targetSizePx } ?: targetSizePx
        val height = drawable.intrinsicHeight.takeIf { it in 1..targetSizePx } ?: targetSizePx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
