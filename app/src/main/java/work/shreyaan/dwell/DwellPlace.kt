package work.shreyaan.dwell

import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class DwellGeofenceType {
    ZONE,
    APPROACH,
}

data class DwellGeofenceRequest(
    val placeId: String,
    val type: DwellGeofenceType,
)

data class DwellPlace(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val durationMinutes: Int,
    val monitoringEnabled: Boolean,
    val autoStart: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val safeLabel: String
        get() = label.ifBlank { "Saved place" }

    fun normalized(): DwellPlace =
        copy(
            label = cleanDisplayLabel(label).take(MAX_LABEL_LENGTH),
            radiusMeters = DwellRadius.normalize(radiusMeters),
            durationMinutes = durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES),
        )

    fun withMonitoring(enabled: Boolean, now: Long = System.currentTimeMillis()): DwellPlace =
        copy(monitoringEnabled = enabled, updatedAtMillis = now).normalized()

    fun withTimerDefaults(
        radiusMeters: Float,
        durationMinutes: Int,
        now: Long = System.currentTimeMillis(),
    ): DwellPlace =
        copy(
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            updatedAtMillis = now,
        ).normalized()

    fun withAutoStart(enabled: Boolean, now: Long = System.currentTimeMillis()): DwellPlace =
        copy(autoStart = enabled, updatedAtMillis = now).normalized()

    fun distanceMetersTo(latitude: Double, longitude: Double): Float {
        val earthRadiusMeters = 6_371_008.8
        val lat1 = Math.toRadians(this.latitude)
        val lat2 = Math.toRadians(latitude)
        val deltaLat = Math.toRadians(latitude - this.latitude)
        val deltaLon = Math.toRadians(longitude - this.longitude)
        val a = sin(deltaLat / 2.0) * sin(deltaLat / 2.0) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2.0) * sin(deltaLon / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return (earthRadiusMeters * c).toFloat()
    }

    fun toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("label", safeLabel)
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("radiusMeters", DwellRadius.normalize(radiusMeters).toDouble())
            .put("durationMinutes", durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES))
            .put("monitoringEnabled", monitoringEnabled)
            .put("autoStart", autoStart)
            .put("createdAtMillis", createdAtMillis)
            .put("updatedAtMillis", updatedAtMillis)

    companion object {
        const val MAX_MONITORED_PLACES = 20
        const val MIN_DURATION_MINUTES = 1
        const val MAX_DURATION_MINUTES = 48 * 60
        private const val GEOFENCE_REQUEST_ID_MAX_LENGTH = 100
        private const val ZONE_REQUEST_ID_PREFIX = "dwell_place_"
        private const val APPROACH_REQUEST_ID_PREFIX = "dwell_approach_"
        private const val MAX_PLACE_ID_LENGTH = GEOFENCE_REQUEST_ID_MAX_LENGTH - 15
        const val MAX_LABEL_LENGTH = 120
        private const val NEAR_EXACT_DUPLICATE_DISTANCE_METERS = 5f
        private const val DUPLICATE_PLACE_DISTANCE_METERS = 25f
        private val LabelWhitespaceRegex = Regex("\\s+")

        fun create(
            label: String,
            latitude: Double,
            longitude: Double,
            radiusMeters: Float,
            durationMinutes: Int,
            monitoringEnabled: Boolean = false,
            autoStart: Boolean = true,
            now: Long = System.currentTimeMillis(),
        ): DwellPlace =
            DwellPlace(
                id = UUID.randomUUID().toString(),
                label = label.ifBlank { "Saved place" },
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                durationMinutes = durationMinutes,
                monitoringEnabled = monitoringEnabled,
                autoStart = autoStart,
                createdAtMillis = now,
                updatedAtMillis = now,
            ).normalized()

        fun fromJson(json: JSONObject): DwellPlace? {
            val id = json.optString("id").trim()
            if (!isValidPlaceId(id)) return null
            val latitude = json.optDouble("latitude", Double.NaN)
            val longitude = json.optDouble("longitude", Double.NaN)
            if (!hasValidCoordinates(latitude, longitude)) return null

            return DwellPlace(
                id = id,
                label = json.optString("label").ifBlank { "Saved place" },
                latitude = latitude,
                longitude = longitude,
                radiusMeters = json.optDouble(
                    "radiusMeters",
                    DwellRadius.DEFAULT_METERS.toDouble(),
                ).toFloat(),
                durationMinutes = json.optInt("durationMinutes", 270),
                monitoringEnabled = json.optBoolean("monitoringEnabled", false),
                autoStart = json.optBoolean("autoStart", true),
                createdAtMillis = json.optLong("createdAtMillis", 0L).takeIf { it > 0L }
                    ?: System.currentTimeMillis(),
                updatedAtMillis = json.optLong("updatedAtMillis", 0L).takeIf { it > 0L }
                    ?: System.currentTimeMillis(),
            ).normalized()
        }

        fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean =
            latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0

        fun isValidPlaceId(id: String): Boolean =
            id.isNotBlank() && id.length <= MAX_PLACE_ID_LENGTH

        internal fun normalizedLabelIdentity(label: String): String =
            cleanDisplayLabel(label)
                .lowercase(Locale.ROOT)

        private fun cleanDisplayLabel(label: String): String =
            label.trim()
                .replace(LabelWhitespaceRegex, " ")
                .ifBlank { "Saved place" }

        internal fun isDuplicateSavedPlace(
            left: DwellPlace,
            right: DwellPlace,
            maxDistanceMeters: Float = DUPLICATE_PLACE_DISTANCE_METERS,
        ): Boolean {
            val distanceMeters = left.distanceMetersTo(right.latitude, right.longitude)
            return distanceMeters <= NEAR_EXACT_DUPLICATE_DISTANCE_METERS ||
                (
                    normalizedLabelIdentity(left.safeLabel) == normalizedLabelIdentity(right.safeLabel) &&
                        distanceMeters <= maxDistanceMeters
                    )
        }

        fun normalizePlaces(places: List<DwellPlace>): List<DwellPlace> {
            var monitoredCount = 0
            val uniqueIds = places
                .map { it.normalized() }
                .fold(mutableListOf<DwellPlace>()) { acc, place ->
                    val index = acc.indexOfFirst { it.id == place.id }
                    if (index >= 0) {
                        acc[index] = place
                    } else {
                        acc += place
                    }
                    acc
                }
            return uniqueIds
                .fold(mutableListOf<DwellPlace>()) { acc, place ->
                    val index = acc.indexOfFirst { existing ->
                        isDuplicateSavedPlace(existing, place)
                    }
                    if (index >= 0) {
                        acc[index] = mergeDuplicatePlaces(acc[index], place)
                    } else {
                        acc += place
                    }
                    acc
                }
                .map { place ->
                    if (!place.monitoringEnabled) {
                        place
                    } else if (monitoredCount < MAX_MONITORED_PLACES) {
                        monitoredCount += 1
                        place
                    } else {
                        place.copy(monitoringEnabled = false).normalized()
                    }
                }
        }

        private fun mergeDuplicatePlaces(existing: DwellPlace, incoming: DwellPlace): DwellPlace {
            return existing.copy(
                monitoringEnabled = existing.monitoringEnabled || incoming.monitoringEnabled,
                autoStart = existing.autoStart,
                updatedAtMillis = maxOf(existing.updatedAtMillis, incoming.updatedAtMillis),
            ).normalized()
        }

        fun requestId(placeId: String): String =
            zoneRequestId(placeId)

        fun zoneRequestId(placeId: String): String =
            "$ZONE_REQUEST_ID_PREFIX$placeId"

        fun approachRequestId(placeId: String): String =
            "$APPROACH_REQUEST_ID_PREFIX$placeId"

        fun idFromRequestId(requestId: String): String? =
            requestFromRequestId(requestId)?.placeId

        fun requestFromRequestId(requestId: String): DwellGeofenceRequest? {
            val approach = requestId.removePrefix(APPROACH_REQUEST_ID_PREFIX)
            if (approach != requestId && approach.isNotBlank()) {
                return DwellGeofenceRequest(approach, DwellGeofenceType.APPROACH)
            }

            val zone = requestId.removePrefix(ZONE_REQUEST_ID_PREFIX)
            if (zone != requestId && zone.isNotBlank()) {
                return DwellGeofenceRequest(zone, DwellGeofenceType.ZONE)
            }

            return null
        }
    }
}
