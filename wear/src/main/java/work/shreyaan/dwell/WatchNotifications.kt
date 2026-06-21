package work.shreyaan.dwell

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

object WatchNotifications {
    private const val CHANNEL_TIMER = "dwell_timer"
    private const val NOTIF_TIMER = 10
    private val placeholderPlaceLabels = setOf(
        "Selected place",
        "Saved place",
        "No place selected",
    )

    private fun notificationManager(c: Context): NotificationManager =
        c.getSystemService(NotificationManager::class.java)

    private fun canNotify(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                c,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun displayPlaceLabel(placeLabel: String): String? =
        placeLabel
            .trim()
            .takeIf { it.isNotBlank() }
            ?.takeUnless { placeholderPlaceLabels.contains(it) }

    internal fun timerStartedTitle(placeLabel: String): String =
        displayPlaceLabel(placeLabel)?.let { "$it timer started" } ?: "Timer started"

    internal fun timerRunningText(placeLabel: String, endsAt: String): String {
        val label = displayPlaceLabel(placeLabel) ?: "Dwell timer"
        return "$label ends at $endsAt"
    }

    internal fun leavingEarlyTitle(placeLabel: String): String {
        val label = displayPlaceLabel(placeLabel) ?: "this place"
        return "Leaving $label?"
    }

    internal fun timeUpTitle(placeLabel: String): String =
        displayPlaceLabel(placeLabel)?.let { "Time's up at $it" } ?: "Time's up"

    internal fun timeUpText(placeLabel: String): String =
        displayPlaceLabel(placeLabel)?.let { "Extend or mark done for $it." }
            ?: "Done or extend from your watch."

    internal fun arrivalPromptTitle(placeLabel: String): String =
        startPromptNotificationTitle(placeLabel, switching = false)

    internal fun arrivalPromptText(placeLabel: String): String =
        startPromptNotificationText(placeLabel, switching = false)

    internal fun startPromptNotificationTitle(placeLabel: String, switching: Boolean): String {
        val label = displayPlaceLabel(placeLabel)
        return when {
            switching && label != null -> "Switch to $label?"
            switching -> "Switch timer?"
            label != null -> "Start timer at $label?"
            else -> "Start timer?"
        }
    }

    internal fun startPromptNotificationText(placeLabel: String, switching: Boolean): String {
        val label = displayPlaceLabel(placeLabel)
        return when {
            switching && label != null -> "Start $label and stop the current timer."
            switching -> "Start a new timer and stop the current one."
            label != null -> "Dwell thinks you arrived at $label."
            else -> "Dwell thinks you arrived."
        }
    }

    fun ensureChannels(c: Context) {
        val channel = NotificationChannel(
            CHANNEL_TIMER,
            "Dwell timer",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Timer updates from your Dwell phone app"
            enableVibration(true)
        }
        notificationManager(c).createNotificationChannel(channel)
    }

    fun showTimerRunning(
        c: Context,
        placeLabel: String,
        timerEnd: Long,
        alert: Boolean,
    ) {
        if (!canNotify(c)) return
        ensureChannels(c)

        val openApp = PendingIntent.getActivity(
            c,
            10,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
        val notification = Notification.Builder(c, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(timerStartedTitle(placeLabel))
            .setContentText(timerRunningText(placeLabel, endsAt))
            .setWhen(timerEnd)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notificationManager(c).notify(NOTIF_TIMER, notification)
        if (alert) vibrateStart(c)
    }

    fun showLeavingEarly(
        c: Context,
        placeLabel: String,
        timerEnd: Long,
        alert: Boolean,
    ) {
        if (!canNotify(c)) return
        ensureChannels(c)

        val openApp = PendingIntent.getActivity(
            c,
            11,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
        val notification = Notification.Builder(c, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(leavingEarlyTitle(placeLabel))
            .setContentText("Keep timer or cancel. Ends at $endsAt.")
            .setAutoCancel(true)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notificationManager(c).notify(NOTIF_TIMER, notification)
        if (alert) vibratePrompt(c)
    }

    fun showArrivalQuestion(
        c: Context,
        placeLabel: String,
        alert: Boolean,
        switching: Boolean = false,
    ) {
        if (!canNotify(c)) return
        ensureChannels(c)

        val openApp = PendingIntent.getActivity(
            c,
            13,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(c, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(startPromptNotificationTitle(placeLabel, switching))
            .setContentText(startPromptNotificationText(placeLabel, switching))
            .setAutoCancel(true)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notificationManager(c).notify(NOTIF_TIMER, notification)
        if (alert) vibratePrompt(c)
    }

    fun showTimeUp(
        c: Context,
        placeLabel: String,
        alert: Boolean,
    ) {
        if (!canNotify(c)) return
        ensureChannels(c)

        val openApp = PendingIntent.getActivity(
            c,
            12,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(c, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(timeUpTitle(placeLabel))
            .setContentText(timeUpText(placeLabel))
            .setAutoCancel(true)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()

        notificationManager(c).notify(NOTIF_TIMER, notification)
        if (alert) vibrateDone(c)
    }

    fun clearTimer(c: Context) {
        notificationManager(c).cancel(NOTIF_TIMER)
    }

    private fun vibrateStart(c: Context) {
        vibrate(c, longArrayOf(0, 70, 80, 110))
    }

    private fun vibratePrompt(c: Context) {
        vibrate(c, longArrayOf(0, 80, 100, 80))
    }

    private fun vibrateDone(c: Context) {
        vibrate(c, longArrayOf(0, 140, 90, 140, 90, 220))
    }

    private fun vibrate(c: Context, pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            c.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            c.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                pattern,
                -1,
            ),
        )
    }
}
