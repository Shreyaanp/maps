package work.shreyaan.dwell

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object Prefs {
    const val WATCH_PROMPT_NONE = "none"
    const val WATCH_PROMPT_LEAVE_EARLY = "leave_early"
    const val WATCH_PROMPT_TIME_UP = "time_up"

    private fun p(c: Context): SharedPreferences =
        c.getSharedPreferences("dwell", Context.MODE_PRIVATE)

    fun hasPlace(c: Context): Boolean = p(c).contains("lat")
    fun getLat(c: Context): Double = Double.fromBits(p(c).getLong("lat", 0L))
    fun getLon(c: Context): Double = Double.fromBits(p(c).getLong("lon", 0L))
    fun getRadius(c: Context): Float = p(c).getFloat("radius", 150f).coerceIn(50f, 500f)
    fun getPlaceLabel(c: Context): String =
        p(c).getString("place_label", null)?.takeIf { it.isNotBlank() } ?: "Saved place"

    fun savePlace(c: Context, lat: Double, lon: Double) {
        p(c).edit()
            .putLong("lat", lat.toRawBits())
            .putLong("lon", lon.toRawBits())
            .apply()
    }

    fun savePlace(c: Context, lat: Double, lon: Double, label: String) {
        p(c).edit()
            .putLong("lat", lat.toRawBits())
            .putLong("lon", lon.toRawBits())
            .putString("place_label", label.ifBlank { "Selected place" })
            .apply()
    }

    fun clearPlace(c: Context) {
        p(c).edit()
            .remove("lat")
            .remove("lon")
            .remove("place_label")
            .remove("armed")
            .apply()
    }

    fun setRadius(c: Context, radius: Float) {
        p(c).edit().putFloat("radius", radius.coerceIn(50f, 500f)).apply()
    }

    // Default 270 minutes = 4.5 hours
    fun getDurationMinutes(c: Context): Int = p(c).getInt("duration_min", 270)
    fun setDurationMinutes(c: Context, min: Int) {
        p(c).edit().putInt("duration_min", min).apply()
    }

    fun isArmed(c: Context): Boolean = p(c).getBoolean("armed", false)
    fun setArmed(c: Context, armed: Boolean) {
        p(c).edit().putBoolean("armed", armed).apply()
    }

    fun getTimerEnd(c: Context): Long = p(c).getLong("timer_end", 0L)
    fun setTimerEnd(c: Context, end: Long) {
        p(c).edit().putLong("timer_end", end).apply()
    }

    fun getTimerStartedAt(c: Context): Long = p(c).getLong("timer_started_at", 0L)
    fun setTimerStartedAt(c: Context, startedAt: Long) {
        p(c).edit().putLong("timer_started_at", startedAt).apply()
    }

    fun getWatchPrompt(c: Context): String =
        p(c).getString("watch_prompt", WATCH_PROMPT_NONE) ?: WATCH_PROMPT_NONE

    fun getWatchPromptUpdated(c: Context): Long = p(c).getLong("watch_prompt_updated", 0L)

    fun setWatchPrompt(c: Context, prompt: String) {
        p(c).edit()
            .putString("watch_prompt", prompt)
            .putLong("watch_prompt_updated", System.currentTimeMillis())
            .apply()
    }

    fun clearWatchPrompt(c: Context) {
        p(c).edit()
            .putString("watch_prompt", WATCH_PROMPT_NONE)
            .putLong("watch_prompt_updated", System.currentTimeMillis())
            .apply()
    }

    fun isSignedIn(c: Context): Boolean = p(c).getBoolean("signed_in", false)
    fun setSignedIn(c: Context, signedIn: Boolean) {
        val editor = p(c).edit().putBoolean("signed_in", signedIn)
        if (!signedIn) {
            editor
                .remove("backend_session_token")
                .remove("account_provider")
                .remove("account_display_name")
                .remove("account_email")
        }
        editor.apply()
    }

    fun saveAccount(
        c: Context,
        provider: String,
        displayName: String = "",
        email: String = "",
    ) {
        p(c).edit()
            .putString("account_provider", if (provider == "google") "google" else "local")
            .putString("account_display_name", displayName.take(120))
            .putString("account_email", email.take(160))
            .apply()
    }

    fun getAccountProvider(c: Context): String =
        p(c).getString("account_provider", "local") ?: "local"

    fun getAccountDisplayName(c: Context): String =
        p(c).getString("account_display_name", "") ?: ""

    fun getAccountEmail(c: Context): String =
        p(c).getString("account_email", "") ?: ""

    fun getBackendSessionToken(c: Context): String =
        p(c).getString("backend_session_token", null).orEmpty()

    fun setBackendSessionToken(c: Context, token: String) {
        p(c).edit().putString("backend_session_token", token).apply()
    }

    fun clearAppData(c: Context, keepSession: Boolean) {
        p(c).edit()
            .remove("lat")
            .remove("lon")
            .remove("radius")
            .remove("duration_min")
            .remove("armed")
            .remove("timer_end")
            .remove("timer_started_at")
            .remove("place_label")
            .remove("watch_prompt")
            .remove("watch_prompt_updated")
            .apply()

        if (!keepSession) {
            setSignedIn(c, false)
        }
    }

    fun getInstallId(c: Context): String {
        val prefs = p(c)
        val existing = prefs.getString("install_id", null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        prefs.edit().putString("install_id", created).apply()
        return created
    }
}
