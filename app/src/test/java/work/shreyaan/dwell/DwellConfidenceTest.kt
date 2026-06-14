package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellConfidenceTest {
    @Test
    fun startsWhenFreshAccurateLocationIsInsideZone() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 40f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 5_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 20_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.START_TIMER, result.decision)
        assertTrue(result.score >= 65)
    }

    @Test
    fun asksWhenUserIsNearBoundaryWithMediumConfidence() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 145f,
            radiusMeters = 150f,
            accuracyMeters = 120f,
            locationAgeMs = 30_000L,
            motion = DwellMotion.WALKING,
            motionAgeMs = 30_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun waitsWhenLocationIsClearlyOutsideZone() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 450f,
            radiusMeters = 150f,
            accuracyMeters = 25f,
            locationAgeMs = 5_000L,
            motion = DwellMotion.WALKING,
            motionAgeMs = 10_000L,
            geofenceEnter = false,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.WAIT, result.decision)
    }

    @Test
    fun asksOnGeofenceEnterWhenNoTriggeringFixIsAvailable() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = null,
            radiusMeters = 150f,
            accuracyMeters = null,
            locationAgeMs = null,
            motion = DwellMotion.UNKNOWN,
            motionAgeMs = null,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun suppressesLikelyGpsJitterExit() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = 175f,
            radiusMeters = 150f,
            accuracyMeters = 80f,
            locationAgeMs = 8_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 10_000L,
        )

        assertFalse(prompt)
    }

    @Test
    fun promptsExitWhenClearlyOutsideAndMovingFast() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = 360f,
            radiusMeters = 150f,
            accuracyMeters = 30f,
            locationAgeMs = 8_000L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 15_000L,
        )

        assertTrue(prompt)
    }

    @Test
    fun suppressesExitPromptWithoutFreshLocationEvidence() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = null,
            radiusMeters = 150f,
            accuracyMeters = null,
            locationAgeMs = null,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 5_000L,
        )

        assertFalse(prompt)
    }

    @Test
    fun suppressesExitPromptForStaleExitLocation() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = 360f,
            radiusMeters = 150f,
            accuracyMeters = 30f,
            locationAgeMs = 90_000L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 5_000L,
        )

        assertFalse(prompt)
    }

    @Test
    fun suppressesExitPromptForImpreciseExitLocation() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = 360f,
            radiusMeters = 150f,
            accuracyMeters = 220f,
            locationAgeMs = 5_000L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 5_000L,
        )

        assertFalse(prompt)
    }

    @Test
    fun lingeringInsideZoneUpgradesToAutoStart() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 130f,
            radiusMeters = 150f,
            accuracyMeters = 100f,
            locationAgeMs = 20_000L,
            speedMetersPerSecond = 0.4f,
            observedInsideDurationMs = 130_000L,
            motion = DwellMotion.WALKING,
            motionAgeMs = 15_000L,
            geofenceEnter = false,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.START_TIMER, result.decision)
    }

    @Test
    fun approximateLocationAsksInsteadOfAutoStarting() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 45f,
            radiusMeters = 150f,
            accuracyMeters = 180f,
            locationAgeMs = 8_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 180_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 1_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun coarseApproachWithoutGeofenceAsksInsteadOfAutoStarting() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 60f,
            radiusMeters = 150f,
            accuracyMeters = 220f,
            locationAgeMs = 8_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 180_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 1_000L,
            geofenceEnter = false,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun broadApproximateApproachNeverAutoStarts() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 60f,
            radiusMeters = 150f,
            accuracyMeters = 1_500f,
            locationAgeMs = 30_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 180_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 1_000L,
            geofenceEnter = false,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun nearBoundaryOutsideFixAsksInsteadOfAutoStarting() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 220f,
            radiusMeters = 150f,
            accuracyMeters = 90f,
            locationAgeMs = 8_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 180_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 1_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun alreadyInsideCheckDoesNotAutoStartWhenFixIsOutsideRadius() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 210f,
            radiusMeters = 150f,
            accuracyMeters = 80f,
            locationAgeMs = 6_000L,
            speedMetersPerSecond = 0.1f,
            observedInsideDurationMs = 60_000L,
            motion = DwellMotion.UNKNOWN,
            motionAgeMs = null,
            geofenceEnter = false,
            alreadyInsideCheck = true,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun staleLocationAsksInsteadOfAutoStarting() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 45f,
            radiusMeters = 150f,
            accuracyMeters = 25f,
            locationAgeMs = 90_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 180_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 1_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun stillApproachCanAutoStartWithoutGeofenceEvent() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 45f,
            radiusMeters = 150f,
            accuracyMeters = 25f,
            locationAgeMs = 8_000L,
            speedMetersPerSecond = 0.1f,
            observedInsideDurationMs = 30_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 2_000L,
            geofenceEnter = false,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.START_TIMER, result.decision)
    }

    @Test
    fun alreadyInsideCheckCanStartWithFreshPreciseEvidence() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 35f,
            radiusMeters = 150f,
            accuracyMeters = 25f,
            locationAgeMs = 6_000L,
            speedMetersPerSecond = 0.1f,
            observedInsideDurationMs = 0L,
            motion = DwellMotion.UNKNOWN,
            motionAgeMs = null,
            geofenceEnter = false,
            alreadyInsideCheck = true,
        )

        assertEquals(ArrivalDecision.START_TIMER, result.decision)
    }

    @Test
    fun geofenceEnterWithNoMovementEvidenceAsksInsteadOfAutoStarting() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 35f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 6_000L,
            speedMetersPerSecond = null,
            observedInsideDurationMs = 0L,
            motion = DwellMotion.UNKNOWN,
            motionAgeMs = null,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun walkingMotionWithoutSpeedOrDwellTimeAsksInsteadOfAutoStarting() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 35f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 6_000L,
            speedMetersPerSecond = null,
            observedInsideDurationMs = 0L,
            motion = DwellMotion.WALKING,
            motionAgeMs = 2_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun geofenceEnterWithObservedDwellTimeCanAutoStartWithoutMotion() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 35f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 6_000L,
            speedMetersPerSecond = null,
            observedInsideDurationMs = 30_000L,
            motion = DwellMotion.UNKNOWN,
            motionAgeMs = null,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.START_TIMER, result.decision)
    }

    @Test
    fun fastPassThroughWaitsQuietly() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 40f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 5_000L,
            speedMetersPerSecond = 18f,
            observedInsideDurationMs = 0L,
            motion = DwellMotion.UNKNOWN,
            motionAgeMs = null,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.WAIT, result.decision)
    }

    @Test
    fun vehicleMotionWithoutSpeedWaitsUntilSettled() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 35f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 6_000L,
            speedMetersPerSecond = null,
            observedInsideDurationMs = 0L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 2_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.WAIT, result.decision)
    }

    @Test
    fun vehiclePassThroughWaitsQuietly() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 40f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 5_000L,
            speedMetersPerSecond = 12f,
            observedInsideDurationMs = 0L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 2_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.WAIT, result.decision)
    }

    @Test
    fun settledVehicleMotionCanAskAfterLingeringInside() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 40f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 5_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 75_000L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 2_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.ASK_TO_START, result.decision)
    }

    @Test
    fun geofenceEnterWithoutFixWaitsWhenFreshMotionIsTransit() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = null,
            radiusMeters = 150f,
            accuracyMeters = null,
            locationAgeMs = null,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = 2_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.WAIT, result.decision)
    }

    @Test
    fun staleMotionDoesNotBlockAccurateInsideStart() {
        val result = DwellConfidence.evaluateArrival(
            distanceMeters = 40f,
            radiusMeters = 150f,
            accuracyMeters = 20f,
            locationAgeMs = 5_000L,
            speedMetersPerSecond = 0.2f,
            observedInsideDurationMs = 30_000L,
            motion = DwellMotion.IN_VEHICLE,
            motionAgeMs = DwellConfidence.MOTION_FRESH_MS + 1_000L,
            geofenceEnter = true,
            alreadyInsideCheck = false,
        )

        assertEquals(ArrivalDecision.START_TIMER, result.decision)
    }

    @Test
    fun preciseExitJustOutsideZoneCanBeSuppressedWhenStillNearBoundary() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = 215f,
            radiusMeters = 150f,
            accuracyMeters = 80f,
            locationAgeMs = 5_000L,
            motion = DwellMotion.STILL,
            motionAgeMs = 5_000L,
        )

        assertFalse(prompt)
    }

    @Test
    fun preciseExitPromptsWhenOutsideAccuracyTolerance() {
        val prompt = DwellConfidence.shouldPromptExit(
            distanceMeters = 310f,
            radiusMeters = 150f,
            accuracyMeters = 45f,
            locationAgeMs = 5_000L,
            motion = DwellMotion.WALKING,
            motionAgeMs = 5_000L,
        )

        assertTrue(prompt)
    }

    @Test
    fun schedulesFollowUpOnlyForWeakNearEvidence() {
        assertTrue(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 210f,
                radiusMeters = 150f,
                accuracyMeters = 80f,
            )
        )
        assertFalse(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 420f,
                radiusMeters = 150f,
                accuracyMeters = 40f,
            )
        )
        assertFalse(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.ASK_TO_START,
                distanceMeters = 120f,
                radiusMeters = 150f,
                accuracyMeters = 80f,
            )
        )
    }

    @Test
    fun followUpSkipsFastPassThroughEvidence() {
        assertFalse(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 40f,
                radiusMeters = 150f,
                accuracyMeters = 30f,
                speedMetersPerSecond = 12f,
            )
        )
    }

    @Test
    fun followUpSkipsFreshTransitUntilUserSettles() {
        assertFalse(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 40f,
                radiusMeters = 150f,
                accuracyMeters = 30f,
                motion = DwellMotion.IN_VEHICLE,
                motionAgeMs = 2_000L,
                observedInsideDurationMs = 0L,
            )
        )
        assertFalse(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 40f,
                radiusMeters = 150f,
                accuracyMeters = 30f,
                motion = DwellMotion.ON_BICYCLE,
                motionAgeMs = 2_000L,
                observedInsideDurationMs = 30_000L,
            )
        )
    }

    @Test
    fun followUpCanRunForStaleMotionOrSettledTransitEvidence() {
        assertTrue(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 40f,
                radiusMeters = 150f,
                accuracyMeters = 30f,
                motion = DwellMotion.IN_VEHICLE,
                motionAgeMs = DwellConfidence.MOTION_FRESH_MS + 1_000L,
                observedInsideDurationMs = 0L,
            )
        )
        assertTrue(
            DwellConfidence.shouldScheduleFollowUp(
                decision = ArrivalDecision.WAIT,
                distanceMeters = 40f,
                radiusMeters = 150f,
                accuracyMeters = 30f,
                motion = DwellMotion.IN_VEHICLE,
                motionAgeMs = 2_000L,
                observedInsideDurationMs = 75_000L,
            )
        )
    }
}
