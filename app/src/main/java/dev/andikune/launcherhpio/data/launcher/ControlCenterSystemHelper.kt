package dev.andikune.launcherhpio.data.launcher

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import dev.andikune.launcherhpio.data.permissions.PermissionCapabilityHelper
import dev.andikune.launcherhpio.domain.model.ControlCenterState

/**
 * Interfaces with official Android system services for the Liquid Glass Control Center.
 * Where Android restricts direct background toggles (Wi-Fi/Bluetooth on Android 10+),
 * cleanly routes to official Settings Panels or Settings screens without workarounds.
 */
class ControlCenterSystemHelper(
    private val context: Context,
    private val permissionHelper: PermissionCapabilityHelper
) {
    private var lastKnownTorchState = false

    fun readState(): ControlCenterState {
        val connectivity = readWifiStatus()
        val (btEnabled, btPermMissing) = readBluetoothStatus()
        val (batteryPct, charging) = readBatteryStatus()
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val mediaVol = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 5
        val mediaMax = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val ringVol = audio?.getStreamVolume(AudioManager.STREAM_RING) ?: 5
        val ringMax = audio?.getStreamMaxVolume(AudioManager.STREAM_RING)?.coerceAtLeast(1) ?: 7
        val brightness = readSystemBrightnessFraction()

        return ControlCenterState(
            wifiEnabled = connectivity.first,
            wifiLabel = connectivity.second,
            bluetoothEnabled = btEnabled,
            bluetoothPermissionMissing = btPermMissing,
            torchEnabled = lastKnownTorchState,
            torchAvailable = permissionHelper.hasFlashUnit(),
            brightnessFraction = brightness,
            canWriteSystemSettings = permissionHelper.canWriteSystemSettings(),
            mediaVolume = mediaVol,
            mediaMaxVolume = mediaMax,
            ringVolume = ringVol,
            ringMaxVolume = ringMax,
            batteryPercent = batteryPct,
            isCharging = charging,
            isDefaultLauncher = permissionHelper.isDefaultLauncher()
        )
    }

    fun toggleTorch(): Result<Boolean> {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return Result.failure(IllegalStateException("CameraManager tidak tersedia"))
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(IllegalStateException("Perangkat tidak memiliki lampu kilat"))

            val next = !lastKnownTorchState
            cm.setTorchMode(cameraId, next)
            lastKnownTorchState = next
            Result.success(next)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setMediaVolume(level: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(0, max), 0)
    }

    fun setRingVolume(level: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        runCatching {
            audio.setStreamVolume(AudioManager.STREAM_RING, level.coerceIn(0, max), 0)
        }
    }

    fun applyBrightnessFraction(fraction: Float): Boolean {
        val clamped = fraction.coerceIn(0.05f, 1.0f)
        return if (permissionHelper.canWriteSystemSettings()) {
            runCatching {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (clamped * 255f).toInt().coerceIn(10, 255)
                )
                true
            }.getOrDefault(false)
        } else {
            false
        }
    }

    fun openWifiPanelOrSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openDisplaySettings() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun readWifiStatus(): Pair<Boolean, String> {
        return try {
            val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiOn = wifiMgr?.isWifiEnabled ?: false
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val onCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            when {
                onWifi -> true to "Wi-Fi Aktif"
                wifiOn -> true to "Wi-Fi Siaga"
                onCell -> false to "Data Seluler"
                else -> false to "Nonaktif"
            }
        } catch (_: Exception) {
            false to "Tidak Diketahui"
        }
    }

    private fun readBluetoothStatus(): Pair<Boolean, Boolean> {
        if (!permissionHelper.hasBluetoothConnectPermission()) {
            return false to true
        }
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val enabled = bm?.adapter?.isEnabled == true
            enabled to false
        } catch (_: SecurityException) {
            false to true
        } catch (_: Exception) {
            false to false
        }
    }

    private fun readBatteryStatus(): Pair<Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 100
            pct to charging
        } catch (_: Exception) {
            100 to false
        }
    }

    private fun readSystemBrightnessFraction(): Float {
        return try {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 165)
            (raw / 255f).coerceIn(0.05f, 1.0f)
        } catch (_: Exception) {
            0.65f
        }
    }
}
