package work.shreyaan.dwell

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ArrivalProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.getArmedPlaces(context).isEmpty() || TimerController.isRunning(context)) {
            return
        }

        val placeId = intent.getStringExtra(EXTRA_PLACE_ID)
        val pending = goAsync()
        DwellArrivalEngine.runFollowUpProbe(context.applicationContext, placeId) {
            pending.finish()
        }
    }

    companion object {
        const val EXTRA_PLACE_ID = "work.shreyaan.dwell.extra.PLACE_ID"
        internal const val ACTION_FOLLOW_UP = "work.shreyaan.dwell.action.ARRIVAL_FOLLOW_UP"
        private const val REQUEST_CODE = 460
        private const val DATA_PREFIX = "dwell://arrival-follow-up/"
        private const val LEGACY_DATA_ID = "_legacy"
        private const val FOLLOW_UP_DELAY_MS = 60_000L
        private const val MAX_FOLLOW_UPS = 3

        internal fun requestCodeFor(placeId: String?): Int {
            val scopedId = placeId?.takeIf { it.isNotBlank() } ?: return REQUEST_CODE
            return REQUEST_CODE + (scopedId.hashCode() and 0x7FFF)
        }

        internal fun dataStringFor(placeId: String?): String {
            val scopedId = placeId?.takeIf { it.isNotBlank() } ?: LEGACY_DATA_ID
            return DATA_PREFIX + URLEncoder.encode(scopedId, StandardCharsets.UTF_8.name())
        }

        private fun legacyPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, ArrivalProbeReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun pendingIntent(context: Context, placeId: String? = null): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCodeFor(placeId),
                Intent(context, ArrivalProbeReceiver::class.java).apply {
                    action = ACTION_FOLLOW_UP
                    data = Uri.parse(dataStringFor(placeId))
                    placeId?.takeIf { it.isNotBlank() }?.let {
                        putExtra(EXTRA_PLACE_ID, it)
                    }
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        fun schedule(
            context: Context,
            placeId: String? = Prefs.getPromptPlaceId(context),
        ): Boolean {
            if (!Prefs.markArrivalFollowUpScheduled(context, placeId, MAX_FOLLOW_UPS)) return false
            val triggerAt = System.currentTimeMillis() + FOLLOW_UP_DELAY_MS
            return runCatching {
                context.getSystemService(AlarmManager::class.java)
                    .setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent(context, placeId),
                    )
                true
            }.onFailure { error ->
                Prefs.clearArrivalFollowUp(context, placeId)
                DwellDiagnostics.logLifecycle(
                    context = context,
                    source = "follow-up",
                    decision = "schedule-failed",
                    detail = scheduleFailureDetail(placeId, error),
                )
            }.getOrDefault(false)
        }

        internal fun scheduleFailureDetail(placeId: String?, error: Throwable): String =
            "scoped=${!placeId.isNullOrBlank()} ${error.javaClass.simpleName}: " +
                (error.message ?: "alarm schedule failed").take(120)

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(legacyPendingIntent(context))
            val placeIds = mutableSetOf<String?>(
                null,
                Prefs.getPromptPlaceId(context).ifBlank { null },
            )
            Prefs.getPlaces(context).forEach { placeIds += it.id }
            placeIds.forEach { alarmManager.cancel(pendingIntent(context, it)) }
            Prefs.clearArrivalRuntime(context)
        }
    }
}
