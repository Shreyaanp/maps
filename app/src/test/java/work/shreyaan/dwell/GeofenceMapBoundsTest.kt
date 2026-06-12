package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceMapBoundsTest {
    @Test
    fun radiusIsClampedToMinimumForTinyValues() {
        val tiny = GeofenceMapBounds.forCircle(
            latitude = 18.5204,
            longitude = 73.8567,
            radiusMeters = 1f,
        )
        val minimum = GeofenceMapBounds.forCircle(
            latitude = 18.5204,
            longitude = 73.8567,
            radiusMeters = 50f,
        )

        assertEquals(minimum, tiny)
    }

    @Test
    fun radiusIsClampedToMaximumForHugeValues() {
        val huge = GeofenceMapBounds.forCircle(
            latitude = 18.5204,
            longitude = 73.8567,
            radiusMeters = 5_000f,
        )
        val maximum = GeofenceMapBounds.forCircle(
            latitude = 18.5204,
            longitude = 73.8567,
            radiusMeters = 500f,
        )

        assertEquals(maximum, huge)
    }

    @Test
    fun boundsContainTheSelectedPoint() {
        val lat = 18.5204
        val lon = 73.8567
        val bounds = GeofenceMapBounds.forCircle(
            latitude = lat,
            longitude = lon,
            radiusMeters = 150f,
        )

        assertTrue(bounds.north > lat)
        assertTrue(bounds.south < lat)
        assertTrue(bounds.east > lon)
        assertTrue(bounds.west < lon)
    }

    @Test
    fun boundsClampNearMapLatitudeLimits() {
        val north = GeofenceMapBounds.forCircle(
            latitude = 89.99,
            longitude = 73.8567,
            radiusMeters = 500f,
        )
        val south = GeofenceMapBounds.forCircle(
            latitude = -89.99,
            longitude = 73.8567,
            radiusMeters = 500f,
        )

        assertEquals(85.0, north.north, 0.0)
        assertEquals(-85.0, south.south, 0.0)
        assertTrue(north.north > north.south)
        assertTrue(south.north > south.south)
    }

    @Test
    fun boundsClampNearDateLine() {
        val east = GeofenceMapBounds.forCircle(
            latitude = 18.5204,
            longitude = 179.999,
            radiusMeters = 500f,
        )
        val west = GeofenceMapBounds.forCircle(
            latitude = 18.5204,
            longitude = -179.999,
            radiusMeters = 500f,
        )

        assertEquals(180.0, east.east, 0.0)
        assertEquals(-180.0, west.west, 0.0)
        assertTrue(east.east > east.west)
        assertTrue(west.east > west.west)
    }

    @Test
    fun invalidCoordinatesFallBackToValidMapBounds() {
        val bounds = GeofenceMapBounds.forCircle(
            latitude = Double.NaN,
            longitude = Double.POSITIVE_INFINITY,
            radiusMeters = 150f,
        )

        assertTrue(bounds.north > bounds.south)
        assertTrue(bounds.east > bounds.west)
        assertTrue(bounds.north <= 85.0)
        assertTrue(bounds.south >= -85.0)
        assertTrue(bounds.east <= 180.0)
        assertTrue(bounds.west >= -180.0)
    }

    @Test
    fun longitudeSpanExpandsAtHighLatitudes() {
        val equator = GeofenceMapBounds.forCircle(
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 150f,
        )
        val highLatitude = GeofenceMapBounds.forCircle(
            latitude = 75.0,
            longitude = 0.0,
            radiusMeters = 150f,
        )

        val equatorSpan = equator.east - equator.west
        val highLatitudeSpan = highLatitude.east - highLatitude.west
        assertTrue(highLatitudeSpan > equatorSpan)
    }
}
