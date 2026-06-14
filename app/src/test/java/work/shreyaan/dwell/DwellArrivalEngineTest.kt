package work.shreyaan.dwell

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellArrivalEngineTest {
    @Test
    fun globalMotionProbeRunsForWalkingAndStillness() {
        assertTrue(DwellArrivalEngine.shouldProbeForGlobalMotion(DwellMotion.STILL))
        assertTrue(DwellArrivalEngine.shouldProbeForGlobalMotion(DwellMotion.WALKING))
    }

    @Test
    fun globalMotionProbeSkipsTransitAndUnknownTransitions() {
        assertFalse(DwellArrivalEngine.shouldProbeForGlobalMotion(DwellMotion.RUNNING))
        assertFalse(DwellArrivalEngine.shouldProbeForGlobalMotion(DwellMotion.ON_BICYCLE))
        assertFalse(DwellArrivalEngine.shouldProbeForGlobalMotion(DwellMotion.IN_VEHICLE))
        assertFalse(DwellArrivalEngine.shouldProbeForGlobalMotion(DwellMotion.UNKNOWN))
    }

    @Test
    fun approachCooldownCanBeBypassedByStrongerArrivalMotion() {
        assertTrue(
            DwellArrivalEngine.shouldBypassApproachCooldown(
                previousMotion = DwellMotion.IN_VEHICLE,
                triggerMotion = DwellMotion.WALKING,
            )
        )
        assertTrue(
            DwellArrivalEngine.shouldBypassApproachCooldown(
                previousMotion = DwellMotion.WALKING,
                triggerMotion = DwellMotion.STILL,
            )
        )
        assertTrue(
            DwellArrivalEngine.shouldBypassApproachCooldown(
                previousMotion = DwellMotion.ON_BICYCLE,
                triggerMotion = DwellMotion.STILL,
            )
        )
    }

    @Test
    fun approachCooldownStillBlocksRepeatedOrWeakerMotion() {
        assertFalse(
            DwellArrivalEngine.shouldBypassApproachCooldown(
                previousMotion = DwellMotion.WALKING,
                triggerMotion = DwellMotion.WALKING,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldBypassApproachCooldown(
                previousMotion = DwellMotion.STILL,
                triggerMotion = DwellMotion.WALKING,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldBypassApproachCooldown(
                previousMotion = DwellMotion.UNKNOWN,
                triggerMotion = DwellMotion.STILL,
            )
        )
    }

    @Test
    fun approachProbeConfirmsStartAndAskDecisionsWithPreciseFix() {
        assertTrue(
            DwellArrivalEngine.shouldConfirmApproachWithPreciseFix(
                ArrivalConfidence(ArrivalDecision.START_TIMER, 80),
            )
        )
        assertTrue(
            DwellArrivalEngine.shouldConfirmApproachWithPreciseFix(
                ArrivalConfidence(ArrivalDecision.ASK_TO_START, 50),
            )
        )
    }

    @Test
    fun approachProbeDoesNotEscalateWaitDecision() {
        assertFalse(
            DwellArrivalEngine.shouldConfirmApproachWithPreciseFix(
                ArrivalConfidence(ArrivalDecision.WAIT, 20),
            )
        )
    }

    @Test
    fun missingPreciseFixCanAskFromBroadNonTransitEvidence() {
        assertTrue(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.STILL,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
        assertTrue(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.START_TIMER, 74),
                motion = DwellMotion.UNKNOWN,
                motionAgeMs = null,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
    }

    @Test
    fun missingPreciseFixDoesNotAskFromCoarseOrStaleBroadEvidence() {
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.STILL,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 180f,
                broadLocationAgeMs = 12_000L,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.STILL,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 90_000L,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.STILL,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = null,
                broadLocationAgeMs = 12_000L,
            )
        )
    }

    @Test
    fun missingPreciseFixWaitsForFreshTransitEvidence() {
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.IN_VEHICLE,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.ON_BICYCLE,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.RUNNING,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
    }

    @Test
    fun staleTransitEvidenceDoesNotBlockMissingPreciseAsk() {
        assertTrue(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
                motion = DwellMotion.IN_VEHICLE,
                motionAgeMs = DwellConfidence.MOTION_FRESH_MS + 1_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
    }

    @Test
    fun missingPreciseFixDoesNotAskFromWaitDecision() {
        assertFalse(
            DwellArrivalEngine.shouldAskWhenPreciseMissing(
                confidence = ArrivalConfidence(ArrivalDecision.WAIT, 20),
                motion = DwellMotion.STILL,
                motionAgeMs = 5_000L,
                broadAccuracyMeters = 45f,
                broadLocationAgeMs = 12_000L,
            )
        )
    }

    @Test
    fun placePolicyKeepsAutoStartDecisionWhenEnabled() {
        val place = testPlace("office", autoStart = true)
        val confidence = ArrivalConfidence(ArrivalDecision.START_TIMER, 86)

        assertEquals(
            confidence,
            DwellArrivalEngine.applyPlacePolicy(place, confidence),
        )
    }

    @Test
    fun placePolicyAsksWhenAutoStartIsDisabled() {
        val place = testPlace("office", autoStart = false)

        assertEquals(
            ArrivalConfidence(ArrivalDecision.ASK_TO_START, 86),
            DwellArrivalEngine.applyPlacePolicy(
                place,
                ArrivalConfidence(ArrivalDecision.START_TIMER, 86),
            ),
        )
    }

    @Test
    fun placePolicyDoesNotChangeAskOrWaitDecisions() {
        val place = testPlace("office", autoStart = false)

        assertEquals(
            ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
            DwellArrivalEngine.applyPlacePolicy(
                place,
                ArrivalConfidence(ArrivalDecision.ASK_TO_START, 54),
            ),
        )
        assertEquals(
            ArrivalConfidence(ArrivalDecision.WAIT, 20),
            DwellArrivalEngine.applyPlacePolicy(
                place,
                ArrivalConfidence(ArrivalDecision.WAIT, 20),
            ),
        )
    }

    @Test
    fun blankPlaceIdFallsBackToActivePlace() {
        val active = testPlace("active")

        assertEquals(
            active,
            DwellArrivalEngine.chooseResolvedPlace(
                requestedPlaceId = null,
                requestedPlace = null,
                activePlace = active,
            ),
        )
        assertEquals(
            active,
            DwellArrivalEngine.chooseResolvedPlace(
                requestedPlaceId = "",
                requestedPlace = null,
                activePlace = active,
            ),
        )
    }

    @Test
    fun explicitPlaceIdUsesOnlyRequestedPlace() {
        val active = testPlace("active")
        val requested = testPlace("requested")

        assertEquals(
            requested,
            DwellArrivalEngine.chooseResolvedPlace(
                requestedPlaceId = requested.id,
                requestedPlace = requested,
                activePlace = active,
            ),
        )
    }

    @Test
    fun explicitMissingPlaceIdDoesNotFallBackToActivePlace() {
        val active = testPlace("active")

        assertEquals(
            null,
            DwellArrivalEngine.chooseResolvedPlace(
                requestedPlaceId = "deleted-place",
                requestedPlace = null,
                activePlace = active,
            ),
        )
    }

    @Test
    fun approachCandidateChoosesNearestMonitoredPlaceInsideWakeRing() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)
        val gym = testPlace("gym", latitude = 17.0030, longitude = 78.0000)

        assertEquals(
            office,
            DwellArrivalEngine.chooseApproachCandidate(
                places = listOf(gym, office),
                latitude = 17.0010,
                longitude = 78.0000,
                promptPlace = null,
                activePlace = null,
            ),
        )
    }

    @Test
    fun approachCandidateSkipsPlacesOutsideWakeRing() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)

        assertEquals(
            null,
            DwellArrivalEngine.chooseApproachCandidate(
                places = listOf(office),
                latitude = 17.0100,
                longitude = 78.0000,
                promptPlace = null,
                activePlace = office,
            ),
        )
    }

    @Test
    fun approachCandidateUsesBroadAccuracyAsWakeAllowance() {
        val office = testPlace("office", latitude = 17.0000, longitude = 78.0000)

        assertEquals(
            null,
            DwellArrivalEngine.chooseApproachCandidate(
                places = listOf(office),
                latitude = 17.0075,
                longitude = 78.0000,
                accuracyMeters = null,
                promptPlace = null,
                activePlace = office,
            ),
        )
        assertEquals(
            office,
            DwellArrivalEngine.chooseApproachCandidate(
                places = listOf(office),
                latitude = 17.0075,
                longitude = 78.0000,
                accuracyMeters = 600f,
                promptPlace = null,
                activePlace = office,
            ),
        )
    }

    @Test
    fun approachCandidateFallsBackToPromptOrActivePlaceWithoutUsableFix() {
        val active = testPlace("active")
        val prompt = testPlace("prompt")

        assertEquals(
            prompt,
            DwellArrivalEngine.chooseApproachCandidate(
                places = listOf(active, prompt),
                latitude = null,
                longitude = null,
                promptPlace = prompt,
                activePlace = active,
            ),
        )
        assertEquals(
            active,
            DwellArrivalEngine.chooseApproachCandidate(
                places = listOf(active),
                latitude = Double.NaN,
                longitude = 78.0,
                promptPlace = null,
                activePlace = active,
            ),
        )
    }

    @Test
    fun broadApproachFixAcceptsFreshApproximateLocationForWakeOnly() {
        assertTrue(
            DwellArrivalEngine.isGoodApproachBroadFix(
                locationAgeMs = 45_000L,
                accuracyMeters = 1_500f,
            )
        )
    }

    @Test
    fun broadApproachFixRejectsStaleOrExtremelyCoarseLocations() {
        assertFalse(
            DwellArrivalEngine.isGoodApproachBroadFix(
                locationAgeMs = 180_000L,
                accuracyMeters = 1_500f,
            )
        )
        assertFalse(
            DwellArrivalEngine.isGoodApproachBroadFix(
                locationAgeMs = 45_000L,
                accuracyMeters = 3_000f,
            )
        )
        assertFalse(
            DwellArrivalEngine.isGoodApproachBroadFix(
                locationAgeMs = 45_000L,
                accuracyMeters = null,
            )
        )
    }

    @Test
    fun arrivalLocationPolicyPrefersMuchFresherCandidate() {
        assertTrue(
            DwellArrivalEngine.shouldReplaceArrivalLocation(
                currentAgeMs = 50_000L,
                currentAccuracyMeters = 20f,
                candidateAgeMs = 5_000L,
                candidateAccuracyMeters = 120f,
            )
        )
    }

    @Test
    fun arrivalLocationPolicyKeepsMuchFresherCurrentFix() {
        assertFalse(
            DwellArrivalEngine.shouldReplaceArrivalLocation(
                currentAgeMs = 5_000L,
                currentAccuracyMeters = 120f,
                candidateAgeMs = 50_000L,
                candidateAccuracyMeters = 20f,
            )
        )
    }

    @Test
    fun arrivalLocationPolicyUsesAccuracyWhenFreshnessIsSimilar() {
        assertTrue(
            DwellArrivalEngine.shouldReplaceArrivalLocation(
                currentAgeMs = 20_000L,
                currentAccuracyMeters = 90f,
                candidateAgeMs = 12_000L,
                candidateAccuracyMeters = 25f,
            )
        )
        assertFalse(
            DwellArrivalEngine.shouldReplaceArrivalLocation(
                currentAgeMs = 12_000L,
                currentAccuracyMeters = 25f,
                candidateAgeMs = 20_000L,
                candidateAccuracyMeters = 90f,
            )
        )
    }

    @Test
    fun activityTransitionExitsClearMotionState() {
        assertEquals(
            DwellMotion.UNKNOWN,
            ActivityRecognitionManager.motionFrom(
                DetectedActivity.WALKING,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
            ),
        )
        assertEquals(
            DwellMotion.UNKNOWN,
            ActivityRecognitionManager.motionFrom(
                DetectedActivity.IN_VEHICLE,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
            ),
        )
    }

    @Test
    fun activityTransitionEntersMapToDwellMotion() {
        assertEquals(
            DwellMotion.STILL,
            ActivityRecognitionManager.motionFrom(
                DetectedActivity.STILL,
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            ),
        )
        assertEquals(
            DwellMotion.ON_BICYCLE,
            ActivityRecognitionManager.motionFrom(
                DetectedActivity.ON_BICYCLE,
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            ),
        )
        assertEquals(
            DwellMotion.WALKING,
            ActivityRecognitionManager.motionFrom(
                DetectedActivity.ON_FOOT,
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            ),
        )
    }

    private fun testPlace(
        id: String,
        autoStart: Boolean = true,
        latitude: Double = 17.0,
        longitude: Double = 78.0,
        radiusMeters: Float = DwellRadius.DEFAULT_METERS,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = id,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            durationMinutes = 270,
            monitoringEnabled = true,
            autoStart = autoStart,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
