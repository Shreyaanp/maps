package work.shreyaan.dwell

import kotlin.math.max

object TimerMath {
    private const val MINUTE_MS = 60_000L

    fun endFromDuration(nowMillis: Long, durationMinutes: Int): Long =
        nowMillis + durationMinutes.coerceAtLeast(1) * MINUTE_MS

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
