package dev.andikune.launcherhpio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HpioInstrumentedTest {
    @Test
    fun useAppContext_hasExactPackageIdentity() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("dev.andikune.launcherhpio", appContext.packageName)
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }
}
