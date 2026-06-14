package work.shreyaan.dwell

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellPlaceTest {
    @Test
    fun normalizesRadiusAndDuration() {
        val place = DwellPlace.create(
            label = "",
            latitude = 17.47,
            longitude = 78.36,
            radiusMeters = 10f,
            durationMinutes = 10_000,
        )

        assertEquals("Saved place", place.safeLabel)
        assertEquals(DwellRadius.MIN_METERS, place.radiusMeters, 0f)
        assertEquals(DwellPlace.MAX_DURATION_MINUTES, place.durationMinutes)
    }

    @Test
    fun updatesTimerDefaultsWithoutLosingPlaceIdentity() {
        val place = DwellPlace.create(
            label = "Office",
            latitude = 17.4793,
            longitude = 78.3686,
            radiusMeters = 150f,
            durationMinutes = 270,
            monitoringEnabled = true,
        )

        val updated = place.withTimerDefaults(radiusMeters = 200f, durationMinutes = 120)

        assertEquals(place.id, updated.id)
        assertEquals("Office", updated.safeLabel)
        assertEquals(17.4793, updated.latitude, 0.0001)
        assertEquals(78.3686, updated.longitude, 0.0001)
        assertEquals(200f, updated.radiusMeters, 0f)
        assertEquals(120, updated.durationMinutes)
        assertTrue(updated.monitoringEnabled)
    }

    @Test
    fun updatesAutoStartWithoutLosingPlaceIdentity() {
        val place = DwellPlace.create(
            label = "Office",
            latitude = 17.4793,
            longitude = 78.3686,
            radiusMeters = 150f,
            durationMinutes = 270,
            monitoringEnabled = true,
            autoStart = true,
            now = 1L,
        )

        val updated = place.withAutoStart(false, now = 2L)

        assertEquals(place.id, updated.id)
        assertEquals("Office", updated.safeLabel)
        assertEquals(17.4793, updated.latitude, 0.0001)
        assertEquals(78.3686, updated.longitude, 0.0001)
        assertEquals(150f, updated.radiusMeters, 0f)
        assertEquals(270, updated.durationMinutes)
        assertTrue(updated.monitoringEnabled)
        assertEquals(false, updated.autoStart)
        assertEquals(2L, updated.updatedAtMillis)
    }

    @Test
    fun geofenceRequestIdMapsBackToPlaceId() {
        val id = "abc-123"

        assertEquals(id, DwellPlace.idFromRequestId(DwellPlace.requestId(id)))
    }

    @Test
    fun fromJsonRejectsOutOfRangeCoordinates() {
        assertNull(
            DwellPlace.fromJson(
                JSONObject()
                    .put("id", "bad-lat")
                    .put("latitude", 118.5204)
                    .put("longitude", 73.8567),
            )
        )
        assertNull(
            DwellPlace.fromJson(
                JSONObject()
                    .put("id", "bad-lon")
                    .put("latitude", 18.5204)
                    .put("longitude", 273.8567),
            )
        )
    }

    @Test
    fun fromJsonAcceptsValidCoordinates() {
        val place = DwellPlace.fromJson(
            JSONObject()
                .put("id", "office")
                .put("label", "Office")
                .put("latitude", 18.5204)
                .put("longitude", 73.8567)
                .put("radiusMeters", 150.0)
                .put("durationMinutes", 270),
        )

        assertEquals("office", place?.id)
        assertEquals("Office", place?.safeLabel)
    }

    @Test
    fun fromJsonTrimsPlaceId() {
        val place = DwellPlace.fromJson(
            JSONObject()
                .put("id", "  office  ")
                .put("label", "Office")
                .put("latitude", 18.5204)
                .put("longitude", 73.8567),
        )

        assertEquals("office", place?.id)
    }

    @Test
    fun fromJsonRejectsPlaceIdsThatWouldExceedGeofenceRequestLimit() {
        val maxValidId = "x".repeat(85)
        val tooLongId = "x".repeat(86)

        assertEquals(
            maxValidId,
            DwellPlace.fromJson(
                JSONObject()
                    .put("id", maxValidId)
                    .put("latitude", 18.5204)
                    .put("longitude", 73.8567),
            )?.id,
        )
        assertNull(
            DwellPlace.fromJson(
                JSONObject()
                    .put("id", tooLongId)
                    .put("latitude", 18.5204)
                    .put("longitude", 73.8567),
            )
        )
    }

    @Test
    fun geofenceRequestIdIncludesZoneAndApproachTypes() {
        val id = "abc-123"

        assertEquals(
            DwellGeofenceRequest(id, DwellGeofenceType.ZONE),
            DwellPlace.requestFromRequestId(DwellPlace.zoneRequestId(id)),
        )
        assertEquals(
            DwellGeofenceRequest(id, DwellGeofenceType.APPROACH),
            DwellPlace.requestFromRequestId(DwellPlace.approachRequestId(id)),
        )
        assertEquals(id, DwellPlace.idFromRequestId(DwellPlace.approachRequestId(id)))
        assertNull(DwellPlace.requestFromRequestId("other_$id"))
    }

    @Test
    fun geofenceRequestIdPreservesPlaceIdCase() {
        val id = "Office-ABC-123"

        assertEquals(
            DwellGeofenceRequest(id, DwellGeofenceType.ZONE),
            DwellPlace.requestFromRequestId(DwellPlace.zoneRequestId(id)),
        )
        assertEquals(
            DwellGeofenceRequest(id, DwellGeofenceType.APPROACH),
            DwellPlace.requestFromRequestId(DwellPlace.approachRequestId(id)),
        )
    }

    @Test
    fun normalizePlacesPreservesSavedPlacesButCapsMonitoredPlaces() {
        val places = (0 until DwellPlace.MAX_MONITORED_PLACES + 2).map { index ->
            DwellPlace(
                id = "place-$index",
                label = "Place $index",
                latitude = 17.0,
                longitude = 78.0,
                radiusMeters = 150f,
                durationMinutes = 270,
                monitoringEnabled = true,
                autoStart = true,
                createdAtMillis = 1L,
                updatedAtMillis = 1L,
            )
        }

        val normalized = DwellPlace.normalizePlaces(places)

        assertEquals(DwellPlace.MAX_MONITORED_PLACES + 2, normalized.size)
        assertEquals(
            DwellPlace.MAX_MONITORED_PLACES,
            normalized.count { it.monitoringEnabled },
        )
        assertEquals(
            listOf("place-20", "place-21"),
            normalized.filterNot { it.monitoringEnabled }.map { it.id },
        )
    }

    @Test
    fun distanceMetersUsesPlaceCenter() {
        val place = DwellPlace.create(
            label = "Office",
            latitude = 17.0,
            longitude = 78.0,
            radiusMeters = 150f,
            durationMinutes = 270,
        )

        assertTrue(place.distanceMetersTo(17.001, 78.0) in 100f..125f)
    }
}
