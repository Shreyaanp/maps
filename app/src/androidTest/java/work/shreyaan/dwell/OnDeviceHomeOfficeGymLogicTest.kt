package work.shreyaan.dwell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.location.Geofence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnDeviceHomeOfficeGymLogicTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearRunningTimerAndNotifications()
        clearDwellPrefs()
    }

    @After
    fun tearDown() {
        clearRunningTimerAndNotifications()
        clearDwellPrefs()
    }

    @Test
    fun nearExactDuplicateLocationDoesNotCreateFourthSavedPlaceOnDevice() {
        val story = createStoryPlaces()

        val duplicateOffice = Prefs.createPlace(
            context,
            label = "Work",
            lat = story.office.latitude + 0.00002,
            lon = story.office.longitude,
            radiusMeters = 300f,
            durationMinutes = 15,
            monitoringEnabled = false,
            autoStart = true,
        )

        val places = Prefs.getPlaces(context)
        val storedOffice = places.single { it.id == story.office.id }

        assertEquals(story.office.id, duplicateOffice.id)
        assertEquals(3, places.size)
        assertEquals(story.office.id, Prefs.getActivePlace(context)?.id)
        assertEquals("Office", storedOffice.safeLabel)
        assertEquals(90, storedOffice.durationMinutes)
        assertEquals(130f, storedOffice.radiusMeters, 0f)
        assertEquals(false, storedOffice.autoStart)
    }

    @Test
    fun nearbyDifferentLocationsRemainSeparateOnDevice() {
        val office = Prefs.createPlace(
            context,
            label = "Office",
            lat = 17.0000,
            lon = 78.0000,
            radiusMeters = 120f,
            durationMinutes = 90,
        )
        val gym = Prefs.createPlace(
            context,
            label = "Gym",
            lat = 17.0001,
            lon = 78.0000,
            radiusMeters = 120f,
            durationMinutes = 60,
        )

        assertEquals(listOf(office.id, gym.id), Prefs.getPlaces(context).map { it.id })
    }

    @Test
    fun editingOfficeSettingsDoesNotLeakIntoHomeGymOrNewPlaceDefaultsOnDevice() {
        val story = createStoryPlaces()

        Prefs.setDefaultRadius(context, 260f)
        Prefs.setDefaultDurationMinutes(context, 480)
        Prefs.setDefaultAutoStart(context, false)
        val editedOffice = Prefs.upsertPlace(
            context,
            Prefs.placeForUpdate(
                active = story.office,
                lat = story.office.latitude + 0.001,
                lon = story.office.longitude + 0.001,
                label = "Office gate",
                radiusMeters = 55f,
                durationMinutes = 135,
                autoStart = true,
                now = story.office.updatedAtMillis + 1L,
            ),
            makeActive = false,
        )

        val places = Prefs.getPlaces(context)

        assertEquals(story.home, places.single { it.id == story.home.id })
        assertEquals(story.gym, places.single { it.id == story.gym.id })
        assertEquals("Office gate", editedOffice.safeLabel)
        assertEquals(55f, editedOffice.radiusMeters, 0f)
        assertEquals(135, editedOffice.durationMinutes)
        assertTrue(editedOffice.autoStart)
        assertEquals(260f, Prefs.getDefaultRadius(context), 0f)
        assertEquals(480, Prefs.getDefaultDurationMinutes(context))
        assertFalse(Prefs.getDefaultAutoStart(context))
        assertEquals(story.home.id, Prefs.getActivePlace(context)?.id)
    }

    @Test
    fun homeOfficeGymMovementRolesStayCorrectInPersistedPhoneState() {
        val story = createStoryPlaces()
        val now = System.currentTimeMillis()

        val homeRequests = GeofenceReceiver.inferredRequestsForLocation(
            places = Prefs.getArmedPlaces(context),
            latitude = story.home.latitude,
            longitude = story.home.longitude,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )

        assertEquals(listOf(DwellGeofenceRequest(story.home.id, DwellGeofenceType.ZONE)), homeRequests)

        Prefs.setActivePlace(context, story.home.id)
        Prefs.setTimerPlaceId(context, story.office.id)
        Prefs.setTimerStartedAt(context, now - 30_000L)
        Prefs.setTimerEnd(context, now + 30 * 60_000L)
        Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_START_TIMER, story.gym.id)

        val gymRequests = GeofenceReceiver.inferredRequestsForLocation(
            places = Prefs.getArmedPlaces(context),
            latitude = story.gym.latitude,
            longitude = story.gym.longitude,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )
        val gymZonePlaces = gymRequests.mapNotNull { request -> Prefs.getPlace(context, request.placeId) }

        assertEquals(story.gym.id, Prefs.getWatchPlace(context)?.id)
        assertEquals(story.gym.id, gymZonePlaces.single().id)
        assertEquals(
            story.gym.id,
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = gymZonePlaces,
                currentPlaceId = story.office.id,
            )?.id,
        )
        assertEquals(
            "timer place should remain Office until the user accepts the switch",
            story.office.id,
            Prefs.getTimerPlaceId(context),
        )
    }

    @Test
    fun overlappingTriggeredPlacesDoNotSwitchWhenCurrentTimerPlaceIsStillInsideOnDevice() {
        val office = Prefs.createPlace(
            context,
            label = "Office",
            lat = 17.0100,
            lon = 78.0000,
            radiusMeters = 180f,
            durationMinutes = 90,
            monitoringEnabled = true,
        )
        val gym = Prefs.createPlace(
            context,
            label = "Gym",
            lat = 17.0107,
            lon = 78.0000,
            radiusMeters = 180f,
            durationMinutes = 60,
            monitoringEnabled = true,
        )

        val requests = GeofenceReceiver.inferredRequestsForLocation(
            places = Prefs.getArmedPlaces(context),
            latitude = gym.latitude,
            longitude = gym.longitude,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )
        val zonePlaces = requests.mapNotNull { request -> Prefs.getPlace(context, request.placeId) }

        assertEquals(listOf(gym.id, office.id), requests.map { it.placeId })
        assertNull(
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = zonePlaces,
                currentPlaceId = office.id,
            )
        )
    }

    @Test
    fun staleWatchStartCommandCannotClearCurrentPromptOnDevice() {
        val story = createStoryPlaces()
        val now = System.currentTimeMillis()
        val timerStartedAt = now - 60_000L
        val timerEnd = now + 60 * 60_000L

        Prefs.setTimerPlaceId(context, story.office.id)
        Prefs.setTimerStartedAt(context, timerStartedAt)
        Prefs.setTimerEnd(context, timerEnd)
        Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_START_TIMER, story.office.id)

        val promptUpdated = Prefs.getWatchPromptUpdated(context)
        val staleCommand = PhoneDataService.WatchPromptCommand(
            prompt = Prefs.WATCH_PROMPT_START_TIMER,
            promptUpdated = promptUpdated,
            promptPlaceId = story.office.id,
            timerPlaceId = story.home.id,
            timerStartedAt = timerStartedAt - 5_000L,
            timerEnd = timerEnd + 5_000L,
        )

        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.getWatchPrompt(context),
                promptPlaceId = Prefs.getPromptPlaceId(context),
                currentPromptUpdated = promptUpdated,
                command = staleCommand,
                timerRunning = TimerController.isRunning(context),
                timerPlaceId = Prefs.getTimerPlaceId(context),
                timerStartedAt = Prefs.getTimerStartedAt(context),
                timerEnd = Prefs.getTimerEnd(context),
                now = now,
            ),
        )
        assertEquals(Prefs.WATCH_PROMPT_START_TIMER, Prefs.getWatchPrompt(context))
        assertEquals(story.office.id, Prefs.getPromptPlaceId(context))
        assertEquals(story.office.id, Prefs.getTimerPlaceId(context))
    }

    @Test
    fun timerControllerStartsPromptedPlaceWithThatPlacesDurationOnDevice() {
        val story = createStoryPlaces()

        Prefs.setActivePlace(context, story.home.id)
        Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_START_TIMER, story.gym.id)

        TimerController.startTimer(
            context,
            durationMinutes = Prefs.getDurationMinutes(context, story.gym.id),
            placeId = null,
        )

        val timerDurationMs = Prefs.getTimerEnd(context) - Prefs.getTimerStartedAt(context)
        assertTrue(TimerController.isRunning(context))
        assertEquals(Prefs.WATCH_PROMPT_NONE, Prefs.getWatchPrompt(context))
        assertEquals(story.gym.id, Prefs.getTimerPlaceId(context))
        assertEquals(60 * 60_000L, timerDurationMs)
    }

    @Test
    fun monitoringPrerequisitesMatchRealDevicePermissionState() {
        val expected = MonitoringPrerequisites.issueFor(
            hasLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            hasBackgroundLocation = Build.VERSION.SDK_INT < 29 ||
                hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            hasNotifications = Build.VERSION.SDK_INT < 33 ||
                hasPermission(Manifest.permission.POST_NOTIFICATIONS),
            hasMotion = ActivityRecognitionManager.hasPermission(context),
        )

        assertEquals(expected, MonitoringPrerequisites.issueForContext(context))
    }

    private fun createStoryPlaces(): StoryPlaces {
        val home = Prefs.createPlace(
            context,
            label = "Home",
            lat = 17.0000,
            lon = 78.0000,
            radiusMeters = 120f,
            durationMinutes = 45,
            monitoringEnabled = true,
            autoStart = true,
        )
        val office = Prefs.createPlace(
            context,
            label = "Office",
            lat = 17.0100,
            lon = 78.0000,
            radiusMeters = 130f,
            durationMinutes = 90,
            monitoringEnabled = true,
            autoStart = false,
        )
        val gym = Prefs.createPlace(
            context,
            label = "Gym",
            lat = 17.0140,
            lon = 78.0000,
            radiusMeters = 140f,
            durationMinutes = 60,
            monitoringEnabled = true,
            autoStart = true,
        )
        Prefs.setActivePlace(context, home.id)
        assertNotNull(Prefs.getActivePlace(context))
        return StoryPlaces(home, office, gym)
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun clearDwellPrefs() {
        context.getSharedPreferences("dwell", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun clearRunningTimerAndNotifications() {
        runCatching { TimerController.cancelTimer(context) }
        runCatching { Notifications.clearAll(context) }
    }

    private data class StoryPlaces(
        val home: DwellPlace,
        val office: DwellPlace,
        val gym: DwellPlace,
    )
}
