package work.shreyaan.dwell

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Pushes the current timer state to the paired watch over the Data Layer.
 * Silently no-ops when no watch is paired.
 */
object WearSync {
    private const val TAG = "DwellWearSync"
    private const val STATE_DATA_PATH = "/dwell/state"
    private const val STATE_MESSAGE_PATH = "/dwell/state_now"

    fun pushState(c: Context) {
        val map = stateMap(c)
        val request = PutDataMapRequest.create(STATE_DATA_PATH).apply {
            dataMap.putAll(map)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(c).putDataItem(request)
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to publish watch state data item", e)
            }

        val payload = map.toByteArray()
        Wearable.getNodeClient(c).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(c)
                        .sendMessage(node.id, STATE_MESSAGE_PATH, payload)
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to send watch state to ${node.displayName}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to load connected Wear nodes", e)
            }
    }

    private fun stateMap(c: Context): DataMap = DataMap().apply {
        putBoolean("has_place", Prefs.hasPlace(c))
        putString("place_label", if (Prefs.hasPlace(c)) Prefs.getPlaceLabel(c) else "")
        putBoolean("armed", Prefs.isArmed(c))
        putLong("end", Prefs.getTimerEnd(c))
        putLong("started_at", Prefs.getTimerStartedAt(c))
        putInt("duration_min", Prefs.getDurationMinutes(c))
        putFloat("radius_m", Prefs.getRadius(c))
        putString("prompt", Prefs.getWatchPrompt(c))
        putLong("prompt_updated", Prefs.getWatchPromptUpdated(c))
        putLong("updated", System.currentTimeMillis())
    }
}
