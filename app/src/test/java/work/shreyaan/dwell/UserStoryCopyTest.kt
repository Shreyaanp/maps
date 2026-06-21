package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserStoryCopyTest {
    @Test
    fun searchPlaceholderSeparatesCreateAndEditModes() {
        assertEquals(
            "Search place or address",
            mapSearchPlaceholder(hasPlace = false, editingSelectedPlace = false),
        )
        assertEquals(
            "Search to move selected place",
            mapSearchPlaceholder(hasPlace = true, editingSelectedPlace = true),
        )
        assertEquals(
            "Search places",
            mapSearchPlaceholder(hasPlace = true, editingSelectedPlace = false),
        )
    }

    @Test
    fun searchFieldActionsKeepCloseAvailableWhenDropdownIsOpen() {
        assertEquals(
            SearchFieldActions(
                showProgress = false,
                showClear = false,
                showClose = false,
            ),
            searchFieldActions(
                expanded = false,
                searching = false,
                locating = false,
                searchText = "",
            ),
        )
        assertEquals(
            SearchFieldActions(
                showProgress = false,
                showClear = true,
                showClose = false,
            ),
            searchFieldActions(
                expanded = false,
                searching = false,
                locating = false,
                searchText = "office",
            ),
        )
        assertEquals(
            SearchFieldActions(
                showProgress = false,
                showClear = true,
                showClose = true,
            ),
            searchFieldActions(
                expanded = true,
                searching = false,
                locating = false,
                searchText = "office",
            ),
        )
        assertEquals(
            SearchFieldActions(
                showProgress = true,
                showClear = false,
                showClose = true,
            ),
            searchFieldActions(
                expanded = true,
                searching = false,
                locating = true,
                searchText = "",
            ),
        )
    }

    @Test
    fun collapsedSearchPanelHidesDropdownEvenWithStaleResults() {
        assertFalse(
            searchDropdownVisible(
                expanded = false,
                searchFocused = true,
                searchText = "office",
                hasResults = true,
                hasSuggestions = true,
                searching = true,
            )
        )
        assertTrue(
            searchDropdownVisible(
                expanded = true,
                searchFocused = false,
                searchText = "office",
                hasResults = false,
                hasSuggestions = false,
                searching = false,
            )
        )
        assertTrue(
            searchDropdownVisible(
                expanded = true,
                searchFocused = false,
                searchText = "",
                hasResults = true,
                hasSuggestions = false,
                searching = false,
            )
        )
        assertFalse(
            searchDropdownVisible(
                expanded = true,
                searchFocused = false,
                searchText = "",
                hasResults = false,
                hasSuggestions = false,
                searching = false,
            )
        )
    }

    @Test
    fun clearingSearchAlsoClearsVisibleInFlightState() {
        assertEquals(
            SearchRuntimeState(
                searching = false,
                searchingQueryKey = "",
            ),
            clearedSearchRuntimeState(),
        )
        assertTrue(
            searchCompletionShouldUpdateUi(
                searchPanelExpanded = true,
                currentSearchText = "Office",
                completedQueryKey = "office",
            )
        )
        assertFalse(
            searchCompletionShouldUpdateUi(
                searchPanelExpanded = false,
                currentSearchText = "Office",
                completedQueryKey = "office",
            )
        )
        assertFalse(
            searchCompletionShouldUpdateUi(
                searchPanelExpanded = true,
                currentSearchText = "Gym",
                completedQueryKey = "office",
            )
        )
        assertFalse(
            shouldRunSearchAutocomplete(
                searchPanelExpanded = true,
                networkAutocomplete = true,
                currentSearchText = "Gym",
                pendingQueryKey = "gym",
                submittedSearchKey = "",
                searching = true,
            )
        )
        assertTrue(
            shouldRunSearchAutocomplete(
                searchPanelExpanded = true,
                networkAutocomplete = true,
                currentSearchText = "Gym",
                pendingQueryKey = "gym",
                submittedSearchKey = "",
                searching = false,
            )
        )
        assertFalse(
            shouldRunSearchAutocomplete(
                searchPanelExpanded = true,
                networkAutocomplete = true,
                currentSearchText = "Gym",
                pendingQueryKey = "office",
                submittedSearchKey = "",
                searching = false,
            )
        )
    }

    @Test
    fun closingSearchPanelIsAFinalDismiss() {
        assertEquals(
            SearchPanelClosePlan(
                expanded = false,
                clearText = true,
                clearFocus = true,
            ),
            searchPanelClosePlan(),
        )
    }

    @Test
    fun idleHomeStatusKeepsSelectedPlaceAheadOfGlobalMonitoring() {
        assertEquals(
            "Ready to monitor",
            idleHomeStatusTitle(
                hasSelectedPlace = true,
                hasPin = true,
                armedPlaceCount = 2,
            ),
        )
        assertEquals(
            "Tap Monitor for this place. Other places stay live.",
            idleHomeStatusDetail(
                hasSelectedPlace = true,
                hasPin = true,
                armedPlaceCount = 2,
            ),
        )
        assertEquals(
            "1 place monitoring",
            idleHomeStatusTitle(
                hasSelectedPlace = false,
                hasPin = false,
                armedPlaceCount = 1,
            ),
        )
        assertEquals(
            "3 places monitoring",
            idleHomeStatusTitle(
                hasSelectedPlace = false,
                hasPin = false,
                armedPlaceCount = 3,
            ),
        )
        assertEquals(
            "Other saved places are monitoring arrivals",
            idleHomeStatusDetail(
                hasSelectedPlace = false,
                hasPin = false,
                armedPlaceCount = 3,
            ),
        )
        assertEquals(
            "Choose a place",
            idleHomeStatusTitle(
                hasSelectedPlace = false,
                hasPin = false,
                armedPlaceCount = 0,
            ),
        )
        assertEquals(
            "Search, use current location, or long-press the map.",
            idleHomeStatusDetail(
                hasSelectedPlace = false,
                hasPin = false,
                armedPlaceCount = 0,
            ),
        )
        assertEquals(
            "Search, use current location, or long-press the map to add a place",
            addPlacePromptMessage(),
        )
        assertEquals(
            "Search, use current location, or long-press the map to choose a place first",
            choosePlaceForMonitoringMessage(),
        )
        assertEquals(
            "Open Places to pause monitoring for a place",
            choosePlaceToPauseMessage(),
        )
        assertEquals(
            "Pause monitoring before increasing this place's radius.",
            pauseMonitoringBeforeIncreasingRadiusMessage(),
        )
        assertEquals(
            "Pause monitoring for Office before changing its location.",
            pauseMonitoringBeforeChangingLocationMessage("Office"),
        )
        assertEquals(
            "Pause monitoring before changing this place's location.",
            pauseMonitoringBeforeChangingLocationMessage("Saved place"),
        )
        assertEquals(
            "Save, move, or cancel the preview before Monitor, Start now, Remove, or Pause monitoring can run.",
            unsavedRuntimeActionsBlockedDetail(),
        )
        assertEquals(
            "Finish setup",
            homeMonitorActionLabel(
                monitoringNeedsSetup = true,
                activePlaceArmed = true,
            ),
        )
        assertEquals(
            "Tap Finish setup to restore arrival detection.",
            homeSetupRecoveryDetail(),
        )
        assertEquals(
            HomeMonitorActionTarget.OpenSetupChecks,
            homeMonitorActionTarget(
                monitoringNeedsSetup = true,
                activePlaceArmed = true,
            ),
        )
        assertEquals(
            "Pause monitoring",
            homeMonitorActionLabel(
                monitoringNeedsSetup = false,
                activePlaceArmed = true,
            ),
        )
        assertEquals(
            HomeMonitorActionTarget.PauseMonitoring,
            homeMonitorActionTarget(
                monitoringNeedsSetup = false,
                activePlaceArmed = true,
            ),
        )
        assertEquals(
            "Monitor",
            homeMonitorActionLabel(
                monitoringNeedsSetup = false,
                activePlaceArmed = false,
            ),
        )
        assertEquals(
            HomeMonitorActionTarget.StartMonitoring,
            homeMonitorActionTarget(
                monitoringNeedsSetup = false,
                activePlaceArmed = false,
            ),
        )
    }

    @Test
    fun currentLocationSubtitleSeparatesMoveFromCreate() {
        assertEquals(
            "Move selected place where you are now",
            currentLocationActionSubtitle(editingSelectedPlace = true),
        )
        assertEquals(
            "Drop a place where you are now",
            currentLocationActionSubtitle(editingSelectedPlace = false),
        )
        assertEquals(
            "Move selected place nearby",
            currentLocationActionSubtitle(editingSelectedPlace = true, compact = true),
        )
        assertEquals(
            "Fastest way to create a nearby place",
            currentLocationActionSubtitle(editingSelectedPlace = false, compact = true),
        )
        assertEquals(
            "Center the map where you are",
            currentLocationActionSubtitle(
                editingSelectedPlace = false,
                selectsPlace = false,
            ),
        )
        assertEquals(
            "Center the map nearby",
            currentLocationActionSubtitle(
                editingSelectedPlace = false,
                selectsPlace = false,
                compact = true,
            ),
        )
        assertEquals(
            true,
            mapCurrentLocationSelectsPlace(
                selectionMode = PlaceSelectionMode.CreateNew,
                hasEditingPlace = false,
            ),
        )
        assertEquals(
            true,
            mapCurrentLocationSelectsPlace(
                selectionMode = PlaceSelectionMode.EditSelected,
                hasEditingPlace = true,
            ),
        )
        assertEquals(
            false,
            mapCurrentLocationSelectsPlace(
                selectionMode = PlaceSelectionMode.EditSelected,
                hasEditingPlace = false,
            ),
        )
        assertEquals(
            false,
            mapCurrentLocationSelectsPlace(
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasEditingPlace = true,
            ),
        )
        assertEquals(
            MapPointSelectionBehavior(
                label = "Dropped pin",
                analyticsSource = "map_long_press",
                forceCreateNew = true,
                expandDock = true,
            ),
            longPressMapSelectionBehavior(),
        )
        assertEquals(
            "Use current location to add a place",
            mapCurrentLocationActionDescription(
                selectsPlace = true,
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Use current location to move selected place",
            mapCurrentLocationActionDescription(
                selectsPlace = true,
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Center map on current location",
            mapCurrentLocationActionDescription(
                selectsPlace = false,
                editingSelectedPlace = true,
            ),
        )
        assertEquals("Allow location to use your current place.", currentLocationPermissionPrompt(selectAsZone = true))
        assertEquals("Allow location to show your position.", currentLocationPermissionPrompt(selectAsZone = false))
        assertEquals(
            "Finish the open Android permission prompt first.",
            currentLocationPermissionAlreadyActiveMessage(backgroundDisclosureVisible = false),
        )
        assertEquals(
            "Finish the open background location setup first.",
            currentLocationPermissionAlreadyActiveMessage(backgroundDisclosureVisible = true),
        )
        assertEquals(
            "Could not get current location. Check Location, or search/long-press instead.",
            currentLocationUnavailableMessage(selectAsZone = true),
        )
        assertEquals(
            "Could not get current location. Check that Location is on.",
            currentLocationUnavailableMessage(selectAsZone = false),
        )
        assertEquals(
            "Location is blocked. Open app settings to allow it, or search/long-press the map.",
            currentLocationPermissionDeniedMessage(selectAsZone = true),
        )
        assertEquals(
            "Location is blocked. Open app settings to allow it, or move the map manually.",
            currentLocationPermissionDeniedMessage(selectAsZone = false),
        )
        assertEquals("Open app settings", permissionRecoveryActionLabel())
    }

    @Test
    fun noSearchResultsCopyOffersCurrentLocationFallback() {
        assertEquals(
            "Try a landmark, or use current location to move this place.",
            noSearchResultsDetail(editingSelectedPlace = true),
        )
        assertEquals(
            "Try a landmark, or use current location to add a place.",
            noSearchResultsDetail(editingSelectedPlace = false),
        )
        assertEquals(
            "Try another landmark, or tap Add place to create a new place.",
            noSearchResultsDetail(
                editingSelectedPlace = false,
                currentLocationSelectsPlace = false,
            ),
        )
        assertEquals(
            "No places found. Use current location to move this place.",
            noSearchResultsToast(editingSelectedPlace = true),
        )
        assertEquals(
            "No places found. Use current location to add a place.",
            noSearchResultsToast(editingSelectedPlace = false),
        )
        assertEquals(
            "No places found. Tap Add place to create a new place.",
            noSearchResultsToast(
                editingSelectedPlace = false,
                currentLocationSelectsPlace = false,
            ),
        )
    }

    @Test
    fun durationInputValidationExplainsProblemsBeforeMonitorOrStart() {
        assertEquals(270, durationMinutesFromText("4.5"))
        assertEquals(1, durationMinutesFromText("0.01"))
        assertEquals(null, durationMinutesFromText(""))
        assertEquals(null, durationMinutesFromText("NaN"))
        assertEquals(null, durationMinutesFromText("49"))

        assertEquals("Enter a timer duration.", durationInputError(""))
        assertEquals("Use hours like 1, 1.5, or 4.5.", durationInputError("tomorrow"))
        assertEquals("Use hours like 1, 1.5, or 4.5.", durationInputError("Infinity"))
        assertEquals("Timer must be greater than 0 hours.", durationInputError("0"))
        assertEquals("Timer can be up to 48 hours.", durationInputError("49"))
        assertEquals(null, durationInputError("4.5"))
        assertEquals("Timer can be up to 48 hours.", durationActionErrorMessage("49"))
        assertEquals("Use hours like 1, 1.5, or 4.5.", durationActionErrorMessage("later"))
        assertEquals("Fix duration", durationFixActionLabel())
        assertEquals("Fix duration | Home", durationFixCollapsedDetail("Home"))
        assertEquals("Fix duration", durationFixCollapsedDetail(""))

        assertEquals(
            true,
            primarySetupActionBlockedByDuration(
                durationText = "nope",
                pendingPlacePreview = true,
                activePlaceArmed = false,
            ),
        )
        assertEquals(
            true,
            primarySetupActionBlockedByDuration(
                durationText = "nope",
                pendingPlacePreview = false,
                activePlaceArmed = false,
            ),
        )
        assertEquals(
            false,
            primarySetupActionBlockedByDuration(
                durationText = "nope",
                pendingPlacePreview = false,
                activePlaceArmed = true,
            ),
        )
        assertEquals(
            false,
            primarySetupActionBlockedByDuration(
                durationText = "4.5",
                pendingPlacePreview = true,
                activePlaceArmed = false,
            ),
        )
        assertEquals(
            true,
            secondaryTimerActionBlockedByDuration(
                durationText = "nope",
                pendingPlacePreview = false,
            ),
        )
        assertEquals(
            false,
            secondaryTimerActionBlockedByDuration(
                durationText = "nope",
                pendingPlacePreview = true,
            ),
        )
        assertEquals(
            null,
            actionDurationMinutes(
                durationText = "nope",
                durationInputVisible = true,
                actionPlaceDurationMinutes = 90,
                defaultDurationMinutes = 270,
            ),
        )
        assertEquals(
            90,
            actionDurationMinutes(
                durationText = "nope",
                durationInputVisible = false,
                actionPlaceDurationMinutes = 90,
                defaultDurationMinutes = 270,
            ),
        )
        assertEquals(
            90,
            actionDurationMinutes(
                durationText = "4.5",
                durationInputVisible = false,
                actionPlaceDurationMinutes = 90,
                defaultDurationMinutes = 270,
            ),
        )
        assertEquals(
            270,
            actionDurationMinutes(
                durationText = "nope",
                durationInputVisible = false,
                actionPlaceDurationMinutes = null,
                defaultDurationMinutes = 270,
            ),
        )
        assertEquals(
            90,
            monitoringActionDurationMinutes(
                durationText = "nope",
                durationInputVisible = true,
                actionPlaceDurationMinutes = 90,
                actionPlaceAlreadyMonitoring = true,
                defaultDurationMinutes = 270,
            ),
        )
        assertEquals(
            null,
            monitoringActionDurationMinutes(
                durationText = "nope",
                durationInputVisible = true,
                actionPlaceDurationMinutes = 90,
                actionPlaceAlreadyMonitoring = false,
                defaultDurationMinutes = 270,
            ),
        )
        assertEquals(
            null,
            monitoringActionDurationMinutes(
                durationText = "nope",
                durationInputVisible = true,
                actionPlaceDurationMinutes = null,
                actionPlaceAlreadyMonitoring = false,
                defaultDurationMinutes = 270,
            ),
        )
    }

    @Test
    fun durationPresetsIncludeFastTestAndCommonUseDurations() {
        assertEquals(
            listOf(
                DurationPresetOption(hours = 0.05, value = "0.05", label = "3m", selected = false),
                DurationPresetOption(hours = 0.25, value = "0.25", label = "15m", selected = false),
                DurationPresetOption(hours = 0.5, value = "0.5", label = "30m", selected = false),
                DurationPresetOption(hours = 1.0, value = "1", label = "1h", selected = false),
                DurationPresetOption(hours = 2.0, value = "2", label = "2h", selected = false),
                DurationPresetOption(hours = 4.5, value = "4.5", label = "4.5h", selected = true),
                DurationPresetOption(hours = 8.0, value = "8", label = "8h", selected = false),
            ),
            durationPresetOptions("4.5"),
        )
    }

    @Test
    fun arrivalModeLabelsMatchTheUserStory() {
        assertEquals("Auto-start", arrivalModeLabel(autoStart = true))
        assertEquals("Confirm first", arrivalModeLabel(autoStart = false))
        assertEquals("High-confidence arrivals start the timer.", arrivalModeDetail(autoStart = true))
        assertEquals("Dwell asks before starting here.", arrivalModeDetail(autoStart = false))
        assertEquals(
            listOf(
                ArrivalModeOption(
                    autoStart = true,
                    label = "Auto-start",
                    detail = "High-confidence arrivals start the timer.",
                    selected = true,
                ),
                ArrivalModeOption(
                    autoStart = false,
                    label = "Confirm first",
                    detail = "Dwell asks before starting here.",
                    selected = false,
                ),
            ),
            arrivalModeOptions(autoStart = true),
        )
        assertEquals(
            listOf(
                ArrivalModeOption(
                    autoStart = true,
                    label = "Auto-start",
                    detail = "High-confidence arrivals start the timer.",
                    selected = false,
                ),
                ArrivalModeOption(
                    autoStart = false,
                    label = "Confirm first",
                    detail = "Dwell asks before starting here.",
                    selected = true,
                ),
            ),
            arrivalModeOptions(autoStart = false),
        )
    }

    @Test
    fun timerRunningTitleNamesTheTimerPlace() {
        assertEquals("Timer running at Home", timerRunningStatusTitle("Home"))
        assertEquals("Timer running", timerRunningStatusTitle(""))
        assertEquals("Timer running", timerRunningStatusTitle("No place selected"))
        assertEquals("Timer running", timerRunningStatusTitle("Selected place"))
        assertEquals("Timer running", timerRunningStatusTitle("Saved place"))
    }

    @Test
    fun arrivalNotificationCanRefreshLegacyPromptWithoutConfidence() {
        assertEquals(
            "Dwell thinks you arrived at Office. Confidence 72%.",
            Notifications.arrivalQuestionText("Office", 72),
        )
        assertEquals(
            "Dwell thinks you arrived at Office.",
            Notifications.arrivalQuestionText("Office", -1),
        )
        assertEquals(
            "Dwell thinks you arrived.",
            Notifications.arrivalQuestionText("", -1),
        )
    }

    @Test
    fun placeRenameRefreshesTheVisibleNotificationSurface() {
        assertEquals(
            PlaceRenameNotificationRefresh.RunningTimer,
            placeRenameNotificationRefresh(
                renamedPlaceId = "office",
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerPlaceId = "office",
                timerRunning = true,
            ),
        )
        assertEquals(
            PlaceRenameNotificationRefresh.ArrivalPrompt,
            placeRenameNotificationRefresh(
                renamedPlaceId = "office",
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                timerPlaceId = "",
                timerRunning = false,
            ),
        )
        assertEquals(
            PlaceRenameNotificationRefresh.SwitchPrompt,
            placeRenameNotificationRefresh(
                renamedPlaceId = "office",
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                timerRunning = true,
            ),
        )
        assertEquals(
            PlaceRenameNotificationRefresh.SwitchPrompt,
            placeRenameNotificationRefresh(
                renamedPlaceId = "gym",
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                timerRunning = true,
            ),
        )
        assertEquals(
            PlaceRenameNotificationRefresh.LeavePrompt,
            placeRenameNotificationRefresh(
                renamedPlaceId = "office",
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "",
                timerPlaceId = "office",
                timerRunning = true,
            ),
        )
        assertEquals(
            PlaceRenameNotificationRefresh.TimeUpPrompt,
            placeRenameNotificationRefresh(
                renamedPlaceId = "office",
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptPlaceId = "",
                timerPlaceId = "office",
                timerRunning = false,
            ),
        )
        assertEquals(
            PlaceRenameNotificationRefresh.None,
            placeRenameNotificationRefresh(
                renamedPlaceId = "home",
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                timerRunning = true,
            ),
        )
    }

    @Test
    fun homePromptStateMakesPhonePromptActionsExplicit() {
        assertEquals(
            HomePromptState(
                kind = HomePromptKind.Arrival,
                title = "Start timer at Office?",
                detail = "1h 30m timer for Office.",
                placeLabel = "Office",
                primaryLabel = "Start",
                secondaryLabel = "Not now",
            ),
            homePromptState(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceLabel = "Office",
                timerRunning = false,
                timerPlaceLabel = "",
                durationMinutes = 90,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )
        assertEquals(
            HomePromptState(
                kind = HomePromptKind.SwitchPlace,
                title = "Switch to Gym?",
                detail = "Stop Office and start Gym?",
                placeLabel = "Gym",
                primaryLabel = "Switch",
                secondaryLabel = "Keep current",
            ),
            homePromptState(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceLabel = "Gym",
                timerRunning = true,
                timerPlaceLabel = "Office",
                durationMinutes = 60,
                timerEnd = 120_000L,
                now = 1_000L,
            ),
        )
        assertEquals(
            HomePromptState(
                kind = HomePromptKind.LeaveEarly,
                title = "Leaving Office?",
                detail = "Keep the Office timer? 1h 59m left.",
                placeLabel = "Office",
                primaryLabel = "Keep timer",
                secondaryLabel = "Cancel timer",
            ),
            homePromptState(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceLabel = "Office",
                timerRunning = true,
                timerPlaceLabel = "Office",
                durationMinutes = 60,
                timerEnd = 7_199_000L,
                now = 0L,
            ),
        )
        assertEquals(
            HomePromptState(
                kind = HomePromptKind.TimeUp,
                title = "Time's up at Office",
                detail = "1h timer complete. Extend or mark done.",
                placeLabel = "Office",
                primaryLabel = "Extend 30m",
                secondaryLabel = "Done",
            ),
            homePromptState(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptPlaceLabel = "",
                timerRunning = false,
                timerPlaceLabel = "Office",
                durationMinutes = 60,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun homePromptActionScopeRejectsStalePromptTokensPlacesAndTimers() {
        val switchScope = HomePromptActionScope(
            kind = HomePromptKind.SwitchPlace,
            prompt = Prefs.WATCH_PROMPT_START_TIMER,
            promptPlaceId = "gym",
            promptUpdated = 42L,
            timerPlaceId = "office",
            timerStartedAt = 1_000L,
            timerEnd = 10_000L,
        )

        assertTrue(
            acceptsHomePromptAction(
                scope = switchScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "gym",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = switchScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "gym",
                currentPromptUpdated = 43L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = switchScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "home",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = switchScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "gym",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "home",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = switchScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "gym",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 10_000L,
            )
        )
    }

    @Test
    fun timerCancelConfirmationOnlyAppliesToSameRunningTimer() {
        val scope = TimerCancelActionScope(
            timerPlaceId = "office",
            timerStartedAt = 1_000L,
            timerEnd = 10_000L,
            timerPlaceLabel = "Office",
        )

        assertTrue(
            acceptsTimerCancelConfirmation(
                scope = scope,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsTimerCancelConfirmation(
                scope = scope,
                currentTimerPlaceId = "gym",
                currentTimerStartedAt = 2_000L,
                currentTimerEnd = 11_000L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsTimerCancelConfirmation(
                scope = scope,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 10_000L,
            )
        )
        assertFalse(
            acceptsTimerCancelConfirmation(
                scope = null,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
        assertEquals(
            "Office HQ",
            timerCancelDialogPlaceLabel(
                scope = scope,
                currentTimerPlaceId = "office",
                currentTimerPlaceLabel = "Office HQ",
            ),
        )
        assertEquals(
            "Office",
            timerCancelDialogPlaceLabel(
                scope = scope,
                currentTimerPlaceId = "gym",
                currentTimerPlaceLabel = "Gym",
            ),
        )
        assertEquals(
            "",
            timerCancelDialogPlaceLabel(
                scope = scope.copy(timerPlaceId = "", timerPlaceLabel = ""),
                currentTimerPlaceId = "",
                currentTimerPlaceLabel = "",
            ),
        )
    }

    @Test
    fun arrivalDockPromptStopsApplyingAfterTimerStarts() {
        val arrivalScope = HomePromptActionScope(
            kind = HomePromptKind.Arrival,
            prompt = Prefs.WATCH_PROMPT_START_TIMER,
            promptPlaceId = "office",
            promptUpdated = 42L,
            timerPlaceId = "",
            timerStartedAt = 0L,
            timerEnd = 0L,
        )

        assertTrue(
            acceptsHomePromptAction(
                scope = arrivalScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "",
                currentTimerStartedAt = 0L,
                currentTimerEnd = 0L,
                now = 1_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = arrivalScope,
                currentPrompt = Prefs.WATCH_PROMPT_START_TIMER,
                currentPromptPlaceId = "office",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
    }

    @Test
    fun timeUpDockPromptRequiresCurrentPromptAndTimerPlace() {
        val timeUpScope = HomePromptActionScope(
            kind = HomePromptKind.TimeUp,
            prompt = Prefs.WATCH_PROMPT_TIME_UP,
            promptPlaceId = "",
            promptUpdated = 42L,
            timerPlaceId = "office",
            timerStartedAt = 0L,
            timerEnd = 0L,
        )

        assertTrue(
            acceptsHomePromptAction(
                scope = timeUpScope,
                currentPrompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 0L,
                currentTimerEnd = 0L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = timeUpScope,
                currentPrompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptPlaceId = "",
                currentPromptUpdated = 43L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 0L,
                currentTimerEnd = 0L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = timeUpScope,
                currentPrompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "gym",
                currentTimerStartedAt = 0L,
                currentTimerEnd = 0L,
                now = 5_000L,
            )
        )
        assertFalse(
            acceptsHomePromptAction(
                scope = timeUpScope,
                currentPrompt = Prefs.WATCH_PROMPT_TIME_UP,
                currentPromptPlaceId = "",
                currentPromptUpdated = 42L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 5_000L,
            )
        )
    }

    @Test
    fun promptRemainingCopyHandlesSubMinuteTime() {
        assertEquals("<1m", formatRemainingPromptDuration(30_000L))
        assertEquals("0m", formatRemainingPromptDuration(0L))
        assertEquals("2h", formatRemainingPromptDuration(7_200_000L))
    }

    @Test
    fun alreadyInsideCopyReflectsFinalTimerOrPromptState() {
        assertEquals(
            "You are already at Office - timer started",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.START_TIMER,
                timerRunningForPlace = true,
            ),
        )
        assertEquals(
            "Dwell thinks you are at Office. Confirm start from Home or the notification.",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.ASK_TO_START,
                timerRunningForPlace = false,
            ),
        )
        assertEquals(
            "Timer already running at Office",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.WAIT,
                timerRunningForPlace = true,
            ),
        )
        assertEquals(
            "Office is monitoring arrivals. Dwell will start when you arrive.",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.WAIT,
                timerRunningForPlace = false,
                waitReason = AlreadyInsideWaitReason.OUTSIDE_PLACE,
            ),
        )
        assertEquals(
            "Office is monitoring arrivals. Current location was unavailable, so Dwell will keep watching.",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.WAIT,
                timerRunningForPlace = false,
                waitReason = AlreadyInsideWaitReason.LOCATION_UNAVAILABLE,
            ),
        )
        assertEquals(
            "Office is monitoring arrivals, but another timer is already running.",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.WAIT,
                timerRunningForPlace = false,
                waitReason = AlreadyInsideWaitReason.TIMER_ALREADY_RUNNING,
            ),
        )
        assertEquals(
            "Office is monitoring arrivals. Dwell will wait for a stronger location signal.",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.WAIT,
                timerRunningForPlace = false,
                waitReason = AlreadyInsideWaitReason.LOW_CONFIDENCE,
            ),
        )
        assertEquals(
            "Office is monitoring arrivals. Other monitored places stay live.",
            alreadyInsideResultMessage(
                placeLabel = "Office",
                decision = ArrivalDecision.WAIT,
                timerRunningForPlace = false,
            ),
        )
    }

    @Test
    fun alreadyInsideCheckRequiresStillMonitoredPlace() {
        val monitored = testPlace("office", monitoringEnabled = true)
        val paused = testPlace("office", monitoringEnabled = false)

        assertEquals(true, shouldRunAlreadyInsideCheck("office", monitored))
        assertEquals(false, shouldRunAlreadyInsideCheck("office", paused))
        assertEquals(false, shouldRunAlreadyInsideCheck("office", null))
        assertEquals(false, shouldRunAlreadyInsideCheck("office", testPlace("gym")))
    }

    @Test
    fun alreadyInsideCheckEvaluatesNearRadiusAccuracyInsteadOfRejectingEarly() {
        assertEquals(
            true,
            shouldEvaluateAlreadyInsideConfidence(
                distanceMeters = 60f,
                radiusMeters = 50f,
                accuracyMeters = 20f,
            ),
        )
        assertEquals(
            true,
            shouldEvaluateAlreadyInsideConfidence(
                distanceMeters = 195f,
                radiusMeters = 50f,
                accuracyMeters = null,
            ),
        )
        assertEquals(
            false,
            shouldEvaluateAlreadyInsideConfidence(
                distanceMeters = 220f,
                radiusMeters = 50f,
                accuracyMeters = 20f,
            ),
        )
    }

    @Test
    fun locationPermissionResumeKeepsOriginalCurrentLocationMode() {
        assertEquals(
            LocationPermissionResume(
                selectAsZone = false,
                expandDock = false,
            ),
            locationPermissionResume(
                requested = true,
                selectAsZone = false,
                expandDock = false,
                hasFineLocation = true,
            ),
        )
        assertEquals(
            LocationPermissionResume(
                selectAsZone = true,
                expandDock = true,
                selectionMode = PlaceSelectionMode.EditSelected,
                targetPlaceId = "home",
            ),
            locationPermissionResume(
                requested = true,
                selectAsZone = true,
                expandDock = true,
                selectionMode = PlaceSelectionMode.EditSelected,
                targetPlaceId = "home",
                hasFineLocation = true,
            ),
        )
        assertEquals(
            null,
            locationPermissionResume(
                requested = true,
                selectAsZone = false,
                expandDock = false,
                hasFineLocation = false,
            ),
        )
        assertEquals(
            true,
            shouldLaunchCurrentLocationPermissionRequest(
                hasFineLocation = false,
                permissionUiAlreadyActive = false,
            ),
        )
        assertEquals(
            false,
            shouldLaunchCurrentLocationPermissionRequest(
                hasFineLocation = false,
                permissionUiAlreadyActive = true,
            ),
        )
        assertEquals(
            false,
            shouldLaunchCurrentLocationPermissionRequest(
                hasFineLocation = true,
                permissionUiAlreadyActive = false,
            ),
        )
    }

    @Test
    fun persistedCurrentLocationResumeKeepsOriginalModeAfterRecreate() {
        val restored = Prefs.PersistedCurrentLocationResume(
            selectAsZone = false,
            expandDock = false,
            selectionModeName = PlaceSelectionMode.ViewSelected.name,
            targetPlaceId = "office",
            requestedAtMillis = 1_000L,
        )

        assertEquals(
            true,
            Prefs.pendingCurrentLocationResumeIsActive(
                requestedAtMillis = restored.requestedAtMillis,
                nowMillis = 1_500L,
            ),
        )
        assertEquals(
            LocationPermissionResume(
                selectAsZone = false,
                expandDock = false,
                selectionMode = PlaceSelectionMode.ViewSelected,
                targetPlaceId = "office",
            ),
            locationPermissionResume(
                requested = true,
                selectAsZone = restored.selectAsZone,
                expandDock = restored.expandDock,
                selectionMode = PlaceSelectionMode.valueOf(restored.selectionModeName!!),
                targetPlaceId = restored.targetPlaceId,
                hasFineLocation = true,
            ),
        )
    }

    @Test
    fun currentLocationResultsIgnoreStaleCallbacksAfterReset() {
        assertTrue(
            currentLocationResultStillCurrent(
                requestGeneration = 2L,
                activeGeneration = 2L,
            )
        )
        assertFalse(
            currentLocationResultStillCurrent(
                requestGeneration = 1L,
                activeGeneration = 2L,
            )
        )
        assertFalse(
            currentLocationResultStillCurrent(
                requestGeneration = 3L,
                activeGeneration = 2L,
            )
        )
    }

    @Test
    fun manualPlaceSelectionInvalidatesPendingCurrentLocationCallbacks() {
        assertFalse(shouldInvalidateCurrentLocationForSelection("current_location"))
        assertTrue(shouldInvalidateCurrentLocationForSelection("search_result"))
        assertTrue(shouldInvalidateCurrentLocationForSelection("map_long_press"))
    }

    @Test
    fun manualTimerStartResumesOnlyAfterFreshNotificationGrant() {
        val request = ManualTimerStartRequest(
            placeId = "office",
            editablePlaceId = "office",
            durationMinutes = 90,
            requestedAtMillis = 1_000L,
        )

        assertEquals(
            request,
            manualTimerStartAfterNotificationPermission(
                request = request,
                notificationsGranted = true,
                timerRunning = false,
                targetPlaceExists = true,
                nowMillis = 1_500L,
            ),
        )
        assertEquals(
            null,
            manualTimerStartAfterNotificationPermission(
                request = request,
                notificationsGranted = false,
                timerRunning = false,
                targetPlaceExists = true,
                nowMillis = 1_500L,
            ),
        )
        assertEquals(
            null,
            manualTimerStartAfterNotificationPermission(
                request = request,
                notificationsGranted = true,
                timerRunning = true,
                targetPlaceExists = true,
                nowMillis = 1_500L,
            ),
        )
        assertEquals(
            null,
            manualTimerStartAfterNotificationPermission(
                request = request,
                notificationsGranted = true,
                timerRunning = false,
                targetPlaceExists = false,
                nowMillis = 1_500L,
            ),
        )
        assertEquals(
            null,
            manualTimerStartAfterNotificationPermission(
                request = request,
                notificationsGranted = true,
                timerRunning = false,
                targetPlaceExists = true,
                nowMillis = 1_000L + MANUAL_TIMER_START_RESUME_TTL_MS + 1L,
            ),
        )
    }

    @Test
    fun persistedManualTimerStartResumesSamePlaceAfterRecreate() {
        val restored = Prefs.PersistedManualTimerStart(
            placeId = "gym",
            editablePlaceId = null,
            durationMinutes = 30,
            requestedAtMillis = 1_000L,
        )
        val request = ManualTimerStartRequest(
            placeId = restored.placeId,
            editablePlaceId = restored.editablePlaceId,
            durationMinutes = restored.durationMinutes,
            requestedAtMillis = restored.requestedAtMillis,
        )

        assertEquals(
            request,
            manualTimerStartAfterNotificationPermission(
                request = request,
                notificationsGranted = true,
                timerRunning = false,
                targetPlaceExists = true,
                nowMillis = 1_500L,
            ),
        )
    }

    @Test
    fun manualTimerNotificationPermissionDoesNotRelaunchWhilePermissionUiIsOpen() {
        assertTrue(
            shouldLaunchManualTimerNotificationPermissionRequest(
                notificationsGranted = false,
                permissionUiAlreadyActive = false,
            )
        )
        assertFalse(
            shouldLaunchManualTimerNotificationPermissionRequest(
                notificationsGranted = false,
                permissionUiAlreadyActive = true,
            )
        )
        assertFalse(
            shouldLaunchManualTimerNotificationPermissionRequest(
                notificationsGranted = true,
                permissionUiAlreadyActive = false,
            )
        )
        assertEquals(
            "Grant notifications to start the timer",
            manualTimerNotificationPermissionPrompt(permissionUiAlreadyActive = false),
        )
        assertEquals(
            "Finish the open Android permission prompt first.",
            manualTimerNotificationPermissionPrompt(permissionUiAlreadyActive = true),
        )
    }

    @Test
    fun manualTimerNotificationPermissionWaitsOnlyWhileRequestIsInFlight() {
        assertTrue(
            manualTimerNotificationPermissionStillPending(
                notificationsGranted = false,
                permissionRequestInFlight = true,
                requestAgeMillis = 1_000L,
            )
        )
        assertFalse(
            manualTimerNotificationPermissionStillPending(
                notificationsGranted = false,
                permissionRequestInFlight = false,
                requestAgeMillis = 1_000L,
            )
        )
        assertFalse(
            manualTimerNotificationPermissionStillPending(
                notificationsGranted = true,
                permissionRequestInFlight = true,
                requestAgeMillis = 1_000L,
            )
        )
        assertFalse(
            manualTimerNotificationPermissionStillPending(
                notificationsGranted = false,
                permissionRequestInFlight = true,
                requestAgeMillis = MANUAL_TIMER_START_RESUME_TTL_MS + 1L,
            )
        )
        assertEquals(
            "Allow notifications to start the timer",
            manualTimerNotificationDeniedMessage(),
        )
        assertEquals("Open app settings", permissionRecoveryActionLabel())
    }

    @Test
    fun manualTimerPermissionResumeExplainsWhyStartNowDidNotRun() {
        assertEquals(
            "That saved place is no longer available. Pick a place and tap Start now again.",
            missingManualTimerStartPlaceMessage(),
        )
        assertEquals(
            "That saved place is no longer available. Pick a place and tap Start now again.",
            manualTimerStartBlockedAfterPermissionMessage(
                targetPlaceExists = false,
                timerRunning = false,
                pendingPlaceId = "office",
                runningTimerPlaceId = "",
                runningTimerPlaceLabel = "",
                notificationsGranted = true,
                requestAgeMillis = 500L,
            ),
        )
        assertEquals(
            "Timer running at Office",
            manualTimerStartBlockedAfterPermissionMessage(
                targetPlaceExists = true,
                timerRunning = true,
                pendingPlaceId = "office",
                runningTimerPlaceId = "office",
                runningTimerPlaceLabel = "Office",
                notificationsGranted = true,
                requestAgeMillis = 500L,
            ),
        )
        assertEquals(
            "Cancel the Office timer before starting another place.",
            manualTimerStartBlockedAfterPermissionMessage(
                targetPlaceExists = true,
                timerRunning = true,
                pendingPlaceId = "gym",
                runningTimerPlaceId = "office",
                runningTimerPlaceLabel = "Office",
                notificationsGranted = true,
                requestAgeMillis = 500L,
            ),
        )
        assertEquals(
            "Allow notifications to start the timer",
            manualTimerStartBlockedAfterPermissionMessage(
                targetPlaceExists = true,
                timerRunning = false,
                pendingPlaceId = "office",
                runningTimerPlaceId = "",
                runningTimerPlaceLabel = "",
                notificationsGranted = false,
                requestAgeMillis = 500L,
            ),
        )
        assertEquals(
            null,
            manualTimerStartBlockedAfterPermissionMessage(
                targetPlaceExists = true,
                timerRunning = false,
                pendingPlaceId = "office",
                runningTimerPlaceId = "",
                runningTimerPlaceLabel = "",
                notificationsGranted = true,
                requestAgeMillis = MANUAL_TIMER_START_RESUME_TTL_MS + 1L,
            ),
        )
    }

    @Test
    fun pendingPlaceCopyMakesSaveOrMoveExplicit() {
        assertEquals(
            "Save this place",
            pendingPlacePrimaryActionLabel(
                editingSelectedPlace = false,
                targetLabel = "",
            ),
        )
        assertEquals(
            "Move Home",
            pendingPlacePrimaryActionLabel(
                editingSelectedPlace = true,
                targetLabel = "Home",
            ),
        )
        assertEquals(
            "Move place",
            pendingPlacePrimaryActionLabel(
                editingSelectedPlace = true,
                targetLabel = "Selected place",
            ),
        )
        assertEquals(
            "Save this place?",
            pendingPlaceStatusTitle(
                editingSelectedPlace = false,
                targetLabel = "",
            ),
        )
        assertEquals(
            "Move Office?",
            pendingPlaceStatusTitle(
                editingSelectedPlace = true,
                targetLabel = "Office",
            ),
        )
        assertEquals(
            "Move place?",
            pendingPlaceStatusTitle(
                editingSelectedPlace = true,
                targetLabel = "Saved place",
            ),
        )
        assertEquals(
            "This place is not saved yet. Adjust details, then save.",
            pendingPlaceStatusDetail(editingSelectedPlace = false),
        )
        assertEquals(
            "This move is not saved yet. Adjust details, then save.",
            pendingPlaceStatusDetail(editingSelectedPlace = true),
        )
        assertEquals(
            "Unsaved place canceled",
            pendingPlaceCancelMessage(
                editingSelectedPlace = false,
                targetPlaceLabel = "",
            ),
        )
        assertEquals(
            "Home move canceled. Original place kept.",
            pendingPlaceCancelMessage(
                editingSelectedPlace = true,
                targetPlaceLabel = "Home",
            ),
        )
        assertEquals(
            "Move canceled. Saved place is no longer available.",
            pendingPlaceCancelMessage(
                editingSelectedPlace = true,
                targetPlaceLabel = "Home",
                targetPlaceAvailable = false,
            ),
        )
    }

    @Test
    fun mapPreviewNamesPreserveSavedPlaceIdentityWhenMoving() {
        assertEquals(
            "Home",
            mapPreviewPlaceName(
                existingPlaceLabel = "Home",
                sourceLabel = "Current location",
            ),
        )
        assertEquals(
            "Office Tower",
            mapPreviewPlaceName(
                existingPlaceLabel = null,
                sourceLabel = "Office Tower",
            ),
        )
        assertEquals(
            "Saved place",
            mapPreviewPlaceName(
                existingPlaceLabel = "",
                sourceLabel = "",
            ),
        )
    }

    @Test
    fun mapPreviewNameKeepsTypedDraftWhenPointChanges() {
        assertEquals(
            "Home",
            mapPreviewPlaceNameForPointChange(
                existingPlaceLabel = null,
                sourceLabel = "Dropped pin",
                previousPreviewPlaceName = "Home",
                previousPreviewSourceLabel = "Current location",
            ),
        )
        assertEquals(
            "Gym",
            mapPreviewPlaceNameForPointChange(
                existingPlaceLabel = null,
                sourceLabel = "Gym",
                previousPreviewPlaceName = "Office Tower",
                previousPreviewSourceLabel = "Office Tower",
            ),
        )
        assertEquals(
            "Office",
            mapPreviewPlaceNameForPointChange(
                existingPlaceLabel = "Office",
                sourceLabel = "Current location",
                previousPreviewPlaceName = "Gym",
                previousPreviewSourceLabel = "Gym",
            ),
        )
    }

    @Test
    fun placeNameInputMakesFallbackAndLimitExplicit() {
        assertEquals(
            "Leave blank to use Office Tower.",
            placeNameSupportingText(
                placeName = "",
                fallbackLabel = "Office Tower",
            ),
        )
        assertEquals(
            "Add a name like Home, Office, or Gym.",
            placeNameSupportingText(
                placeName = "",
                fallbackLabel = "",
            ),
        )
        assertEquals(
            "Add a name like Home, Office, or Gym.",
            placeNameSupportingText(
                placeName = "",
                fallbackLabel = "Selected place",
            ),
        )
        assertEquals(
            "10 characters left.",
            placeNameSupportingText(
                placeName = "A".repeat(DwellPlace.MAX_LABEL_LENGTH - 10),
                fallbackLabel = "",
            ),
        )
        assertEquals(
            "Name limit reached.",
            placeNameSupportingText(
                placeName = "A".repeat(DwellPlace.MAX_LABEL_LENGTH),
                fallbackLabel = "",
            ),
        )
        assertEquals(
            DwellPlace.MAX_LABEL_LENGTH,
            placeNameInputValue("A".repeat(DwellPlace.MAX_LABEL_LENGTH + 20)).length,
        )
        assertEquals(
            null,
            placeNameSupportingText(
                placeName = "Home",
                fallbackLabel = "",
            ),
        )
        assertEquals(
            "Home",
            selectedPlaceDisplayLabel(
                hasPin = true,
                typedPlaceLabel = "",
                fallbackLabel = "Home",
            ),
        )
        assertEquals(
            "Gym",
            selectedPlaceDisplayLabel(
                hasPin = true,
                typedPlaceLabel = "Gym",
                fallbackLabel = "Home",
            ),
        )
        assertEquals(
            "Dropped pin",
            selectedPlaceDisplayLabel(
                hasPin = true,
                typedPlaceLabel = "",
                fallbackLabel = "",
            ),
        )
        assertEquals(
            "Dropped pin",
            selectedPlaceDisplayLabel(
                hasPin = true,
                typedPlaceLabel = "",
                fallbackLabel = "Saved place",
            ),
        )
        assertEquals(
            "No place selected",
            selectedPlaceDisplayLabel(
                hasPin = false,
                typedPlaceLabel = "Gym",
                fallbackLabel = "Home",
            ),
        )
        assertTrue(
            hasMapSearchContext(
                hasPin = false,
                hasSavedPlace = true,
                hasPendingPlacePreview = false,
            )
        )
        assertFalse(
            hasMapSearchContext(
                hasPin = false,
                hasSavedPlace = false,
                hasPendingPlacePreview = false,
            )
        )
        assertTrue(
            hasActionableDockPlace(
                hasPin = false,
                hasActivePlace = false,
                timerActive = true,
                promptActive = false,
            )
        )
        assertTrue(
            hasActionableDockPlace(
                hasPin = false,
                hasActivePlace = true,
                timerActive = false,
                promptActive = false,
            )
        )
        assertFalse(
            hasActionableDockPlace(
                hasPin = false,
                hasActivePlace = false,
                timerActive = false,
                promptActive = false,
            )
        )
        assertEquals(
            "Home",
            pendingPlaceCommitLabel(
                typedPlaceLabel = "",
                previewPlaceName = "",
                sourceLabel = "Current location",
                targetPlaceLabel = "Home",
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Office Tower",
            pendingPlaceCommitLabel(
                typedPlaceLabel = "",
                previewPlaceName = "",
                sourceLabel = "Office Tower",
                targetPlaceLabel = "",
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Gym",
            pendingPlaceCommitLabel(
                typedPlaceLabel = "Gym",
                previewPlaceName = "Office Tower",
                sourceLabel = "Office Tower",
                targetPlaceLabel = "Home",
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Saved place",
            pendingPlaceCommitLabel(
                typedPlaceLabel = "",
                previewPlaceName = "",
                sourceLabel = "",
                targetPlaceLabel = "",
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Office Tower",
            pendingPlaceNameFallbackLabel(
                previewPlaceName = "",
                sourceLabel = "Office Tower",
                targetPlaceLabel = "",
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Home",
            pendingPlaceNameFallbackLabel(
                previewPlaceName = "",
                sourceLabel = "Current location",
                targetPlaceLabel = "Home",
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Home saved. Tap Monitor to watch arrivals.",
            pendingPlaceSavedMessage(
                placeLabel = "Home",
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Office already exists. Opened saved place; settings kept.",
            pendingPlaceSavedMessage(
                placeLabel = "Office",
                editingSelectedPlace = false,
                selectedExistingDuplicate = true,
            ),
        )
        assertEquals(
            "Gym moved",
            pendingPlaceSavedMessage(
                placeLabel = "Gym",
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Place saved. Tap Monitor to watch arrivals.",
            pendingPlaceSavedMessage(
                placeLabel = "",
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Place saved. Tap Monitor to watch arrivals.",
            pendingPlaceSavedMessage(
                placeLabel = "Selected place",
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            PendingPlaceCommitFeedback(
                message = "Office already exists. Opened saved place; settings kept.",
                expandDock = true,
            ),
            pendingPlaceCommitFeedback(
                placeLabel = "Office",
                editingSelectedPlace = false,
                selectedExistingDuplicate = true,
            ),
        )
        assertEquals(
            PendingPlaceCommitFeedback(
                message = "Home saved. Tap Monitor to watch arrivals.",
                expandDock = false,
            ),
            pendingPlaceCommitFeedback(
                placeLabel = "Home",
                editingSelectedPlace = false,
                selectedExistingDuplicate = false,
            ),
        )
        assertEquals(
            "Office already overlaps Home. Move it farther away or cancel this move.",
            duplicateEditBlockedMessage(
                editedPlaceLabel = "Office",
                duplicatePlaceLabel = "Home",
            ),
        )
        val home = testPlace(id = "home", label = "Home")
        val office = testPlace(id = "office", label = "Office").copy(
            latitude = home.latitude + 0.01,
        )
        val officeMovedOntoHome = office.copy(
            label = "Office",
            latitude = home.latitude,
            longitude = home.longitude,
        )
        val officeMovedAway = office.copy(
            latitude = home.latitude + 0.02,
            longitude = home.longitude,
        )
        assertEquals(
            home,
            duplicatePlaceForEditCommit(
                existingPlaces = listOf(home, office),
                editedPlaceId = office.id,
                candidate = officeMovedOntoHome,
            ),
        )
        assertEquals(
            null,
            duplicatePlaceForEditCommit(
                existingPlaces = listOf(home, office),
                editedPlaceId = office.id,
                candidate = officeMovedAway,
            ),
        )
        assertEquals(
            "Leave blank to use Office Tower.",
            placeNameSupportingText(
                placeName = "",
                fallbackLabel = pendingPlaceNameFallbackLabel(
                    previewPlaceName = "",
                    sourceLabel = "Office Tower",
                    targetPlaceLabel = "",
                    editingSelectedPlace = false,
                ),
            ),
        )
        assertEquals(
            "Gym",
            selectedZoneSyncLabel(
                typedPlaceLabel = "Gym",
                fallbackPlaceLabel = "Home",
            ),
        )
        assertEquals(
            "Home",
            selectedZoneSyncLabel(
                typedPlaceLabel = "",
                fallbackPlaceLabel = "Home",
            ),
        )
        assertEquals(
            "Selected place",
            selectedZoneSyncLabel(
                typedPlaceLabel = "",
                fallbackPlaceLabel = "",
            ),
        )
        assertEquals(
            270,
            selectedZoneSyncDurationMinutes(
                durationText = "4.5",
                fallbackPlaceDurationMinutes = 90,
                defaultDurationMinutes = 60,
            ),
        )
        assertEquals(
            90,
            selectedZoneSyncDurationMinutes(
                durationText = "nope",
                fallbackPlaceDurationMinutes = 90,
                defaultDurationMinutes = 60,
            ),
        )
        assertEquals(
            60,
            selectedZoneSyncDurationMinutes(
                durationText = "nope",
                fallbackPlaceDurationMinutes = null,
                defaultDurationMinutes = 60,
            ),
        )
    }

    @Test
    fun placesPreviewBannerMakesUnsavedStateExplicit() {
        assertEquals(
            "Unsaved place",
            placesPreviewBannerTitle(
                editingSelectedPlace = false,
                targetLabel = "",
            ),
        )
        assertEquals(
            "Unsaved move for Home",
            placesPreviewBannerTitle(
                editingSelectedPlace = true,
                targetLabel = "Home",
            ),
        )
        assertEquals(
            "Unsaved move for place",
            placesPreviewBannerTitle(
                editingSelectedPlace = true,
                targetLabel = "Selected place",
            ),
        )
        assertEquals(
            "Review it on the map to save it, or cancel it.",
            placesPreviewBannerDetail(editingSelectedPlace = false),
        )
        assertEquals(
            "Review the move on the map to save it, or cancel it.",
            placesPreviewBannerDetail(editingSelectedPlace = true),
        )
        assertEquals("Cancel preview", placesPreviewDiscardActionLabel(editingSelectedPlace = false))
        assertEquals("Cancel move", placesPreviewDiscardActionLabel(editingSelectedPlace = true))
        assertEquals(
            "Cancel preview",
            homeDockSecondaryActionLabel(
                timerActive = false,
                pendingPlacePreview = true,
                pendingPlaceMove = false,
            ),
        )
        assertEquals(
            "Cancel move",
            homeDockSecondaryActionLabel(
                timerActive = false,
                pendingPlacePreview = true,
                pendingPlaceMove = true,
            ),
        )
        assertEquals(
            "Cancel timer",
            homeDockSecondaryActionLabel(
                timerActive = true,
                pendingPlacePreview = false,
                pendingPlaceMove = false,
            ),
        )
        assertEquals(
            "Keep current",
            homeDockSecondaryActionLabel(
                promptSecondaryLabel = "Keep current",
                timerActive = true,
                pendingPlacePreview = true,
                pendingPlaceMove = true,
            ),
        )
        assertEquals(
            PlacesEmptyStateCopy(
                title = "Unsaved place waiting",
                detail = "Review it on the map or cancel it before adding another place.",
                actionLabel = "Review on map",
            ),
            placesEmptyStateCopy(hasPendingPlacePreview = true),
        )
        assertEquals(
            PlacesEmptyStateCopy(
                title = "No saved places",
                detail = "Add places from search, current location, or the map.",
                actionLabel = "Add place",
            ),
            placesEmptyStateCopy(hasPendingPlacePreview = false),
        )
        assertEquals("Review unsaved place", placesAddActionLabel(hasPendingPlacePreview = true))
        assertEquals("Add place", placesAddActionLabel(hasPendingPlacePreview = false))
        assertEquals(true, placesBackShouldExpandPendingPreview(hasPendingPlacePreview = true))
        assertEquals(false, placesBackShouldExpandPendingPreview(hasPendingPlacePreview = false))
        assertEquals(
            HomeBackAction.CloseSearch,
            homeBackAction(
                searchPanelExpanded = true,
                homeDockExpanded = true,
                hasPendingPlacePreview = true,
            ),
        )
        assertEquals(
            HomeBackAction.CollapseDock,
            homeBackAction(
                searchPanelExpanded = false,
                homeDockExpanded = true,
                hasPendingPlacePreview = true,
            ),
        )
        assertEquals(
            HomeBackAction.ExpandPendingPreview,
            homeBackAction(
                searchPanelExpanded = false,
                homeDockExpanded = false,
                hasPendingPlacePreview = true,
            ),
        )
        assertEquals(
            HomeBackAction.LetSystemHandle,
            homeBackAction(
                searchPanelExpanded = false,
                homeDockExpanded = false,
                hasPendingPlacePreview = false,
            ),
        )
        assertEquals(
            "No places monitoring arrivals",
            placesSummaryStatusText(monitoredCount = 0, liveCount = 0),
        )
        assertEquals(
            "1 place live",
            placesSummaryStatusText(monitoredCount = 1, liveCount = 1),
        )
        assertEquals(
            "2 places live",
            placesSummaryStatusText(monitoredCount = 2, liveCount = 2),
        )
        assertEquals(
            "1 place needs setup",
            placesSummaryStatusText(monitoredCount = 1, liveCount = 0),
        )
        assertEquals(
            "2 places live, 1 needs setup",
            placesSummaryStatusText(monitoredCount = 3, liveCount = 2),
        )
        assertEquals(
            "1 place live",
            placesSummaryStatusText(monitoredCount = 1, liveCount = 4),
        )
        assertEquals(
            null,
            placesSummaryPlaceNamesText(
                places = listOf(testPlace(id = "home", label = "Home", monitoringEnabled = false)),
                registeredPlaceIds = setOf("home"),
            ),
        )
        assertEquals(
            "Live: Home, Office",
            placesSummaryPlaceNamesText(
                places = listOf(
                    testPlace(id = "home", label = "Home"),
                    testPlace(id = "office", label = "Office"),
                    testPlace(id = "gym", label = "Gym", monitoringEnabled = false),
                ),
                registeredPlaceIds = setOf("home", "office"),
            ),
        )
        assertEquals(
            "Needs setup: Gym",
            placesSummaryPlaceNamesText(
                places = listOf(testPlace(id = "gym", label = "Gym")),
                registeredPlaceIds = emptySet(),
            ),
        )
        assertEquals(
            "Live: Home; needs setup: Gym",
            placesSummaryPlaceNamesText(
                places = listOf(
                    testPlace(id = "home", label = "Home"),
                    testPlace(id = "gym", label = "Gym"),
                ),
                registeredPlaceIds = setOf("home"),
            ),
        )
        assertEquals(
            "Live: Home, Office, and 1 more",
            placesSummaryPlaceNamesText(
                places = listOf(
                    testPlace(id = "home", label = "Home"),
                    testPlace(id = "office", label = "Office"),
                    testPlace(id = "gym", label = "Gym"),
                ),
                registeredPlaceIds = setOf("home", "office", "gym"),
                maxNamesPerGroup = 2,
            ),
        )
        assertEquals(
            "Not live",
            homeDockMonitoringMetaText(monitoredCount = 0, liveCount = 0),
        )
        assertEquals(
            "1 live",
            homeDockMonitoringMetaText(monitoredCount = 1, liveCount = 1),
        )
        assertEquals(
            "1 needs setup",
            homeDockMonitoringMetaText(monitoredCount = 1, liveCount = 0),
        )
        assertEquals(
            "2 need setup",
            homeDockMonitoringMetaText(monitoredCount = 2, liveCount = 0),
        )
        assertEquals(
            "1 live, 1 setup",
            homeDockMonitoringMetaText(monitoredCount = 2, liveCount = 1),
        )
        assertEquals(
            "1 live",
            homeDockMonitoringMetaText(monitoredCount = 1, liveCount = 4),
        )
        val limitMessage = GeofenceManager.monitoredPlaceLimitMessage()
        assertEquals(
            "Dwell can monitor up to ${DwellPlace.MAX_MONITORED_PLACES} places. Pause another monitored place first.",
            monitoringStartFailureMessage(limitMessage),
        )
        assertEquals(
            "Dwell can monitor up to ${DwellPlace.MAX_MONITORED_PLACES} places. Pause another place before monitoring Gym.",
            monitoringStartFailureMessage(limitMessage, "Gym"),
        )
        assertEquals(
            "Could not start monitoring: GPS unavailable",
            monitoringStartFailureMessage("GPS unavailable"),
        )
        assertEquals(
            "Could not monitor Office: GPS unavailable",
            monitoringStartFailureMessage("GPS unavailable", "Office"),
        )
        assertEquals(
            "Could not start monitoring: unknown error",
            monitoringStartFailureMessage(null),
        )
        assertEquals(
            "Could not monitor this place: unknown error",
            monitoringStartFailureMessage(null, "Saved place"),
        )
        assertEquals("Open Places", monitoringStartFailureActionLabel(limitMessage, alreadyOnPlaces = false))
        assertEquals(null, monitoringStartFailureActionLabel(limitMessage, alreadyOnPlaces = true))
        assertEquals(null, monitoringStartFailureActionLabel("GPS unavailable", alreadyOnPlaces = false))
    }

    @Test
    fun mapModeLabelsSeparateViewingFromMoving() {
        assertEquals(PlaceSelectionMode.ViewSelected, initialPlaceSelectionMode(hasSavedPlace = true))
        assertEquals(PlaceSelectionMode.CreateNew, initialPlaceSelectionMode(hasSavedPlace = false))
        assertEquals(
            "",
            initialEditingPlaceId(
                selectionMode = PlaceSelectionMode.ViewSelected,
                initialSelectedPlaceId = "home",
            ),
        )
        assertEquals(
            "home",
            initialEditingPlaceId(
                selectionMode = PlaceSelectionMode.EditSelected,
                initialSelectedPlaceId = "home",
            ),
        )
        assertEquals(
            SavedPlaceFocusState(
                selectionMode = PlaceSelectionMode.ViewSelected,
                selectedPlaceId = "home",
                viewingPlaceId = "home",
                editingPlaceId = "",
            ),
            savedPlaceFocusState(" home "),
        )
        assertEquals(
            PlaceSelectionMode.EditSelected,
            previewModeForMapPoint(
                selectionMode = PlaceSelectionMode.EditSelected,
                hasSelectedExistingPlace = true,
                forceCreateNew = false,
            ),
        )
        assertEquals(
            PlaceSelectionMode.CreateNew,
            previewModeForMapPoint(
                selectionMode = PlaceSelectionMode.EditSelected,
                hasSelectedExistingPlace = true,
                forceCreateNew = true,
            ),
        )
        assertEquals(
            PlaceSelectionMode.CreateNew,
            previewModeForMapPoint(
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasSelectedExistingPlace = true,
                forceCreateNew = longPressMapSelectionBehavior().forceCreateNew,
            ),
        )
        assertEquals(
            PlaceSelectionMode.CreateNew,
            previewModeForMapPoint(
                selectionMode = PlaceSelectionMode.EditSelected,
                hasSelectedExistingPlace = true,
                forceCreateNew = longPressMapSelectionBehavior().forceCreateNew,
            ),
        )
        assertEquals(
            true,
            shouldCarryPendingPreviewDraft(
                previousPreviewMode = PlaceSelectionMode.CreateNew,
                previewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            true,
            shouldCarryPendingPreviewDraft(
                previousPreviewMode = PlaceSelectionMode.EditSelected,
                previewMode = PlaceSelectionMode.EditSelected,
            ),
        )
        assertEquals(
            false,
            shouldCarryPendingPreviewDraft(
                previousPreviewMode = PlaceSelectionMode.EditSelected,
                previewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            true,
            shouldResetPreviewSettingsToDefaults(
                previousPreviewMode = null,
                previewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            true,
            shouldResetPreviewSettingsToDefaults(
                previousPreviewMode = PlaceSelectionMode.EditSelected,
                previewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            false,
            shouldResetPreviewSettingsToDefaults(
                previousPreviewMode = PlaceSelectionMode.CreateNew,
                previewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            false,
            shouldResetPreviewSettingsToDefaults(
                previousPreviewMode = PlaceSelectionMode.EditSelected,
                previewMode = PlaceSelectionMode.EditSelected,
            ),
        )
        assertEquals(
            false,
            pendingPreviewAutoStartForPointChange(
                existingPlaceAutoStart = false,
                previousPreviewAutoStart = true,
                carryPreviousDraft = false,
                defaultAutoStart = true,
            ),
        )
        assertEquals(
            false,
            pendingPreviewAutoStartForPointChange(
                existingPlaceAutoStart = null,
                previousPreviewAutoStart = false,
                carryPreviousDraft = true,
                defaultAutoStart = true,
            ),
        )
        assertEquals(
            true,
            pendingPreviewAutoStartForPointChange(
                existingPlaceAutoStart = null,
                previousPreviewAutoStart = false,
                carryPreviousDraft = false,
                defaultAutoStart = true,
            ),
        )
        assertEquals(
            false,
            pendingPreviewAutoStartForPointChange(
                existingPlaceAutoStart = null,
                previousPreviewAutoStart = false,
                carryPreviousDraft = false,
                defaultAutoStart = false,
            ),
        )
        assertEquals(
            false,
            pendingPreviewAutoStartForPointChange(
                existingPlaceAutoStart = null,
                previousPreviewAutoStart = null,
                carryPreviousDraft = true,
                defaultAutoStart = false,
            ),
        )
        assertEquals(
            PendingPreviewReturnFocus(
                selectionMode = PlaceSelectionMode.ViewSelected,
                placeId = "home",
            ),
            pendingPreviewReturnFocus(
                selectionMode = PlaceSelectionMode.ViewSelected,
                selectedPlaceId = "home",
                viewingPlaceId = " home ",
                editingPlaceId = "",
            ),
        )
        assertEquals(
            PendingPreviewReturnFocus(
                selectionMode = PlaceSelectionMode.EditSelected,
                placeId = "office",
            ),
            pendingPreviewReturnFocus(
                selectionMode = PlaceSelectionMode.EditSelected,
                selectedPlaceId = "home",
                viewingPlaceId = "home",
                editingPlaceId = "office",
            ),
        )
        assertEquals(
            null,
            pendingPreviewReturnFocus(
                selectionMode = PlaceSelectionMode.CreateNew,
                selectedPlaceId = "home",
                viewingPlaceId = "home",
                editingPlaceId = "",
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.PendingPreview,
            settingsPersistenceTarget(
                hasPendingPlacePreview = true,
                selectionMode = PlaceSelectionMode.EditSelected,
                hasEditingPlace = true,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.EditingPlace,
            settingsPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.EditSelected,
                hasEditingPlace = true,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.ReadOnlyPlace,
            settingsPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.EditSelected,
                hasEditingPlace = false,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.ReadOnlyPlace,
            settingsPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasEditingPlace = true,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.DefaultSettings,
            settingsPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.CreateNew,
                hasEditingPlace = false,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.PendingPreview,
            durationPresetPersistenceTarget(
                hasPendingPlacePreview = true,
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasSettingsPlace = false,
                hasSelectedPlace = true,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.ReadOnlyPlace,
            durationPresetPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasSettingsPlace = false,
                hasSelectedPlace = true,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.EditingPlace,
            durationPresetPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.EditSelected,
                hasSettingsPlace = true,
                hasSelectedPlace = true,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.DefaultSettings,
            durationPresetPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.CreateNew,
                hasSettingsPlace = false,
                hasSelectedPlace = false,
            ),
        )
        assertEquals(
            SettingsPersistenceTarget.ReadOnlyPlace,
            durationPresetPersistenceTarget(
                hasPendingPlacePreview = false,
                selectionMode = PlaceSelectionMode.EditSelected,
                hasSettingsPlace = false,
                hasSelectedPlace = false,
            ),
        )
        assertEquals(false, settingsLocalChangeAllowed(SettingsPersistenceTarget.ReadOnlyPlace))
        assertEquals(true, settingsLocalChangeAllowed(SettingsPersistenceTarget.PendingPreview))
        assertEquals(true, settingsLocalChangeAllowed(SettingsPersistenceTarget.EditingPlace))
        assertEquals(true, settingsLocalChangeAllowed(SettingsPersistenceTarget.DefaultSettings))
        assertEquals("Move Home", placeModeMoveLabel("Home"))
        assertEquals("Viewing Home", placeModeViewLabel("Home"))
        assertEquals("Move place", placeModeMoveLabel("No place selected"))
        assertEquals("Move place", placeModeMoveLabel("Selected place"))
        assertEquals("Viewing place", placeModeViewLabel("Saved place"))
        assertEquals("Viewing place", placeModeViewLabel(""))
        assertEquals(
            "Move Home",
            placeModePrimaryLabel(
                placeLabel = "Home",
                addingNewPlace = true,
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Move Home",
            placeModePrimaryLabel(
                placeLabel = "Home",
                addingNewPlace = false,
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Viewing Home",
            placeModePrimaryLabel(
                placeLabel = "Home",
                addingNewPlace = false,
                editingSelectedPlace = false,
            ),
        )
        assertEquals(true, savedPlaceModeChipSelected(addingNewPlace = false))
        assertEquals(false, savedPlaceModeChipSelected(addingNewPlace = true))
        assertEquals(true, savedPlaceModeChipCanSwitchToEdit(addingNewPlace = true))
        assertEquals(false, savedPlaceModeChipCanSwitchToEdit(addingNewPlace = false))
        assertEquals("Add place", mapSearchBlockedDestinationLabel(PlaceSelectionMode.CreateNew))
        assertEquals("Move place", mapSearchBlockedDestinationLabel(PlaceSelectionMode.EditSelected))
        assertEquals("View map", mapSearchBlockedDestinationLabel(PlaceSelectionMode.ViewSelected))
        assertEquals(
            "Tap Edit settings to change Home.",
            viewingOnlyNoticeDetail("Home"),
        )
        assertEquals(
            true,
            isViewingSavedPlaceReadOnly(
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasPendingPlacePreview = false,
                hasSavedPlaceSelected = true,
            ),
        )
        assertEquals(
            false,
            isViewingSavedPlaceReadOnly(
                selectionMode = PlaceSelectionMode.EditSelected,
                hasPendingPlacePreview = false,
                hasSavedPlaceSelected = true,
            ),
        )
        assertEquals(
            false,
            isViewingSavedPlaceReadOnly(
                selectionMode = PlaceSelectionMode.ViewSelected,
                hasPendingPlacePreview = true,
                hasSavedPlaceSelected = true,
            ),
        )
        assertEquals("Editing Home", editingPlaceStatusTitle("Home"))
        assertEquals(
            "Review radius, timer, and arrival mode before monitoring.",
            editingPlaceStatusDetail(monitoringEnabled = false),
        )
        assertEquals(
            "Review settings. Pause monitoring before moving this place or increasing radius.",
            editingPlaceStatusDetail(monitoringEnabled = true),
        )
        assertEquals(
            "Save or cancel the preview first",
            pendingPreviewMutationBlockedMessage(),
        )
        assertEquals(
            "Move or cancel the preview before opening Settings.",
            pendingPreviewMutationBlockedMessage(
                destinationLabel = "Settings",
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            "Save or cancel the preview before opening Settings.",
            pendingPreviewMutationBlockedMessage("Settings"),
        )
        assertEquals(
            "Save or cancel the preview before opening Insights.",
            pendingPreviewMutationBlockedMessage("Insights"),
        )
        assertEquals(
            "Save or cancel the preview before opening Add place.",
            pendingPreviewMutationBlockedMessage("Add place"),
        )
        assertEquals(
            "Save or cancel the preview before opening View map.",
            pendingPreviewMutationBlockedMessage("View map"),
        )
        assertEquals(
            "Save or cancel the preview before opening Edit settings.",
            pendingPreviewMutationBlockedMessage("Edit settings"),
        )
        assertEquals(
            "Save or cancel the preview before opening Move place.",
            pendingPreviewMutationBlockedMessage("Move place"),
        )
        assertEquals(true, shouldBlockMapModeSwitch(hasPendingPlacePreview = true))
        assertEquals(false, shouldBlockMapModeSwitch(hasPendingPlacePreview = false))
        assertEquals(
            false,
            shouldBlockMapPointSelection(
                pendingPreviewMode = null,
                nextPreviewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            false,
            shouldBlockMapPointSelection(
                pendingPreviewMode = PlaceSelectionMode.CreateNew,
                nextPreviewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(
            false,
            shouldBlockMapPointSelection(
                pendingPreviewMode = PlaceSelectionMode.EditSelected,
                nextPreviewMode = PlaceSelectionMode.EditSelected,
            ),
        )
        assertEquals(
            true,
            shouldBlockMapPointSelection(
                pendingPreviewMode = PlaceSelectionMode.EditSelected,
                nextPreviewMode = PlaceSelectionMode.CreateNew,
            ),
        )
        assertEquals(false, placesRowMutationEnabled(hasPendingPlacePreview = true))
        assertEquals(true, placesRowMutationEnabled(hasPendingPlacePreview = false))
        assertEquals(
            "Save or cancel the preview before changing saved rows or setup.",
            placesRowMutationLockDetail(hasPendingPlacePreview = true),
        )
        assertEquals(
            "Move or cancel the preview before changing saved rows or setup.",
            placesRowMutationLockDetail(
                hasPendingPlacePreview = true,
                editingSelectedPlace = true,
            ),
        )
        assertEquals(null, placesRowMutationLockDetail(hasPendingPlacePreview = false))
        assertEquals(
            PlacesRowActionAvailability(
                viewMapEnabled = false,
                editSettingsEnabled = false,
                monitoringToggleEnabled = false,
                startNowEnabled = false,
                removeEnabled = false,
                setupRecoveryEnabled = false,
                lockDetail = "Save or cancel the preview before changing saved rows or setup.",
            ),
            placesRowActionAvailability(hasPendingPlacePreview = true),
        )
        assertEquals(
            PlacesRowActionAvailability(
                viewMapEnabled = false,
                editSettingsEnabled = false,
                monitoringToggleEnabled = false,
                startNowEnabled = false,
                removeEnabled = false,
                setupRecoveryEnabled = false,
                lockDetail = "Move or cancel the preview before changing saved rows or setup.",
            ),
            placesRowActionAvailability(
                hasPendingPlacePreview = true,
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            PlacesRowActionAvailability(
                viewMapEnabled = true,
                editSettingsEnabled = true,
                monitoringToggleEnabled = true,
                startNowEnabled = true,
                removeEnabled = true,
                setupRecoveryEnabled = true,
                lockDetail = null,
            ),
            placesRowActionAvailability(hasPendingPlacePreview = false),
        )
        assertEquals(
            false,
            placesRowActionAvailability(
                hasPendingPlacePreview = false,
                timerActive = true,
            ).startNowEnabled,
        )
        assertEquals(
            PlacesRowTimerAction(
                label = "Start now",
                enabled = true,
                cancelTimer = false,
            ),
            placesRowTimerAction(
                isTimerPlace = false,
                actionAvailability = placesRowActionAvailability(hasPendingPlacePreview = false),
            ),
        )
        assertEquals(
            PlacesRowTimerAction(
                label = "Start now",
                enabled = false,
                cancelTimer = false,
                detail = "Cancel the Office timer before starting another place.",
            ),
            placesRowTimerAction(
                isTimerPlace = false,
                actionAvailability = placesRowActionAvailability(
                    hasPendingPlacePreview = false,
                    timerActive = true,
                ),
                timerPlaceLabel = "Office",
            ),
        )
        assertEquals(
            "Cancel the Office timer before starting another place.",
            placesRowActiveTimerBlockDetail("Office"),
        )
        assertEquals(
            "Cancel the running timer before starting another place.",
            placesRowActiveTimerBlockDetail(),
        )
        assertEquals(
            PlacesRowTimerAction(
                label = "Cancel timer",
                enabled = true,
                cancelTimer = true,
            ),
            placesRowTimerAction(
                isTimerPlace = true,
                actionAvailability = placesRowActionAvailability(
                    hasPendingPlacePreview = true,
                    timerActive = true,
                ),
            ),
        )
        assertEquals(false, placesRowSetupRecoveryEnabled(hasPendingPlacePreview = true))
        assertEquals(true, placesRowSetupRecoveryEnabled(hasPendingPlacePreview = false))
        assertEquals(
            null,
            dockSetupIssue(
                setupIssue = "Background location permission is needed",
                activePlaceArmed = false,
                hasPendingPlacePreview = false,
            ),
        )
        assertEquals(
            null,
            dockSetupIssue(
                setupIssue = "Background location permission is needed",
                activePlaceArmed = true,
                hasPendingPlacePreview = true,
            ),
        )
        assertEquals(
            "Background location permission is needed",
            dockSetupIssue(
                setupIssue = "Background location permission is needed",
                activePlaceArmed = true,
                hasPendingPlacePreview = false,
            ),
        )
        assertEquals(
            "Monitoring needs setup",
            homeSetupStatusTitle(
                activePlaceNeedsSetup = true,
                hasActivePlaceSetupIssue = true,
            ),
        )
        assertEquals(
            "Needs setup",
            homeSetupStatusTitle(
                activePlaceNeedsSetup = false,
                hasActivePlaceSetupIssue = true,
            ),
        )
        assertEquals(
            "",
            homeSetupStatusTitle(
                activePlaceNeedsSetup = false,
                hasActivePlaceSetupIssue = false,
            ),
        )
    }

    @Test
    fun deletingLegacyActivePlaceDoesNotCountAsOpenPlaceDeletion() {
        assertEquals(
            true,
            isOpenPlaceDeletion(
                deletedPlaceId = "home",
                selectedPlaceId = "home",
                viewingPlaceId = "home",
                editingPlaceId = "",
            ),
        )
        assertEquals(
            true,
            isOpenPlaceDeletion(
                deletedPlaceId = "home",
                selectedPlaceId = "office",
                viewingPlaceId = "office",
                editingPlaceId = "home",
            ),
        )
        assertEquals(
            false,
            isOpenPlaceDeletion(
                deletedPlaceId = "home",
                selectedPlaceId = "office",
                viewingPlaceId = "office",
                editingPlaceId = "",
            ),
        )
    }

    @Test
    fun deletingAPlaceFromPlacesKeepsThePlacesRoute() {
        assertEquals(
            AppRoute.SavedZones,
            routeAfterSavedPlaceDeletion(AppRoute.SavedZones),
        )
        assertEquals(
            AppRoute.Home,
            routeAfterSavedPlaceDeletion(AppRoute.Home),
        )
    }

    @Test
    fun deletingAPlaceUsesALocalFocusTransitionWithoutKickingUserOutOfPlaces() {
        val cases = listOf(
            DeletionTransitionCase(
                name = "places non-open with next",
                route = AppRoute.SavedZones,
                deletedPlaceWasOpen = false,
                nextPlaceId = "office",
                expected = SavedPlaceDeletionUiTransition(
                    route = AppRoute.SavedZones,
                    focusAction = SavedPlaceDeletionFocusAction.PreserveCurrentPlace,
                    nextPlaceId = null,
                ),
            ),
            DeletionTransitionCase(
                name = "places open with next",
                route = AppRoute.SavedZones,
                deletedPlaceWasOpen = true,
                nextPlaceId = "office",
                expected = SavedPlaceDeletionUiTransition(
                    route = AppRoute.SavedZones,
                    focusAction = SavedPlaceDeletionFocusAction.FocusNextPlace,
                    nextPlaceId = "office",
                ),
            ),
            DeletionTransitionCase(
                name = "places open last place",
                route = AppRoute.SavedZones,
                deletedPlaceWasOpen = true,
                nextPlaceId = null,
                expected = SavedPlaceDeletionUiTransition(
                    route = AppRoute.SavedZones,
                    focusAction = SavedPlaceDeletionFocusAction.ClearOpenPlace,
                    nextPlaceId = null,
                ),
            ),
            DeletionTransitionCase(
                name = "home open last place",
                route = AppRoute.Home,
                deletedPlaceWasOpen = true,
                nextPlaceId = "",
                expected = SavedPlaceDeletionUiTransition(
                    route = AppRoute.Home,
                    focusAction = SavedPlaceDeletionFocusAction.ClearOpenPlace,
                    nextPlaceId = null,
                ),
            ),
        )

        cases.forEach { case ->
            assertEquals(
                case.name,
                case.expected,
                savedPlaceDeletionUiTransition(
                    currentRoute = case.route,
                    deletedPlaceWasOpen = case.deletedPlaceWasOpen,
                    nextPlaceId = case.nextPlaceId,
                ),
            )
        }
    }

    @Test
    fun undoDeleteOnlyRestoresMapFocusForTheDeletedOpenPlace() {
        assertEquals(
            false,
            shouldRestoreDeletedPlaceFocusOnUndo(deletedPlaceWasOpen = false),
        )
        assertEquals(
            true,
            shouldRestoreDeletedPlaceFocusOnUndo(deletedPlaceWasOpen = true),
        )
    }

    @Test
    fun placesBackDismissesDeleteDialogBeforeLeavingTheList() {
        assertEquals(
            PlacesBackAction.DismissDeleteDialog,
            placesBackAction(
                deleteDialogVisible = true,
            ),
        )
        assertEquals(
            PlacesBackAction.DismissMonitorDialog,
            placesBackAction(
                monitorDialogVisible = true,
                deleteDialogVisible = true,
            ),
        )
        assertEquals(
            PlacesBackAction.DismissMonitorDialog,
            placesBackAction(
                monitorDialogVisible = true,
                deleteDialogVisible = false,
            ),
        )
        assertEquals(
            PlacesBackAction.LeavePlaces,
            placesBackAction(
                deleteDialogVisible = false,
            ),
        )
    }

    @Test
    fun monitorConfirmationExplainsThePlaceSettingsBeforeGoingLive() {
        val staleOffice = testPlace(
            id = "office",
            label = "Office",
            monitoringEnabled = false,
        ).copy(
            radiusMeters = 100f,
            durationMinutes = 45,
            autoStart = true,
        )
        val latestOffice = staleOffice.copy(
            radiusMeters = 50f,
            durationMinutes = 60,
            autoStart = false,
            updatedAtMillis = staleOffice.updatedAtMillis + 1L,
        )

        assertEquals(latestOffice, latestDialogPlace("office", listOf(latestOffice)))
        assertEquals(null, latestDialogPlace("office", emptyList()))
        assertEquals(null, latestDialogPlace("", listOf(latestOffice)))
        assertEquals(
            PlacesMonitoringConfirmationCopy(
                title = "Monitor Office?",
                detail = "Dwell will monitor Office with a 50 m radius, 1h timer, and Confirm first arrival mode.",
                confirmLabel = "Start monitoring",
                dismissLabel = "Not now",
            ),
            placesMonitoringConfirmationCopy(
                latestDialogPlace("office", listOf(latestOffice)) ?: staleOffice,
            ),
        )
    }

    @Test
    fun placeMonitoringStatusLabelsSeparateTimerLiveSetupAndPaused() {
        assertEquals(
            "Timer here",
            placeMonitoringStatusLabel(
                monitoringEnabled = true,
                isRegistered = true,
                isTimerPlace = true,
            ),
        )
        assertEquals(
            "Monitoring live",
            placeMonitoringStatusLabel(
                monitoringEnabled = true,
                isRegistered = true,
                isTimerPlace = false,
            ),
        )
        assertEquals(
            "Needs setup",
            placeMonitoringStatusLabel(
                monitoringEnabled = true,
                isRegistered = false,
                isTimerPlace = false,
            ),
        )
        assertEquals("Fix setup", placeSetupActionLabel())
        assertEquals(
            true,
            placeNeedsMonitoringSetup(
                monitoringEnabled = true,
                isRegistered = false,
            ),
        )
        assertEquals(
            false,
            placeNeedsMonitoringSetup(
                monitoringEnabled = true,
                isRegistered = true,
            ),
        )
        assertEquals(
            false,
            placeNeedsMonitoringSetup(
                monitoringEnabled = false,
                isRegistered = false,
            ),
        )
        assertEquals(
            "Paused",
            placeMonitoringStatusLabel(
                monitoringEnabled = false,
                isRegistered = false,
                isTimerPlace = false,
            ),
        )
    }

    @Test
    fun placeRoleLabelsSeparateViewingEditingAndTimerPlace() {
        assertEquals(
            listOf("Viewing"),
            placeRoleLabels(
                isViewing = true,
                isEditing = false,
                isTimerPlace = false,
            ),
        )
        assertEquals(
            listOf("Editing"),
            placeRoleLabels(
                isViewing = true,
                isEditing = true,
                isTimerPlace = false,
            ),
        )
        assertEquals(
            listOf("Timer here", "Viewing"),
            placeRoleLabels(
                isViewing = true,
                isEditing = false,
                isTimerPlace = true,
            ),
        )
        assertEquals(
            listOf("Timer here", "Editing"),
            placeRoleLabels(
                isViewing = true,
                isEditing = true,
                isTimerPlace = true,
            ),
        )
        assertEquals(
            listOf("Timer here"),
            placeRoleLabels(
                isViewing = false,
                isEditing = false,
                isTimerPlace = true,
            ),
        )
        assertEquals(
            emptyList<String>(),
            placeRoleLabels(
                isViewing = false,
                isEditing = false,
                isTimerPlace = false,
            ),
        )
    }

    @Test
    fun destructiveActionCopyNamesThePlaceAndSideEffects() {
        assertEquals("Cancel timer for Home?", timerCancelTitle("Home"))
        assertEquals("Cancel timer?", timerCancelTitle(""))
        assertEquals("Cancel timer?", timerCancelTitle("Saved place"))
        assertEquals("Remove Gym?", placeRemovalTitle("Gym"))
        assertEquals("Remove place?", placeRemovalTitle("No place selected"))
        assertEquals(
            "This removes Gym from Places. Arrival monitoring for Gym stops. The running timer here will be canceled.",
            placeRemovalDetail(
                placeLabel = "Gym",
                monitoringEnabled = true,
                isTimerPlace = true,
            ),
        )
        assertEquals(
            "This removes this place from Places.",
            placeRemovalDetail(
                placeLabel = "",
                monitoringEnabled = false,
                isTimerPlace = false,
            ),
        )
        assertEquals(
            "This removes this place from Places. Arrival monitoring for this place stops.",
            placeRemovalDetail(
                placeLabel = "Selected place",
                monitoringEnabled = true,
                isTimerPlace = false,
            ),
        )
        assertEquals("Gym removed from Places", placeRemovedMessage("Gym"))
        assertEquals(
            PlaceDeleteUndoSnackbarPlan(
                message = "Gym removed from Places",
                actionLabel = "Undo",
                dismissCurrentSnackbar = true,
            ),
            placeDeleteUndoSnackbarPlan(
                placeLabel = "Gym",
                currentSnackbarVisible = true,
            ),
        )
        assertEquals(
            PlaceDeleteUndoSnackbarPlan(
                message = "Place removed from Places",
                actionLabel = "Undo",
                dismissCurrentSnackbar = false,
            ),
            placeDeleteUndoSnackbarPlan(
                placeLabel = "Saved place",
                currentSnackbarVisible = false,
            ),
        )
        assertEquals("Gym restored to Places", placeRestoredMessage("Gym"))
        assertEquals(
            "Gym restored and selected",
            placeRestoredMessage("Gym", focusRestored = true),
        )
        assertEquals(
            "Gym restored to Places. Monitoring is paused because the live-place limit is full.",
            placeRestoredMessage("Gym", monitoringPausedByLimit = true),
        )
        assertEquals("Place removed from Places", placeRemovedMessage("Saved place"))
        assertEquals("Place restored to Places", placeRestoredMessage("No place selected"))
        assertEquals(
            true,
            placeRestorePausedByMonitoringLimit(
                deletedPlaceWasMonitoring = true,
                restoredPlaceMonitoring = false,
            ),
        )
        assertEquals(
            false,
            placeRestorePausedByMonitoringLimit(
                deletedPlaceWasMonitoring = false,
                restoredPlaceMonitoring = false,
            ),
        )
        assertEquals(
            false,
            placeRestorePausedByMonitoringLimit(
                deletedPlaceWasMonitoring = true,
                restoredPlaceMonitoring = true,
            ),
        )
        assertEquals(
            "Gym paused. Other monitored places stay live.",
            placePausedMessage("Gym"),
        )
        assertEquals(
            "Place paused. Other monitored places stay live.",
            placePausedMessage("Selected place"),
        )
        assertEquals(
            "Could not pause Gym: geofence failed",
            monitoringPauseFailureMessage("Gym", "geofence failed"),
        )
        assertEquals(
            "Could not pause this place: unknown error",
            monitoringPauseFailureMessage("Saved place", null),
        )
    }

    @Test
    fun monitoringHealthExplainsHealthyLiveState() {
        val health = monitoringHealthState(
            placesCount = 2,
            monitoredCount = 2,
            liveCount = 2,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )

        assertEquals("Monitoring live", health.title)
        assertEquals("Healthy", health.stateLabel)
        assertEquals(true, health.healthy)
        assertEquals(MonitoringHealthAction.None, health.action)
    }

    @Test
    fun monitoringHealthPrioritizesSetupIssues() {
        val health = monitoringHealthState(
            placesCount = 1,
            monitoredCount = 1,
            liveCount = 0,
            setupIssue = "Background location permission is needed",
            monitoringError = "Monitoring setup failed",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )

        assertEquals("Monitoring needs setup", health.title)
        assertEquals("Background location permission is needed", health.detail)
        assertEquals("Needs setup", health.stateLabel)
        assertEquals("Finish setup", monitoringSetupActionLabel())
        assertEquals("Finish setup", health.actionLabel)
        assertEquals(MonitoringHealthAction.OpenSettings, health.action)
        assertEquals(
            false,
            monitoringHealthActionEnabled(
                action = health.action,
                hasPendingPlacePreview = true,
            ),
        )
    }

    @Test
    fun monitoringHealthExplainsPartialRegistration() {
        val noneLiveHealth = monitoringHealthState(
            placesCount = 3,
            monitoredCount = 3,
            liveCount = 0,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )
        assertEquals("Monitoring needs setup", noneLiveHealth.title)
        assertEquals(
            "3 places enabled, but none are live yet. Tap Finish setup to restore arrival detection.",
            noneLiveHealth.detail,
        )
        assertEquals("Needs setup", noneLiveHealth.stateLabel)
        assertEquals("Finish setup", noneLiveHealth.actionLabel)
        assertEquals(MonitoringHealthAction.RefreshMonitoring, noneLiveHealth.action)

        val health = monitoringHealthState(
            placesCount = 3,
            monitoredCount = 3,
            liveCount = 1,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )

        assertEquals("Some places need setup", health.title)
        assertEquals(
            "1 place live; 2 places need setup. Tap Finish setup to restore arrival detection.",
            health.detail,
        )
        assertEquals("Partial", health.stateLabel)
        assertEquals("Finish setup", health.actionLabel)
        assertEquals(MonitoringHealthAction.RefreshMonitoring, health.action)
    }

    @Test
    fun monitoringHealthTreatsAutoPausedMonitorLimitAsAPlacesLimit() {
        val health = monitoringHealthState(
            placesCount = DwellPlace.MAX_MONITORED_PLACES + 3,
            monitoredCount = DwellPlace.MAX_MONITORED_PLACES,
            liveCount = DwellPlace.MAX_MONITORED_PLACES,
            setupIssue = null,
            monitoringError = Prefs.monitoringLimitNormalizationMessage(3),
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )

        assertEquals("Some places paused", health.title)
        assertEquals("Limit", health.stateLabel)
        assertEquals(
            "Dwell paused 3 extra monitored places because the live monitoring limit is ${DwellPlace.MAX_MONITORED_PLACES}. Pause other monitored places before turning them back on.",
            health.detail,
        )
        assertEquals("", health.actionLabel)
        assertEquals(MonitoringHealthAction.None, health.action)
    }

    @Test
    fun monitoringHealthExplainsTimerAndBatteryRisks() {
        val exactAlarmRisk = monitoringHealthState(
            placesCount = 1,
            monitoredCount = 1,
            liveCount = 1,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = false,
            batteryReliabilityStatus = batteryStatus(),
        )
        assertEquals("Timers may be delayed", exactAlarmRisk.title)
        assertEquals("Allow alarms", timerRiskActionLabel())
        assertEquals("Allow alarms", exactAlarmRisk.actionLabel)
        assertEquals(MonitoringHealthAction.OpenExactAlarm, exactAlarmRisk.action)

        val optimizedNormalDevice = monitoringHealthState(
            placesCount = 1,
            monitoredCount = 1,
            liveCount = 1,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(
                isKnownAggressiveOem = false,
                isIgnoringOptimizations = false,
            ),
        )
        assertEquals("Monitoring live", optimizedNormalDevice.title)
        assertEquals(true, optimizedNormalDevice.healthy)
        assertEquals(MonitoringHealthAction.None, optimizedNormalDevice.action)

        val batteryRisk = monitoringHealthState(
            placesCount = 1,
            monitoredCount = 1,
            liveCount = 1,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(
                isKnownAggressiveOem = true,
                isIgnoringOptimizations = false,
            ),
        )
        assertEquals("Monitoring live, battery may delay", batteryRisk.title)
        assertEquals("Review battery", batteryRiskActionLabel())
        assertEquals("Review battery", batteryRisk.actionLabel)
        assertEquals(MonitoringHealthAction.OpenBattery, batteryRisk.action)
        assertEquals(
            false,
            monitoringHealthActionEnabled(
                action = batteryRisk.action,
                hasPendingPlacePreview = true,
            ),
        )
        assertEquals(
            "Save or cancel the preview before using Review battery.",
            monitoringHealthActionDisabledDetail(
                actionLabel = batteryRisk.actionLabel,
                hasPendingPlacePreview = true,
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            "Move or cancel the preview before using Finish setup.",
            monitoringHealthActionDisabledDetail(
                actionLabel = monitoringSetupActionLabel(),
                hasPendingPlacePreview = true,
                editingSelectedPlace = true,
            ),
        )
        assertEquals(
            null,
            monitoringHealthActionDisabledDetail(
                actionLabel = "Allow alarms",
                hasPendingPlacePreview = false,
                editingSelectedPlace = false,
            ),
        )
        assertEquals(
            false,
            monitoringHealthActionEnabled(
                action = MonitoringHealthAction.None,
                hasPendingPlacePreview = true,
            ),
        )
    }

    @Test
    fun longRecoverySnackbarsNameTheSettingsDestination() {
        assertEquals(
            "Allow all-the-time location so Dwell can detect arrivals after you leave the app.",
            backgroundLocationHelpMessage(),
        )
        assertEquals("Open app settings", backgroundLocationHelpActionLabel())

        assertEquals(
            "Test may delay background arrivals. Open app info, then Battery, and choose Unrestricted.",
            batteryHelpMessage(
                batteryStatus(
                    isKnownAggressiveOem = true,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals(
            "Android may delay background arrivals while battery optimization is enabled. Open app info, then Battery, and choose Unrestricted.",
            batteryHelpMessage(
                batteryStatus(
                    isKnownAggressiveOem = false,
                    isIgnoringOptimizations = false,
                ),
            ),
        )
        assertEquals("Open app info", batteryHelpActionLabel())
    }

    @Test
    fun longRecoverySnackbarsDoNotQueueIdenticalMessages() {
        val key = longActionMessageKey(
            message = backgroundLocationHelpMessage(),
            actionLabel = backgroundLocationHelpActionLabel(),
        )
        assertEquals(
            "Allow all-the-time location so Dwell can detect arrivals after you leave the app.|Open app settings",
            key,
        )
        assertEquals(
            false,
            shouldEnqueueLongActionMessage(
                activeLongActionKey = key,
                nextLongActionKey = key,
            ),
        )
        assertEquals(
            true,
            shouldEnqueueLongActionMessage(
                activeLongActionKey = key,
                nextLongActionKey = longActionMessageKey(
                    message = "Different recovery",
                    actionLabel = "Open settings",
                ),
            ),
        )
        assertEquals(
            true,
            shouldEnqueueLongActionMessage(
                activeLongActionKey = null,
                nextLongActionKey = key,
            ),
        )
    }

    @Test
    fun deleteAppDataCopyKeepsLocalCleanupIndependentFromServerCleanup() {
        assertEquals("App data deleted", appDataDeletedMessage())
        assertEquals(
            "Server cleanup did not confirm. Local data is deleted.",
            appDataServerCleanupFailedMessage(),
        )
        assertEquals(
            AppDataClearUiReset(
                onboardingComplete = false,
                route = AppRoute.Home,
                placeSelectionMode = PlaceSelectionMode.CreateNew,
                selectedPlaceId = "",
                viewingPlaceId = "",
                editingPlaceId = "",
                selectedPlaceLabel = "",
                timerEndMillis = 0L,
                dismissCurrentSnackbar = true,
                closeSearchPanel = true,
                clearPendingMonitoringResume = true,
                clearPendingLocationResume = true,
            ),
            appDataClearUiReset(),
        )
    }

    private fun batteryStatus(
        isKnownAggressiveOem: Boolean = false,
        isIgnoringOptimizations: Boolean = true,
    ): BatteryReliabilityStatus =
        BatteryReliabilityStatus(
            manufacturer = "Test",
            isKnownAggressiveOem = isKnownAggressiveOem,
            isIgnoringOptimizations = isIgnoringOptimizations,
        )

    private data class DeletionTransitionCase(
        val name: String,
        val route: AppRoute,
        val deletedPlaceWasOpen: Boolean,
        val nextPlaceId: String?,
        val expected: SavedPlaceDeletionUiTransition,
    )

    private fun testPlace(
        id: String,
        label: String = id,
        monitoringEnabled: Boolean = true,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = label,
            latitude = 17.0,
            longitude = 78.0,
            radiusMeters = DwellRadius.DEFAULT_METERS,
            durationMinutes = 270,
            monitoringEnabled = monitoringEnabled,
            autoStart = true,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
}
