package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class TileStateCalculatorTest {
    @Test
    fun setupStatePromptsPhoneSetupWhenNoPlaceExists() {
        assertEquals(
            TileState("Dwell", "No place", "Choose on phone", "Open"),
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
    fun tileLeadsWithSyncStateBeforeFirstPhoneUpdate() {
        assertEquals(
            TileState("Dwell", "Syncing", "Open phone once", "Open"),
            TileStateCalculator.state(
                hasPlace = false,
                placeLabel = "",
                armed = false,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 120_000L,
                lastUpdated = 0L,
            ),
        )
    }

    @Test
    fun tileLeadsWithPhoneAwayWhenNonTimerStateIsStale() {
        assertEquals(
            TileState("Dwell", "Phone away", "Open phone app", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                registeredPlaceCount = 1,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 180_000L,
                lastUpdated = 1_000L,
            ),
        )
    }

    @Test
    fun tileStillCountsDownActiveTimerWhenPhoneIsStale() {
        assertEquals(
            TileState("Office", "1:30", "Still counting", "Timer"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                timerEnd = 270_000L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 180_000L,
                lastUpdated = 1_000L,
            ),
        )
    }

    @Test
    fun pausedStateShowsSavedPlaceWhenNotArmed() {
        assertEquals(
            TileState("Office", "Paused", "Monitor on phone", "Open"),
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
            TileState("Office", "Registered", "Starts on arrival", "Open"),
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
    fun readyStateLeadsWithRegisteredPlaceCountWhenMultiplePlacesAreRegistered() {
        assertEquals(
            TileState("Dwell", "2 registered", "Monitoring registered", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                registeredPlaceCount = 2,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun needsSetupStateWinsOverReady() {
        assertEquals(
            TileState("Office", "Needs setup", "Finish setup on phone", "Open"),
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
    fun noPlaceTimerStatesStayGeneric() {
        assertEquals(
            TileState("Dwell", "1:30", "Still counting", "Timer"),
            TileStateCalculator.state(
                hasPlace = false,
                placeLabel = "",
                armed = false,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Dwell", "Done", "Time's up", "Open"),
            TileStateCalculator.state(
                hasPlace = false,
                placeLabel = "",
                armed = false,
                timerEnd = 999L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun placeholderPlaceLabelsStayGenericOnTile() {
        assertEquals("", TileStateCalculator.run { "Saved place".shortTilePlace() })
        assertEquals("", TileStateCalculator.run { "No place selected".shortTilePlace() })
        assertEquals("", TileStateCalculator.run { "Selected place".shortTilePlace() })
        assertEquals(
            TileState("Dwell", "1:30", "Still counting", "Timer"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Saved place",
                armed = true,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Arrived?", "Start?", "Confirm timer", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "No place selected",
                armed = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Dwell", "Needs setup", "Finish setup on phone", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Selected place",
                armed = true,
                needsSetup = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Dwell", "Paused", "Monitor on phone", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Saved place",
                armed = false,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun promptAndTimerPrecedenceBeatsSetupAndMultipleLiveSummary() {
        assertEquals(
            TileState("Switch to Gym?", "Switch?", "Confirm timer", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Gym",
                armed = true,
                needsSetup = true,
                registeredPlaceCount = 3,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Office", "1:30", "Still counting", "Timer"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                needsSetup = true,
                registeredPlaceCount = 3,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Office", "Needs setup", "Finish setup on phone", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Office",
                armed = true,
                needsSetup = true,
                registeredPlaceCount = 3,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_NONE,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun promptAndTimerLabelsStayCompartmentalizedForMultiPlaceTileStates() {
        assertEquals(
            TileState("Switch to Gym?", "Switch?", "Confirm timer", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
                armed = true,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_START_TIMER,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Leaving Office?", "Keep?", "1:30 left", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
                armed = true,
                timerEnd = 91_000L,
                prompt = TileStateCalculator.PROMPT_LEAVE_EARLY,
                now = 1_000L,
            ),
        )
        assertEquals(
            TileState("Office", "Done", "Time's up", "Open"),
            TileStateCalculator.state(
                hasPlace = true,
                placeLabel = "Gym",
                promptPlaceLabel = "Gym",
                timerPlaceLabel = "Office",
                armed = true,
                timerEnd = 0L,
                prompt = TileStateCalculator.PROMPT_TIME_UP,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun staleBoundaryIsExactAtTwoMinutes() {
        assertEquals(false, TileStateCalculator.isPhoneStateStale(lastUpdated = 1_000L, now = 120_999L))
        assertEquals(true, TileStateCalculator.isPhoneStateStale(lastUpdated = 1_000L, now = 121_000L))
        assertEquals(
            "Synced just now",
            WatchSyncCopy.syncText(lastUpdated = 1_000L, now = 120_999L, activeTimer = false),
        )
        assertEquals(
            "Phone not nearby",
            WatchSyncCopy.syncText(lastUpdated = 1_000L, now = 121_000L, activeTimer = false),
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

    @Test
    fun watchDataIgnoresOlderIncomingPhoneState() {
        assertEquals(
            true,
            WatchDataService.shouldApplyIncomingState(
                previousUpdated = 0L,
                incomingUpdated = 0L,
            ),
        )
        assertEquals(
            true,
            WatchDataService.shouldApplyIncomingState(
                previousUpdated = 1_000L,
                incomingUpdated = 2_000L,
            ),
        )
        assertEquals(
            false,
            WatchDataService.shouldApplyIncomingState(
                previousUpdated = 2_000L,
                incomingUpdated = 1_000L,
            ),
        )
        assertEquals(
            false,
            WatchDataService.shouldApplyIncomingState(
                previousUpdated = 2_000L,
                incomingUpdated = 2_000L,
            ),
        )
        assertEquals(
            false,
            WatchDataService.shouldApplyIncomingState(
                previousUpdated = 2_000L,
                incomingUpdated = 0L,
            ),
        )
    }
}
