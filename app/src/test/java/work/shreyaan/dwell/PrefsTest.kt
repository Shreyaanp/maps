package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTest {
    @Test
    fun approachProbeKeysAreGlobalWhenPlaceIsUnknown() {
        assertEquals(
            "approach_last_probe",
            Prefs.scopedApproachKey("approach_last_probe", null),
        )
        assertEquals(
            "approach_last_probe_motion",
            Prefs.scopedApproachKey("approach_last_probe_motion", ""),
        )
    }

    @Test
    fun approachProbeKeysAreScopedWhenPlaceIsKnown() {
        assertEquals(
            "approach_last_probe_office",
            Prefs.scopedApproachKey("approach_last_probe", "office"),
        )
        assertEquals(
            "approach_last_probe_motion_gym",
            Prefs.scopedApproachKey("approach_last_probe_motion", "gym"),
        )
    }

    @Test
    fun arrivalRuntimeKeysIncludeGlobalAndScopedPlaceState() {
        val keys = Prefs.arrivalRuntimeKeysForPlaces(listOf("office", "gym", "office", ""))

        assertTrue(keys.contains("arrival_inside_since"))
        assertTrue(keys.contains("arrival_last_observed"))
        assertTrue(keys.contains("arrival_follow_up_count"))
        assertTrue(keys.contains("arrival_follow_up_scheduled"))
        assertTrue(keys.contains("approach_last_probe"))
        assertTrue(keys.contains("approach_last_probe_motion"))
        assertTrue(keys.contains("exit_keep_until"))
        assertTrue(keys.contains("arrival_inside_since_office"))
        assertTrue(keys.contains("arrival_last_observed_gym"))
        assertTrue(keys.contains("arrival_follow_up_count_office"))
        assertTrue(keys.contains("arrival_follow_up_scheduled_gym"))
        assertTrue(keys.contains("approach_last_probe_office"))
        assertTrue(keys.contains("approach_last_probe_motion_gym"))
        assertTrue(keys.contains("exit_keep_until_office"))
        assertFalse(keys.contains("arrival_inside_since_"))
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun exitPromptSuppressionOnlyRunsUntilStoredDeadline() {
        assertTrue(
            Prefs.shouldSuppressExitPrompt(
                suppressedUntilMillis = 10_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldSuppressExitPrompt(
                suppressedUntilMillis = 10_000L,
                now = 10_000L,
            )
        )
        assertFalse(
            Prefs.shouldSuppressExitPrompt(
                suppressedUntilMillis = 0L,
                now = 1L,
            )
        )
    }

    @Test
    fun pendingMonitoringResumeExpiresAfterSettingsRoundTripTtl() {
        assertTrue(
            Prefs.pendingMonitoringResumeIsActive(
                requestedAtMillis = 1_000L,
                nowMillis = 1_000L + Prefs.PENDING_MONITORING_RESUME_TTL_MS,
            )
        )
        assertFalse(
            Prefs.pendingMonitoringResumeIsActive(
                requestedAtMillis = 1_000L,
                nowMillis = 1_001L + Prefs.PENDING_MONITORING_RESUME_TTL_MS,
            )
        )
        assertFalse(
            Prefs.pendingMonitoringResumeIsActive(
                requestedAtMillis = 0L,
                nowMillis = 1_000L,
            )
        )
        assertFalse(
            Prefs.pendingMonitoringResumeIsActive(
                requestedAtMillis = 2_000L,
                nowMillis = 1_000L,
            )
        )
    }

    @Test
    fun pendingManualTimerStartExpiresAfterNotificationPermissionTtl() {
        assertTrue(
            Prefs.pendingManualTimerStartIsActive(
                requestedAtMillis = 1_000L,
                nowMillis = 1_000L + MANUAL_TIMER_START_RESUME_TTL_MS,
            )
        )
        assertFalse(
            Prefs.pendingManualTimerStartIsActive(
                requestedAtMillis = 1_000L,
                nowMillis = 1_001L + MANUAL_TIMER_START_RESUME_TTL_MS,
            )
        )
        assertFalse(
            Prefs.pendingManualTimerStartIsActive(
                requestedAtMillis = 0L,
                nowMillis = 1_000L,
            )
        )
        assertFalse(
            Prefs.pendingManualTimerStartIsActive(
                requestedAtMillis = 2_000L,
                nowMillis = 1_000L,
            )
        )
    }

    @Test
    fun pendingCurrentLocationResumeExpiresAfterPermissionRoundTripTtl() {
        assertTrue(
            Prefs.pendingCurrentLocationResumeIsActive(
                requestedAtMillis = 1_000L,
                nowMillis = 1_000L + Prefs.PENDING_CURRENT_LOCATION_RESUME_TTL_MS,
            )
        )
        assertFalse(
            Prefs.pendingCurrentLocationResumeIsActive(
                requestedAtMillis = 1_000L,
                nowMillis = 1_001L + Prefs.PENDING_CURRENT_LOCATION_RESUME_TTL_MS,
            )
        )
        assertFalse(
            Prefs.pendingCurrentLocationResumeIsActive(
                requestedAtMillis = 0L,
                nowMillis = 1_000L,
            )
        )
        assertFalse(
            Prefs.pendingCurrentLocationResumeIsActive(
                requestedAtMillis = 2_000L,
                nowMillis = 1_000L,
            )
        )
    }

    @Test
    fun switchPromptSuppressionRequiresSameTargetTimerAndDeadline() {
        assertTrue(
            Prefs.shouldSuppressSwitchPrompt(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetPlaceId = "gym",
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldSuppressSwitchPrompt(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetPlaceId = "library",
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldSuppressSwitchPrompt(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetPlaceId = "gym",
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 2_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldSuppressSwitchPrompt(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetPlaceId = "gym",
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                now = 10_000L,
            )
        )
    }

    @Test
    fun switchPromptSuppressionCanBeExtendedOnlyForSameLiveTimer() {
        assertTrue(
            Prefs.shouldExtendSwitchPromptSuppression(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetUntilMillis = 20_000L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldExtendSwitchPromptSuppression(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetUntilMillis = 20_000L,
                currentTimerPlaceId = "home",
                currentTimerStartedAt = 1_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldExtendSwitchPromptSuppression(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetUntilMillis = 20_000L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 2_000L,
                now = 9_999L,
            )
        )
        assertFalse(
            Prefs.shouldExtendSwitchPromptSuppression(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                suppressedTimerStartedAt = 1_000L,
                suppressedUntilMillis = 10_000L,
                targetUntilMillis = 20_000L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                now = 10_000L,
            )
        )
    }

    @Test
    fun approachProbeCooldownBlocksRecentProbeWithoutBypass() {
        assertFalse(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 1_000L,
                now = 30_000L,
                cooldownMs = 90_000L,
                bypassCooldown = false,
            )
        )
    }

    @Test
    fun approachProbeCooldownAllowsExpiredMissingOrBypassedProbe() {
        assertTrue(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 0L,
                now = 30_000L,
                cooldownMs = 90_000L,
                bypassCooldown = false,
            )
        )
        assertTrue(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 1_000L,
                now = 100_000L,
                cooldownMs = 90_000L,
                bypassCooldown = false,
            )
        )
        assertTrue(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 1_000L,
                now = 30_000L,
                cooldownMs = 90_000L,
                bypassCooldown = true,
            )
        )
    }

    @Test
    fun createPlaceSelectsNearbyDuplicateWithoutMutatingSavedSettings() {
        val saved = placeForCreateTest(
            id = "office",
            label = "Office",
            latitude = 17.0000,
            longitude = 78.0000,
            radiusMeters = 120f,
            durationMinutes = 45,
            monitoringEnabled = true,
            autoStart = false,
        )
        val duplicateCandidate = placeForCreateTest(
            id = "candidate",
            label = " office ",
            latitude = 17.0001,
            longitude = 78.0000,
            radiusMeters = 250f,
            durationMinutes = 120,
            monitoringEnabled = false,
            autoStart = true,
        )

        val selected = Prefs.placeForCreate(listOf(saved), duplicateCandidate)

        assertEquals("office", selected.id)
        assertEquals(17.0000, selected.latitude, 0.00001)
        assertEquals(120f, selected.radiusMeters, 0f)
        assertEquals(45, selected.durationMinutes)
        assertTrue(selected.monitoringEnabled)
        assertFalse(selected.autoStart)
    }

    @Test
    fun createPlaceDuplicateDoesNotEnableMonitoringWithoutGeofencePreflight() {
        val saved = placeForCreateTest(
            id = "office",
            label = "Office",
            latitude = 17.0000,
            longitude = 78.0000,
            monitoringEnabled = false,
        )
        val duplicateCandidate = placeForCreateTest(
            id = "candidate",
            label = "Office",
            latitude = 17.0001,
            longitude = 78.0000,
            monitoringEnabled = true,
        )

        val selected = Prefs.placeForCreate(listOf(saved), duplicateCandidate)

        assertEquals("office", selected.id)
        assertFalse(selected.monitoringEnabled)
    }

    @Test
    fun createPlaceAllowsSameLabelWhenFarAway() {
        val saved = placeForCreateTest(
            id = "office",
            label = "Office",
            latitude = 17.0000,
            longitude = 78.0000,
        )
        val farCandidate = placeForCreateTest(
            id = "other-office",
            label = "Office",
            latitude = 17.0100,
            longitude = 78.0000,
        )

        val selected = Prefs.placeForCreate(listOf(saved), farCandidate)

        assertEquals("other-office", selected.id)
    }

    @Test
    fun monitoringLimitNormalizationNamesPausedExtraMonitoredPlaces() {
        val requested = (0 until DwellPlace.MAX_MONITORED_PLACES + 3).map { index ->
            placeForCreateTest(
                id = "place-$index",
                label = "Place $index",
                latitude = 17.0 + index,
                longitude = 78.0,
                monitoringEnabled = true,
            )
        }
        val normalized = DwellPlace.normalizePlaces(requested)

        assertEquals(
            3,
            Prefs.monitoringLimitNormalizationPauseCount(
                requestedPlaces = requested,
                savedPlaces = normalized,
            ),
        )
        assertEquals(
            "Dwell paused 3 extra monitored places because the monitoring limit is ${DwellPlace.MAX_MONITORED_PLACES}. Pause other monitored places before turning them back on.",
            Prefs.monitoringLimitNormalizationMessage(3),
        )
        assertTrue(
            Prefs.isMonitoringLimitNormalizationMessage(
                Prefs.monitoringLimitNormalizationMessage(1),
            ),
        )
        assertTrue(
            Prefs.isMonitoringLimitNormalizationMessage(
                "Dwell paused 1 extra monitored place because the live monitoring limit is ${DwellPlace.MAX_MONITORED_PLACES}. Pause another monitored place before turning it back on.",
            ),
        )
    }

    @Test
    fun createPlaceSelectsNearExactDuplicateEvenWhenLabelDiffers() {
        val saved = placeForCreateTest(
            id = "office",
            label = "Office",
            latitude = 17.0000,
            longitude = 78.0000,
        )
        val samePointCandidate = placeForCreateTest(
            id = "candidate",
            label = "Work",
            latitude = 17.00002,
            longitude = 78.0000,
        )

        val selected = Prefs.placeForCreate(listOf(saved), samePointCandidate)

        assertEquals("office", selected.id)
    }

    @Test
    fun duplicateCollapseRemapsReferencesToSurvivingPlace() {
        assertEquals(
            "office",
            Prefs.remapPlaceIdReference(
                placeId = "work",
                removedPlaceIds = setOf("work"),
                survivingPlaceId = "office",
            ),
        )
        assertEquals(
            "gym",
            Prefs.remapPlaceIdReference(
                placeId = "gym",
                removedPlaceIds = setOf("work"),
                survivingPlaceId = "office",
            ),
        )
        assertEquals(
            setOf("office", "gym"),
            Prefs.remapPlaceIdSet(
                placeIds = setOf("work", "office", "gym"),
                removedPlaceIds = setOf("work"),
                survivingPlaceId = "office",
            ),
        )
    }

    @Test
    fun duplicateCollapseBuildsPerSurvivorReferenceMap() {
        val office = placeForCreateTest("office", "Office", 17.0000, 78.0000)
        val work = placeForCreateTest("work", " office ", 17.0001, 78.0000)
        val home = placeForCreateTest("home", "Home", 18.0000, 79.0000)
        val house = placeForCreateTest("house", "home", 18.0001, 79.0000)
        val gym = placeForCreateTest("gym", "Gym", 17.0001, 78.0000)
        val requested = listOf(office, work, home, house, gym)
        val normalized = DwellPlace.normalizePlaces(requested)

        val remaps = Prefs.placeIdRemapsForNormalizedPlaces(requested, normalized)

        assertEquals(mapOf("work" to "office", "house" to "home"), remaps)
        assertEquals("office", Prefs.remapPlaceIdReference("work", remaps))
        assertEquals("home", Prefs.remapPlaceIdReference("house", remaps))
        assertEquals("gym", Prefs.remapPlaceIdReference("gym", remaps))
        assertEquals(
            setOf("office", "home", "gym"),
            Prefs.remapPlaceIdSet(setOf("work", "house", "gym"), remaps),
        )
        assertEquals(
            "gym" to "office",
            Prefs.remapSwitchPromptSuppressionPlaceIds(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "work",
                placeIdRemaps = remaps,
            ),
        )
    }

    @Test
    fun duplicateCollapsePreservesRuntimeMeaningWhenMergingReferences() {
        assertEquals(
            100L,
            Prefs.mergedArrivalRuntimeLong(
                base = "arrival_inside_since",
                survivingValue = 150L,
                removedValue = 100L,
            ),
        )
        assertEquals(
            300L,
            Prefs.mergedArrivalRuntimeLong(
                base = "arrival_last_observed",
                survivingValue = 200L,
                removedValue = 300L,
            ),
        )
        assertEquals(
            2,
            Prefs.mergedArrivalRuntimeInt(
                base = "arrival_follow_up_count",
                survivingValue = 1,
                removedValue = 2,
            ),
        )
        assertTrue(
            Prefs.shouldUseMergedApproachMotion(
                survivingMotionExists = false,
                removedProbeMillis = 50L,
                survivingProbeMillis = 100L,
            )
        )
        assertTrue(
            Prefs.shouldUseMergedApproachMotion(
                survivingMotionExists = true,
                removedProbeMillis = 200L,
                survivingProbeMillis = 100L,
            )
        )
        assertFalse(
            Prefs.shouldUseMergedApproachMotion(
                survivingMotionExists = true,
                removedProbeMillis = 50L,
                survivingProbeMillis = 100L,
            )
        )
    }

    @Test
    fun promptPlaceMustExistWhenPromptNamesAPlace() {
        assertTrue(Prefs.promptPlaceStillExists(placeId = null, placeExists = false))
        assertTrue(Prefs.promptPlaceStillExists(placeId = "", placeExists = false))
        assertTrue(Prefs.promptPlaceStillExists(placeId = "office", placeExists = true))
        assertFalse(Prefs.promptPlaceStillExists(placeId = "office", placeExists = false))
    }

    @Test
    fun deletedPlaceClearsOnlyPromptsThatNameThatPlace() {
        assertTrue(
            Prefs.shouldClearPromptForDeletedPlace(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                timerPlaceId = "",
                deletedPlaceId = "office",
            )
        )
        assertFalse(
            Prefs.shouldClearPromptForDeletedPlace(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "",
                deletedPlaceId = "office",
            )
        )
        assertFalse(
            Prefs.shouldClearPromptForDeletedPlace(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "",
                timerPlaceId = "",
                deletedPlaceId = "office",
            )
        )
    }

    @Test
    fun deletedTimerPlaceClearsTimeUpPromptEvenWithoutPromptPlace() {
        assertTrue(
            Prefs.shouldClearPromptForDeletedPlace(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptPlaceId = "",
                timerPlaceId = "office",
                deletedPlaceId = "office",
            )
        )
        assertFalse(
            Prefs.shouldClearPromptForDeletedPlace(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptPlaceId = "",
                timerPlaceId = "gym",
                deletedPlaceId = "office",
            )
        )
        assertFalse(
            Prefs.shouldClearPromptForDeletedPlace(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "",
                timerPlaceId = "office",
                deletedPlaceId = "office",
            )
        )
    }

    @Test
    fun deletedPlaceClearsSwitchSuppressionWhenItNamesDeletedPlace() {
        assertTrue(
            Prefs.shouldClearSwitchSuppressionForDeletedPlace(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                deletedPlaceId = "gym",
            )
        )
        assertTrue(
            Prefs.shouldClearSwitchSuppressionForDeletedPlace(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                deletedPlaceId = "office",
            )
        )
        assertFalse(
            Prefs.shouldClearSwitchSuppressionForDeletedPlace(
                suppressedTargetPlaceId = "gym",
                suppressedTimerPlaceId = "office",
                deletedPlaceId = "home",
            )
        )
    }

    @Test
    fun deletedPlaceRuntimeCleanupIsScopedToDeletedPlace() {
        val keys = Prefs.runtimeKeysForDeletedPlace("office")

        assertTrue(keys.contains("arrival_inside_since_office"))
        assertTrue(keys.contains("arrival_last_observed_office"))
        assertTrue(keys.contains("arrival_follow_up_count_office"))
        assertTrue(keys.contains("arrival_follow_up_scheduled_office"))
        assertTrue(keys.contains("approach_last_probe_office"))
        assertTrue(keys.contains("approach_last_probe_motion_office"))
        assertTrue(keys.contains("exit_keep_until_office"))
        assertFalse(keys.contains("arrival_inside_since"))
        assertFalse(keys.contains("exit_keep_until_gym"))
        assertTrue(Prefs.runtimeKeysForDeletedPlace("").isEmpty())
    }

    @Test
    fun updatingPlaceFromMapPreviewPersistsReviewedTimerSettings() {
        val saved = placeForCreateTest(
            id = "home",
            label = "Home",
            latitude = 17.0000,
            longitude = 78.0000,
            radiusMeters = 120f,
            durationMinutes = 45,
            monitoringEnabled = true,
            autoStart = false,
        )

        val updated = Prefs.placeForUpdate(
            active = saved,
            lat = 17.0100,
            lon = 78.0200,
            label = "Home gate",
            radiusMeters = 240f,
            durationMinutes = 180,
            autoStart = true,
            now = 5L,
        )

        assertEquals("home", updated.id)
        assertEquals("Home gate", updated.safeLabel)
        assertEquals(17.0100, updated.latitude, 0.00001)
        assertEquals(78.0200, updated.longitude, 0.00001)
        assertEquals(240f, updated.radiusMeters, 0f)
        assertEquals(180, updated.durationMinutes)
        assertTrue(updated.monitoringEnabled)
        assertTrue(updated.autoStart)
        assertEquals(1L, updated.createdAtMillis)
        assertEquals(5L, updated.updatedAtMillis)
    }

    @Test
    fun editingOnePlaceBuildsAnUpdateForOnlyThatPlace() {
        val home = placeForCreateTest(
            id = "home",
            label = "Home",
            latitude = 17.0000,
            longitude = 78.0000,
            radiusMeters = 120f,
            durationMinutes = 45,
            monitoringEnabled = true,
            autoStart = true,
        )
        val office = placeForCreateTest(
            id = "office",
            label = "Office",
            latitude = 17.0100,
            longitude = 78.0000,
            radiusMeters = 130f,
            durationMinutes = 90,
            monitoringEnabled = true,
            autoStart = false,
        )
        val gym = placeForCreateTest(
            id = "gym",
            label = "Gym",
            latitude = 17.0140,
            longitude = 78.0000,
            radiusMeters = 140f,
            durationMinutes = 60,
            monitoringEnabled = true,
            autoStart = true,
        )

        val editedOffice = Prefs.placeForUpdate(
            active = office,
            lat = office.latitude + 0.001,
            lon = office.longitude + 0.001,
            label = "Office gate",
            radiusMeters = 55f,
            durationMinutes = 135,
            autoStart = true,
            now = 99L,
        )
        val updatedPlaces = listOf(home, editedOffice, gym)

        assertEquals(home, updatedPlaces.single { it.id == home.id })
        assertEquals(gym, updatedPlaces.single { it.id == gym.id })
        assertEquals("office", editedOffice.id)
        assertEquals("Office gate", editedOffice.safeLabel)
        assertEquals(55f, editedOffice.radiusMeters, 0f)
        assertEquals(135, editedOffice.durationMinutes)
        assertTrue(editedOffice.autoStart)
        assertEquals(office.createdAtMillis, editedOffice.createdAtMillis)
        assertEquals(99L, editedOffice.updatedAtMillis)
    }

    private fun placeForCreateTest(
        id: String,
        label: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 150f,
        durationMinutes: Int = 270,
        monitoringEnabled: Boolean = false,
        autoStart: Boolean = true,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = label,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            monitoringEnabled = monitoringEnabled,
            autoStart = autoStart,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
