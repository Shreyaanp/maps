package work.shreyaan.dwell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            handleGeofenceEventError(context.applicationContext, event.errorCode)
            return
        }
        if (!Prefs.isArmed(context) || Prefs.getArmedPlaces(context).isEmpty()) return

        val triggers = triggersForEvent(context, event)
        if (triggers.isEmpty()) return
        val eventLocation = event.triggeringLocation
        val zonePlaces = prioritizeTriggeredPlaces(
            places = triggers
                .filter { it.type == DwellGeofenceType.ZONE }
                .map { it.place }
                .distinctBy { it.id },
            latitude = eventLocation?.latitude,
            longitude = eventLocation?.longitude,
        )
        val approachPlaces = prioritizeTriggeredPlaces(
            places = triggers
                .filter { it.type == DwellGeofenceType.APPROACH }
                .map { it.place }
                .distinctBy { it.id },
            latitude = eventLocation?.latitude,
            longitude = eventLocation?.longitude,
        )

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // Don't restart an already-running timer (e.g. GPS jitter
                // bouncing in and out of the zone).
                if (TimerController.isRunning(context)) {
                    val currentPlaceId = Prefs.getTimerPlaceId(context)
                    val newPlace = switchPromptTargetForTriggeredEnter(
                        zonePlaces = zonePlaces,
                        currentPlaceId = currentPlaceId,
                    )
                    if (currentPlaceId.isNotBlank() && newPlace != null) {
                        if (Prefs.isSwitchPromptSuppressed(context, newPlace.id)) return
                        val currentPlace = Prefs.getPlace(context, currentPlaceId)
                        Prefs.setWatchPrompt(
                            context,
                            Prefs.WATCH_PROMPT_START_TIMER,
                            newPlace.id,
                        )
                        Notifications.notifySwitchPlaceQuestion(
                            context,
                            newPlaceLabel = newPlace.safeLabel,
                            currentPlaceLabel = currentPlace?.safeLabel ?: "current timer",
                        )
                        WearSync.pushState(context)
                    }
                    return
                }

                if (zonePlaces.isEmpty()) {
                    val pending = goAsync()
                    val appContext = context.applicationContext
                    DwellArrivalEngine.runApproachProbe(
                        context = appContext,
                        triggerMotion = Prefs.getMotion(appContext),
                        placeIds = approachPlaces.map { it.id },
                        fallbackLocation = event.triggeringLocation,
                        onComplete = { pending.finish() },
                    )
                    return
                }

                val pending = goAsync()
                val appContext = context.applicationContext
                DwellArrivalEngine.runGeofenceEnterProbe(
                    context = appContext,
                    placeIds = zonePlaces.map { it.id },
                    fallbackLocation = event.triggeringLocation,
                    onComplete = { pending.finish() },
                )
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                if (zonePlaces.isEmpty()) return
                zonePlaces.forEach { Prefs.clearArrivalObservation(context, it.id) }
                if (!TimerController.isRunning(context)) return

                val timerPlaceId = Prefs.getTimerPlaceId(context)
                val exitPlaces = exitProbePlacesForTimer(zonePlaces, timerPlaceId)
                if (exitPlaces.isEmpty()) return

                val pending = goAsync()
                val appContext = context.applicationContext
                fun runNext(index: Int) {
                    if (index >= exitPlaces.size || !TimerController.isRunning(appContext)) {
                        pending.finish()
                        return
                    }
                    DwellArrivalEngine.runGeofenceExitProbe(
                        context = appContext,
                        placeId = exitPlaces[index].id,
                        fallbackLocation = event.triggeringLocation,
                        onComplete = { runNext(index + 1) },
                    )
                }
                runNext(0)
            }
        }
    }

    private fun handleGeofenceEventError(context: Context, errorCode: Int) {
        val setupIssue = MonitoringPrerequisites.issueForContext(context)
        if (setupIssue != null) {
            MonitoringPrerequisites.markSetupNeeded(
                context = context,
                source = "geofence",
                issue = setupIssue,
            )
            return
        }

        val message = geofenceEventErrorMessage(errorCode)
        Prefs.setMonitoringError(context, message)
        DwellDiagnostics.logLifecycle(
            context = context,
            source = "geofence",
            decision = "error",
            detail = geofenceEventErrorDetail(errorCode),
        )
        WearSync.pushState(context)
    }

    private data class TriggeredPlace(
        val place: DwellPlace,
        val type: DwellGeofenceType,
    )

    private fun triggersForEvent(
        context: Context,
        event: GeofencingEvent,
    ): List<TriggeredPlace> {
        val triggered = event.triggeringGeofences
            ?.mapNotNull { geofence ->
                val request = DwellPlace.requestFromRequestId(geofence.requestId) ?: return@mapNotNull null
                val place = Prefs.getPlace(context, request.placeId) ?: return@mapNotNull null
                TriggeredPlace(place, request.type)
            }
            ?.distinctBy { "${it.type}:${it.place.id}" }
            .orEmpty()
        if (triggered.isNotEmpty()) return triggered

        val fallbackRequests = event.triggeringLocation?.let {
            inferredRequestsForLocation(
                places = Prefs.getArmedPlaces(context),
                latitude = it.latitude,
                longitude = it.longitude,
                transition = event.geofenceTransition,
            )
        }.orEmpty()
        return fallbackRequests.mapNotNull { request ->
            val place = Prefs.getPlace(context, request.placeId) ?: return@mapNotNull null
            TriggeredPlace(place, request.type)
        }
    }

    companion object {
        internal fun geofenceEventErrorMessage(errorCode: Int): String =
            when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
                    "Monitoring event error: location services unavailable"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
                    "Monitoring event error: too many monitored places"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
                    "Monitoring event error: too many monitoring requests"
                else ->
                    "Monitoring event error: code $errorCode"
            }

        internal fun geofenceEventErrorDetail(errorCode: Int): String =
            "event error $errorCode ${GeofenceStatusCodes.getStatusCodeString(errorCode)}"

        internal fun inferredRequestForLocation(
            places: List<DwellPlace>,
            latitude: Double?,
            longitude: Double?,
            transition: Int,
        ): DwellGeofenceRequest? =
            inferredRequestsForLocation(
                places = places,
                latitude = latitude,
                longitude = longitude,
                transition = transition,
            ).firstOrNull()

        internal fun inferredRequestsForLocation(
            places: List<DwellPlace>,
            latitude: Double?,
            longitude: Double?,
            transition: Int,
        ): List<DwellGeofenceRequest> {
            if (
                latitude == null ||
                longitude == null ||
                !DwellPlace.hasValidCoordinates(latitude, longitude)
            ) {
                return emptyList()
            }

            val candidates = places
                .filter { it.monitoringEnabled }
                .map { place -> place to place.distanceMetersTo(latitude, longitude) }
                .sortedWith(
                    compareBy<Pair<DwellPlace, Float>> { it.second }
                        .thenBy { it.first.createdAtMillis }
                        .thenBy { it.first.safeLabel.lowercase() }
                        .thenBy { it.first.id },
                )

            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                return candidates
                    .filter { (place, distance) -> distance <= DwellRadius.approachRadius(place.radiusMeters) }
                    .map { (place, _) -> DwellGeofenceRequest(place.id, DwellGeofenceType.ZONE) }
            }

            val zoneRequests = candidates
                .filter { (place, distance) -> distance <= DwellRadius.normalize(place.radiusMeters) }
                .map { (place, _) -> DwellGeofenceRequest(place.id, DwellGeofenceType.ZONE) }
            if (zoneRequests.isNotEmpty()) return zoneRequests

            return candidates
                .filter { (place, distance) -> distance <= DwellRadius.approachRadius(place.radiusMeters) }
                .map { (place, _) -> DwellGeofenceRequest(place.id, DwellGeofenceType.APPROACH) }
        }

        internal fun prioritizeTriggeredPlaces(
            places: List<DwellPlace>,
            latitude: Double?,
            longitude: Double?,
        ): List<DwellPlace> {
            val distinctPlaces = places
                .filter { it.monitoringEnabled }
                .distinctBy { it.id }
            if (
                latitude == null ||
                longitude == null ||
                !DwellPlace.hasValidCoordinates(latitude, longitude)
            ) {
                return distinctPlaces.sortedWith(
                    compareBy<DwellPlace> { it.createdAtMillis }
                        .thenBy { it.safeLabel.lowercase() }
                        .thenBy { it.id },
                )
            }

            return distinctPlaces.sortedWith(
                compareBy<DwellPlace> { it.distanceMetersTo(latitude, longitude) }
                    .thenBy { it.createdAtMillis }
                    .thenBy { it.safeLabel.lowercase() }
                    .thenBy { it.id },
            )
        }

        internal fun switchPromptTargetForTriggeredEnter(
            zonePlaces: List<DwellPlace>,
            currentPlaceId: String,
        ): DwellPlace? {
            val runningPlaceId = currentPlaceId.takeIf { it.isNotBlank() } ?: return null
            if (zonePlaces.any { it.id == runningPlaceId }) return null
            return zonePlaces.firstOrNull { it.id != runningPlaceId }
        }

        internal fun exitProbePlacesForTimer(
            zonePlaces: List<DwellPlace>,
            timerPlaceId: String,
        ): List<DwellPlace> =
            timerPlaceId
                .takeIf { it.isNotBlank() }
                ?.let { runningPlaceId -> zonePlaces.filter { it.id == runningPlaceId } }
                .orEmpty()
    }
}
