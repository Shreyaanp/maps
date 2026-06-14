package work.shreyaan.dwell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellDiagnosticsTest {
    @Test
    fun exportIncludesPrivacyHeaderAndContext() {
        val text = DwellDiagnostics.exportText(
            entries = listOf(
                DwellDiagnosticEntry(
                    happenedAt = 0L,
                    source = "approach",
                    decision = "probe",
                    score = null,
                    detail = "balanced location requested",
                ),
            ),
            contextLines = listOf(
                "app 1.0 (3)",
                "permissions location=on background=on notifications=on motion=on",
                "map host=tiles.openfreemap.org attributionOsm=true",
                "search host=nominatim.openstreetmap.org networkAutocomplete=false",
            ),
            generatedAtMillis = 0L,
        )

        assertTrue(text.contains("Dwell diagnostics"))
        assertTrue(text.contains("Generated UTC: 1970-01-01 00:00:00Z"))
        assertTrue(text.contains("No coordinates are included."))
        assertTrue(text.contains("Context"))
        assertTrue(text.contains("- permissions location=on"))
        assertTrue(text.contains("- map host=tiles.openfreemap.org attributionOsm=true"))
        assertTrue(text.contains("- search host=nominatim.openstreetmap.org networkAutocomplete=false"))
        assertTrue(text.contains("Recent decisions"))
        assertTrue(text.contains("- "))
        assertTrue(text.contains("approach - probe"))
        assertFalse(text.contains("lat"))
        assertFalse(text.contains("lon"))
    }

    @Test
    fun exportEmptyEntriesStillExplainsNoRecentDecisions() {
        val text = DwellDiagnostics.exportText(
            entries = emptyList(),
            contextLines = listOf("monitoring monitored=0 registered=0 timer=idle"),
            generatedAtMillis = 0L,
        )

        assertTrue(text.contains("No coordinates are included."))
        assertTrue(text.contains("- monitoring monitored=0"))
        assertTrue(text.contains("Recent decisions\n- No recent decisions"))
    }

    @Test
    fun providerContextLinesIncludeMapAndSearchWithoutCoordinates() {
        val lines = DwellDiagnostics.providerContextLines(
            searchConfig = MobileSearchConfig(
                baseUrl = "https://search.dwell.example:8443/search",
                userAgent = "Dwell test user agent",
                networkAutocomplete = true,
            ),
            mapConfig = MobileMapConfig(
                styleUrl = "https://maps.dwell.example:443/styles/liberty",
                attributionLabel = MobileMapConfig.normalizeAttribution("OpenFreeMap"),
            ),
        )
        val text = lines.joinToString("\n")

        assertTrue(text.contains("map host=maps.dwell.example attributionOsm=true"))
        assertTrue(text.contains("search host=search.dwell.example networkAutocomplete=true"))
        assertFalse(text.contains("lat"))
        assertFalse(text.contains("lon"))
    }

    @Test
    fun providerContextLinesRedactUserInfoAndNormalizeHosts() {
        val lines = DwellDiagnostics.providerContextLines(
            searchConfig = MobileSearchConfig(
                baseUrl = "https://token:secret@SEARCH.DWELL.EXAMPLE.:8443/search?q=home",
                userAgent = "Dwell test user agent",
                networkAutocomplete = true,
            ),
            mapConfig = MobileMapConfig(
                styleUrl = "https://key@MAPS.DWELL.EXAMPLE.:443/styles/liberty?token=secret",
                attributionLabel = MobileMapConfig.normalizeAttribution("OpenFreeMap"),
            ),
        )
        val text = lines.joinToString("\n")

        assertTrue(text.contains("map host=maps.dwell.example attributionOsm=true"))
        assertTrue(text.contains("search host=search.dwell.example networkAutocomplete=true"))
        assertFalse(text.contains("token"))
        assertFalse(text.contains("secret"))
        assertFalse(text.contains("q=home"))
    }

    @Test
    fun providerContextLinesHandleBareAndMalformedUrls() {
        val bareLines = DwellDiagnostics.providerContextLines(
            searchConfig = MobileSearchConfig(
                baseUrl = "search.dwell.example/search",
                userAgent = "Dwell test user agent",
                networkAutocomplete = false,
            ),
            mapConfig = MobileMapConfig(
                styleUrl = "maps.dwell.example/styles/liberty",
                attributionLabel = MobileMapConfig.normalizeAttribution("OpenFreeMap"),
            ),
        ).joinToString("\n")

        assertTrue(bareLines.contains("map host=maps.dwell.example attributionOsm=true"))
        assertTrue(bareLines.contains("search host=search.dwell.example networkAutocomplete=false"))

        val malformedLines = DwellDiagnostics.providerContextLines(
            searchConfig = MobileSearchConfig(
                baseUrl = "https://",
                userAgent = "Dwell test user agent",
                networkAutocomplete = false,
            ),
            mapConfig = MobileMapConfig(
                styleUrl = "not a url",
                attributionLabel = MobileMapConfig.normalizeAttribution("OpenFreeMap"),
            ),
        ).joinToString("\n")

        assertTrue(malformedLines.contains("map host=unknown attributionOsm=true"))
        assertTrue(malformedLines.contains("search host=unknown networkAutocomplete=false"))
    }

    @Test
    fun sanitizerRedactsCoordinatesAndUrlsFromFreeformDetails() {
        val text = DwellDiagnostics.sanitizeDiagnosticText(
            "failed near lat=17.479312 lon:78.368611 at 17.4793, 78.3686 from https://example.com/search?q=home",
        )

        assertTrue(text.contains("lat=[coordinate]"))
        assertTrue(text.contains("lon=[coordinate]"))
        assertTrue(text.contains("[coordinates]"))
        assertTrue(text.contains("[url]"))
        assertFalse(text.contains("17.479312"))
        assertFalse(text.contains("78.368611"))
        assertFalse(text.contains("https://example.com"))
    }

    @Test
    fun sanitizerRedactsWhitespaceDelimitedCoordinateFields() {
        val text = DwellDiagnostics.sanitizeDiagnosticText(
            "provider returned latitude 17.479312 longitude 78.368611 and location.lng -122.0840",
        )

        assertTrue(text.contains("latitude=[coordinate]"))
        assertTrue(text.contains("longitude=[coordinate]"))
        assertTrue(text.contains("lng=[coordinate]"))
        assertFalse(text.contains("17.479312"))
        assertFalse(text.contains("78.368611"))
        assertFalse(text.contains("-122.0840"))
    }

    @Test
    fun sanitizerRedactsJsonPathSnakeAndCamelCoordinateFields() {
        val text = DwellDiagnostics.sanitizeDiagnosticText(
            """raw {"lat":17.479312,"lon":78.368611} current_lat=17.479312 centerLat:17.479312 location.lng -122.0840""",
        )

        assertTrue(text.contains(""""lat"=[coordinate]"""))
        assertTrue(text.contains(""""lon"=[coordinate]"""))
        assertTrue(text.contains("current_lat=[coordinate]"))
        assertTrue(text.contains("centerLat=[coordinate]"))
        assertTrue(text.contains("location.lng=[coordinate]"))
        assertFalse(text.contains("17.479312"))
        assertFalse(text.contains("78.368611"))
        assertFalse(text.contains("-122.0840"))
    }

    @Test
    fun sanitizerKeepsNonCoordinateNumericFields() {
        val text = DwellDiagnostics.sanitizeDiagnosticText(
            "platform=17 translation 12 durationMinutes:270 resultCount=5",
        )

        assertTrue(text.contains("platform=17"))
        assertTrue(text.contains("translation 12"))
        assertTrue(text.contains("durationMinutes:270"))
        assertTrue(text.contains("resultCount=5"))
    }

    @Test
    fun exportSanitizesStoredFreeformDetails() {
        val text = DwellDiagnostics.exportText(
            entries = listOf(
                DwellDiagnosticEntry(
                    happenedAt = 0L,
                    source = "test",
                    decision = "failed",
                    score = null,
                    detail = "raw 17.4793, 78.3686 https://example.com/path",
                ),
            ),
            contextLines = emptyList(),
            generatedAtMillis = 0L,
        )

        assertTrue(text.contains("[coordinates]"))
        assertTrue(text.contains("[url]"))
        assertFalse(text.contains("17.4793, 78.3686"))
        assertFalse(text.contains("https://example.com"))
    }
}
