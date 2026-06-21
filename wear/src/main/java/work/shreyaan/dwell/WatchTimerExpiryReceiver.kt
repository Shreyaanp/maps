package work.shreyaan.dwell

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.wear.tiles.TileService

class WatchTimerExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TIMER_EXPIRED) return
        handleLocalTimerExpiry(context)
    }

    companion object {
        private const val ACTION_TIMER_EXPIRED = "work.shreyaan.dwell.watch.TIMER_EXPIRED"
        private const val REQUEST_CODE = 41

        internal fun shouldScheduleLocalTimerExpiry(timerEnd: Long, now: Long): Boolean =
            timerEnd > now

        internal fun shouldShowLocalTimeUp(
            timerEnd: Long,
            prompt: String,
            now: Long,
        ): Boolean =
            timerEnd in 1..now && prompt != TileStateCalculator.PROMPT_TIME_UP

        fun schedule(context: Context, timerEnd: Long, now: Long = System.currentTimeMillis()) {
            if (!shouldScheduleLocalTimerExpiry(timerEnd, now)) {
                cancel(context)
                return
            }
            val delayMs = timerEnd - now
            val alarmAt = SystemClock.elapsedRealtime() + delayMs
            alarmManager(context).setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                alarmAt,
                pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)!!,
            )
        }

        fun cancel(context: Context) {
            alarmManager(context).cancel(
                pendingIntent(context, PendingIntent.FLAG_NO_CREATE) ?: return
            )
        }

        fun handleLocalTimerExpiry(
            context: Context,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            val prefs = context.getSharedPreferences("dwell", Context.MODE_PRIVATE)
            val timerEnd = prefs.getLong("timer_end", 0L)
            val prompt = prefs.getString("prompt", TileStateCalculator.PROMPT_NONE)
                ?: TileStateCalculator.PROMPT_NONE
            if (!shouldShowLocalTimeUp(timerEnd = timerEnd, prompt = prompt, now = now)) {
                if (shouldScheduleLocalTimerExpiry(timerEnd, now)) {
                    schedule(context, timerEnd, now)
                }
                return false
            }

            prefs.edit()
                .putString("prompt", TileStateCalculator.PROMPT_TIME_UP)
                .putLong("prompt_updated", now)
                .putLong("updated", now)
                .apply()

            WatchNotifications.showTimeUp(
                context,
                placeLabel = WatchDataService.stateNotificationPlaceLabel(
                    prompt = TileStateCalculator.PROMPT_TIME_UP,
                    placeLabel = prefs.getString("place_label", "").orEmpty(),
                    promptPlaceLabel = prefs.getString("prompt_place_label", "").orEmpty(),
                    timerPlaceLabel = prefs.getString("timer_place_label", "").orEmpty(),
                ),
                alert = true,
            )
            TileService.getUpdater(context).requestUpdate(DwellTileService::class.java)
            return true
        }

        private fun alarmManager(context: Context): AlarmManager =
            context.getSystemService(AlarmManager::class.java)

        private fun pendingIntent(context: Context, flags: Int): PendingIntent? =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, WatchTimerExpiryReceiver::class.java).setAction(ACTION_TIMER_EXPIRED),
                flags or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
