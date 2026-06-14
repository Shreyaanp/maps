package work.shreyaan.dwell

import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives timer state pushed by the phone over the Data Layer and persists
 * it locally so the watch UI shows it even after the phone app is closed.
 */
class WatchDataService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type == DataEvent.TYPE_CHANGED &&
                e.dataItem.uri.path == "/dwell/state"
            ) {
                applyStateMap(DataMapItem.fromDataItem(e.dataItem).dataMap)
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/dwell/state_now") {
            runCatching {
                applyStateMap(DataMap.fromByteArray(event.data))
            }.onFailure { e ->
                Log.w(TAG, "Failed to parse immediate phone state", e)
            }
        }
    }

    private fun applyStateMap(map: DataMap) {
        val prefs = getSharedPreferences("dwell", MODE_PRIVATE)
        val previousEnd = prefs.getLong("timer_end", 0L)
        val previousPrompt = prefs.getString("prompt", "none") ?: "none"
        val previousPromptUpdated = prefs.getLong("prompt_updated", 0L)
        val nextEnd = map.getLong("end", 0L)
        val prompt = map.getString("prompt", "none")
        val promptUpdated = map.getLong("prompt_updated", 0L)
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("has_place", map.getBoolean("has_place", false))
            .putString("place_label", map.getString("place_label", ""))
            .putBoolean("armed", map.getBoolean("armed", false))
            .putBoolean("needs_setup", map.getBoolean("needs_setup", false))
            .putString("monitoring_error", map.getString("monitoring_error", ""))
            .putLong("timer_end", nextEnd)
            .putLong("timer_started_at", map.getLong("started_at", 0L))
            .putInt("duration_min", map.getInt("duration_min", 270))
            .putString("prompt", prompt)
            .putLong("prompt_updated", promptUpdated)
            .putInt("place_count", map.getInt("place_count", 0))
            .putInt("armed_place_count", map.getInt("armed_place_count", 0))
            .putInt("registered_place_count", map.getInt("registered_place_count", 0))
            .putLong("updated", map.getLong("updated", System.currentTimeMillis()))
            .apply()

        val alert = prompt != previousPrompt || promptUpdated != previousPromptUpdated
        if (prompt == "start_timer" && nextEnd <= now) {
            WatchNotifications.showArrivalQuestion(
                this,
                placeLabel = map.getString("place_label", ""),
                alert = alert,
            )
        } else if (prompt == "leave_early" && nextEnd > now) {
            WatchNotifications.showLeavingEarly(
                this,
                placeLabel = map.getString("place_label", ""),
                timerEnd = nextEnd,
                alert = alert,
            )
        } else if (prompt == "time_up") {
            WatchNotifications.showTimeUp(
                this,
                placeLabel = map.getString("place_label", ""),
                alert = alert,
            )
        } else if (nextEnd > now) {
            WatchNotifications.showTimerRunning(
                this,
                placeLabel = map.getString("place_label", ""),
                timerEnd = nextEnd,
                alert = previousEnd <= now,
            )
        } else {
            WatchNotifications.clearTimer(this)
        }
        TileService.getUpdater(this).requestUpdate(DwellTileService::class.java)
    }

    companion object {
        private const val TAG = "DwellWatchData"
    }
}
