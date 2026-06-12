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
            "/dwell/keep" -> {
                Prefs.clearWatchPrompt(this)
                Notifications.clearExitQuestion(this)
                WearSync.pushState(this)
            }
            "/dwell/done" -> {
                Prefs.clearWatchPrompt(this)
                TimerController.clearCompletedTimer(this)
                Notifications.clearAll(this)
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
}
