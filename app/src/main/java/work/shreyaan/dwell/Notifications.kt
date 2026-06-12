package work.shreyaan.dwell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import java.text.DateFormat
import java.util.Date

object Notifications {
    const val CHANNEL_STATUS = "status"
    const val CHANNEL_ALERT = "alert"

    const val NOTIF_RUNNING = 1
    const val NOTIF_DONE = 2
    const val NOTIF_EXIT = 3

    private fun nm(c: Context): NotificationManager =
        c.getSystemService(NotificationManager::class.java)

    fun ensureChannels(c: Context) {
        val manager = nm(c)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS, "Timer status",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val alert = NotificationChannel(
            CHANNEL_ALERT, "Timer alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
        }
        manager.createNotificationChannel(alert)
    }

    private fun openAppIntent(c: Context): PendingIntent =
        PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun actionIntent(c: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, requestCode,
            Intent(c, NotificationActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun timeText(end: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(end))

    /**
     * Ongoing live-countdown notification. Wear OS mirrors this to the watch,
     * chronometer and action buttons included.
     */
    fun notifyTimerRunning(c: Context, end: Long) {
        ensureChannels(c)
        val n = Notification.Builder(c, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Dwell running")
            .setContentText("Ends at ${timeText(end)}")
            .setWhen(end)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Cancel timer",
                    actionIntent(c, NotificationActionReceiver.ACTION_CANCEL, 311)
                ).build()
            )
            .build()
        nm(c).notify(NOTIF_RUNNING, n)
    }

    fun notifyTimerDone(c: Context, durationMinutes: Int) {
        ensureChannels(c)
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Time's up!")
            .setContentText("Your ${formatDuration(durationMinutes)} timer is complete.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(c))
            .build()
        nm(c).cancel(NOTIF_RUNNING)
        nm(c).notify(NOTIF_DONE, n)
    }

    fun notifyExitQuestion(c: Context, end: Long) {
        ensureChannels(c)
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("You left the area")
            .setContentText("Keep the timer? It ends at ${timeText(end)}.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Keep",
                    actionIntent(c, NotificationActionReceiver.ACTION_KEEP, 312)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Cancel timer",
                    actionIntent(c, NotificationActionReceiver.ACTION_CANCEL, 313)
                ).build()
            )
            .build()
        nm(c).notify(NOTIF_EXIT, n)
    }

    fun notifyTimerCancelled(c: Context) {
        ensureChannels(c)
        val n = Notification.Builder(c, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Timer cancelled")
            .setAutoCancel(true)
            .setTimeoutAfter(10_000)
            .setContentIntent(openAppIntent(c))
            .build()
        nm(c).cancel(NOTIF_RUNNING)
        nm(c).cancel(NOTIF_EXIT)
        nm(c).notify(NOTIF_DONE, n)
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }
}
