package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationActionReceiverTest {
    @Test
    fun promptPlaceForActionUsesCurrentPromptForLegacyActions() {
        assertEquals(
            "office",
            NotificationActionReceiver.promptPlaceForAction(
                currentPromptPlaceId = "office",
                actionPlaceId = null,
            ),
        )
    }

    @Test
    fun promptPlaceForActionAcceptsMatchingScopedAction() {
        assertEquals(
            "office",
            NotificationActionReceiver.promptPlaceForAction(
                currentPromptPlaceId = "office",
                actionPlaceId = "office",
            ),
        )
    }

    @Test
    fun promptPlaceForActionRejectsStaleScopedAction() {
        assertNull(
            NotificationActionReceiver.promptPlaceForAction(
                currentPromptPlaceId = "gym",
                actionPlaceId = "office",
            )
        )
        assertNull(
            NotificationActionReceiver.promptPlaceForAction(
                currentPromptPlaceId = "",
                actionPlaceId = "office",
            )
        )
    }

    @Test
    fun scopedTimerActionAcceptsLegacyOrMatchingPlace() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "office",
                actionPlaceId = null,
            )
        )
        assertTrue(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "office",
                actionPlaceId = "office",
            )
        )
    }

    @Test
    fun scopedTimerActionRejectsStaleOrMissingCurrentPlace() {
        assertFalse(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "gym",
                actionPlaceId = "office",
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "",
                actionPlaceId = "office",
            )
        )
    }

    @Test
    fun actionDataStringScopesPendingIntentsByPlace() {
        val office = Notifications.actionDataString(
            NotificationActionReceiver.ACTION_START_TIMER,
            "office",
        )
        val gym = Notifications.actionDataString(
            NotificationActionReceiver.ACTION_START_TIMER,
            "gym",
        )

        assertTrue(office != gym)
    }

    @Test
    fun actionDataStringEncodesUnusualPlaceIds() {
        val data = Notifications.actionDataString(
            NotificationActionReceiver.ACTION_START_TIMER,
            "home/second floor",
        )

        assertTrue(data.contains("home%2Fsecond+floor"))
    }
}
