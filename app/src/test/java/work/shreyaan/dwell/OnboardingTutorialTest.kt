package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTutorialTest {
    @Test
    fun onboardingWaitsForAllMajorPermissions() {
        assertTrue(
            OnboardingPermissionStatus(
                locationGranted = true,
                backgroundGranted = true,
                notificationsGranted = true,
                motionGranted = true,
            ).allMajorGranted
        )
        assertFalse(
            OnboardingPermissionStatus(
                locationGranted = true,
                backgroundGranted = false,
                notificationsGranted = true,
                motionGranted = true,
            ).allMajorGranted
        )
        assertFalse(
            OnboardingPermissionStatus(
                locationGranted = false,
                backgroundGranted = true,
                notificationsGranted = true,
                motionGranted = true,
            ).allMajorGranted
        )
    }

    @Test
    fun onboardingPermissionHelpNamesTheNextRequiredPermission() {
        assertEquals(
            "Allow location so Dwell can find your current place and monitor arrivals.",
            onboardingPermissionHelp(
                OnboardingPermissionStatus(
                    locationGranted = false,
                    backgroundGranted = false,
                    notificationsGranted = false,
                    motionGranted = false,
                )
            ),
        )
        assertEquals(
            "Allow notifications so Dwell can ask before starting and alert when time is up.",
            onboardingPermissionHelp(
                OnboardingPermissionStatus(
                    locationGranted = true,
                    backgroundGranted = false,
                    notificationsGranted = false,
                    motionGranted = false,
                )
            ),
        )
        assertEquals(
            "Allow physical activity so Dwell can avoid starting timers during pass-through movement.",
            onboardingPermissionHelp(
                OnboardingPermissionStatus(
                    locationGranted = true,
                    backgroundGranted = false,
                    notificationsGranted = true,
                    motionGranted = false,
                )
            ),
        )
        assertEquals(
            "Allow all-the-time location so arrivals work after the app is closed.",
            onboardingPermissionHelp(
                OnboardingPermissionStatus(
                    locationGranted = true,
                    backgroundGranted = false,
                    notificationsGranted = true,
                    motionGranted = true,
                )
            ),
        )
        assertEquals(
            "Core permissions are ready. You can create and monitor your first place.",
            onboardingPermissionHelp(
                OnboardingPermissionStatus(
                    locationGranted = true,
                    backgroundGranted = true,
                    notificationsGranted = true,
                    motionGranted = true,
                )
            ),
        )
    }

    @Test
    fun monitoringPermissionRequestWaitsWhenPermissionUiIsAlreadyActive() {
        assertFalse(
            monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = false,
                backgroundDisclosureVisible = false,
            )
        )
        assertTrue(
            monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = true,
                backgroundDisclosureVisible = false,
            )
        )
        assertTrue(
            monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = false,
                backgroundDisclosureVisible = true,
            )
        )
        assertEquals(
            "Finish the open Android permission prompt first.",
            monitoringPermissionUiAlreadyActiveMessage(backgroundDisclosureVisible = false),
        )
        assertEquals(
            "Finish the open background location setup first.",
            monitoringPermissionUiAlreadyActiveMessage(backgroundDisclosureVisible = true),
        )
    }

    @Test
    fun monitoringSetupRecoveryOnlyRepeatsWhenTheMissingStepAdvances() {
        assertEquals(
            MonitoringSetupRecoveryStep.ForegroundPermissions,
            monitoringSetupRecoveryStep(
                foregroundPermissionsMissing = true,
                backgroundLocationMissing = true,
            ),
        )
        assertEquals(
            MonitoringSetupRecoveryStep.BackgroundLocation,
            monitoringSetupRecoveryStep(
                foregroundPermissionsMissing = false,
                backgroundLocationMissing = true,
            ),
        )
        assertEquals(
            null,
            monitoringSetupRecoveryStep(
                foregroundPermissionsMissing = false,
                backgroundLocationMissing = false,
            ),
        )
        assertTrue(
            shouldShowMonitoringSetupRecovery(
                nextStep = MonitoringSetupRecoveryStep.ForegroundPermissions,
                alreadyShownStep = null,
            )
        )
        assertFalse(
            shouldShowMonitoringSetupRecovery(
                nextStep = MonitoringSetupRecoveryStep.ForegroundPermissions,
                alreadyShownStep = MonitoringSetupRecoveryStep.ForegroundPermissions,
            )
        )
        assertTrue(
            shouldShowMonitoringSetupRecovery(
                nextStep = MonitoringSetupRecoveryStep.BackgroundLocation,
                alreadyShownStep = MonitoringSetupRecoveryStep.ForegroundPermissions,
            )
        )
        assertFalse(
            shouldShowMonitoringSetupRecovery(
                nextStep = null,
                alreadyShownStep = MonitoringSetupRecoveryStep.BackgroundLocation,
            )
        )
    }

    @Test
    fun monitoringPermissionResumeStopsWhenRequestedSavedPlaceIsGone() {
        assertEquals(
            MonitoringResumeTargetAction.ArmCurrentSelection,
            monitoringResumeTargetAction(
                requestedPlaceId = null,
                savedPlaceExists = false,
            ),
        )
        assertEquals(
            MonitoringResumeTargetAction.ArmCurrentSelection,
            monitoringResumeTargetAction(
                requestedPlaceId = "",
                savedPlaceExists = false,
            ),
        )
        assertEquals(
            MonitoringResumeTargetAction.MonitorSavedPlace,
            monitoringResumeTargetAction(
                requestedPlaceId = "office",
                savedPlaceExists = true,
            ),
        )
        assertEquals(
            MonitoringResumeTargetAction.StopMissingPlace,
            monitoringResumeTargetAction(
                requestedPlaceId = "office",
                savedPlaceExists = false,
            ),
        )
        assertEquals(
            "That saved place is no longer available. Pick a place and tap Monitor again.",
            missingMonitoringResumePlaceMessage(),
        )
    }

    @Test
    fun monitoringPermissionResumePlanKeepsRequestedPlaceAheadOfCurrentSelection() {
        assertEquals(
            MonitoringResumeTargetPlan(
                action = MonitoringResumeTargetAction.MonitorSavedPlace,
                placeId = "gym",
            ),
            monitoringResumeTargetPlan(
                requestedPlaceId = "gym",
                savedPlaceExists = true,
                currentSelectionPlaceId = "home",
            ),
        )
        assertEquals(
            MonitoringResumeTargetPlan(
                action = MonitoringResumeTargetAction.StopMissingPlace,
                placeId = null,
            ),
            monitoringResumeTargetPlan(
                requestedPlaceId = "gym",
                savedPlaceExists = false,
                currentSelectionPlaceId = "home",
            ),
        )
        assertEquals(
            MonitoringResumeTargetPlan(
                action = MonitoringResumeTargetAction.ArmCurrentSelection,
                placeId = "home",
            ),
            monitoringResumeTargetPlan(
                requestedPlaceId = "",
                savedPlaceExists = false,
                currentSelectionPlaceId = "home",
            ),
        )
    }

    @Test
    fun persistedMonitoringPermissionResumeStillTargetsOriginalSavedPlaceAfterRecreate() {
        val restored = Prefs.PersistedMonitoringResume(
            placeId = "gym",
            requestedAtMillis = 1_000L,
        )

        assertEquals(
            true,
            Prefs.pendingMonitoringResumeIsActive(
                requestedAtMillis = restored.requestedAtMillis,
                nowMillis = 1_000L + 60_000L,
            ),
        )
        assertEquals(
            MonitoringResumeTargetPlan(
                action = MonitoringResumeTargetAction.MonitorSavedPlace,
                placeId = "gym",
            ),
            monitoringResumeTargetPlan(
                requestedPlaceId = restored.placeId,
                savedPlaceExists = true,
                currentSelectionPlaceId = "home",
            ),
        )
    }

    @Test
    fun monitoringSetupRecoveryMessageNamesOnlyMissingForegroundPermissions() {
        assertEquals(
            "Allow location so Dwell can find your current place and monitor arrivals.",
            monitoringSetupForegroundRecoveryMessage(
                permissionsReady(location = false),
            ),
        )
        assertEquals(
            "Allow notifications so Dwell can ask before starting and alert when time is up.",
            monitoringSetupForegroundRecoveryMessage(
                permissionsReady(notifications = false),
            ),
        )
        assertEquals(
            "Allow physical activity so Dwell can avoid starting timers during pass-through movement.",
            monitoringSetupForegroundRecoveryMessage(
                permissionsReady(motion = false),
            ),
        )
        assertEquals(
            "Allow location, notifications, and physical activity so monitoring can start.",
            monitoringSetupForegroundRecoveryMessage(
                permissionsReady(
                    location = false,
                    notifications = false,
                    motion = false,
                ),
            ),
        )
        assertEquals(
            "Foreground permissions are ready.",
            monitoringSetupForegroundRecoveryMessage(permissionsReady(background = false)),
        )
    }

    @Test
    fun monitoringSetupRecoveryActionNamesThePermissionButton() {
        assertEquals(
            "Allow location",
            monitoringSetupForegroundRecoveryActionLabel(
                permissionsReady(location = false),
            ),
        )
        assertEquals(
            "Allow notifications",
            monitoringSetupForegroundRecoveryActionLabel(
                permissionsReady(notifications = false),
            ),
        )
        assertEquals(
            "Allow physical activity",
            monitoringSetupForegroundRecoveryActionLabel(
                permissionsReady(motion = false),
            ),
        )
        assertEquals(
            "Allow permissions",
            monitoringSetupForegroundRecoveryActionLabel(
                permissionsReady(
                    location = false,
                    notifications = false,
                    motion = false,
                ),
            ),
        )
        assertEquals(
            "",
            monitoringSetupForegroundRecoveryActionLabel(
                permissionsReady(background = false),
            ),
        )
    }

    @Test
    fun onboardingPrimaryActionNamesTheNextStep() {
        assertEquals(
            "Set up",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Intro,
                permissionStatus = permissionsReady(location = false),
            ),
        )
        assertEquals(
            "Allow location",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Permissions,
                permissionStatus = permissionsReady(location = false),
            ),
        )
        assertEquals(
            "Allow notifications",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Permissions,
                permissionStatus = permissionsReady(notifications = false),
            ),
        )
        assertEquals(
            "Allow physical activity",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Permissions,
                permissionStatus = permissionsReady(motion = false),
            ),
        )
        assertEquals(
            "Allow background location",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Permissions,
                permissionStatus = permissionsReady(background = false),
            ),
        )
        assertEquals(
            "Continue",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Permissions,
                permissionStatus = permissionsReady(),
            ),
        )
        assertEquals(
            "Add first place",
            onboardingPrimaryActionLabel(
                page = OnboardingPage.Guide,
                permissionStatus = permissionsReady(),
            ),
        )
    }

    @Test
    fun onboardingPermissionRecoveryButtonNamesTheNextStep() {
        assertEquals(
            "Allow location",
            onboardingPermissionRecoveryButtonLabel(
                permissionStatus = permissionsReady(location = false),
            ),
        )
        assertEquals(
            "Allow notifications",
            onboardingPermissionRecoveryButtonLabel(
                permissionStatus = permissionsReady(notifications = false),
            ),
        )
        assertEquals(
            "Allow physical activity",
            onboardingPermissionRecoveryButtonLabel(
                permissionStatus = permissionsReady(motion = false),
            ),
        )
        assertEquals(
            "Allow background location",
            onboardingPermissionRecoveryButtonLabel(
                permissionStatus = permissionsReady(background = false),
            ),
        )
    }

    @Test
    fun onboardingGuideExplainsHowToCreateSaveAndMonitor() {
        assertEquals(
            listOf(
                "Tap Add place, then pick Home, Office, Gym, or any exact spot.",
                "Use search for an address, current location for where you are, or a long-press for a precise map point.",
                "Review the unsaved name, radius, timer duration, and arrival mode before it changes anything.",
                "Tap Save this place, then Monitor. Repeat for every place you want watched.",
            ),
            onboardingGuideSteps(),
        )
    }

    @Test
    fun appTutorialExplainsCompartmentalizedPlaceFlow() {
        assertEquals(
            listOf(
                TutorialFlowStep(
                    title = "Set up once",
                    detail = "Finish location, notifications, physical activity, alarms, and battery checks once.",
                ),
                TutorialFlowStep(
                    title = "Add a place",
                    detail = "Tap Add place, then use search, current location, or a long-press on the map.",
                ),
                TutorialFlowStep(
                    title = "Save the preview",
                    detail = "Name it, choose radius, timer, and arrival mode, then tap Save this place.",
                ),
                TutorialFlowStep(
                    title = "Monitor that row",
                    detail = "Turn on Monitor for the saved place. Dwell can watch it after the app is closed.",
                ),
                TutorialFlowStep(
                    title = "Add more places",
                    detail = "Repeat for Home, Office, Gym, or more. Each row keeps its own settings.",
                ),
                TutorialFlowStep(
                    title = "Edit safely",
                    detail = "View map is read-only. Edit settings changes only the selected saved row.",
                ),
                TutorialFlowStep(
                    title = "Arrive and respond",
                    detail = "Auto-start begins the timer. Confirm first asks before starting or switching places.",
                ),
                TutorialFlowStep(
                    title = "Use it daily",
                    detail = "Places is the dashboard: monitor, pause, start now, edit one row, or finish setup when needed.",
                ),
                TutorialFlowStep(
                    title = "Use the watch",
                    detail = "Phone notifications, watch app, and Tile should name the same active place.",
                ),
            ),
            appTutorialFlowSteps(),
        )
    }

    @Test
    fun appTutorialExplainsWaysToPickAPlace() {
        assertEquals(
            listOf(
                TutorialFlowStep(
                    title = "Search",
                    detail = "Use for addresses or landmarks. Choosing a result creates an unsaved preview.",
                ),
                TutorialFlowStep(
                    title = "Current location",
                    detail = "In Add or Edit it chooses the phone's spot. In View map it only centers the map.",
                ),
                TutorialFlowStep(
                    title = "Long-press map",
                    detail = "Drops a new unsaved preview at the pressed point. It will not silently move a saved place.",
                ),
            ),
            appTutorialPickPlaceSteps(),
        )
    }

    @Test
    fun appTutorialGivesAConcreteHomeOfficeGymStory() {
        assertEquals(
            listOf(
                TutorialFlowStep(
                    title = "Home",
                    detail = "Use a 50 m radius, a short timer, and Confirm first if nearby places overlap.",
                ),
                TutorialFlowStep(
                    title = "Office",
                    detail = "Use its own radius and work timer. Office changes should not affect Home.",
                ),
                TutorialFlowStep(
                    title = "Gym",
                    detail = "Give it a separate timer and turn on Monitor alongside Home and Office.",
                ),
                TutorialFlowStep(
                    title = "When you arrive",
                    detail = "Dwell uses the place you entered, even if another place is open on the map.",
                ),
            ),
            appTutorialExampleSteps(),
        )
    }

    @Test
    fun appTutorialExplainsMultiplePlaceRules() {
        assertEquals(
            listOf(
                TutorialFlowStep(
                    title = "Monitor several rows",
                    detail = "Home, Office, Gym, and other saved places can all stay registered together.",
                ),
                TutorialFlowStep(
                    title = "Settings stay separate",
                    detail = "Name, radius, timer duration, and arrival mode belong to the selected saved row.",
                ),
                TutorialFlowStep(
                    title = "The entered place wins",
                    detail = "Arrival prompts use the place you entered, even if another place is open on the map.",
                ),
            ),
            appTutorialMultiplePlaceRules(),
        )
    }

    @Test
    fun appTutorialExplainsHowToRecoverFromStuckStates() {
        assertEquals(
            listOf(
                TutorialFlowStep(
                    title = "Unsaved preview",
                    detail = "Save, move, or cancel the preview before Monitor, Start now, Remove, or Pause monitoring can run.",
                ),
                TutorialFlowStep(
                    title = "Search suggestions",
                    detail = "Choose a result, clear text, tap close, tap outside the panel, or press Back.",
                ),
                TutorialFlowStep(
                    title = "Duplicate place",
                    detail = "If the spot already exists, Dwell opens that saved place and keeps its settings.",
                ),
                TutorialFlowStep(
                    title = "Needs setup",
                    detail = "Tap Finish setup when background location, notifications, alarms, or battery block monitoring.",
                ),
                TutorialFlowStep(
                    title = "Monitoring limit",
                    detail = "If the monitoring limit appears, pause another monitored place before monitoring a new row.",
                ),
            ),
            appTutorialStuckStateSteps(),
        )
    }

    @Test
    fun setupChecksPrioritizeTheNextBlockingRecoveryStep() {
        assertEquals(
            "Allow location so Dwell can find your current place and monitor arrivals.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(location = false),
                exactAlarmAllowed = false,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Allow notifications so Dwell can ask before starting and alert when time is up.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(notifications = false),
                exactAlarmAllowed = false,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Allow physical activity so Dwell can avoid starting timers during pass-through movement.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(motion = false),
                exactAlarmAllowed = false,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Allow all-the-time location so arrivals work after the app is closed.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(background = false),
                exactAlarmAllowed = false,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Allow exact alarms so timers can alert on time.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(),
                exactAlarmAllowed = false,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Turn off Battery Saver so background arrivals are not blocked.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(),
                exactAlarmAllowed = true,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = false,
                    isIgnoringOptimizations = false,
                    isPowerSaveMode = true,
                ),
            ),
        )
        assertEquals(
            "Open app info, then Battery, and choose Unrestricted so background arrivals are not delayed.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(),
                exactAlarmAllowed = true,
                batteryReliabilityStatus = batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Setup checks are ready for background monitoring.",
            setupChecksIntroDetail(
                permissionStatus = permissionsReady(),
                exactAlarmAllowed = true,
                batteryReliabilityStatus = batteryStatus(),
            ),
        )
    }

    @Test
    fun setupChecksPermissionActionNamesTheNextPermissionStep() {
        assertEquals(
            "Allow location",
            setupChecksPermissionActionLabel(permissionsReady(location = false)),
        )
        assertEquals(
            "Allow notifications",
            setupChecksPermissionActionLabel(permissionsReady(notifications = false)),
        )
        assertEquals(
            "Allow physical activity",
            setupChecksPermissionActionLabel(permissionsReady(motion = false)),
        )
        assertEquals(
            "Allow background location",
            setupChecksPermissionActionLabel(permissionsReady(background = false)),
        )
        assertEquals(
            null,
            setupChecksPermissionActionLabel(permissionsReady()),
        )
    }

    @Test
    fun setupChecksPermissionButtonsFollowRecoveryPriority() {
        assertEquals(
            listOf(
                SetupCheckPermissionButton(
                    action = SetupCheckPermissionAction.Permissions,
                    label = "Allow location",
                ),
                SetupCheckPermissionButton(
                    action = SetupCheckPermissionAction.ExactAlarm,
                    label = "Allow exact alarms",
                ),
            ),
            setupChecksPermissionButtons(
                permissionStatus = permissionsReady(location = false),
                exactAlarmAllowed = false,
            ),
        )
        assertEquals(
            listOf(
                SetupCheckPermissionButton(
                    action = SetupCheckPermissionAction.Permissions,
                    label = "Allow background location",
                ),
                SetupCheckPermissionButton(
                    action = SetupCheckPermissionAction.ExactAlarm,
                    label = "Allow exact alarms",
                ),
            ),
            setupChecksPermissionButtons(
                permissionStatus = permissionsReady(background = false),
                exactAlarmAllowed = false,
            ),
        )
        assertEquals(
            listOf(
                SetupCheckPermissionButton(
                    action = SetupCheckPermissionAction.ExactAlarm,
                    label = "Allow exact alarms",
                ),
            ),
            setupChecksPermissionButtons(
                permissionStatus = permissionsReady(),
                exactAlarmAllowed = false,
            ),
        )
        assertEquals(
            emptyList<SetupCheckPermissionButton>(),
            setupChecksPermissionButtons(
                permissionStatus = permissionsReady(),
                exactAlarmAllowed = true,
            ),
        )
    }

    @Test
    fun setupChecksBatteryActionOnlyAppearsWhenOptimizationCanBlockBackgroundWork() {
        assertEquals(
            false,
            batteryNeedsReliabilityReview(
                batteryStatus(
                    isKnownAggressiveOem = false,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            true,
            batteryNeedsReliabilityReview(
                batteryStatus(
                    isKnownAggressiveOem = false,
                    isIgnoringOptimizations = false,
                    isPowerSaveMode = true,
                ),
            ),
        )
        assertEquals(
            false,
            batteryNeedsReliabilityReview(
                batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = true,
                ),
            ),
        )
        assertEquals(
            "Open Battery Saver",
            setupChecksBatteryActionLabel(
                batteryStatus(
                    isKnownAggressiveOem = false,
                    isIgnoringOptimizations = false,
                    isPowerSaveMode = true,
                ),
            ),
        )
        assertEquals(
            "Open app info",
            setupChecksBatteryActionLabel(
                batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            null,
            setupChecksBatteryActionLabel(
                batteryStatus(
                    isKnownAggressiveOem = false,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            null,
            setupChecksBatteryActionLabel(
                batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = true,
                ),
            ),
        )
    }

    @Test
    fun setupPermissionRowsNameTheMissingRecoveryStep() {
        assertEquals("Allowed", permissionRowDetail("Location", granted = true))
        assertEquals("Allow location", permissionRowDetail("Location", granted = false))
        assertEquals(
            "Allow all-the-time location",
            permissionRowDetail("Background location", granted = false),
        )
        assertEquals("Allow notifications", permissionRowDetail("Notifications", granted = false))
        assertEquals(
            "Allow physical activity",
            permissionRowDetail("Physical activity", granted = false),
        )
        assertEquals("Allow exact alarms", permissionRowDetail("Exact alarms", granted = false))
        assertEquals("Not allowed yet", permissionRowDetail("Unknown", granted = false))
    }

    @Test
    fun onboardingPermissionRowsUseActionBadgesForMissingSteps() {
        assertEquals("Ready", onboardingPermissionRowStatus("Location", granted = true))
        assertEquals(
            "Allow location",
            onboardingPermissionRowStatus("Location", granted = false),
        )
        assertEquals(
            "Allow all-the-time location",
            onboardingPermissionRowStatus("Background location", granted = false),
        )
        assertEquals(
            "Allow notifications",
            onboardingPermissionRowStatus("Notifications", granted = false),
        )
        assertEquals(
            "Allow physical activity",
            onboardingPermissionRowStatus("Physical activity", granted = false),
        )
    }

    @Test
    fun onboardingCompletionStartsNewUsersInSearchReadyAddPlaceFlow() {
        assertEquals(
            OnboardingCompletionAction(
                route = AppRoute.Home,
                beginCreatePlace = true,
                openSearchPanel = true,
                focusSearch = true,
                toastMessage = "Search, use current location, or long-press the map to add a place",
            ),
            onboardingCompletionAction(hasSavedPlace = false),
        )
    }

    @Test
    fun onboardingCompletionTakesExistingUsersToPlacesOverview() {
        assertEquals(
            OnboardingCompletionAction(
                route = AppRoute.SavedZones,
                beginCreatePlace = false,
                openSearchPanel = false,
                focusSearch = false,
                toastMessage = null,
            ),
            onboardingCompletionAction(hasSavedPlace = true),
        )
    }

    private fun permissionsReady(
        location: Boolean = true,
        background: Boolean = true,
        notifications: Boolean = true,
        motion: Boolean = true,
    ): OnboardingPermissionStatus =
        OnboardingPermissionStatus(
            locationGranted = location,
            backgroundGranted = background,
            notificationsGranted = notifications,
            motionGranted = motion,
        )

    private fun batteryStatus(
        isKnownAggressiveOem: Boolean = false,
        isIgnoringOptimizations: Boolean = true,
        isPowerSaveMode: Boolean = false,
    ): BatteryReliabilityStatus =
        BatteryReliabilityStatus(
            manufacturer = "Test",
            isKnownAggressiveOem = isKnownAggressiveOem,
            isIgnoringOptimizations = isIgnoringOptimizations,
            isPowerSaveMode = isPowerSaveMode,
        )
}
