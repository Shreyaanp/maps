package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationActionReceiverTest {
    @Test
    fun promptPlaceForActionRejectsUnscopedLegacyActions() {
        assertNull(
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
    fun scopedPromptActionAcceptsMatchingPromptToken() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 42L,
                actionPlaceId = "office",
            )
        )
    }

    @Test
    fun scopedPromptActionAcceptsNoPlacePromptWithMatchingToken() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 42L,
                actionPlaceId = null,
            )
        )
    }

    @Test
    fun scopedPromptActionRejectsStaleTokenTypeOrPlace() {
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 43L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 42L,
                actionPlaceId = "office",
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 42L,
                actionPlaceId = "office",
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "gym",
                currentPromptUpdated = 42L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 42L,
                actionPlaceId = "office",
            )
        )
    }

    @Test
    fun scopedPromptActionRejectsLegacyActionsWithoutPromptToken() {
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                actionPrompt = null,
                actionPromptUpdated = 0L,
                actionPlaceId = null,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                actionPrompt = null,
                actionPromptUpdated = 42L,
                actionPlaceId = "office",
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 0L,
                actionPlaceId = "office",
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptAction(
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                actionPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                actionPromptUpdated = 42L,
                actionPlaceId = null,
            )
        )
    }

    @Test
    fun scopedTimerActionAcceptsMatchingTimerToken() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
            )
        )
        assertTrue(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                actionTimerPlaceId = null,
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
            )
        )
    }

    @Test
    fun scopedTimerActionRejectsLegacyStaleOrMissingTokens() {
        assertFalse(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                actionTimerPlaceId = null,
                actionTimerStartedAt = 0L,
                actionTimerEnd = 0L,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "gym",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 2_000L,
                currentTimerEnd = 10_000L,
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedTimerAction(
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 11_000L,
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
            )
        )
    }

    @Test
    fun scopedCancelActionAllowsTimerOnlyCancelFromRunningNotification() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedCancelAction(
                timerScopeAccepted = true,
                promptScopePresent = false,
                promptScopeAccepted = false,
            )
        )
    }

    @Test
    fun scopedCancelActionRequiresPromptTokenWhenCancelCameFromPrompt() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedCancelAction(
                timerScopeAccepted = true,
                promptScopePresent = true,
                promptScopeAccepted = true,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedCancelAction(
                timerScopeAccepted = true,
                promptScopePresent = true,
                promptScopeAccepted = false,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedCancelAction(
                timerScopeAccepted = false,
                promptScopePresent = true,
                promptScopeAccepted = true,
            )
        )
    }

    @Test
    fun scopedPromptTimerActionRequiresPromptAndTimerTokens() {
        assertTrue(
            NotificationActionReceiver.acceptsScopedPromptTimerAction(
                promptScopeAccepted = true,
                timerScopePresent = true,
                timerScopeAccepted = true,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptTimerAction(
                promptScopeAccepted = true,
                timerScopePresent = false,
                timerScopeAccepted = false,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptTimerAction(
                promptScopeAccepted = false,
                timerScopePresent = true,
                timerScopeAccepted = true,
            )
        )
        assertFalse(
            NotificationActionReceiver.acceptsScopedPromptTimerAction(
                promptScopeAccepted = true,
                timerScopePresent = true,
                timerScopeAccepted = false,
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

    @Test
    fun actionDataStringScopesTimerActionsByTimerToken() {
        val first = Notifications.actionDataString(
            action = NotificationActionReceiver.ACTION_CANCEL,
            placeId = "office",
            timerPlaceId = "office",
            timerStartedAt = 1_000L,
            timerEnd = 10_000L,
        )
        val second = Notifications.actionDataString(
            action = NotificationActionReceiver.ACTION_CANCEL,
            placeId = "office",
            timerPlaceId = "office",
            timerStartedAt = 2_000L,
            timerEnd = 11_000L,
        )

        assertTrue(first != second)
        assertTrue(first.contains("/timer/office/1000/10000"))
    }

    @Test
    fun actionDataStringScopesPromptActionsByPromptToken() {
        val first = Notifications.actionDataString(
            action = NotificationActionReceiver.ACTION_START_TIMER,
            placeId = "office",
            prompt = Prefs.WATCH_PROMPT_START_TIMER,
            promptUpdated = 42L,
        )
        val second = Notifications.actionDataString(
            action = NotificationActionReceiver.ACTION_START_TIMER,
            placeId = "office",
            prompt = Prefs.WATCH_PROMPT_START_TIMER,
            promptUpdated = 43L,
        )

        assertTrue(first != second)
        assertTrue(first.contains("/prompt/start_timer/42"))
    }

    @Test
    fun setupNotificationUsesPlaceLanguage() {
        assertEquals(
            "Open Dwell to restore background location for your monitored place.",
            Notifications.setupNeededText(),
        )
    }

    @Test
    fun timerAndPromptNotificationsNameThePlaceWhenKnown() {
        assertEquals("Office timer running", Notifications.timerRunningTitle("Office"))
        assertEquals("Dwell running", Notifications.timerRunningTitle(""))
        assertEquals("Dwell running", Notifications.timerRunningTitle("Selected place"))
        assertEquals("Dwell running", Notifications.timerRunningTitle(" No place selected "))
        assertEquals(null, Notifications.timerPlaceLabel("") { "Office" })
        assertEquals("Office", Notifications.timerPlaceLabel("office") { "Office" })
        assertEquals("Time's up at Office", Notifications.timerDoneTitle("Office"))
        assertEquals("Time's up!", Notifications.timerDoneTitle(""))
        assertEquals("Time's up!", Notifications.timerDoneTitle("Saved place"))
        assertEquals(
            "Your 1h Office timer is complete.",
            Notifications.timerDoneText("Office", 60),
        )
        assertEquals(
            "Your 1h timer is complete.",
            Notifications.timerDoneText("", 60),
        )
        assertEquals(
            "Your 1h timer is complete.",
            Notifications.timerDoneText("Selected place", 60),
        )
        assertEquals("Leaving Office?", Notifications.exitQuestionTitle("Office"))
        assertEquals(
            "Keep the Office timer? Ends at 5:30 PM.",
            Notifications.exitQuestionText("Office", "5:30 PM"),
        )
        assertEquals("You left the area", Notifications.exitQuestionTitle("No place selected"))
        assertEquals(
            "Keep the timer? It ends at 5:30 PM.",
            Notifications.exitQuestionText("Saved place", "5:30 PM"),
        )
        assertEquals("Start timer at Gym?", Notifications.arrivalQuestionTitle("Gym"))
        assertEquals("Start Dwell timer?", Notifications.arrivalQuestionTitle("Selected place"))
        assertEquals(
            "Dwell thinks you arrived at Gym. Confidence 82%.",
            Notifications.arrivalQuestionText("Gym", 82),
        )
        assertEquals(
            "Dwell thinks you arrived. Confidence 82%.",
            Notifications.arrivalQuestionText("No place selected", 82),
        )
        assertEquals("Switch to Gym?", Notifications.switchQuestionTitle("Gym"))
        assertEquals("Switch Dwell place?", Notifications.switchQuestionTitle("Saved place"))
        assertEquals(
            "Stop Office and start Gym?",
            Notifications.switchQuestionText(
                newPlaceLabel = "Gym",
                currentPlaceLabel = "Office",
            ),
        )
        assertEquals(
            "Start Gym and stop the current timer?",
            Notifications.switchQuestionText(
                newPlaceLabel = "Gym",
                currentPlaceLabel = "",
            ),
        )
        assertEquals(
            "Start the new place and stop the current timer?",
            Notifications.switchQuestionText(
                newPlaceLabel = "Selected place",
                currentPlaceLabel = "No place selected",
            ),
        )
    }

    @Test
    fun notificationReplacementClearsStalePromptSurfaces() {
        assertEquals(
            setOf(
                Notifications.NOTIF_DONE,
                Notifications.NOTIF_EXIT,
                Notifications.NOTIF_ARRIVAL,
                Notifications.NOTIF_CONFLICT,
            ),
            Notifications.notificationIdsClearedBeforeRunningTimer(),
        )
        assertEquals(
            setOf(
                Notifications.NOTIF_DONE,
                Notifications.NOTIF_ARRIVAL,
                Notifications.NOTIF_CONFLICT,
            ),
            Notifications.notificationIdsClearedBeforeExitPrompt(),
        )
        assertEquals(
            setOf(
                Notifications.NOTIF_DONE,
                Notifications.NOTIF_EXIT,
                Notifications.NOTIF_CONFLICT,
            ),
            Notifications.notificationIdsClearedBeforeArrivalPrompt(),
        )
        assertEquals(
            setOf(
                Notifications.NOTIF_DONE,
                Notifications.NOTIF_EXIT,
                Notifications.NOTIF_ARRIVAL,
            ),
            Notifications.notificationIdsClearedBeforeSwitchPrompt(),
        )
        assertEquals(
            setOf(Notifications.NOTIF_SETUP),
            Notifications.notificationIdsClearedAfterSetupRecovery(),
        )
    }
}
