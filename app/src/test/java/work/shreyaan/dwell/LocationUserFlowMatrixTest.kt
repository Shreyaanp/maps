package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUserFlowMatrixTest {
    private val home = place(
        id = "home",
        label = "Home",
        latitude = 17.0000,
        longitude = 78.0000,
        radiusMeters = 50f,
        durationMinutes = 45,
        monitoringEnabled = true,
        autoStart = true,
        createdAtMillis = 1L,
    )
    private val office = place(
        id = "office",
        label = "Office",
        latitude = 17.0100,
        longitude = 78.0000,
        radiusMeters = 130f,
        durationMinutes = 90,
        monitoringEnabled = true,
        autoStart = false,
        createdAtMillis = 2L,
    )
    private val gym = place(
        id = "gym",
        label = "Gym",
        latitude = 17.0140,
        longitude = 78.0000,
        radiusMeters = 140f,
        durationMinutes = 60,
        monitoringEnabled = false,
        autoStart = true,
        createdAtMillis = 3L,
    )
    private val savedPlaces = listOf(home, office, gym)

    @Test
    fun locationSetSourceMatrixSeparatesNewPlacesFromMovingExistingPlaces() {
        var coveredCases = 0

        LocationInput.values().forEach { source ->
            StartingPlaceState.values().forEach { start ->
                coveredCases += 1
                val actual = previewModeForMapPoint(
                    selectionMode = start.selectionMode,
                    hasSelectedExistingPlace = start.hasSelectedExistingPlace,
                    forceCreateNew = source.forceCreateNew,
                )
                val expected =
                    if (
                        !source.forceCreateNew &&
                        start.selectionMode == PlaceSelectionMode.EditSelected &&
                        start.hasSelectedExistingPlace
                    ) {
                        PlaceSelectionMode.EditSelected
                    } else {
                        PlaceSelectionMode.CreateNew
                    }

                assertEquals("${source.name} from ${start.name}", expected, actual)
            }
        }

        assertEquals(15, coveredCases)
    }

    @Test
    fun pendingPreviewPointSourceMatrixKeepsDraftsInsideOneCompartment() {
        var coveredCases = 0

        LocationInput.values().forEach { source ->
            StartingPlaceState.values().forEach { start ->
                listOf(null, PlaceSelectionMode.CreateNew, PlaceSelectionMode.EditSelected)
                    .forEach { pendingMode ->
                        coveredCases += 1
                        val nextMode = previewModeForMapPoint(
                            selectionMode = start.selectionMode,
                            hasSelectedExistingPlace = start.hasSelectedExistingPlace,
                            forceCreateNew = source.forceCreateNew,
                        )
                        val blocked = shouldBlockMapPointSelection(
                            pendingPreviewMode = pendingMode,
                            nextPreviewMode = nextMode,
                        )

                        assertEquals(
                            "${source.name} from ${start.name} with pending=$pendingMode",
                            pendingMode != null && pendingMode != nextMode,
                            blocked,
                        )
                        assertEquals(
                            "${source.name} from ${start.name} carry draft with pending=$pendingMode",
                            !blocked && pendingMode == nextMode,
                            shouldCarryPendingPreviewDraft(
                                previousPreviewMode = pendingMode,
                                previewMode = nextMode,
                            ),
                        )
                    }
            }
        }

        assertEquals(45, coveredCases)
    }

    @Test
    fun settingsPersistenceTargetMatrixKeepsPendingEditingAndDefaultsSeparate() {
        var coveredCases = 0
        val booleans = listOf(false, true)

        booleans.forEach { hasPendingPlacePreview ->
            PlaceSelectionMode.values().forEach { selectionMode ->
                booleans.forEach { hasEditingPlace ->
                    coveredCases += 1
                    val expected = when {
                        hasPendingPlacePreview -> SettingsPersistenceTarget.PendingPreview
                        selectionMode == PlaceSelectionMode.EditSelected && !hasEditingPlace ->
                            SettingsPersistenceTarget.ReadOnlyPlace
                        selectionMode == PlaceSelectionMode.ViewSelected && hasEditingPlace ->
                            SettingsPersistenceTarget.ReadOnlyPlace
                        selectionMode == PlaceSelectionMode.EditSelected && hasEditingPlace ->
                            SettingsPersistenceTarget.EditingPlace
                        else -> SettingsPersistenceTarget.DefaultSettings
                    }

                    assertEquals(
                        "pending=$hasPendingPlacePreview mode=$selectionMode editing=$hasEditingPlace",
                        expected,
                        settingsPersistenceTarget(
                            hasPendingPlacePreview = hasPendingPlacePreview,
                            selectionMode = selectionMode,
                            hasEditingPlace = hasEditingPlace,
                        ),
                    )
                }
            }
        }

        assertEquals(12, coveredCases)
    }

    @Test
    fun everySavedPlaceMutationOnlyChangesTheTargetPlaceCompartment() {
        var coveredCases = 0

        savedPlaces.forEach { target ->
            PlaceMutation.values().forEach { mutation ->
                coveredCases += 1
                val updatedTarget = mutation.applyTo(target)
                val updatedPlaces = savedPlaces.map { place ->
                    if (place.id == target.id) updatedTarget else place
                }

                savedPlaces
                    .filterNot { it.id == target.id }
                    .forEach { untouched ->
                        assertEquals(
                            "${mutation.name} on ${target.id} must not mutate ${untouched.id}",
                            untouched,
                            updatedPlaces.single { it.id == untouched.id },
                        )
                    }

                assertEquals(target.id, updatedTarget.id)
                assertEquals(target.createdAtMillis, updatedTarget.createdAtMillis)
                assertEquals(99L, updatedTarget.updatedAtMillis)
            }
        }

        assertEquals(15, coveredCases)
    }

    @Test
    fun everyNewPlaceEntryPointAddsOnePlaceWithoutChangingExistingPlaceSettings() {
        var coveredCases = 0

        LocationInput.values().forEach { source ->
            coveredCases += 1
            val candidate = place(
                id = "new-${source.name.lowercase()}",
                label = source.defaultLabel,
                latitude = 18.0000 + coveredCases,
                longitude = 79.0000,
                radiusMeters = 75f + coveredCases,
                durationMinutes = 30 + coveredCases,
                monitoringEnabled = false,
                autoStart = coveredCases % 2 == 0,
                createdAtMillis = 10L + coveredCases,
            )

            val normalized = DwellPlace.normalizePlaces(savedPlaces + candidate)

            assertEquals(4, normalized.size)
            savedPlaces.forEach { existing ->
                assertEquals(
                    "${source.name} new-place flow changed ${existing.id}",
                    existing,
                    normalized.single { it.id == existing.id },
                )
            }
            assertEquals(candidate.normalized(), normalized.single { it.id == candidate.id })
        }

        assertEquals(3, coveredCases)
    }

    @Test
    fun duplicateAndNearbyPlaceMatrixSelectsOnlyTrueDuplicates() {
        val cases = listOf(
            DuplicateCase(
                name = "near exact, different label",
                label = "Work",
                latitude = office.latitude + 0.00002,
                expectedExisting = true,
            ),
            DuplicateCase(
                name = "same label nearby",
                label = " office ",
                latitude = office.latitude + 0.0001,
                expectedExisting = true,
            ),
            DuplicateCase(
                name = "different label nearby",
                label = "Gym",
                latitude = office.latitude + 0.0001,
                expectedExisting = false,
            ),
            DuplicateCase(
                name = "same label far away",
                label = "Office",
                latitude = office.latitude + 0.0100,
                expectedExisting = false,
            ),
        )

        cases.forEachIndexed { index, duplicateCase ->
            val candidate = place(
                id = "candidate-$index",
                label = duplicateCase.label,
                latitude = duplicateCase.latitude,
                longitude = office.longitude,
                radiusMeters = 300f,
                durationMinutes = 15,
                monitoringEnabled = false,
                autoStart = true,
                createdAtMillis = 20L + index,
            )

            val selected = Prefs.placeForCreate(listOf(office), candidate)

            if (duplicateCase.expectedExisting) {
                assertEquals(duplicateCase.name, office.id, selected.id)
                assertEquals(office.radiusMeters, selected.radiusMeters, 0f)
                assertEquals(office.durationMinutes, selected.durationMinutes)
                assertEquals(office.autoStart, selected.autoStart)
            } else {
                assertEquals(duplicateCase.name, candidate.id, selected.id)
                assertEquals(candidate.radiusMeters, selected.radiusMeters, 0f)
                assertEquals(candidate.durationMinutes, selected.durationMinutes)
                assertEquals(candidate.autoStart, selected.autoStart)
            }
        }

        assertEquals(4, cases.size)
    }

    @Test
    fun monitoredPlaceLimitKeepsSavedPlacesButCompartmentalizesLiveMonitoring() {
        val places = (0 until DwellPlace.MAX_MONITORED_PLACES + 3).map { index ->
            place(
                id = "place-$index",
                label = "Place $index",
                latitude = 17.0 + (index * 0.01),
                longitude = 78.0,
                radiusMeters = 50f,
                durationMinutes = 30 + index,
                monitoringEnabled = true,
                autoStart = index % 2 == 0,
                createdAtMillis = index.toLong() + 1L,
            )
        }

        val normalized = DwellPlace.normalizePlaces(places)

        assertEquals(DwellPlace.MAX_MONITORED_PLACES + 3, normalized.size)
        assertEquals(DwellPlace.MAX_MONITORED_PLACES, normalized.count { it.monitoringEnabled })
        assertEquals(
            listOf("place-20", "place-21", "place-22"),
            normalized.filterNot { it.monitoringEnabled }.map { it.id },
        )
        places.forEach { original ->
            val saved = normalized.single { it.id == original.id }
            assertEquals(original.durationMinutes, saved.durationMinutes)
            assertEquals(original.autoStart, saved.autoStart)
        }
    }

    @Test
    fun runtimePlaceMatrixNamesTimerOwnerWhenStartingAnotherRowIsBlocked() {
        val rowActionAvailability = placesRowActionAvailability(
            hasPendingPlacePreview = false,
            timerActive = true,
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
                actionAvailability = rowActionAvailability,
                timerPlaceLabel = "Office",
            ),
        )
        assertEquals(
            PlacesRowTimerAction(
                label = "Cancel timer",
                enabled = true,
                cancelTimer = true,
            ),
            placesRowTimerAction(
                isTimerPlace = true,
                actionAvailability = rowActionAvailability,
                timerPlaceLabel = "Office",
            ),
        )
    }

    private enum class LocationInput(
        val forceCreateNew: Boolean,
        val defaultLabel: String,
    ) {
        SearchResult(forceCreateNew = false, defaultLabel = "Searched cafe"),
        CurrentLocation(forceCreateNew = false, defaultLabel = "Current location"),
        LongPressMap(forceCreateNew = true, defaultLabel = "Dropped pin"),
    }

    private enum class StartingPlaceState(
        val selectionMode: PlaceSelectionMode,
        val hasSelectedExistingPlace: Boolean,
    ) {
        NoSavedPlace(PlaceSelectionMode.CreateNew, false),
        AddPlaceWithSavedPlaces(PlaceSelectionMode.CreateNew, false),
        ViewingHome(PlaceSelectionMode.ViewSelected, true),
        EditingHome(PlaceSelectionMode.EditSelected, true),
        EditingMissingPlace(PlaceSelectionMode.EditSelected, false),
    }

    private enum class PlaceMutation {
        RenameAndMove,
        Radius,
        Duration,
        AutoStart,
        Monitoring;

        fun applyTo(place: DwellPlace): DwellPlace =
            when (this) {
                RenameAndMove -> Prefs.placeForUpdate(
                    active = place,
                    lat = place.latitude + 0.001,
                    lon = place.longitude + 0.001,
                    label = "${place.safeLabel} moved",
                    radiusMeters = place.radiusMeters,
                    durationMinutes = place.durationMinutes,
                    autoStart = null,
                    now = 99L,
                )
                Radius -> place.withTimerDefaults(
                    radiusMeters = place.radiusMeters + 50f,
                    durationMinutes = place.durationMinutes,
                    now = 99L,
                )
                Duration -> place.withTimerDefaults(
                    radiusMeters = place.radiusMeters,
                    durationMinutes = place.durationMinutes + 15,
                    now = 99L,
                )
                AutoStart -> place.withAutoStart(!place.autoStart, now = 99L)
                Monitoring -> place.withMonitoring(!place.monitoringEnabled, now = 99L)
            }
    }

    private data class DuplicateCase(
        val name: String,
        val label: String,
        val latitude: Double,
        val expectedExisting: Boolean,
    )

    private fun place(
        id: String,
        label: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        durationMinutes: Int,
        monitoringEnabled: Boolean,
        autoStart: Boolean,
        createdAtMillis: Long,
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
            createdAtMillis = createdAtMillis,
            updatedAtMillis = createdAtMillis,
        ).normalized()
}
