package work.shreyaan.dwell

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class DwellSessionOutcome {
    Completed,
    Cancelled;

    companion object {
        fun fromStored(value: String): DwellSessionOutcome =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Cancelled
    }
}

data class DwellSession(
    val id: String,
    val placeId: String,
    val placeLabel: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val plannedDurationMinutes: Int,
    val outcome: DwellSessionOutcome,
) {
    val elapsedMinutes: Int
        get() = DwellInsights.elapsedMinutes(startedAtMillis, endedAtMillis)
}

data class DwellPlaceInsight(
    val placeId: String,
    val label: String,
    val todayMinutes: Int,
    val weekMinutes: Int,
    val completedSessionsThisWeek: Int,
    val latestSessionAtMillis: Long,
)

internal data class DwellTimerSessionPlace(
    val placeId: String,
    val placeLabel: String,
)

data class DwellDayInsight(
    val dayStartMillis: Long,
    val minutes: Int,
)

data class DwellInsightsSummary(
    val todayMinutes: Int,
    val weekMinutes: Int,
    val completedSessionsThisWeek: Int,
    val dayInsights: List<DwellDayInsight>,
    val placeInsights: List<DwellPlaceInsight>,
    val recentSessions: List<DwellSession>,
) {
    val bestPlace: DwellPlaceInsight?
        get() = placeInsights.maxWithOrNull(
            compareBy<DwellPlaceInsight> { it.weekMinutes }
                .thenBy { it.completedSessionsThisWeek }
                .thenBy { it.latestSessionAtMillis },
        )
}

object DwellInsights {
    private const val PREFS_NAME = "dwell_insights"
    private const val SESSIONS_KEY = "sessions_v1"
    private const val MAX_SESSIONS = 240

    fun recordTimerFinished(
        context: Context,
        outcome: DwellSessionOutcome,
        finishedAtMillis: Long = System.currentTimeMillis(),
    ) {
        val startedAt = Prefs.getTimerStartedAt(context)
        val timerEnd = Prefs.getTimerEnd(context)
        if (startedAt <= 0L || timerEnd <= 0L) return

        val placeId = Prefs.getTimerPlaceId(context)
        val sessionPlace = timerSessionPlace(
            timerPlaceId = placeId,
            place = Prefs.getSavedPlace(context, placeId),
        )
        val endedAt = when (outcome) {
            DwellSessionOutcome.Completed -> timerEnd.coerceAtLeast(startedAt)
            DwellSessionOutcome.Cancelled -> finishedAtMillis.coerceIn(startedAt, Long.MAX_VALUE)
        }
        if (elapsedMinutes(startedAt, endedAt) <= 0) return

        saveSession(
            context,
            DwellSession(
                id = stableSessionId(
                    startedAtMillis = startedAt,
                    endedAtMillis = endedAt,
                    placeId = sessionPlace.placeId,
                    outcome = outcome,
                ),
                placeId = sessionPlace.placeId,
                placeLabel = sessionPlace.placeLabel.take(120),
                startedAtMillis = startedAt,
                endedAtMillis = endedAt,
                plannedDurationMinutes = TimerController.completionDurationMinutes(context),
                outcome = outcome,
            ),
        )
    }

    internal fun timerSessionPlace(timerPlaceId: String?, place: DwellPlace?): DwellTimerSessionPlace {
        val explicitPlaceId = timerPlaceId?.takeIf { it.isNotBlank() }
        return if (explicitPlaceId != null && place != null) {
            DwellTimerSessionPlace(place.id, place.safeLabel)
        } else {
            DwellTimerSessionPlace(explicitPlaceId ?: "unknown_place", "Dwell session")
        }
    }

    fun loadSessions(context: Context): List<DwellSession> =
        decodeSessions(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(SESSIONS_KEY, null),
        )

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun summaryFor(
        sessions: List<DwellSession>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DwellInsightsSummary {
        val todayStart = dayStartMillis(nowMillis, zoneId)
        val dayStarts = (6 downTo 0).map { offset ->
            Instant.ofEpochMilli(todayStart)
                .minus(offset.toLong(), ChronoUnit.DAYS)
                .toEpochMilli()
        }
        val weekStart = dayStarts.first()
        val completed = sessions.filter {
            it.outcome == DwellSessionOutcome.Completed &&
                it.endedAtMillis in weekStart..nowMillis
        }
        val today = completed.filter { it.endedAtMillis >= todayStart }
        val dayInsights = dayStarts.mapIndexed { index, dayStart ->
            val nextDayStart = dayStarts.getOrNull(index + 1) ?: Long.MAX_VALUE
            DwellDayInsight(
                dayStartMillis = dayStart,
                minutes = completed
                    .filter { it.endedAtMillis >= dayStart && it.endedAtMillis < nextDayStart }
                    .sumOf { it.elapsedMinutes },
            )
        }

        val placeInsights = completed
            .groupBy { it.placeId }
            .map { (placeId, placeSessions) ->
                val latest = placeSessions.maxBy { it.endedAtMillis }
                DwellPlaceInsight(
                    placeId = placeId,
                    label = latest.placeLabel,
                    todayMinutes = placeSessions
                        .filter { it.endedAtMillis >= todayStart }
                        .sumOf { it.elapsedMinutes },
                    weekMinutes = placeSessions.sumOf { it.elapsedMinutes },
                    completedSessionsThisWeek = placeSessions.size,
                    latestSessionAtMillis = latest.endedAtMillis,
                )
            }
            .sortedWith(
                compareByDescending<DwellPlaceInsight> { it.weekMinutes }
                    .thenByDescending { it.completedSessionsThisWeek }
                    .thenByDescending { it.latestSessionAtMillis },
            )

        return DwellInsightsSummary(
            todayMinutes = today.sumOf { it.elapsedMinutes },
            weekMinutes = completed.sumOf { it.elapsedMinutes },
            completedSessionsThisWeek = completed.size,
            dayInsights = dayInsights,
            placeInsights = placeInsights,
            recentSessions = sessions.sortedByDescending { it.endedAtMillis }.take(8),
        )
    }

    internal fun elapsedMinutes(startedAtMillis: Long, endedAtMillis: Long): Int =
        (((endedAtMillis - startedAtMillis).coerceAtLeast(0L) + 59_999L) / 60_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    internal fun stableSessionId(
        startedAtMillis: Long,
        endedAtMillis: Long,
        placeId: String,
        outcome: DwellSessionOutcome,
    ): String = "${startedAtMillis}_${endedAtMillis}_${placeId}_${outcome.name}"

    internal fun decodeSessions(raw: String?): List<DwellSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    decodeSession(array.optJSONObject(index))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
            .sortedByDescending { it.endedAtMillis }
            .take(MAX_SESSIONS)
    }

    private fun saveSession(context: Context, session: DwellSession) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = decodeSessions(prefs.getString(SESSIONS_KEY, null))
        if (existing.any { it.id == session.id }) return
        val updated = (listOf(session) + existing)
            .sortedByDescending { it.endedAtMillis }
            .take(MAX_SESSIONS)
        prefs.edit()
            .putString(SESSIONS_KEY, encodeSessions(updated))
            .apply()
    }

    private fun dayStartMillis(timeMillis: Long, zoneId: ZoneId): Long =
        Instant.ofEpochMilli(timeMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun encodeSessions(sessions: List<DwellSession>): String {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("placeId", session.placeId)
                    .put("placeLabel", session.placeLabel)
                    .put("startedAt", session.startedAtMillis)
                    .put("endedAt", session.endedAtMillis)
                    .put("plannedDurationMinutes", session.plannedDurationMinutes)
                    .put("outcome", session.outcome.name),
            )
        }
        return array.toString()
    }

    private fun decodeSession(json: JSONObject?): DwellSession? {
        if (json == null) return null
        val startedAt = json.optLong("startedAt", 0L)
        val endedAt = json.optLong("endedAt", 0L)
        if (startedAt <= 0L || endedAt <= startedAt) return null
        val placeId = json.optString("placeId").ifBlank { "unknown_place" }
        val outcome = DwellSessionOutcome.fromStored(json.optString("outcome"))
        return DwellSession(
            id = json.optString("id").ifBlank {
                stableSessionId(startedAt, endedAt, placeId, outcome)
            },
            placeId = placeId,
            placeLabel = json.optString("placeLabel").ifBlank { "Dwell session" }.take(120),
            startedAtMillis = startedAt,
            endedAtMillis = endedAt,
            plannedDurationMinutes = TimerMath.normalizedDurationMinutes(
                json.optInt("plannedDurationMinutes", DwellPlace.MIN_DURATION_MINUTES),
            ),
            outcome = outcome,
        )
    }
}
