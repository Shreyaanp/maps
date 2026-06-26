package work.shreyaan.dwell

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class MonitoringReliabilityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HEARTBEAT) return

        val appContext = context.applicationContext
        val armedPlaces = Prefs.getArmedPlaces(appContext)
        if (!shouldKeepHeartbeatScheduled(armedPlaces.size)) {
            cancel(appContext)
            return
        }

        val setupIssue = MonitoringPrerequisites.issueForContext(appContext)
        val timerRunning = TimerController.isRunning(appContext)
        if (
            !shouldRunHeartbeat(
                monitoredCount = armedPlaces.size,
                timerRunning = timerRunning,
                hasSetupIssue = setupIssue != null,
            )
        ) {
            if (setupIssue != null) {
                MonitoringPrerequisites.markSetupNeeded(
                    context = appContext,
                    source = "reliability",
                    issue = setupIssue,
                )
                return
            }
            scheduleNext(appContext)
            return
        }

        DwellDiagnostics.logLifecycle(
            appContext,
            source = "reliability",
            decision = "heartbeat",
            detail = "${armedPlaces.size} monitored place${if (armedPlaces.size == 1) "" else "s"} already-inside check",
        )
        val pending = goAsync()
        DwellArrivalEngine.runApproachProbe(
            context = appContext,
            triggerMotion = Prefs.getMotion(appContext),
            placeIds = armedPlaces.map { it.id },
        ) {
            scheduleNext(appContext)
            WearSync.pushState(appContext)
            pending.finish()
        }
    }

    companion object {
        internal const val ACTION_HEARTBEAT = "work.shreyaan.dwell.action.MONITORING_RELIABILITY_HEARTBEAT"
        internal const val HEARTBEAT_INTERVAL_MS = 6 * 60 * 60 * 1_000L
        private const val REQUEST_CODE = 620

        internal fun shouldRunHeartbeat(
            monitoredCount: Int,
            timerRunning: Boolean,
            hasSetupIssue: Boolean,
        ): Boolean =
            monitoredCount > 0 && !timerRunning && !hasSetupIssue

        internal fun shouldKeepHeartbeatScheduled(monitoredCount: Int): Boolean =
            monitoredCount > 0

        fun ensureScheduled(context: Context) {
            if (shouldKeepHeartbeatScheduled(Prefs.getArmedPlaces(context).size)) {
                scheduleNext(context)
            } else {
                cancel(context)
            }
        }

        fun scheduleNext(
            context: Context,
            delayMs: Long = HEARTBEAT_INTERVAL_MS,
        ) {
            val triggerAt = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(60_000L)
            context.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent(context),
                )
        }

        fun cancel(context: Context) {
            context.getSystemService(AlarmManager::class.java)
                .cancel(pendingIntent(context))
        }

        private fun pendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, MonitoringReliabilityReceiver::class.java).apply {
                    action = ACTION_HEARTBEAT
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
