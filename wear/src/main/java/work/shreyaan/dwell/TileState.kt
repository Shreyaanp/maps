package work.shreyaan.dwell

data class TileState(
    val title: String,
    val primary: String,
    val secondary: String,
    val action: String,
)

object TileStateCalculator {
    const val PROMPT_NONE = "none"
    const val PROMPT_START_TIMER = "start_timer"
    const val PROMPT_LEAVE_EARLY = "leave_early"
    const val PROMPT_TIME_UP = "time_up"
    private val placeholderPlaceLabels = setOf(
        "Selected place",
        "Saved place",
        "No place selected",
    )

    fun state(
        hasPlace: Boolean,
        placeLabel: String,
        promptPlaceLabel: String = "",
        timerPlaceLabel: String = "",
        armed: Boolean,
        needsSetup: Boolean = false,
        registeredPlaceCount: Int = 0,
        timerEnd: Long,
        prompt: String,
        now: Long,
        lastUpdated: Long = now,
    ): TileState {
        val place = placeLabel.shortTilePlace()
        val promptPlace = promptPlaceLabel.shortTilePlace().ifBlank { place }
        val timerPlace = timerPlaceLabel.shortTilePlace().ifBlank { place }
        return when {
            prompt == PROMPT_START_TIMER && !isPhoneStateStale(lastUpdated, now) -> TileState(
                title = if (timerEnd > now) {
                    if (promptPlace.isBlank()) "Switch place?" else "Switch to $promptPlace?"
                } else {
                    if (promptPlace.isBlank()) "Arrived?" else "Arrived $promptPlace?"
                },
                primary = if (timerEnd > now) "Switch?" else "Start?",
                secondary = "Confirm timer",
                action = "Open",
            )
            prompt == PROMPT_LEAVE_EARLY && timerEnd > now && !isPhoneStateStale(lastUpdated, now) -> TileState(
                title = "Leaving ${timerPlace.ifBlank { "place" }}?",
                primary = "Keep?",
                secondary = "${formatRemaining(timerEnd - now)} left",
                action = "Open",
            )
            prompt == PROMPT_TIME_UP && !isPhoneStateStale(lastUpdated, now) -> TileState(
                title = timerPlace.ifBlank { "Dwell" },
                primary = "Done",
                secondary = "Time's up",
                action = "Open",
            )
            timerEnd > now -> TileState(
                title = timerPlace.ifBlank { "Dwell" },
                primary = formatRemaining(timerEnd - now),
                secondary = "Still counting",
                action = "Timer",
            )
            timerEnd in 1..now -> TileState(
                title = timerPlace.ifBlank { "Dwell" },
                primary = "Done",
                secondary = "Time's up",
                action = "Open",
            )
            lastUpdated <= 0L -> TileState(
                title = "Dwell",
                primary = "Syncing",
                secondary = "Open phone once",
                action = "Open",
            )
            isPhoneStateStale(lastUpdated, now) -> TileState(
                title = "Dwell",
                primary = "Phone away",
                secondary = "Open phone app",
                action = "Open",
            )
            needsSetup -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = "Needs setup",
                secondary = "Finish setup on phone",
                action = "Open",
            )
            armed && registeredPlaceCount > 1 -> TileState(
                title = "Dwell",
                primary = "$registeredPlaceCount live",
                secondary = "Monitoring live",
                action = "Open",
            )
            armed -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = "Live",
                secondary = "Starts on arrival",
                action = "Open",
            )
            hasPlace -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = "Paused",
                secondary = "Monitor on phone",
                action = "Open",
            )
            else -> TileState(
                title = "Dwell",
                primary = "No place",
                secondary = "Choose on phone",
                action = "Open",
            )
        }
    }

    internal fun isPhoneStateStale(
        lastUpdated: Long,
        now: Long,
        staleAfterMs: Long = 120_000L,
    ): Boolean =
        lastUpdated > 0L && now - lastUpdated >= staleAfterMs

    fun formatRemaining(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}:${minutes.toString().padStart(2, '0')}"
            minutes > 0 -> "${minutes}:${seconds.toString().padStart(2, '0')}"
            else -> "${seconds}s"
        }
    }

    fun String.shortTilePlace(): String =
        trim()
            .takeUnless { placeholderPlaceLabels.contains(it) }
            ?.let { label ->
                label
                    .substringBefore(",")
                    .trim()
                    .take(18)
            }
            ?: ""
}

object WatchSyncCopy {
    fun syncText(
        lastUpdated: Long,
        now: Long,
        activeTimer: Boolean,
    ): String {
        if (lastUpdated <= 0L) return "No phone sync yet"
        val ageMinutes = phoneStateAgeMinutes(lastUpdated, now)
        return when {
            ageMinutes < 2L -> "Synced just now"
            activeTimer -> "Phone not nearby, still counting"
            else -> "Phone not nearby"
        }
    }

    fun isStale(lastUpdated: Long, now: Long): Boolean =
        lastUpdated > 0L && phoneStateAgeMinutes(lastUpdated, now) >= 2L

    private fun phoneStateAgeMinutes(lastUpdated: Long, now: Long): Long =
        ((now - lastUpdated) / 60_000L).coerceAtLeast(0)
}
