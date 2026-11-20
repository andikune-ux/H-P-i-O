package dev.andikune.launcherhpio.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.andikune.launcherhpio.domain.model.HomeGridItem
import dev.andikune.launcherhpio.domain.model.InstalledApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    appEntry: InstalledApp,
    existingFolders: List<HomeGridItem.FolderItem>,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onPinToHome: () -> Unit,
    onPinToDock: () -> Unit,
    onCreateFolder: () -> Unit,
    onAddToFolder: (String) -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUninstall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = appEntry.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = appEntry.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SheetActionRow(
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                label = "Buka Aplikasi",
                testTag = "action_launch_app",
                onClick = onLaunch
            )
            SheetActionRow(
                icon = Icons.Rounded.AddToHomeScreen,
                label = "Sematkan ke Layar Utama",
                testTag = "action_pin_home",
                onClick = onPinToHome
            )
            SheetActionRow(
                icon = Icons.Rounded.Dock,
                label = "Sematkan ke Dock",
                testTag = "action_pin_dock",
                onClick = onPinToDock
            )
            SheetActionRow(
                icon = Icons.Rounded.CreateNewFolder,
                label = "Buat Folder Baru di Layar Utama",
                testTag = "action_create_folder",
                onClick = onCreateFolder
            )

            existingFolders.forEach { folder ->
                SheetActionRow(
                    icon = Icons.Rounded.FolderOpen,
                    label = "Tambahkan ke Folder '${folder.name}'",
                    testTag = "action_add_folder_${folder.id}",
                    onClick = { onAddToFolder(folder.id) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            SheetActionRow(
                icon = Icons.Rounded.Info,
                label = "Info Aplikasi Sistem",
                testTag = "action_app_info",
                onClick = onOpenAppInfo
            )
            if (!appEntry.isSystemApp) {
                SheetActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Copot Pemasangan",
                    isDestructive = true,
                    testTag = "action_uninstall_app",
                    onClick = onRequestUninstall
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
