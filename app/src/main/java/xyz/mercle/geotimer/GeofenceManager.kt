package xyz.mercle.geotimer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    private const val GEOFENCE_ID = "geotimer_zone"

    // Geofencing requires a MUTABLE PendingIntent so Play services can attach
    // the triggering event data.
    private fun pendingIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 100, Intent(c, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

    @SuppressLint("MissingPermission")
    fun arm(
        c: Context,
        lat: Double,
        lon: Double,
        radiusMeters: Float,
        onResult: (Boolean, String?) -> Unit,
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(lat, lon, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setNotificationResponsiveness(60_000)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        LocationServices.getGeofencingClient(c)
            .addGeofences(request, pendingIntent(c))
            .addOnSuccessListener {
                Prefs.setArmed(c, true)
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    fun disarm(c: Context, onResult: (Boolean) -> Unit) {
        LocationServices.getGeofencingClient(c)
            .removeGeofences(pendingIntent(c))
            .addOnCompleteListener {
                Prefs.setArmed(c, false)
                onResult(it.isSuccessful)
            }
    }
}
