package work.shreyaan.dwell

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

data class GeoBounds(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double,
)

object GeofenceMapBounds {
    private const val METERS_PER_DEGREE = 111_320.0
    private const val MIN_MAP_LATITUDE = -85.0
    private const val MAX_MAP_LATITUDE = 85.0
    private const val MIN_MAP_LONGITUDE = -180.0
    private const val MAX_MAP_LONGITUDE = 180.0
    private const val MIN_RADIUS_METERS = 50f
    private const val MAX_RADIUS_METERS = 500f
    private const val PADDING_SCALE = 1.45
    private const val MIN_LONGITUDE_SCALE = 0.25

    fun forCircle(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
    ): GeoBounds {
        val paddedRadius = radiusMeters
            .coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
            .toDouble() * PADDING_SCALE
        val latDelta = paddedRadius / METERS_PER_DEGREE
        val centerLatitude = latitude
            .takeIf { it.isFinite() }
            ?.coerceIn(MIN_MAP_LATITUDE + latDelta, MAX_MAP_LATITUDE - latDelta)
            ?: 0.0
        val lonMetersAtLatitude = METERS_PER_DEGREE * max(
            MIN_LONGITUDE_SCALE,
            abs(cos(Math.toRadians(centerLatitude))),
        )
        val lonDelta = paddedRadius / lonMetersAtLatitude
        val centerLongitude = longitude
            .takeIf { it.isFinite() }
            ?.coerceIn(MIN_MAP_LONGITUDE + lonDelta, MAX_MAP_LONGITUDE - lonDelta)
            ?: 0.0

        return GeoBounds(
            north = centerLatitude + latDelta,
            east = centerLongitude + lonDelta,
            south = centerLatitude - latDelta,
            west = centerLongitude - lonDelta,
        )
    }
}
