package dev.andikune.launcherhpio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HpioUnitTest {

    @Test
    fun packageIdentity_matchesOwnerSpecification() {
        assertEquals("dev.andikune.launcherhpio", BuildConfig.APPLICATION_ID)
        assertFalse(BuildConfig.APPLICATION_ID.contains("example"))
        assertTrue(BuildConfig.VERSION_CODE > 0)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
    }
}
