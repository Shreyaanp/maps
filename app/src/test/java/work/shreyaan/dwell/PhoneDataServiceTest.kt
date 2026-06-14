package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneDataServiceTest {
    @Test
    fun watchStartIgnoresStaleCommandWhenNoStartPromptIsLive() {
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = null,
                currentPromptUpdated = 0L,
                commandPromptUpdated = null,
                timerRunning = false,
                timerPlaceId = "",
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "office",
                currentPromptUpdated = 10L,
                commandPromptUpdated = 10L,
                timerRunning = false,
                timerPlaceId = "",
            ),
        )
    }

    @Test
    fun watchStartClearsPromptWhenTimerAlreadyRunsForSamePlace() {
        assertEquals(
            PhoneDataService.WatchStartAction.ClearMatchingRunningTimerPrompt,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
                timerRunning = true,
                timerPlaceId = "office",
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.ClearMatchingRunningTimerPrompt,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = null,
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
                timerRunning = true,
                timerPlaceId = "office",
            ),
        )
    }

    @Test
    fun watchStartAllowsLiveStartOrSwitchPrompt() {
        assertEquals(
            PhoneDataService.WatchStartAction.StartPromptedPlace,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
                timerRunning = false,
                timerPlaceId = "",
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.StartPromptedPlace,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
                timerRunning = true,
                timerPlaceId = "office",
            ),
        )
    }

    @Test
    fun watchStartRejectsMissingOrStalePromptToken() {
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                currentPromptUpdated = 42L,
                commandPromptUpdated = null,
                timerRunning = false,
                timerPlaceId = "",
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                currentPromptUpdated = 42L,
                commandPromptUpdated = 41L,
                timerRunning = false,
                timerPlaceId = "",
            ),
        )
    }

    @Test
    fun promptCommandsRequireMatchingPromptAndToken() {
        assertEquals(
            true,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptUpdated = 42L,
                commandPromptUpdated = 43L,
            ),
        )
    }

    @Test
    fun doneCommandDoesNotClearNewRunningTimerFromStalePrompt() {
        assertEquals(
            PhoneDataService.WatchDoneAction.Ignore,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptUpdated = 50L,
                commandPromptUpdated = 42L,
                timerEnd = 120_000L,
                now = 60_000L,
            ),
        )
    }

    @Test
    fun doneCommandAppliesForMatchingTimeUpPromptOrExpiredTimer() {
        assertEquals(
            PhoneDataService.WatchDoneAction.Apply,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptUpdated = 42L,
                commandPromptUpdated = 42L,
                timerEnd = 120_000L,
                now = 60_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchDoneAction.Apply,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptUpdated = 50L,
                commandPromptUpdated = null,
                timerEnd = 59_000L,
                now = 60_000L,
            ),
        )
    }
}
