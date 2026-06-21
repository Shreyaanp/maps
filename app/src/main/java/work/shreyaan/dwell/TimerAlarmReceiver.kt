package work.shreyaan.dwell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            !TimerController.acceptsTimerAlarm(
                actionTimerPlaceId = intent.getStringExtra(TimerController.EXTRA_TIMER_PLACE_ID),
                actionTimerStartedAt = intent.getLongExtra(TimerController.EXTRA_TIMER_STARTED_AT, 0L),
                actionTimerEnd = intent.getLongExtra(TimerController.EXTRA_TIMER_END, 0L),
                currentTimerPlaceId = Prefs.getTimerPlaceId(context),
                currentTimerStartedAt = Prefs.getTimerStartedAt(context),
                currentTimerEnd = Prefs.getTimerEnd(context),
                now = System.currentTimeMillis(),
            )
        ) {
            WearSync.pushState(context)
            return
        }
        Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_TIME_UP)
        Prefs.clearArrivalRuntime(context)
        DwellInsights.recordTimerFinished(context, DwellSessionOutcome.Completed)
        Prefs.setTimerEnd(context, 0L)
        Prefs.setTimerStartedAt(context, 0L)
        Notifications.notifyTimerDone(context, TimerController.completionDurationMinutes(context))
        WearSync.pushState(context)
    }
}
