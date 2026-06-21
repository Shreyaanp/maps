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
        val places = Prefs.getPlaces(c)
        val armedPlaces = places.filter { it.monitoringEnabled }
        val registeredPlaceIds = Prefs.getRegisteredPlaceIds(c)
        val liveArmedPlaces = armedPlaces.count { registeredPlaceIds.contains(it.id) }
        val monitoringError = Prefs.getMonitoringError(c)
        val setupIssue = if (armedPlaces.isNotEmpty()) {
            MonitoringPrerequisites.issueForContext(c)
        } else {
            null
        }
        val prompt = Prefs.getWatchPrompt(c)
        val promptPlaceId = Prefs.getPromptPlaceId(c)
        val timerEnd = Prefs.getTimerEnd(c)
        val timerPlaceId = Prefs.getTimerPlaceId(c)
        val placesById = places.associateBy { it.id }
        val promptPlace = promptPlaceId
            .takeIf { it.isNotBlank() }
            ?.let { placesById[it] }
        val timerPlace = timerPlaceId
            .takeIf { it.isNotBlank() }
            ?.let { placesById[it] }
        val place = watchDisplayPlace(
            places = places,
            activePlace = Prefs.getActivePlace(c),
            armedPlaces = armedPlaces,
            registeredPlaceIds = registeredPlaceIds,
            prompt = prompt,
            promptPlaceId = promptPlaceId,
            timerEnd = timerEnd,
            timerPlaceId = timerPlaceId,
        )
        putBoolean("has_place", place != null)
        putString("place_id", place?.id.orEmpty())
        putString("place_label", place?.safeLabel.orEmpty())
        putString("prompt_place_label", promptPlace?.safeLabel.orEmpty())
        putString("timer_place_label", timerPlace?.safeLabel.orEmpty())
        putBoolean("armed", armedPlaces.isNotEmpty())
        putBoolean(
            "needs_setup",
            shouldMarkWatchSetupNeeded(
                watchPlace = place,
                armedPlaces = armedPlaces,
                registeredPlaceIds = registeredPlaceIds,
                setupIssue = setupIssue?.error,
                monitoringError = monitoringError,
            ),
        )
        putString("monitoring_error", setupIssue?.error ?: monitoringError)
        putLong("end", timerEnd)
        putLong("started_at", Prefs.getTimerStartedAt(c))
        putString("timer_place_id", timerPlaceId)
        putInt(
            "duration_min",
            watchDurationMinutes(
                watchPlace = place,
                defaultDurationMinutes = Prefs.getDefaultDurationMinutes(c),
            ),
        )
        putFloat(
            "radius_m",
            watchRadiusMeters(
                watchPlace = place,
                defaultRadiusMeters = Prefs.getDefaultRadius(c),
            ),
        )
        putString("prompt", prompt)
        putString("prompt_place_id", promptPlaceId)
        putLong("prompt_updated", Prefs.getWatchPromptUpdated(c))
        putInt("place_count", places.size)
        putInt("armed_place_count", armedPlaces.size)
        putInt("registered_place_count", liveArmedPlaces)
        putLong("updated", System.currentTimeMillis())
    }

    internal fun watchDisplayPlace(
        places: List<DwellPlace>,
        activePlace: DwellPlace?,
        armedPlaces: List<DwellPlace>,
        registeredPlaceIds: Set<String>,
        prompt: String,
        promptPlaceId: String,
        timerEnd: Long,
        timerPlaceId: String,
    ): DwellPlace? {
        fun placeById(id: String): DwellPlace? =
            id.takeIf { it.isNotBlank() }?.let { placeId ->
                places.firstOrNull { it.id == placeId }
            }

        when (prompt) {
            Prefs.WATCH_PROMPT_START_TIMER ->
                return placeById(promptPlaceId)
            Prefs.WATCH_PROMPT_LEAVE_EARLY,
            Prefs.WATCH_PROMPT_TIME_UP ->
                return placeById(timerPlaceId) ?: placeById(promptPlaceId)
        }
        if (timerEnd > 0L) return placeById(timerPlaceId)
        return activePlace
            ?.takeIf { active -> active.monitoringEnabled || armedPlaces.isEmpty() }
            ?: armedPlaces.firstOrNull { registeredPlaceIds.contains(it.id) }
            ?: armedPlaces.firstOrNull()
            ?: activePlace
    }

    internal fun watchDurationMinutes(
        watchPlace: DwellPlace?,
        defaultDurationMinutes: Int,
    ): Int =
        TimerMath.normalizedDurationMinutes(watchPlace?.durationMinutes ?: defaultDurationMinutes)

    internal fun watchRadiusMeters(
        watchPlace: DwellPlace?,
        defaultRadiusMeters: Float,
    ): Float =
        DwellRadius.normalize(watchPlace?.radiusMeters ?: defaultRadiusMeters)

    internal fun shouldMarkWatchSetupNeeded(
        watchPlace: DwellPlace?,
        armedPlaces: List<DwellPlace>,
        registeredPlaceIds: Set<String>,
        setupIssue: String? = null,
        monitoringError: String? = null,
    ): Boolean {
        if (armedPlaces.isEmpty()) return false
        if (!setupIssue.isNullOrBlank()) return true
        if (!monitoringError.isNullOrBlank()) return true
        val liveArmedPlaces = armedPlaces.count { registeredPlaceIds.contains(it.id) }
        val missingAnyArmedPlace = armedPlaces.any { !registeredPlaceIds.contains(it.id) }
        val displayedPlaceNeedsSetup =
            watchPlace?.monitoringEnabled == true &&
                !registeredPlaceIds.contains(watchPlace.id)
        return displayedPlaceNeedsSetup || liveArmedPlaces == 0 || missingAnyArmedPlace
    }
}
