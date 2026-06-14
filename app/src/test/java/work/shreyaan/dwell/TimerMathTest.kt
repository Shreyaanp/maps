package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerMathTest {
    @Test
    fun endFromDurationUsesAtLeastOneMinute() {
        assertEquals(1_060_000L, TimerMath.endFromDuration(1_000_000L, 1))
        assertEquals(1_060_000L, TimerMath.endFromDuration(1_000_000L, 0))
        assertEquals(1_060_000L, TimerMath.endFromDuration(1_000_000L, -5))
    }

    @Test
    fun endFromDurationClampsToMaxDuration() {
        assertEquals(173_800_000L, TimerMath.endFromDuration(1_000_000L, 99_999))
    }

    @Test
    fun completionDurationPrefersTimerPlaceDuration() {
        assertEquals(
            90,
            TimerMath.completionDurationMinutes(
                timerPlaceDurationMinutes = 90,
                fallbackDurationMinutes = 270,
            ),
        )
    }

    @Test
    fun completionDurationFallsBackWhenTimerPlaceIsMissing() {
        assertEquals(
            270,
            TimerMath.completionDurationMinutes(
                timerPlaceDurationMinutes = null,
                fallbackDurationMinutes = 270,
            ),
        )
    }

    @Test
    fun completionDurationIsClamped() {
        assertEquals(
            DwellPlace.MIN_DURATION_MINUTES,
            TimerMath.completionDurationMinutes(
                timerPlaceDurationMinutes = 0,
                fallbackDurationMinutes = 270,
            ),
        )
        assertEquals(
            DwellPlace.MAX_DURATION_MINUTES,
            TimerMath.completionDurationMinutes(
                timerPlaceDurationMinutes = 99_999,
                fallbackDurationMinutes = 270,
            ),
        )
    }

    @Test
    fun extendedEndUsesExistingEndWhenTimerIsActive() {
        assertEquals(
            4_600_000L,
            TimerMath.extendedEnd(
                nowMillis = 1_000_000L,
                currentEndMillis = 1_000_000L + 30 * 60_000L,
                extraMinutes = 30,
            ),
        )
    }

    @Test
    fun extendedEndUsesNowWhenTimerIsExpired() {
        assertEquals(
            1_900_000L,
            TimerMath.extendedEnd(
                nowMillis = 1_000_000L,
                currentEndMillis = 900_000L,
                extraMinutes = 15,
            ),
        )
    }

    @Test
    fun extendedEndClampsExtraMinutes() {
        assertEquals(1_060_000L, TimerMath.extendedEnd(1_000_000L, 0L, 0))
        assertEquals(15_400_000L, TimerMath.extendedEnd(1_000_000L, 0L, 999))
    }

    @Test
    fun isRunningOnlyWhenEndIsInFuture() {
        assertTrue(TimerMath.isRunning(timerEndMillis = 101L, nowMillis = 100L))
        assertFalse(TimerMath.isRunning(timerEndMillis = 100L, nowMillis = 100L))
        assertFalse(TimerMath.isRunning(timerEndMillis = 99L, nowMillis = 100L))
    }
}
