package work.shreyaan.dwell

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    private const val APP_OPEN_REFRESH_INTERVAL_MS = 30 * 60 * 1_000L
    private val refreshLock = Any()
    private var refreshInFlight = false
    private var refreshQueued = false
    private val refreshCallbacks = mutableListOf<(Boolean, String?) -> Unit>()

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
        placeId: String? = null,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (!DwellPlace.hasValidCoordinates(lat, lon)) {
            onResult(false, "Invalid place location")
            return
        }
        val requestedPlaceId = placeId?.takeIf { it.isNotBlank() }
        val places = Prefs.getPlaces(c)
        val active = if (requestedPlaceId != null) {
            places.firstOrNull { it.id == requestedPlaceId }
        } else {
            Prefs.getActivePlace(c)
        }
        armMonitoringUpdateError(
            places = places,
            activePlaceId = requestedPlaceId ?: active?.id,
        )?.let { error ->
            onResult(false, error)
            return
        }
        val place = if (active != null) {
            active.copy(
                latitude = lat,
                longitude = lon,
                radiusMeters = radiusMeters,
            ).normalized()
        } else {
            DwellPlace.create(
                label = Prefs.getPlaceLabel(c),
                latitude = lat,
                longitude = lon,
                radiusMeters = radiusMeters,
                durationMinutes = Prefs.getDurationMinutes(c),
            )
        }.withMonitoring(true)
        Prefs.upsertPlace(c, place, makeActive = requestedPlaceId == null)
        refresh(c, onResult)
    }

    fun disarm(c: Context, onResult: (Boolean) -> Unit) {
        Prefs.setAllPlacesArmed(c, false)
        clearMonitoringPromptForAllPaused(c)
        LocationServices.getGeofencingClient(c)
            .removeGeofences(pendingIntent(c))
            .addOnCompleteListener { task ->
                ArrivalProbeReceiver.cancel(c)
                Prefs.clearArrivalRuntime(c)
                Prefs.clearRegisteredPlaces(c)
                Prefs.setMonitoringError(c, null)
                ActivityRecognitionManager.stop(c)
                Notifications.clearSetup(c)
                DwellDiagnostics.logLifecycle(
                    c,
                    source = "monitoring",
                    decision = if (task.isSuccessful) "inactive" else "disarm-local",
                    detail = if (task.isSuccessful) {
                        "all places paused"
                    } else {
                        "local monitoring cleared after geofence removal failure"
                    },
                )
                WearSync.pushState(c)
                onResult(task.isSuccessful)
            }
    }

    fun setPlaceMonitoring(
        c: Context,
        placeId: String,
        enabled: Boolean,
        onResult: (Boolean, String?) -> Unit,
    ) {
        placeMonitoringUpdateError(
            places = Prefs.getPlaces(c),
            placeId = placeId,
            enabled = enabled,
        )?.let { error ->
            onResult(false, error)
            return
        }
        if (!Prefs.setPlaceArmed(c, placeId, enabled)) {
            onResult(false, "Could not update this place")
            return
        }
        if (!enabled) {
            clearMonitoringPromptForPausedPlace(c, placeId)
            Prefs.clearArrivalRuntimeForPlace(c, placeId)
            if (Prefs.getArmedPlaces(c).isEmpty()) {
                clearMonitoringPromptForAllPaused(c)
            }
        }
        refresh(c, onResult)
    }

    private fun clearMonitoringPromptForPausedPlace(c: Context, placeId: String) {
        if (
            shouldClearMonitoringPromptForPausedPlace(
                prompt = Prefs.getWatchPrompt(c),
                promptPlaceId = Prefs.getPromptPlaceId(c),
                pausedPlaceId = placeId,
            )
        ) {
            Prefs.clearWatchPrompt(c)
            Notifications.clearMonitoringPrompts(c)
        }
    }

    private fun clearMonitoringPromptForAllPaused(c: Context) {
        if (shouldClearMonitoringPromptWhenAllPlacesPaused(Prefs.getWatchPrompt(c))) {
            Prefs.clearWatchPrompt(c)
            Notifications.clearMonitoringPrompts(c)
        }
    }

    fun refreshOnAppOpen(
        c: Context,
        onResult: (Boolean, String?) -> Unit = { _, _ -> },
    ) {
        val armedPlaces = Prefs.getArmedPlaces(c)
        val setupIssue = MonitoringPrerequisites.issueForContext(c)
        if (
            shouldReportSetupNeededOnAppOpen(
                armedPlaceIds = armedPlaces.map { it.id },
                hasSetupIssue = setupIssue != null,
            )
        ) {
            MonitoringPrerequisites.markSetupNeeded(
                context = c,
                source = "monitoring",
                issue = setupIssue!!,
            )
            onResult(false, setupIssue.error)
            return
        }
        val registrablePlaceIds = registrablePlaces(armedPlaces).map { it.id }
        if (
            !shouldRefreshOnAppOpen(
                armedPlaceIds = registrablePlaceIds,
                registeredPlaceIds = Prefs.getRegisteredPlaceIds(c),
                monitoringError = Prefs.getMonitoringError(c),
                lastUpdatedMillis = Prefs.getMonitoringUpdated(c),
                nowMillis = System.currentTimeMillis(),
            )
        ) {
            Notifications.clearSetup(c)
            onResult(true, null)
            return
        }

        DwellDiagnostics.logLifecycle(
            c,
            source = "monitoring",
            decision = "app-open-refresh",
            detail = "${registrablePlaceIds.size} monitored place${if (registrablePlaceIds.size == 1) "" else "s"} self-heal",
        )
        refresh(c, onResult)
    }

    internal fun shouldRefreshOnAppOpen(
        armedPlaceIds: Collection<String>,
        registeredPlaceIds: Set<String>,
        monitoringError: String,
        lastUpdatedMillis: Long,
        nowMillis: Long,
        refreshIntervalMs: Long = APP_OPEN_REFRESH_INTERVAL_MS,
    ): Boolean {
        val armedIds = armedPlaceIds.filter { it.isNotBlank() }.toSet()
        if (armedIds.isEmpty()) return false
        if (monitoringError.isNotBlank()) return true
        if (registeredPlaceIds != armedIds) return true
        if (lastUpdatedMillis <= 0L) return true
        return nowMillis - lastUpdatedMillis >= refreshIntervalMs
    }

    internal fun shouldReportSetupNeededOnAppOpen(
        armedPlaceIds: Collection<String>,
        hasSetupIssue: Boolean,
    ): Boolean =
        hasSetupIssue && armedPlaceIds.any { it.isNotBlank() }

    internal fun shouldClearMonitoringPromptForPausedPlace(
        prompt: String,
        promptPlaceId: String,
        pausedPlaceId: String,
    ): Boolean =
        isMonitoringPrompt(prompt) &&
            pausedPlaceId.isNotBlank() &&
            promptPlaceId == pausedPlaceId

    internal fun shouldClearMonitoringPromptWhenAllPlacesPaused(prompt: String): Boolean =
        isMonitoringPrompt(prompt)

    internal fun shouldClearSetupNotificationAfterRefresh(
        ok: Boolean,
        error: String?,
    ): Boolean =
        ok && error.isNullOrBlank()

    private fun isMonitoringPrompt(prompt: String): Boolean =
        prompt == Prefs.WATCH_PROMPT_START_TIMER ||
            prompt == Prefs.WATCH_PROMPT_LEAVE_EARLY

    @SuppressLint("MissingPermission")
    fun refresh(c: Context, onResult: (Boolean, String?) -> Unit) {
        val appContext = c.applicationContext
        val shouldStart = synchronized(refreshLock) {
            refreshCallbacks += onResult
            if (refreshInFlight) {
                refreshQueued = true
                false
            } else {
                refreshInFlight = true
                true
            }
        }
        if (shouldStart) runRefresh(appContext)
    }

    @SuppressLint("MissingPermission")
    private fun runRefresh(c: Context) {
        val client = LocationServices.getGeofencingClient(c)
        val pendingIntent = pendingIntent(c)

        fun failRegistration(message: String?) {
            val error = message ?: "Monitoring setup failed"
            ArrivalProbeReceiver.cancel(c)
            Prefs.clearRegisteredPlaces(c)
            Prefs.clearArrivalRuntime(c)
            Prefs.setMonitoringError(c, error)
            ActivityRecognitionManager.stop(c)
            DwellDiagnostics.logLifecycle(
                c,
                source = "monitoring",
                decision = "failed",
                detail = error.take(120),
            )
            WearSync.pushState(c)
            finishRefresh(c, false, error)
        }

        client.removeGeofences(pendingIntent).addOnCompleteListener {
            val armedPlaces = Prefs.getArmedPlaces(c)
            val places = registrablePlaces(armedPlaces)
            if (places.isEmpty()) {
                ArrivalProbeReceiver.cancel(c)
                Prefs.clearArrivalRuntime(c)
                Prefs.clearRegisteredPlaces(c)
                ActivityRecognitionManager.stop(c)
                val invalidOnly = armedPlaces.isNotEmpty()
                val error = if (invalidOnly) "Saved place has invalid location" else null
                Prefs.setMonitoringError(c, error)
                if (shouldClearSetupNotificationAfterRefresh(ok = !invalidOnly, error = error)) {
                    Notifications.clearSetup(c)
                }
                DwellDiagnostics.logLifecycle(
                    c,
                    source = "monitoring",
                    decision = if (invalidOnly) "failed" else "inactive",
                    detail = error ?: "no monitored places",
                )
                WearSync.pushState(c)
                finishRefresh(c, !invalidOnly, error)
                return@addOnCompleteListener
            }

            val setupIssue = MonitoringPrerequisites.issueForContext(c)
            if (setupIssue != null) {
                MonitoringPrerequisites.markSetupNeeded(
                    context = c,
                    source = "monitoring",
                    issue = setupIssue,
                )
                finishRefresh(c, false, setupIssue.error)
                return@addOnCompleteListener
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(places.flatMap(::geofencesForPlace))
                .build()

            val addTask = runCatching {
                client.addGeofences(request, pendingIntent)
            }.getOrElse { e ->
                failRegistration(e.message)
                return@addOnCompleteListener
            }

            addTask
                .addOnSuccessListener {
                    Prefs.setRegisteredPlaces(c, places.map { it.id })
                    Prefs.setMonitoringError(c, null)
                    Notifications.clearSetup(c)
                    ActivityRecognitionManager.start(c)
                    DwellDiagnostics.logLifecycle(
                        c,
                        source = "monitoring",
                        decision = "live",
                        detail = "${places.size} monitored place${if (places.size == 1) "" else "s"} registered with approach rings",
                    )
                    WearSync.pushState(c)
                    finishRefresh(c, true, null)
                }
                .addOnFailureListener { e ->
                    failRegistration(e.message)
                }
        }
    }

    private fun finishRefresh(c: Context, ok: Boolean, error: String?) {
        var callbacks: List<(Boolean, String?) -> Unit> = emptyList()
        val shouldRunQueued = synchronized(refreshLock) {
            if (refreshQueued) {
                refreshQueued = false
                true
            } else {
                refreshInFlight = false
                callbacks = refreshCallbacks.toList()
                refreshCallbacks.clear()
                false
            }
        }
        if (shouldRunQueued) {
            runRefresh(c.applicationContext)
        } else {
            callbacks.forEach { it(ok, error) }
        }
    }

    private fun geofencesForPlace(place: DwellPlace): List<Geofence> =
        listOf(zoneGeofenceForPlace(place), approachGeofenceForPlace(place))

    internal fun registrablePlaces(places: List<DwellPlace>): List<DwellPlace> =
        places.filter { place ->
            place.monitoringEnabled &&
                DwellPlace.hasValidCoordinates(place.latitude, place.longitude) &&
                DwellPlace.isValidPlaceId(place.id)
        }

    internal fun placeMonitoringUpdateError(
        places: List<DwellPlace>,
        placeId: String,
        enabled: Boolean,
    ): String? {
        val place = places.firstOrNull { it.id == placeId }
            ?: return "Saved place no longer exists"
        if (
            enabled &&
            !place.monitoringEnabled &&
            places.count { it.monitoringEnabled } >= DwellPlace.MAX_MONITORED_PLACES
        ) {
            return monitoredPlaceLimitMessage()
        }
        return null
    }

    internal fun armMonitoringUpdateError(
        places: List<DwellPlace>,
        activePlaceId: String?,
    ): String? =
        if (activePlaceId.isNullOrBlank()) {
            monitoredPlaceLimitMessage().takeIf {
                places.count { place -> place.monitoringEnabled } >= DwellPlace.MAX_MONITORED_PLACES
            }
        } else {
            placeMonitoringUpdateError(
                places = places,
                placeId = activePlaceId,
                enabled = true,
            )
        }

    internal fun monitoredPlaceLimitMessage(): String =
        "Dwell can monitor up to ${DwellPlace.MAX_MONITORED_PLACES} places. Pause another monitored place first."

    private fun zoneGeofenceForPlace(place: DwellPlace): Geofence =
        Geofence.Builder()
            .setRequestId(DwellPlace.zoneRequestId(place.id))
            .setCircularRegion(
                place.latitude,
                place.longitude,
                DwellRadius.normalize(place.radiusMeters),
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setNotificationResponsiveness(60_000)
            .build()

    private fun approachGeofenceForPlace(place: DwellPlace): Geofence =
        Geofence.Builder()
            .setRequestId(DwellPlace.approachRequestId(place.id))
            .setCircularRegion(
                place.latitude,
                place.longitude,
                DwellRadius.approachRadius(place.radiusMeters),
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(120_000)
            .build()
}
