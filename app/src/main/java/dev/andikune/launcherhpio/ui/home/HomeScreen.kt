package dev.andikune.launcherhpio.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.data.launcher.LauncherWidgetHelper
import dev.andikune.launcherhpio.domain.model.BuiltInWidgetType
import dev.andikune.launcherhpio.domain.model.HomeGridItem
import dev.andikune.launcherhpio.domain.model.InstalledApp
import dev.andikune.launcherhpio.ui.LauncherUiState
import dev.andikune.launcherhpio.ui.components.GlassAppIconTile
import dev.andikune.launcherhpio.ui.components.LiquidGlassPill
import dev.andikune.launcherhpio.ui.components.LiquidGlassSurface

@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    installedAppsByComponent: Map<String, InstalledApp>,
    widgetHelper: LauncherWidgetHelper,
    onPageChanged: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onAddPage: () -> Unit,
    onRemoveEmptyLastPage: () -> Unit,
    onLaunchApp: (String, String, String) -> Unit,
    onOpenAppActions: (InstalledApp) -> Unit,
    onRemoveHomeItem: (Int, String) -> Unit,
    onRemoveDockApp: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onUpdateQuickNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val pages = uiState.pages.ifEmpty { listOf() }
    val pagerState = rememberPagerState(
        initialPage = uiState.currentHomePage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
        pageCount = { pages.size.coerceAtLeast(1) }
    )

    LaunchedEffect(uiState.currentHomePage, pages.size) {
        val target = uiState.currentHomePage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .pointerInput(prefs.swipeUpForDrawer, prefs.swipeDownForControlCenter) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragAccumulator = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        verticalDragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        if (prefs.swipeUpForDrawer && verticalDragAccumulator < -90f) {
                            onOpenDrawer()
                        } else if (prefs.swipeDownForControlCenter && verticalDragAccumulator > 90f) {
                            onOpenControlCenter()
                        }
                        verticalDragAccumulator = 0f
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Utility & Search Bar
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassPill(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_search_pill"),
                        preferences = prefs,
                        onClick = onOpenSearch
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Cari",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cari aplikasi, folder, atau pengaturan…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    LiquidGlassPill(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("control_center_button"),
                        preferences = prefs,
                        onClick = onOpenControlCenter
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Buka Pusat Kontrol",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    LiquidGlassPill(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("personalization_button"),
                        preferences = prefs,
                        onClick = onOpenPersonalization
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = "Personalisasi Launcher",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (prefs.showBuiltInClockHero && pagerState.currentPage == 0) {
                    HeroGlassClockHeader(
                        preferences = prefs,
                        controlCenterState = uiState.controlCenterState,
                        onOpenControlCenter = onOpenControlCenter
                    )
                }
            }

            // Multi-Page Home Workspace
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) { pageIndex ->
                val pageData = pages.getOrNull(pageIndex)
                val items = pageData?.items.orEmpty()
                val widgets = items.filter {
                    it is HomeGridItem.BuiltInWidgetItem || it is HomeGridItem.SystemWidgetItem
                }
                val gridEntries = items.filter {
                    it is HomeGridItem.AppShortcut || it is HomeGridItem.FolderItem
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(widgets, key = { it.id }) { widget ->
                        when (widget) {
                            is HomeGridItem.BuiltInWidgetItem -> {
                                BuiltInGlassWidgetCard(
                                    widgetType = widget.widgetType,
                                    preferences = prefs,
                                    controlCenterState = uiState.controlCenterState,
                                    onUpdateNote = onUpdateQuickNote,
                                    onRemove = { onRemoveHomeItem(pageIndex, widget.id) }
                                )
                            }
                            is HomeGridItem.SystemWidgetItem -> {
                                SystemAppWidgetCard(
                                    appWidgetId = widget.appWidgetId,
                                    providerLabel = widget.providerLabel,
                                    widgetHelper = widgetHelper,
                                    preferences = prefs,
                                    onRemove = { onRemoveHomeItem(pageIndex, widget.id) }
                                )
                            }
                            else -> Unit
                        }
                    }

                    if (gridEntries.isNotEmpty()) {
                        val cols = prefs.homeGridPreset.columns
                        val rows = gridEntries.chunked(cols)
                        items(rows.size) { rowIndex ->
                            val rowItems = rows[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (colIndex in 0 until cols) {
                                    val entry = rowItems.getOrNull(colIndex)
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (entry) {
                                            is HomeGridItem.AppShortcut -> {
                                                val resolved = installedAppsByComponent[
                                                    "${entry.packageName}/${entry.activityName}"
                                                ]
                                                GlassAppIconTile(
                                                    label = resolved?.label ?: entry.fallbackLabel,
                                                    appEntry = resolved,
                                                    iconSize = prefs.iconSizePreset.iconDp.dp,
                                                    onClick = {
                                                        onLaunchApp(
                                                            entry.packageName,
                                                            entry.activityName,
                                                            resolved?.label ?: entry.fallbackLabel
                                                        )
                                                    },
                                                    onLongClick = {
                                                        if (resolved != null) {
                                                            onOpenAppActions(resolved)
                                                        } else {
                                                            onRemoveHomeItem(pageIndex, entry.id)
                                                        }
                                                    }
                                                )
                                            }
                                            is HomeGridItem.FolderItem -> {
                                                FolderIconTile(
                                                    folder = entry,
                                                    installedAppsByComponent = installedAppsByComponent,
                                                    iconSize = prefs.iconSizePreset.iconDp.dp,
                                                    onClick = { onOpenFolder(entry.id) },
                                                    onLongClick = { onRemoveHomeItem(pageIndex, entry.id) }
                                                )
                                            }
                                            else -> Spacer(modifier = Modifier.size(prefs.iconSizePreset.iconDp.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else if (widgets.isEmpty()) {
                        item {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                preferences = prefs,
                                cornerRadius = 24.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Halaman ${pageIndex + 1} Masih Kosong",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tambahkan widget atau tekan lama aplikasi di Laci Aplikasi untuk menyematkannya ke sini.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Page Indicators + Quick Workspace Actions + Floating Liquid Glass Dock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassPill(
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("add_widget_pill"),
                        preferences = prefs,
                        onClick = onOpenWidgetPicker
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = "Tambah widget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Widget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { idx, _ ->
                            val selected = idx == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(width = if (selected) 20.dp else 7.dp, height = 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }

                    LiquidGlassPill(
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("add_home_page_button"),
                        preferences = prefs,
                        onClick = onAddPage
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Tambah halaman Layar Utama",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (pages.size > 1 && pages.last().items.isEmpty()) {
                        LiquidGlassPill(
                            modifier = Modifier.size(32.dp),
                            preferences = prefs,
                            onClick = onRemoveEmptyLastPage
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Hapus halaman kosong terakhir",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Floating Liquid Glass Dock & Configurable App Drawer Button
                if (prefs.showDock || prefs.showAppDrawerButton) {
                    LiquidGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_dock_bar"),
                        preferences = prefs,
                        cornerRadius = 30.dp,
                        alphaBoost = 0.12f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (prefs.showDock) {
                                uiState.dockApps.forEach { shortcut ->
                                    val resolved = installedAppsByComponent[
                                        "${shortcut.packageName}/${shortcut.activityName}"
                                    ]
                                    GlassAppIconTile(
                                        label = resolved?.label ?: shortcut.fallbackLabel,
                                        appEntry = resolved,
                                        iconSize = (prefs.iconSizePreset.iconDp - 4).dp,
                                        showLabel = false,
                                        onClick = {
                                            onLaunchApp(
                                                shortcut.packageName,
                                                shortcut.activityName,
                                                resolved?.label ?: shortcut.fallbackLabel
                                            )
                                        },
                                        onLongClick = { onRemoveDockApp(shortcut.id) }
                                    )
                                }
                            }

                            if (prefs.showAppDrawerButton) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable(onClick = onOpenDrawer)
                                        .testTag("app_drawer_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Apps,
                                        contentDescription = "Buka Laci Aplikasi",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderIconTile(
    folder: HomeGridItem.FolderItem,
    installedAppsByComponent: Map<String, InstalledApp>,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.20f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            val previewApps = folder.apps.take(4)
            if (previewApps.isEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = folder.name,
                    tint = Color.White
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    previewApps.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            pair.forEach { child ->
                                val resolved = installedAppsByComponent[
                                    "${child.packageName}/${child.activityName}"
                                ]
                                if (resolved?.icon != null) {
                                    Image(
                                        bitmap = resolved.icon,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size((iconSize - 14.dp) / 2)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size((iconSize - 14.dp) / 2)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.35f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 5.dp)
                .background(Color.Black.copy(alpha = 0.24f), CircleShape)
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    widgetHelper: LauncherWidgetHelper,
    onDismiss: () -> Unit,
    onSelectBuiltInWidget: (BuiltInWidgetType) -> Unit,
    onRequestSystemWidget: (android.appwidget.AppWidgetProviderInfo) -> Unit
) {
    val systemProviders = remember { widgetHelper.getInstalledProviders() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Text(
                    text = "Widget Liquid Glass H P i O",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(BuiltInWidgetType.entries) { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onSelectBuiltInWidget(type) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (systemProviders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Widget Aplikasi Android Terpasang (${systemProviders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(systemProviders.take(25)) { provider ->
                    val label = provider.loadLabel(
                        androidx.compose.ui.platform.LocalContext.current.packageManager
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { onRequestSystemWidget(provider) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
