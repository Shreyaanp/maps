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

    private fun notificationManager(c: Context): NotificationManager =
        c.getSystemService(NotificationManager::class.java)

    private fun canNotify(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                c,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

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
        val label = placeLabel.ifBlank { "Dwell timer" }
        val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
        val notification = Notification.Builder(c, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Timer started")
            .setContentText("$label ends at $endsAt")
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
        val label = placeLabel.ifBlank { "this place" }
        val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
        val notification = Notification.Builder(c, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Leaving $label?")
            .setContentText("Keep timer or cancel. Ends at $endsAt.")
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
            .setContentTitle("Time's up")
            .setContentText(placeLabel.ifBlank { "Done or extend from your watch." })
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
