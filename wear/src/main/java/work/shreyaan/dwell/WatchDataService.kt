package work.shreyaan.dwell

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
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
                val map = DataMapItem.fromDataItem(e.dataItem).dataMap
                getSharedPreferences("dwell", MODE_PRIVATE).edit()
                    .putLong("timer_end", map.getLong("end"))
                    .putInt("duration_min", map.getInt("duration_min"))
                    .apply()
            }
        }
    }
}
