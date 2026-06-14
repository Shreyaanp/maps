package work.shreyaan.dwell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Date

object Notifications {
    const val CHANNEL_STATUS = "status"
    const val CHANNEL_ALERT = "alert"

    const val NOTIF_RUNNING = 1
    const val NOTIF_DONE = 2
    const val NOTIF_EXIT = 3
    const val NOTIF_ARRIVAL = 4
    const val NOTIF_SETUP = 5
    const val NOTIF_CONFLICT = 6

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

    private fun actionIntent(
        c: Context,
        action: String,
        requestCode: Int,
        placeId: String? = null,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            c, requestCode,
            Intent(c, NotificationActionReceiver::class.java).apply {
                setAction(action)
                data = Uri.parse(actionDataString(action, placeId))
                placeId?.takeIf { it.isNotBlank() }?.let {
                    putExtra(NotificationActionReceiver.EXTRA_PLACE_ID, it)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun actionDataString(action: String, placeId: String?): String {
        val scopedPlace = placeId?.takeIf { it.isNotBlank() } ?: "_global"
        val encodedAction = URLEncoder.encode(action, StandardCharsets.UTF_8.name())
        val encodedPlace = URLEncoder.encode(scopedPlace, StandardCharsets.UTF_8.name())
        return "dwell://notification-action/$encodedAction/$encodedPlace"
    }

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
        nm(c).cancel(NOTIF_ARRIVAL)
        nm(c).cancel(NOTIF_CONFLICT)
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
        val placeId = Prefs.getPromptPlaceId(c).ifBlank { Prefs.getTimerPlaceId(c).ifBlank { null } }
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("You left the area")
            .setContentText("Keep the timer? It ends at ${timeText(end)}.")
            .setAutoCancel(true)
            .setDeleteIntent(
                actionIntent(c, NotificationActionReceiver.ACTION_KEEP, 318, placeId)
            )
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Keep",
                    actionIntent(c, NotificationActionReceiver.ACTION_KEEP, 312, placeId)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Cancel timer",
                    actionIntent(c, NotificationActionReceiver.ACTION_CANCEL, 313, placeId)
                ).build()
            )
            .build()
        nm(c).notify(NOTIF_EXIT, n)
    }

    fun notifyArrivalQuestion(c: Context, confidenceScore: Int) {
        ensureChannels(c)
        val placeId = Prefs.getPromptPlaceId(c).ifBlank { null }
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Start Dwell timer?")
            .setContentText("Dwell thinks you arrived. Confidence $confidenceScore%.")
            .setAutoCancel(true)
            .setDeleteIntent(
                actionIntent(c, NotificationActionReceiver.ACTION_DISMISS_ARRIVAL, 319, placeId)
            )
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Start timer",
                    actionIntent(c, NotificationActionReceiver.ACTION_START_TIMER, 314, placeId)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Not now",
                    actionIntent(c, NotificationActionReceiver.ACTION_DISMISS_ARRIVAL, 315, placeId)
                ).build()
            )
            .build()
        nm(c).notify(NOTIF_ARRIVAL, n)
    }

    fun notifySwitchPlaceQuestion(
        c: Context,
        newPlaceLabel: String,
        currentPlaceLabel: String,
    ) {
        ensureChannels(c)
        val placeId = Prefs.getPromptPlaceId(c).ifBlank { null }
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Switch Dwell place?")
            .setContentText("Start $newPlaceLabel and stop $currentPlaceLabel?")
            .setAutoCancel(true)
            .setDeleteIntent(
                actionIntent(c, NotificationActionReceiver.ACTION_KEEP_CURRENT, 320, placeId)
            )
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Switch",
                    actionIntent(c, NotificationActionReceiver.ACTION_SWITCH_TIMER, 316, placeId)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Keep current",
                    actionIntent(c, NotificationActionReceiver.ACTION_KEEP_CURRENT, 317, placeId)
                ).build()
            )
            .build()
        nm(c).notify(NOTIF_CONFLICT, n)
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
        nm(c).cancel(NOTIF_ARRIVAL)
        nm(c).cancel(NOTIF_CONFLICT)
        nm(c).notify(NOTIF_DONE, n)
    }

    fun notifySetupNeeded(c: Context) {
        ensureChannels(c)
        val n = Notification.Builder(c, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Dwell needs attention")
            .setContentText("Open Dwell to restore background location for your armed zone.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(c))
            .build()
        nm(c).notify(NOTIF_SETUP, n)
    }

    fun clearExitQuestion(c: Context) {
        nm(c).cancel(NOTIF_EXIT)
    }

    fun clearArrivalQuestion(c: Context) {
        nm(c).cancel(NOTIF_ARRIVAL)
        nm(c).cancel(NOTIF_CONFLICT)
    }

    fun clearDone(c: Context) {
        nm(c).cancel(NOTIF_DONE)
    }

    fun clearAll(c: Context) {
        val manager = nm(c)
        manager.cancel(NOTIF_RUNNING)
        manager.cancel(NOTIF_DONE)
        manager.cancel(NOTIF_EXIT)
        manager.cancel(NOTIF_ARRIVAL)
        manager.cancel(NOTIF_SETUP)
        manager.cancel(NOTIF_CONFLICT)
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
