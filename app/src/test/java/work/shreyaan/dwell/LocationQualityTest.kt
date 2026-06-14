package work.shreyaan.dwell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQualityTest {
    @Test
    fun acceptsFreshAccurateRealLocation() {
        assertTrue(
            LocationQuality.isUsable(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 5_000L,
                accuracyMeters = 24f,
                isMock = false,
            )
        )
    }

    @Test
    fun rejectsMockLocation() {
        assertFalse(
            LocationQuality.isUsable(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 5_000L,
                accuracyMeters = 24f,
                isMock = true,
            )
        )
    }

    @Test
    fun acceptsMockLocationOnlyWhenExplicitlyAllowed() {
        assertTrue(
            LocationQuality.isUsable(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 5_000L,
                accuracyMeters = 24f,
                isMock = true,
                allowMock = true,
            )
        )
        assertTrue(
            LocationQuality.isImmediate(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 5_000L,
                accuracyMeters = 24f,
                isMock = true,
                allowMock = true,
            )
        )
    }

    @Test
    fun rejectsStaleLocation() {
        assertFalse(
            LocationQuality.isUsable(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 120_000L,
                accuracyMeters = 24f,
                isMock = false,
            )
        )
    }

    @Test
    fun rejectsBadAccuracy() {
        assertFalse(
            LocationQuality.isUsable(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 5_000L,
                accuracyMeters = 500f,
                isMock = false,
            )
        )
    }

    @Test
    fun rejectsInvalidCoordinates() {
        assertFalse(
            LocationQuality.isUsable(
                latitude = 118.5204,
                longitude = 73.8567,
                ageMs = 5_000L,
                accuracyMeters = 24f,
                isMock = false,
            )
        )
        assertFalse(
            LocationQuality.isUsable(
                latitude = 18.5204,
                longitude = 273.8567,
                ageMs = 5_000L,
                accuracyMeters = 24f,
                isMock = false,
            )
        )
    }

    @Test
    fun immediateLocationRequiresFreshAndAccurate() {
        assertTrue(
            LocationQuality.isImmediate(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 10_000L,
                accuracyMeters = 50f,
                isMock = false,
            )
        )
        assertFalse(
            LocationQuality.isImmediate(
                latitude = 18.5204,
                longitude = 73.8567,
                ageMs = 45_000L,
                accuracyMeters = 50f,
                isMock = false,
            )
        )
    }
}
