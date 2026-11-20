package dev.andikune.launcherhpio

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andikune.launcherhpio.data.launcher.PackageChangeReceiver
import dev.andikune.launcherhpio.domain.model.CrashReportRecord
import dev.andikune.launcherhpio.domain.model.DrawerAnimationStyle
import dev.andikune.launcherhpio.domain.model.HomeGridItem
import dev.andikune.launcherhpio.domain.model.UpdateDownloadState
import dev.andikune.launcherhpio.ui.ActiveOverlay
import dev.andikune.launcherhpio.ui.LauncherViewModel
import dev.andikune.launcherhpio.ui.components.WallpaperBackdrop
import dev.andikune.launcherhpio.ui.controlcenter.ControlCenterOverlay
import dev.andikune.launcherhpio.ui.dialogs.CrashRecoveryDialog
import dev.andikune.launcherhpio.ui.dialogs.UpdateAvailableDialog
import dev.andikune.launcherhpio.ui.drawer.AppDrawerOverlay
import dev.andikune.launcherhpio.ui.home.AppActionSheet
import dev.andikune.launcherhpio.ui.home.FolderOverlay
import dev.andikune.launcherhpio.ui.home.HomeScreen
import dev.andikune.launcherhpio.ui.home.WidgetPickerSheet
import dev.andikune.launcherhpio.ui.search.UniversalSearchOverlay
import dev.andikune.launcherhpio.ui.settings.PersonalizationScreen
import dev.andikune.launcherhpio.ui.settings.SystemHubScreen
import dev.andikune.launcherhpio.ui.theme.HpioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var packageReceiver: PackageChangeReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        packageReceiver = PackageChangeReceiver {
            viewModel.refreshInstalledApps()
        }.also { it.register(this) }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HpioTheme(themeMode = uiState.preferences.themeMode) {
                HpioLauncherRoot(viewModel = viewModel, activity = this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.widgetHelper.startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemDiagnostics(this)
    }

    override fun onStop() {
        viewModel.widgetHelper.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        packageReceiver?.unregister(this)
        packageReceiver = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.navigateHome()
        }
    }
}

@Composable
private fun HpioLauncherRoot(
    viewModel: LauncherViewModel,
    activity: Activity
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inspectedCrashReport by remember { mutableStateOf<CrashReportRecord?>(null) }
    var pendingBindWidgetLabel by remember { mutableStateOf("Widget Sistem") }

    val installedAppsByComponent = remember(uiState.installedApps) {
        uiState.installedApps.associateBy { it.componentKey }
    }

    val allFolders = remember(uiState.pages) {
        uiState.pages.flatMap { it.items }.filterIsInstance<HomeGridItem.FolderItem>()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomWallpaperFromUri(uri)
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemDiagnostics(activity)
        if (!granted) {
            viewModel.showSnackbar("Izin Bluetooth ditolak. Pengaturan Bluetooth tetap dapat dibuka manual.")
        }
    }

    val roleOrSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemDiagnostics(activity)
    }

    val widgetBindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == Activity.RESULT_OK && widgetId != -1) {
            viewModel.addSystemAppWidget(widgetId, pendingBindWidgetLabel)
        } else if (widgetId != -1) {
            viewModel.widgetHelper.deleteWidgetId(widgetId)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    LaunchedEffect(uiState.updateDownloadState) {
        if (uiState.updateDownloadState is UpdateDownloadState.ReadyToInstall) {
            viewModel.launchPendingApkInstall()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val extraBlur = if (uiState.activeOverlay != ActiveOverlay.NONE || uiState.openFolderId != null) {
            10.dp
        } else {
            0.dp
        }

        WallpaperBackdrop(
            preferences = uiState.preferences,
            extraBlurDp = extraBlur
        )

        HomeScreen(
            uiState = uiState,
            installedAppsByComponent = installedAppsByComponent,
            widgetHelper = viewModel.widgetHelper,
            onPageChanged = viewModel::setCurrentHomePage,
            onOpenDrawer = { viewModel.openOverlay(ActiveOverlay.APP_DRAWER) },
            onOpenControlCenter = { viewModel.openOverlay(ActiveOverlay.CONTROL_CENTER) },
            onOpenSearch = { viewModel.openOverlay(ActiveOverlay.SEARCH) },
            onOpenPersonalization = { viewModel.openOverlay(ActiveOverlay.PERSONALIZATION) },
            onOpenWidgetPicker = { viewModel.setWidgetPickerVisible(true) },
            onAddPage = viewModel::addHomePage,
            onRemoveEmptyLastPage = viewModel::removeEmptyLastPage,
            onLaunchApp = viewModel::launchApp,
            onOpenAppActions = viewModel::openAppActions,
            onRemoveHomeItem = viewModel::removeItemFromHomePage,
            onRemoveDockApp = viewModel::removeAppFromDock,
            onOpenFolder = viewModel::openFolder,
            onUpdateQuickNote = { note ->
                viewModel.updatePreferences { it.copy(quickNoteText = note) }
            }
        )

        // App Drawer Overlay with configurable animation
        val drawerEnter = when (uiState.preferences.drawerAnimationStyle) {
            DrawerAnimationStyle.SPRING_GLASS ->
                slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
                    initialOffsetY = { it / 3 }
                ) + fadeIn(tween(180))
            DrawerAnimationStyle.SLIDE_UP ->
                slideInVertically(tween(240), initialOffsetY = { it }) + fadeIn(tween(180))
            DrawerAnimationStyle.FADE_SCALE ->
                scaleIn(tween(200), initialScale = 0.94f) + fadeIn(tween(200))
        }
        val drawerExit = when (uiState.preferences.drawerAnimationStyle) {
            DrawerAnimationStyle.SPRING_GLASS, DrawerAnimationStyle.SLIDE_UP ->
                slideOutVertically(tween(200), targetOffsetY = { it / 2 }) + fadeOut(tween(160))
            DrawerAnimationStyle.FADE_SCALE ->
                scaleOut(tween(180), targetScale = 0.95f) + fadeOut(tween(160))
        }

        AnimatedVisibility(
            visible = uiState.activeOverlay == ActiveOverlay.APP_DRAWER,
            enter = drawerEnter,
            exit = drawerExit
        ) {
            AppDrawerOverlay(
                installedApps = uiState.installedApps,
                searchQuery = uiState.drawerSearchQuery,
                selectedCategory = uiState.drawerSelectedCategory,
                preferences = uiState.preferences,
                onSearchChange = viewModel::updateDrawerSearch,
                onCategoryChange = viewModel::updateDrawerCategory,
                onSortOrderChange = viewModel::updateDrawerSort,
                onRefreshApps = viewModel::refreshInstalledApps,
                onOpenPersonalization = { viewModel.openOverlay(ActiveOverlay.PERSONALIZATION) },
                onLaunchApp = { app -> viewModel.launchApp(app.packageName, app.activityName, app.label) },
                onLongPressApp = viewModel::openAppActions,
                onClose = viewModel::closeOverlay
            )
        }

        AnimatedVisibility(
            visible = uiState.activeOverlay == ActiveOverlay.CONTROL_CENTER,
            enter = slideInVertically(tween(220), initialOffsetY = { -it / 2 }) + fadeIn(tween(180)),
            exit = slideOutVertically(tween(180), targetOffsetY = { -it / 2 }) + fadeOut(tween(150))
        ) {
            ControlCenterOverlay(
                state = uiState.controlCenterState,
                preferences = uiState.preferences,
                onOpenWifiPanel = { viewModel.controlCenterHelper.openWifiPanelOrSettings() },
                onOpenBluetoothOrRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !viewModel.permissionHelper.hasBluetoothConnectPermission()
                    ) {
                        viewModel.markBluetoothPermissionRequested()
                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        viewModel.controlCenterHelper.openBluetoothSettings()
                    }
                },
                onToggleTorch = viewModel::toggleTorch,
                onBrightnessChange = { frac -> viewModel.setBrightnessFraction(frac, activity) },
                onRequestWriteSettings = {
                    runCatching { context.startActivity(viewModel.permissionHelper.createWriteSettingsIntent()) }
                },
                onMediaVolumeChange = viewModel::setMediaVolume,
                onRingVolumeChange = viewModel::setRingVolume,
                onOpenDefaultLauncherSetup = {
                    runCatching { roleOrSettingsLauncher.launch(viewModel.permissionHelper.createDefaultHomeIntent()) }
                },
                onOpenSystemSettings = { viewModel.controlCenterHelper.openSystemSettings() },
                onOpenPersonalization = { viewModel.openOverlay(ActiveOverlay.PERSONALIZATION) },
                onClose = viewModel::closeOverlay
            )
        }

        AnimatedVisibility(
            visible = uiState.activeOverlay == ActiveOverlay.SEARCH,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f),
            exit = fadeOut(tween(140))
        ) {
            UniversalSearchOverlay(
                query = uiState.universalSearchQuery,
                installedApps = uiState.installedApps,
                folders = allFolders,
                preferences = uiState.preferences,
                onQueryChange = viewModel::updateUniversalSearch,
                onLaunchApp = { app -> viewModel.launchApp(app.packageName, app.activityName, app.label) },
                onOpenFolder = viewModel::openFolder,
                onOpenControlCenter = { viewModel.openOverlay(ActiveOverlay.CONTROL_CENTER) },
                onOpenPersonalization = { viewModel.openOverlay(ActiveOverlay.PERSONALIZATION) },
                onOpenSystemHub = {
                    viewModel.refreshSystemDiagnostics(activity)
                    viewModel.openOverlay(ActiveOverlay.SYSTEM_HUB)
                },
                onWebSearch = { q -> viewModel.appActionsHelper.openWebSearch(q) },
                onClose = viewModel::closeOverlay
            )
        }

        AnimatedVisibility(
            visible = uiState.activeOverlay == ActiveOverlay.PERSONALIZATION,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            PersonalizationScreen(
                preferences = uiState.preferences,
                onUpdatePreferences = viewModel::updatePreferences,
                onPickCustomWallpaper = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenSystemWallpaperPicker = { viewModel.appActionsHelper.openSystemWallpaperPicker() },
                onOpenSystemHub = {
                    viewModel.refreshSystemDiagnostics(activity)
                    viewModel.openOverlay(ActiveOverlay.SYSTEM_HUB)
                },
                onClose = viewModel::closeOverlay
            )
        }

        AnimatedVisibility(
            visible = uiState.activeOverlay == ActiveOverlay.SYSTEM_HUB,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            SystemHubScreen(
                preferences = uiState.preferences,
                capabilities = uiState.capabilityStatuses,
                storageSummary = uiState.storageSummary,
                crashHistory = uiState.crashHistory,
                lastUpdateStatus = uiState.lastUpdateStatusMessage,
                skippedVersion = uiState.skippedVersion,
                onCapabilityAction = { item ->
                    when (item.id) {
                        "default_home" -> runCatching {
                            roleOrSettingsLauncher.launch(viewModel.permissionHelper.createDefaultHomeIntent())
                        }
                        "bluetooth_connect" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                viewModel.markBluetoothPermissionRequested()
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        }
                        "write_settings" -> runCatching {
                            context.startActivity(viewModel.permissionHelper.createWriteSettingsIntent())
                        }
                        "install_packages" -> runCatching {
                            context.startActivity(viewModel.updateHelper.createManageUnknownAppSourcesIntent())
                        }
                    }
                },
                onCheckUpdatesNow = { viewModel.checkForUpdates(forceManual = true) },
                onClearSkippedVersion = viewModel::clearSkippedVersion,
                onClearUpdatesCache = viewModel::clearDownloadedUpdates,
                onClearCrashLogs = viewModel::clearCrashLogs,
                onViewCrashReport = { report -> inspectedCrashReport = report },
                onClose = { viewModel.openOverlay(ActiveOverlay.PERSONALIZATION) }
            )
        }

        // Open Folder Modal
        val activeFolder = uiState.openFolderId?.let { fid -> allFolders.firstOrNull { it.id == fid } }
        if (activeFolder != null) {
            FolderOverlay(
                folder = activeFolder,
                installedAppsByComponent = installedAppsByComponent,
                preferences = uiState.preferences,
                onDismiss = viewModel::closeFolder,
                onLaunchApp = { shortcut ->
                    viewModel.launchApp(shortcut.packageName, shortcut.activityName, shortcut.fallbackLabel)
                },
                onRenameFolder = { newName -> viewModel.renameFolder(activeFolder.id, newName) },
                onRemoveAppFromFolder = { shortcutId ->
                    viewModel.removeAppFromFolder(activeFolder.id, shortcutId)
                }
            )
        }

        // Long-press App Context Action Sheet
        val selectedApp = uiState.selectedAppForActions
        if (selectedApp != null) {
            AppActionSheet(
                appEntry = selectedApp,
                existingFolders = allFolders,
                onDismiss = viewModel::closeAppActions,
                onLaunch = {
                    viewModel.launchApp(selectedApp.packageName, selectedApp.activityName, selectedApp.label)
                },
                onPinToHome = { viewModel.pinAppToCurrentHomePage(selectedApp) },
                onPinToDock = { viewModel.pinAppToDock(selectedApp) },
                onCreateFolder = { viewModel.createFolderWithApp(selectedApp) },
                onAddToFolder = { folderId -> viewModel.addAppToFolder(folderId, selectedApp) },
                onOpenAppInfo = {
                    viewModel.closeAppActions()
                    viewModel.appActionsHelper.openAppInfo(selectedApp.packageName)
                },
                onRequestUninstall = {
                    viewModel.closeAppActions()
                    viewModel.appActionsHelper.requestUninstall(selectedApp.packageName)
                }
            )
        }

        // Widget Picker Bottom Sheet
        if (uiState.showWidgetPickerSheet) {
            WidgetPickerSheet(
                widgetHelper = viewModel.widgetHelper,
                onDismiss = { viewModel.setWidgetPickerVisible(false) },
                onSelectBuiltInWidget = viewModel::addBuiltInWidget,
                onRequestSystemWidget = { provider ->
                    val label = provider.loadLabel(context.packageManager)
                    val id = viewModel.widgetHelper.allocateWidgetId()
                    if (id != -1) {
                        if (viewModel.widgetHelper.tryBindWidget(id, provider)) {
                            viewModel.addSystemAppWidget(id, label)
                        } else {
                            pendingBindWidgetLabel = label
                            widgetBindLauncher.launch(viewModel.widgetHelper.createBindIntent(id, provider))
                        }
                    }
                }
            )
        }

        // Pending Crash Recovery Dialog (next-launch recovery)
        val dialogCrash = uiState.pendingCrashReport ?: inspectedCrashReport
        if (dialogCrash != null) {
            CrashRecoveryDialog(
                report = dialogCrash,
                formattedShareText = viewModel.crashRecoveryHelper.formatShareableReport(dialogCrash),
                safeModeActive = uiState.safeModeActive,
                onDismissBack = {
                    if (uiState.pendingCrashReport != null) {
                        viewModel.acknowledgeCrashReport()
                    }
                    inspectedCrashReport = null
                }
            )
        }

        // GitHub Release Update Notification Dialog
        val availableUpdate = uiState.availableUpdate
        if (availableUpdate != null && uiState.pendingCrashReport == null) {
            UpdateAvailableDialog(
                updateInfo = availableUpdate,
                downloadState = uiState.updateDownloadState,
                onSkip = viewModel::onUpdateSkip,
                onLater = viewModel::onUpdateLater,
                onUpdate = viewModel::onStartUpdateDownload,
                onRequestInstallPermission = {
                    runCatching {
                        context.startActivity(viewModel.updateHelper.createManageUnknownAppSourcesIntent())
                    }
                },
                onInstallReadyApk = { viewModel.launchPendingApkInstall() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
        )
    }
}
