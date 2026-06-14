package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileMapConfigTest {
    @Test
    fun defaultOpenFreeMapStyleIsAllowed() {
        assertTrue(
            MobileMapConfig.isAllowedStyleUrl(
                "https://tiles.openfreemap.org/styles/liberty",
            )
        )
    }

    @Test
    fun publicOpenStreetMapTileHostsAreRejected() {
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://tile.openstreetmap.org/styles/liberty",
            )
        )
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://a.tile.openstreetmap.org/styles/liberty",
            )
        )
    }

    @Test
    fun publicOpenStreetMapTileHostDetectionHandlesTrailingDot() {
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://tile.openstreetmap.org./styles/liberty",
            )
        )
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://a.tile.openstreetmap.org./styles/liberty",
            )
        )
    }

    @Test
    fun publicOpenStreetMapTileHostDetectionHandlesUserInfoAndCase() {
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://dwell@tile.openstreetmap.org/styles/liberty",
            )
        )
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://DWELL@A.TILE.OPENSTREETMAP.ORG/styles/liberty",
            )
        )
    }

    @Test
    fun knownPaidMapHostsAreRejected() {
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://api.mapbox.com/styles/v1/dwell/liberty",
            )
        )
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://tiles.mapbox.com/styles/v1/dwell/liberty",
            )
        )
        assertFalse(
            MobileMapConfig.isAllowedStyleUrl(
                "https://maps.googleapis.com/styles/liberty",
            )
        )
    }

    @Test
    fun styleUrlMustBeHttpsStyleEndpoint() {
        assertFalse(MobileMapConfig.isAllowedStyleUrl("http://tiles.example/styles/basic"))
        assertFalse(MobileMapConfig.isAllowedStyleUrl("https://tiles.example/tiles/basic"))
    }

    @Test
    fun attributionKeepsOpenStreetMapCredit() {
        assertEquals(
            "OpenFreeMap | OpenStreetMap",
            MobileMapConfig.normalizeAttribution("OpenFreeMap | OpenStreetMap"),
        )
    }

    @Test
    fun attributionAddsOpenStreetMapCreditWhenMissing() {
        assertEquals(
            "Dwell Maps | OpenStreetMap",
            MobileMapConfig.normalizeAttribution("Dwell Maps"),
        )
    }

    @Test
    fun blankAttributionUsesDefaultCredit() {
        assertEquals(
            MobileMapConfig.defaults().attributionLabel,
            MobileMapConfig.normalizeAttribution(""),
        )
    }
}
