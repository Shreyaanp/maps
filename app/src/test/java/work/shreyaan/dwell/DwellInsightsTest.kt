package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DwellInsightsTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun summaryCountsTodayWeekAndPlaceSessions() {
        val now = millis(2026, 6, 14, 20, 0)
        val sessions = listOf(
            session("library", "Library", millis(2026, 6, 14, 10, 0), 120),
            session("library", "Library", millis(2026, 6, 13, 10, 0), 90),
            session("gym", "Gym", millis(2026, 6, 12, 8, 0), 45),
            session("library", "Library", millis(2026, 6, 14, 14, 0), 30, DwellSessionOutcome.Cancelled),
            session("old", "Old", millis(2026, 6, 1, 10, 0), 240),
        )

        val summary = DwellInsights.summaryFor(sessions, now, zone)

        assertEquals(120, summary.todayMinutes)
        assertEquals(255, summary.weekMinutes)
        assertEquals(3, summary.completedSessionsThisWeek)
        assertEquals("Library", summary.bestPlace?.label)
        assertEquals(210, summary.bestPlace?.weekMinutes)
        assertEquals(2, summary.bestPlace?.completedSessionsThisWeek)
        assertTrue(summary.recentSessions.first().outcome == DwellSessionOutcome.Cancelled)
    }

    @Test
    fun decodeSessionsDropsInvalidAndKeepsNewestFirst() {
        val raw = """
            [
              {
                "id": "older",
                "placeId": "library",
                "placeLabel": "Library",
                "startedAt": 1000,
                "endedAt": 61000,
                "plannedDurationMinutes": 60,
                "outcome": "Completed"
              },
              {
                "id": "invalid",
                "placeId": "bad",
                "placeLabel": "Bad",
                "startedAt": 1000,
                "endedAt": 1000,
                "plannedDurationMinutes": 60,
                "outcome": "Completed"
              },
              {
                "id": "newer",
                "placeId": "office",
                "placeLabel": "Office",
                "startedAt": 1000,
                "endedAt": 121000,
                "plannedDurationMinutes": 120,
                "outcome": "Cancelled"
              }
            ]
        """.trimIndent()

        val sessions = DwellInsights.decodeSessions(raw)

        assertEquals(2, sessions.size)
        assertEquals("newer", sessions.first().id)
        assertEquals(DwellSessionOutcome.Cancelled, sessions.first().outcome)
        assertEquals(1, sessions.last().elapsedMinutes)
    }

    private fun session(
        placeId: String,
        label: String,
        startedAtMillis: Long,
        minutes: Int,
        outcome: DwellSessionOutcome = DwellSessionOutcome.Completed,
    ): DwellSession {
        val endedAt = startedAtMillis + minutes * 60_000L
        return DwellSession(
            id = DwellInsights.stableSessionId(startedAtMillis, endedAt, placeId, outcome),
            placeId = placeId,
            placeLabel = label,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAt,
            plannedDurationMinutes = minutes,
            outcome = outcome,
        )
    }

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
