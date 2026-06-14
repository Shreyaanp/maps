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
                TimerController.cancelTimer(this)
                Notifications.notifyTimerCancelled(this)
            }
            "/dwell/start" -> {
                val prompt = Prefs.getWatchPrompt(this)
                val promptUpdated = Prefs.getWatchPromptUpdated(this)
                val commandPromptUpdated = promptUpdatedFrom(event)
                val placeId = Prefs.getPromptPlaceId(this).ifBlank { null }
                val timerRunning = TimerController.isRunning(this)
                val timerPlaceId = Prefs.getTimerPlaceId(this)

                when (
                    watchStartAction(
                        prompt = prompt,
                        promptPlaceId = placeId,
                        currentPromptUpdated = promptUpdated,
                        commandPromptUpdated = commandPromptUpdated,
                        timerRunning = timerRunning,
                        timerPlaceId = timerPlaceId,
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
                if (
                    shouldApplyPromptCommand(
                        expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                        currentPrompt = Prefs.getWatchPrompt(this),
                        currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                        commandPromptUpdated = promptUpdatedFrom(event),
                    )
                ) {
                    Prefs.clearWatchPrompt(this)
                    Notifications.clearArrivalQuestion(this)
                }
                WearSync.pushState(this)
            }
            "/dwell/keep" -> {
                if (
                    shouldApplyPromptCommand(
                        expectedPrompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                        currentPrompt = Prefs.getWatchPrompt(this),
                        currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                        commandPromptUpdated = promptUpdatedFrom(event),
                    )
                ) {
                    Prefs.clearWatchPrompt(this)
                    Notifications.clearExitQuestion(this)
                }
                WearSync.pushState(this)
            }
            "/dwell/done" -> {
                val action = watchDoneAction(
                    prompt = Prefs.getWatchPrompt(this),
                    currentPromptUpdated = Prefs.getWatchPromptUpdated(this),
                    commandPromptUpdated = promptUpdatedFrom(event),
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
                val minutes = event.data
                    ?.toString(Charsets.UTF_8)
                    ?.toIntOrNull()
                    ?: 30
                TimerController.extendTimer(this, minutes)
            }
        }
    }

    private fun promptUpdatedFrom(event: MessageEvent): Long? =
        event.data
            ?.toString(Charsets.UTF_8)
            ?.toLongOrNull()

    internal enum class WatchStartAction {
        Ignore,
        ClearMatchingRunningTimerPrompt,
        StartPromptedPlace,
    }

    internal enum class WatchDoneAction {
        Ignore,
        Apply,
    }

    companion object {
        internal fun watchStartAction(
            prompt: String,
            promptPlaceId: String?,
            currentPromptUpdated: Long,
            commandPromptUpdated: Long?,
            timerRunning: Boolean,
            timerPlaceId: String,
        ): WatchStartAction {
            if (prompt != Prefs.WATCH_PROMPT_START_TIMER) return WatchStartAction.Ignore
            if (
                !shouldApplyPromptCommand(
                    expectedPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                    currentPrompt = prompt,
                    currentPromptUpdated = currentPromptUpdated,
                    commandPromptUpdated = commandPromptUpdated,
                )
            ) {
                return WatchStartAction.Ignore
            }

            if (
                timerRunning &&
                (promptPlaceId.isNullOrBlank() || promptPlaceId == timerPlaceId)
            ) {
                return WatchStartAction.ClearMatchingRunningTimerPrompt
            }

            return WatchStartAction.StartPromptedPlace
        }

        internal fun shouldApplyPromptCommand(
            expectedPrompt: String,
            currentPrompt: String,
            currentPromptUpdated: Long,
            commandPromptUpdated: Long?,
        ): Boolean =
            currentPrompt == expectedPrompt &&
                currentPromptUpdated > 0L &&
                commandPromptUpdated == currentPromptUpdated

        internal fun watchDoneAction(
            prompt: String,
            currentPromptUpdated: Long,
            commandPromptUpdated: Long?,
            timerEnd: Long,
            now: Long,
        ): WatchDoneAction {
            if (
                shouldApplyPromptCommand(
                    expectedPrompt = Prefs.WATCH_PROMPT_TIME_UP,
                    currentPrompt = prompt,
                    currentPromptUpdated = currentPromptUpdated,
                    commandPromptUpdated = commandPromptUpdated,
                )
            ) {
                return WatchDoneAction.Apply
            }

            return if (timerEnd in 1..now) {
                WatchDoneAction.Apply
            } else {
                WatchDoneAction.Ignore
            }
        }
    }
}
