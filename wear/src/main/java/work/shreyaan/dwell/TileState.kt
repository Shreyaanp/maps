package work.shreyaan.dwell

data class TileState(
    val title: String,
    val primary: String,
    val secondary: String,
    val action: String,
)

object TileStateCalculator {
    const val PROMPT_NONE = "none"
    const val PROMPT_LEAVE_EARLY = "leave_early"
    const val PROMPT_TIME_UP = "time_up"

    fun state(
        hasPlace: Boolean,
        placeLabel: String,
        armed: Boolean,
        timerEnd: Long,
        prompt: String,
        now: Long,
    ): TileState {
        val place = placeLabel.shortTilePlace()
        return when {
            prompt == PROMPT_LEAVE_EARLY && timerEnd > now -> TileState(
                title = "Leaving ${place.ifBlank { "place" }}?",
                primary = "Keep?",
                secondary = "${formatRemaining(timerEnd - now)} left",
                action = "Open",
            )
            prompt == PROMPT_TIME_UP || (timerEnd in 1..now) -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = "Done",
                secondary = "Time's up",
                action = "Open",
            )
            timerEnd > now -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = formatRemaining(timerEnd - now),
                secondary = "Still counting",
                action = "Open",
            )
            armed -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = "Ready",
                secondary = "Starts on arrival",
                action = "Open",
            )
            hasPlace -> TileState(
                title = place.ifBlank { "Dwell" },
                primary = "Paused",
                secondary = "Arm on phone",
                action = "Open",
            )
            else -> TileState(
                title = "Dwell",
                primary = "Setup",
                secondary = "Open phone app",
                action = "Open",
            )
        }
    }

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
        substringBefore(",")
            .trim()
            .ifBlank { this }
            .take(18)
}
