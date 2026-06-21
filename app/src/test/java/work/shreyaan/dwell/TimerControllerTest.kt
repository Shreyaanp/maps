package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerControllerTest {
    @Test
    fun blankRequestedPlaceIdFallsBackToPromptOrActivePlace() {
        assertEquals(
            "office",
            TimerController.resolvedTimerPlaceId(
                requestedPlaceId = "",
                promptPlaceId = "office",
                activePlaceId = "home",
            ),
        )
        assertEquals(
            "home",
            TimerController.resolvedTimerPlaceId(
                requestedPlaceId = " ",
                promptPlaceId = "",
                activePlaceId = "home",
            ),
        )
        assertEquals(
            "gym",
            TimerController.resolvedTimerPlaceId(
                requestedPlaceId = "gym",
                promptPlaceId = "office",
                activePlaceId = "home",
            ),
        )
    }

    @Test
    fun explicitNoPlaceTimerCanOptOutOfActivePlaceFallback() {
        assertEquals(
            null,
            TimerController.resolvedTimerPlaceId(
                requestedPlaceId = "",
                promptPlaceId = "",
                activePlaceId = "home",
                allowActivePlaceFallback = false,
            ),
        )
        assertEquals(
            "office",
            TimerController.resolvedTimerPlaceId(
                requestedPlaceId = "",
                promptPlaceId = "office",
                activePlaceId = "home",
                allowActivePlaceFallback = false,
            ),
        )
        assertEquals(
            "gym",
            TimerController.resolvedTimerPlaceId(
                requestedPlaceId = "gym",
                promptPlaceId = "office",
                activePlaceId = "home",
                allowActivePlaceFallback = false,
            ),
        )
    }

    @Test
    fun explicitTimerPlaceDurationIgnoresBlankTimerPlace() {
        var lookupCount = 0

        val duration = TimerController.explicitTimerPlaceDurationMinutes("") {
            lookupCount += 1
            90
        }

        assertEquals(null, duration)
        assertEquals(0, lookupCount)
    }

    @Test
    fun explicitTimerPlaceDurationUsesOnlyExplicitTimerPlace() {
        assertEquals(
            45,
            TimerController.explicitTimerPlaceDurationMinutes("office") { placeId ->
                if (placeId == "office") 45 else null
            },
        )
    }

    @Test
    fun timerAlarmAcceptsMatchingScopedDueTimer() {
        assertTrue(
            TimerController.acceptsTimerAlarm(
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 10_001L,
            )
        )
    }

    @Test
    fun timerAlarmRejectsStaleScopedAlarmForNewerTimer() {
        assertFalse(
            TimerController.acceptsTimerAlarm(
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
                currentTimerPlaceId = "gym",
                currentTimerStartedAt = 2_000L,
                currentTimerEnd = 20_000L,
                now = 10_001L,
            )
        )
    }

    @Test
    fun timerAlarmRejectsCurrentTimerThatIsNotDue() {
        assertFalse(
            TimerController.acceptsTimerAlarm(
                actionTimerPlaceId = "office",
                actionTimerStartedAt = 1_000L,
                actionTimerEnd = 10_000L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 9_999L,
            )
        )
    }

    @Test
    fun legacyTimerAlarmOnlyAppliesWhenCurrentTimerIsDue() {
        assertTrue(
            TimerController.acceptsTimerAlarm(
                actionTimerPlaceId = null,
                actionTimerStartedAt = 0L,
                actionTimerEnd = 0L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 10_001L,
            )
        )
        assertFalse(
            TimerController.acceptsTimerAlarm(
                actionTimerPlaceId = null,
                actionTimerStartedAt = 0L,
                actionTimerEnd = 0L,
                currentTimerPlaceId = "office",
                currentTimerStartedAt = 1_000L,
                currentTimerEnd = 10_000L,
                now = 9_999L,
            )
        )
    }
}
