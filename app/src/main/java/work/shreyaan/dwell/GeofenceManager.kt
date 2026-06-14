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
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (!DwellPlace.hasValidCoordinates(lat, lon)) {
            onResult(false, "Invalid place location")
            return
        }
        val active = Prefs.getActivePlace(c)
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
        Prefs.upsertPlace(c, place, makeActive = true)
        refresh(c, onResult)
    }

    fun disarm(c: Context, onResult: (Boolean) -> Unit) {
        Prefs.setAllPlacesArmed(c, false)
        LocationServices.getGeofencingClient(c)
            .removeGeofences(pendingIntent(c))
            .addOnCompleteListener { task ->
                ArrivalProbeReceiver.cancel(c)
                Prefs.clearArrivalRuntime(c)
                Prefs.clearRegisteredPlaces(c)
                Prefs.setMonitoringError(c, null)
                ActivityRecognitionManager.stop(c)
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
        if (!Prefs.setPlaceArmed(c, placeId, enabled)) {
            onResult(false, "Could not update this place")
            return
        }
        refresh(c, onResult)
    }

    fun refreshOnAppOpen(
        c: Context,
        onResult: (Boolean, String?) -> Unit = { _, _ -> },
    ) {
        val armedPlaces = Prefs.getArmedPlaces(c)
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

    @SuppressLint("MissingPermission")
    fun refresh(c: Context, onResult: (Boolean, String?) -> Unit) {
        val client = LocationServices.getGeofencingClient(c)
        val pendingIntent = pendingIntent(c)
        val armedPlaces = Prefs.getArmedPlaces(c)
        val places = registrablePlaces(armedPlaces)

        fun failRegistration(message: String?) {
            val error = message ?: "Geofence registration failed"
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
            onResult(false, error)
        }

        client.removeGeofences(pendingIntent).addOnCompleteListener {
            if (places.isEmpty()) {
                ArrivalProbeReceiver.cancel(c)
                Prefs.clearArrivalRuntime(c)
                Prefs.clearRegisteredPlaces(c)
                ActivityRecognitionManager.stop(c)
                val invalidOnly = armedPlaces.isNotEmpty()
                val error = if (invalidOnly) "Saved place has invalid location" else null
                Prefs.setMonitoringError(c, error)
                DwellDiagnostics.logLifecycle(
                    c,
                    source = "monitoring",
                    decision = if (invalidOnly) "failed" else "inactive",
                    detail = error ?: "no monitored places",
                )
                WearSync.pushState(c)
                onResult(!invalidOnly, error)
                return@addOnCompleteListener
            }

            val setupIssue = MonitoringPrerequisites.issueForContext(c)
            if (setupIssue != null) {
                MonitoringPrerequisites.markSetupNeeded(
                    context = c,
                    source = "monitoring",
                    issue = setupIssue,
                )
                onResult(false, setupIssue.error)
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
                    ActivityRecognitionManager.start(c)
                    DwellDiagnostics.logLifecycle(
                        c,
                        source = "monitoring",
                        decision = "live",
                        detail = "${places.size} monitored place${if (places.size == 1) "" else "s"} registered with approach rings",
                    )
                    WearSync.pushState(c)
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    failRegistration(e.message)
                }
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
