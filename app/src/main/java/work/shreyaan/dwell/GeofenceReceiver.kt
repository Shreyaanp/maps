package work.shreyaan.dwell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // Don't restart an already-running timer (e.g. GPS jitter
                // bouncing in and out of the zone).
                if (!TimerController.isRunning(context)) {
                    TimerController.startTimer(context, Prefs.getDurationMinutes(context))
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                if (TimerController.isRunning(context)) {
                    Notifications.notifyExitQuestion(context, Prefs.getTimerEnd(context))
                }
            }
        }
    }
}
