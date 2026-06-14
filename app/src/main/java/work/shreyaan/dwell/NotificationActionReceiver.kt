package work.shreyaan.dwell

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_KEEP = "work.shreyaan.dwell.action.KEEP_TIMER"
        const val ACTION_CANCEL = "work.shreyaan.dwell.action.CANCEL_TIMER"
        const val ACTION_START_TIMER = "work.shreyaan.dwell.action.START_TIMER"
        const val ACTION_DISMISS_ARRIVAL = "work.shreyaan.dwell.action.DISMISS_ARRIVAL"
        const val ACTION_SWITCH_TIMER = "work.shreyaan.dwell.action.SWITCH_TIMER"
        const val ACTION_KEEP_CURRENT = "work.shreyaan.dwell.action.KEEP_CURRENT_TIMER"
        const val EXTRA_PLACE_ID = "work.shreyaan.dwell.extra.PLACE_ID"

        internal fun promptPlaceForAction(
            currentPromptPlaceId: String,
            actionPlaceId: String?,
        ): String? {
            val current = currentPromptPlaceId.takeIf { it.isNotBlank() }
            val requested = actionPlaceId?.takeIf { it.isNotBlank() }
            return when {
                requested == null -> current
                requested == current -> requested
                else -> null
            }
        }

        internal fun acceptsScopedTimerAction(
            currentTimerPlaceId: String,
            actionPlaceId: String?,
        ): Boolean {
            val requested = actionPlaceId?.takeIf { it.isNotBlank() } ?: return true
            return currentTimerPlaceId.isNotBlank() && currentTimerPlaceId == requested
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        fun scopedPromptPlace(): String? =
            promptPlaceForAction(
                currentPromptPlaceId = Prefs.getPromptPlaceId(context),
                actionPlaceId = intent.getStringExtra(EXTRA_PLACE_ID),
            )

        fun hasRejectedScopedPrompt(): Boolean =
            intent.hasExtra(EXTRA_PLACE_ID) && scopedPromptPlace() == null

        fun clearScopedPromptAndPush(vararg notificationIds: Int) {
            if (hasRejectedScopedPrompt()) {
                notificationIds.forEach(nm::cancel)
                WearSync.pushState(context)
                return
            }
            Prefs.clearWatchPrompt(context)
            notificationIds.forEach(nm::cancel)
            WearSync.pushState(context)
        }

        fun startPromptPlace(replaceRunning: Boolean) {
            val placeId = scopedPromptPlace()
            if (intent.hasExtra(EXTRA_PLACE_ID) && placeId == null) {
                nm.cancel(Notifications.NOTIF_ARRIVAL)
                nm.cancel(Notifications.NOTIF_CONFLICT)
                WearSync.pushState(context)
                return
            }
            if (TimerController.isRunning(context)) {
                if (!replaceRunning || placeId == null || placeId == Prefs.getTimerPlaceId(context)) {
                    Prefs.clearWatchPrompt(context)
                    nm.cancel(Notifications.NOTIF_ARRIVAL)
                    nm.cancel(Notifications.NOTIF_CONFLICT)
                    WearSync.pushState(context)
                    return
                }
                TimerController.cancelTimer(context)
            }

            TimerController.startTimer(
                context,
                Prefs.getDurationMinutes(context, placeId),
                placeId,
            )
            nm.cancel(Notifications.NOTIF_ARRIVAL)
            nm.cancel(Notifications.NOTIF_CONFLICT)
        }

        when (intent.action) {
            ACTION_KEEP -> {
                clearScopedPromptAndPush(Notifications.NOTIF_EXIT)
            }
            ACTION_CANCEL -> {
                if (
                    !acceptsScopedTimerAction(
                        currentTimerPlaceId = Prefs.getTimerPlaceId(context),
                        actionPlaceId = intent.getStringExtra(EXTRA_PLACE_ID),
                    )
                ) {
                    nm.cancel(Notifications.NOTIF_EXIT)
                    WearSync.pushState(context)
                    return
                }
                TimerController.cancelTimer(context)
                Notifications.notifyTimerCancelled(context)
            }
            ACTION_START_TIMER -> {
                startPromptPlace(replaceRunning = true)
            }
            ACTION_SWITCH_TIMER -> {
                startPromptPlace(replaceRunning = true)
            }
            ACTION_KEEP_CURRENT -> {
                clearScopedPromptAndPush(Notifications.NOTIF_CONFLICT)
            }
            ACTION_DISMISS_ARRIVAL -> {
                clearScopedPromptAndPush(Notifications.NOTIF_ARRIVAL, Notifications.NOTIF_CONFLICT)
            }
        }
    }
}
