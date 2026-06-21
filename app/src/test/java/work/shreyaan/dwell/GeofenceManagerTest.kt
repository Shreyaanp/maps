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
                monitoringError = "Monitoring setup failed",
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
    fun appOpenReportsSetupNeededBeforeHealthySkipWhenArmed() {
        assertTrue(
            GeofenceManager.shouldReportSetupNeededOnAppOpen(
                armedPlaceIds = listOf("office"),
                hasSetupIssue = true,
            )
        )
        assertFalse(
            GeofenceManager.shouldReportSetupNeededOnAppOpen(
                armedPlaceIds = emptyList(),
                hasSetupIssue = true,
            )
        )
        assertFalse(
            GeofenceManager.shouldReportSetupNeededOnAppOpen(
                armedPlaceIds = listOf("office"),
                hasSetupIssue = false,
            )
        )
    }

    @Test
    fun pausedPlaceClearsOnlyItsMonitoringPrompt() {
        assertTrue(
            GeofenceManager.shouldClearMonitoringPromptForPausedPlace(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                pausedPlaceId = "office",
            )
        )
        assertTrue(
            GeofenceManager.shouldClearMonitoringPromptForPausedPlace(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "office",
                pausedPlaceId = "office",
            )
        )
        assertFalse(
            GeofenceManager.shouldClearMonitoringPromptForPausedPlace(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                pausedPlaceId = "office",
            )
        )
    }

    @Test
    fun allPlacesPausedClearsMonitoringPromptsButNotTimeUp() {
        assertTrue(
            GeofenceManager.shouldClearMonitoringPromptWhenAllPlacesPaused(
                Prefs.WATCH_PROMPT_START_TIMER,
            )
        )
        assertTrue(
            GeofenceManager.shouldClearMonitoringPromptWhenAllPlacesPaused(
                Prefs.WATCH_PROMPT_LEAVE_EARLY,
            )
        )
        assertFalse(
            GeofenceManager.shouldClearMonitoringPromptWhenAllPlacesPaused(
                Prefs.WATCH_PROMPT_TIME_UP,
            )
        )
        assertFalse(
            GeofenceManager.shouldClearMonitoringPromptWhenAllPlacesPaused(
                Prefs.WATCH_PROMPT_NONE,
            )
        )
    }

    @Test
    fun setupNotificationClearsOnlyAfterHealthyMonitoringRecovery() {
        assertTrue(
            GeofenceManager.shouldClearSetupNotificationAfterRefresh(
                ok = true,
                error = null,
            )
        )
        assertTrue(
            GeofenceManager.shouldClearSetupNotificationAfterRefresh(
                ok = true,
                error = "",
            )
        )
        assertFalse(
            GeofenceManager.shouldClearSetupNotificationAfterRefresh(
                ok = false,
                error = "Background location permission is needed",
            )
        )
        assertFalse(
            GeofenceManager.shouldClearSetupNotificationAfterRefresh(
                ok = true,
                error = "Saved place has invalid location",
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

    @Test
    fun monitoringUpdateExplainsPlaceLimitBeforeMutatingState() {
        val monitored = (0 until DwellPlace.MAX_MONITORED_PLACES).map { index ->
            testPlace("place-$index", latitude = 17.0 + (index * 0.001))
        }
        val paused = testPlace(
            id = "extra",
            latitude = 18.0,
            longitude = 79.0,
            monitoringEnabled = false,
        )

        assertEquals(
            "Dwell can monitor up to ${DwellPlace.MAX_MONITORED_PLACES} places. Pause another monitored place first.",
            GeofenceManager.placeMonitoringUpdateError(
                places = monitored + paused,
                placeId = paused.id,
                enabled = true,
            ),
        )
    }

    @Test
    fun monitoringUpdateAllowsExistingMonitoredPlaceOrDisableAtLimit() {
        val monitored = (0 until DwellPlace.MAX_MONITORED_PLACES).map { index ->
            testPlace("place-$index", latitude = 17.0 + (index * 0.001))
        }

        assertEquals(
            null,
            GeofenceManager.placeMonitoringUpdateError(
                places = monitored,
                placeId = "place-0",
                enabled = true,
            ),
        )
        assertEquals(
            null,
            GeofenceManager.placeMonitoringUpdateError(
                places = monitored,
                placeId = "place-0",
                enabled = false,
            ),
        )
    }

    @Test
    fun monitoringUpdateExplainsMissingPlace() {
        assertEquals(
            "Saved place no longer exists",
            GeofenceManager.placeMonitoringUpdateError(
                places = listOf(testPlace("office")),
                placeId = "gym",
                enabled = true,
            ),
        )
    }

    @Test
    fun armMonitoringUsesSameLimitPreflightAsPlaces() {
        val monitored = (0 until DwellPlace.MAX_MONITORED_PLACES).map { index ->
            testPlace("place-$index", latitude = 17.0 + (index * 0.001))
        }
        val paused = testPlace(
            id = "extra",
            latitude = 18.0,
            longitude = 79.0,
            monitoringEnabled = false,
        )
        val expected = "Dwell can monitor up to ${DwellPlace.MAX_MONITORED_PLACES} places. Pause another monitored place first."

        assertEquals(
            expected,
            GeofenceManager.armMonitoringUpdateError(
                places = monitored + paused,
                activePlaceId = paused.id,
            ),
        )
        assertEquals(
            expected,
            GeofenceManager.armMonitoringUpdateError(
                places = monitored,
                activePlaceId = null,
            ),
        )
    }

    @Test
    fun armMonitoringExplainsMissingExplicitPlace() {
        assertEquals(
            "Saved place no longer exists",
            GeofenceManager.armMonitoringUpdateError(
                places = listOf(testPlace("office")),
                activePlaceId = "gym",
            ),
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
