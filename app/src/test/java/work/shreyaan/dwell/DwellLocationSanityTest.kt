package work.shreyaan.dwell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellLocationSanityTest {
    @Test
    fun emulatorDefaultCoordinateIsSuspiciousOnPhysicalDevice() {
        assertTrue(
            DwellLocationSanity.isSuspiciousPhysicalEmulatorDefault(
                latitude = 37.4219983,
                longitude = -122.084,
                isLikelyAndroidEmulatorDevice = false,
            )
        )
    }

    @Test
    fun emulatorDefaultCoordinateIsAllowedOnEmulator() {
        assertFalse(
            DwellLocationSanity.isSuspiciousPhysicalEmulatorDefault(
                latitude = 37.4219983,
                longitude = -122.084,
                isLikelyAndroidEmulatorDevice = true,
            )
        )
    }

    @Test
    fun mockPolicyRejectsMocksByDefaultAndAllowsWhenRequested() {
        assertFalse(DwellLocationSanity.mockPolicyAllows(isMock = true, allowMock = false))
        assertTrue(DwellLocationSanity.mockPolicyAllows(isMock = true, allowMock = true))
        assertTrue(DwellLocationSanity.mockPolicyAllows(isMock = false, allowMock = false))
    }

    @Test
    fun realWorldIndiaCoordinateIsNotNearEmulatorDefault() {
        assertFalse(
            DwellLocationSanity.isNearAndroidEmulatorDefault(
                latitude = 18.5204,
                longitude = 73.8567,
            )
        )
    }

    @Test
    fun nearbyNorthShorelineCoordinateIsNearEmulatorDefault() {
        assertTrue(
            DwellLocationSanity.isNearAndroidEmulatorDefault(
                latitude = 37.4221,
                longitude = -122.0839,
            )
        )
    }

    @Test
    fun invalidCoordinatesAreNotTreatedAsEmulatorDefault() {
        assertFalse(
            DwellLocationSanity.isNearAndroidEmulatorDefault(
                latitude = 118.5204,
                longitude = 73.8567,
            )
        )
        assertFalse(
            DwellLocationSanity.isNearAndroidEmulatorDefault(
                latitude = 18.5204,
                longitude = 273.8567,
            )
        )
    }
}
