package dev.andikune.launcherhpio.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.CrashReportRecord
import dev.andikune.launcherhpio.ui.theme.JetBrainsMonoFontFamily

@Composable
fun CrashRecoveryDialog(
    report: CrashReportRecord,
    formattedShareText: String,
    safeModeActive: Boolean,
    onDismissBack: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissBack,
        title = {
            Text(
                text = "Laporan Pemulihan Crash",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (safeModeActive) {
                    Text(
                        text = "Mode Aman aktif sementara: efek blur diturunkan untuk mencegah crash berulang.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "Waktu: ${report.timestampIso}\nVersi: ${report.appVersion} (${report.versionCode})\nPerangkat: ${report.deviceModel} · ${report.androidVersion}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Exception: ${report.exceptionType}\nPesan: ${report.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = report.stackTrace,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
                Text(
                    text = "Data sensitif telah disensor otomatis. Catatan: Force-stop manual, OS kill, mati daya, atau native crash tertentu tidak selalu dapat ditangkap.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (copied) {
                    Text(
                        text = "Log crash berhasil disalin ke clipboard.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("H P i O Crash Log", formattedShareText))
                        copied = true
                    },
                    modifier = Modifier.testTag("crash_copy_log_button")
                ) {
                    Text("Salin Log")
                }
                Button(
                    onClick = onDismissBack,
                    modifier = Modifier.testTag("crash_back_button")
                ) {
                    Text("Kembali")
                }
            }
        }
    )
}
