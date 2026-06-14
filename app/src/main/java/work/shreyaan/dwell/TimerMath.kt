package work.shreyaan.dwell

import kotlin.math.max

object TimerMath {
    private const val MINUTE_MS = 60_000L

    fun normalizedDurationMinutes(durationMinutes: Int): Int =
        durationMinutes.coerceIn(
            DwellPlace.MIN_DURATION_MINUTES,
            DwellPlace.MAX_DURATION_MINUTES,
        )

    fun completionDurationMinutes(
        timerPlaceDurationMinutes: Int?,
        fallbackDurationMinutes: Int,
    ): Int =
        normalizedDurationMinutes(timerPlaceDurationMinutes ?: fallbackDurationMinutes)

    fun endFromDuration(nowMillis: Long, durationMinutes: Int): Long =
        nowMillis + normalizedDurationMinutes(durationMinutes) * MINUTE_MS

    fun extendedEnd(
        nowMillis: Long,
        currentEndMillis: Long,
        extraMinutes: Int,
    ): Long {
        val minutes = extraMinutes.coerceIn(1, 240)
        return max(currentEndMillis, nowMillis) + minutes * MINUTE_MS
    }

    fun isRunning(timerEndMillis: Long, nowMillis: Long): Boolean =
        timerEndMillis > nowMillis
}
