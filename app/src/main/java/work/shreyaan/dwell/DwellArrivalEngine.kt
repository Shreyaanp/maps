package work.shreyaan.dwell

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

object DwellArrivalEngine {
    private const val ARRIVAL_FIX_TIMEOUT_MS = 4_500L
    private const val APPROACH_FIX_TIMEOUT_MS = 3_500L
    private const val APPROACH_PROBE_COOLDOWN_MS = 90_000L
    private const val MAX_ARRIVAL_FIX_AGE_MS = 60_000L
    private const val MAX_ARRIVAL_FIX_ACCURACY_METERS = 250f
    private const val MAX_APPROACH_BROAD_FIX_AGE_MS = 120_000L
    private const val MAX_APPROACH_BROAD_FIX_ACCURACY_METERS = 2_000f
    private const val PRECISE_MISSING_ASK_MAX_BROAD_ACCURACY_METERS = 100f
    private const val PRECISE_MISSING_ASK_MAX_BROAD_AGE_MS = 60_000L

    fun shouldProbeForGlobalMotion(motion: DwellMotion): Boolean =
        when (motion) {
            DwellMotion.STILL,
            DwellMotion.WALKING -> true
            DwellMotion.RUNNING,
            DwellMotion.ON_BICYCLE,
            DwellMotion.IN_VEHICLE,
            DwellMotion.UNKNOWN -> false
        }

    fun shouldBypassApproachCooldown(
        previousMotion: DwellMotion,
        triggerMotion: DwellMotion,
    ): Boolean {
        val previousPriority = approachMotionPriority(previousMotion)
        val triggerPriority = approachMotionPriority(triggerMotion)
        return previousPriority > 0 && triggerPriority > previousPriority
    }

    fun shouldConfirmApproachWithPreciseFix(confidence: ArrivalConfidence): Boolean =
        confidence.decision == ArrivalDecision.START_TIMER ||
            confidence.decision == ArrivalDecision.ASK_TO_START

    fun shouldAskWhenPreciseMissing(
        confidence: ArrivalConfidence,
        motion: DwellMotion,
        motionAgeMs: Long?,
        broadAccuracyMeters: Float?,
        broadLocationAgeMs: Long?,
    ): Boolean {
        if (confidence.decision == ArrivalDecision.WAIT) return false
        val accuracy = broadAccuracyMeters
            ?.takeIf { it.isFinite() && it >= 0f }
            ?: return false
        val locationAge = broadLocationAgeMs
            ?.takeIf { it >= 0L }
            ?: return false
        if (
            accuracy > PRECISE_MISSING_ASK_MAX_BROAD_ACCURACY_METERS ||
            locationAge > PRECISE_MISSING_ASK_MAX_BROAD_AGE_MS
        ) {
            return false
        }
        val freshMotion = if ((motionAgeMs ?: Long.MAX_VALUE) <= DwellConfidence.MOTION_FRESH_MS) {
            motion
        } else {
            DwellMotion.UNKNOWN
        }
        return freshMotion != DwellMotion.IN_VEHICLE &&
            freshMotion != DwellMotion.ON_BICYCLE &&
            freshMotion != DwellMotion.RUNNING
    }

    fun applyPlacePolicy(
        place: DwellPlace?,
        confidence: ArrivalConfidence,
    ): ArrivalConfidence =
        if (place?.autoStart == false && confidence.decision == ArrivalDecision.START_TIMER) {
            confidence.copy(decision = ArrivalDecision.ASK_TO_START)
        } else {
            confidence
        }

    fun chooseResolvedPlace(
        requestedPlaceId: String?,
        requestedPlace: DwellPlace?,
        activePlace: DwellPlace?,
    ): DwellPlace? =
        if (requestedPlaceId.isNullOrBlank()) {
            activePlace
        } else {
            requestedPlace
        }

    internal fun chooseApproachCandidate(
        places: List<DwellPlace>,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float? = null,
        promptPlace: DwellPlace?,
        activePlace: DwellPlace?,
    ): DwellPlace? {
        val monitoredPlaces = places.filter { it.monitoringEnabled }
        if (
            latitude != null &&
            longitude != null &&
            DwellPlace.hasValidCoordinates(latitude, longitude)
        ) {
            val broadAllowance = accuracyMeters
                ?.takeIf { it.isFinite() && it >= 0f }
                ?.coerceAtMost(MAX_APPROACH_BROAD_FIX_ACCURACY_METERS)
                ?: 0f
            return monitoredPlaces
                .map { place -> place to place.distanceMetersTo(latitude, longitude) }
                .filter { (place, distance) ->
                    distance <= DwellRadius.approachRadius(place.radiusMeters) + broadAllowance
                }
                .minByOrNull { (_, distance) -> distance }
                ?.first
        }

        return listOf(promptPlace, activePlace)
            .firstOrNull { it?.monitoringEnabled == true }
    }

    internal fun chooseZoneCandidate(
        places: List<DwellPlace>,
        latitude: Double?,
        longitude: Double?,
    ): DwellPlace? {
        val candidates = places
            .filter { it.monitoringEnabled }
            .distinctBy { it.id }
        if (candidates.isEmpty()) return null
        if (
            latitude != null &&
            longitude != null &&
            DwellPlace.hasValidCoordinates(latitude, longitude)
        ) {
            return candidates.minWithOrNull(
                compareBy<DwellPlace> { it.distanceMetersTo(latitude, longitude) }
                    .thenBy { it.createdAtMillis }
                    .thenBy { it.safeLabel.lowercase() }
                    .thenBy { it.id },
            )
        }
        return candidates.first()
    }

    fun runGeofenceEnterProbe(
        context: Context,
        placeId: String? = null,
        fallbackLocation: Location?,
        onComplete: () -> Unit,
    ) = runGeofenceEnterProbe(
        context = context,
        placeIds = listOfNotNull(placeId?.takeIf { it.isNotBlank() }),
        fallbackLocation = fallbackLocation,
        onComplete = onComplete,
    )

    fun runGeofenceEnterProbe(
        context: Context,
        placeIds: List<String>,
        fallbackLocation: Location?,
        onComplete: () -> Unit,
    ) {
        val appContext = context.applicationContext
        val places = placeIds
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { Prefs.getPlace(appContext, it) }
            .filter { it.monitoringEnabled }
        if (places.isEmpty()) {
            onComplete()
            return
        }
        resolveFreshArrivalLocation(
            context = appContext,
            fallbackLocation = fallbackLocation,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            timeoutMs = ARRIVAL_FIX_TIMEOUT_MS,
            allowCoarse = false,
        ) { location ->
            if (!TimerController.isRunning(appContext)) {
                val freshPlaces = refreshedMonitoredPlaces(appContext, places)
                chooseZoneCandidate(
                    places = freshPlaces,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                )?.let { place ->
                    handleArrival(
                        context = appContext,
                        place = place,
                        location = location,
                        source = "geofence",
                        geofenceEnter = true,
                        alreadyInsideCheck = false,
                    )
                }
            }
            onComplete()
        }
    }

    fun runApproachProbe(
        context: Context,
        triggerMotion: DwellMotion = Prefs.getMotion(context),
        placeId: String? = null,
        fallbackLocation: Location? = null,
        onComplete: () -> Unit,
    ) = runApproachProbe(
        context = context,
        triggerMotion = triggerMotion,
        placeIds = listOfNotNull(placeId?.takeIf { it.isNotBlank() }),
        fallbackLocation = fallbackLocation,
        onComplete = onComplete,
    )

    fun runApproachProbe(
        context: Context,
        triggerMotion: DwellMotion = Prefs.getMotion(context),
        placeIds: List<String>,
        fallbackLocation: Location? = null,
        onComplete: () -> Unit,
    ) {
        val appContext = context.applicationContext
        val requestedPlaceIds = placeIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val armedPlaces = approachProbePlaces(
            requestedPlaceIds = requestedPlaceIds,
            armedPlaces = Prefs.getArmedPlaces(appContext),
        )
        val cooldownPlaceId = requestedPlaceIds.singleOrNull()
        val bypassCooldown = shouldBypassApproachCooldown(
            previousMotion = Prefs.getLastApproachProbeMotion(appContext, cooldownPlaceId),
            triggerMotion = triggerMotion,
        )
        when {
            armedPlaces.isEmpty() -> {
                DwellDiagnostics.logLifecycle(
                    appContext,
                    source = "approach",
                    decision = "skipped",
                    detail = "no monitored places",
                )
                onComplete()
                return
            }
            TimerController.isRunning(appContext) -> {
                DwellDiagnostics.logLifecycle(
                    appContext,
                    source = "approach",
                    decision = "skipped",
                    detail = "timer already running",
                )
                onComplete()
                return
            }
            !Prefs.shouldRunApproachProbe(
                appContext,
                cooldownMs = APPROACH_PROBE_COOLDOWN_MS,
                placeId = cooldownPlaceId,
                triggerMotion = triggerMotion,
                bypassCooldown = bypassCooldown,
            ) -> {
                DwellDiagnostics.logLifecycle(
                    appContext,
                    source = "approach",
                    decision = "skipped",
                    detail = "probe cooldown active",
                )
                onComplete()
                return
            }
        }

        DwellDiagnostics.logLifecycle(
            appContext,
            source = "approach",
            decision = "probe",
            detail = "balanced location requested for ${armedPlaces.size} monitored place${if (armedPlaces.size == 1) "" else "s"}",
        )
        resolveFreshArrivalLocation(
            context = appContext,
            fallbackLocation = fallbackLocation,
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            timeoutMs = APPROACH_FIX_TIMEOUT_MS,
            allowCoarse = true,
        ) { location ->
            if (!TimerController.isRunning(appContext)) {
                handleApproachProbeResult(
                    context = appContext,
                    location = location,
                    candidatePlaces = armedPlaces,
                    onComplete = onComplete,
                )
            } else {
                onComplete()
            }
        }
    }

    internal fun approachProbePlaces(
        requestedPlaceIds: List<String>,
        armedPlaces: List<DwellPlace>,
    ): List<DwellPlace> {
        val monitoredPlaces = armedPlaces
            .filter { it.monitoringEnabled }
            .distinctBy { it.id }
        val requestedIds = requestedPlaceIds
            .filter { it.isNotBlank() }
            .distinct()
        if (requestedIds.isEmpty()) return monitoredPlaces
        return requestedIds.mapNotNull { requestedId ->
            monitoredPlaces.firstOrNull { it.id == requestedId }
        }
    }

    fun runFollowUpProbe(
        context: Context,
        placeId: String? = Prefs.getPromptPlaceId(context),
        onComplete: () -> Unit,
    ) {
        val appContext = context.applicationContext
        val followUpPlaceId = scopedFollowUpPlaceId(placeId)
        if (followUpPlaceId == null) {
            DwellDiagnostics.logLifecycle(
                appContext,
                source = "follow-up",
                decision = "skipped",
                detail = "no scoped place",
            )
            onComplete()
            return
        }
        val place = resolvePlace(appContext, followUpPlaceId)
        if (place == null || !place.monitoringEnabled || TimerController.isRunning(appContext)) {
            onComplete()
            return
        }

        resolveFreshArrivalLocation(
            context = appContext,
            fallbackLocation = null,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            timeoutMs = ARRIVAL_FIX_TIMEOUT_MS,
            allowCoarse = false,
        ) { location ->
            val freshPlace = refreshedMonitoredPlace(appContext, place)
            if (!TimerController.isRunning(appContext) && freshPlace != null) {
                handleArrival(
                    context = appContext,
                    place = freshPlace,
                    location = location,
                    source = "follow-up",
                    geofenceEnter = false,
                    alreadyInsideCheck = false,
                )
            }
            onComplete()
        }
    }

    internal fun scopedFollowUpPlaceId(placeId: String?): String? =
        placeId?.trim()?.takeIf { it.isNotBlank() }

    fun runGeofenceExitProbe(
        context: Context,
        placeId: String? = null,
        fallbackLocation: Location?,
        onComplete: () -> Unit,
    ) {
        val appContext = context.applicationContext
        val place = resolvePlace(appContext, placeId)
        if (place == null || !TimerController.isRunning(appContext)) {
            onComplete()
            return
        }
        val timerPlaceId = Prefs.getTimerPlaceId(appContext)
        if (timerPlaceId.isBlank() || timerPlaceId != place.id) {
            onComplete()
            return
        }
        val timerStartedAt = Prefs.getTimerStartedAt(appContext)
        val timerEnd = Prefs.getTimerEnd(appContext)
        if (
            !acceptsExitProbeTimerScope(
                probePlaceId = place.id,
                probeTimerPlaceId = timerPlaceId,
                probeTimerStartedAt = timerStartedAt,
                probeTimerEnd = timerEnd,
                currentTimerPlaceId = timerPlaceId,
                currentTimerStartedAt = timerStartedAt,
                currentTimerEnd = timerEnd,
                now = System.currentTimeMillis(),
            )
        ) {
            onComplete()
            return
        }

        resolveFreshArrivalLocation(
            context = appContext,
            fallbackLocation = fallbackLocation,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            timeoutMs = ARRIVAL_FIX_TIMEOUT_MS,
            allowCoarse = false,
        ) { location ->
            if (
                acceptsExitProbeTimerScope(
                    probePlaceId = place.id,
                    probeTimerPlaceId = timerPlaceId,
                    probeTimerStartedAt = timerStartedAt,
                    probeTimerEnd = timerEnd,
                    currentTimerPlaceId = Prefs.getTimerPlaceId(appContext),
                    currentTimerStartedAt = Prefs.getTimerStartedAt(appContext),
                    currentTimerEnd = Prefs.getTimerEnd(appContext),
                    now = System.currentTimeMillis(),
                ) &&
                shouldPromptExit(appContext, place.id, location)
            ) {
                Prefs.setWatchPrompt(
                    appContext,
                    Prefs.WATCH_PROMPT_LEAVE_EARLY,
                    place.id,
                )
                Notifications.notifyExitQuestion(appContext, Prefs.getTimerEnd(appContext))
                WearSync.pushState(appContext)
            }
            onComplete()
        }
    }

    internal fun acceptsExitProbeTimerScope(
        probePlaceId: String,
        probeTimerPlaceId: String,
        probeTimerStartedAt: Long,
        probeTimerEnd: Long,
        currentTimerPlaceId: String,
        currentTimerStartedAt: Long,
        currentTimerEnd: Long,
        now: Long,
    ): Boolean {
        if (currentTimerEnd <= now) return false
        val probeTimerPlace = probeTimerPlaceId.takeIf { it.isNotBlank() }
            ?: return false
        if (currentTimerPlaceId.isBlank()) return false
        if (probeTimerPlace != probePlaceId) return false
        return NotificationActionReceiver.acceptsScopedTimerAction(
            currentTimerPlaceId = currentTimerPlaceId,
            currentTimerStartedAt = currentTimerStartedAt,
            currentTimerEnd = currentTimerEnd,
            actionTimerPlaceId = probeTimerPlace,
            actionTimerStartedAt = probeTimerStartedAt,
            actionTimerEnd = probeTimerEnd,
        )
    }

    fun shouldScheduleFollowUp(
        confidence: ArrivalConfidence,
        distanceMeters: Float?,
        radiusMeters: Float,
        accuracyMeters: Float?,
        speedMetersPerSecond: Float? = null,
        observedInsideDurationMs: Long? = null,
        motion: DwellMotion = DwellMotion.UNKNOWN,
        motionAgeMs: Long? = null,
    ): Boolean =
        DwellConfidence.shouldScheduleFollowUp(
            decision = confidence.decision,
            distanceMeters = distanceMeters,
            radiusMeters = radiusMeters,
            accuracyMeters = accuracyMeters,
            speedMetersPerSecond = speedMetersPerSecond,
            observedInsideDurationMs = observedInsideDurationMs,
            motion = motion,
            motionAgeMs = motionAgeMs,
        )

    fun arrivalConfidence(
        context: Context,
        placeId: String? = null,
        location: Location?,
        source: String,
        geofenceEnter: Boolean,
        alreadyInsideCheck: Boolean,
    ): ArrivalConfidence {
        val place = resolvePlace(context, placeId)
            ?: return DwellConfidence.evaluateArrival(
                distanceMeters = null,
                radiusMeters = DwellRadius.DEFAULT_METERS,
                accuracyMeters = accuracyMeters(location),
                locationAgeMs = locationAgeMs(location),
                speedMetersPerSecond = speedMetersPerSecond(location),
                observedInsideDurationMs = null,
                motion = Prefs.getMotion(context),
                motionAgeMs = Prefs.getMotionAgeMs(context),
                geofenceEnter = geofenceEnter,
                alreadyInsideCheck = alreadyInsideCheck,
            )
        val distance = distanceFromZone(place, location)
        val accuracy = accuracyMeters(location)
        val locationAge = locationAgeMs(location)
        val speed = speedMetersPerSecond(location)
        val observedDuration = observedInsideDurationMs(context, place, distance, accuracy)
        val motion = Prefs.getMotion(context)
        val confidence = DwellConfidence.evaluateArrival(
            distanceMeters = distance,
            radiusMeters = place.radiusMeters,
            accuracyMeters = accuracy,
            locationAgeMs = locationAge,
            speedMetersPerSecond = speed,
            observedInsideDurationMs = observedDuration,
            motion = motion,
            motionAgeMs = Prefs.getMotionAgeMs(context),
            geofenceEnter = geofenceEnter,
            alreadyInsideCheck = alreadyInsideCheck,
        )
        DwellDiagnostics.logArrival(
            context,
            DwellDiagnosticSnapshot(
                source = source,
                confidence = confidence,
                distanceMeters = distance,
                accuracyMeters = accuracy,
                locationAgeMs = locationAge,
                speedMetersPerSecond = speed,
                observedInsideDurationMs = observedDuration,
                motion = motion,
                geofenceEnter = geofenceEnter,
                alreadyInsideCheck = alreadyInsideCheck,
            ),
        )
        return confidence
    }

    fun shouldPromptExit(context: Context, location: Location?): Boolean {
        return shouldPromptExit(context, Prefs.getTimerPlaceId(context), location)
    }

    fun shouldPromptExit(context: Context, placeId: String?, location: Location?): Boolean {
        val place = resolvePlace(context, placeId) ?: return false
        val distance = distanceFromZone(place, location)
        val accuracy = accuracyMeters(location)
        val locationAge = locationAgeMs(location)
        val motion = Prefs.getMotion(context)
        if (Prefs.isExitPromptSuppressed(context, place.id)) {
            DwellDiagnostics.logExitPrompt(
                context = context,
                prompted = false,
                distanceMeters = distance,
                accuracyMeters = accuracy,
                locationAgeMs = locationAge,
                motion = motion,
            )
            return false
        }
        val prompted = DwellConfidence.shouldPromptExit(
            distanceMeters = distance,
            radiusMeters = place.radiusMeters,
            accuracyMeters = accuracy,
            locationAgeMs = locationAge,
            motion = motion,
            motionAgeMs = Prefs.getMotionAgeMs(context),
        )
        DwellDiagnostics.logExitPrompt(
            context = context,
            prompted = prompted,
            distanceMeters = distance,
            accuracyMeters = accuracy,
            locationAgeMs = locationAge,
            motion = motion,
        )
        return prompted
    }

    private fun handleArrival(
        context: Context,
        place: DwellPlace,
        location: Location?,
        source: String,
        geofenceEnter: Boolean,
        alreadyInsideCheck: Boolean,
    ) {
        val freshPlace = refreshedMonitoredPlace(context, place) ?: return
        val confidence = arrivalConfidence(
            context = context,
            placeId = freshPlace.id,
            location = location,
            source = source,
            geofenceEnter = geofenceEnter,
            alreadyInsideCheck = alreadyInsideCheck,
        )
        applyArrivalDecision(context, freshPlace, confidence, location)
    }

    private fun handleApproachProbeResult(
        context: Context,
        location: Location?,
        candidatePlaces: List<DwellPlace>? = null,
        onComplete: () -> Unit,
    ) {
        val refreshedCandidates = refreshedMonitoredPlaces(
            context = context,
            places = candidatePlaces ?: Prefs.getArmedPlaces(context),
        )
        val place = selectProbePlace(context, location, refreshedCandidates)
        if (place == null) {
            DwellDiagnostics.logLifecycle(
                context,
                source = "approach",
                decision = "skipped",
                detail = "no candidate place after balanced fix",
            )
            onComplete()
            return
        }
        val broadConfidence = arrivalConfidence(
            context = context,
            placeId = place.id,
            location = location,
            source = "approach",
            geofenceEnter = false,
            alreadyInsideCheck = false,
        )

        if (!shouldConfirmApproachWithPreciseFix(broadConfidence)) {
            DwellDiagnostics.logLifecycle(
                context,
                source = "approach",
                decision = "broad-${broadConfidence.decision.name.lowercase()}",
                detail = "score ${broadConfidence.score}; precise fix not needed",
            )
            applyArrivalDecision(context, place, broadConfidence, location)
            onComplete()
            return
        }

        DwellDiagnostics.logLifecycle(
            context,
            source = "approach",
            decision = "precise-requested",
            detail = "broad ${broadConfidence.decision.name.lowercase()} score ${broadConfidence.score}",
        )
        resolveFreshArrivalLocation(
            context = context,
            fallbackLocation = null,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            timeoutMs = ARRIVAL_FIX_TIMEOUT_MS,
            allowCoarse = false,
        ) { preciseLocation ->
            val freshPlace = refreshedMonitoredPlace(context, place)
            if (!TimerController.isRunning(context) && freshPlace != null) {
                if (preciseLocation == null) {
                    ArrivalProbeReceiver.cancel(context)
                    val motion = Prefs.getMotion(context)
                    val motionAgeMs = Prefs.getMotionAgeMs(context)
                    if (
                        shouldAskWhenPreciseMissing(
                            confidence = broadConfidence,
                            motion = motion,
                            motionAgeMs = motionAgeMs,
                            broadAccuracyMeters = accuracyMeters(location),
                            broadLocationAgeMs = locationAgeMs(location),
                        )
                    ) {
                        DwellDiagnostics.logLifecycle(
                            context,
                            source = "approach",
                            decision = "precise-missing",
                            detail = "asking from broad score ${broadConfidence.score}",
                        )
                        requestStartConfirmation(context, freshPlace.id, broadConfidence.score)
                    } else {
                        Prefs.setPromptPlaceId(context, freshPlace.id)
                        val scheduled = ArrivalProbeReceiver.schedule(context, freshPlace.id)
                        DwellDiagnostics.logLifecycle(
                            context,
                            source = "approach",
                            decision = "precise-missing-wait",
                            detail = "motion ${motion.name.lowercase()} broad score ${broadConfidence.score}; follow-up ${if (scheduled) "scheduled" else "not scheduled"}",
                        )
                    }
                } else {
                    val precisePlace = selectProbePlace(
                        context = context,
                        location = preciseLocation,
                        candidatePlaces = refreshedCandidates,
                    ) ?: freshPlace
                    handleArrival(
                        context = context,
                        place = precisePlace,
                        location = preciseLocation,
                        source = "approach-precise",
                        geofenceEnter = false,
                        alreadyInsideCheck = false,
                    )
                }
            }
            onComplete()
        }
    }

    private fun applyArrivalDecision(
        context: Context,
        place: DwellPlace,
        confidence: ArrivalConfidence,
        location: Location?,
    ) {
        val currentPlace = refreshedMonitoredPlace(context, place) ?: return
        val adjustedConfidence = applyPlacePolicy(currentPlace, confidence)
        when (adjustedConfidence.decision) {
            ArrivalDecision.START_TIMER -> {
                ArrivalProbeReceiver.cancel(context)
                TimerController.startTimer(context, currentPlace.durationMinutes, currentPlace.id)
            }
            ArrivalDecision.ASK_TO_START -> {
                ArrivalProbeReceiver.cancel(context)
                requestStartConfirmation(context, currentPlace.id, adjustedConfidence.score)
            }
            ArrivalDecision.WAIT -> {
                val distance = distanceFromZone(currentPlace, location)
                val accuracy = accuracyMeters(location)
                val motion = Prefs.getMotion(context)
                val shouldFollowUp = shouldScheduleFollowUp(
                    confidence = adjustedConfidence,
                    distanceMeters = distance,
                    radiusMeters = currentPlace.radiusMeters,
                    accuracyMeters = accuracy,
                    speedMetersPerSecond = speedMetersPerSecond(location),
                    observedInsideDurationMs = Prefs.getArrivalInsideDurationMs(context, currentPlace.id),
                    motion = motion,
                    motionAgeMs = Prefs.getMotionAgeMs(context),
                )
                if (shouldFollowUp) {
                    Prefs.setPromptPlaceId(context, currentPlace.id)
                    ArrivalProbeReceiver.schedule(context, currentPlace.id)
                }
            }
        }
    }

    internal fun shouldRequestStartConfirmation(
        currentPrompt: String,
        currentPromptPlaceId: String,
        placeId: String,
    ): Boolean =
        currentPrompt != Prefs.WATCH_PROMPT_START_TIMER ||
            currentPromptPlaceId != placeId

    fun requestStartConfirmation(context: Context, placeId: String, score: Int) {
        if (
            !shouldRequestStartConfirmation(
                currentPrompt = Prefs.getWatchPrompt(context),
                currentPromptPlaceId = Prefs.getPromptPlaceId(context),
                placeId = placeId,
            )
        ) {
            WearSync.pushState(context)
            return
        }
        Prefs.setWatchPrompt(
            context,
            Prefs.WATCH_PROMPT_START_TIMER,
            placeId,
            confidenceScore = score,
        )
        Notifications.notifyArrivalQuestion(context, score)
        WearSync.pushState(context)
    }

    private fun approachMotionPriority(motion: DwellMotion): Int =
        when (motion) {
            DwellMotion.IN_VEHICLE,
            DwellMotion.ON_BICYCLE,
            DwellMotion.RUNNING -> 1
            DwellMotion.WALKING -> 2
            DwellMotion.STILL -> 3
            DwellMotion.UNKNOWN -> 0
        }

    @SuppressLint("MissingPermission")
    private fun resolveFreshArrivalLocation(
        context: Context,
        fallbackLocation: Location?,
        priority: Int,
        timeoutMs: Long,
        allowCoarse: Boolean,
        onResult: (Location?) -> Unit,
    ) {
        if (!hasLocationPermission(context, allowCoarse)) {
            onResult(fallbackLocation.takeIf(::isValidArrivalLocation))
            return
        }

        val handler = Handler(Looper.getMainLooper())
        val cancellation = CancellationTokenSource()
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationManager = context.getSystemService(LocationManager::class.java)
        var delivered = false
        var bestLocation: Location? = fallbackLocation.takeIf(::isValidArrivalLocation)
        var fusedUpdates: LocationCallback? = null
        var platformUpdates: android.location.LocationListener? = null
        lateinit var timeout: Runnable

        fun updateBest(location: Location?) {
            bestLocation = betterArrivalLocation(bestLocation, location)
        }

        fun stopLiveUpdates() {
            fusedUpdates?.let { fusedClient.removeLocationUpdates(it) }
            fusedUpdates = null
            platformUpdates?.let { runCatching { locationManager.removeUpdates(it) } }
            platformUpdates = null
        }

        fun deliver(location: Location?) {
            if (delivered) return
            delivered = true
            cancellation.cancel()
            handler.removeCallbacks(timeout)
            stopLiveUpdates()
            onResult(betterArrivalLocation(bestLocation, location))
        }

        fun platformLastKnownLocation(): Location? =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            ).fold(null as Location?) { best, provider ->
                val location = runCatching {
                    locationManager.getLastKnownLocation(provider)
                }.onFailure { e ->
                    Log.w("DwellArrival", "Raw $provider last-known lookup failed", e)
                }.getOrNull()
                betterArrivalLocation(best, location)
            }

        timeout = Runnable { deliver(null) }
        handler.postDelayed(timeout, timeoutMs)

        updateBest(platformLastKnownLocation())
        if (isGoodArrivalFix(bestLocation, allowCoarse)) {
            deliver(bestLocation)
            return
        }

        runCatching {
            val liveRequest = LocationRequest.Builder(priority, 1_000L)
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdates(4)
                .setDurationMillis(timeoutMs)
                .setWaitForAccurateLocation(priority == Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach(::updateBest)
                    if (isGoodArrivalFix(bestLocation, allowCoarse)) {
                        deliver(bestLocation)
                    }
                }
            }
            fusedUpdates = callback
            fusedClient.requestLocationUpdates(liveRequest, callback, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    Log.w("DwellArrival", "Live fused arrival updates failed", e)
                }
        }.onFailure { e ->
            Log.w("DwellArrival", "Live fused arrival update registration failed", e)
        }

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    val listener = platformUpdates ?: android.location.LocationListener { location ->
                        updateBest(location)
                        if (isGoodArrivalFix(bestLocation, allowCoarse)) {
                            deliver(bestLocation)
                        }
                    }.also { platformUpdates = it }
                    locationManager.requestLocationUpdates(
                        provider,
                        1_000L,
                        0f,
                        listener,
                        Looper.getMainLooper(),
                    )
                }
            }.onFailure { e ->
                Log.w("DwellArrival", "Raw $provider arrival updates failed", e)
            }
        }

        runCatching {
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    updateBest(location)
                    if (isGoodArrivalFix(bestLocation, allowCoarse)) {
                        deliver(bestLocation)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DwellArrival", "Fused last-known arrival lookup failed", e)
                }
        }.onFailure { e ->
            Log.w("DwellArrival", "Fused last-known arrival registration failed", e)
        }

        runCatching {
            fusedClient.getCurrentLocation(priority, cancellation.token)
                .addOnSuccessListener { location ->
                    updateBest(location)
                    if (isGoodArrivalFix(bestLocation, allowCoarse)) {
                        deliver(bestLocation)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DwellArrival", "Current arrival location lookup failed", e)
                }
        }.onFailure { e ->
            Log.w("DwellArrival", "Current arrival location registration failed", e)
        }
    }

    private fun hasLocationPermission(context: Context, allowCoarse: Boolean): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fine) return true
        return allowCoarse &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun betterArrivalLocation(fallback: Location?, fresh: Location?): Location? {
        val fallbackValid = fallback.takeIf(::isValidArrivalLocation)
        val freshValid = fresh.takeIf(::isValidArrivalLocation)
        if (fallbackValid == null) return freshValid
        if (freshValid == null) return fallbackValid

        val fallbackAge = locationAgeMs(fallbackValid) ?: Long.MAX_VALUE
        val freshAge = locationAgeMs(freshValid) ?: Long.MAX_VALUE
        val fallbackAccuracy = accuracyMeters(fallbackValid) ?: Float.MAX_VALUE
        val freshAccuracy = accuracyMeters(freshValid) ?: Float.MAX_VALUE
        return if (
            shouldReplaceArrivalLocation(
                currentAgeMs = fallbackAge,
                currentAccuracyMeters = fallbackAccuracy,
                candidateAgeMs = freshAge,
                candidateAccuracyMeters = freshAccuracy,
            )
        ) {
            freshValid
        } else {
            fallbackValid
        }
    }

    internal fun shouldReplaceArrivalLocation(
        currentAgeMs: Long?,
        currentAccuracyMeters: Float?,
        candidateAgeMs: Long?,
        candidateAccuracyMeters: Float?,
    ): Boolean {
        val currentAge = currentAgeMs ?: Long.MAX_VALUE
        val candidateAge = candidateAgeMs ?: Long.MAX_VALUE
        val currentAccuracy = currentAccuracyMeters ?: Float.MAX_VALUE
        val candidateAccuracy = candidateAccuracyMeters ?: Float.MAX_VALUE
        return when {
            candidateAge + 15_000L < currentAge -> true
            currentAge + 15_000L < candidateAge -> false
            candidateAccuracy < currentAccuracy -> true
            else -> false
        }
    }

    private fun isValidArrivalLocation(location: Location?): Boolean =
        DwellLocationSanity.isUsableDwellLocation(location, allowMock = BuildConfig.DEBUG)

    private fun isGoodArrivalFix(location: Location?, allowBroad: Boolean): Boolean {
        if (!isValidArrivalLocation(location)) return false
        val age = locationAgeMs(location) ?: Long.MAX_VALUE
        val accuracy = accuracyMeters(location) ?: Float.MAX_VALUE
        return age <= MAX_ARRIVAL_FIX_AGE_MS && accuracy <= MAX_ARRIVAL_FIX_ACCURACY_METERS ||
            allowBroad && isGoodApproachBroadFix(age, accuracy)
    }

    internal fun isGoodApproachBroadFix(
        locationAgeMs: Long?,
        accuracyMeters: Float?,
    ): Boolean {
        val age = locationAgeMs?.takeIf { it >= 0L } ?: return false
        val accuracy = accuracyMeters
            ?.takeIf { it.isFinite() && it >= 0f }
            ?: return false
        return age <= MAX_APPROACH_BROAD_FIX_AGE_MS &&
            accuracy <= MAX_APPROACH_BROAD_FIX_ACCURACY_METERS
    }

    private fun observedInsideDurationMs(
        context: Context,
        place: DwellPlace,
        distanceMeters: Float?,
        accuracyMeters: Float?,
    ): Long? {
        if (distanceMeters == null) {
            return Prefs.getArrivalInsideDurationMs(context, place.id)
        }
        val accuracyAllowance = (accuracyMeters ?: 150f).coerceIn(50f, 150f)
        val insideOrNear = distanceMeters <= place.radiusMeters + accuracyAllowance
        return Prefs.updateArrivalObservation(context, place.id, insideOrNear)
    }

    private fun distanceFromZone(place: DwellPlace, location: Location?): Float? {
        if (!isValidArrivalLocation(location)) return null
        location ?: return null
        return place.distanceMetersTo(location.latitude, location.longitude)
    }

    private fun resolvePlace(context: Context, placeId: String?): DwellPlace? =
        chooseResolvedPlace(
            requestedPlaceId = placeId,
            requestedPlace = placeId
                ?.takeIf { it.isNotBlank() }
                ?.let { Prefs.getPlace(context, it) },
            activePlace = Prefs.getActivePlace(context),
        )

    internal fun refreshedMonitoredPlace(
        snapshot: DwellPlace,
        current: DwellPlace?,
    ): DwellPlace? =
        current?.takeIf { it.id == snapshot.id && it.monitoringEnabled }

    private fun refreshedMonitoredPlace(context: Context, snapshot: DwellPlace): DwellPlace? =
        refreshedMonitoredPlace(snapshot, Prefs.getSavedPlace(context, snapshot.id))

    internal fun refreshedMonitoredPlaces(
        snapshots: List<DwellPlace>,
        currentPlaces: List<DwellPlace>,
    ): List<DwellPlace> {
        val currentById = currentPlaces.associateBy { it.id }
        return snapshots
            .distinctBy { it.id }
            .mapNotNull { snapshot -> refreshedMonitoredPlace(snapshot, currentById[snapshot.id]) }
    }

    private fun refreshedMonitoredPlaces(
        context: Context,
        places: List<DwellPlace>,
    ): List<DwellPlace> =
        refreshedMonitoredPlaces(places, Prefs.getPlaces(context))

    private fun selectProbePlace(
        context: Context,
        location: Location?,
        candidatePlaces: List<DwellPlace>? = null,
    ): DwellPlace? {
        val validLocation = location.takeIf(::isValidArrivalLocation)
        return chooseApproachCandidate(
            places = candidatePlaces ?: Prefs.getArmedPlaces(context),
            latitude = validLocation?.latitude,
            longitude = validLocation?.longitude,
            accuracyMeters = accuracyMeters(validLocation),
            promptPlace = Prefs.getPlace(context, Prefs.getPromptPlaceId(context)),
            activePlace = Prefs.getActivePlace(context),
        )
    }

    private fun accuracyMeters(location: Location?): Float? =
        location?.takeIf { isValidArrivalLocation(it) && it.hasAccuracy() }?.accuracy

    private fun speedMetersPerSecond(location: Location?): Float? =
        location?.takeIf { isValidArrivalLocation(it) && it.hasSpeed() }?.speed

    private fun locationAgeMs(location: Location?): Long? {
        if (!isValidArrivalLocation(location)) return null
        location ?: return null
        return ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L)
            .coerceAtLeast(0L)
    }
}
