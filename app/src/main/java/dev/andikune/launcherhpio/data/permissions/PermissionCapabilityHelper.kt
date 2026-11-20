package dev.andikune.launcherhpio.data.permissions

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.andikune.launcherhpio.domain.model.CapabilityAvailability
import dev.andikune.launcherhpio.domain.model.CapabilityStatusItem

/**
 * Evaluates minimum required & optional launcher capabilities, explains access purpose,
 * and handles granted / denied / permanently denied / revoked / unavailable states cleanly.
 */
class PermissionCapabilityHelper(private val context: Context) {

    fun isDefaultLauncher(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun hasFlashUnit(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
            cm.cameraIdList.any { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canWriteSystemSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun evaluateRuntimePermissionState(
        permission: String,
        activity: Activity?,
        wasRequestedBefore: Boolean
    ): CapabilityAvailability {
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) return CapabilityAvailability.GRANTED
        if (activity == null) return CapabilityAvailability.DENIED
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return if (wasRequestedBefore && !shouldShowRationale) {
            CapabilityAvailability.PERMANENTLY_DENIED
        } else {
            CapabilityAvailability.DENIED
        }
    }

    fun collectCapabilityStatuses(
        activity: Activity?,
        bluetoothRequestedBefore: Boolean
    ): List<CapabilityStatusItem> {
        val homeStatus = if (isDefaultLauncher()) {
            CapabilityAvailability.GRANTED
        } else {
            CapabilityAvailability.DENIED
        }

        val btStatus = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> CapabilityAvailability.GRANTED
            else -> evaluateRuntimePermissionState(
                Manifest.permission.BLUETOOTH_CONNECT,
                activity,
                bluetoothRequestedBefore
            )
        }

        val writeSettingsStatus = if (canWriteSystemSettings()) {
            CapabilityAvailability.GRANTED
        } else {
            CapabilityAvailability.DENIED
        }

        val installPkgStatus = if (canInstallPackages()) {
            CapabilityAvailability.GRANTED
        } else {
            CapabilityAvailability.DENIED
        }

        val torchStatus = if (hasFlashUnit()) {
            CapabilityAvailability.GRANTED
        } else {
            CapabilityAvailability.UNAVAILABLE
        }

        return listOf(
            CapabilityStatusItem(
                id = "default_home",
                title = "Launcher Utama (Home Role)",
                purpose = "Menjadikan H P i O sebagai layar utama saat tombol Home ditekan.",
                availability = homeStatus,
                isOptional = false,
                actionLabel = if (homeStatus == CapabilityAvailability.GRANTED) "Aktif" else "Atur Default"
            ),
            CapabilityStatusItem(
                id = "bluetooth_connect",
                title = "Status Bluetooth (Opsional)",
                purpose = "Membaca status aktif/nonaktif Bluetooth di Pusat Kontrol tanpa memaksa akses.",
                availability = btStatus,
                isOptional = true,
                actionLabel = when (btStatus) {
                    CapabilityAvailability.GRANTED -> "Diizinkan"
                    CapabilityAvailability.PERMANENTLY_DENIED -> "Buka Pengaturan"
                    else -> "Izinkan"
                }
            ),
            CapabilityStatusItem(
                id = "write_settings",
                title = "Ubah Kecerahan Sistem (Opsional)",
                purpose = "Mengubah kecerahan layar sistem langsung dari slider Pusat Kontrol.",
                availability = writeSettingsStatus,
                isOptional = true,
                actionLabel = if (writeSettingsStatus == CapabilityAvailability.GRANTED) "Diizinkan" else "Kelola Izin"
            ),
            CapabilityStatusItem(
                id = "install_packages",
                title = "Pasang Pembaruan APK (Opsional)",
                purpose = "Memungkinkan pemasangan pembaruan resmi dari GitHub Releases dengan konfirmasi Anda.",
                availability = installPkgStatus,
                isOptional = true,
                actionLabel = if (installPkgStatus == CapabilityAvailability.GRANTED) "Diizinkan" else "Izinkan Sumber"
            ),
            CapabilityStatusItem(
                id = "camera_flash",
                title = "Unit Lampu Senter",
                purpose = "Mengaktifkan senter dari Pusat Kontrol menggunakan API perangkat keras resmi.",
                availability = torchStatus,
                isOptional = true,
                actionLabel = if (torchStatus == CapabilityAvailability.GRANTED) "Tersedia" else "Tidak Tersedia"
            )
        )
    }

    fun createDefaultHomeIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createAppDetailsSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createWriteSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
