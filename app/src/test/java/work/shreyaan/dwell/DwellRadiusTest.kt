package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class DwellRadiusTest {
    @Test
    fun normalizesBelowFiftyMetersToMinimum() {
        assertEquals(DwellRadius.MIN_METERS, DwellRadius.normalize(10f), 0f)
    }

    @Test
    fun keepsFiftyMeterRadius() {
        assertEquals(DwellRadius.MIN_METERS, DwellRadius.normalize(50f), 0f)
    }

    @Test
    fun keepsConfiguredRadiusWithinReliableRange() {
        assertEquals(150f, DwellRadius.normalize(150f), 0f)
    }

    @Test
    fun normalizesHugeOrInvalidRadius() {
        assertEquals(DwellRadius.MAX_METERS, DwellRadius.normalize(5_000f), 0f)
        assertEquals(DwellRadius.DEFAULT_METERS, DwellRadius.normalize(Float.NaN), 0f)
        assertEquals(50f, DwellRadius.DEFAULT_METERS, 0f)
    }

    @Test
    fun approachRadiusCreatesLargerWakeRing() {
        assertEquals(DwellRadius.APPROACH_MIN_METERS, DwellRadius.approachRadius(100f), 0f)
        assertEquals(600f, DwellRadius.approachRadius(200f), 0f)
        assertEquals(DwellRadius.APPROACH_MAX_METERS, DwellRadius.approachRadius(500f), 0f)
    }

    @Test
    fun radiusControlExplainsLiveMonitoringLimitsBeforeDrag() {
        val unrestricted = radiusControlState(
            radiusMeters = 120f,
            monitoredRadiusLimitMeters = null,
        )
        assertEquals(120f, unrestricted.valueMeters, 0f)
        assertEquals(DwellRadius.MAX_METERS, unrestricted.maxMeters, 0f)
        assertEquals(true, unrestricted.sliderEnabled)
        assertEquals(null, unrestricted.helperText)

        val monitored = radiusControlState(
            radiusMeters = 120f,
            monitoredRadiusLimitMeters = 150f,
        )
        assertEquals(120f, monitored.valueMeters, 0f)
        assertEquals(150f, monitored.maxMeters, 0f)
        assertEquals(true, monitored.sliderEnabled)
        assertEquals(
            "Monitoring is live. You can tighten radius; pause to increase above 150 m.",
            monitored.helperText,
        )

        val minimum = radiusControlState(
            radiusMeters = 50f,
            monitoredRadiusLimitMeters = 50f,
        )
        assertEquals(50f, minimum.valueMeters, 0f)
        assertEquals(false, minimum.sliderEnabled)
        assertEquals(
            "Monitoring is live at the 50 m minimum. Pause monitoring to increase radius.",
            minimum.helperText,
        )
    }

    @Test
    fun radiusPresetOptionsRespectCurrentRadiusAndMonitoringLimit() {
        assertEquals(
            listOf(
                RadiusPresetOption(50f, "50 m", selected = true, enabled = true),
                RadiusPresetOption(100f, "100 m", selected = false, enabled = true),
                RadiusPresetOption(150f, "150 m", selected = false, enabled = true),
                RadiusPresetOption(250f, "250 m", selected = false, enabled = true),
            ),
            radiusPresetOptions(
                radiusMeters = 50f,
                maxMeters = DwellRadius.MAX_METERS,
                controlsEnabled = true,
            ),
        )

        assertEquals(
            listOf(
                RadiusPresetOption(50f, "50 m", selected = false, enabled = true),
                RadiusPresetOption(100f, "100 m", selected = true, enabled = true),
                RadiusPresetOption(150f, "150 m", selected = false, enabled = false),
                RadiusPresetOption(250f, "250 m", selected = false, enabled = false),
            ),
            radiusPresetOptions(
                radiusMeters = 100f,
                maxMeters = 100f,
                controlsEnabled = true,
            ),
        )

        assertEquals(
            listOf(
                RadiusPresetOption(50f, "50 m", selected = true, enabled = false),
                RadiusPresetOption(100f, "100 m", selected = false, enabled = false),
                RadiusPresetOption(150f, "150 m", selected = false, enabled = false),
                RadiusPresetOption(250f, "250 m", selected = false, enabled = false),
            ),
            radiusPresetOptions(
                radiusMeters = 50f,
                maxMeters = DwellRadius.MAX_METERS,
                controlsEnabled = false,
            ),
        )
    }
}
