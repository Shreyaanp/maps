package xyz.mercle.geotimer

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Pushes the current timer state to the paired watch over the Data Layer.
 * Silently no-ops when no watch is paired.
 */
object WearSync {
    fun pushState(c: Context) {
        val request = PutDataMapRequest.create("/geotimer/state").apply {
            dataMap.putLong("end", Prefs.getTimerEnd(c))
            dataMap.putInt("duration_min", Prefs.getDurationMinutes(c))
            dataMap.putLong("updated", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(c).putDataItem(request).addOnFailureListener { }
    }
}
