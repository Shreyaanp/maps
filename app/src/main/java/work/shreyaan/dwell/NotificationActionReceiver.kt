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
        const val EXTRA_TIMER_PLACE_ID = "work.shreyaan.dwell.extra.TIMER_PLACE_ID"
        const val EXTRA_TIMER_STARTED_AT = "work.shreyaan.dwell.extra.TIMER_STARTED_AT"
        const val EXTRA_TIMER_END = "work.shreyaan.dwell.extra.TIMER_END"
        const val EXTRA_PROMPT = "work.shreyaan.dwell.extra.PROMPT"
        const val EXTRA_PROMPT_UPDATED = "work.shreyaan.dwell.extra.PROMPT_UPDATED"

        internal fun promptPlaceForAction(
            currentPromptPlaceId: String,
            actionPlaceId: String?,
        ): String? {
            val current = currentPromptPlaceId.takeIf { it.isNotBlank() }
            val requested = actionPlaceId?.takeIf { it.isNotBlank() }
            return when {
                requested == current -> requested
                else -> null
            }
        }

        internal fun acceptsScopedPromptAction(
            currentPrompt: String,
            currentPromptPlaceId: String,
            currentPromptUpdated: Long,
            actionPrompt: String?,
            actionPromptUpdated: Long,
            actionPlaceId: String?,
        ): Boolean {
            val requestedPrompt = actionPrompt?.takeIf { it.isNotBlank() }
            val currentPlace = currentPromptPlaceId.takeIf { it.isNotBlank() }
            val requestedPlace = actionPlaceId?.takeIf { it.isNotBlank() }
            if (requestedPrompt == null || actionPromptUpdated <= 0L) return false
            if (currentPlace != null && requestedPlace != currentPlace) return false
            if (currentPlace == null && requestedPlace != null) return false
            if (requestedPrompt != currentPrompt || currentPrompt == Prefs.WATCH_PROMPT_NONE) return false
            if (currentPromptUpdated <= 0L || actionPromptUpdated <= 0L) return false
            return currentPromptUpdated == actionPromptUpdated
        }

        internal fun acceptsScopedTimerAction(
            currentTimerPlaceId: String,
            currentTimerStartedAt: Long,
            currentTimerEnd: Long,
            actionTimerPlaceId: String?,
            actionTimerStartedAt: Long,
            actionTimerEnd: Long,
        ): Boolean {
            if (currentTimerStartedAt <= 0L || currentTimerEnd <= 0L) return false
            if (actionTimerStartedAt <= 0L || actionTimerEnd <= 0L) return false
            if (currentTimerStartedAt != actionTimerStartedAt || currentTimerEnd != actionTimerEnd) {
                return false
            }
            val requestedPlace = actionTimerPlaceId?.takeIf { it.isNotBlank() }
            return when {
                currentTimerPlaceId.isBlank() -> requestedPlace == null
                requestedPlace == null -> false
                else -> currentTimerPlaceId == requestedPlace
            }
        }

        internal fun acceptsScopedCancelAction(
            timerScopeAccepted: Boolean,
            promptScopePresent: Boolean,
            promptScopeAccepted: Boolean,
        ): Boolean =
            timerScopeAccepted && (!promptScopePresent || promptScopeAccepted)

        internal fun acceptsScopedPromptTimerAction(
            promptScopeAccepted: Boolean,
            timerScopePresent: Boolean,
            timerScopeAccepted: Boolean,
        ): Boolean =
            promptScopeAccepted && timerScopePresent && timerScopeAccepted
    }

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        fun scopedPromptPlace(): String? =
            promptPlaceForAction(
                currentPromptPlaceId = Prefs.getPromptPlaceId(context),
                actionPlaceId = intent.getStringExtra(EXTRA_PLACE_ID),
            )

        fun acceptsPromptScope(): Boolean =
            acceptsScopedPromptAction(
                currentPrompt = Prefs.getWatchPrompt(context),
                currentPromptPlaceId = Prefs.getPromptPlaceId(context),
                currentPromptUpdated = Prefs.getWatchPromptUpdated(context),
                actionPrompt = intent.getStringExtra(EXTRA_PROMPT),
                actionPromptUpdated = intent.getLongExtra(EXTRA_PROMPT_UPDATED, 0L),
                actionPlaceId = intent.getStringExtra(EXTRA_PLACE_ID),
            )

        fun hasRejectedScopedPrompt(): Boolean =
            !acceptsPromptScope()

        fun hasPromptScope(): Boolean =
            intent.hasExtra(EXTRA_PROMPT) ||
                intent.hasExtra(EXTRA_PROMPT_UPDATED)

        fun hasTimerScope(): Boolean =
            intent.hasExtra(EXTRA_TIMER_STARTED_AT) ||
                intent.hasExtra(EXTRA_TIMER_END) ||
                intent.hasExtra(EXTRA_TIMER_PLACE_ID)

        fun acceptsTimerScope(): Boolean =
            hasTimerScope() &&
                acceptsScopedTimerAction(
                    currentTimerPlaceId = Prefs.getTimerPlaceId(context),
                    currentTimerStartedAt = Prefs.getTimerStartedAt(context),
                    currentTimerEnd = Prefs.getTimerEnd(context),
                    actionTimerPlaceId = intent.getStringExtra(EXTRA_TIMER_PLACE_ID),
                    actionTimerStartedAt = intent.getLongExtra(EXTRA_TIMER_STARTED_AT, 0L),
                    actionTimerEnd = intent.getLongExtra(EXTRA_TIMER_END, 0L),
                )

        fun rejectStaleTimerScope(): Boolean {
            if (acceptsTimerScope()) return false
            WearSync.pushState(context)
            return true
        }

        fun acceptsCancelAction(): Boolean =
            acceptsScopedCancelAction(
                timerScopeAccepted = acceptsTimerScope(),
                promptScopePresent = hasPromptScope(),
                promptScopeAccepted = acceptsPromptScope(),
            )

        fun acceptsPromptTimerAction(): Boolean =
            acceptsScopedPromptTimerAction(
                promptScopeAccepted = acceptsPromptScope(),
                timerScopePresent = hasTimerScope(),
                timerScopeAccepted = acceptsTimerScope(),
            )

        fun clearScopedPromptAndPush(vararg notificationIds: Int) {
            if (hasRejectedScopedPrompt()) {
                WearSync.pushState(context)
                return
            }
            Prefs.clearWatchPrompt(context)
            notificationIds.forEach(nm::cancel)
            WearSync.pushState(context)
        }

        fun startPromptPlace(
            replaceRunning: Boolean,
            requireCurrentTimerScope: Boolean = false,
        ) {
            val placeId = scopedPromptPlace()
            if (!acceptsPromptScope()) {
                WearSync.pushState(context)
                return
            }
            if (
                !Prefs.promptPlaceStillExists(
                    placeId = placeId,
                    placeExists = Prefs.hasSavedPlaceId(context, placeId),
                )
            ) {
                Prefs.clearWatchPrompt(context)
                nm.cancel(Notifications.NOTIF_ARRIVAL)
                nm.cancel(Notifications.NOTIF_CONFLICT)
                WearSync.pushState(context)
                return
            }
            if (TimerController.isRunning(context)) {
                if (
                    requireCurrentTimerScope &&
                    rejectStaleTimerScope()
                ) {
                    return
                }
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
                placeId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Prefs.getDurationMinutes(context, it) }
                    ?: Prefs.getDefaultDurationMinutes(context),
                placeId ?: "",
            )
            nm.cancel(Notifications.NOTIF_ARRIVAL)
            nm.cancel(Notifications.NOTIF_CONFLICT)
        }

        when (intent.action) {
            ACTION_KEEP -> {
                if (!acceptsPromptTimerAction()) {
                    WearSync.pushState(context)
                    return
                }
                Prefs.markExitPromptKept(
                    context,
                    placeId = scopedPromptPlace(),
                    untilMillis = Prefs.getTimerEnd(context),
                )
                clearScopedPromptAndPush(Notifications.NOTIF_EXIT)
            }
            ACTION_CANCEL -> {
                if (!acceptsCancelAction()) {
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
                startPromptPlace(replaceRunning = true, requireCurrentTimerScope = true)
            }
            ACTION_KEEP_CURRENT -> {
                if (!acceptsPromptTimerAction()) {
                    WearSync.pushState(context)
                    return
                }
                Prefs.markSwitchPromptKept(
                    context,
                    targetPlaceId = scopedPromptPlace(),
                    untilMillis = Prefs.getTimerEnd(context),
                )
                clearScopedPromptAndPush(Notifications.NOTIF_CONFLICT)
            }
            ACTION_DISMISS_ARRIVAL -> {
                clearScopedPromptAndPush(Notifications.NOTIF_ARRIVAL, Notifications.NOTIF_CONFLICT)
            }
        }
    }
}
