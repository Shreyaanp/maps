package work.shreyaan.dwell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSyncTest {
    @Test
    fun watchSetupNeededWhenDisplayedArmedPlaceIsNotRegistered() {
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertTrue(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = setOf("gym"),
            )
        )
    }

    @Test
    fun watchSetupNeededWhenNoArmedPlacesAreRegistered() {
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertTrue(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = emptySet(),
            )
        )
    }

    @Test
    fun watchSetupNotNeededWhenDisplayedArmedPlaceIsRegistered() {
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertFalse(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = setOf("office"),
            )
        )
    }

    @Test
    fun watchSetupNotNeededWhenNothingIsArmed() {
        assertFalse(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = testPlace("office", monitoringEnabled = false),
                armedPlaces = emptyList(),
                registeredPlaceIds = emptySet(),
            )
        )
    }

    private fun testPlace(
        id: String,
        monitoringEnabled: Boolean,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = id,
            latitude = 17.0,
            longitude = 78.0,
            radiusMeters = DwellRadius.DEFAULT_METERS,
            durationMinutes = 270,
            monitoringEnabled = monitoringEnabled,
            autoStart = true,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
