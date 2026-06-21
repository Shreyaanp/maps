package work.shreyaan.dwell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Geofences and alarms can be lost across reboot or app update — re-register both.
 */
class BootReceiver : BroadcastReceiver() {
    internal enum class RunningTimerRecoveryAction {
        RunningTimer,
        LeavePrompt,
        SwitchPrompt,
        ClearPromptAndRun,
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val end = Prefs.getTimerEnd(context)
        val now = System.currentTimeMillis()
        if (end > now) {
            val prompt = Prefs.getWatchPrompt(context)
            val promptPlaceId = Prefs.getPromptPlaceId(context)
            val timerPlaceId = Prefs.getTimerPlaceId(context)
            val recoveryAction = runningTimerRecoveryAction(
                prompt = prompt,
                promptPlaceId = promptPlaceId,
                timerPlaceId = timerPlaceId,
                promptPlaceExists = Prefs.hasSavedPlaceId(context, promptPlaceId),
            )
            if (recoveryAction == RunningTimerRecoveryAction.ClearPromptAndRun) {
                Prefs.clearWatchPrompt(context)
            }
            TimerController.scheduleAlarm(context, end)
            Notifications.notifyTimerRunning(context, end)
            when (recoveryAction) {
                RunningTimerRecoveryAction.LeavePrompt -> {
                    Notifications.notifyExitQuestion(context, end)
                }
                RunningTimerRecoveryAction.SwitchPrompt -> {
                    val newPlace = Prefs.getPlace(context, promptPlaceId)
                    val currentPlace = Prefs.getPlace(context, timerPlaceId)
                    Notifications.notifySwitchPlaceQuestion(
                        context,
                        newPlaceLabel = newPlace?.safeLabel ?: "new place",
                        currentPlaceLabel = currentPlace?.safeLabel ?: "current timer",
                    )
                }
                RunningTimerRecoveryAction.RunningTimer,
                RunningTimerRecoveryAction.ClearPromptAndRun -> Unit
            }
            WearSync.pushState(context)
        } else if (end != 0L) {
            // Timer expired while the phone was off.
            Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_TIME_UP)
            Prefs.clearArrivalRuntime(context)
            DwellInsights.recordTimerFinished(context, DwellSessionOutcome.Completed)
            Prefs.setTimerEnd(context, 0L)
            Prefs.setTimerStartedAt(context, 0L)
            Notifications.notifyTimerDone(context, TimerController.completionDurationMinutes(context))
            WearSync.pushState(context)
        }

        val armedPlaces = Prefs.getArmedPlaces(context)
        if (Prefs.isArmed(context) && armedPlaces.isNotEmpty()) {
            val setupIssue = MonitoringPrerequisites.issueForContext(context)
            if (setupIssue == null) {
                DwellDiagnostics.logLifecycle(
                    context,
                    source = "boot",
                    decision = "refresh",
                    detail = "${armedPlaces.size} monitored place${if (armedPlaces.size == 1) "" else "s"} after boot/update",
                )
                val pending = goAsync()
                GeofenceManager.refresh(context) { _, _ ->
                    pending.finish()
                }
            } else {
                MonitoringPrerequisites.markSetupNeeded(
                    context,
                    source = "boot",
                    issue = setupIssue,
                )
            }
        }
    }

    companion object {
        internal fun runningTimerRecoveryAction(
            prompt: String,
            promptPlaceId: String,
            timerPlaceId: String,
            promptPlaceExists: Boolean,
        ): RunningTimerRecoveryAction =
            when (prompt) {
                Prefs.WATCH_PROMPT_LEAVE_EARLY -> {
                    if (promptPlaceId.isBlank() || promptPlaceId == timerPlaceId) {
                        RunningTimerRecoveryAction.LeavePrompt
                    } else {
                        RunningTimerRecoveryAction.ClearPromptAndRun
                    }
                }
                Prefs.WATCH_PROMPT_START_TIMER -> {
                    if (
                        promptPlaceId.isNotBlank() &&
                        promptPlaceId != timerPlaceId &&
                        promptPlaceExists
                    ) {
                        RunningTimerRecoveryAction.SwitchPrompt
                    } else {
                        RunningTimerRecoveryAction.ClearPromptAndRun
                    }
                }
                else -> RunningTimerRecoveryAction.RunningTimer
            }

        internal fun monitoringSetupIssue(
            hasLocation: Boolean,
            hasBackgroundLocation: Boolean,
            hasNotifications: Boolean,
            hasMotion: Boolean,
        ): MonitoringPrerequisites.SetupIssue? =
            MonitoringPrerequisites.issueFor(
                hasLocation = hasLocation,
                hasBackgroundLocation = hasBackgroundLocation,
                hasNotifications = hasNotifications,
                hasMotion = hasMotion,
            )
    }
}
