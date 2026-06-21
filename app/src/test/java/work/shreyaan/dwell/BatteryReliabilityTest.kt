package work.shreyaan.dwell

import android.provider.Settings
import org.junit.Assert.assertEquals
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
    fun optimizedBatteryDetailNamesTheAppInfoPathToUnrestricted() {
        assertEquals(
            "Test may delay background arrival checks. Open app info, then Battery, and choose Unrestricted.",
            BatteryReliabilityStatus(
                manufacturer = "Test",
                isKnownAggressiveOem = true,
                isIgnoringOptimizations = false,
            ).detail,
        )
        assertEquals(
            "Android may delay background arrival checks while battery optimization is enabled. Open app info, then Battery, and choose Unrestricted.",
            BatteryReliabilityStatus(
                manufacturer = "Test",
                isKnownAggressiveOem = false,
                isIgnoringOptimizations = false,
            ).detail,
        )
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
