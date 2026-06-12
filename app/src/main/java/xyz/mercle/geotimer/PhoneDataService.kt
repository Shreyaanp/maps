package xyz.mercle.geotimer

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Handles commands sent from the watch app.
 */
class PhoneDataService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/geotimer/cancel") {
            TimerController.cancelTimer(this)
            Notifications.notifyTimerCancelled(this)
        }
    }
}
