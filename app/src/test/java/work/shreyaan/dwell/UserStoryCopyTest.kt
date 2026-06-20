package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class UserStoryCopyTest {
    @Test
    fun searchPlaceholderSeparatesCreateAndEditModes() {
        assertEquals(
            "Search place or address",
            mapSearchPlaceholder(hasPlace = false, editingSelectedPlace = false),
        )
        assertEquals(
            "Search to move selected place",
            mapSearchPlaceholder(hasPlace = true, editingSelectedPlace = true),
        )
        assertEquals(
            "Search places",
            mapSearchPlaceholder(hasPlace = true, editingSelectedPlace = false),
        )
    }

    @Test
    fun currentLocationSubtitleSeparatesMoveFromCreate() {
        assertEquals(
            "Move selected place where you are now",
            currentLocationActionSubtitle(editingSelectedPlace = true),
        )
        assertEquals(
            "Drop a zone where you are now",
            currentLocationActionSubtitle(editingSelectedPlace = false),
        )
        assertEquals(
            "Move selected place nearby",
            currentLocationActionSubtitle(editingSelectedPlace = true, compact = true),
        )
        assertEquals(
            "Fastest way to create a nearby zone",
            currentLocationActionSubtitle(editingSelectedPlace = false, compact = true),
        )
    }

    @Test
    fun arrivalModeLabelsMatchTheUserStory() {
        assertEquals("Auto-start", arrivalModeLabel(autoStart = true))
        assertEquals("Confirm first", arrivalModeLabel(autoStart = false))
    }
}
