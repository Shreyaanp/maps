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
        val zonePlaces = triggers
            .filter { it.type == DwellGeofenceType.ZONE }
            .map { it.place }
            .distinctBy { it.id }
        val approachPlaces = triggers
            .filter { it.type == DwellGeofenceType.APPROACH }
            .map { it.place }
            .distinctBy { it.id }

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // Don't restart an already-running timer (e.g. GPS jitter
                // bouncing in and out of the zone).
                if (TimerController.isRunning(context)) {
                    val currentPlaceId = Prefs.getTimerPlaceId(context)
                    val newPlace = zonePlaces.firstOrNull { it.id != currentPlaceId }
                    if (currentPlaceId.isNotBlank() && newPlace != null) {
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
                    val approachPlaceId = approachPlaces.singleOrNull()?.id
                    DwellArrivalEngine.runApproachProbe(
                        context = appContext,
                        triggerMotion = Prefs.getMotion(appContext),
                        placeId = approachPlaceId,
                        fallbackLocation = event.triggeringLocation,
                        onComplete = { pending.finish() },
                    )
                    return
                }

                val pending = goAsync()
                val appContext = context.applicationContext
                fun runNext(index: Int) {
                    if (index >= zonePlaces.size || TimerController.isRunning(appContext)) {
                        pending.finish()
                        return
                    }
                    DwellArrivalEngine.runGeofenceEnterProbe(
                        context = appContext,
                        placeId = zonePlaces[index].id,
                        fallbackLocation = event.triggeringLocation,
                        onComplete = { runNext(index + 1) },
                    )
                }
                runNext(0)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                if (zonePlaces.isEmpty()) return
                zonePlaces.forEach { Prefs.clearArrivalObservation(context, it.id) }
                if (!TimerController.isRunning(context)) return

                val timerPlaceId = Prefs.getTimerPlaceId(context)
                val exitPlaces = zonePlaces.filter { place ->
                    timerPlaceId.isBlank() || timerPlaceId == place.id
                }
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

        val fallbackRequest = event.triggeringLocation?.let {
            inferredRequestForLocation(
                places = Prefs.getArmedPlaces(context),
                latitude = it.latitude,
                longitude = it.longitude,
                transition = event.geofenceTransition,
            )
        } ?: return emptyList()
        val place = Prefs.getPlace(context, fallbackRequest.placeId) ?: return emptyList()
        return listOf(TriggeredPlace(place, fallbackRequest.type))
    }

    companion object {
        internal fun geofenceEventErrorMessage(errorCode: Int): String =
            "Geofence event error: ${GeofenceStatusCodes.getStatusCodeString(errorCode)}"

        internal fun geofenceEventErrorDetail(errorCode: Int): String =
            "event error $errorCode ${GeofenceStatusCodes.getStatusCodeString(errorCode)}"

        internal fun inferredRequestForLocation(
            places: List<DwellPlace>,
            latitude: Double?,
            longitude: Double?,
            transition: Int,
        ): DwellGeofenceRequest? {
            if (
                latitude == null ||
                longitude == null ||
                !DwellPlace.hasValidCoordinates(latitude, longitude)
            ) {
                return null
            }

            val candidates = places
                .asSequence()
                .filter { it.monitoringEnabled }
                .map { place -> place to place.distanceMetersTo(latitude, longitude) }
                .toList()

            if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                return candidates
                    .filter { (place, distance) -> distance <= DwellRadius.approachRadius(place.radiusMeters) }
                    .minByOrNull { (_, distance) -> distance }
                    ?.first
                    ?.let { DwellGeofenceRequest(it.id, DwellGeofenceType.ZONE) }
            }

            val zone = candidates
                .filter { (place, distance) -> distance <= DwellRadius.normalize(place.radiusMeters) }
                .minByOrNull { (_, distance) -> distance }
                ?.first
            if (zone != null) {
                return DwellGeofenceRequest(zone.id, DwellGeofenceType.ZONE)
            }

            return candidates
                .filter { (place, distance) -> distance <= DwellRadius.approachRadius(place.radiusMeters) }
                .minByOrNull { (_, distance) -> distance }
                ?.first
                ?.let { DwellGeofenceRequest(it.id, DwellGeofenceType.APPROACH) }
        }
    }
}
