package xyz.mercle.geotimer

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private fun p(c: Context): SharedPreferences =
        c.getSharedPreferences("geotimer", Context.MODE_PRIVATE)

    fun hasPlace(c: Context): Boolean = p(c).contains("lat")
    fun getLat(c: Context): Double = Double.fromBits(p(c).getLong("lat", 0L))
    fun getLon(c: Context): Double = Double.fromBits(p(c).getLong("lon", 0L))
    fun getRadius(c: Context): Float = p(c).getFloat("radius", 150f)

    fun savePlace(c: Context, lat: Double, lon: Double) {
        p(c).edit()
            .putLong("lat", lat.toRawBits())
            .putLong("lon", lon.toRawBits())
            .apply()
    }

    fun setRadius(c: Context, radius: Float) {
        p(c).edit().putFloat("radius", radius).apply()
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
}
