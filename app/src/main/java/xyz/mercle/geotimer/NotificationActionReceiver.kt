package xyz.mercle.geotimer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_KEEP = "xyz.mercle.geotimer.action.KEEP_TIMER"
        const val ACTION_CANCEL = "xyz.mercle.geotimer.action.CANCEL_TIMER"
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
