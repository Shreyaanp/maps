package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class TileStateCalculatorTest {
    @Test
    fun setupStatePromptsPhoneSetupWhenNoPlaceExists() {
        assertEquals(
            TileState("Dwell", "Setup", "Open phone app", "Open"),
            TileStateCalculator.state(
                hasPlace = false,
                placeLabel = "",
                armed = false,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun pausedStateShowsSavedPlaceWhenNotArmed() {
        assertEquals(
            TileState("Office", "Paused", "Arm on phone", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office, Pune, India",
                armed = false,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun readyStateShowsArrivalMessageWhenArmed() {
        assertEquals(
            TileState("Office", "Ready", "Starts on arrival", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun needsSetupStateWinsOverReady() {
        assertEquals(
            TileState("Office", "Setup", "Open phone app", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                needsSetup = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun activeStateShowsRemainingTime() {
        assertEquals(
            TileState("Office", "1:30", "Still counting", "Timer"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun arrivalPromptWinsBeforeTimerStarts() {
        assertEquals(
            TileState("Arrived Office?", "Start?", "Confirm timer", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun switchPromptWinsOverActiveCountdown() {
        assertEquals(
            TileState("Switch to Gym?", "Switch?", "Confirm timer", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Gym",
                armed = true,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun leavingEarlyStateWinsOverActiveCountdown() {
        assertEquals(
            TileState("Leaving Office?", "Keep?", "1:30 left", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_LEAVE_EARLY,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun timeUpPromptWinsEvenWhenTimerEndIsCleared() {
        assertEquals(
            TileState("Office", "Done", "Time's up", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_TIME_UP,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun expiredTimerShowsTimeUpWithoutPrompt() {
        assertEquals(
            TileState("Office", "Done", "Time's up", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 999L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun formatRemainingCoversSecondsMinutesAndHours() {
        assertEquals("42s", TileStateCalculator.formatRemaining(42_000L))
        assertEquals("1:05", TileStateCalculator.formatRemaining(65_000L))
        assertEquals("2:03", TileStateCalculator.formatRemaining(7_380_000L))
    }

    @Test
    fun watchSyncCopyExplainsFreshAndMissingPhoneState() {
        assertEquals(
            "No phone sync yet",
            WatchSyncCopy.syncText(lastUpdated = 0L, now = 120_000L, activeTimer = false),
        )
        assertEquals(
            "Synced just now",
            WatchSyncCopy.syncText(lastUpdated = 90_000L, now = 120_000L, activeTimer = true),
        )
    }

    @Test
    fun watchSyncCopyKeepsDisconnectedCountdownCalm() {
        assertEquals(
            "Phone not nearby, still counting",
            WatchSyncCopy.syncText(lastUpdated = 1_000L, now = 180_000L, activeTimer = true),
        )
        assertEquals(
            "Phone not nearby",
            WatchSyncCopy.syncText(lastUpdated = 1_000L, now = 180_000L, activeTimer = false),
        )
    }
}
