package work.shreyaan.dwell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Geofences and alarms can be lost across reboot or app update — re-register both.
 */
class BootReceiver : BroadcastReceiver() {
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
            TimerController.scheduleAlarm(context, end)
            Notifications.notifyTimerRunning(context, end)
            WearSync.pushState(context)
        } else if (end != 0L) {
            // Timer expired while the phone was off.
            Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_TIME_UP)
            Prefs.clearArrivalRuntime(context)
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
