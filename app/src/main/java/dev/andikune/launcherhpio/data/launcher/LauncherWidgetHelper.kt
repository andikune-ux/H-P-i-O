package dev.andikune.launcherhpio.data.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/**
 * Manages real Android AppWidgetHost & AppWidgetManager lifecycle for Home Screen widgets
 * with safe exception handling and leak-free host listening.
 */
class LauncherWidgetHelper(private val context: Context) {

    companion object {
        const val APPWIDGET_HOST_ID = 2048
    }

    val appWidgetManager: AppWidgetManager? by lazy {
        runCatching { AppWidgetManager.getInstance(context) }.getOrNull()
    }

    val appWidgetHost: AppWidgetHost? by lazy {
        runCatching { AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID) }.getOrNull()
    }

    fun startListening() {
        runCatching { appWidgetHost?.startListening() }
    }

    fun stopListening() {
        runCatching { appWidgetHost?.stopListening() }
    }

    fun allocateWidgetId(): Int {
        return runCatching { appWidgetHost?.allocateAppWidgetId() ?: -1 }.getOrDefault(-1)
    }

    fun deleteWidgetId(appWidgetId: Int) {
        if (appWidgetId != -1) {
            runCatching { appWidgetHost?.deleteAppWidgetId(appWidgetId) }
        }
    }

    fun getInstalledProviders(): List<AppWidgetProviderInfo> {
        return runCatching { appWidgetManager?.installedProviders.orEmpty() }.getOrDefault(emptyList())
    }

    fun tryBindWidget(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean {
        return runCatching {
            appWidgetManager?.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider) == true
        }.getOrDefault(false)
    }

    fun createBindIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
        }
    }

    fun createHostView(appWidgetId: Int): AppWidgetHostView? {
        return runCatching {
            val info = appWidgetManager?.getAppWidgetInfo(appWidgetId) ?: return null
            appWidgetHost?.createView(context, appWidgetId, info)?.apply {
                setAppWidget(appWidgetId, info)
            }
        }.getOrNull()
    }
}
