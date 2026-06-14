package work.shreyaan.dwell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCacheManagerTest {
    @Test
    fun prewarmRequestAcceptsAllowedOpenFreeMapStyle() {
        assertTrue(
            MapCacheManager.isValidPrewarmRequest(
                styleUrl = "https://tiles.openfreemap.org/styles/liberty",
                latitude = 17.47,
                longitude = 78.36,
            )
        )
    }

    @Test
    fun prewarmRequestRejectsPublicOpenStreetMapTileHosts() {
        assertFalse(
            MapCacheManager.isValidPrewarmRequest(
                styleUrl = "https://tile.openstreetmap.org/styles/liberty",
                latitude = 17.47,
                longitude = 78.36,
            )
        )
        assertFalse(
            MapCacheManager.isValidPrewarmRequest(
                styleUrl = "https://a.tile.openstreetmap.org/styles/liberty",
                latitude = 17.47,
                longitude = 78.36,
            )
        )
    }

    @Test
    fun prewarmRequestRejectsInvalidCoordinates() {
        assertFalse(
            MapCacheManager.isValidPrewarmRequest(
                styleUrl = "https://tiles.openfreemap.org/styles/liberty",
                latitude = 90.0,
                longitude = 78.36,
            )
        )
        assertFalse(
            MapCacheManager.isValidPrewarmRequest(
                styleUrl = "https://tiles.openfreemap.org/styles/liberty",
                latitude = 17.47,
                longitude = 181.0,
            )
        )
        assertFalse(
            MapCacheManager.isValidPrewarmRequest(
                styleUrl = "https://tiles.openfreemap.org/styles/liberty",
                latitude = Double.NaN,
                longitude = 78.36,
            )
        )
    }

    @Test
    fun prewarmStartsForNewKey() {
        assertTrue(
            MapCacheManager.shouldStartPrewarm(
                lastKey = "old",
                lastAtMillis = 1_000L,
                nextKey = "new",
                nowMillis = 2_000L,
                dedupeWindowMs = 15_000L,
            )
        )
    }

    @Test
    fun prewarmSkipsRepeatedKeyInsideWindow() {
        assertFalse(
            MapCacheManager.shouldStartPrewarm(
                lastKey = "same",
                lastAtMillis = 1_000L,
                nextKey = "same",
                nowMillis = 5_000L,
                dedupeWindowMs = 15_000L,
            )
        )
    }

    @Test
    fun prewarmStartsRepeatedKeyAfterWindow() {
        assertTrue(
            MapCacheManager.shouldStartPrewarm(
                lastKey = "same",
                lastAtMillis = 1_000L,
                nextKey = "same",
                nowMillis = 20_000L,
                dedupeWindowMs = 15_000L,
            )
        )
    }

    @Test
    fun prewarmKeyIsStableWithinSmallCoordinateDrift() {
        val first = MapCacheManager.prewarmKeyFor(
            styleUrl = "https://tiles.openfreemap.org/styles/liberty",
            latitude = 17.47011,
            longitude = 78.36011,
            radiusMeters = 151f,
        )
        val second = MapCacheManager.prewarmKeyFor(
            styleUrl = "https://tiles.openfreemap.org/styles/liberty",
            latitude = 17.47019,
            longitude = 78.36019,
            radiusMeters = 165f,
        )

        assertTrue(first == second)
    }

    @Test
    fun prewarmKeyChangesForDifferentStyleOrArea() {
        val first = MapCacheManager.prewarmKeyFor(
            styleUrl = "https://tiles.openfreemap.org/styles/liberty",
            latitude = 17.47,
            longitude = 78.36,
            radiusMeters = 150f,
        )
        val differentStyle = MapCacheManager.prewarmKeyFor(
            styleUrl = "https://tiles.example/styles/liberty",
            latitude = 17.47,
            longitude = 78.36,
            radiusMeters = 150f,
        )
        val differentArea = MapCacheManager.prewarmKeyFor(
            styleUrl = "https://tiles.openfreemap.org/styles/liberty",
            latitude = 17.59,
            longitude = 78.36,
            radiusMeters = 150f,
        )

        assertTrue(first != differentStyle)
        assertTrue(first != differentArea)
    }
}
