package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class DwellRadiusTest {
    @Test
    fun normalizesTinyRadiusToReliableMinimum() {
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
    }

    @Test
    fun approachRadiusCreatesLargerWakeRing() {
        assertEquals(DwellRadius.APPROACH_MIN_METERS, DwellRadius.approachRadius(100f), 0f)
        assertEquals(600f, DwellRadius.approachRadius(200f), 0f)
        assertEquals(DwellRadius.APPROACH_MAX_METERS, DwellRadius.approachRadius(500f), 0f)
    }
}
