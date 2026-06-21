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
                promptPlaceId = "",
                currentPromptUpdated = 0L,
                command = null,
                timerRunning = false,
                timerPlaceId = "",
                timerStartedAt = 0L,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "office",
                currentPromptUpdated = 10L,
                command = promptCommand(
                    prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                    promptUpdated = 10L,
                    promptPlaceId = "office",
                ),
                timerRunning = false,
                timerPlaceId = "",
                timerStartedAt = 0L,
                timerEnd = 0L,
                now = 1_000L,
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
                command = promptCommand(promptPlaceId = "office"),
                timerRunning = true,
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 10_000L,
                now = 5_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.ClearMatchingRunningTimerPrompt,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "",
                currentPromptUpdated = 42L,
                command = promptCommand(promptPlaceId = ""),
                timerRunning = true,
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 10_000L,
                now = 5_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                currentPromptUpdated = 42L,
                command = promptCommand(
                    promptPlaceId = "office",
                    timerPlaceId = "home",
                    timerStartedAt = 2_000L,
                    timerEnd = 11_000L,
                ),
                timerRunning = true,
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 10_000L,
                now = 5_000L,
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
                command = promptCommand(promptPlaceId = "office"),
                timerRunning = false,
                timerPlaceId = "",
                timerStartedAt = 0L,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.StartPromptedPlace,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                currentPromptUpdated = 42L,
                command = promptCommand(
                    promptPlaceId = "gym",
                    timerPlaceId = "office",
                    timerStartedAt = 1_000L,
                    timerEnd = 10_000L,
                ),
                timerRunning = true,
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 10_000L,
                now = 5_000L,
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
                command = null,
                timerRunning = false,
                timerPlaceId = "",
                timerStartedAt = 0L,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                currentPromptUpdated = 42L,
                command = promptCommand(promptUpdated = 41L, promptPlaceId = "office"),
                timerRunning = false,
                timerPlaceId = "",
                timerStartedAt = 0L,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun watchStartRejectsSwitchPromptWhenTimerIdentityIsStale() {
        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                currentPromptUpdated = 42L,
                command = promptCommand(
                    promptPlaceId = "gym",
                    timerPlaceId = "office",
                    timerStartedAt = 1_000L,
                    timerEnd = 10_000L,
                ),
                timerRunning = true,
                timerPlaceId = "home",
                timerStartedAt = 2_000L,
                timerEnd = 11_000L,
                now = 5_000L,
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
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                command = promptCommand(promptPlaceId = "office"),
            ),
        )
        assertEquals(
            false,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                command = promptCommand(promptPlaceId = "office"),
            ),
        )
        assertEquals(
            false,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                command = promptCommand(promptUpdated = 43L, promptPlaceId = "office"),
            ),
        )
        assertEquals(
            false,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                command = promptCommand(promptPlaceId = "gym"),
            ),
        )
        assertEquals(
            false,
            PhoneDataService.shouldApplyPromptCommand(
                expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                command = promptCommand(promptPlaceId = "gym"),
            ),
        )
    }

    @Test
    fun watchKeepSuppressesExitPromptForPromptPlaceThenTimerPlace() {
        assertEquals(
            "gym",
            PhoneDataService.exitKeepSuppressionPlaceId(
                promptPlaceId = "gym",
                timerPlaceId = "office",
            ),
        )
        assertEquals(
            "office",
            PhoneDataService.exitKeepSuppressionPlaceId(
                promptPlaceId = "",
                timerPlaceId = "office",
            ),
        )
        assertEquals(
            null,
            PhoneDataService.exitKeepSuppressionPlaceId(
                promptPlaceId = null,
                timerPlaceId = "",
            ),
        )
    }

    @Test
    fun doneCommandDoesNotClearNewRunningTimerFromStalePrompt() {
        assertEquals(
            PhoneDataService.WatchDoneAction.Ignore,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptPlaceId = "",
                currentPromptUpdated = 50L,
                promptCommand = promptCommand(
                    prompt = Prefs.WATCH_PROMPT_TIME_UP,
                    promptUpdated = 42L,
                    timerPlaceId = "office",
                ),
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
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                promptCommand = promptCommand(
                    prompt = Prefs.WATCH_PROMPT_TIME_UP,
                    promptPlaceId = "",
                    timerPlaceId = "office",
                ),
                currentPlaceId = "office",
                timerEnd = 120_000L,
                now = 60_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchDoneAction.Apply,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptPlaceId = "",
                currentPromptUpdated = 50L,
                promptCommand = null,
                timerCommand = PhoneDataService.WatchTimerCommand(
                    placeId = "office",
                    startedAt = 1_000L,
                    end = 59_000L,
                    minutes = null,
                ),
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                timerEnd = 59_000L,
                now = 60_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchDoneAction.Apply,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                promptCommand = promptCommand(
                    prompt = Prefs.WATCH_PROMPT_TIME_UP,
                    promptPlaceId = "",
                    timerPlaceId = "",
                ),
                currentPlaceId = "",
                timerEnd = 120_000L,
                now = 60_000L,
            ),
        )
    }

    @Test
    fun doneCommandRequiresMatchingTimerTokenForExpiredTimer() {
        assertEquals(
            PhoneDataService.WatchDoneAction.Ignore,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptPlaceId = "",
                currentPromptUpdated = 50L,
                promptCommand = null,
                timerCommand = null,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                timerEnd = 59_000L,
                now = 60_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchDoneAction.Ignore,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptPlaceId = "",
                currentPromptUpdated = 50L,
                promptCommand = null,
                timerCommand = PhoneDataService.WatchTimerCommand(
                    placeId = "gym",
                    startedAt = 1_000L,
                    end = 59_000L,
                    minutes = null,
                ),
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                timerEnd = 59_000L,
                now = 60_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchDoneAction.Ignore,
            PhoneDataService.watchDoneAction(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                promptCommand = promptCommand(
                    prompt = Prefs.WATCH_PROMPT_TIME_UP,
                    promptPlaceId = "",
                    timerPlaceId = "gym",
                ),
                currentPlaceId = "office",
                timerEnd = 59_000L,
                now = 60_000L,
            ),
        )
    }

    @Test
    fun scopedTimerCommandPayloadParsesTimerIdentityAndMinutes() {
        assertEquals(
            PhoneDataService.WatchTimerCommand(
                placeId = "office",
                startedAt = 1_000L,
                end = 2_000L,
                minutes = 30,
            ),
            PhoneDataService.parseWatchTimerCommandPayload("office|1000|2000|30"),
        )
        assertEquals(
            PhoneDataService.WatchTimerCommand(
                placeId = "office",
                startedAt = 1_000L,
                end = 2_000L,
                minutes = null,
            ),
            PhoneDataService.parseWatchTimerCommandPayload("office|1000|2000|"),
        )
    }

    @Test
    fun promptCommandPayloadParsesPromptPlaceAndTimerIdentity() {
        assertEquals(
            PhoneDataService.WatchPromptCommand(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptUpdated = 42L,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 2_000L,
            ),
            PhoneDataService.parseWatchPromptCommandPayload("prompt|start_timer|42|gym|office|1000|2000"),
        )
        assertEquals(
            PhoneDataService.WatchPromptCommand(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptUpdated = 42L,
                promptPlaceId = "",
                timerPlaceId = "",
                timerStartedAt = 0L,
                timerEnd = 0L,
            ),
            PhoneDataService.parseWatchPromptCommandPayload("prompt|time_up|42|||0|0"),
        )
    }

    @Test
    fun promptCommandPayloadRejectsLegacyOrInvalidPromptIdentity() {
        assertEquals(null, PhoneDataService.parseWatchPromptCommandPayload(null))
        assertEquals(null, PhoneDataService.parseWatchPromptCommandPayload("42"))
        assertEquals(null, PhoneDataService.parseWatchPromptCommandPayload("prompt|start_timer|0|office||0|0"))
        assertEquals(null, PhoneDataService.parseWatchPromptCommandPayload("prompt||42|office||0|0"))
        assertEquals(null, PhoneDataService.parseWatchPromptCommandPayload("prompt|start_timer|42|office||bad|0"))
    }

    @Test
    fun timeUpExtendPayloadParsesPromptTokenPlaceAndMinutes() {
        assertEquals(
            PhoneDataService.WatchTimeUpExtendCommand(
                promptUpdated = 42L,
                placeId = "office",
                minutes = 30,
            ),
            PhoneDataService.parseWatchTimeUpExtendCommandPayload("time_up_extend|42|office|30"),
        )
        assertEquals(
            PhoneDataService.WatchTimeUpExtendCommand(
                promptUpdated = 42L,
                placeId = "",
                minutes = 30,
            ),
            PhoneDataService.parseWatchTimeUpExtendCommandPayload("time_up_extend|42||30"),
        )
    }

    @Test
    fun timeUpExtendPayloadRejectsInvalidOrMissingToken() {
        assertEquals(null, PhoneDataService.parseWatchTimeUpExtendCommandPayload(null))
        assertEquals(null, PhoneDataService.parseWatchTimeUpExtendCommandPayload("office|1000|2000|30"))
        assertEquals(null, PhoneDataService.parseWatchTimeUpExtendCommandPayload("time_up_extend|0|office|30"))
        assertEquals(null, PhoneDataService.parseWatchTimeUpExtendCommandPayload("time_up_extend|42|office|bad"))
    }

    @Test
    fun scopedTimerCommandAllowsBlankPlaceForNoPlaceTimer() {
        val command = PhoneDataService.WatchTimerCommand(
            placeId = "",
            startedAt = 1_000L,
            end = 2_000L,
            minutes = 30,
        )

        assertEquals(
            command,
            PhoneDataService.parseWatchTimerCommandPayload("|1000|2000|30"),
        )
        assertEquals(
            true,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
    }

    @Test
    fun scopedTimerCommandRejectsMissingOrInvalidIdentity() {
        assertEquals(null, PhoneDataService.parseWatchTimerCommandPayload(null))
        assertEquals(null, PhoneDataService.parseWatchTimerCommandPayload(""))
        assertEquals(null, PhoneDataService.parseWatchTimerCommandPayload("office|bad|2000|30"))
        assertEquals(null, PhoneDataService.parseWatchTimerCommandPayload("office|1000"))
    }

    @Test
    fun scopedTimerCommandOnlyMatchesCurrentRunningTimer() {
        val command = PhoneDataService.WatchTimerCommand(
            placeId = "office",
            startedAt = 1_000L,
            end = 2_000L,
            minutes = null,
        )

        assertEquals(
            true,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "gym",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "office",
                currentStartedAt = 1_001L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 2_100L,
                now = 1_500L,
            ),
        )
        assertEquals(
            false,
            PhoneDataService.watchTimerCommandMatches(
                command = command,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 2_000L,
            ),
        )
    }

    @Test
    fun extendCommandActionHandlesRunningExpiredAndTimeUpPrompt() {
        val timerCommand = PhoneDataService.WatchTimerCommand(
            placeId = "office",
            startedAt = 1_000L,
            end = 2_000L,
            minutes = 30,
        )

        assertEquals(
            PhoneDataService.WatchExtendAction.ExtendRunningTimer,
            PhoneDataService.watchExtendAction(
                command = timerCommand,
                timeUpCommand = null,
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptUpdated = 0L,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchExtendAction.StartExpiredTimer,
            PhoneDataService.watchExtendAction(
                command = timerCommand,
                timeUpCommand = null,
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptUpdated = 0L,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 2_500L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchExtendAction.StartTimeUpPrompt,
            PhoneDataService.watchExtendAction(
                command = null,
                timeUpCommand = PhoneDataService.WatchTimeUpExtendCommand(
                    promptUpdated = 42L,
                    placeId = "office",
                    minutes = 30,
                ),
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptUpdated = 42L,
                currentPlaceId = "office",
                currentStartedAt = 0L,
                currentEnd = 0L,
                now = 2_500L,
            ),
        )
    }

    @Test
    fun extendCommandActionHandlesNoPlaceTimerIdentity() {
        val timerCommand = PhoneDataService.WatchTimerCommand(
            placeId = "",
            startedAt = 1_000L,
            end = 2_000L,
            minutes = 30,
        )

        assertEquals(
            PhoneDataService.WatchExtendAction.ExtendRunningTimer,
            PhoneDataService.watchExtendAction(
                command = timerCommand,
                timeUpCommand = null,
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptUpdated = 0L,
                currentPlaceId = "",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 1_500L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchExtendAction.StartExpiredTimer,
            PhoneDataService.watchExtendAction(
                command = timerCommand,
                timeUpCommand = null,
                prompt = Prefs.WATCH_PROMPT_NONE,
                currentPromptUpdated = 0L,
                currentPlaceId = "",
                currentStartedAt = 1_000L,
                currentEnd = 2_000L,
                now = 2_500L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchExtendAction.StartTimeUpPrompt,
            PhoneDataService.watchExtendAction(
                command = null,
                timeUpCommand = PhoneDataService.WatchTimeUpExtendCommand(
                    promptUpdated = 42L,
                    placeId = "",
                    minutes = 30,
                ),
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptUpdated = 42L,
                currentPlaceId = "",
                currentStartedAt = 0L,
                currentEnd = 0L,
                now = 2_500L,
            ),
        )
    }

    @Test
    fun extendCommandActionRejectsStaleTimeUpPromptOrWrongPlace() {
        assertEquals(
            PhoneDataService.WatchExtendAction.Ignore,
            PhoneDataService.watchExtendAction(
                command = null,
                timeUpCommand = PhoneDataService.WatchTimeUpExtendCommand(
                    promptUpdated = 41L,
                    placeId = "office",
                    minutes = 30,
                ),
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptUpdated = 42L,
                currentPlaceId = "office",
                currentStartedAt = 0L,
                currentEnd = 0L,
                now = 2_500L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchExtendAction.Ignore,
            PhoneDataService.watchExtendAction(
                command = null,
                timeUpCommand = PhoneDataService.WatchTimeUpExtendCommand(
                    promptUpdated = 42L,
                    placeId = "gym",
                    minutes = 30,
                ),
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptUpdated = 42L,
                currentPlaceId = "office",
                currentStartedAt = 0L,
                currentEnd = 0L,
                now = 2_500L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchExtendAction.Ignore,
            PhoneDataService.watchExtendAction(
                command = null,
                timeUpCommand = PhoneDataService.WatchTimeUpExtendCommand(
                    promptUpdated = 42L,
                    placeId = "office",
                    minutes = 30,
                ),
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptUpdated = 42L,
                currentPlaceId = "office",
                currentStartedAt = 1_000L,
                currentEnd = 10_000L,
                now = 2_500L,
            ),
        )
    }

    private fun promptCommand(
        prompt: String = Prefs.WATCH_PROMPT_START_TIMER,
        promptUpdated: Long = 42L,
        promptPlaceId: String = "",
        timerPlaceId: String = "",
        timerStartedAt: Long = 0L,
        timerEnd: Long = 0L,
    ): PhoneDataService.WatchPromptCommand =
        PhoneDataService.WatchPromptCommand(
            prompt = prompt,
            promptUpdated = promptUpdated,
            promptPlaceId = promptPlaceId,
            timerPlaceId = timerPlaceId,
            timerStartedAt = timerStartedAt,
            timerEnd = timerEnd,
        )
}
