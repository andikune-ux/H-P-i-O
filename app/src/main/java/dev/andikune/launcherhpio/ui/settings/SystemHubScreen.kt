package dev.andikune.launcherhpio.ui.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.BuildConfig
import dev.andikune.launcherhpio.domain.model.CapabilityAvailability
import dev.andikune.launcherhpio.domain.model.CapabilityStatusItem
import dev.andikune.launcherhpio.domain.model.CrashReportRecord
import dev.andikune.launcherhpio.domain.model.LauncherPreferences
import dev.andikune.launcherhpio.domain.model.StorageSummary
import dev.andikune.launcherhpio.ui.components.LiquidGlassSurface

@Composable
fun SystemHubScreen(
    preferences: LauncherPreferences,
    capabilities: List<CapabilityStatusItem>,
    storageSummary: StorageSummary?,
    crashHistory: List<CrashReportRecord>,
    lastUpdateStatus: String?,
    skippedVersion: String?,
    onCapabilityAction: (CapabilityStatusItem) -> Unit,
    onCheckUpdatesNow: () -> Unit,
    onClearSkippedVersion: () -> Unit,
    onClearUpdatesCache: () -> Unit,
    onClearCrashLogs: () -> Unit,
    onViewCrashReport: (CrashReportRecord) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("system_hub_screen"),
            preferences = preferences,
            cornerRadius = 30.dp,
            alphaBoost = 0.34f
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
                    Column {
                        Text(
                            text = "Pusat Sistem & Diagnostik",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "H P i O v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.APPLICATION_ID}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Permission & Capability Management
                HubCard(
                    icon = Icons.Rounded.Security,
                    title = "Izin & Kapabilitas Minimum"
                ) {
                    capabilities.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.purpose,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when (item.availability) {
                                        CapabilityAvailability.GRANTED -> "Status: Diizinkan / Tersedia"
                                        CapabilityAvailability.DENIED -> "Status: Belum diizinkan (fitur tetap aman digunakan)"
                                        CapabilityAvailability.PERMANENTLY_DENIED -> "Status: Ditolak permanen (kelola lewat Pengaturan)"
                                        CapabilityAvailability.REVOKED -> "Status: Dicabut oleh sistem"
                                        CapabilityAvailability.UNAVAILABLE -> "Status: Perangkat keras tidak tersedia"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (item.availability != CapabilityAvailability.UNAVAILABLE) {
                                OutlinedButton(onClick = { onCapabilityAction(item) }) {
                                    Text(item.actionLabel)
                                }
                            }
                        }
                    }
                }

                // 2. Release Update Checker
                HubCard(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Pembaruan GitHub Releases (andikune-ux/H-P-i-O)"
                ) {
                    Text(
                        text = lastUpdateStatus ?: "Memeriksa rilis stabil resmi secara asinkron tanpa token privat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!skippedVersion.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Versi dilewati: $skippedVersion",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onClearSkippedVersion) {
                                Text("Reset Skip")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCheckUpdatesNow,
                        modifier = Modifier.testTag("manual_check_update_button")
                    ) {
                        Text("Periksa Pembaruan Sekarang")
                    }
                }

                // 3. Structured Application Storage
                HubCard(
                    icon = Icons.Rounded.Folder,
                    title = "Penyimpanan Privat Terstruktur"
                ) {
                    if (storageSummary != null) {
                        Text(
                            text = "Root Privat: ${storageSummary.rootFolderPath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Pembaruan Aplikasi: ${storageSummary.updatesFileCount} berkas (${Formatter.formatShortFileSize(context, storageSummary.updatesBytes)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Log Crash: ${storageSummary.crashFileCount} berkas (${Formatter.formatShortFileSize(context, storageSummary.crashBytes)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Ruang Bebas Tersedia: ${Formatter.formatShortFileSize(context, storageSummary.freeSpaceBytes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onClearUpdatesCache) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bersihkan APK")
                            }
                            OutlinedButton(onClick = onClearCrashLogs) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bersihkan Log")
                            }
                        }
                    }
                }

                // 4. Crash Recovery Logs & Limitations
                HubCard(
                    icon = Icons.Rounded.BugReport,
                    title = "Riwayat Pemulihan Crash (${crashHistory.size})"
                ) {
                    Text(
                        text = "Catatan batasan OS: Force-stop manual, penghentian paksa oleh OS (OOM/LMK), kehilangan daya mendadak, atau native crash tertentu tidak selalu dapat ditangkap sebelum proses berhenti.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (crashHistory.isEmpty()) {
                        Text(
                            text = "Belum ada catatan crash tersimpan.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        crashHistory.take(5).forEach { report ->
                            AssistChip(
                                onClick = { onViewCrashReport(report) },
                                label = {
                                    Text("${report.exceptionType} · ${report.timestampIso}")
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
