package work.shreyaan.dwell

import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryReliabilityTest {
    @Test
    fun detectsKnownAggressiveManufacturers() {
        assertTrue(BatteryReliability.isKnownAggressiveManufacturer("Samsung"))
        assertTrue(BatteryReliability.isKnownAggressiveManufacturer("Xiaomi"))
        assertTrue(BatteryReliability.isKnownAggressiveManufacturer("OnePlus"))
        assertTrue(BatteryReliability.isKnownAggressiveManufacturer("realme"))
    }

    @Test
    fun doesNotFlagPixelManufacturerAsAggressive() {
        assertFalse(BatteryReliability.isKnownAggressiveManufacturer("Google"))
    }

    @Test
    fun settingsFlowDoesNotUseDirectBatteryOptimizationExemptionRequest() {
        assertFalse(
            BatteryReliability.settingsActionOrder()
                .contains(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        )
        assertTrue(
            BatteryReliability.settingsActionOrder()
                .contains(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        )
        assertTrue(
            BatteryReliability.settingsActionOrder()
                .contains(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        )
    }
}
