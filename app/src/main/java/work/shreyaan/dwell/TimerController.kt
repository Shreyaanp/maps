package work.shreyaan.dwell

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerController {
    const val EXTRA_TIMER_PLACE_ID = "timer_place_id"
    const val EXTRA_TIMER_STARTED_AT = "timer_started_at"
    const val EXTRA_TIMER_END = "timer_end"

    private fun alarmIntent(
        c: Context,
        timerPlaceId: String? = null,
        timerStartedAt: Long = 0L,
        timerEnd: Long = 0L,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            c,
            200,
            Intent(c, TimerAlarmReceiver::class.java)
                .putExtra(EXTRA_TIMER_PLACE_ID, timerPlaceId.orEmpty())
                .putExtra(EXTRA_TIMER_STARTED_AT, timerStartedAt)
                .putExtra(EXTRA_TIMER_END, timerEnd),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    @Synchronized
    fun startTimer(
        c: Context,
        durationMinutes: Int,
        placeId: String? = null,
        allowActivePlaceFallback: Boolean = true,
    ) {
        if (isRunning(c)) return
        val resolvedPlaceId = resolvedTimerPlaceId(
            requestedPlaceId = placeId,
            promptPlaceId = Prefs.getPromptPlaceId(c),
            activePlaceId = if (allowActivePlaceFallback) Prefs.getActivePlace(c)?.id else null,
        )
        val now = System.currentTimeMillis()
        val end = TimerMath.endFromDuration(now, durationMinutes)
        ArrivalProbeReceiver.cancel(c)
        Prefs.clearArrivalRuntime(c)
        Prefs.clearWatchPrompt(c)
        Prefs.setTimerPlaceId(c, resolvedPlaceId)
        Prefs.setTimerStartedAt(c, now)
        Prefs.setTimerEnd(c, end)
        scheduleAlarm(c, end)
        Notifications.notifyTimerRunning(c, end)
        WearSync.pushState(c)
    }

    internal fun resolvedTimerPlaceId(
        requestedPlaceId: String?,
        promptPlaceId: String?,
        activePlaceId: String?,
        allowActivePlaceFallback: Boolean = true,
    ): String? =
        requestedPlaceId?.takeIf { it.isNotBlank() }
            ?: promptPlaceId?.takeIf { it.isNotBlank() }
            ?: activePlaceId?.takeIf { allowActivePlaceFallback && it.isNotBlank() }

    @Synchronized
    fun extendTimer(c: Context, extraMinutes: Int) {
        val now = System.currentTimeMillis()
        val currentEnd = Prefs.getTimerEnd(c)
        val end = TimerMath.extendedEnd(now, currentEnd, extraMinutes)
        ArrivalProbeReceiver.cancel(c)
        Prefs.clearArrivalRuntime(c, clearSwitchPromptSuppression = false)
        if (Prefs.getTimerStartedAt(c) <= 0L) {
            Prefs.setTimerStartedAt(c, now)
        }
        Prefs.clearWatchPrompt(c)
        Prefs.setTimerEnd(c, end)
        Prefs.extendSwitchPromptSuppressionForCurrentTimer(c, end)
        scheduleAlarm(c, end)
        Notifications.notifyTimerRunning(c, end)
        WearSync.pushState(c)
    }

    fun completionDurationMinutes(c: Context): Int =
        TimerMath.completionDurationMinutes(
            timerPlaceDurationMinutes = explicitTimerPlaceDurationMinutes(
                timerPlaceId = Prefs.getTimerPlaceId(c),
                durationForPlaceId = { placeId -> Prefs.getSavedPlace(c, placeId)?.durationMinutes },
            ),
            fallbackDurationMinutes = Prefs.getDurationMinutes(c),
        )

    internal fun explicitTimerPlaceDurationMinutes(
        timerPlaceId: String?,
        durationForPlaceId: (String) -> Int?,
    ): Int? =
        timerPlaceId
            ?.takeIf { it.isNotBlank() }
            ?.let(durationForPlaceId)

    fun scheduleAlarm(c: Context, end: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(
            c = c,
            timerPlaceId = Prefs.getTimerPlaceId(c),
            timerStartedAt = Prefs.getTimerStartedAt(c),
            timerEnd = end,
        )
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // Exact alarms not allowed: fall back to an inexact alarm (may be
            // delayed by a few minutes by the OS).
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi)
        }
    }

    internal fun acceptsTimerAlarm(
        actionTimerPlaceId: String?,
        actionTimerStartedAt: Long,
        actionTimerEnd: Long,
        currentTimerPlaceId: String,
        currentTimerStartedAt: Long,
        currentTimerEnd: Long,
        now: Long,
    ): Boolean {
        if (currentTimerStartedAt <= 0L || currentTimerEnd <= 0L || currentTimerEnd > now) {
            return false
        }
        val hasActionTimerScope = actionTimerStartedAt > 0L && actionTimerEnd > 0L
        if (!hasActionTimerScope) return true

        return NotificationActionReceiver.acceptsScopedTimerAction(
            currentTimerPlaceId = currentTimerPlaceId,
            currentTimerStartedAt = currentTimerStartedAt,
            currentTimerEnd = currentTimerEnd,
            actionTimerPlaceId = actionTimerPlaceId?.takeIf { it.isNotBlank() },
            actionTimerStartedAt = actionTimerStartedAt,
            actionTimerEnd = actionTimerEnd,
        )
    }

    @Synchronized
    fun cancelTimer(c: Context) {
        if (TimerMath.isRunning(Prefs.getTimerEnd(c), System.currentTimeMillis())) {
            DwellInsights.recordTimerFinished(c, DwellSessionOutcome.Cancelled)
        }
        c.getSystemService(AlarmManager::class.java).cancel(alarmIntent(c))
        ArrivalProbeReceiver.cancel(c)
        Prefs.clearArrivalRuntime(c)
        Prefs.clearWatchPrompt(c)
        Prefs.setTimerEnd(c, 0L)
        Prefs.setTimerStartedAt(c, 0L)
        Prefs.setTimerPlaceId(c, null)
        WearSync.pushState(c)
    }

    @Synchronized
    fun clearCompletedTimer(c: Context) {
        if (Prefs.getTimerEnd(c) > 0L && Prefs.getTimerEnd(c) <= System.currentTimeMillis()) {
            DwellInsights.recordTimerFinished(c, DwellSessionOutcome.Completed)
        }
        c.getSystemService(AlarmManager::class.java).cancel(alarmIntent(c))
        ArrivalProbeReceiver.cancel(c)
        Prefs.clearArrivalRuntime(c)
        Prefs.setTimerEnd(c, 0L)
        Prefs.setTimerStartedAt(c, 0L)
        Prefs.setTimerPlaceId(c, null)
        WearSync.pushState(c)
    }

    fun isRunning(c: Context): Boolean =
        TimerMath.isRunning(Prefs.getTimerEnd(c), System.currentTimeMillis())
}
