package work.shreyaan.dwell

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Handles commands sent from the watch app.
 */
class PhoneDataService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/dwell/request_state" -> {
                WearSync.pushState(this)
            }
            "/dwell/cancel" -> {
                val command = watchTimerCommandFrom(event)
                if (
                    watchTimerCommandMatches(
                        command = command,
                        currentPlaceId = Prefs.getTimerPlaceId(this),
                        currentStartedAt = Prefs.getTimerStartedAt(this),
                        currentEnd = Prefs.getTimerEnd(this),
                        now = System.currentTimeMillis(),
                    )
                ) {
                    TimerController.cancelTimer(this)
                    Notifications.notifyTimerCancelled(this)
                } else {
                    WearSync.pushState(this)
                }
            }
            "/dwell/start" -> {
                val prompt = Prefs.getWatchPrompt(this)
                val promptUpdated = Prefs.getWatchPromptUpdated(this)
                val command = watchPromptCommandFrom(event)
                val promptPlaceId = Prefs.getPromptPlaceId(this)
                val placeId = promptPlaceId.ifBlank { null }
                val timerRunning = TimerController.isRunning(this)
                val timerPlaceId = Prefs.getTimerPlaceId(this)
                val timerStartedAt = Prefs.getTimerStartedAt(this)
                val timerEnd = Prefs.getTimerEnd(this)
                val now = System.currentTimeMillis()

                when (
                    watchStartAction(
                        prompt = prompt,
                        promptPlaceId = promptPlaceId,
                        currentPromptUpdated = promptUpdated,
                        command = command,
                        timerRunning = timerRunning,
                        timerPlaceId = timerPlaceId,
                        timerStartedAt = timerStartedAt,
                        timerEnd = timerEnd,
                        now = now,
                    )
                ) {
                    WatchStartAction.Ignore -> {
                        WearSync.pushState(this)
                        return
                    }
                    WatchStartAction.ClearMatchingRunningTimerPrompt -> {
                        Prefs.clearWatchPrompt(this)
                        Notifications.clearArrivalQuestion(this)
                        WearSync.pushState(this)
                        return
                    }
                    WatchStartAction.StartPromptedPlace -> Unit
                }

                if (
                    !Prefs.promptPlaceStillExists(
                        placeId = placeId,
                        placeExists = Prefs.hasSavedPlaceId(this, placeId),
                    )
                ) {
                    Prefs.clearWatchPrompt(this)
                    Notifications.clearArrivalQuestion(this)
                    WearSync.pushState(this)
                    return
                }

                if (timerRunning) {
                    TimerController.cancelTimer(this)
                }

                Notifications.clearArrivalQuestion(this)
                TimerController.startTimer(
                    this,
                    Prefs.getDurationMinutes(this, placeId),
                    placeId,
                )
            }
            "/dwell/dismiss_arrival" -> {
                val promptPlaceId = Prefs.getPromptPlaceId(this)
                val command = watchPromptCommandFrom(event)
                val timerEnd = Prefs.getTimerEnd(this)
                val timerRunning = TimerController.isRunning(this)
                if (
                    shouldApplyPromptCommand(
                        expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                        currentPrompt = Prefs.getWatchPrompt(this),
                        currentPromptPlaceId = promptPlaceId,
                        currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                        command = command,
                    )
                ) {
                    if (timerRunning) {
                        if (
                            !watchPromptTimerCommandMatches(
                                command = command,
                                currentPlaceId = Prefs.getTimerPlaceId(this),
                                currentStartedAt = Prefs.getTimerStartedAt(this),
                                currentEnd = timerEnd,
                                now = System.currentTimeMillis(),
                            )
                        ) {
                            WearSync.pushState(this)
                            return
                        }
                        Prefs.markSwitchPromptKept(
                            this,
                            targetPlaceId = promptPlaceId,
                            untilMillis = timerEnd,
                        )
                    } else if (watchPromptCommandHasTimerIdentity(command)) {
                        WearSync.pushState(this)
                        return
                    }
                    Prefs.clearWatchPrompt(this)
                    Notifications.clearArrivalQuestion(this)
                }
                WearSync.pushState(this)
            }
            "/dwell/keep" -> {
                val promptPlaceId = Prefs.getPromptPlaceId(this)
                val command = watchPromptCommandFrom(event)
                val timerEnd = Prefs.getTimerEnd(this)
                if (
                    shouldApplyPromptCommand(
                        expectedPrompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                        currentPrompt = Prefs.getWatchPrompt(this),
                        currentPromptPlaceId = promptPlaceId,
                        currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                        command = command,
                    ) &&
                    watchPromptTimerCommandMatches(
                        command = command,
                        currentPlaceId = Prefs.getTimerPlaceId(this),
                        currentStartedAt = Prefs.getTimerStartedAt(this),
                        currentEnd = timerEnd,
                        now = System.currentTimeMillis(),
                    )
                ) {
                    Prefs.markExitPromptKept(
                        this,
                        placeId = exitKeepSuppressionPlaceId(
                            promptPlaceId = promptPlaceId,
                            timerPlaceId = Prefs.getTimerPlaceId(this),
                        ),
                        untilMillis = timerEnd,
                    )
                    Prefs.clearWatchPrompt(this)
                    Notifications.clearExitQuestion(this)
                }
                WearSync.pushState(this)
            }
            "/dwell/done" -> {
                val action = watchDoneAction(
                    prompt = Prefs.getWatchPrompt(this),
                    currentPromptPlaceId = Prefs.getPromptPlaceId(this),
                    currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                    promptCommand = watchPromptCommandFrom(event),
                    timerCommand = watchTimerCommandFrom(event),
                    currentPlaceId = Prefs.getTimerPlaceId(this),
                    currentStartedAt = Prefs.getTimerStartedAt(this),
                    timerEnd = Prefs.getTimerEnd(this),
                    now = System.currentTimeMillis(),
                )
                if (action == WatchDoneAction.Apply) {
                    Prefs.clearWatchPrompt(this)
                    TimerController.clearCompletedTimer(this)
                    Notifications.clearAll(this)
                } else {
                    WearSync.pushState(this)
                }
            }
            "/dwell/extend" -> {
                val command = watchTimerCommandFrom(event)
                val timeUpCommand = watchTimeUpExtendCommandFrom(event)
                when (
                    watchExtendAction(
                        command = command,
                        timeUpCommand = timeUpCommand,
                        prompt = Prefs.getWatchPrompt(this),
                        currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                        currentPlaceId = Prefs.getTimerPlaceId(this),
                        currentStartedAt = Prefs.getTimerStartedAt(this),
                        currentEnd = Prefs.getTimerEnd(this),
                        now = System.currentTimeMillis(),
                    )
                ) {
                    WatchExtendAction.ExtendRunningTimer -> {
                        TimerController.extendTimer(this, command?.minutes ?: 30)
                    }
                    WatchExtendAction.StartExpiredTimer -> {
                        TimerController.startTimer(
                            this,
                            command?.minutes ?: 30,
                            command?.placeId,
                            allowActivePlaceFallback = false,
                        )
                    }
                    WatchExtendAction.StartTimeUpPrompt -> {
                        val placeId = timeUpCommand?.placeId.orEmpty()
                        if (
                            Prefs.promptPlaceStillExists(
                                placeId = placeId,
                                placeExists = Prefs.hasSavedPlaceId(this, placeId),
                            )
                        ) {
                            TimerController.startTimer(
                                this,
                                timeUpCommand?.minutes ?: 30,
                                placeId,
                                allowActivePlaceFallback = false,
                            )
                        } else {
                            Prefs.clearWatchPrompt(this)
                            Notifications.clearAll(this)
                            WearSync.pushState(this)
                        }
                    }
                    WatchExtendAction.Ignore -> {
                        WearSync.pushState(this)
                    }
                }
            }
        }
    }

    private fun watchPromptCommandFrom(event: MessageEvent): WatchPromptCommand? =
        parseWatchPromptCommandPayload(event.data?.toString(Charsets.UTF_8))

    private fun watchTimerCommandFrom(event: MessageEvent): WatchTimerCommand? =
        parseWatchTimerCommandPayload(event.data?.toString(Charsets.UTF_8))

    private fun watchTimeUpExtendCommandFrom(event: MessageEvent): WatchTimeUpExtendCommand? =
        parseWatchTimeUpExtendCommandPayload(event.data?.toString(Charsets.UTF_8))

    internal data class WatchTimerCommand(
        val placeId: String,
        val startedAt: Long,
        val end: Long,
        val minutes: Int?,
    )

    internal data class WatchPromptCommand(
        val prompt: String,
        val promptUpdated: Long,
        val promptPlaceId: String,
        val timerPlaceId: String,
        val timerStartedAt: Long,
        val timerEnd: Long,
    )

    internal data class WatchTimeUpExtendCommand(
        val promptUpdated: Long,
        val placeId: String,
        val minutes: Int,
    )

    internal enum class WatchStartAction {
        Ignore,
        ClearMatchingRunningTimerPrompt,
        StartPromptedPlace,
    }

    internal enum class WatchDoneAction {
        Ignore,
        Apply,
    }

    internal enum class WatchExtendAction {
        Ignore,
        ExtendRunningTimer,
        StartExpiredTimer,
        StartTimeUpPrompt,
    }

    companion object {
        internal fun watchStartAction(
            prompt: String,
            promptPlaceId: String,
            currentPromptUpdated: Long,
            command: WatchPromptCommand?,
            timerRunning: Boolean,
            timerPlaceId: String,
            timerStartedAt: Long,
            timerEnd: Long,
            now: Long,
        ): WatchStartAction {
            if (prompt != Prefs.WATCH_PROMPT_START_TIMER) return WatchStartAction.Ignore
            if (
                !shouldApplyPromptCommand(
                    expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                    currentPrompt = prompt,
                    currentPromptPlaceId = promptPlaceId,
                    currentPromptUpdated = currentPromptUpdated,
                    command = command,
                )
            ) {
                return WatchStartAction.Ignore
            }

            if (
                timerRunning &&
                (promptPlaceId.isBlank() || promptPlaceId == timerPlaceId)
            ) {
                return if (
                    !watchPromptCommandHasTimerIdentity(command) ||
                    watchPromptTimerCommandMatches(
                        command = command,
                        currentPlaceId = timerPlaceId,
                        currentStartedAt = timerStartedAt,
                        currentEnd = timerEnd,
                        now = now,
                    )
                ) {
                    WatchStartAction.ClearMatchingRunningTimerPrompt
                } else {
                    WatchStartAction.Ignore
                }
            }

            if (
                timerRunning &&
                !watchPromptTimerCommandMatches(
                    command = command,
                    currentPlaceId = timerPlaceId,
                    currentStartedAt = timerStartedAt,
                    currentEnd = timerEnd,
                    now = now,
                )
            ) {
                return WatchStartAction.Ignore
            }

            if (!timerRunning && watchPromptCommandHasTimerIdentity(command)) {
                return WatchStartAction.Ignore
            }

            return WatchStartAction.StartPromptedPlace
        }

        internal fun shouldApplyPromptCommand(
            expectedPrompt: String,
            currentPrompt: String,
            currentPromptPlaceId: String,
            currentPromptUpdated: Long,
            command: WatchPromptCommand?,
        ): Boolean {
            if (command == null) return false
            if (command.prompt != expectedPrompt || currentPrompt != expectedPrompt) return false
            if (currentPromptUpdated <= 0L || command.promptUpdated != currentPromptUpdated) return false
            return if (currentPromptPlaceId.isBlank()) {
                command.promptPlaceId.isBlank()
            } else {
                command.promptPlaceId == currentPromptPlaceId
            }
        }

        internal fun parseWatchPromptCommandPayload(payload: String?): WatchPromptCommand? {
            val parts = payload
                ?.takeIf { it.isNotBlank() }
                ?.split("|")
                ?: return null
            if (parts.size != 7 || parts[0] != "prompt") return null
            val promptUpdated = parts[2].toLongOrNull() ?: return null
            val timerStartedAt = parts[5].toLongOrNull() ?: return null
            val timerEnd = parts[6].toLongOrNull() ?: return null
            if (parts[1].isBlank() || promptUpdated <= 0L || timerStartedAt < 0L || timerEnd < 0L) {
                return null
            }
            return WatchPromptCommand(
                prompt = parts[1],
                promptUpdated = promptUpdated,
                promptPlaceId = parts[3],
                timerPlaceId = parts[4],
                timerStartedAt = timerStartedAt,
                timerEnd = timerEnd,
            )
        }

        internal fun parseWatchTimerCommandPayload(payload: String?): WatchTimerCommand? {
            val parts = payload
                ?.takeIf { it.isNotBlank() }
                ?.split("|")
                ?: return null
            if (parts.size !in 3..4) return null
            val startedAt = parts[1].toLongOrNull() ?: return null
            val end = parts[2].toLongOrNull() ?: return null
            val minutes = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull()
            return WatchTimerCommand(
                placeId = parts[0],
                startedAt = startedAt,
                end = end,
                minutes = minutes,
            )
        }

        internal fun parseWatchTimeUpExtendCommandPayload(payload: String?): WatchTimeUpExtendCommand? {
            val parts = payload
                ?.takeIf { it.isNotBlank() }
                ?.split("|")
                ?: return null
            if (parts.size != 4 || parts[0] != "time_up_extend") return null
            val promptUpdated = parts[1].toLongOrNull() ?: return null
            val minutes = parts[3].toIntOrNull()?.coerceIn(1, 240) ?: return null
            if (promptUpdated <= 0L) return null
            return WatchTimeUpExtendCommand(
                promptUpdated = promptUpdated,
                placeId = parts[2],
                minutes = minutes,
            )
        }

        internal fun watchTimerCommandMatches(
            command: WatchTimerCommand?,
            currentPlaceId: String,
            currentStartedAt: Long,
            currentEnd: Long,
            now: Long,
            requireRunning: Boolean = true,
        ): Boolean {
            if (command == null || currentStartedAt <= 0L || currentEnd <= 0L) return false
            if (requireRunning && currentEnd <= now) return false
            return (
                command.placeId == currentPlaceId &&
                    command.startedAt == currentStartedAt &&
                    command.end == currentEnd
                )
        }

        internal fun watchPromptCommandHasTimerIdentity(command: WatchPromptCommand?): Boolean =
            command != null &&
                (
                    command.timerPlaceId.isNotBlank() ||
                        command.timerStartedAt > 0L ||
                        command.timerEnd > 0L
                    )

        internal fun watchPromptTimerCommandMatches(
            command: WatchPromptCommand?,
            currentPlaceId: String,
            currentStartedAt: Long,
            currentEnd: Long,
            now: Long,
            requireRunning: Boolean = true,
        ): Boolean {
            if (command == null || currentStartedAt <= 0L || currentEnd <= 0L) return false
            if (requireRunning && currentEnd <= now) return false
            return (
                command.timerPlaceId == currentPlaceId &&
                    command.timerStartedAt == currentStartedAt &&
                    command.timerEnd == currentEnd
                )
        }

        internal fun watchTimeUpExtendCommandMatches(
            command: WatchTimeUpExtendCommand?,
            currentPrompt: String,
            currentPromptUpdated: Long,
            currentPlaceId: String,
            currentEnd: Long,
            now: Long,
        ): Boolean {
            if (command == null || currentEnd > now) return false
            if (currentPrompt != Prefs.WATCH_PROMPT_TIME_UP || currentPromptUpdated <= 0L) {
                return false
            }
            if (command.promptUpdated != currentPromptUpdated) return false
            return if (currentPlaceId.isBlank()) {
                command.placeId.isBlank()
            } else {
                command.placeId == currentPlaceId
            }
        }

        internal fun watchExtendAction(
            command: WatchTimerCommand?,
            timeUpCommand: WatchTimeUpExtendCommand?,
            prompt: String,
            currentPromptUpdated: Long,
            currentPlaceId: String,
            currentStartedAt: Long,
            currentEnd: Long,
            now: Long,
        ): WatchExtendAction =
            when {
                watchTimerCommandMatches(
                    command = command,
                    currentPlaceId = currentPlaceId,
                    currentStartedAt = currentStartedAt,
                    currentEnd = currentEnd,
                    now = now,
                ) -> WatchExtendAction.ExtendRunningTimer
                currentEnd in 1..now &&
                    watchTimerCommandMatches(
                        command = command,
                        currentPlaceId = currentPlaceId,
                        currentStartedAt = currentStartedAt,
                        currentEnd = currentEnd,
                        now = now,
                        requireRunning = false,
                    ) -> WatchExtendAction.StartExpiredTimer
                watchTimeUpExtendCommandMatches(
                    command = timeUpCommand,
                    currentPrompt = prompt,
                    currentPromptUpdated = currentPromptUpdated,
                    currentPlaceId = currentPlaceId,
                    currentEnd = currentEnd,
                    now = now,
                ) -> WatchExtendAction.StartTimeUpPrompt
                else -> WatchExtendAction.Ignore
            }

        internal fun exitKeepSuppressionPlaceId(
            promptPlaceId: String?,
            timerPlaceId: String,
        ): String? =
            promptPlaceId?.takeIf { it.isNotBlank() }
                ?: timerPlaceId.takeIf { it.isNotBlank() }

        internal fun watchDoneAction(
            prompt: String,
            currentPromptPlaceId: String,
            currentPromptUpdated: Long,
            promptCommand: WatchPromptCommand?,
            timerCommand: WatchTimerCommand? = null,
            currentPlaceId: String = "",
            currentStartedAt: Long = 0L,
            timerEnd: Long,
            now: Long,
        ): WatchDoneAction {
            if (
                shouldApplyPromptCommand(
                    expectedPrompt = Prefs.WATCH_PROMPT_TIME_UP,
                    currentPrompt = prompt,
                    currentPromptPlaceId = currentPromptPlaceId,
                    currentPromptUpdated = currentPromptUpdated,
                    command = promptCommand,
                ) &&
                watchTimeUpPromptPlaceMatches(
                    command = promptCommand,
                    currentTimerPlaceId = currentPlaceId,
                )
            ) {
                return WatchDoneAction.Apply
            }

            return if (
                timerEnd in 1..now &&
                watchTimerCommandMatches(
                    command = timerCommand,
                    currentPlaceId = currentPlaceId,
                    currentStartedAt = currentStartedAt,
                    currentEnd = timerEnd,
                    now = now,
                    requireRunning = false,
                )
            ) {
                WatchDoneAction.Apply
            } else {
                WatchDoneAction.Ignore
            }
        }

        internal fun watchTimeUpPromptPlaceMatches(
            command: WatchPromptCommand?,
            currentTimerPlaceId: String,
        ): Boolean {
            if (command == null) return false
            return if (currentTimerPlaceId.isBlank()) {
                command.timerPlaceId.isBlank()
            } else {
                command.timerPlaceId == currentTimerPlaceId
            }
        }
    }
}
