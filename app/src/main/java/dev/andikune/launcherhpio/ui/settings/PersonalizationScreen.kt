package dev.andikune.launcherhpio.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.BuiltInWallpaper
import dev.andikune.launcherhpio.domain.model.DrawerAnimationStyle
import dev.andikune.launcherhpio.domain.model.GridPreset
import dev.andikune.launcherhpio.domain.model.IconSizePreset
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.domain.model.ThemeMode
import dev.andikune.launcherhpio.ui.components.LiquidGlassSurface
import dev.andikune.launcherhpio.ui.components.liquidGlassPanelModifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(
    preferences: LauncherPreferences,
    onUpdatePreferences: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onOpenSystemWallpaperPicker: () -> Unit,
    onOpenSystemHub: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .testTag("personalization_screen"),
            preferences = preferences,
            cornerRadius = 30.dp,
            alphaBoost = 0.32f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personalisasi H P i O",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Tema Hybrid OS, Liquid Glass, Grid & Gestur",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // System Hub & Mandatory Capabilities Entry
                Button(
                    onClick = onOpenSystemHub,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_system_hub_button")
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnostik, Izin, Penyimpanan & Pembaruan")
                }

                // Theme Mode
                SettingsSectionCard(preferences = preferences, title = "Mode Tampilan") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    onUpdatePreferences { it.copy(themeMode = mode) }
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }

                // Wallpaper Selection
                SettingsSectionCard(preferences = preferences, title = "Wallpaper Liquid Glass") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BuiltInWallpaper.entries
                            .filter { it != BuiltInWallpaper.CUSTOM_IMAGE || preferences.customWallpaperPath != null }
                            .forEach { wp ->
                                FilterChip(
                                    selected = preferences.selectedWallpaper == wp,
                                    onClick = {
                                        onUpdatePreferences { it.copy(selectedWallpaper = wp) }
                                    },
                                    label = { Text(wp.title) }
                                )
                            }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickCustomWallpaper,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Foto")
                        }
                        OutlinedButton(
                            onClick = onOpenSystemWallpaperPicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Wallpaper, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wallpaper OS")
                        }
                    }
                }

                // Liquid Glass Transparency & Blur
                SettingsSectionCard(preferences = preferences, title = "Material Liquid Glass") {
                    Text(
                        text = "Transparansi Panel (${(preferences.glassTransparency * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = preferences.glassTransparency,
                        onValueChange = { v -> onUpdatePreferences { it.copy(glassTransparency = v) } },
                        valueRange = 0.15f..0.85f
                    )
                    Text(
                        text = "Intensitas Blur Latar (${preferences.blurIntensityDp.toInt()} dp)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = preferences.blurIntensityDp,
                        onValueChange = { v -> onUpdatePreferences { it.copy(blurIntensityDp = v) } },
                        valueRange = 0f..24f
                    )
                }

                // Grid & Icon Size
                SettingsSectionCard(preferences = preferences, title = "Grid & Ukuran Ikon") {
                    Text("Grid Layar Utama", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GridPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = preferences.homeGridPreset == preset,
                                onClick = { onUpdatePreferences { it.copy(homeGridPreset = preset) } },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Kolom Laci Aplikasi", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5).forEach { cols ->
                            FilterChip(
                                selected = preferences.drawerColumns == cols,
                                onClick = { onUpdatePreferences { it.copy(drawerColumns = cols) } },
                                label = { Text("$cols Kolom") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ukuran Ikon", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconSizePreset.entries.forEach { size ->
                            FilterChip(
                                selected = preferences.iconSizePreset == size,
                                onClick = { onUpdatePreferences { it.copy(iconSizePreset = size) } },
                                label = { Text(size.label) }
                            )
                        }
                    }
                }

                // Animation Style
                SettingsSectionCard(preferences = preferences, title = "Animasi Laci Aplikasi") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrawerAnimationStyle.entries.forEach { style ->
                            FilterChip(
                                selected = preferences.drawerAnimationStyle == style,
                                onClick = { onUpdatePreferences { it.copy(drawerAnimationStyle = style) } },
                                label = { Text(style.label) }
                            )
                        }
                    }
                }

                // Dock, Widgets & Gestures
                SettingsSectionCard(preferences = preferences, title = "Dock, Widget & Gestur") {
                    SettingToggleRow(
                        title = "Tampilkan Dock Mengambang",
                        subtitle = "Bar aplikasi cepat di bawah Layar Utama",
                        checked = preferences.showDock,
                        onCheckedChange = { checked -> onUpdatePreferences { it.copy(showDock = checked) } }
                    )
                    SettingToggleRow(
                        title = "Tombol Laci Aplikasi di Dock",
                        subtitle = "Akses satu ketuk ke seluruh aplikasi terpasang",
                        checked = preferences.showAppDrawerButton,
                        onCheckedChange = { checked -> onUpdatePreferences { it.copy(showAppDrawerButton = checked) } }
                    )
                    SettingToggleRow(
                        title = "Geser ke Atas untuk Laci Aplikasi",
                        subtitle = "Gestur swipe-up di Layar Utama",
                        checked = preferences.swipeUpForDrawer,
                        onCheckedChange = { checked -> onUpdatePreferences { it.copy(swipeUpForDrawer = checked) } }
                    )
                    SettingToggleRow(
                        title = "Geser ke Bawah untuk Pusat Kontrol",
                        subtitle = "Buka Pusat Kontrol Liquid Glass dari Layar Utama",
                        checked = preferences.swipeDownForControlCenter,
                        onCheckedChange = { checked -> onUpdatePreferences { it.copy(swipeDownForControlCenter = checked) } }
                    )
                    SettingToggleRow(
                        title = "Header Jam & Status Hybrid OS",
                        subtitle = "Tampilkan jam besar, tanggal, baterai & status jaringan di Halaman 1",
                        checked = preferences.showBuiltInClockHero,
                        onCheckedChange = { checked -> onUpdatePreferences { it.copy(showBuiltInClockHero = checked) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    preferences: LauncherPreferences,
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(liquidGlassPanelModifier(preferences, 22.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
