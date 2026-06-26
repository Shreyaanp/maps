package work.shreyaan.dwell

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object Prefs {
    const val WATCH_PROMPT_NONE = "none"
    const val WATCH_PROMPT_START_TIMER = "start_timer"
    const val WATCH_PROMPT_LEAVE_EARLY = "leave_early"
    const val WATCH_PROMPT_TIME_UP = "time_up"

    private fun p(c: Context): SharedPreferences =
        c.getSharedPreferences("dwell", Context.MODE_PRIVATE)

    private const val PLACES_KEY = "places_v1"
    private const val PLACES_MIGRATED_KEY = "places_migrated_v1"
    private const val ACTIVE_PLACE_ID_KEY = "active_place_id"
    private const val TIMER_PLACE_ID_KEY = "timer_place_id"
    private const val PROMPT_PLACE_ID_KEY = "prompt_place_id"
    private const val PROMPT_CONFIDENCE_SCORE_KEY = "prompt_confidence_score"
    private const val REGISTERED_PLACE_IDS_KEY = "registered_place_ids_v1"
    private const val MONITORING_ERROR_KEY = "monitoring_error"
    private const val MONITORING_UPDATED_KEY = "monitoring_updated"
    private const val PENDING_MONITORING_RESUME_PLACE_ID_KEY = "pending_monitoring_resume_place_id"
    private const val PENDING_MONITORING_RESUME_REQUESTED_AT_KEY = "pending_monitoring_resume_requested_at"
    private const val PENDING_MANUAL_TIMER_PLACE_ID_KEY = "pending_manual_timer_place_id"
    private const val PENDING_MANUAL_TIMER_EDITABLE_PLACE_ID_KEY = "pending_manual_timer_editable_place_id"
    private const val PENDING_MANUAL_TIMER_DURATION_MINUTES_KEY = "pending_manual_timer_duration_minutes"
    private const val PENDING_MANUAL_TIMER_REQUESTED_AT_KEY = "pending_manual_timer_requested_at"
    private const val PENDING_CURRENT_LOCATION_SELECT_AS_ZONE_KEY = "pending_current_location_select_as_zone"
    private const val PENDING_CURRENT_LOCATION_EXPAND_DOCK_KEY = "pending_current_location_expand_dock"
    private const val PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY = "pending_current_location_selection_mode"
    private const val PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY = "pending_current_location_target_place_id"
    private const val PENDING_CURRENT_LOCATION_REQUESTED_AT_KEY = "pending_current_location_requested_at"
    private const val SWITCH_KEEP_TARGET_PLACE_ID_KEY = "switch_keep_target_place_id"
    private const val SWITCH_KEEP_TIMER_PLACE_ID_KEY = "switch_keep_timer_place_id"
    private const val SWITCH_KEEP_TIMER_STARTED_AT_KEY = "switch_keep_timer_started_at"
    private const val SWITCH_KEEP_UNTIL_KEY = "switch_keep_until"
    private const val MOBILE_SEARCH_BASE_URL_KEY = "mobile_search_base_url"
    private const val MOBILE_SEARCH_USER_AGENT_KEY = "mobile_search_user_agent"
    private const val MOBILE_SEARCH_AUTOCOMPLETE_KEY = "mobile_search_autocomplete"
    private const val MOBILE_MAP_STYLE_URL_KEY = "mobile_map_style_url"
    private const val MOBILE_MAP_ATTRIBUTION_KEY = "mobile_map_attribution"
    private const val MOBILE_CONFIG_UPDATED_KEY = "mobile_config_updated"
    private const val ONBOARDING_COMPLETE_KEY = "onboarding_complete_v1"
    private const val DEFAULT_AUTO_START_KEY = "default_auto_start"
    private val ARRIVAL_RUNTIME_BASE_KEYS = listOf(
        "arrival_inside_since",
        "arrival_last_observed",
        "arrival_follow_up_count",
        "arrival_follow_up_scheduled",
        "approach_last_probe",
        "approach_last_probe_motion",
        "exit_keep_until",
    )

    internal const val PENDING_MONITORING_RESUME_TTL_MS: Long = 10 * 60 * 1000L
    internal const val PENDING_CURRENT_LOCATION_RESUME_TTL_MS: Long = 2 * 60 * 1000L

    internal data class PersistedMonitoringResume(
        val placeId: String?,
        val requestedAtMillis: Long,
    )

    internal data class PersistedManualTimerStart(
        val placeId: String?,
        val editablePlaceId: String?,
        val durationMinutes: Int,
        val requestedAtMillis: Long,
    )

    internal data class PersistedCurrentLocationResume(
        val selectAsZone: Boolean,
        val expandDock: Boolean,
        val selectionModeName: String? = null,
        val targetPlaceId: String? = null,
        val requestedAtMillis: Long,
    )

    fun hasPlace(c: Context): Boolean = getActivePlace(c) != null
    fun getLat(c: Context): Double =
        getActivePlace(c)?.latitude ?: Double.fromBits(p(c).getLong("lat", 0L))

    fun getLon(c: Context): Double =
        getActivePlace(c)?.longitude ?: Double.fromBits(p(c).getLong("lon", 0L))

    fun getRadius(c: Context): Float =
        getActivePlace(c)?.radiusMeters
            ?: getDefaultRadius(c)

    fun getDefaultRadius(c: Context): Float =
        DwellRadius.normalize(p(c).getFloat("radius", DwellRadius.DEFAULT_METERS))

    fun getPlaceLabel(c: Context): String =
        getActivePlace(c)?.safeLabel
            ?: p(c).getString("place_label", null)?.takeIf { it.isNotBlank() }
            ?: "Saved place"

    fun getPlaces(c: Context): List<DwellPlace> {
        ensureLegacyPlaceMigrated(c)
        val decodedPlaces = decodePlaces(p(c).getString(PLACES_KEY, null))
        val normalizedPlaces = DwellPlace.normalizePlaces(decodedPlaces)
        reconcileStoredPlaces(c, decodedPlaces, normalizedPlaces)
        return normalizedPlaces
    }

    fun getActivePlace(c: Context): DwellPlace? {
        val places = getPlaces(c)
        if (places.isEmpty()) return null
        val activeId = p(c).getString(ACTIVE_PLACE_ID_KEY, null)
        return places.firstOrNull { it.id == activeId } ?: places.first()
    }

    fun getPlace(c: Context, placeId: String?): DwellPlace? {
        if (placeId.isNullOrBlank()) return getActivePlace(c)
        return getPlaces(c).firstOrNull { it.id == placeId }
    }

    fun getSavedPlace(c: Context, placeId: String?): DwellPlace? {
        val id = placeId?.takeIf { it.isNotBlank() } ?: return null
        return getPlaces(c).firstOrNull { it.id == id }
    }

    fun hasSavedPlaceId(c: Context, placeId: String?): Boolean =
        !placeId.isNullOrBlank() && getPlaces(c).any { it.id == placeId }

    internal fun promptPlaceStillExists(placeId: String?, placeExists: Boolean): Boolean =
        placeId.isNullOrBlank() || placeExists

    fun getPlaceForRequestId(c: Context, requestId: String): DwellPlace? {
        val placeId = DwellPlace.idFromRequestId(requestId) ?: return null
        return getPlace(c, placeId)
    }

    fun getArmedPlaces(c: Context): List<DwellPlace> =
        getPlaces(c).filter { it.monitoringEnabled }.take(DwellPlace.MAX_MONITORED_PLACES)

    fun getRegisteredPlaceIds(c: Context): Set<String> =
        decodeStringSet(p(c).getString(REGISTERED_PLACE_IDS_KEY, null))

    fun isPlaceRegistered(c: Context, placeId: String): Boolean =
        getRegisteredPlaceIds(c).contains(placeId)

    fun setRegisteredPlaces(c: Context, placeIds: Collection<String>) {
        p(c).edit()
            .putString(REGISTERED_PLACE_IDS_KEY, encodeStringSet(placeIds))
            .putLong(MONITORING_UPDATED_KEY, System.currentTimeMillis())
            .apply()
    }

    fun clearRegisteredPlaces(c: Context) {
        p(c).edit()
            .remove(REGISTERED_PLACE_IDS_KEY)
            .putLong(MONITORING_UPDATED_KEY, System.currentTimeMillis())
            .apply()
    }

    fun getMonitoringError(c: Context): String =
        p(c).getString(MONITORING_ERROR_KEY, null).orEmpty()

    fun setMonitoringError(c: Context, error: String?) {
        val editor = p(c).edit().putLong(MONITORING_UPDATED_KEY, System.currentTimeMillis())
        if (error.isNullOrBlank()) {
            editor.remove(MONITORING_ERROR_KEY)
        } else {
            editor.putString(MONITORING_ERROR_KEY, error.take(240))
        }
        editor.apply()
    }

    internal fun monitoringLimitNormalizationPauseCount(
        requestedPlaces: List<DwellPlace>,
        savedPlaces: List<DwellPlace>,
    ): Int {
        val savedById = savedPlaces.associateBy { it.id }
        return requestedPlaces.count { requested ->
            requested.monitoringEnabled && savedById[requested.id]?.monitoringEnabled == false
        }
    }

    internal fun monitoringLimitNormalizationMessage(pausedCount: Int): String =
        "Dwell paused ${monitoringLimitPausedCountText(pausedCount)} because the monitoring limit is ${DwellPlace.MAX_MONITORED_PLACES}. ${monitoringLimitResumeInstruction(pausedCount)}"

    internal fun isMonitoringLimitNormalizationMessage(error: String?): Boolean =
        error?.startsWith("Dwell paused ") == true &&
            (
                error.contains("because the monitoring limit is ${DwellPlace.MAX_MONITORED_PLACES}.") ||
                    error.contains("because the live monitoring limit is ${DwellPlace.MAX_MONITORED_PLACES}.")
                )

    private fun monitoringLimitPausedCountText(count: Int): String =
        "$count ${if (count == 1) "extra monitored place" else "extra monitored places"}"

    private fun monitoringLimitResumeInstruction(count: Int): String =
        if (count == 1) {
            "Pause another monitored place before turning it back on."
        } else {
            "Pause other monitored places before turning them back on."
        }

    fun getMonitoringUpdated(c: Context): Long =
        p(c).getLong(MONITORING_UPDATED_KEY, 0L)

    internal fun pendingMonitoringResumeIsActive(
        requestedAtMillis: Long,
        nowMillis: Long,
        ttlMs: Long = PENDING_MONITORING_RESUME_TTL_MS,
    ): Boolean {
        if (requestedAtMillis <= 0L) return false
        val age = nowMillis - requestedAtMillis
        return age in 0L..ttlMs
    }

    fun savePendingMonitoringResume(
        c: Context,
        placeId: String?,
        requestedAtMillis: Long = System.currentTimeMillis(),
    ) {
        p(c).edit()
            .putString(PENDING_MONITORING_RESUME_PLACE_ID_KEY, placeId.orEmpty())
            .putLong(PENDING_MONITORING_RESUME_REQUESTED_AT_KEY, requestedAtMillis)
            .apply()
    }

    internal fun getPendingMonitoringResume(
        c: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): PersistedMonitoringResume? {
        val prefs = p(c)
        val requestedAt = prefs.getLong(PENDING_MONITORING_RESUME_REQUESTED_AT_KEY, 0L)
        if (!pendingMonitoringResumeIsActive(requestedAt, nowMillis)) {
            clearPendingMonitoringResume(c)
            return null
        }
        return PersistedMonitoringResume(
            placeId = prefs.getString(PENDING_MONITORING_RESUME_PLACE_ID_KEY, null)
                ?.takeIf { it.isNotBlank() },
            requestedAtMillis = requestedAt,
        )
    }

    fun clearPendingMonitoringResume(c: Context) {
        p(c).edit()
            .remove(PENDING_MONITORING_RESUME_PLACE_ID_KEY)
            .remove(PENDING_MONITORING_RESUME_REQUESTED_AT_KEY)
            .apply()
    }

    internal fun pendingManualTimerStartIsActive(
        requestedAtMillis: Long,
        nowMillis: Long,
        ttlMs: Long = MANUAL_TIMER_START_RESUME_TTL_MS,
    ): Boolean {
        if (requestedAtMillis <= 0L) return false
        val age = nowMillis - requestedAtMillis
        return age in 0L..ttlMs
    }

    fun savePendingManualTimerStart(
        c: Context,
        placeId: String?,
        editablePlaceId: String?,
        durationMinutes: Int,
        requestedAtMillis: Long = System.currentTimeMillis(),
    ) {
        p(c).edit()
            .putString(PENDING_MANUAL_TIMER_PLACE_ID_KEY, placeId.orEmpty())
            .putString(PENDING_MANUAL_TIMER_EDITABLE_PLACE_ID_KEY, editablePlaceId.orEmpty())
            .putInt(PENDING_MANUAL_TIMER_DURATION_MINUTES_KEY, durationMinutes)
            .putLong(PENDING_MANUAL_TIMER_REQUESTED_AT_KEY, requestedAtMillis)
            .apply()
    }

    internal fun getPendingManualTimerStart(
        c: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): PersistedManualTimerStart? {
        val prefs = p(c)
        val requestedAt = prefs.getLong(PENDING_MANUAL_TIMER_REQUESTED_AT_KEY, 0L)
        if (!pendingManualTimerStartIsActive(requestedAt, nowMillis)) {
            clearPendingManualTimerStart(c)
            return null
        }
        val durationMinutes = prefs.getInt(PENDING_MANUAL_TIMER_DURATION_MINUTES_KEY, 0)
        if (durationMinutes <= 0) {
            clearPendingManualTimerStart(c)
            return null
        }
        return PersistedManualTimerStart(
            placeId = prefs.getString(PENDING_MANUAL_TIMER_PLACE_ID_KEY, null)
                ?.takeIf { it.isNotBlank() },
            editablePlaceId = prefs.getString(PENDING_MANUAL_TIMER_EDITABLE_PLACE_ID_KEY, null)
                ?.takeIf { it.isNotBlank() },
            durationMinutes = durationMinutes,
            requestedAtMillis = requestedAt,
        )
    }

    fun clearPendingManualTimerStart(c: Context) {
        p(c).edit()
            .remove(PENDING_MANUAL_TIMER_PLACE_ID_KEY)
            .remove(PENDING_MANUAL_TIMER_EDITABLE_PLACE_ID_KEY)
            .remove(PENDING_MANUAL_TIMER_DURATION_MINUTES_KEY)
            .remove(PENDING_MANUAL_TIMER_REQUESTED_AT_KEY)
            .apply()
    }

    internal fun pendingCurrentLocationResumeIsActive(
        requestedAtMillis: Long,
        nowMillis: Long,
        ttlMs: Long = PENDING_CURRENT_LOCATION_RESUME_TTL_MS,
    ): Boolean {
        if (requestedAtMillis <= 0L) return false
        val age = nowMillis - requestedAtMillis
        return age in 0L..ttlMs
    }

    fun savePendingCurrentLocationResume(
        c: Context,
        selectAsZone: Boolean,
        expandDock: Boolean,
        selectionModeName: String? = null,
        targetPlaceId: String? = null,
        requestedAtMillis: Long = System.currentTimeMillis(),
    ) {
        val editor = p(c).edit()
            .putBoolean(PENDING_CURRENT_LOCATION_SELECT_AS_ZONE_KEY, selectAsZone)
            .putBoolean(PENDING_CURRENT_LOCATION_EXPAND_DOCK_KEY, expandDock)
            .putLong(PENDING_CURRENT_LOCATION_REQUESTED_AT_KEY, requestedAtMillis)
        if (selectionModeName.isNullOrBlank()) {
            editor.remove(PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY)
        } else {
            editor.putString(PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY, selectionModeName)
        }
        if (targetPlaceId.isNullOrBlank()) {
            editor.remove(PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY)
        } else {
            editor.putString(PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY, targetPlaceId)
        }
        editor.apply()
    }

    internal fun getPendingCurrentLocationResume(
        c: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): PersistedCurrentLocationResume? {
        val prefs = p(c)
        val requestedAt = prefs.getLong(PENDING_CURRENT_LOCATION_REQUESTED_AT_KEY, 0L)
        if (!pendingCurrentLocationResumeIsActive(requestedAt, nowMillis)) {
            clearPendingCurrentLocationResume(c)
            return null
        }
        return PersistedCurrentLocationResume(
            selectAsZone = prefs.getBoolean(PENDING_CURRENT_LOCATION_SELECT_AS_ZONE_KEY, true),
            expandDock = prefs.getBoolean(PENDING_CURRENT_LOCATION_EXPAND_DOCK_KEY, true),
            selectionModeName = prefs.getString(PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY, null)
                ?.takeIf { it.isNotBlank() },
            targetPlaceId = prefs.getString(PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY, null)
                ?.takeIf { it.isNotBlank() },
            requestedAtMillis = requestedAt,
        )
    }

    fun clearPendingCurrentLocationResume(c: Context) {
        p(c).edit()
            .remove(PENDING_CURRENT_LOCATION_SELECT_AS_ZONE_KEY)
            .remove(PENDING_CURRENT_LOCATION_EXPAND_DOCK_KEY)
            .remove(PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY)
            .remove(PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY)
            .remove(PENDING_CURRENT_LOCATION_REQUESTED_AT_KEY)
            .apply()
    }

    fun nearestArmedPlace(c: Context, latitude: Double, longitude: Double): DwellPlace? =
        getArmedPlaces(c).minByOrNull { it.distanceMetersTo(latitude, longitude) }

    fun getWatchPlace(c: Context): DwellPlace? =
        if (getWatchPrompt(c) != WATCH_PROMPT_NONE && getPromptPlaceId(c).isNotBlank()) {
            getSavedPlace(c, getPromptPlaceId(c))
                ?: getSavedPlace(c, getTimerPlaceId(c))
        } else if (getTimerEnd(c) > 0L) {
            getSavedPlace(c, getTimerPlaceId(c))
        } else {
            getSavedPlace(c, getTimerPlaceId(c))
                ?: getActivePlace(c)
        }

    fun setActivePlace(c: Context, placeId: String) {
        p(c).edit().putString(ACTIVE_PLACE_ID_KEY, placeId).apply()
    }

    fun createPlace(
        c: Context,
        label: String,
        lat: Double,
        lon: Double,
        radiusMeters: Float = getRadius(c),
        durationMinutes: Int = getDurationMinutes(c),
        monitoringEnabled: Boolean = false,
        autoStart: Boolean = getDefaultAutoStart(c),
    ): DwellPlace {
        val place = DwellPlace.create(
            label = label,
            latitude = lat,
            longitude = lon,
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            monitoringEnabled = monitoringEnabled,
            autoStart = autoStart,
        )
        return upsertPlace(c, placeForCreate(getPlaces(c), place), makeActive = true)
    }

    internal fun placeForCreate(
        existingPlaces: List<DwellPlace>,
        candidate: DwellPlace,
    ): DwellPlace =
        // Creating a duplicate opens the existing place as-is. Monitoring changes
        // must go through GeofenceManager so permissions and place limits are checked.
        existingPlaces.firstOrNull { DwellPlace.isDuplicateSavedPlace(it, candidate) }
            ?: candidate

    fun savePlace(c: Context, lat: Double, lon: Double) {
        savePlace(c, lat, lon, getPlaceLabel(c))
    }

    fun savePlace(
        c: Context,
        lat: Double,
        lon: Double,
        label: String,
        radiusMeters: Float? = null,
        durationMinutes: Int? = null,
        autoStart: Boolean? = null,
    ) {
        val now = System.currentTimeMillis()
        val active = getActivePlace(c)
        val resolvedRadius = radiusMeters ?: active?.radiusMeters ?: getDefaultRadius(c)
        val resolvedDuration = durationMinutes ?: active?.durationMinutes ?: getDefaultDurationMinutes(c)
        val place = if (active != null) {
            placeForUpdate(
                active = active,
                lat = lat,
                lon = lon,
                label = label,
                radiusMeters = resolvedRadius,
                durationMinutes = resolvedDuration,
                autoStart = autoStart,
                now = now,
            )
        } else {
            DwellPlace.create(
                label = label,
                latitude = lat,
                longitude = lon,
                radiusMeters = resolvedRadius,
                durationMinutes = resolvedDuration,
                monitoringEnabled = p(c).getBoolean("armed", false),
                autoStart = autoStart ?: getDefaultAutoStart(c),
                now = now,
            )
        }
        upsertPlace(c, place, makeActive = true)
        p(c).edit()
            .putLong("lat", lat.toRawBits())
            .putLong("lon", lon.toRawBits())
            .putString("place_label", label.ifBlank { "Selected place" })
            .apply()
    }

    internal fun placeForUpdate(
        active: DwellPlace,
        lat: Double,
        lon: Double,
        label: String,
        radiusMeters: Float,
        durationMinutes: Int,
        autoStart: Boolean?,
        now: Long = System.currentTimeMillis(),
    ): DwellPlace =
        active.copy(
            label = label.ifBlank { active.safeLabel },
            latitude = lat,
            longitude = lon,
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            autoStart = autoStart ?: active.autoStart,
            updatedAtMillis = now,
        ).normalized()

    fun upsertPlace(c: Context, place: DwellPlace, makeActive: Boolean): DwellPlace {
        val normalized = place.normalized()
        val places = getPlaces(c).toMutableList()
        val index = places.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            places[index] = normalized
        } else {
            places.add(normalized)
        }
        val savedPlaces = DwellPlace.normalizePlaces(places)
        val placeIdRemaps = placeIdRemapsForNormalizedPlaces(places, savedPlaces)
        val savedPlace = savedPlaces.firstOrNull { it.id == normalized.id }
            ?: savedPlaces.firstOrNull { DwellPlace.isDuplicateSavedPlace(it, normalized) }
            ?: normalized
        val prefs = p(c)
        val editor = p(c).edit()
            .putPlaces(savedPlaces)
            .applyMonitoringLimitNormalizationMessage(
                prefs = prefs,
                requestedPlaces = places,
                savedPlaces = savedPlaces,
            )
        if (makeActive) {
            editor
                .putString(ACTIVE_PLACE_ID_KEY, savedPlace.id)
                .putLegacyPlaceFields(savedPlace)
        }
        editor.remapMergedPlaceReferences(
            prefs = prefs,
            normalizedPlaces = savedPlaces,
            placeIdRemaps = placeIdRemaps,
            remapActive = !makeActive,
        )
        editor.apply()
        return savedPlace
    }

    internal fun remapPlaceIdReference(
        placeId: String?,
        removedPlaceIds: Set<String>,
        survivingPlaceId: String,
    ): String? =
        remapPlaceIdReference(
            placeId = placeId,
            placeIdRemaps = removedPlaceIds.associateWith { survivingPlaceId },
        )

    internal fun remapPlaceIdReference(
        placeId: String?,
        placeIdRemaps: Map<String, String>,
    ): String? =
        if (!placeId.isNullOrBlank()) {
            placeIdRemaps[placeId] ?: placeId
        } else {
            placeId
        }

    internal fun remapPlaceIdSet(
        placeIds: Set<String>,
        removedPlaceIds: Set<String>,
        survivingPlaceId: String,
    ): Set<String> =
        remapPlaceIdSet(
            placeIds = placeIds,
            placeIdRemaps = removedPlaceIds.associateWith { survivingPlaceId },
        )

    internal fun remapPlaceIdSet(
        placeIds: Set<String>,
        placeIdRemaps: Map<String, String>,
    ): Set<String> =
        placeIds
            .map { placeId ->
                remapPlaceIdReference(placeId, placeIdRemaps)
            }
            .filterNot { it.isNullOrBlank() }
            .map { it!! }
            .toSet()

    internal fun placeIdRemapsForNormalizedPlaces(
        requestedPlaces: List<DwellPlace>,
        normalizedPlaces: List<DwellPlace>,
    ): Map<String, String> {
        val normalizedIds = normalizedPlaces.map { it.id }.toSet()
        return requestedPlaces
            .map { it.normalized() }
            .filter { it.id !in normalizedIds }
            .mapNotNull { removedPlace ->
                normalizedPlaces
                    .firstOrNull { survivor ->
                        survivor.id != removedPlace.id &&
                            DwellPlace.isDuplicateSavedPlace(survivor, removedPlace)
                    }
                    ?.let { survivor -> removedPlace.id to survivor.id }
            }
            .toMap()
    }

    internal fun remapSwitchPromptSuppressionPlaceIds(
        suppressedTargetPlaceId: String?,
        suppressedTimerPlaceId: String?,
        placeIdRemaps: Map<String, String>,
    ): Pair<String?, String?> =
        remapPlaceIdReference(suppressedTargetPlaceId, placeIdRemaps) to
            remapPlaceIdReference(suppressedTimerPlaceId, placeIdRemaps)

    internal fun mergedArrivalRuntimeLong(
        base: String,
        survivingValue: Long,
        removedValue: Long,
    ): Long =
        if (base == "arrival_inside_since") {
            when {
                survivingValue <= 0L -> removedValue
                removedValue <= 0L -> survivingValue
                else -> minOf(survivingValue, removedValue)
            }
        } else {
            maxOf(survivingValue, removedValue)
        }

    internal fun mergedArrivalRuntimeInt(
        base: String,
        survivingValue: Int,
        removedValue: Int,
    ): Int =
        if (base == "arrival_follow_up_count") {
            maxOf(survivingValue, removedValue)
        } else {
            survivingValue
        }

    internal fun shouldUseMergedApproachMotion(
        survivingMotionExists: Boolean,
        removedProbeMillis: Long,
        survivingProbeMillis: Long,
    ): Boolean =
        !survivingMotionExists || removedProbeMillis >= survivingProbeMillis

    fun deletePlace(c: Context, placeId: String) {
        val remaining = getPlaces(c).filterNot { it.id == placeId }
        savePlaces(c, remaining)
        val prefs = p(c)
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        if (prefs.getString(ACTIVE_PLACE_ID_KEY, null) == placeId) {
            remaining.firstOrNull()?.let {
                editor.putString(ACTIVE_PLACE_ID_KEY, it.id)
                editor.putLegacyPlaceFields(it)
            } ?: editor.remove(ACTIVE_PLACE_ID_KEY)
        }
        if (prefs.getString(TIMER_PLACE_ID_KEY, null) == placeId) {
            editor.remove(TIMER_PLACE_ID_KEY)
        }
        if (
            shouldClearPromptForDeletedPlace(
                prompt = prefs.getString("watch_prompt", WATCH_PROMPT_NONE) ?: WATCH_PROMPT_NONE,
                promptPlaceId = prefs.getString(PROMPT_PLACE_ID_KEY, null),
                timerPlaceId = prefs.getString(TIMER_PLACE_ID_KEY, null),
                deletedPlaceId = placeId,
            )
        ) {
            editor
                .putString("watch_prompt", WATCH_PROMPT_NONE)
                .putLong("watch_prompt_updated", now)
                .remove(PROMPT_PLACE_ID_KEY)
                .remove(PROMPT_CONFIDENCE_SCORE_KEY)
        }
        runtimeKeysForDeletedPlace(placeId).forEach(editor::remove)
        if (
            shouldClearSwitchSuppressionForDeletedPlace(
                suppressedTargetPlaceId = prefs.getString(SWITCH_KEEP_TARGET_PLACE_ID_KEY, null).orEmpty(),
                suppressedTimerPlaceId = prefs.getString(SWITCH_KEEP_TIMER_PLACE_ID_KEY, null).orEmpty(),
                deletedPlaceId = placeId,
            )
        ) {
            editor
                .remove(SWITCH_KEEP_TARGET_PLACE_ID_KEY)
                .remove(SWITCH_KEEP_TIMER_PLACE_ID_KEY)
                .remove(SWITCH_KEEP_TIMER_STARTED_AT_KEY)
                .remove(SWITCH_KEEP_UNTIL_KEY)
        }
        editor.putBoolean("armed", remaining.any { it.monitoringEnabled }).apply()
        setRegisteredPlaces(c, getRegisteredPlaceIds(c) - placeId)
    }

    internal fun shouldClearPromptForDeletedPlace(
        prompt: String,
        promptPlaceId: String?,
        timerPlaceId: String?,
        deletedPlaceId: String,
    ): Boolean =
        deletedPlaceId.isNotBlank() &&
            (
                (!promptPlaceId.isNullOrBlank() && promptPlaceId == deletedPlaceId) ||
                    (
                        prompt == WATCH_PROMPT_TIME_UP &&
                            !timerPlaceId.isNullOrBlank() &&
                            timerPlaceId == deletedPlaceId
                    )
                )

    internal fun shouldClearSwitchSuppressionForDeletedPlace(
        suppressedTargetPlaceId: String,
        suppressedTimerPlaceId: String,
        deletedPlaceId: String,
    ): Boolean =
        deletedPlaceId.isNotBlank() &&
            (suppressedTargetPlaceId == deletedPlaceId || suppressedTimerPlaceId == deletedPlaceId)

    internal fun runtimeKeysForDeletedPlace(placeId: String): Set<String> =
        if (placeId.isBlank()) {
            emptySet()
        } else {
            ARRIVAL_RUNTIME_BASE_KEYS
                .map { scopedArrivalKey(it, placeId) }
                .toSet()
        }

    fun clearPlace(c: Context) {
        clearArrivalRuntime(c)
        DwellInsights.clear(c)
        p(c).edit()
            .remove(PLACES_KEY)
            .remove(ACTIVE_PLACE_ID_KEY)
            .remove(TIMER_PLACE_ID_KEY)
            .remove(PROMPT_PLACE_ID_KEY)
            .remove(PROMPT_CONFIDENCE_SCORE_KEY)
            .remove(REGISTERED_PLACE_IDS_KEY)
            .remove(MONITORING_ERROR_KEY)
            .remove(MONITORING_UPDATED_KEY)
            .remove(PENDING_MONITORING_RESUME_PLACE_ID_KEY)
            .remove(PENDING_MONITORING_RESUME_REQUESTED_AT_KEY)
            .remove(PENDING_MANUAL_TIMER_PLACE_ID_KEY)
            .remove(PENDING_MANUAL_TIMER_EDITABLE_PLACE_ID_KEY)
            .remove(PENDING_MANUAL_TIMER_DURATION_MINUTES_KEY)
            .remove(PENDING_MANUAL_TIMER_REQUESTED_AT_KEY)
            .remove(PENDING_CURRENT_LOCATION_SELECT_AS_ZONE_KEY)
            .remove(PENDING_CURRENT_LOCATION_EXPAND_DOCK_KEY)
            .remove(PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY)
            .remove(PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY)
            .remove(PENDING_CURRENT_LOCATION_REQUESTED_AT_KEY)
            .remove("lat")
            .remove("lon")
            .remove("place_label")
            .remove("armed")
            .remove("arrival_inside_since")
            .remove("arrival_last_observed")
            .remove("approach_last_probe")
            .remove("approach_last_probe_motion")
            .remove("arrival_follow_up_count")
            .remove("arrival_follow_up_scheduled")
            .remove("diagnostics_log")
            .apply()
    }

    fun setRadius(c: Context, radius: Float) {
        val normalized = DwellRadius.normalize(radius)
        getActivePlace(c)?.let {
            upsertPlace(c, it.withTimerDefaults(normalized, it.durationMinutes), makeActive = false)
        } ?: p(c).edit().putFloat("radius", normalized).apply()
    }

    fun setDefaultRadius(c: Context, radius: Float) {
        p(c).edit()
            .putFloat("radius", DwellRadius.normalize(radius))
            .apply()
    }

    // Default 270 minutes = 4.5 hours
    fun getDurationMinutes(c: Context): Int =
        getActivePlace(c)?.durationMinutes ?: getDefaultDurationMinutes(c)

    fun getDefaultDurationMinutes(c: Context): Int =
        p(c).getInt("duration_min", 270).coerceIn(
            DwellPlace.MIN_DURATION_MINUTES,
            DwellPlace.MAX_DURATION_MINUTES,
        )

    fun getDurationMinutes(c: Context, placeId: String?): Int =
        getPlace(c, placeId)?.durationMinutes ?: getDurationMinutes(c)

    fun setDurationMinutes(c: Context, min: Int) {
        val normalized = min.coerceIn(
            DwellPlace.MIN_DURATION_MINUTES,
            DwellPlace.MAX_DURATION_MINUTES,
        )
        getActivePlace(c)?.let {
            upsertPlace(c, it.withTimerDefaults(it.radiusMeters, normalized), makeActive = false)
        } ?: p(c).edit().putInt("duration_min", normalized).apply()
    }

    fun setDefaultDurationMinutes(c: Context, min: Int) {
        val normalized = min.coerceIn(
            DwellPlace.MIN_DURATION_MINUTES,
            DwellPlace.MAX_DURATION_MINUTES,
        )
        p(c).edit().putInt("duration_min", normalized).apply()
    }

    fun getDefaultAutoStart(c: Context): Boolean =
        p(c).getBoolean(DEFAULT_AUTO_START_KEY, true)

    fun setDefaultAutoStart(c: Context, enabled: Boolean) {
        p(c).edit().putBoolean(DEFAULT_AUTO_START_KEY, enabled).apply()
    }

    fun setPlaceAutoStart(c: Context, placeId: String, enabled: Boolean): Boolean {
        val place = getPlace(c, placeId) ?: return false
        upsertPlace(c, place.withAutoStart(enabled), makeActive = false)
        return true
    }

    fun isArmed(c: Context): Boolean = getPlaces(c).any { it.monitoringEnabled }
    fun setArmed(c: Context, armed: Boolean) {
        val active = getActivePlace(c)
        if (active != null) {
            upsertPlace(c, active.withMonitoring(armed), makeActive = true)
        } else {
            p(c).edit().putBoolean("armed", armed).apply()
        }
    }

    fun setPlaceArmed(c: Context, placeId: String, armed: Boolean): Boolean {
        val places = getPlaces(c)
        val place = places.firstOrNull { it.id == placeId } ?: return false
        if (
            armed &&
            !place.monitoringEnabled &&
            places.count { it.monitoringEnabled } >= DwellPlace.MAX_MONITORED_PLACES
        ) {
            return false
        }
        upsertPlace(c, place.withMonitoring(armed), makeActive = false)
        return true
    }

    fun setAllPlacesArmed(c: Context, armed: Boolean) {
        val updated = getPlaces(c).map { it.withMonitoring(armed) }
        savePlaces(c, updated)
        p(c).edit().putBoolean("armed", armed && updated.isNotEmpty()).apply()
    }

    fun getTimerEnd(c: Context): Long = p(c).getLong("timer_end", 0L)
    fun setTimerEnd(c: Context, end: Long) {
        p(c).edit().putLong("timer_end", end).apply()
    }

    fun getTimerStartedAt(c: Context): Long = p(c).getLong("timer_started_at", 0L)
    fun setTimerStartedAt(c: Context, startedAt: Long) {
        p(c).edit().putLong("timer_started_at", startedAt).apply()
    }

    fun getTimerPlaceId(c: Context): String =
        p(c).getString(TIMER_PLACE_ID_KEY, null).orEmpty()

    fun setTimerPlaceId(c: Context, placeId: String?) {
        val editor = p(c).edit()
        if (placeId.isNullOrBlank()) {
            editor.remove(TIMER_PLACE_ID_KEY)
        } else {
            editor.putString(TIMER_PLACE_ID_KEY, placeId)
        }
        editor.apply()
    }

    fun getPromptPlaceId(c: Context): String =
        p(c).getString(PROMPT_PLACE_ID_KEY, null).orEmpty()

    fun setPromptPlaceId(c: Context, placeId: String?) {
        val editor = p(c).edit()
        if (placeId.isNullOrBlank()) {
            editor.remove(PROMPT_PLACE_ID_KEY)
        } else {
            editor.putString(PROMPT_PLACE_ID_KEY, placeId)
        }
        editor.apply()
    }

    fun getWatchPrompt(c: Context): String =
        p(c).getString("watch_prompt", WATCH_PROMPT_NONE) ?: WATCH_PROMPT_NONE

    fun getWatchPromptUpdated(c: Context): Long = p(c).getLong("watch_prompt_updated", 0L)

    fun getPromptConfidenceScore(c: Context): Int =
        p(c).getInt(PROMPT_CONFIDENCE_SCORE_KEY, -1)

    fun setWatchPrompt(
        c: Context,
        prompt: String,
        placeId: String? = null,
        confidenceScore: Int? = null,
    ) {
        val editor = p(c).edit()
            .putString("watch_prompt", prompt)
            .putLong("watch_prompt_updated", System.currentTimeMillis())
        if (placeId.isNullOrBlank()) {
            editor.remove(PROMPT_PLACE_ID_KEY)
        } else {
            editor.putString(PROMPT_PLACE_ID_KEY, placeId)
        }
        if (
            prompt == WATCH_PROMPT_START_TIMER &&
            confidenceScore != null &&
            confidenceScore in 0..100
        ) {
            editor.putInt(PROMPT_CONFIDENCE_SCORE_KEY, confidenceScore)
        } else {
            editor.remove(PROMPT_CONFIDENCE_SCORE_KEY)
        }
        editor.apply()
    }

    fun clearWatchPrompt(c: Context) {
        p(c).edit()
            .putString("watch_prompt", WATCH_PROMPT_NONE)
            .putLong("watch_prompt_updated", System.currentTimeMillis())
            .remove(PROMPT_PLACE_ID_KEY)
            .remove(PROMPT_CONFIDENCE_SCORE_KEY)
            .apply()
    }

    fun getMotion(c: Context): DwellMotion {
        val raw = p(c).getString("motion", DwellMotion.UNKNOWN.name) ?: DwellMotion.UNKNOWN.name
        return runCatching { DwellMotion.valueOf(raw) }.getOrDefault(DwellMotion.UNKNOWN)
    }

    fun getMotionUpdated(c: Context): Long = p(c).getLong("motion_updated", 0L)

    fun getMotionAgeMs(c: Context, now: Long = System.currentTimeMillis()): Long {
        val updated = getMotionUpdated(c)
        return if (updated <= 0L) Long.MAX_VALUE else (now - updated).coerceAtLeast(0L)
    }

    fun setMotion(c: Context, motion: DwellMotion) {
        p(c).edit()
            .putString("motion", motion.name)
            .putLong("motion_updated", System.currentTimeMillis())
            .apply()
    }

    fun updateArrivalObservation(c: Context, insideOrNear: Boolean): Long? =
        updateArrivalObservation(c, null, insideOrNear)

    fun updateArrivalObservation(c: Context, placeId: String?, insideOrNear: Boolean): Long? {
        val prefs = p(c)
        val insideSinceKey = scopedArrivalKey("arrival_inside_since", placeId)
        val lastObservedKey = scopedArrivalKey("arrival_last_observed", placeId)
        val followUpCountKey = scopedArrivalKey("arrival_follow_up_count", placeId)
        val followUpScheduledKey = scopedArrivalKey("arrival_follow_up_scheduled", placeId)
        if (!insideOrNear) {
            prefs.edit()
                .remove(insideSinceKey)
                .remove(lastObservedKey)
                .remove(followUpCountKey)
                .remove(followUpScheduledKey)
                .apply()
            return null
        }

        val now = System.currentTimeMillis()
        val existingSince = prefs.getLong(insideSinceKey, 0L)
        val since = existingSince.takeIf { it > 0L } ?: now
        prefs.edit()
            .putLong(insideSinceKey, since)
            .putLong(lastObservedKey, now)
            .apply()
        return (now - since).coerceAtLeast(0L)
    }

    fun getArrivalInsideDurationMs(c: Context): Long? =
        getArrivalInsideDurationMs(c, null)

    fun getArrivalInsideDurationMs(c: Context, placeId: String?): Long? {
        val since = p(c).getLong(scopedArrivalKey("arrival_inside_since", placeId), 0L)
        if (since <= 0L) return null
        return (System.currentTimeMillis() - since).coerceAtLeast(0L)
    }

    fun clearArrivalObservation(c: Context) {
        clearArrivalObservation(c, null)
    }

    fun clearArrivalObservation(c: Context, placeId: String?) {
        p(c).edit()
            .remove(scopedArrivalKey("arrival_inside_since", placeId))
            .remove(scopedArrivalKey("arrival_last_observed", placeId))
            .remove(scopedArrivalKey("arrival_follow_up_count", placeId))
            .remove(scopedArrivalKey("arrival_follow_up_scheduled", placeId))
            .apply()
    }

    fun markArrivalFollowUpScheduled(
        c: Context,
        maxCount: Int,
        now: Long = System.currentTimeMillis(),
    ): Boolean = markArrivalFollowUpScheduled(c, null, maxCount, now)

    fun markArrivalFollowUpScheduled(
        c: Context,
        placeId: String?,
        maxCount: Int,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val prefs = p(c)
        val countKey = scopedArrivalKey("arrival_follow_up_count", placeId)
        val scheduledKey = scopedArrivalKey("arrival_follow_up_scheduled", placeId)
        val count = prefs.getInt(countKey, 0)
        if (count >= maxCount) return false
        prefs.edit()
            .putInt(countKey, count + 1)
            .putLong(scheduledKey, now)
            .apply()
        return true
    }

    fun clearArrivalFollowUp(c: Context) {
        clearArrivalFollowUp(c, null)
    }

    fun clearArrivalFollowUp(c: Context, placeId: String?) {
        p(c).edit()
            .remove(scopedArrivalKey("arrival_follow_up_count", placeId))
            .remove(scopedArrivalKey("arrival_follow_up_scheduled", placeId))
            .apply()
    }

    internal fun shouldSuppressExitPrompt(
        suppressedUntilMillis: Long,
        now: Long,
    ): Boolean =
        suppressedUntilMillis > now

    fun markExitPromptKept(
        c: Context,
        placeId: String?,
        untilMillis: Long,
    ) {
        if (untilMillis <= System.currentTimeMillis()) return
        val scopedPlaceId = placeId?.takeIf { it.isNotBlank() }
            ?: getTimerPlaceId(c).takeIf { it.isNotBlank() }
        p(c).edit()
            .putLong(scopedArrivalKey("exit_keep_until", scopedPlaceId), untilMillis)
            .apply()
    }

    fun isExitPromptSuppressed(
        c: Context,
        placeId: String?,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val key = scopedArrivalKey("exit_keep_until", placeId)
        val suppressedUntil = p(c).getLong(key, 0L)
        val suppressed = shouldSuppressExitPrompt(suppressedUntil, now)
        if (!suppressed && suppressedUntil > 0L) {
            p(c).edit().remove(key).apply()
        }
        return suppressed
    }

    internal fun shouldSuppressSwitchPrompt(
        suppressedTargetPlaceId: String,
        suppressedTimerPlaceId: String,
        suppressedTimerStartedAt: Long,
        suppressedUntilMillis: Long,
        targetPlaceId: String,
        currentTimerPlaceId: String,
        currentTimerStartedAt: Long,
        now: Long,
    ): Boolean =
        targetPlaceId.isNotBlank() &&
            targetPlaceId == suppressedTargetPlaceId &&
            currentTimerPlaceId == suppressedTimerPlaceId &&
            currentTimerStartedAt > 0L &&
            currentTimerStartedAt == suppressedTimerStartedAt &&
            suppressedUntilMillis > now

    fun markSwitchPromptKept(
        c: Context,
        targetPlaceId: String?,
        untilMillis: Long,
    ) {
        val cleanTarget = targetPlaceId?.takeIf { it.isNotBlank() } ?: return
        val timerStartedAt = getTimerStartedAt(c).takeIf { it > 0L } ?: return
        if (untilMillis <= System.currentTimeMillis()) return
        p(c).edit()
            .putString(SWITCH_KEEP_TARGET_PLACE_ID_KEY, cleanTarget)
            .putString(SWITCH_KEEP_TIMER_PLACE_ID_KEY, getTimerPlaceId(c))
            .putLong(SWITCH_KEEP_TIMER_STARTED_AT_KEY, timerStartedAt)
            .putLong(SWITCH_KEEP_UNTIL_KEY, untilMillis)
            .apply()
    }

    fun isSwitchPromptSuppressed(
        c: Context,
        targetPlaceId: String?,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val cleanTarget = targetPlaceId?.takeIf { it.isNotBlank() } ?: return false
        val prefs = p(c)
        val suppressed = shouldSuppressSwitchPrompt(
            suppressedTargetPlaceId = prefs.getString(SWITCH_KEEP_TARGET_PLACE_ID_KEY, null).orEmpty(),
            suppressedTimerPlaceId = prefs.getString(SWITCH_KEEP_TIMER_PLACE_ID_KEY, null).orEmpty(),
            suppressedTimerStartedAt = prefs.getLong(SWITCH_KEEP_TIMER_STARTED_AT_KEY, 0L),
            suppressedUntilMillis = prefs.getLong(SWITCH_KEEP_UNTIL_KEY, 0L),
            targetPlaceId = cleanTarget,
            currentTimerPlaceId = getTimerPlaceId(c),
            currentTimerStartedAt = getTimerStartedAt(c),
            now = now,
        )
        if (!suppressed && prefs.getLong(SWITCH_KEEP_UNTIL_KEY, 0L) > 0L) {
            clearSwitchPromptSuppression(c)
        }
        return suppressed
    }

    fun clearSwitchPromptSuppression(c: Context) {
        p(c).edit()
            .remove(SWITCH_KEEP_TARGET_PLACE_ID_KEY)
            .remove(SWITCH_KEEP_TIMER_PLACE_ID_KEY)
            .remove(SWITCH_KEEP_TIMER_STARTED_AT_KEY)
            .remove(SWITCH_KEEP_UNTIL_KEY)
            .apply()
    }

    fun extendSwitchPromptSuppressionForCurrentTimer(c: Context, untilMillis: Long) {
        if (untilMillis <= System.currentTimeMillis()) return
        val prefs = p(c)
        val targetPlaceId = prefs.getString(SWITCH_KEEP_TARGET_PLACE_ID_KEY, null).orEmpty()
        val timerPlaceId = prefs.getString(SWITCH_KEEP_TIMER_PLACE_ID_KEY, null).orEmpty()
        val timerStartedAt = prefs.getLong(SWITCH_KEEP_TIMER_STARTED_AT_KEY, 0L)
        val suppressedUntil = prefs.getLong(SWITCH_KEEP_UNTIL_KEY, 0L)
        val currentTimerPlaceId = getTimerPlaceId(c)
        val currentTimerStartedAt = getTimerStartedAt(c)
        if (
            !shouldExtendSwitchPromptSuppression(
                suppressedTargetPlaceId = targetPlaceId,
                suppressedTimerPlaceId = timerPlaceId,
                suppressedTimerStartedAt = timerStartedAt,
                suppressedUntilMillis = suppressedUntil,
                targetUntilMillis = untilMillis,
                currentTimerPlaceId = currentTimerPlaceId,
                currentTimerStartedAt = currentTimerStartedAt,
                now = System.currentTimeMillis(),
            )
        ) {
            clearSwitchPromptSuppression(c)
            return
        }
        prefs.edit()
            .putLong(SWITCH_KEEP_UNTIL_KEY, untilMillis)
            .apply()
    }

    internal fun shouldExtendSwitchPromptSuppression(
        suppressedTargetPlaceId: String,
        suppressedTimerPlaceId: String,
        suppressedTimerStartedAt: Long,
        suppressedUntilMillis: Long,
        targetUntilMillis: Long,
        currentTimerPlaceId: String,
        currentTimerStartedAt: Long,
        now: Long,
    ): Boolean =
        suppressedTargetPlaceId.isNotBlank() &&
            suppressedUntilMillis > now &&
            targetUntilMillis > 0L &&
            currentTimerStartedAt > 0L &&
            suppressedTimerStartedAt == currentTimerStartedAt &&
            suppressedTimerPlaceId == currentTimerPlaceId

    fun clearArrivalRuntime(c: Context, clearSwitchPromptSuppression: Boolean = true) {
        val editor = p(c).edit()
        arrivalRuntimeKeysForPlaces(getPlaces(c).map { it.id })
            .forEach { editor.remove(it) }
        if (clearSwitchPromptSuppression) {
            editor.remove(SWITCH_KEEP_TARGET_PLACE_ID_KEY)
                .remove(SWITCH_KEEP_TIMER_PLACE_ID_KEY)
                .remove(SWITCH_KEEP_TIMER_STARTED_AT_KEY)
                .remove(SWITCH_KEEP_UNTIL_KEY)
        }
        editor.apply()
    }

    fun clearArrivalRuntimeForPlace(c: Context, placeId: String) {
        if (placeId.isBlank()) return
        p(c).edit()
            .apply {
                runtimeKeysForDeletedPlace(placeId).forEach(::remove)
            }
            .apply()
    }

    internal fun arrivalRuntimeKeysForPlaces(placeIds: Collection<String>): Set<String> =
        buildSet {
            ARRIVAL_RUNTIME_BASE_KEYS.forEach(::add)
            placeIds
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { placeId ->
                    ARRIVAL_RUNTIME_BASE_KEYS.forEach { base ->
                        add(scopedArrivalKey(base, placeId))
                    }
                }
        }

    fun getLastApproachProbeMotion(c: Context, placeId: String? = null): DwellMotion {
        val raw = p(c).getString(
            scopedApproachKey("approach_last_probe_motion", placeId),
            DwellMotion.UNKNOWN.name,
        )
            ?: DwellMotion.UNKNOWN.name
        return runCatching { DwellMotion.valueOf(raw) }.getOrDefault(DwellMotion.UNKNOWN)
    }

    internal fun shouldAllowApproachProbe(
        lastProbeMillis: Long,
        now: Long,
        cooldownMs: Long,
        bypassCooldown: Boolean,
    ): Boolean =
        lastProbeMillis <= 0L ||
            now - lastProbeMillis >= cooldownMs ||
            bypassCooldown

    fun shouldRunApproachProbe(
        c: Context,
        cooldownMs: Long,
        placeId: String? = null,
        triggerMotion: DwellMotion = DwellMotion.UNKNOWN,
        bypassCooldown: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val prefs = p(c)
        val probeKey = scopedApproachKey("approach_last_probe", placeId)
        val motionKey = scopedApproachKey("approach_last_probe_motion", placeId)
        val lastProbe = prefs.getLong(probeKey, 0L)
        if (!shouldAllowApproachProbe(lastProbe, now, cooldownMs, bypassCooldown)) return false
        prefs.edit()
            .putLong(probeKey, now)
            .putString(motionKey, triggerMotion.name)
            .apply()
        return true
    }

    fun isSignedIn(c: Context): Boolean = p(c).getBoolean("signed_in", false)

    fun isOnboardingComplete(c: Context): Boolean =
        p(c).getBoolean(ONBOARDING_COMPLETE_KEY, false)

    fun setOnboardingComplete(c: Context, complete: Boolean) {
        p(c).edit().putBoolean(ONBOARDING_COMPLETE_KEY, complete).apply()
    }

    fun setSignedIn(c: Context, signedIn: Boolean) {
        val editor = p(c).edit().putBoolean("signed_in", signedIn)
        if (!signedIn) {
            editor
                .remove("backend_session_token")
                .remove("account_provider")
                .remove("account_display_name")
                .remove("account_email")
        }
        editor.apply()
    }

    fun saveAccount(
        c: Context,
        provider: String,
        displayName: String = "",
        email: String = "",
    ) {
        p(c).edit()
            .putString("account_provider", if (provider == "google") "google" else "local")
            .putString("account_display_name", displayName.take(120))
            .putString("account_email", email.take(160))
            .apply()
    }

    fun getAccountProvider(c: Context): String =
        p(c).getString("account_provider", "local") ?: "local"

    fun getAccountDisplayName(c: Context): String =
        p(c).getString("account_display_name", "") ?: ""

    fun getAccountEmail(c: Context): String =
        p(c).getString("account_email", "") ?: ""

    fun getBackendSessionToken(c: Context): String =
        p(c).getString("backend_session_token", null).orEmpty()

    fun setBackendSessionToken(c: Context, token: String) {
        p(c).edit().putString("backend_session_token", token).apply()
    }

    fun saveMobileConfig(c: Context, config: MobileConfig) {
        p(c).edit()
            .putString(MOBILE_SEARCH_BASE_URL_KEY, config.search.baseUrl.trim().trimEnd('/'))
            .putString(MOBILE_SEARCH_USER_AGENT_KEY, config.search.userAgent.trim())
            .putBoolean(MOBILE_SEARCH_AUTOCOMPLETE_KEY, config.search.networkAutocomplete)
            .putString(MOBILE_MAP_STYLE_URL_KEY, config.map.styleUrl.trim())
            .putString(
                MOBILE_MAP_ATTRIBUTION_KEY,
                MobileMapConfig.normalizeAttribution(config.map.attributionLabel),
            )
            .putLong(MOBILE_CONFIG_UPDATED_KEY, System.currentTimeMillis())
            .apply()
    }

    fun getMobileConfig(c: Context): MobileConfig =
        MobileConfig(
            search = getMobileSearchConfig(c),
            map = getMobileMapConfig(c),
        )

    fun getMobileSearchConfig(c: Context): MobileSearchConfig {
        val fallback = MobileSearchConfig.defaults()
        val baseUrl = p(c).getString(MOBILE_SEARCH_BASE_URL_KEY, null)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: fallback.baseUrl
        val userAgent = p(c).getString(MOBILE_SEARCH_USER_AGENT_KEY, null)
            ?.trim()
            ?.takeIf { it.length >= 12 }
            ?: fallback.userAgent
        val autocomplete = p(c).getBoolean(
            MOBILE_SEARCH_AUTOCOMPLETE_KEY,
            fallback.networkAutocomplete,
        )
        return MobileSearchConfig(
            baseUrl = baseUrl,
            userAgent = userAgent,
            networkAutocomplete = MobileSearchConfig.shouldAllowNetworkAutocomplete(
                baseUrl = baseUrl,
                requestedAutocomplete = autocomplete,
            ),
        )
    }

    fun getMobileMapConfig(c: Context): MobileMapConfig {
        val fallback = MobileMapConfig.defaults()
        val styleUrl = p(c).getString(MOBILE_MAP_STYLE_URL_KEY, null)
            ?.trim()
            ?.takeIf(MobileMapConfig::isAllowedStyleUrl)
            ?: fallback.styleUrl
        val attribution = p(c).getString(MOBILE_MAP_ATTRIBUTION_KEY, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: fallback.attributionLabel
        return MobileMapConfig(
            styleUrl = styleUrl,
            attributionLabel = MobileMapConfig.normalizeAttribution(attribution),
        )
    }

    fun clearAppData(c: Context, keepSession: Boolean) {
        clearArrivalRuntime(c)
        DwellInsights.clear(c)
        p(c).edit()
            .remove(PLACES_KEY)
            .remove(ACTIVE_PLACE_ID_KEY)
            .remove(TIMER_PLACE_ID_KEY)
            .remove(PROMPT_PLACE_ID_KEY)
            .remove(PROMPT_CONFIDENCE_SCORE_KEY)
            .remove(REGISTERED_PLACE_IDS_KEY)
            .remove(MONITORING_ERROR_KEY)
            .remove(MONITORING_UPDATED_KEY)
            .remove(PENDING_MONITORING_RESUME_PLACE_ID_KEY)
            .remove(PENDING_MONITORING_RESUME_REQUESTED_AT_KEY)
            .remove(PENDING_MANUAL_TIMER_PLACE_ID_KEY)
            .remove(PENDING_MANUAL_TIMER_EDITABLE_PLACE_ID_KEY)
            .remove(PENDING_MANUAL_TIMER_DURATION_MINUTES_KEY)
            .remove(PENDING_MANUAL_TIMER_REQUESTED_AT_KEY)
            .remove(PENDING_CURRENT_LOCATION_SELECT_AS_ZONE_KEY)
            .remove(PENDING_CURRENT_LOCATION_EXPAND_DOCK_KEY)
            .remove(PENDING_CURRENT_LOCATION_SELECTION_MODE_KEY)
            .remove(PENDING_CURRENT_LOCATION_TARGET_PLACE_ID_KEY)
            .remove(PENDING_CURRENT_LOCATION_REQUESTED_AT_KEY)
            .remove("lat")
            .remove("lon")
            .remove("radius")
            .remove("duration_min")
            .remove(DEFAULT_AUTO_START_KEY)
            .remove("armed")
            .remove("timer_end")
            .remove("timer_started_at")
            .remove("place_label")
            .remove("watch_prompt")
            .remove("watch_prompt_updated")
            .remove("motion")
            .remove("motion_updated")
            .remove("arrival_inside_since")
            .remove("arrival_last_observed")
            .remove("approach_last_probe")
            .remove("approach_last_probe_motion")
            .remove("arrival_follow_up_count")
            .remove("arrival_follow_up_scheduled")
            .remove("diagnostics_log")
            .remove(ONBOARDING_COMPLETE_KEY)
            .apply()

        if (!keepSession) {
            setSignedIn(c, false)
        }
    }

    fun getInstallId(c: Context): String {
        val prefs = p(c)
        val existing = prefs.getString("install_id", null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        prefs.edit().putString("install_id", created).apply()
        return created
    }

    private fun ensureLegacyPlaceMigrated(c: Context) {
        val prefs = p(c)
        if (prefs.getBoolean(PLACES_MIGRATED_KEY, false)) return

        if (!prefs.contains(PLACES_KEY) && prefs.contains("lat") && prefs.contains("lon")) {
            val now = System.currentTimeMillis()
            val latitude = Double.fromBits(prefs.getLong("lat", 0L))
            val longitude = Double.fromBits(prefs.getLong("lon", 0L))
            if (DwellPlace.hasValidCoordinates(latitude, longitude)) {
                val place = DwellPlace(
                    id = "legacy_primary",
                    label = prefs.getString("place_label", null)?.takeIf { it.isNotBlank() }
                        ?: "Saved place",
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = DwellRadius.normalize(
                        prefs.getFloat("radius", DwellRadius.DEFAULT_METERS),
                    ),
                    durationMinutes = prefs.getInt("duration_min", 270),
                    monitoringEnabled = prefs.getBoolean("armed", false),
                    autoStart = getDefaultAutoStart(c),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ).normalized()
                savePlaces(c, listOf(place))
                prefs.edit()
                    .putString(ACTIVE_PLACE_ID_KEY, place.id)
                    .putBoolean("armed", place.monitoringEnabled)
                    .apply()
            } else {
                prefs.edit()
                    .remove("lat")
                    .remove("lon")
                    .putBoolean("armed", false)
                    .apply()
            }
        }

        prefs.edit().putBoolean(PLACES_MIGRATED_KEY, true).apply()
    }

    private fun savePlaces(c: Context, places: List<DwellPlace>): List<DwellPlace> {
        val normalizedPlaces = DwellPlace.normalizePlaces(places)
        val prefs = p(c)
        prefs.edit()
            .putPlaces(normalizedPlaces)
            .applyMonitoringLimitNormalizationMessage(
                prefs = prefs,
                requestedPlaces = places,
                savedPlaces = normalizedPlaces,
            )
            .apply()
        return normalizedPlaces
    }

    private fun reconcileStoredPlaces(
        c: Context,
        decodedPlaces: List<DwellPlace>,
        normalizedPlaces: List<DwellPlace>,
    ) {
        if (decodedPlaces == normalizedPlaces) return

        val prefs = p(c)
        val placeIdRemaps = placeIdRemapsForNormalizedPlaces(decodedPlaces, normalizedPlaces)
        prefs.edit()
            .putPlaces(normalizedPlaces)
            .applyMonitoringLimitNormalizationMessage(
                prefs = prefs,
                requestedPlaces = decodedPlaces,
                savedPlaces = normalizedPlaces,
            )
            .remapMergedPlaceReferences(
                prefs = prefs,
                normalizedPlaces = normalizedPlaces,
                placeIdRemaps = placeIdRemaps,
                remapActive = true,
            )
            .apply()
    }

    private fun decodePlaces(raw: String?): List<DwellPlace> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val place = DwellPlace.fromJson(array.optJSONObject(index) ?: JSONObject())
                    if (place != null) add(place)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeStringSet(values: Collection<String>): String {
        val array = JSONArray()
        values.filter { it.isNotBlank() }
            .distinct()
            .forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeStringSet(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun SharedPreferences.Editor.putPlaces(places: List<DwellPlace>): SharedPreferences.Editor {
        val json = JSONArray()
        places.forEach { json.put(it.toJson()) }
        return putString(PLACES_KEY, json.toString())
            .putBoolean("armed", places.any { it.monitoringEnabled })
    }

    private fun SharedPreferences.Editor.applyMonitoringLimitNormalizationMessage(
        prefs: SharedPreferences,
        requestedPlaces: List<DwellPlace>,
        savedPlaces: List<DwellPlace>,
    ): SharedPreferences.Editor {
        val pausedCount = monitoringLimitNormalizationPauseCount(requestedPlaces, savedPlaces)
        val currentError = prefs.getString(MONITORING_ERROR_KEY, null)
        return when {
            pausedCount > 0 &&
                (currentError.isNullOrBlank() || isMonitoringLimitNormalizationMessage(currentError)) ->
                putString(MONITORING_ERROR_KEY, monitoringLimitNormalizationMessage(pausedCount))
                    .putLong(MONITORING_UPDATED_KEY, System.currentTimeMillis())
            pausedCount == 0 && isMonitoringLimitNormalizationMessage(currentError) ->
                remove(MONITORING_ERROR_KEY)
                    .putLong(MONITORING_UPDATED_KEY, System.currentTimeMillis())
            else -> this
        }
    }

    private fun SharedPreferences.Editor.remapMergedPlaceReferences(
        prefs: SharedPreferences,
        normalizedPlaces: List<DwellPlace>,
        placeIdRemaps: Map<String, String>,
        remapActive: Boolean,
    ): SharedPreferences.Editor {
        if (placeIdRemaps.isEmpty()) return this

        if (remapActive) {
            val currentActivePlaceId = prefs.getString(ACTIVE_PLACE_ID_KEY, null)
            val remappedActivePlaceId = remapPlaceIdReference(currentActivePlaceId, placeIdRemaps)
            if (remappedActivePlaceId != currentActivePlaceId) {
                putOptionalString(ACTIVE_PLACE_ID_KEY, remappedActivePlaceId)
                normalizedPlaces
                    .firstOrNull { it.id == remappedActivePlaceId }
                    ?.let { putLegacyPlaceFields(it) }
            }
        }

        val currentTimerPlaceId = prefs.getString(TIMER_PLACE_ID_KEY, null)
        val remappedTimerPlaceId = remapPlaceIdReference(currentTimerPlaceId, placeIdRemaps)
        if (remappedTimerPlaceId != currentTimerPlaceId) {
            putOptionalString(TIMER_PLACE_ID_KEY, remappedTimerPlaceId)
        }

        val currentPromptPlaceId = prefs.getString(PROMPT_PLACE_ID_KEY, null)
        val remappedPromptPlaceId = remapPlaceIdReference(currentPromptPlaceId, placeIdRemaps)
        if (remappedPromptPlaceId != currentPromptPlaceId) {
            putOptionalString(PROMPT_PLACE_ID_KEY, remappedPromptPlaceId)
        }

        val registeredPlaceIds = decodeStringSet(prefs.getString(REGISTERED_PLACE_IDS_KEY, null))
        val remappedRegisteredPlaceIds = remapPlaceIdSet(
            placeIds = registeredPlaceIds,
            placeIdRemaps = placeIdRemaps,
        )
        if (remappedRegisteredPlaceIds != registeredPlaceIds) {
            putString(REGISTERED_PLACE_IDS_KEY, encodeStringSet(remappedRegisteredPlaceIds))
            putLong(MONITORING_UPDATED_KEY, System.currentTimeMillis())
        }

        val currentSwitchTargetPlaceId = prefs.getString(SWITCH_KEEP_TARGET_PLACE_ID_KEY, null)
        val currentSwitchTimerPlaceId = prefs.getString(SWITCH_KEEP_TIMER_PLACE_ID_KEY, null)
        val remappedSwitchPlaceIds = remapSwitchPromptSuppressionPlaceIds(
            suppressedTargetPlaceId = currentSwitchTargetPlaceId,
            suppressedTimerPlaceId = currentSwitchTimerPlaceId,
            placeIdRemaps = placeIdRemaps,
        )
        if (remappedSwitchPlaceIds.first != currentSwitchTargetPlaceId) {
            putOptionalString(SWITCH_KEEP_TARGET_PLACE_ID_KEY, remappedSwitchPlaceIds.first)
        }
        if (remappedSwitchPlaceIds.second != currentSwitchTimerPlaceId) {
            putOptionalString(SWITCH_KEEP_TIMER_PLACE_ID_KEY, remappedSwitchPlaceIds.second)
        }

        remapMergedPlaceRuntime(prefs, placeIdRemaps)
        return this
    }

    private fun SharedPreferences.Editor.putOptionalString(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, value)
        }
    }

    private fun SharedPreferences.Editor.remapMergedPlaceRuntime(
        prefs: SharedPreferences,
        placeIdRemaps: Map<String, String>,
    ) {
        val mergedLongValues = mutableMapOf<String, Long>()
        val mergedIntValues = mutableMapOf<String, Int>()
        val mergedStringValues = mutableMapOf<String, String>()

        for ((removedPlaceId, survivingPlaceId) in placeIdRemaps) {
            if (
                removedPlaceId.isBlank() ||
                survivingPlaceId.isBlank() ||
                removedPlaceId == survivingPlaceId
            ) {
                continue
            }

            val removedProbeMillis = prefs.getLong(
                scopedApproachKey("approach_last_probe", removedPlaceId),
                0L,
            )
            val survivingProbeKey = scopedApproachKey("approach_last_probe", survivingPlaceId)
            val survivingProbeBeforeMerge = mergedLongValues[survivingProbeKey]
                ?: prefs.getLong(survivingProbeKey, 0L)

            for (base in ARRIVAL_RUNTIME_BASE_KEYS) {
                val removedKey = scopedArrivalKey(base, removedPlaceId)
                if (!prefs.contains(removedKey)) continue

                val survivingKey = scopedArrivalKey(base, survivingPlaceId)
                when (base) {
                    "arrival_follow_up_count" -> {
                        val mergedValue = mergedArrivalRuntimeInt(
                            base = base,
                            survivingValue = mergedIntValues[survivingKey]
                                ?: prefs.getInt(survivingKey, 0),
                            removedValue = prefs.getInt(removedKey, 0),
                        )
                        mergedIntValues[survivingKey] = mergedValue
                        putInt(survivingKey, mergedValue)
                    }

                    "approach_last_probe_motion" -> {
                        val survivingMotionExists =
                            mergedStringValues.containsKey(survivingKey) || prefs.contains(survivingKey)
                        if (
                            shouldUseMergedApproachMotion(
                                survivingMotionExists = survivingMotionExists,
                                removedProbeMillis = removedProbeMillis,
                                survivingProbeMillis = survivingProbeBeforeMerge,
                            )
                        ) {
                            val mergedValue = prefs.getString(removedKey, DwellMotion.UNKNOWN.name)
                                ?: DwellMotion.UNKNOWN.name
                            mergedStringValues[survivingKey] = mergedValue
                            putString(survivingKey, mergedValue)
                        }
                    }

                    else -> {
                        val mergedValue = mergedArrivalRuntimeLong(
                            base = base,
                            survivingValue = mergedLongValues[survivingKey]
                                ?: prefs.getLong(survivingKey, 0L),
                            removedValue = prefs.getLong(removedKey, 0L),
                        )
                        mergedLongValues[survivingKey] = mergedValue
                        putLong(survivingKey, mergedValue)
                    }
                }
                remove(removedKey)
            }
        }
    }

    private fun SharedPreferences.Editor.putLegacyPlaceFields(place: DwellPlace) {
        putLong("lat", place.latitude.toRawBits())
        putLong("lon", place.longitude.toRawBits())
        putFloat("radius", place.radiusMeters)
        putInt("duration_min", place.durationMinutes)
        putString("place_label", place.safeLabel)
    }

    private fun scopedArrivalKey(base: String, placeId: String?): String {
        val scopedId = placeId?.takeIf { it.isNotBlank() } ?: return base
        return "${base}_$scopedId"
    }

    internal fun scopedApproachKey(base: String, placeId: String?): String =
        scopedArrivalKey(base, placeId)
}
