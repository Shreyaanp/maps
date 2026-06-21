package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchCommandPayloadTest {
    @Test
    fun promptCommandPayloadCarriesPromptPlaceAndTimerIdentity() {
        assertEquals(
            "prompt|start_timer|42|gym|office|1000|2000",
            promptCommandPayload(
                prompt = "start_timer",
                promptUpdated = 42L,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
            ),
        )
    }

    @Test
    fun timerCommandPayloadCarriesTimerIdentityAndMinutes() {
        assertEquals(
            "office|1000|2000|30",
            timerCommandPayload(
                placeId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
                minutes = 30,
            ),
        )
    }

    @Test
    fun timerCommandPayloadCanOmitMinutesForCancel() {
        assertEquals(
            "office|1000|2000|",
            timerCommandPayload(
                placeId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
            ),
        )
    }

    @Test
    fun timerCommandPayloadKeepsBlankTimerPlaceIdentity() {
        assertEquals(
            "|1000|2000|30",
            timerCommandPayload(
                placeId = "",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
                minutes = 30,
            ),
        )
    }

    @Test
    fun extendCommandPayloadUsesPromptTokenForPhoneTimeUpPrompt() {
        assertEquals(
            "time_up_extend|42|office|30",
            extendCommandPayload(
                prompt = "time_up",
                promptUpdated = 42L,
                placeId = "office",
                timerStartedAt = 0L,
                timerEnd = 0L,
                minutes = 30,
            ),
        )
    }

    @Test
    fun extendCommandPayloadUsesTimerIdentityForLocalTimeUpWithoutPhonePromptToken() {
        assertEquals(
            "office|1000|2000|30",
            extendCommandPayload(
                prompt = "time_up",
                promptUpdated = 0L,
                placeId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
                minutes = 30,
            ),
        )
    }

    @Test
    fun extendCommandPayloadUsesTimerIdentityForRunningOrLocalExpiredTimer() {
        assertEquals(
            "office|1000|2000|30",
            extendCommandPayload(
                prompt = "none",
                promptUpdated = 42L,
                placeId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
                minutes = 30,
            ),
        )
    }

    @Test
    fun doneCommandPayloadUsesPromptIdentityForPhoneTimeUpPrompt() {
        assertEquals(
            "prompt|time_up|42||office|1000|2000",
            doneCommandPayload(
                prompt = "time_up",
                promptUpdated = 42L,
                promptPlaceId = "",
                placeId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
            ),
        )
    }

    @Test
    fun doneCommandPayloadUsesTimerIdentityForLocalExpiry() {
        assertEquals(
            "office|1000|2000|",
            doneCommandPayload(
                prompt = "none",
                promptUpdated = 42L,
                promptPlaceId = "",
                placeId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
            ),
        )
    }

    @Test
    fun sentWatchCommandFeedbackDoesNotClaimPhoneAcceptedMutation() {
        assertEquals("Sent to phone", watchCommandSentFeedback(sent = true))
        assertEquals("Phone not nearby", watchCommandSentFeedback(sent = false))
        assertEquals(
            "Start sent to phone",
            watchCommandSentFeedback(WatchCommandAction.StartTimer, sent = true),
        )
        assertEquals(
            "Not now sent to phone",
            watchCommandSentFeedback(WatchCommandAction.DismissArrival, sent = true),
        )
        assertEquals(
            "Keep sent to phone",
            watchCommandSentFeedback(WatchCommandAction.KeepTimer, sent = true),
        )
        assertEquals(
            "Cancel sent to phone",
            watchCommandSentFeedback(WatchCommandAction.CancelTimer, sent = true),
        )
        assertEquals(
            "Done sent to phone",
            watchCommandSentFeedback(WatchCommandAction.MarkDone, sent = true),
        )
        assertEquals(
            "Extend sent to phone",
            watchCommandSentFeedback(WatchCommandAction.ExtendTimer, sent = true),
        )
        assertEquals(
            "Phone not nearby",
            watchCommandSentFeedback(WatchCommandAction.StartTimer, sent = false),
        )
    }

    @Test
    fun watchAppHidesStalePhonePromptsButKeepsLocalTimeUp() {
        assertEquals(
            WatchPromptVisibility(startPrompt = true, leavingEarly = false, timeUp = false),
            watchPromptVisibility(
                prompt = "start_timer",
                timerEnd = 0L,
                lastUpdated = 1_000L,
                now = 60_000L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = false, timeUp = false),
            watchPromptVisibility(
                prompt = "start_timer",
                timerEnd = 0L,
                lastUpdated = 1_000L,
                now = 180_000L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = false, timeUp = false),
            watchPromptVisibility(
                prompt = "leave_early",
                timerEnd = 240_000L,
                lastUpdated = 1_000L,
                now = 180_000L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = false, timeUp = false),
            watchPromptVisibility(
                prompt = "time_up",
                timerEnd = 0L,
                lastUpdated = 1_000L,
                now = 180_000L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = false, timeUp = true),
            watchPromptVisibility(
                prompt = "none",
                timerEnd = 120_000L,
                lastUpdated = 1_000L,
                now = 180_000L,
            ),
        )
    }

    @Test
    fun watchPromptVisibilityUsesExactFreshnessBoundary() {
        assertEquals(
            WatchPromptVisibility(startPrompt = true, leavingEarly = false, timeUp = false),
            watchPromptVisibility(
                prompt = "start_timer",
                timerEnd = 0L,
                lastUpdated = 1_000L,
                now = 120_999L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = false, timeUp = false),
            watchPromptVisibility(
                prompt = "start_timer",
                timerEnd = 0L,
                lastUpdated = 1_000L,
                now = 121_000L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = true, timeUp = false),
            watchPromptVisibility(
                prompt = "leave_early",
                timerEnd = 240_000L,
                lastUpdated = 1_000L,
                now = 120_999L,
            ),
        )
        assertEquals(
            WatchPromptVisibility(startPrompt = false, leavingEarly = false, timeUp = true),
            watchPromptVisibility(
                prompt = "time_up",
                timerEnd = 0L,
                lastUpdated = 1_000L,
                now = 120_999L,
            ),
        )
    }

    @Test
    fun watchNotificationPermissionActionAppearsOnlyWhenNeeded() {
        assertEquals("Allow watch alerts", watchNotificationPermissionActionLabel(canNotify = false))
        assertEquals("", watchNotificationPermissionActionLabel(canNotify = true))
    }

    @Test
    fun watchNotificationPermissionFeedbackNamesWhatHappened() {
        assertEquals("Watch alert prompt opened", watchNotificationPermissionFeedback(opened = true))
        assertEquals("Open watch settings", watchNotificationPermissionFeedback(opened = false))
    }

    @Test
    fun watchReadyCopySeparatesNoPlacePausedReadyAndSetup() {
        assertEquals(
            "No place yet",
            watchReadyTitle(hasPlace = false, armed = false, needsSetup = false),
        )
        assertEquals(
            "Choose a place on phone",
            watchReadyDetail(hasPlace = false, armed = false, needsSetup = false),
        )
        assertEquals(
            "Syncing",
            watchReadyTitle(
                hasPlace = false,
                armed = false,
                needsSetup = false,
                lastUpdated = 0L,
            ),
        )
        assertEquals(
            "Open phone once",
            watchReadyDetail(
                hasPlace = false,
                armed = false,
                needsSetup = false,
                lastUpdated = 0L,
            ),
        )
        assertEquals(
            "Phone not nearby",
            watchReadyTitle(
                hasPlace = true,
                armed = true,
                needsSetup = false,
                lastUpdated = 1_000L,
                now = 180_000L,
            ),
        )
        assertEquals(
            "Open phone app",
            watchReadyDetail(
                hasPlace = true,
                armed = true,
                needsSetup = false,
                lastUpdated = 1_000L,
                now = 180_000L,
            ),
        )
        assertEquals(
            "Monitoring paused",
            watchReadyTitle(hasPlace = true, armed = false, needsSetup = false),
        )
        assertEquals(
            "Turn on Monitor",
            watchReadyDetail(hasPlace = true, armed = false, needsSetup = false),
        )
        assertEquals(
            "Monitoring live",
            watchReadyTitle(hasPlace = true, armed = true, needsSetup = false),
        )
        assertEquals(
            "2 places live",
            watchReadyTitle(
                hasPlace = true,
                armed = true,
                needsSetup = false,
                livePlaceCount = 2,
            ),
        )
        assertEquals(
            "Waiting for arrivals",
            watchReadyDetail(
                hasPlace = true,
                armed = true,
                needsSetup = false,
                livePlaceCount = 2,
            ),
        )
        assertEquals(
            "Needs setup",
            watchReadyTitle(hasPlace = true, armed = true, needsSetup = true),
        )
        assertEquals(
            "Finish setup on phone",
            watchReadyDetail(hasPlace = true, armed = true, needsSetup = true),
        )
    }
}
