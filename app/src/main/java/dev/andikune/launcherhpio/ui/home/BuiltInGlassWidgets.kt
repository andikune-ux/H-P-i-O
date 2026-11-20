package dev.andikune.launcherhpio.ui.home

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.andikune.launcherhpio.data.launcher.LauncherWidgetHelper
import dev.andikune.launcherhpio.domain.model.BuiltInWidgetType
import dev.andikune.launcherhpio.domain.model.ControlCenterState
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.ui.components.LiquidGlassSurface
import dev.andikune.launcherhpio.ui.theme.CyanPrism
import dev.andikune.launcherhpio.ui.theme.EmeraldPulse
import dev.andikune.launcherhpio.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeroGlassClockHeader(
    preferences: LauncherPreferences,
    controlCenterState: ControlCenterState,
    onOpenControlCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 60_000L - (nowMillis % 60_000L)
            delay(delayMs.coerceIn(1_000L, 60_000L))
        }
    }

    val timeStr = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateStr = remember(nowMillis) {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date(nowMillis))
    }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenControlCenter),
        preferences = preferences,
        cornerRadius = 28.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (controlCenterState.isCharging) {
                            Icons.Rounded.BatteryChargingFull
                        } else {
                            Icons.Rounded.BatteryStd
                        },
                        contentDescription = "Baterai",
                        tint = EmeraldPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${controlCenterState.batteryPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = "Konektivitas",
                        tint = CyanPrism,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = controlCenterState.wifiLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BuiltInGlassWidgetCard(
    widgetType: BuiltInWidgetType,
    preferences: LauncherPreferences,
    controlCenterState: ControlCenterState,
    onUpdateNote: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (widgetType) {
        BuiltInWidgetType.CLOCK_STATUS -> {
            HeroGlassClockHeader(
                preferences = preferences,
                controlCenterState = controlCenterState,
                onOpenControlCenter = {},
                modifier = modifier
            )
        }
        BuiltInWidgetType.SYSTEM_TELEMETRY -> {
            SystemTelemetryGlassWidget(
                preferences = preferences,
                controlCenterState = controlCenterState,
                onRemove = onRemove,
                modifier = modifier
            )
        }
        BuiltInWidgetType.QUICK_NOTES -> {
            QuickNotesGlassWidget(
                preferences = preferences,
                onUpdateNote = onUpdateNote,
                onRemove = onRemove,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SystemTelemetryGlassWidget(
    preferences: LauncherPreferences,
    controlCenterState: ControlCenterState,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (ramFraction, ramLabel) = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        val totalMb = (mi.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = (mi.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val frac = (usedMb.toFloat() / totalMb.toFloat()).coerceIn(0f, 1f)
        frac to "${usedMb / 1024.0f} / ${totalMb / 1024.0f}".let {
            String.format(Locale.US, "%.1f / %.1f GB RAM", usedMb / 1024f, totalMb / 1024f)
        }
    }
    val (storageFraction, storageLabel) = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val total = stat.totalBytes.coerceAtLeast(1L)
            val avail = stat.availableBytes.coerceAtLeast(0L)
            val used = (total - avail).coerceAtLeast(0L)
            val frac = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val freeGb = avail / (1024f * 1024f * 1024f)
            frac to String.format(Locale.US, "%.1f GB bebas", freeGb)
        }.getOrDefault(0.4f to "Penyimpanan siap")
    }

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        preferences = preferences,
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monitor Hybrid OS",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus widget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ramLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Baterai ${controlCenterState.batteryPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPulse,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { ramFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = storageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { storageFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun QuickNotesGlassWidget(
    preferences: LauncherPreferences,
    onUpdateNote: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    var draftText by remember(preferences.quickNoteText) { mutableStateOf(preferences.quickNoteText) }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { editing = true },
        preferences = preferences,
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catatan Kaca Cepat",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus widget catatan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preferences.quickNoteText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Ubah Catatan Cepat") },
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it.take(240) },
                    label = { Text("Isi catatan") },
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_note_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNote(draftText.ifBlank { "Ketuk untuk menulis catatan cepat di Layar Utama H P i O." })
                        editing = false
                    },
                    modifier = Modifier.testTag("save_quick_note_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SystemAppWidgetCard(
    appWidgetId: Int,
    providerLabel: String,
    widgetHelper: LauncherWidgetHelper,
    preferences: LauncherPreferences,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hostView = remember(appWidgetId) { widgetHelper.createHostView(appWidgetId) }

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        preferences = preferences,
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = providerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus widget sistem",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (hostView != null) {
                AndroidView(
                    factory = { hostView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            } else {
                Text(
                    text = "Widget sistem tidak tersedia atau izin bind belum diberikan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}
