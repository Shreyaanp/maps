package work.shreyaan.dwell

enum class DwellMotion {
    UNKNOWN,
    STILL,
    WALKING,
    RUNNING,
    ON_BICYCLE,
    IN_VEHICLE,
}

enum class ArrivalDecision {
    START_TIMER,
    ASK_TO_START,
    WAIT,
}

data class ArrivalConfidence(
    val decision: ArrivalDecision,
    val score: Int,
)

object DwellConfidence {
    const val MOTION_FRESH_MS = 10 * 60 * 1000L
    private const val AUTO_START_MAX_ACCURACY_METERS = 100f
    private const val AUTO_START_MAX_LOCATION_AGE_MS = 60_000L
    private const val EXIT_PROMPT_MAX_ACCURACY_METERS = 150f
    private const val EXIT_PROMPT_MAX_LOCATION_AGE_MS = 60_000L
    private const val START_THRESHOLD = 65
    private const val ASK_THRESHOLD = 45

    fun evaluateArrival(
        distanceMeters: Float?,
        radiusMeters: Float,
        accuracyMeters: Float?,
        locationAgeMs: Long?,
        speedMetersPerSecond: Float? = null,
        observedInsideDurationMs: Long? = null,
        motion: DwellMotion,
        motionAgeMs: Long?,
        geofenceEnter: Boolean,
        alreadyInsideCheck: Boolean,
    ): ArrivalConfidence {
        val freshMotion = if ((motionAgeMs ?: Long.MAX_VALUE) <= MOTION_FRESH_MS) {
            motion
        } else {
            DwellMotion.UNKNOWN
        }
        val transitMotion = isTransitMotion(freshMotion)

        if (distanceMeters == null || accuracyMeters == null || locationAgeMs == null) {
            return if (geofenceEnter) {
                if (transitMotion) {
                    ArrivalConfidence(ArrivalDecision.WAIT, 0)
                } else {
                    ArrivalConfidence(ArrivalDecision.ASK_TO_START, ASK_THRESHOLD)
                }
            } else {
                ArrivalConfidence(ArrivalDecision.WAIT, 0)
            }
        }

        val radius = DwellRadius.normalize(radiusMeters)
        val accuracy = accuracyMeters.takeIf { it.isFinite() && it >= 0f } ?: Float.MAX_VALUE
        val distance = distanceMeters.takeIf { it.isFinite() && it >= 0f } ?: Float.MAX_VALUE
        val age = locationAgeMs.coerceAtLeast(0L)
        val speed = speedMetersPerSecond?.takeIf { it.isFinite() && it >= 0f }
        val insideDuration = observedInsideDurationMs?.coerceAtLeast(0L) ?: 0L

        val inside = distance <= radius
        val near = distance <= radius + accuracy.coerceIn(50f, 150f)
        val fullyInside = distance + accuracy <= radius
        var score = 0

        score += when {
            fullyInside -> 50
            inside -> 35
            near -> 15
            else -> -25
        }
        score += when {
            accuracy <= 35f -> 20
            accuracy <= 75f -> 14
            accuracy <= 150f -> 6
            else -> -10
        }
        score += when {
            age <= 15_000L -> 10
            age <= 60_000L -> 5
            else -> -20
        }
        if (geofenceEnter) score += 12
        if (alreadyInsideCheck) score += 12
        score += when {
            insideDuration >= 120_000L -> 14
            insideDuration >= 60_000L -> 8
            insideDuration >= 20_000L -> 4
            else -> 0
        }

        score += when (freshMotion) {
            DwellMotion.STILL -> 12
            DwellMotion.WALKING -> 6
            DwellMotion.RUNNING,
            DwellMotion.ON_BICYCLE -> 0
            DwellMotion.IN_VEHICLE -> -12
            DwellMotion.UNKNOWN -> 0
        }
        score += when {
            speed == null -> 0
            speed <= 0.8f -> 10
            speed <= 2.4f -> 5
            speed <= 6f -> -4
            else -> -30
        }

        score = score.coerceIn(0, 100)
        val preciseEnoughToStart =
            accuracy <= AUTO_START_MAX_ACCURACY_METERS &&
                age <= AUTO_START_MAX_LOCATION_AGE_MS
        val slowMeasuredSpeed = speed != null && speed <= 2.4f
        val fastMeasuredSpeed = speed != null && speed > 2.4f
        val hasStabilizingSignal =
            freshMotion == DwellMotion.STILL ||
                slowMeasuredSpeed ||
                insideDuration >= 20_000L ||
                alreadyInsideCheck
        val transitStillSettling =
            transitMotion &&
                !alreadyInsideCheck &&
                insideDuration < 60_000L
        val tooUnsettledToAsk = fastMeasuredSpeed || transitStillSettling
        val decision = when {
            !inside && !near -> ArrivalDecision.WAIT
            tooUnsettledToAsk -> ArrivalDecision.WAIT
            score >= START_THRESHOLD &&
                inside &&
                preciseEnoughToStart &&
                hasStabilizingSignal &&
                !transitMotion ->
                ArrivalDecision.START_TIMER
            score >= ASK_THRESHOLD -> ArrivalDecision.ASK_TO_START
            else -> ArrivalDecision.WAIT
        }
        return ArrivalConfidence(decision, score)
    }

    private fun isTransitMotion(motion: DwellMotion): Boolean =
        motion == DwellMotion.IN_VEHICLE ||
            motion == DwellMotion.ON_BICYCLE ||
            motion == DwellMotion.RUNNING

    fun shouldPromptExit(
        distanceMeters: Float?,
        radiusMeters: Float,
        accuracyMeters: Float?,
        locationAgeMs: Long?,
        motion: DwellMotion,
        motionAgeMs: Long?,
    ): Boolean {
        if (distanceMeters == null || accuracyMeters == null || locationAgeMs == null) return false

        val radius = DwellRadius.normalize(radiusMeters)
        val accuracy = accuracyMeters.takeIf { it.isFinite() && it >= 0f } ?: return false
        val age = locationAgeMs.takeIf { it >= 0L } ?: return false
        if (accuracy > EXIT_PROMPT_MAX_ACCURACY_METERS || age > EXIT_PROMPT_MAX_LOCATION_AGE_MS) {
            return false
        }

        val distance = distanceMeters.takeIf { it.isFinite() && it >= 0f } ?: return false
        val freshMotion = if ((motionAgeMs ?: Long.MAX_VALUE) <= MOTION_FRESH_MS) motion else DwellMotion.UNKNOWN
        val tolerance = accuracy.coerceIn(75f, 150f)
        val likelyStillInside = distance <= radius + tolerance

        return when {
            likelyStillInside && freshMotion != DwellMotion.IN_VEHICLE -> false
            else -> true
        }
    }

    fun shouldScheduleFollowUp(
        decision: ArrivalDecision,
        distanceMeters: Float?,
        radiusMeters: Float,
        accuracyMeters: Float?,
        speedMetersPerSecond: Float? = null,
        observedInsideDurationMs: Long? = null,
        motion: DwellMotion = DwellMotion.UNKNOWN,
        motionAgeMs: Long? = null,
    ): Boolean {
        if (decision != ArrivalDecision.WAIT || distanceMeters == null) return false
        val speed = speedMetersPerSecond?.takeIf { it.isFinite() && it >= 0f }
        if (speed != null && speed > 2.4f) return false

        val freshMotion = if ((motionAgeMs ?: Long.MAX_VALUE) <= MOTION_FRESH_MS) {
            motion
        } else {
            DwellMotion.UNKNOWN
        }
        val insideDuration = observedInsideDurationMs?.coerceAtLeast(0L) ?: 0L
        if (isTransitMotion(freshMotion) && insideDuration < 60_000L) return false

        val radius = DwellRadius.normalize(radiusMeters)
        val accuracyAllowance = (accuracyMeters ?: 150f).coerceIn(50f, 150f)
        val distance = distanceMeters.takeIf { it.isFinite() && it >= 0f } ?: return false
        return distance <= radius + accuracyAllowance
    }
}
