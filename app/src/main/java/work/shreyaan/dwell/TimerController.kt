package work.shreyaan.dwell

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerController {

    private fun alarmIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 200, Intent(c, TimerAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun startTimer(c: Context, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val end = TimerMath.endFromDuration(now, durationMinutes)
        Prefs.clearWatchPrompt(c)
        Prefs.setTimerStartedAt(c, now)
        Prefs.setTimerEnd(c, end)
        scheduleAlarm(c, end)
        Notifications.notifyTimerRunning(c, end)
        WearSync.pushState(c)
    }

    fun extendTimer(c: Context, extraMinutes: Int) {
        val now = System.currentTimeMillis()
        val currentEnd = Prefs.getTimerEnd(c)
        val end = TimerMath.extendedEnd(now, currentEnd, extraMinutes)
        if (Prefs.getTimerStartedAt(c) <= 0L) {
            Prefs.setTimerStartedAt(c, now)
        }
        Prefs.clearWatchPrompt(c)
        Prefs.setTimerEnd(c, end)
        scheduleAlarm(c, end)
        Notifications.notifyTimerRunning(c, end)
        WearSync.pushState(c)
    }

    fun scheduleAlarm(c: Context, end: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(c)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // Exact alarms not allowed: fall back to an inexact alarm (may be
            // delayed by a few minutes by the OS).
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi)
        }
    }

    fun cancelTimer(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(alarmIntent(c))
        Prefs.clearWatchPrompt(c)
        Prefs.setTimerEnd(c, 0L)
        Prefs.setTimerStartedAt(c, 0L)
        WearSync.pushState(c)
    }

    fun clearCompletedTimer(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(alarmIntent(c))
        Prefs.setTimerEnd(c, 0L)
        Prefs.setTimerStartedAt(c, 0L)
        WearSync.pushState(c)
    }

    fun isRunning(c: Context): Boolean =
        TimerMath.isRunning(Prefs.getTimerEnd(c), System.currentTimeMillis())
}
