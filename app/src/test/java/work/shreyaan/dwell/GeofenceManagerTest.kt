package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceManagerTest {
    @Test
    fun appOpenRefreshSkipsWhenNothingIsArmed() {
        assertFalse(
            GeofenceManager.shouldRefreshOnAppOpen(
                armedPlaceIds = emptyList(),
                registeredPlaceIds = emptySet(),
                monitoringError = "",
                lastUpdatedMillis = 10_000L,
                nowMillis = 20_000L,
            )
        )
    }

    @Test
    fun appOpenRefreshRunsWhenRegisteredPlacesDoNotMatchArmedPlaces() {
        assertTrue(
            GeofenceManager.shouldRefreshOnAppOpen(
                armedPlaceIds = listOf("office", "gym"),
                registeredPlaceIds = setOf("office"),
                monitoringError = "",
                lastUpdatedMillis = 10_000L,
                nowMillis = 20_000L,
            )
        )
    }

    @Test
    fun appOpenRefreshRunsWhenMonitoringHasAnError() {
        assertTrue(
            GeofenceManager.shouldRefreshOnAppOpen(
                armedPlaceIds = listOf("office"),
                registeredPlaceIds = setOf("office"),
                monitoringError = "Geofence registration failed",
                lastUpdatedMillis = 10_000L,
                nowMillis = 20_000L,
            )
        )
    }

    @Test
    fun appOpenRefreshRunsPeriodicallyToSelfHealOsSideLoss() {
        assertTrue(
            GeofenceManager.shouldRefreshOnAppOpen(
                armedPlaceIds = listOf("office"),
                registeredPlaceIds = setOf("office"),
                monitoringError = "",
                lastUpdatedMillis = 10_000L,
                nowMillis = 50_000L,
                refreshIntervalMs = 30_000L,
            )
        )
    }

    @Test
    fun appOpenRefreshSkipsInsideHealthyRefreshWindow() {
        assertFalse(
            GeofenceManager.shouldRefreshOnAppOpen(
                armedPlaceIds = listOf("office"),
                registeredPlaceIds = setOf("office"),
                monitoringError = "",
                lastUpdatedMillis = 10_000L,
                nowMillis = 20_000L,
                refreshIntervalMs = 30_000L,
            )
        )
    }

    @Test
    fun registrablePlacesKeepOnlyEnabledPlacesWithValidCoordinatesAndIds() {
        val valid = testPlace("office")
        val disabled = testPlace("gym", monitoringEnabled = false)
        val invalidLat = testPlace("bad-lat", latitude = 118.0)
        val invalidLon = testPlace("bad-lon", longitude = 273.0)
        val invalidId = testPlace("x".repeat(86))

        assertEquals(
            listOf(valid),
            GeofenceManager.registrablePlaces(
                listOf(valid, disabled, invalidLat, invalidLon, invalidId),
            ),
        )
    }

    @Test
    fun registrablePlacesKeepsAllValidMonitoredPlaces() {
        val office = testPlace("office")
        val gym = testPlace("gym", latitude = 17.1, longitude = 78.1)

        assertEquals(
            listOf(office, gym),
            GeofenceManager.registrablePlaces(listOf(office, gym)),
        )
    }

    private fun testPlace(
        id: String,
        latitude: Double = 17.0,
        longitude: Double = 78.0,
        monitoringEnabled: Boolean = true,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = id,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = DwellRadius.DEFAULT_METERS,
            durationMinutes = 270,
            monitoringEnabled = monitoringEnabled,
            autoStart = true,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
