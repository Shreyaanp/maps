package work.shreyaan.dwell

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceReceiverTest {
    @Test
    fun geofenceErrorMessagesUsePlayServicesStatusNames() {
        val message = GeofenceReceiver.geofenceEventErrorMessage(
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE,
        )
        val detail = GeofenceReceiver.geofenceEventErrorDetail(
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE,
        )

        assertEquals("Monitoring event error: location services unavailable", message)
        assertTrue(detail.contains(GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE.toString()))
        assertTrue(detail.contains("GEOFENCE_NOT_AVAILABLE"))
    }

    @Test
    fun geofenceErrorMessagesHandleUnknownStatusCodes() {
        val message = GeofenceReceiver.geofenceEventErrorMessage(999_999)

        assertEquals("Monitoring event error: code 999999", message)
    }

    @Test
    fun fallbackEnterInsideZoneInfersNearestZoneRequest() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)
        val gym = testPlace("gym", latitude = 17.0030, longitude = 78.0000)

        assertEquals(
            DwellGeofenceRequest("office", DwellGeofenceType.ZONE),
            GeofenceReceiver.inferredRequestForLocation(
                places = listOf(gym, office),
                latitude = 17.0004,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            ),
        )
    }

    @Test
    fun fallbackEnterInsideOverlappingZonesInfersEveryContainingZone() {
        val office = testPlace(
            "office",
            latitude = 17.0000,
            longitude = 78.0000,
            radiusMeters = 150f,
        )
        val gym = testPlace(
            "gym",
            latitude = 17.0003,
            longitude = 78.0000,
            radiusMeters = 150f,
        )

        val requests = GeofenceReceiver.inferredRequestsForLocation(
            places = listOf(office, gym),
            latitude = 17.0003,
            longitude = 78.0000,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )

        assertEquals(
            listOf(
                DwellGeofenceRequest("gym", DwellGeofenceType.ZONE),
                DwellGeofenceRequest("office", DwellGeofenceType.ZONE),
            ),
            requests,
        )
        assertNull(
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = requests.mapNotNull { request ->
                    listOf(office, gym).firstOrNull { it.id == request.placeId }
                },
                currentPlaceId = "office",
            )
        )
    }

    @Test
    fun fallbackEnterInsideApproachRingInfersApproachRequest() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000, radiusMeters = 150f)

        assertEquals(
            DwellGeofenceRequest("office", DwellGeofenceType.APPROACH),
            GeofenceReceiver.inferredRequestForLocation(
                places = listOf(office),
                latitude = 17.0022,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            ),
        )
    }

    @Test
    fun fallbackEnterInsideApproachRingInfersEveryContainingApproachRing() {
        val office = testPlace(
            "office",
            latitude = 17.0000,
            longitude = 78.0000,
            radiusMeters = 100f,
        )
        val gym = testPlace(
            "gym",
            latitude = 17.0030,
            longitude = 78.0000,
            radiusMeters = 100f,
        )

        assertEquals(
            listOf(
                DwellGeofenceRequest("gym", DwellGeofenceType.APPROACH),
                DwellGeofenceRequest("office", DwellGeofenceType.APPROACH),
            ),
            GeofenceReceiver.inferredRequestsForLocation(
                places = listOf(office, gym),
                latitude = 17.0015,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            ),
        )
    }

    @Test
    fun fallbackEnterOutsideApproachRingDoesNotGuessActivePlace() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000, radiusMeters = 150f)

        assertNull(
            GeofenceReceiver.inferredRequestForLocation(
                places = listOf(office),
                latitude = 17.0200,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            )
        )
    }

    @Test
    fun fallbackEnterWithoutUsableLocationDoesNotGuess() {
        val office = testPlace("office")

        assertNull(
            GeofenceReceiver.inferredRequestForLocation(
                places = listOf(office),
                latitude = Double.NaN,
                longitude = 78.0,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            )
        )
    }

    @Test
    fun fallbackExitNearPlaceInfersZoneExitRequest() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000, radiusMeters = 150f)

        assertEquals(
            DwellGeofenceRequest("office", DwellGeofenceType.ZONE),
            GeofenceReceiver.inferredRequestForLocation(
                places = listOf(office),
                latitude = 17.0022,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_EXIT,
            ),
        )
    }

    @Test
    fun fallbackExitNearMultiplePlacesInfersEveryZoneExitCandidate() {
        val office = testPlace(
            "office",
            latitude = 17.0000,
            longitude = 78.0000,
            radiusMeters = 100f,
        )
        val gym = testPlace(
            "gym",
            latitude = 17.0008,
            longitude = 78.0000,
            radiusMeters = 100f,
        )

        assertEquals(
            listOf(
                DwellGeofenceRequest("gym", DwellGeofenceType.ZONE),
                DwellGeofenceRequest("office", DwellGeofenceType.ZONE),
            ),
            GeofenceReceiver.inferredRequestsForLocation(
                places = listOf(office, gym),
                latitude = 17.0008,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_EXIT,
            ),
        )
    }

    @Test
    fun fallbackIgnoresUnmonitoredPlaces() {
        val office = testPlace("office", monitoringEnabled = false)

        assertNull(
            GeofenceReceiver.inferredRequestForLocation(
                places = listOf(office),
                latitude = 17.0004,
                longitude = 78.0000,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            )
        )
    }

    @Test
    fun triggeredPlacePriorityUsesNearestLocationInsteadOfEventOrder() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)
        val gym = testPlace("gym", latitude = 17.0030, longitude = 78.0000)

        val forward = GeofenceReceiver.prioritizeTriggeredPlaces(
            places = listOf(gym, office),
            latitude = 17.0002,
            longitude = 78.0000,
        )
        val reversed = GeofenceReceiver.prioritizeTriggeredPlaces(
            places = listOf(office, gym),
            latitude = 17.0002,
            longitude = 78.0000,
        )

        assertEquals(listOf("office", "gym"), forward.map { it.id })
        assertEquals(listOf("office", "gym"), reversed.map { it.id })
    }

    @Test
    fun triggeredPlacePriorityIsStableWithoutUsableLocation() {
        val zulu = testPlace("zulu", createdAtMillis = 20L)
        val alpha = testPlace("alpha", createdAtMillis = 10L)

        assertEquals(
            listOf("alpha", "zulu"),
            GeofenceReceiver.prioritizeTriggeredPlaces(
                places = listOf(zulu, alpha),
                latitude = null,
                longitude = null,
            ).map { it.id },
        )
    }

    @Test
    fun triggeredPlacePriorityIgnoresPausedPlacesFromStaleOsEvents() {
        val pausedNearPlace = testPlace(
            "paused",
            latitude = 17.0000,
            longitude = 78.0000,
            monitoringEnabled = false,
        )
        val monitoredFarPlace = testPlace(
            "monitored",
            latitude = 17.0030,
            longitude = 78.0000,
            monitoringEnabled = true,
        )

        assertEquals(
            listOf("monitored"),
            GeofenceReceiver.prioritizeTriggeredPlaces(
                places = listOf(pausedNearPlace, monitoredFarPlace),
                latitude = 17.0001,
                longitude = 78.0000,
            ).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            GeofenceReceiver.prioritizeTriggeredPlaces(
                places = listOf(pausedNearPlace),
                latitude = 17.0001,
                longitude = 78.0000,
            ).map { it.id },
        )
    }

    @Test
    fun switchPromptTargetIgnoresOverlapWhenCurrentTimerPlaceAlsoTriggered() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)
        val gym = testPlace("gym", latitude = 17.0002, longitude = 78.0000)

        assertNull(
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = listOf(gym, office),
                currentPlaceId = "office",
            )
        )
    }

    @Test
    fun switchPromptTargetUsesOtherPlaceOnlyWhenTimerPlaceIsNotTriggered() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)
        val gym = testPlace("gym", latitude = 17.0002, longitude = 78.0000)

        assertEquals(
            "gym",
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = listOf(gym),
                currentPlaceId = "office",
            )?.id,
        )
        assertNull(
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = listOf(gym),
                currentPlaceId = "",
            )
        )
    }

    @Test
    fun switchPromptTargetDoesNotUsePausedStaleTrigger() {
        val pausedGym = testPlace(
            "gym",
            latitude = 17.0002,
            longitude = 78.0000,
            monitoringEnabled = false,
        )

        val zonePlaces = GeofenceReceiver.prioritizeTriggeredPlaces(
            places = listOf(pausedGym),
            latitude = 17.0002,
            longitude = 78.0000,
        )

        assertNull(
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = zonePlaces,
                currentPlaceId = "office",
            )
        )
    }

    @Test
    fun exitProbePlacesRequireAPlaceScopedTimer() {
        val office = testPlace("office")
        val gym = testPlace("gym")

        assertEquals(
            listOf("office"),
            GeofenceReceiver.exitProbePlacesForTimer(
                zonePlaces = listOf(office, gym),
                timerPlaceId = "office",
            ).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            GeofenceReceiver.exitProbePlacesForTimer(
                zonePlaces = listOf(office, gym),
                timerPlaceId = "",
            ).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            GeofenceReceiver.exitProbePlacesForTimer(
                zonePlaces = listOf(office, gym),
                timerPlaceId = "library",
            ).map { it.id },
        )
    }

    private fun testPlace(
        id: String,
        latitude: Double = 17.0,
        longitude: Double = 78.0,
        radiusMeters: Float = DwellRadius.DEFAULT_METERS,
        monitoringEnabled: Boolean = true,
        createdAtMillis: Long = 1L,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = id,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            durationMinutes = 270,
            monitoringEnabled = monitoringEnabled,
            autoStart = true,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = createdAtMillis,
        )
}
