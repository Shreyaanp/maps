package work.shreyaan.dwell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_TIME_UP)
        Prefs.setTimerEnd(context, 0L)
        Prefs.setTimerStartedAt(context, 0L)
        Notifications.notifyTimerDone(context, Prefs.getDurationMinutes(context))
        WearSync.pushState(context)
    }
}
