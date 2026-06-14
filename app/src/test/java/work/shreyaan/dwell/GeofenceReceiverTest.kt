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

        assertTrue(message.contains("GEOFENCE_NOT_AVAILABLE"))
        assertTrue(detail.contains(GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE.toString()))
        assertTrue(detail.contains("GEOFENCE_NOT_AVAILABLE"))
    }

    @Test
    fun geofenceErrorMessagesHandleUnknownStatusCodes() {
        val message = GeofenceReceiver.geofenceEventErrorMessage(999_999)

        assertTrue(message.contains("999999"))
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

    private fun testPlace(
        id: String,
        latitude: Double = 17.0,
        longitude: Double = 78.0,
        radiusMeters: Float = DwellRadius.DEFAULT_METERS,
        monitoringEnabled: Boolean = true,
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
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
