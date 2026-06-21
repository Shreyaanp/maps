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

    private data class TimerActionScope(
        val placeId: String?,
        val startedAt: Long,
        val end: Long,
    )

    private data class PromptActionScope(
        val prompt: String,
        val updated: Long,
    )

    private fun currentTimerActionScope(c: Context, end: Long = Prefs.getTimerEnd(c)): TimerActionScope? {
        val startedAt = Prefs.getTimerStartedAt(c)
        val resolvedEnd = end.takeIf { it > 0L } ?: Prefs.getTimerEnd(c)
        if (startedAt <= 0L || resolvedEnd <= 0L) return null
        return TimerActionScope(
            placeId = Prefs.getTimerPlaceId(c).takeIf { it.isNotBlank() },
            startedAt = startedAt,
            end = resolvedEnd,
        )
    }

    private fun currentPromptActionScope(c: Context): PromptActionScope? {
        val prompt = Prefs.getWatchPrompt(c)
        val updated = Prefs.getWatchPromptUpdated(c)
        if (prompt == Prefs.WATCH_PROMPT_NONE || updated <= 0L) return null
        return PromptActionScope(prompt = prompt, updated = updated)
    }

    private fun actionIntent(
        c: Context,
        action: String,
        requestCode: Int,
        placeId: String? = null,
        timerScope: TimerActionScope? = null,
        promptScope: PromptActionScope? = null,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            c, requestCode,
            Intent(c, NotificationActionReceiver::class.java).apply {
                setAction(action)
                data = Uri.parse(
                    actionDataString(
                        action = action,
                        placeId = placeId,
                        timerPlaceId = timerScope?.placeId,
                        timerStartedAt = timerScope?.startedAt ?: 0L,
                        timerEnd = timerScope?.end ?: 0L,
                        prompt = promptScope?.prompt,
                        promptUpdated = promptScope?.updated ?: 0L,
                    )
                )
                placeId?.takeIf { it.isNotBlank() }?.let {
                    putExtra(NotificationActionReceiver.EXTRA_PLACE_ID, it)
                }
                timerScope?.placeId?.takeIf { it.isNotBlank() }?.let {
                    putExtra(NotificationActionReceiver.EXTRA_TIMER_PLACE_ID, it)
                }
                timerScope?.let {
                    putExtra(NotificationActionReceiver.EXTRA_TIMER_STARTED_AT, it.startedAt)
                    putExtra(NotificationActionReceiver.EXTRA_TIMER_END, it.end)
                }
                promptScope?.let {
                    putExtra(NotificationActionReceiver.EXTRA_PROMPT, it.prompt)
                    putExtra(NotificationActionReceiver.EXTRA_PROMPT_UPDATED, it.updated)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun actionDataString(
        action: String,
        placeId: String?,
        timerPlaceId: String? = null,
        timerStartedAt: Long = 0L,
        timerEnd: Long = 0L,
        prompt: String? = null,
        promptUpdated: Long = 0L,
    ): String {
        val scopedPlace = placeId?.takeIf { it.isNotBlank() } ?: "_global"
        val scopedTimerPlace = timerPlaceId?.takeIf { it.isNotBlank() } ?: "_none"
        val scopedPrompt = prompt?.takeIf { it.isNotBlank() } ?: "_none"
        val encodedAction = URLEncoder.encode(action, StandardCharsets.UTF_8.name())
        val encodedPlace = URLEncoder.encode(scopedPlace, StandardCharsets.UTF_8.name())
        val encodedTimerPlace = URLEncoder.encode(scopedTimerPlace, StandardCharsets.UTF_8.name())
        val encodedPrompt = URLEncoder.encode(scopedPrompt, StandardCharsets.UTF_8.name())
        val timerPath = if (timerStartedAt > 0L || timerEnd > 0L || !timerPlaceId.isNullOrBlank()) {
            "/timer/$encodedTimerPlace/$timerStartedAt/$timerEnd"
        } else {
            ""
        }
        val promptPath = if (promptUpdated > 0L || !prompt.isNullOrBlank()) {
            "/prompt/$encodedPrompt/$promptUpdated"
        } else {
            ""
        }
        return "dwell://notification-action/$encodedAction/$encodedPlace$timerPath$promptPath"
    }

    private fun timeText(end: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(end))

    private val placeholderPlaceLabels = setOf(
        "Selected place",
        "Saved place",
        "No place selected",
    )

    private fun displayPlaceLabel(placeLabel: String?): String? =
        placeLabel
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless { placeholderPlaceLabels.contains(it) }

    internal fun timerRunningTitle(placeLabel: String?): String =
        displayPlaceLabel(placeLabel)?.let { "$it timer running" } ?: "Dwell running"

    internal fun timerDoneTitle(placeLabel: String?): String =
        displayPlaceLabel(placeLabel)?.let { "Time's up at $it" } ?: "Time's up!"

    internal fun timerDoneText(placeLabel: String?, durationMinutes: Int): String =
        displayPlaceLabel(placeLabel)
            ?.let { "Your ${formatDuration(durationMinutes)} $it timer is complete." }
            ?: "Your ${formatDuration(durationMinutes)} timer is complete."

    internal fun exitQuestionTitle(placeLabel: String?): String =
        displayPlaceLabel(placeLabel)?.let { "Leaving $it?" } ?: "You left the area"

    internal fun exitQuestionText(placeLabel: String?, endsAt: String): String =
        displayPlaceLabel(placeLabel)?.let { "Keep the $it timer? Ends at $endsAt." }
            ?: "Keep the timer? It ends at $endsAt."

    internal fun arrivalQuestionTitle(placeLabel: String?): String =
        displayPlaceLabel(placeLabel)?.let { "Start timer at $it?" } ?: "Start Dwell timer?"

    internal fun arrivalQuestionText(placeLabel: String?, confidenceScore: Int): String {
        val confidence = confidenceScore
            .takeIf { it in 0..100 }
            ?.let { " Confidence $it%." }
            ?: ""
        return displayPlaceLabel(placeLabel)
            ?.let { "Dwell thinks you arrived at $it.$confidence" }
            ?: "Dwell thinks you arrived.$confidence"
    }

    internal fun switchQuestionTitle(newPlaceLabel: String?): String =
        displayPlaceLabel(newPlaceLabel)?.let { "Switch to $it?" } ?: "Switch Dwell place?"

    internal fun switchQuestionText(
        newPlaceLabel: String?,
        currentPlaceLabel: String?,
    ): String {
        val newPlace = displayPlaceLabel(newPlaceLabel)
        val currentPlace = displayPlaceLabel(currentPlaceLabel)
        return when {
            newPlace != null && currentPlace != null -> "Stop $currentPlace and start $newPlace?"
            newPlace != null -> "Start $newPlace and stop the current timer?"
            currentPlace != null -> "Stop $currentPlace and start the new place?"
            else -> "Start the new place and stop the current timer?"
        }
    }

    internal fun notificationIdsClearedBeforeRunningTimer(): Set<Int> =
        setOf(NOTIF_DONE, NOTIF_EXIT, NOTIF_ARRIVAL, NOTIF_CONFLICT)

    internal fun notificationIdsClearedBeforeExitPrompt(): Set<Int> =
        setOf(NOTIF_DONE, NOTIF_ARRIVAL, NOTIF_CONFLICT)

    internal fun notificationIdsClearedBeforeArrivalPrompt(): Set<Int> =
        setOf(NOTIF_DONE, NOTIF_EXIT, NOTIF_CONFLICT)

    internal fun notificationIdsClearedBeforeSwitchPrompt(): Set<Int> =
        setOf(NOTIF_DONE, NOTIF_EXIT, NOTIF_ARRIVAL)

    internal fun notificationIdsClearedAfterSetupRecovery(): Set<Int> =
        setOf(NOTIF_SETUP)

    /**
     * Ongoing live-countdown notification. Wear OS mirrors this to the watch,
     * chronometer and action buttons included.
     */
    fun notifyTimerRunning(c: Context, end: Long) {
        ensureChannels(c)
        val manager = nm(c)
        val timerScope = currentTimerActionScope(c, end)
        val placeLabel = timerPlaceLabel(
            timerPlaceId = Prefs.getTimerPlaceId(c),
            labelForPlaceId = { placeId -> Prefs.getSavedPlace(c, placeId)?.safeLabel },
        )
        val n = Notification.Builder(c, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(timerRunningTitle(placeLabel))
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
                    actionIntent(
                        c,
                        NotificationActionReceiver.ACTION_CANCEL,
                        311,
                        placeId = timerScope?.placeId,
                        timerScope = timerScope,
                    )
                ).build()
            )
            .build()
        notificationIdsClearedBeforeRunningTimer().forEach(manager::cancel)
        manager.notify(NOTIF_RUNNING, n)
    }

    internal fun timerPlaceLabel(
        timerPlaceId: String?,
        labelForPlaceId: (String) -> String?,
    ): String? =
        timerPlaceId
            ?.takeIf { it.isNotBlank() }
            ?.let(labelForPlaceId)

    fun notifyTimerDone(c: Context, durationMinutes: Int) {
        ensureChannels(c)
        val placeLabel = timerPlaceLabel(
            timerPlaceId = Prefs.getTimerPlaceId(c),
            labelForPlaceId = { placeId -> Prefs.getSavedPlace(c, placeId)?.safeLabel },
        )
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(timerDoneTitle(placeLabel))
            .setContentText(timerDoneText(placeLabel, durationMinutes))
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(c))
            .build()
        nm(c).cancel(NOTIF_RUNNING)
        nm(c).cancel(NOTIF_EXIT)
        nm(c).cancel(NOTIF_ARRIVAL)
        nm(c).cancel(NOTIF_CONFLICT)
        nm(c).notify(NOTIF_DONE, n)
    }

    fun notifyExitQuestion(c: Context, end: Long) {
        ensureChannels(c)
        val manager = nm(c)
        val timerScope = currentTimerActionScope(c, end)
        val promptScope = currentPromptActionScope(c)
        val placeId = Prefs.getPromptPlaceId(c).ifBlank { Prefs.getTimerPlaceId(c).ifBlank { null } }
        val placeLabel = placeId?.let { Prefs.getPlace(c, it)?.safeLabel }
        val endsAt = timeText(end)
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(exitQuestionTitle(placeLabel))
            .setContentText(exitQuestionText(placeLabel, endsAt))
            .setAutoCancel(true)
            .setDeleteIntent(
                actionIntent(
                    c,
                    NotificationActionReceiver.ACTION_KEEP,
                    318,
                    placeId,
                    timerScope,
                    promptScope,
                )
            )
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Keep",
                    actionIntent(
                        c,
                        NotificationActionReceiver.ACTION_KEEP,
                        312,
                        placeId,
                        timerScope,
                        promptScope,
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Cancel timer",
                    actionIntent(
                        c,
                        NotificationActionReceiver.ACTION_CANCEL,
                        313,
                        placeId,
                        timerScope,
                        promptScope,
                    )
                ).build()
            )
            .build()
        notificationIdsClearedBeforeExitPrompt().forEach(manager::cancel)
        manager.notify(NOTIF_EXIT, n)
    }

    fun notifyArrivalQuestion(c: Context, confidenceScore: Int) {
        ensureChannels(c)
        val manager = nm(c)
        val promptScope = currentPromptActionScope(c)
        val placeId = Prefs.getPromptPlaceId(c).ifBlank { null }
        val placeLabel = placeId?.let { Prefs.getPlace(c, it)?.safeLabel }
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(arrivalQuestionTitle(placeLabel))
            .setContentText(arrivalQuestionText(placeLabel, confidenceScore))
            .setAutoCancel(true)
            .setDeleteIntent(
                actionIntent(c, NotificationActionReceiver.ACTION_DISMISS_ARRIVAL, 319, placeId, promptScope = promptScope)
            )
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Start timer",
                    actionIntent(c, NotificationActionReceiver.ACTION_START_TIMER, 314, placeId, promptScope = promptScope)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Not now",
                    actionIntent(c, NotificationActionReceiver.ACTION_DISMISS_ARRIVAL, 315, placeId, promptScope = promptScope)
                ).build()
            )
            .build()
        notificationIdsClearedBeforeArrivalPrompt().forEach(manager::cancel)
        manager.notify(NOTIF_ARRIVAL, n)
    }

    fun notifySwitchPlaceQuestion(
        c: Context,
        newPlaceLabel: String,
        currentPlaceLabel: String,
    ) {
        ensureChannels(c)
        val manager = nm(c)
        val timerScope = currentTimerActionScope(c)
        val promptScope = currentPromptActionScope(c)
        val placeId = Prefs.getPromptPlaceId(c).ifBlank { null }
        val n = Notification.Builder(c, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(switchQuestionTitle(newPlaceLabel))
            .setContentText(switchQuestionText(newPlaceLabel, currentPlaceLabel))
            .setAutoCancel(true)
            .setDeleteIntent(
                actionIntent(
                    c,
                    NotificationActionReceiver.ACTION_KEEP_CURRENT,
                    320,
                    placeId,
                    timerScope,
                    promptScope,
                )
            )
            .setContentIntent(openAppIntent(c))
            .addAction(
                Notification.Action.Builder(
                    null, "Switch",
                    actionIntent(
                        c,
                        NotificationActionReceiver.ACTION_SWITCH_TIMER,
                        316,
                        placeId,
                        timerScope,
                        promptScope,
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null, "Keep current",
                    actionIntent(
                        c,
                        NotificationActionReceiver.ACTION_KEEP_CURRENT,
                        317,
                        placeId,
                        timerScope,
                        promptScope,
                    )
                ).build()
            )
            .build()
        notificationIdsClearedBeforeSwitchPrompt().forEach(manager::cancel)
        manager.notify(NOTIF_CONFLICT, n)
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
            .setContentText(setupNeededText())
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(c))
            .build()
        nm(c).notify(NOTIF_SETUP, n)
    }

    internal fun setupNeededText(): String =
        "Open Dwell to restore background location for your monitored place."

    fun clearSetup(c: Context) {
        val manager = nm(c)
        notificationIdsClearedAfterSetupRecovery().forEach(manager::cancel)
    }

    fun clearExitQuestion(c: Context) {
        nm(c).cancel(NOTIF_EXIT)
    }

    fun clearArrivalQuestion(c: Context) {
        nm(c).cancel(NOTIF_ARRIVAL)
        nm(c).cancel(NOTIF_CONFLICT)
    }

    fun clearMonitoringPrompts(c: Context) {
        clearExitQuestion(c)
        clearArrivalQuestion(c)
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
