package work.shreyaan.dwell

import org.junit.Assert.assertEquals
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
    fun watchSetupNotNeededWhenAllArmedPlacesAreRegistered() {
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertFalse(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = setOf("office", "gym"),
            )
        )
    }

    @Test
    fun watchSetupNeededWhenAnyArmedPlaceIsMissingRegistration() {
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertTrue(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = setOf("office"),
            )
        )
    }

    @Test
    fun watchSetupNeededWhenPhoneSetupIssueExistsEvenIfRegisteredIdsLookHealthy() {
        val office = testPlace("office", monitoringEnabled = true)

        assertTrue(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office),
                registeredPlaceIds = setOf("office"),
                setupIssue = "Background location permission is needed",
            )
        )
    }

    @Test
    fun watchSetupNeededWhenMonitoringErrorExistsEvenIfRegisteredIdsLookHealthy() {
        val office = testPlace("office", monitoringEnabled = true)

        assertTrue(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(office),
                registeredPlaceIds = setOf("office"),
                monitoringError = "Monitoring event error: location services unavailable",
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
                monitoringError = "Monitoring event error: location services unavailable",
            )
        )
    }

    @Test
    fun watchDisplayPlaceUsesLiveMonitoredPlaceInsteadOfPausedActivePlace() {
        val office = testPlace("office", monitoringEnabled = false)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertEquals(
            "gym",
            WearSync.watchDisplayPlace(
                places = listOf(office, gym),
                activePlace = office,
                armedPlaces = listOf(gym),
                registeredPlaceIds = setOf("gym"),
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerEnd = 0L,
                timerPlaceId = "",
            )?.id,
        )
    }

    @Test
    fun watchDisplayPlaceKeepsPromptAndTimerPlaceAheadOfMonitoringSummary() {
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertEquals(
            "office",
            WearSync.watchDisplayPlace(
                places = listOf(office, gym),
                activePlace = gym,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = setOf("gym"),
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                timerEnd = 0L,
                timerPlaceId = "",
            )?.id,
        )
        assertEquals(
            "office",
            WearSync.watchDisplayPlace(
                places = listOf(office, gym),
                activePlace = gym,
                armedPlaces = listOf(office, gym),
                registeredPlaceIds = setOf("gym"),
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerEnd = 10_000L,
                timerPlaceId = "office",
            )?.id,
        )
    }

    @Test
    fun watchDisplayPlaceUsesTimerPlaceForLeaveAndTimeUpPrompts() {
        val home = testPlace("home", monitoringEnabled = true)
        val office = testPlace("office", monitoringEnabled = true)
        val gym = testPlace("gym", monitoringEnabled = true)
        val places = listOf(home, office, gym)

        assertEquals(
            "office",
            WearSync.watchDisplayPlace(
                places = places,
                activePlace = gym,
                armedPlaces = places,
                registeredPlaceIds = setOf("home", "office", "gym"),
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "home",
                timerEnd = 10_000L,
                timerPlaceId = "office",
            )?.id,
        )
        assertEquals(
            "office",
            WearSync.watchDisplayPlace(
                places = places,
                activePlace = gym,
                armedPlaces = places,
                registeredPlaceIds = setOf("home", "office", "gym"),
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptPlaceId = "home",
                timerEnd = 0L,
                timerPlaceId = "office",
            )?.id,
        )
    }

    @Test
    fun watchDisplayPlaceDoesNotUseCurrentTimerAsMissingStartPromptTarget() {
        val office = testPlace("office", monitoringEnabled = true)

        assertEquals(
            null,
            WearSync.watchDisplayPlace(
                places = listOf(office),
                activePlace = office,
                armedPlaces = listOf(office),
                registeredPlaceIds = setOf("office"),
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "deleted-gym",
                timerEnd = 10_000L,
                timerPlaceId = "office",
            ),
        )
    }

    @Test
    fun watchDisplayPlaceDoesNotBorrowActivePlaceForNoPlaceOrUnknownPlaceTimer() {
        val office = testPlace("office", monitoringEnabled = false)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertEquals(
            null,
            WearSync.watchDisplayPlace(
                places = listOf(office, gym),
                activePlace = office,
                armedPlaces = listOf(gym),
                registeredPlaceIds = setOf("gym"),
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerEnd = 10_000L,
                timerPlaceId = "",
            ),
        )
        assertEquals(
            null,
            WearSync.watchDisplayPlace(
                places = listOf(office, gym),
                activePlace = office,
                armedPlaces = listOf(gym),
                registeredPlaceIds = setOf("gym"),
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerEnd = 10_000L,
                timerPlaceId = "deleted",
            ),
        )
    }

    @Test
    fun watchDisplayPlaceUsesDeterministicLivePlaceWhenActivePlaceIsPaused() {
        val home = testPlace("home", monitoringEnabled = true)
        val office = testPlace("office", monitoringEnabled = false)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertEquals(
            "home",
            WearSync.watchDisplayPlace(
                places = listOf(office, home, gym),
                activePlace = office,
                armedPlaces = listOf(home, gym),
                registeredPlaceIds = setOf("gym", "home"),
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerEnd = 0L,
                timerPlaceId = "",
            )?.id,
        )
    }

    @Test
    fun watchSetupNeededCoversNullAndNonMonitoringDisplayedPlaces() {
        val office = testPlace("office", monitoringEnabled = false)
        val gym = testPlace("gym", monitoringEnabled = true)

        assertTrue(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = null,
                armedPlaces = listOf(gym),
                registeredPlaceIds = emptySet(),
            )
        )
        assertFalse(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = null,
                armedPlaces = listOf(gym),
                registeredPlaceIds = setOf("gym"),
            )
        )
        assertFalse(
            WearSync.shouldMarkWatchSetupNeeded(
                watchPlace = office,
                armedPlaces = listOf(gym),
                registeredPlaceIds = setOf("gym"),
                setupIssue = "",
            )
        )
    }

    @Test
    fun watchDefaultsDoNotBorrowActivePlaceWhenNoPlaceIsDisplayed() {
        assertEquals(30, WearSync.watchDurationMinutes(watchPlace = null, defaultDurationMinutes = 30))
        assertEquals(120f, WearSync.watchRadiusMeters(watchPlace = null, defaultRadiusMeters = 120f), 0f)
    }

    @Test
    fun watchDefaultsUseDisplayedPlaceSettingsWhenKnown() {
        val office = testPlace(
            id = "office",
            monitoringEnabled = true,
            radiusMeters = 180f,
            durationMinutes = 90,
        )

        assertEquals(90, WearSync.watchDurationMinutes(watchPlace = office, defaultDurationMinutes = 30))
        assertEquals(180f, WearSync.watchRadiusMeters(watchPlace = office, defaultRadiusMeters = 75f), 0f)
    }

    private fun testPlace(
        id: String,
        monitoringEnabled: Boolean,
        radiusMeters: Float = DwellRadius.DEFAULT_METERS,
        durationMinutes: Int = 270,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = id,
            latitude = 17.0,
            longitude = 78.0,
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            monitoringEnabled = monitoringEnabled,
            autoStart = true,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
