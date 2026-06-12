package work.shreyaan.dwell

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_KEEP = "work.shreyaan.dwell.action.KEEP_TIMER"
        const val ACTION_CANCEL = "work.shreyaan.dwell.action.CANCEL_TIMER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        when (intent.action) {
            ACTION_KEEP -> nm.cancel(Notifications.NOTIF_EXIT)
            ACTION_CANCEL -> {
                TimerController.cancelTimer(context)
                Notifications.notifyTimerCancelled(context)
            }
        }
    }
}
