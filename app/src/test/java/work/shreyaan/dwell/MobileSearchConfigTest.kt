package work.shreyaan.dwell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSearchConfigTest {
    @Test
    fun publicNominatimNeverAllowsNetworkAutocomplete() {
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://nominatim.openstreetmap.org",
                requestedAutocomplete = true,
            )
        )
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://nominatim.openstreetmap.org/search",
                requestedAutocomplete = true,
            )
        )
    }

    @Test
    fun publicNominatimDetectionIsCaseAndPortTolerant() {
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "HTTPS://NOMINATIM.OPENSTREETMAP.ORG:443/search",
                requestedAutocomplete = true,
            )
        )
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://nominatim.openstreetmap.org./",
                requestedAutocomplete = true,
            )
        )
    }

    @Test
    fun publicNominatimDetectionHandlesUserInfoUrls() {
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://dwell@nominatim.openstreetmap.org/search",
                requestedAutocomplete = true,
            )
        )
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://DWELL@NOMINATIM.OPENSTREETMAP.ORG:443/search",
                requestedAutocomplete = true,
            )
        )
    }

    @Test
    fun nonPublicSearchCanOptIntoNetworkAutocomplete() {
        assertTrue(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://search.dwell.example",
                requestedAutocomplete = true,
            )
        )
    }

    @Test
    fun knownPaidSearchHostsCannotBeUsedOrAutocomplete() {
        assertFalse(MobileSearchConfig.isAllowedBaseUrl("https://maps.googleapis.com"))
        assertFalse(MobileSearchConfig.isAllowedBaseUrl("https://places.googleapis.com/v1"))
        assertFalse(MobileSearchConfig.isAllowedBaseUrl("https://api.mapbox.com/geocoding/v5"))
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://api.mapbox.com/geocoding/v5",
                requestedAutocomplete = true,
            )
        )
    }

    @Test
    fun nominatimSearchBaseAllowsPublicAndSelfHostedHttps() {
        assertTrue(MobileSearchConfig.isAllowedBaseUrl("https://nominatim.openstreetmap.org"))
        assertTrue(MobileSearchConfig.isAllowedBaseUrl("https://search.dwell.example"))
        assertFalse(MobileSearchConfig.isAllowedBaseUrl("http://search.dwell.example"))
    }

    @Test
    fun explicitOptOutDisablesNetworkAutocomplete() {
        assertFalse(
            MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = "https://search.dwell.example",
                requestedAutocomplete = false,
            )
        )
    }

    @Test
    fun searchEndpointAcceptsServerRoot() {
        assertEquals(
            "https://nominatim.openstreetmap.org/search",
            MobileSearchConfig.searchEndpoint("https://nominatim.openstreetmap.org"),
        )
    }

    @Test
    fun searchEndpointDoesNotAppendSearchTwice() {
        assertEquals(
            "https://nominatim.openstreetmap.org/search",
            MobileSearchConfig.searchEndpoint("https://nominatim.openstreetmap.org/search/"),
        )
    }

    @Test
    fun blankSearchEndpointFallsBackToPublicNominatimSearch() {
        assertEquals(
            "https://nominatim.openstreetmap.org/search",
            MobileSearchConfig.searchEndpoint(""),
        )
    }
}
