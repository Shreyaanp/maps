package xyz.mercle.geotimer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Geofences and alarms don't survive a reboot — re-register both.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val end = Prefs.getTimerEnd(context)
        val now = System.currentTimeMillis()
        if (end > now) {
            TimerController.scheduleAlarm(context, end)
            Notifications.notifyTimerRunning(context, end)
        } else if (end != 0L) {
            // Timer expired while the phone was off.
            Prefs.setTimerEnd(context, 0L)
            Notifications.notifyTimerDone(context, Prefs.getDurationMinutes(context))
        }

        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (Prefs.isArmed(context) && Prefs.hasPlace(context) && hasLocation) {
            GeofenceManager.arm(
                context,
                Prefs.getLat(context),
                Prefs.getLon(context),
                Prefs.getRadius(context),
            ) { _, _ -> }
        }
    }
}
