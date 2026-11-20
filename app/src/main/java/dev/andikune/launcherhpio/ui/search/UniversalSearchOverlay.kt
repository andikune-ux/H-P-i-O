package dev.andikune.launcherhpio.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.HomeGridItem
import dev.andikune.launcherhpio.domain.model.InstalledApp
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.ui.components.GlassAppIconTile
import dev.andikune.launcherhpio.ui.components.LiquidGlassSurface

@Composable
fun UniversalSearchOverlay(
    query: String,
    installedApps: List<InstalledApp>,
    folders: List<HomeGridItem.FolderItem>,
    preferences: LauncherPreferences,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenSystemHub: () -> Unit,
    onWebSearch: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val trimmed = query.trim().lowercase()
    val matchingApps = remember(installedApps, trimmed) {
        if (trimmed.isEmpty()) installedApps.take(8)
        else installedApps.filter {
            it.label.lowercase().contains(trimmed) || it.packageName.lowercase().contains(trimmed)
        }.take(20)
    }
    val matchingFolders = remember(folders, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else folders.filter { it.name.lowercase().contains(trimmed) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            preferences = preferences,
            cornerRadius = 30.dp,
            alphaBoost = 0.30f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali ke Layar Utama"
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Cari aplikasi, folder, atau web…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Bersihkan")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("universal_search_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (matchingApps.isNotEmpty()) {
                        item {
                            Text(
                                text = if (trimmed.isEmpty()) "Aplikasi Teratas" else "Aplikasi Cocok (${matchingApps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchingApps.chunked(4)) { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowApps.forEach { app ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        GlassAppIconTile(
                                            label = app.label,
                                            appEntry = app,
                                            iconSize = 50.dp,
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = { onLaunchApp(app) }
                                        )
                                    }
                                }
                                repeat(4 - rowApps.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (matchingFolders.isNotEmpty()) {
                        item {
                            Text(
                                text = "Folder Layar Utama",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchingFolders, key = { it.id }) { folder ->
                            SearchActionRow(
                                icon = Icons.Rounded.Folder,
                                title = folder.name,
                                subtitle = "${folder.apps.size} aplikasi",
                                onClick = {
                                    onClose()
                                    onOpenFolder(folder.id)
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Pintasan H P i O",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        SearchActionRow(
                            icon = Icons.Rounded.Tune,
                            title = "Pusat Kontrol Liquid Glass",
                            subtitle = "Wi-Fi, Bluetooth, Senter, Kecerahan & Volume",
                            onClick = onOpenControlCenter
                        )
                    }
                    item {
                        SearchActionRow(
                            icon = Icons.Rounded.Palette,
                            title = "Personalisasi & Tampilan",
                            subtitle = "Wallpaper, blur kaca, ukuran ikon, grid & gestur",
                            onClick = onOpenPersonalization
                        )
                    }
                    item {
                        SearchActionRow(
                            icon = Icons.Rounded.Settings,
                            title = "Diagnostik, Izin & Pembaruan Sistem",
                            subtitle = "Kelola izin, penyimpanan privat, log crash & rilis GitHub",
                            onClick = onOpenSystemHub
                        )
                    }

                    if (trimmed.isNotEmpty()) {
                        item {
                            SearchActionRow(
                                icon = Icons.Rounded.Public,
                                title = "Cari \"$query\" di Web",
                                subtitle = "Buka pencarian lewat browser sistem",
                                onClick = { onWebSearch(query) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
