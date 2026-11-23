package dev.andikune.launcherhpio.ui.controlcenter

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.ControlCenterState
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.ui.components.LiquidGlassSurface
import dev.andikune.launcherhpio.ui.components.liquidGlassPanelModifier

@Composable
fun ControlCenterOverlay(
    state: ControlCenterState,
    preferences: LauncherPreferences,
    onOpenWifiPanel: () -> Unit,
    onOpenBluetoothOrRequestPermission: () -> Unit,
    onToggleTorch: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onRequestWriteSettings: () -> Unit,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onOpenDefaultLauncherSetup: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

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
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .testTag("control_center_surface"),
            preferences = preferences,
            cornerRadius = 32.dp,
            alphaBoost = 0.30f
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pusat Kontrol",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Baterai ${state.batteryPercent}% · Kontrol Sistem Hybrid OS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_control_center_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup Pusat Kontrol"
                        )
                    }
                }

                // Connectivity + Torch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ControlTile(
                        icon = Icons.Rounded.Wifi,
                        title = "Wi-Fi",
                        subtitle = state.wifiLabel,
                        isActive = state.wifiEnabled,
                        preferences = preferences,
                        onClick = onOpenWifiPanel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cc_wifi_tile")
                    )
                    ControlTile(
                        icon = Icons.Rounded.Bluetooth,
                        title = "Bluetooth",
                        subtitle = when {
                            state.bluetoothPermissionMissing -> "Izin baca status"
                            state.bluetoothEnabled -> "Aktif"
                            else -> "Nonaktif"
                        },
                        isActive = state.bluetoothEnabled,
                        preferences = preferences,
                        onClick = onOpenBluetoothOrRequestPermission,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cc_bluetooth_tile")
                    )
                    ControlTile(
                        icon = if (state.torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        title = "Senter",
                        subtitle = when {
                            !state.torchAvailable -> "Tidak ada"
                            state.torchEnabled -> "Menyala"
                            else -> "Mati"
                        },
                        isActive = state.torchEnabled,
                        preferences = preferences,
                        onClick = onToggleTorch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cc_torch_tile")
                    )
                }

                // Brightness Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(liquidGlassPanelModifier(preferences, 22.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.BrightnessHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kecerahan Layar (${(state.brightnessFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (!state.canWriteSystemSettings) {
                            TextButton(onClick = onRequestWriteSettings) {
                                Text("Izin Sistem")
                            }
                        }
                    }
                    Slider(
                        value = state.brightnessFraction,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier.testTag("cc_brightness_slider")
                    )
                    if (!state.canWriteSystemSettings) {
                        Text(
                            text = "Slider saat ini mengatur kecerahan jendela launcher. Ketuk 'Izin Sistem' untuk mengubah kecerahan sistem global.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Audio Volume Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Volume Media (${state.mediaVolume}/${state.mediaMaxVolume})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Slider(
                        value = state.mediaVolume.toFloat(),
                        onValueChange = { onMediaVolumeChange(it.toInt()) },
                        valueRange = 0f..state.mediaMaxVolume.toFloat().coerceAtLeast(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Volume Dering (${state.ringVolume}/${state.ringMaxVolume})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Slider(
                        value = state.ringVolume.toFloat(),
                        onValueChange = { onRingVolumeChange(it.toInt()) },
                        valueRange = 0f..state.ringMaxVolume.toFloat().coerceAtLeast(1f)
                    )
                }

                // Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ControlTile(
                        icon = Icons.Rounded.Home,
                        title = "Launcher",
                        subtitle = if (state.isDefaultLauncher) "Default Aktif" else "Atur Default",
                        isActive = state.isDefaultLauncher,
                        preferences = preferences,
                        onClick = onOpenDefaultLauncherSetup,
                        modifier = Modifier.weight(1f)
                    )
                    ControlTile(
                        icon = Icons.Rounded.Palette,
                        title = "Tema Kaca",
                        subtitle = "Personalisasi",
                        isActive = false,
                        preferences = preferences,
                        onClick = onOpenPersonalization,
                        modifier = Modifier.weight(1f)
                    )
                    ControlTile(
                        icon = Icons.Rounded.Settings,
                        title = "Pengaturan",
                        subtitle = "Sistem Android",
                        isActive = false,
                        preferences = preferences,
                        onClick = onOpenSystemSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    preferences: LauncherPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val contentTint = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .then(liquidGlassPanelModifier(preferences, 22.dp, if (isActive) 0.10f else 0f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = contentTint,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = contentTint.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}
