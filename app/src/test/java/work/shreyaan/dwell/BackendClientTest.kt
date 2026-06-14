package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendClientTest {
    @Test
    fun analyticsPropertiesRedactCoordinateKeys() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "source" to "map_long_press",
                "lat" to 17.479312,
                "longitude" to 78.368611,
                "radiusMeters" to 150,
            )
        )

        assertEquals("map_long_press", properties["source"])
        assertEquals("[redacted]", properties["lat"])
        assertEquals("[redacted]", properties["longitude"])
        assertEquals(150, properties["radiusMeters"])
    }

    @Test
    fun analyticsPropertiesRedactCoordinateKeyVariants() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "centerLat" to 17.479312,
                "placeLongitude" to 78.368611,
                "arrival_lng" to 78.368611,
                "gps-coords" to "17.479312, 78.368611",
                "currentLocationLat" to 17.479312,
            )
        )

        assertEquals("[redacted]", properties["centerLat"])
        assertEquals("[redacted]", properties["placeLongitude"])
        assertEquals("[redacted]", properties["arrival_lng"])
        assertEquals("[redacted]", properties["gps-coords"])
        assertEquals("[redacted]", properties["currentLocationLat"])
    }

    @Test
    fun analyticsPropertiesKeepNonLocationLookalikes() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "platform" to "android",
                "translation" to "done",
                "durationMinutes" to 270,
                "radiusMeters" to 150,
                "resultCount" to 5,
            )
        )

        assertEquals("android", properties["platform"])
        assertEquals("done", properties["translation"])
        assertEquals(270, properties["durationMinutes"])
        assertEquals(150, properties["radiusMeters"])
        assertEquals(5, properties["resultCount"])
    }

    @Test
    fun analyticsPropertiesRedactSearchAndPlaceTextKeys() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "searchQuery" to "221B Baker Street",
                "address" to "1 Infinite Loop",
                "placeLabel" to "Home",
                "locationName" to "Office",
                "display_name" to "A private result label",
                "label" to "Gym near my house",
                "resultCount" to 4,
                "source" to "search_result",
            )
        )

        assertEquals("[redacted]", properties["searchQuery"])
        assertEquals("[redacted]", properties["address"])
        assertEquals("[redacted]", properties["placeLabel"])
        assertEquals("[redacted]", properties["locationName"])
        assertEquals("[redacted]", properties["display_name"])
        assertEquals("[redacted]", properties["label"])
        assertEquals(4, properties["resultCount"])
        assertEquals("search_result", properties["source"])
    }

    @Test
    fun analyticsPropertiesRedactNestedSearchAndPlaceTextKeys() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "search" to mapOf(
                    "query" to "private home address",
                    "result_label" to "Private place",
                    "resultCount" to 2,
                ),
            )
        )

        @Suppress("UNCHECKED_CAST")
        val search = properties["search"] as Map<String, Any?>
        assertEquals("[redacted]", search["query"])
        assertEquals("[redacted]", search["result_label"])
        assertEquals(2, search["resultCount"])
    }

    @Test
    fun analyticsPropertiesSanitizeFreeformStrings() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "message" to "near 17.4793, 78.3686 from https://example.com/debug",
            )
        )

        val message = properties["message"].toString()
        assertTrue(message.contains("[coordinates]"))
        assertTrue(message.contains("[url]"))
        assertFalse(message.contains("17.4793, 78.3686"))
        assertFalse(message.contains("https://example.com"))
    }

    @Test
    fun analyticsPropertiesSanitizeJsonLikeCoordinateStrings() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "message" to """raw {"lat":17.479312,"lon":78.368611} current_lat=17.479312 centerLat:17.479312""",
            )
        )

        val message = properties["message"].toString()
        assertTrue(message.contains(""""lat"=[coordinate]"""))
        assertTrue(message.contains(""""lon"=[coordinate]"""))
        assertTrue(message.contains("current_lat=[coordinate]"))
        assertTrue(message.contains("centerLat=[coordinate]"))
        assertFalse(message.contains("17.479312"))
        assertFalse(message.contains("78.368611"))
    }

    @Test
    fun analyticsPropertiesSanitizeNestedMaps() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "confidence" to mapOf(
                    "score" to 72,
                    "lng" to 78.368611,
                    "detail" to "lat=17.479312",
                ),
            )
        )

        @Suppress("UNCHECKED_CAST")
        val confidence = properties["confidence"] as Map<String, Any?>
        assertEquals(72, confidence["score"])
        assertEquals("[redacted]", confidence["lng"])
        assertEquals("lat=[coordinate]", confidence["detail"])
    }

    @Test
    fun analyticsPropertiesSanitizeNestedLists() {
        val properties = BackendClient.sanitizedEventProperties(
            mapOf(
                "samples" to listOf(
                    "near 17.4793, 78.3686",
                    mapOf(
                        "lat" to 17.479312,
                        "detail" to "lon:78.368611",
                    ),
                    arrayOf(
                        "https://example.com/debug?lat=17.479312",
                        mapOf("longitude" to 78.368611),
                    ),
                ),
            )
        )

        @Suppress("UNCHECKED_CAST")
        val samples = properties["samples"] as List<Any?>
        assertEquals("near [coordinates]", samples[0])

        @Suppress("UNCHECKED_CAST")
        val sampleMap = samples[1] as Map<String, Any?>
        assertEquals("[redacted]", sampleMap["lat"])
        assertEquals("lon=[coordinate]", sampleMap["detail"])

        @Suppress("UNCHECKED_CAST")
        val nestedArray = samples[2] as List<Any?>
        assertEquals("[url]", nestedArray[0])
        @Suppress("UNCHECKED_CAST")
        val nestedMap = nestedArray[1] as Map<String, Any?>
        assertEquals("[redacted]", nestedMap["longitude"])
    }
}
