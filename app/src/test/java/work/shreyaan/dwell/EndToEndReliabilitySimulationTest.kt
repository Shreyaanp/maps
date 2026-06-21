package work.shreyaan.dwell

import com.google.android.gms.location.Geofence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndToEndReliabilitySimulationTest {
    private val home = place("home", "Home", latitude = 17.0000, longitude = 78.0000, durationMinutes = 45)
    private val office = place("office", "Office", latitude = 17.0100, longitude = 78.0000, durationMinutes = 90)
    private val gym = place("gym", "Gym", latitude = 17.0140, longitude = 78.0000, durationMinutes = 60)
    private val storyPlaces = listOf(home, office, gym)

    @Test
    fun homeOfficeGymMovementKeepsPlaceRolesExplicitAcrossPhoneAndWatch() {
        val homeRequests = GeofenceReceiver.inferredRequestsForLocation(
            places = storyPlaces,
            latitude = home.latitude,
            longitude = home.longitude,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )

        assertEquals(listOf(DwellGeofenceRequest("home", DwellGeofenceType.ZONE)), homeRequests)
        assertEquals(
            HomePromptState(
                kind = HomePromptKind.Arrival,
                title = "Start timer at Home?",
                detail = "45m timer for Home.",
                placeLabel = "Home",
                primaryLabel = "Start",
                secondaryLabel = "Not now",
            ),
            homePromptState(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceLabel = home.safeLabel,
                timerRunning = false,
                timerPlaceLabel = "",
                durationMinutes = home.durationMinutes,
                timerEnd = 0L,
                now = 1_000L,
            ),
        )

        assertEquals(
            "office",
            WearSync.watchDisplayPlace(
                places = storyPlaces,
                activePlace = home,
                armedPlaces = storyPlaces,
                registeredPlaceIds = storyPlaces.map { it.id }.toSet(),
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerEnd = 120_000L,
                timerPlaceId = "office",
            )?.id,
        )

        val gymRequests = GeofenceReceiver.inferredRequestsForLocation(
            places = storyPlaces,
            latitude = gym.latitude,
            longitude = gym.longitude,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )
        val gymZonePlaces = placesForRequests(storyPlaces, gymRequests)

        assertEquals("gym", gymZonePlaces.first().id)
        assertEquals(
            "gym",
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = gymZonePlaces,
                currentPlaceId = "office",
            )?.id,
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
                promptPlaceLabel = gym.safeLabel,
                timerRunning = true,
                timerPlaceLabel = office.safeLabel,
                durationMinutes = gym.durationMinutes,
                timerEnd = 120_000L,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun everyHomeOfficeGymArrivalInfersThatPlaceAndUsesThatTimerDuration() {
        storyPlaces.forEach { arrived ->
            val requests = GeofenceReceiver.inferredRequestsForLocation(
                places = storyPlaces.shuffledForStablePermutation(),
                latitude = arrived.latitude,
                longitude = arrived.longitude,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            )

            assertEquals(
                "arrival should resolve to ${arrived.id}",
                listOf(DwellGeofenceRequest(arrived.id, DwellGeofenceType.ZONE)),
                requests,
            )
            assertEquals(
                "${Notifications.formatDuration(arrived.durationMinutes)} timer for ${arrived.safeLabel}.",
                homePromptState(
                    prompt = Prefs.WATCH_PROMPT_START_TIMER,
                    promptPlaceLabel = arrived.safeLabel,
                    timerRunning = false,
                    timerPlaceLabel = "",
                    durationMinutes = arrived.durationMinutes,
                    timerEnd = 0L,
                    now = 1_000L,
                )?.detail,
            )
        }
    }

    @Test
    fun everyHomeOfficeGymSwitchPermutationNamesTheTargetAndCurrentTimerPlace() {
        storyPlaces.forEach { currentTimerPlace ->
            storyPlaces
                .filterNot { it.id == currentTimerPlace.id }
                .forEach { targetPlace ->
                    val state = homePromptState(
                        prompt = Prefs.WATCH_PROMPT_START_TIMER,
                        promptPlaceLabel = targetPlace.safeLabel,
                        timerRunning = true,
                        timerPlaceLabel = currentTimerPlace.safeLabel,
                        durationMinutes = targetPlace.durationMinutes,
                        timerEnd = 120_000L,
                        now = 1_000L,
                    )

                    assertEquals(
                        "switch ${currentTimerPlace.id} -> ${targetPlace.id}",
                        HomePromptKind.SwitchPlace,
                        state?.kind,
                    )
                    assertEquals("Switch to ${targetPlace.safeLabel}?", state?.title)
                    assertEquals(
                        "Stop ${currentTimerPlace.safeLabel} and start ${targetPlace.safeLabel}?",
                        state?.detail,
                    )
                    assertEquals(targetPlace.safeLabel, state?.placeLabel)
                }
        }
    }

    @Test
    fun leaveAndTimeUpPromptsPreferTimerPlaceForEveryHomeOfficeGymTimer() {
        storyPlaces.forEach { timerPlace ->
            val leave = homePromptState(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceLabel = gym.safeLabel,
                timerRunning = true,
                timerPlaceLabel = timerPlace.safeLabel,
                durationMinutes = timerPlace.durationMinutes,
                timerEnd = 121_000L,
                now = 1_000L,
            )
            val done = homePromptState(
                prompt = Prefs.WATCH_PROMPT_TIME_UP,
                promptPlaceLabel = gym.safeLabel,
                timerRunning = false,
                timerPlaceLabel = timerPlace.safeLabel,
                durationMinutes = timerPlace.durationMinutes,
                timerEnd = 0L,
                now = 1_000L,
            )

            assertEquals("Leaving ${timerPlace.safeLabel}?", leave?.title)
            assertEquals(timerPlace.safeLabel, leave?.placeLabel)
            assertEquals("Time's up at ${timerPlace.safeLabel}", done?.title)
            assertEquals(timerPlace.safeLabel, done?.placeLabel)
        }
    }

    @Test
    fun watchDisplayPlacePriorityPermutationsKeepPromptTimerAndMonitoringDistinct() {
        storyPlaces.forEach { activePlace ->
            storyPlaces.forEach { timerPlace ->
                storyPlaces.forEach { promptPlace ->
                    assertEquals(
                        "prompt should beat timer and active",
                        promptPlace.id,
                        WearSync.watchDisplayPlace(
                            places = storyPlaces,
                            activePlace = activePlace,
                            armedPlaces = storyPlaces,
                            registeredPlaceIds = storyPlaces.map { it.id }.toSet(),
                            prompt = Prefs.WATCH_PROMPT_START_TIMER,
                            promptPlaceId = promptPlace.id,
                            timerEnd = 120_000L,
                            timerPlaceId = timerPlace.id,
                        )?.id,
                    )
                    assertEquals(
                        "timer should beat active when there is no prompt",
                        timerPlace.id,
                        WearSync.watchDisplayPlace(
                            places = storyPlaces,
                            activePlace = activePlace,
                            armedPlaces = storyPlaces,
                            registeredPlaceIds = storyPlaces.map { it.id }.toSet(),
                            prompt = Prefs.WATCH_PROMPT_NONE,
                            promptPlaceId = "",
                            timerEnd = 120_000L,
                            timerPlaceId = timerPlace.id,
                        )?.id,
                    )
                }
            }
        }
    }

    @Test
    fun overlappingOfficeGymDoesNotSwitchAwayWhenCurrentTimerPlaceIsStillTriggered() {
        val office = place("office", "Office", latitude = 17.0100, longitude = 78.0000, radiusMeters = 180f)
        val gym = place("gym", "Gym", latitude = 17.0107, longitude = 78.0000, radiusMeters = 180f)
        val places = listOf(gym, office)

        val requests = GeofenceReceiver.inferredRequestsForLocation(
            places = places,
            latitude = 17.0107,
            longitude = 78.0000,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
        )

        assertEquals(
            listOf(
                DwellGeofenceRequest("gym", DwellGeofenceType.ZONE),
                DwellGeofenceRequest("office", DwellGeofenceType.ZONE),
            ),
            requests,
        )
        assertNull(
            GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = placesForRequests(places, requests),
                currentPlaceId = "office",
            )
        )
    }

    @Test
    fun switchPromptTargetMatrixOnlySwitchesWhenCurrentTimerPlaceIsAbsent() {
        storyPlaces.forEach { currentTimerPlace ->
            val allTriggered = GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = storyPlaces,
                currentPlaceId = currentTimerPlace.id,
            )
            val onlyOtherPlacesTriggered = GeofenceReceiver.switchPromptTargetForTriggeredEnter(
                zonePlaces = storyPlaces.filterNot { it.id == currentTimerPlace.id },
                currentPlaceId = currentTimerPlace.id,
            )

            assertNull("no switch while ${currentTimerPlace.id} is still triggered", allTriggered)
            assertEquals(
                storyPlaces.first { it.id != currentTimerPlace.id }.id,
                onlyOtherPlacesTriggered?.id,
            )
        }
    }

    @Test
    fun backgroundReliabilityStatesAreExplicitForPermissionsBatteryAndReboot() {
        val setup = monitoringHealthState(
            placesCount = 3,
            monitoredCount = 3,
            liveCount = 0,
            setupIssue = "Background location permission is needed",
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )
        assertEquals("Monitoring needs setup", setup.title)
        assertEquals("Needs setup", setup.stateLabel)
        assertEquals(MonitoringHealthAction.OpenSettings, setup.action)

        val partial = monitoringHealthState(
            placesCount = 3,
            monitoredCount = 3,
            liveCount = 2,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(),
        )
        assertEquals("Some places need setup", partial.title)
        assertEquals("Partial", partial.stateLabel)

        val batteryRisk = monitoringHealthState(
            placesCount = 3,
            monitoredCount = 3,
            liveCount = 3,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = true,
            batteryReliabilityStatus = batteryStatus(
                isKnownAggressiveOem = true,
                isIgnoringOptimizations = false,
            ),
        )
        assertEquals("Monitoring live, battery may delay", batteryRisk.title)
        assertEquals("Review battery", batteryRisk.actionLabel)
        assertEquals(MonitoringHealthAction.OpenBattery, batteryRisk.action)

        val exactAlarmRiskBeatsBatteryRisk = monitoringHealthState(
            placesCount = 3,
            monitoredCount = 3,
            liveCount = 3,
            setupIssue = null,
            monitoringError = "",
            exactAlarmAllowed = false,
            batteryReliabilityStatus = batteryStatus(
                isKnownAggressiveOem = true,
                isIgnoringOptimizations = false,
            ),
        )
        assertEquals("Timers may be delayed", exactAlarmRiskBeatsBatteryRisk.title)
        assertEquals("Allow alarms", exactAlarmRiskBeatsBatteryRisk.actionLabel)
        assertEquals(MonitoringHealthAction.OpenExactAlarm, exactAlarmRiskBeatsBatteryRisk.action)

        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.LeavePrompt,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "office",
                timerPlaceId = "office",
                promptPlaceExists = true,
            ),
        )
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.SwitchPrompt,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                promptPlaceExists = true,
            ),
        )
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.ClearPromptAndRun,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                promptPlaceExists = false,
            ),
        )
    }

    @Test
    fun monitoringPrerequisitePermissionMatrixOnlyReportsHealthyWhenAllRequiredSignalsArePresent() {
        val booleans = listOf(false, true)
        booleans.forEach { hasLocation ->
            booleans.forEach { hasBackground ->
                booleans.forEach { hasNotifications ->
                    booleans.forEach { hasMotion ->
                        val issue = MonitoringPrerequisites.issueFor(
                            hasLocation = hasLocation,
                            hasBackgroundLocation = hasBackground,
                            hasNotifications = hasNotifications,
                            hasMotion = hasMotion,
                        )
                        val allPresent = hasLocation && hasBackground && hasNotifications && hasMotion

                        if (allPresent) {
                            assertNull(issue)
                        } else {
                            val expected = when {
                                !hasLocation -> "Location permission is needed"
                                !hasNotifications -> "Notification permission is needed"
                                !hasMotion -> "Physical activity permission is needed"
                                else -> "Background location permission is needed"
                            }
                            assertEquals(expected, issue?.error)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun bootRecoveryPromptMatrixKeepsOnlyStillValidRunningTimerPrompts() {
        storyPlaces.forEach { timerPlace ->
            assertEquals(
                BootReceiver.RunningTimerRecoveryAction.LeavePrompt,
                BootReceiver.runningTimerRecoveryAction(
                    prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                    promptPlaceId = timerPlace.id,
                    timerPlaceId = timerPlace.id,
                    promptPlaceExists = true,
                ),
            )
            assertEquals(
                BootReceiver.RunningTimerRecoveryAction.ClearPromptAndRun,
                BootReceiver.runningTimerRecoveryAction(
                    prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                    promptPlaceId = storyPlaces.first { it.id != timerPlace.id }.id,
                    timerPlaceId = timerPlace.id,
                    promptPlaceExists = true,
                ),
            )
            storyPlaces
                .filterNot { it.id == timerPlace.id }
                .forEach { promptPlace ->
                    assertEquals(
                        BootReceiver.RunningTimerRecoveryAction.SwitchPrompt,
                        BootReceiver.runningTimerRecoveryAction(
                            prompt = Prefs.WATCH_PROMPT_START_TIMER,
                            promptPlaceId = promptPlace.id,
                            timerPlaceId = timerPlace.id,
                            promptPlaceExists = true,
                        ),
                    )
                    assertEquals(
                        BootReceiver.RunningTimerRecoveryAction.ClearPromptAndRun,
                        BootReceiver.runningTimerRecoveryAction(
                            prompt = Prefs.WATCH_PROMPT_START_TIMER,
                            promptPlaceId = promptPlace.id,
                            timerPlaceId = timerPlace.id,
                            promptPlaceExists = false,
                        ),
                    )
                }
        }
    }

    @Test
    fun staleWatchStartCommandCannotSwitchOrCancelNewerPhoneTimer() {
        val staleOfficeTimerCommand = PhoneDataService.WatchPromptCommand(
            prompt = Prefs.WATCH_PROMPT_START_TIMER,
            promptUpdated = 42L,
            promptPlaceId = "gym",
            timerPlaceId = "office",
            timerStartedAt = 1_000L,
            timerEnd = 20_000L,
        )

        assertEquals(
            PhoneDataService.WatchStartAction.Ignore,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                currentPromptUpdated = 42L,
                command = staleOfficeTimerCommand,
                timerRunning = true,
                timerPlaceId = "home",
                timerStartedAt = 3_000L,
                timerEnd = 30_000L,
                now = 4_000L,
            ),
        )
        assertEquals(
            PhoneDataService.WatchStartAction.StartPromptedPlace,
            PhoneDataService.watchStartAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                currentPromptUpdated = 42L,
                command = staleOfficeTimerCommand,
                timerRunning = true,
                timerPlaceId = "office",
                timerStartedAt = 1_000L,
                timerEnd = 20_000L,
                now = 4_000L,
            ),
        )
    }

    @Test
    fun watchStartCommandMatrixRejectsStaleTimerIdentityForEveryCurrentPlace() {
        storyPlaces.forEach { currentTimerPlace ->
            storyPlaces.forEach { promptPlace ->
                val matchingCommand = PhoneDataService.WatchPromptCommand(
                    prompt = Prefs.WATCH_PROMPT_START_TIMER,
                    promptUpdated = 42L,
                    promptPlaceId = promptPlace.id,
                    timerPlaceId = currentTimerPlace.id,
                    timerStartedAt = 1_000L,
                    timerEnd = 20_000L,
                )
                val staleCommand = matchingCommand.copy(
                    timerPlaceId = storyPlaces.first { it.id != currentTimerPlace.id }.id,
                    timerStartedAt = 900L,
                    timerEnd = 19_000L,
                )

                assertEquals(
                    if (promptPlace.id == currentTimerPlace.id) {
                        PhoneDataService.WatchStartAction.ClearMatchingRunningTimerPrompt
                    } else {
                        PhoneDataService.WatchStartAction.StartPromptedPlace
                    },
                    PhoneDataService.watchStartAction(
                        prompt = Prefs.WATCH_PROMPT_START_TIMER,
                        promptPlaceId = promptPlace.id,
                        currentPromptUpdated = 42L,
                        command = matchingCommand,
                        timerRunning = true,
                        timerPlaceId = currentTimerPlace.id,
                        timerStartedAt = 1_000L,
                        timerEnd = 20_000L,
                        now = 2_000L,
                    ),
                )
                assertEquals(
                    PhoneDataService.WatchStartAction.Ignore,
                    PhoneDataService.watchStartAction(
                        prompt = Prefs.WATCH_PROMPT_START_TIMER,
                        promptPlaceId = promptPlace.id,
                        currentPromptUpdated = 42L,
                        command = staleCommand,
                        timerRunning = true,
                        timerPlaceId = currentTimerPlace.id,
                        timerStartedAt = 1_000L,
                        timerEnd = 20_000L,
                        now = 2_000L,
                    ),
                )
            }
        }
    }

    private fun placesForRequests(
        places: List<DwellPlace>,
        requests: List<DwellGeofenceRequest>,
    ): List<DwellPlace> =
        requests.mapNotNull { request -> places.firstOrNull { it.id == request.placeId } }

    private fun List<DwellPlace>.shuffledForStablePermutation(): List<DwellPlace> =
        listOf(this[2], this[0], this[1])

    private fun place(
        id: String,
        label: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 150f,
        durationMinutes: Int = 60,
    ): DwellPlace =
        DwellPlace(
            id = id,
            label = label,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            durationMinutes = durationMinutes,
            monitoringEnabled = true,
            autoStart = true,
            createdAtMillis = when (id) {
                "home" -> 1L
                "office" -> 2L
                else -> 3L
            },
            updatedAtMillis = 10L,
        )

    private fun batteryStatus(
        isKnownAggressiveOem: Boolean = false,
        isIgnoringOptimizations: Boolean = true,
    ): BatteryReliabilityStatus =
        BatteryReliabilityStatus(
            manufacturer = "Test",
            isKnownAggressiveOem = isKnownAggressiveOem,
            isIgnoringOptimizations = isIgnoringOptimizations,
        )
}
