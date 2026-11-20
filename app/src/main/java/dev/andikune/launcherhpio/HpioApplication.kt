package dev.andikune.launcherhpio

import android.app.Application
import dev.andikune.launcherhpio.data.crash.CrashRecoveryHelper
import dev.andikune.launcherhpio.data.storage.AppStorageHelper

class HpioApplication : Application() {

    lateinit var storageHelper: AppStorageHelper
        private set

    lateinit var crashRecoveryHelper: CrashRecoveryHelper
        private set

    override fun onCreate() {
        super.onCreate()
        storageHelper = AppStorageHelper(this).also { it.ensureStructure() }
        crashRecoveryHelper = CrashRecoveryHelper(this, storageHelper).also {
            it.installUncaughtExceptionHandler()
        }
    }
}
