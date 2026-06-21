package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchNotificationsTest {
    @Test
    fun localTimerExpirySchedulesOnlyFutureTimersAndIgnoresStalePrompts() {
        assertEquals(
            true,
            WatchTimerExpiryReceiver.shouldScheduleLocalTimerExpiry(
                timerEnd = 2_000L,
                now = 1_000L,
            ),
        )
        assertEquals(
            false,
            WatchTimerExpiryReceiver.shouldScheduleLocalTimerExpiry(
                timerEnd = 1_000L,
                now = 1_000L,
            ),
        )
        assertEquals(
            true,
            WatchTimerExpiryReceiver.shouldShowLocalTimeUp(
                timerEnd = 1_000L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
        assertEquals(
            false,
            WatchTimerExpiryReceiver.shouldShowLocalTimeUp(
                timerEnd = 1_000L,
                prompt = TileStateCalculator.PROMPT_TIME_UP,
                now = 1_000L,
            ),
        )
        assertEquals(
            false,
            WatchTimerExpiryReceiver.shouldShowLocalTimeUp(
                timerEnd = 2_000L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun timerAndArrivalNotificationsNameThePlaceWhenKnown() {
        assertEquals(
            "Gym",
            WatchDataService.stateNotificationPlaceLabel(
                prompt = "start_timer",
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
            ),
        )
        assertEquals(
            "Office",
            WatchDataService.stateNotificationPlaceLabel(
                prompt = "leave_early",
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
            ),
        )
        assertEquals(
            "Office",
            WatchDataService.stateNotificationPlaceLabel(
                prompt = "time_up",
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
            ),
        )
        assertEquals(
            "Office",
            WatchDataService.stateNotificationPlaceLabel(
                prompt = "none",
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
            ),
        )
        assertEquals("Office timer started", WatchNotifications.timerStartedTitle("Office"))
        assertEquals("Timer started", WatchNotifications.timerStartedTitle(""))
        assertEquals("Timer started", WatchNotifications.timerStartedTitle("Saved place"))
        assertEquals("Timer started", WatchNotifications.timerStartedTitle("No place selected"))
        assertEquals(
            "Office ends at 5:30 PM",
            WatchNotifications.timerRunningText(" Office ", "5:30 PM"),
        )
        assertEquals(
            "Dwell timer ends at 5:30 PM",
            WatchNotifications.timerRunningText("Saved place", "5:30 PM"),
        )
        assertEquals(
            "Dwell timer ends at 5:30 PM",
            WatchNotifications.timerRunningText("No place selected", "5:30 PM"),
        )
        assertEquals("Leaving Office?", WatchNotifications.leavingEarlyTitle("Office"))
        assertEquals("Leaving this place?", WatchNotifications.leavingEarlyTitle(""))
        assertEquals("Leaving this place?", WatchNotifications.leavingEarlyTitle("Selected place"))
        assertEquals("Start timer at Gym?", WatchNotifications.arrivalPromptTitle("Gym"))
        assertEquals("Start timer?", WatchNotifications.arrivalPromptTitle(""))
        assertEquals("Start timer?", WatchNotifications.arrivalPromptTitle("Selected place"))
        assertEquals(
            "Dwell thinks you arrived at Gym.",
            WatchNotifications.arrivalPromptText("Gym"),
        )
        assertEquals(
            "Dwell thinks you arrived.",
            WatchNotifications.arrivalPromptText(""),
        )
        assertEquals(
            "Dwell thinks you arrived.",
            WatchNotifications.arrivalPromptText("No place selected"),
        )
        assertEquals(
            "Switch to Gym?",
            WatchNotifications.startPromptNotificationTitle("Gym", switching = true),
        )
        assertEquals(
            "Start Gym and stop the current timer.",
            WatchNotifications.startPromptNotificationText("Gym", switching = true),
        )
        assertEquals(
            "Switch timer?",
            WatchNotifications.startPromptNotificationTitle("", switching = true),
        )
        assertEquals("Time's up at Office", WatchNotifications.timeUpTitle("Office"))
        assertEquals("Time's up", WatchNotifications.timeUpTitle(""))
        assertEquals("Time's up", WatchNotifications.timeUpTitle("Saved place"))
        assertEquals(
            "Extend or mark done for Office.",
            WatchNotifications.timeUpText("Office"),
        )
        assertEquals(
            "Done or extend from your watch.",
            WatchNotifications.timeUpText(""),
        )
        assertEquals(
            "Done or extend from your watch.",
            WatchNotifications.timeUpText("Selected place"),
        )
    }

    @Test
    fun watchAppStartPromptNamesSwitchTargetWhenKnown() {
        assertEquals(
            "Gym",
            watchPromptDisplayPlaceLabel(promptPlaceLabel = "Gym", placeLabel = "Office"),
        )
        assertEquals(
            "Office",
            watchPromptDisplayPlaceLabel(promptPlaceLabel = "", placeLabel = "Office"),
        )
        assertEquals(
            "Office",
            watchTimerDisplayPlaceLabel(timerPlaceLabel = "Office", placeLabel = "Gym"),
        )
        assertEquals(
            "Gym",
            watchTimerDisplayPlaceLabel(timerPlaceLabel = "", placeLabel = "Gym"),
        )
        assertEquals("Start at Office?", startPromptTitle("Office", switching = false))
        assertEquals("Office", startPromptSubtitle("Office", switching = false))
        assertEquals("Switch to Gym?", startPromptTitle("Gym", switching = true))
        assertEquals("New timer place", startPromptSubtitle("Gym", switching = true))
        assertEquals("Start timer?", startPromptTitle("", switching = false))
        assertEquals("Arrived", startPromptSubtitle("", switching = false))
        assertEquals("Switch timer?", startPromptTitle("", switching = true))
        assertEquals("Choose new timer", startPromptSubtitle("", switching = true))
        assertEquals("Start at Office?", startPromptTitle("Office, Pune", switching = false))
        assertEquals("Office timer", activeTimerTitle("Office"))
        assertEquals("Office timer", activeTimerTitle("Office, Pune"))
        assertEquals("Timer active", activeTimerSubtitle("Office"))
        assertEquals("Timer active", activeTimerTitle(""))
        assertEquals("Dwell timer", activeTimerSubtitle(""))
        assertEquals("Timer active", activeTimerTitle("Saved place"))
        assertEquals("Dwell timer", activeTimerSubtitle("No place selected"))
        assertEquals("Time's up at Office", timeUpScreenTitle("Office"))
        assertEquals("Time's up at Office", timeUpScreenTitle("Office, Pune"))
        assertEquals("Timer complete", timeUpScreenSubtitle("Office"))
        assertEquals("Time's up", timeUpScreenTitle(""))
        assertEquals("Dwell timer", timeUpScreenSubtitle(""))
        assertEquals("Time's up", timeUpScreenTitle("Selected place"))
        assertEquals("Dwell timer", timeUpScreenSubtitle("Saved place"))
        assertEquals("Office", watchReadyPlaceLabel("Office, Pune"))
        assertEquals("this place", watchReadyPlaceLabel(""))
        assertEquals("this place", watchReadyPlaceLabel("Saved place"))
        assertEquals("this place", watchReadyPlaceLabel("No place selected"))
        assertEquals("Office", watchTimerPlaceLabel("Office, Pune", fallback = "Dwell"))
        assertEquals("Dwell", watchTimerPlaceLabel("", fallback = "Dwell"))
        assertEquals("Dwell", watchTimerPlaceLabel("Saved place", fallback = "Dwell"))
        assertEquals("Dwell timer", watchTimerPlaceLabel("No place selected", fallback = "Dwell timer"))
        assertEquals(
            "Background location permission is needed",
            watchReadyMetaText(
                needsSetup = true,
                monitoringError = "Background location permission is needed",
                registeredPlaceCount = 1,
                armedPlaceCount = 1,
                durationMinutes = 270,
            ),
        )
        assertEquals(
            "Needs setup",
            watchReadyMetaText(
                needsSetup = true,
                monitoringError = "",
                registeredPlaceCount = 1,
                armedPlaceCount = 1,
                durationMinutes = 270,
            ),
        )
        assertEquals(
            "2 places live",
            watchReadyMetaText(
                needsSetup = false,
                monitoringError = "",
                registeredPlaceCount = 2,
                armedPlaceCount = 2,
                durationMinutes = 270,
            ),
        )
        assertEquals(
            "4h 30m default",
            watchReadyMetaText(
                needsSetup = false,
                monitoringError = "",
                registeredPlaceCount = 1,
                armedPlaceCount = 1,
                durationMinutes = 270,
            ),
        )
    }
}
