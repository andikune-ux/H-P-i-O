package dev.andikune.launcherhpio.ui.dialogs

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.ReleaseUpdateInfo
import dev.andikune.launcherhpio.domain.model.UpdateDownloadState

@Composable
fun UpdateAvailableDialog(
    updateInfo: ReleaseUpdateInfo,
    downloadState: UpdateDownloadState,
    onSkip: (String) -> Unit,
    onLater: () -> Unit,
    onUpdate: (ReleaseUpdateInfo) -> Unit,
    onRequestInstallPermission: () -> Unit,
    onInstallReadyApk: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onLater()
            }
        },
        title = {
            Text(
                text = "Pembaruan Tersedia · v${updateInfo.versionName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = buildString {
                        append("Rilis resmi GitHub: ${updateInfo.tagName}")
                        if (updateInfo.publishedAt.isNotBlank()) {
                            append(" · ${updateInfo.publishedAt}")
                        }
                        if (updateInfo.apkSizeBytes > 0L) {
                            append(" (${Formatter.formatShortFileSize(context, updateInfo.apkSizeBytes)})")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Mengunduh APK… ${downloadState.progressPercent}% (${Formatter.formatShortFileSize(context, downloadState.downloadedBytes)})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    is UpdateDownloadState.RequiresInstallPermission -> {
                        Text(
                            text = "Unduhan selesai & terverifikasi. Izinkan 'Pasang aplikasi dari sumber ini' di Pengaturan Android untuk melanjutkan pemasangan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Text(
                            text = "Berkas APK siap dipasang melalui dialog persetujuan resmi Android.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is UpdateDownloadState.Error -> {
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    UpdateDownloadState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (downloadState !is UpdateDownloadState.Downloading) {
                    TextButton(
                        onClick = { onSkip(updateInfo.versionName) },
                        modifier = Modifier.testTag("update_skip_button")
                    ) {
                        Text("Skip")
                    }
                    OutlinedButton(
                        onClick = onLater,
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text("Nanti")
                    }
                }
                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text("Mengunduh…")
                        }
                    }
                    is UpdateDownloadState.RequiresInstallPermission -> {
                        Button(onClick = onRequestInstallPermission) {
                            Text("Buka Izin")
                        }
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Button(onClick = onInstallReadyApk) {
                            Text("Pasang")
                        }
                    }
                    else -> {
                        Button(
                            onClick = { onUpdate(updateInfo) },
                            modifier = Modifier.testTag("update_confirm_button")
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    )
}
