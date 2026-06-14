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
    private const val REGISTERED_PLACE_IDS_KEY = "registered_place_ids_v1"
    private const val MONITORING_ERROR_KEY = "monitoring_error"
    private const val MONITORING_UPDATED_KEY = "monitoring_updated"
    private const val MOBILE_SEARCH_BASE_URL_KEY = "mobile_search_base_url"
    private const val MOBILE_SEARCH_USER_AGENT_KEY = "mobile_search_user_agent"
    private const val MOBILE_SEARCH_AUTOCOMPLETE_KEY = "mobile_search_autocomplete"
    private const val MOBILE_MAP_STYLE_URL_KEY = "mobile_map_style_url"
    private const val MOBILE_MAP_ATTRIBUTION_KEY = "mobile_map_attribution"
    private const val MOBILE_CONFIG_UPDATED_KEY = "mobile_config_updated"
    private val ARRIVAL_RUNTIME_BASE_KEYS = listOf(
        "arrival_inside_since",
        "arrival_last_observed",
        "arrival_follow_up_count",
        "arrival_follow_up_scheduled",
        "approach_last_probe",
        "approach_last_probe_motion",
    )

    fun hasPlace(c: Context): Boolean = getActivePlace(c) != null
    fun getLat(c: Context): Double =
        getActivePlace(c)?.latitude ?: Double.fromBits(p(c).getLong("lat", 0L))

    fun getLon(c: Context): Double =
        getActivePlace(c)?.longitude ?: Double.fromBits(p(c).getLong("lon", 0L))

    fun getRadius(c: Context): Float =
        getActivePlace(c)?.radiusMeters
            ?: DwellRadius.normalize(p(c).getFloat("radius", DwellRadius.DEFAULT_METERS))

    fun getPlaceLabel(c: Context): String =
        getActivePlace(c)?.safeLabel
            ?: p(c).getString("place_label", null)?.takeIf { it.isNotBlank() }
            ?: "Saved place"

    fun getPlaces(c: Context): List<DwellPlace> {
        ensureLegacyPlaceMigrated(c)
        return decodePlaces(p(c).getString(PLACES_KEY, null))
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

    fun getMonitoringUpdated(c: Context): Long =
        p(c).getLong(MONITORING_UPDATED_KEY, 0L)

    fun nearestArmedPlace(c: Context, latitude: Double, longitude: Double): DwellPlace? =
        getArmedPlaces(c).minByOrNull { it.distanceMetersTo(latitude, longitude) }

    fun getWatchPlace(c: Context): DwellPlace? =
        if (getWatchPrompt(c) != WATCH_PROMPT_NONE && getPromptPlaceId(c).isNotBlank()) {
            getPlace(c, getPromptPlaceId(c))
                ?: getPlace(c, getTimerPlaceId(c))
                ?: getActivePlace(c)
        } else {
            getPlace(c, getTimerPlaceId(c))
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
    ): DwellPlace {
        val place = DwellPlace.create(
            label = label,
            latitude = lat,
            longitude = lon,
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            monitoringEnabled = monitoringEnabled,
        )
        upsertPlace(c, place, makeActive = true)
        return place
    }

    fun savePlace(c: Context, lat: Double, lon: Double) {
        savePlace(c, lat, lon, getPlaceLabel(c))
    }

    fun savePlace(c: Context, lat: Double, lon: Double, label: String) {
        val now = System.currentTimeMillis()
        val active = getActivePlace(c)
        val place = if (active != null) {
            active.copy(
                label = label.ifBlank { active.safeLabel },
                latitude = lat,
                longitude = lon,
                updatedAtMillis = now,
            ).normalized()
        } else {
            DwellPlace.create(
                label = label,
                latitude = lat,
                longitude = lon,
                radiusMeters = DwellRadius.normalize(p(c).getFloat("radius", DwellRadius.DEFAULT_METERS)),
                durationMinutes = p(c).getInt("duration_min", 270),
                monitoringEnabled = p(c).getBoolean("armed", false),
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

    fun upsertPlace(c: Context, place: DwellPlace, makeActive: Boolean = true) {
        val normalized = place.normalized()
        val places = getPlaces(c).toMutableList()
        val index = places.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            places[index] = normalized
        } else {
            places.add(normalized)
        }
        val savedPlaces = savePlaces(c, places)
        val savedPlace = savedPlaces.firstOrNull { it.id == normalized.id } ?: normalized
        val editor = p(c).edit()
        if (makeActive) {
            editor.putString(ACTIVE_PLACE_ID_KEY, savedPlace.id)
        }
        editor
            .putLong("lat", savedPlace.latitude.toRawBits())
            .putLong("lon", savedPlace.longitude.toRawBits())
            .putFloat("radius", savedPlace.radiusMeters)
            .putInt("duration_min", savedPlace.durationMinutes)
            .putString("place_label", savedPlace.safeLabel)
            .putBoolean("armed", savedPlaces.any { it.monitoringEnabled })
            .apply()
    }

    fun deletePlace(c: Context, placeId: String) {
        val remaining = getPlaces(c).filterNot { it.id == placeId }
        savePlaces(c, remaining)
        val prefs = p(c)
        val editor = prefs.edit()
        if (prefs.getString(ACTIVE_PLACE_ID_KEY, null) == placeId) {
            remaining.firstOrNull()?.let {
                editor.putString(ACTIVE_PLACE_ID_KEY, it.id)
                editor.putLegacyPlaceFields(it)
            } ?: editor.remove(ACTIVE_PLACE_ID_KEY)
        }
        if (prefs.getString(TIMER_PLACE_ID_KEY, null) == placeId) {
            editor.remove(TIMER_PLACE_ID_KEY)
        }
        if (prefs.getString(PROMPT_PLACE_ID_KEY, null) == placeId) {
            editor.remove(PROMPT_PLACE_ID_KEY)
        }
        editor.putBoolean("armed", remaining.any { it.monitoringEnabled }).apply()
        setRegisteredPlaces(c, getRegisteredPlaceIds(c) - placeId)
    }

    fun clearPlace(c: Context) {
        clearArrivalRuntime(c)
        DwellInsights.clear(c)
        p(c).edit()
            .remove(PLACES_KEY)
            .remove(ACTIVE_PLACE_ID_KEY)
            .remove(TIMER_PLACE_ID_KEY)
            .remove(PROMPT_PLACE_ID_KEY)
            .remove(REGISTERED_PLACE_IDS_KEY)
            .remove(MONITORING_ERROR_KEY)
            .remove(MONITORING_UPDATED_KEY)
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
            upsertPlace(c, it.withTimerDefaults(normalized, it.durationMinutes))
        } ?: p(c).edit().putFloat("radius", normalized).apply()
    }

    // Default 270 minutes = 4.5 hours
    fun getDurationMinutes(c: Context): Int =
        getActivePlace(c)?.durationMinutes ?: p(c).getInt("duration_min", 270)

    fun getDurationMinutes(c: Context, placeId: String?): Int =
        getPlace(c, placeId)?.durationMinutes ?: getDurationMinutes(c)

    fun setDurationMinutes(c: Context, min: Int) {
        val normalized = min.coerceIn(
            DwellPlace.MIN_DURATION_MINUTES,
            DwellPlace.MAX_DURATION_MINUTES,
        )
        getActivePlace(c)?.let {
            upsertPlace(c, it.withTimerDefaults(it.radiusMeters, normalized))
        } ?: p(c).edit().putInt("duration_min", normalized).apply()
    }

    fun setPlaceAutoStart(c: Context, placeId: String, enabled: Boolean): Boolean {
        val place = getPlace(c, placeId) ?: return false
        upsertPlace(c, place.withAutoStart(enabled), makeActive = true)
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
        upsertPlace(c, place.withMonitoring(armed), makeActive = true)
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

    fun setWatchPrompt(c: Context, prompt: String, placeId: String? = null) {
        val editor = p(c).edit()
            .putString("watch_prompt", prompt)
            .putLong("watch_prompt_updated", System.currentTimeMillis())
        if (placeId.isNullOrBlank()) {
            editor.remove(PROMPT_PLACE_ID_KEY)
        } else {
            editor.putString(PROMPT_PLACE_ID_KEY, placeId)
        }
        editor.apply()
    }

    fun clearWatchPrompt(c: Context) {
        p(c).edit()
            .putString("watch_prompt", WATCH_PROMPT_NONE)
            .putLong("watch_prompt_updated", System.currentTimeMillis())
            .remove(PROMPT_PLACE_ID_KEY)
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

    fun clearArrivalRuntime(c: Context) {
        val editor = p(c).edit()
        arrivalRuntimeKeysForPlaces(getPlaces(c).map { it.id })
            .forEach { editor.remove(it) }
        editor.apply()
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
            .remove(REGISTERED_PLACE_IDS_KEY)
            .remove(MONITORING_ERROR_KEY)
            .remove(MONITORING_UPDATED_KEY)
            .remove("lat")
            .remove("lon")
            .remove("radius")
            .remove("duration_min")
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
                    autoStart = true,
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
        val json = JSONArray()
        val normalizedPlaces = DwellPlace.normalizePlaces(places)
        normalizedPlaces
            .forEach { json.put(it.toJson()) }
        p(c).edit()
            .putString(PLACES_KEY, json.toString())
            .putBoolean("armed", normalizedPlaces.any { it.monitoringEnabled })
            .apply()
        return normalizedPlaces
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
            }.let(DwellPlace::normalizePlaces)
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
