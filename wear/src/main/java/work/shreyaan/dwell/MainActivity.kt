package work.shreyaan.dwell

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchNotifications.ensureChannels(this)
        setContent {
            MaterialTheme {
                WatchScreen()
            }
        }
    }
}

private data class WatchState(
    val hasPlace: Boolean,
    val placeId: String,
    val placeLabel: String,
    val promptPlaceLabel: String,
    val timerPlaceLabel: String,
    val armed: Boolean,
    val needsSetup: Boolean,
    val monitoringError: String,
    val timerEnd: Long,
    val timerStartedAt: Long,
    val timerPlaceId: String,
    val durationMinutes: Int,
    val prompt: String,
    val promptPlaceId: String,
    val promptUpdated: Long,
    val placeCount: Int,
    val armedPlaceCount: Int,
    val registeredPlaceCount: Int,
    val lastUpdated: Long,
)

private const val PROMPT_NONE = "none"
private const val PROMPT_START_TIMER = "start_timer"
private const val PROMPT_LEAVE_EARLY = "leave_early"
private const val PROMPT_TIME_UP = "time_up"
private val placeholderPlaceLabels = setOf(
    "Selected place",
    "Saved place",
    "No place selected",
)

private enum class WatchPage {
    Glance,
    Focus,
    Actions,
}

private fun prefs(c: Context): SharedPreferences =
    c.getSharedPreferences("dwell", Context.MODE_PRIVATE)

private fun hasWatchNotificationPermission(c: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            c,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

private fun requestWatchNotificationPermission(c: Context): Boolean {
    if (hasWatchNotificationPermission(c)) return true
    if (Build.VERSION.SDK_INT < 33) return true
    val activity = c as? Activity ?: return false
    activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
    return true
}

internal fun watchNotificationPermissionActionLabel(canNotify: Boolean): String =
    if (canNotify) "" else "Allow watch alerts"

internal fun watchNotificationPermissionFeedback(opened: Boolean): String =
    if (opened) "Watch alert prompt opened" else "Open watch settings"

internal fun watchReadyTitle(
    hasPlace: Boolean,
    armed: Boolean,
    needsSetup: Boolean,
    lastUpdated: Long = 1L,
    now: Long = lastUpdated,
    livePlaceCount: Int = 0,
): String = when {
    lastUpdated <= 0L -> "Syncing"
    WatchSyncCopy.isStale(lastUpdated, now) -> "Phone not nearby"
    needsSetup -> "Needs setup"
    armed && livePlaceCount > 1 -> "$livePlaceCount places registered"
    armed -> "Monitoring registered"
    hasPlace -> "Monitoring paused"
    else -> "No place yet"
}

internal fun watchReadyDetail(
    hasPlace: Boolean,
    armed: Boolean,
    needsSetup: Boolean,
    lastUpdated: Long = 1L,
    now: Long = lastUpdated,
    livePlaceCount: Int = 0,
): String = when {
    lastUpdated <= 0L -> "Open phone once"
    WatchSyncCopy.isStale(lastUpdated, now) -> "Open phone app"
    needsSetup -> "Finish setup on phone"
    armed && livePlaceCount > 1 -> "Waiting for arrivals"
    armed -> "Waiting for arrival"
    hasPlace -> "Turn on Monitor"
    else -> "Choose a place on phone"
}

private fun readWatchState(c: Context): WatchState {
    val p = prefs(c)
    return WatchState(
        hasPlace = p.getBoolean("has_place", false),
        placeId = p.getString("place_id", "") ?: "",
        placeLabel = p.getString("place_label", "") ?: "",
        promptPlaceLabel = p.getString("prompt_place_label", "") ?: "",
        timerPlaceLabel = p.getString("timer_place_label", "") ?: "",
        armed = p.getBoolean("armed", false),
        needsSetup = p.getBoolean("needs_setup", false),
        monitoringError = p.getString("monitoring_error", "") ?: "",
        timerEnd = p.getLong("timer_end", 0L),
        timerStartedAt = p.getLong("timer_started_at", 0L),
        timerPlaceId = p.getString("timer_place_id", "") ?: "",
        durationMinutes = p.getInt("duration_min", 270),
        prompt = p.getString("prompt", PROMPT_NONE) ?: PROMPT_NONE,
        promptPlaceId = p.getString("prompt_place_id", "") ?: "",
        promptUpdated = p.getLong("prompt_updated", 0L),
        placeCount = p.getInt("place_count", 0),
        armedPlaceCount = p.getInt("armed_place_count", 0),
        registeredPlaceCount = p.getInt("registered_place_count", 0),
        lastUpdated = p.getLong("updated", 0L),
    )
}

private fun persistDataMap(c: Context, map: DataMap): Boolean {
    val p = prefs(c)
    val incomingUpdated = map.getLong("updated", 0L)
    if (!WatchDataService.shouldApplyIncomingState(p.getLong("updated", 0L), incomingUpdated)) {
        return false
    }
    val placeLabel = map.getString("place_label", "")
    val nextEnd = map.getLong("end", 0L)
    val now = System.currentTimeMillis()
    p.edit()
        .putBoolean("has_place", map.getBoolean("has_place", false))
        .putString("place_id", map.getString("place_id", ""))
        .putString("place_label", placeLabel)
        .putString(
            "prompt_place_label",
            map.getString("prompt_place_label", "").ifBlank { placeLabel },
        )
        .putString(
            "timer_place_label",
            map.getString("timer_place_label", "").ifBlank { placeLabel },
        )
        .putBoolean("armed", map.getBoolean("armed", false))
        .putBoolean("needs_setup", map.getBoolean("needs_setup", false))
        .putString("monitoring_error", map.getString("monitoring_error", ""))
        .putLong("timer_end", nextEnd)
        .putLong("timer_started_at", map.getLong("started_at", 0L))
        .putString("timer_place_id", map.getString("timer_place_id", ""))
        .putInt("duration_min", map.getInt("duration_min", 270))
        .putString("prompt", map.getString("prompt", PROMPT_NONE))
        .putString("prompt_place_id", map.getString("prompt_place_id", ""))
        .putLong("prompt_updated", map.getLong("prompt_updated", 0L))
        .putInt("place_count", map.getInt("place_count", 0))
        .putInt("armed_place_count", map.getInt("armed_place_count", 0))
        .putInt("registered_place_count", map.getInt("registered_place_count", 0))
        .putLong("updated", incomingUpdated.takeIf { it > 0L } ?: now)
        .apply()
    if (nextEnd > now) {
        WatchTimerExpiryReceiver.schedule(c, nextEnd, now)
    } else {
        WatchTimerExpiryReceiver.cancel(c)
    }
    return true
}

private suspend fun sendPhoneCommand(
    context: Context,
    path: String,
    payload: String? = null,
): Boolean = runCatching {
    val nodes = Wearable.getNodeClient(context).connectedNodes.await()
    if (nodes.isEmpty()) return@runCatching false

    val data = payload?.toByteArray(Charsets.UTF_8)
    for (node in nodes) {
        Wearable.getMessageClient(context)
            .sendMessage(node.id, path, data)
            .await()
    }
    true
}.getOrDefault(false)

internal fun promptCommandPayload(
    prompt: String,
    promptUpdated: Long,
    promptPlaceId: String,
    timerPlaceId: String,
    timerStartedAt: Long,
    timerEnd: Long,
): String = listOf(
    "prompt",
    prompt,
    promptUpdated.toString(),
    promptPlaceId,
    timerPlaceId,
    timerStartedAt.toString(),
    timerEnd.toString(),
).joinToString("|")

private fun promptCommandPayload(state: WatchState): String =
    promptCommandPayload(
        prompt = state.prompt,
        promptUpdated = state.promptUpdated,
        promptPlaceId = state.promptPlaceId,
        timerPlaceId = state.timerPlaceId,
        timerStartedAt = state.timerStartedAt,
        timerEnd = state.timerEnd,
    )

internal fun timerCommandPayload(
    placeId: String,
    timerStartedAt: Long,
    timerEnd: Long,
    minutes: Int? = null,
): String = listOf(
    placeId,
    timerStartedAt.toString(),
    timerEnd.toString(),
    minutes?.toString().orEmpty(),
).joinToString("|")

private fun timerCommandPayload(state: WatchState, minutes: Int? = null): String =
    timerCommandPayload(
        placeId = state.timerPlaceId,
        timerStartedAt = state.timerStartedAt,
        timerEnd = state.timerEnd,
        minutes = minutes,
    )

internal fun timeUpExtendCommandPayload(
    promptUpdated: Long,
    placeId: String,
    minutes: Int,
): String = listOf(
    "time_up_extend",
    promptUpdated.toString(),
    placeId,
    minutes.toString(),
).joinToString("|")

internal fun extendCommandPayload(
    prompt: String,
    promptUpdated: Long,
    placeId: String,
    timerStartedAt: Long,
    timerEnd: Long,
    minutes: Int,
): String =
    if (prompt == PROMPT_TIME_UP && promptUpdated > 0L) {
        timeUpExtendCommandPayload(
            promptUpdated = promptUpdated,
            placeId = placeId,
            minutes = minutes,
        )
    } else {
        timerCommandPayload(
            placeId = placeId,
            timerStartedAt = timerStartedAt,
            timerEnd = timerEnd,
            minutes = minutes,
        )
    }

private fun extendCommandPayload(state: WatchState, minutes: Int): String =
    extendCommandPayload(
        prompt = state.prompt,
        promptUpdated = state.promptUpdated,
        placeId = state.timerPlaceId,
        timerStartedAt = state.timerStartedAt,
        timerEnd = state.timerEnd,
        minutes = minutes,
    )

internal fun doneCommandPayload(
    prompt: String,
    promptUpdated: Long,
    promptPlaceId: String,
    placeId: String,
    timerStartedAt: Long,
    timerEnd: Long,
): String =
    if (prompt == PROMPT_TIME_UP) {
        promptCommandPayload(
            prompt = prompt,
            promptUpdated = promptUpdated,
            promptPlaceId = promptPlaceId,
            timerPlaceId = placeId,
            timerStartedAt = timerStartedAt,
            timerEnd = timerEnd,
        )
    } else {
        timerCommandPayload(
            placeId = placeId,
            timerStartedAt = timerStartedAt,
            timerEnd = timerEnd,
        )
    }

private fun doneCommandPayload(state: WatchState): String =
    doneCommandPayload(
        prompt = state.prompt,
        promptUpdated = state.promptUpdated,
        promptPlaceId = state.promptPlaceId,
        placeId = state.timerPlaceId,
        timerStartedAt = state.timerStartedAt,
        timerEnd = state.timerEnd,
    )

internal fun watchCommandSentFeedback(sent: Boolean): String =
    if (sent) "Sent to phone" else "Phone not nearby"

internal enum class WatchCommandAction {
    StartTimer,
    DismissArrival,
    KeepTimer,
    CancelTimer,
    MarkDone,
    ExtendTimer,
}

internal fun watchCommandSentFeedback(
    action: WatchCommandAction,
    sent: Boolean,
): String {
    if (!sent) return "Phone not nearby"
    return when (action) {
        WatchCommandAction.StartTimer -> "Start sent to phone"
        WatchCommandAction.DismissArrival -> "Not now sent to phone"
        WatchCommandAction.KeepTimer -> "Keep sent to phone"
        WatchCommandAction.CancelTimer -> "Cancel sent to phone"
        WatchCommandAction.MarkDone -> "Done sent to phone"
        WatchCommandAction.ExtendTimer -> "Extend sent to phone"
    }
}

internal data class WatchPromptVisibility(
    val startPrompt: Boolean,
    val leavingEarly: Boolean,
    val timeUp: Boolean,
)

internal fun watchPromptVisibility(
    prompt: String,
    timerEnd: Long,
    lastUpdated: Long,
    now: Long,
): WatchPromptVisibility {
    val running = timerEnd > now
    val promptFresh = !WatchSyncCopy.isStale(lastUpdated, now)
    val finishedLocally = timerEnd > 0L && timerEnd <= now
    return WatchPromptVisibility(
        startPrompt = prompt == PROMPT_START_TIMER && promptFresh,
        leavingEarly = prompt == PROMPT_LEAVE_EARLY && running && promptFresh,
        timeUp = (prompt == PROMPT_TIME_UP && promptFresh) || finishedLocally,
    )
}

@Composable
fun WatchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(readWatchState(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf("") }
    var feedbackKey by remember { mutableLongStateOf(0L) }
    var notificationsAllowed by remember { mutableStateOf(hasWatchNotificationPermission(context)) }
    val pagerState = rememberPagerState(pageCount = { WatchPage.entries.size })

    fun showFeedback(message: String) {
        feedback = message
        feedbackKey += 1L
    }

    suspend fun requestFreshPhoneState() {
        sendPhoneCommand(context, "/dwell/request_state")
    }

    LaunchedEffect(Unit) {
        runCatching {
            val items = Wearable.getDataClient(context).dataItems.await()
            for (item in items) {
                if (item.uri.path == "/dwell/state") {
                    persistDataMap(context, DataMapItem.fromDataItem(item).dataMap)
                }
            }
            items.release()
        }
        sendPhoneCommand(context, "/dwell/request_state")
        while (true) {
            now = System.currentTimeMillis()
            state = readWatchState(context)
            notificationsAllowed = hasWatchNotificationPermission(context)
            delay(1000)
        }
    }

    LaunchedEffect(feedbackKey) {
        if (feedback.isNotBlank()) {
            val shownKey = feedbackKey
            delay(2_500)
            if (feedbackKey == shownKey) {
                feedback = ""
            }
        }
    }

    fun cancelTimer() {
        showFeedback("Cancelling...")
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/cancel", timerCommandPayload(state))
            if (sent) {
                requestFreshPhoneState()
            }
            showFeedback(watchCommandSentFeedback(WatchCommandAction.CancelTimer, sent))
        }
    }

    fun startArrivalTimer() {
        showFeedback("Starting...")
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/start", promptCommandPayload(state))
            if (sent) {
                requestFreshPhoneState()
            }
            showFeedback(watchCommandSentFeedback(WatchCommandAction.StartTimer, sent))
        }
    }

    fun dismissArrival() {
        showFeedback("Not now")
        val payload = promptCommandPayload(state)
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/dismiss_arrival", payload)
            if (sent) {
                requestFreshPhoneState()
            }
            showFeedback(watchCommandSentFeedback(WatchCommandAction.DismissArrival, sent))
        }
    }

    fun keepTimer() {
        showFeedback("Keeping timer...")
        val payload = promptCommandPayload(state)
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/keep", payload)
            if (sent) {
                requestFreshPhoneState()
            }
            showFeedback(watchCommandSentFeedback(WatchCommandAction.KeepTimer, sent))
        }
    }

    fun markDone() {
        showFeedback("Marking done...")
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/done", doneCommandPayload(state))
            if (sent) {
                requestFreshPhoneState()
            }
            showFeedback(watchCommandSentFeedback(WatchCommandAction.MarkDone, sent))
        }
    }

    fun extendTimer(minutes: Int) {
        showFeedback("Extending...")
        scope.launch {
            val sent = sendPhoneCommand(
                context,
                "/dwell/extend",
                extendCommandPayload(state, minutes),
            )
            if (sent) {
                requestFreshPhoneState()
            }
            showFeedback(watchCommandSentFeedback(WatchCommandAction.ExtendTimer, sent))
        }
    }

    val running = state.timerEnd > now
    val promptVisibility = watchPromptVisibility(
        prompt = state.prompt,
        timerEnd = state.timerEnd,
        lastUpdated = state.lastUpdated,
        now = now,
    )
    val startPrompt = promptVisibility.startPrompt
    val leavingEarly = promptVisibility.leavingEarly
    val timeUp = promptVisibility.timeUp

    BackHandler(enabled = pagerState.currentPage != WatchPage.Glance.ordinal) {
        scope.launch { pagerState.animateScrollToPage(WatchPage.Glance.ordinal) }
    }

    LaunchedEffect(running, startPrompt, leavingEarly, timeUp) {
        if (!running || startPrompt || leavingEarly || timeUp) {
            pagerState.scrollToPage(WatchPage.Glance.ordinal)
        }
    }

    val currentPage = WatchPage.entries[pagerState.currentPage]

    Scaffold(timeText = { if (currentPage != WatchPage.Focus) TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(
                    horizontal = if (currentPage == WatchPage.Focus) 4.dp else 14.dp,
                    vertical = if (currentPage == WatchPage.Focus) 4.dp else 12.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                startPrompt -> StartTimerPromptContent(
                    state = state,
                    switching = running,
                    feedback = feedback,
                    onStart = { startArrivalTimer() },
                    onDismiss = { dismissArrival() },
                )
                leavingEarly -> LeavingEarlyContent(
                    state = state,
                    feedback = feedback,
                    onKeep = { keepTimer() },
                    onCancel = { cancelTimer() },
                )
                timeUp -> TimeUpContent(
                    state = state,
                    feedback = feedback,
                    onDone = { markDone() },
                    onExtend = { extendTimer(it) },
                )
                running -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) { pageIndex ->
                    when (WatchPage.entries[pageIndex]) {
                        WatchPage.Glance -> ActiveGlanceContent(
                            state = state,
                            now = now,
                            feedback = feedback,
                            onExtend = { extendTimer(30) },
                            onOpenFocus = {
                                scope.launch { pagerState.animateScrollToPage(WatchPage.Focus.ordinal) }
                            },
                            onOpenActions = {
                                scope.launch { pagerState.animateScrollToPage(WatchPage.Actions.ordinal) }
                            },
                        )
                        WatchPage.Focus -> FocusTimerContent(
                            state = state,
                            now = now,
                            onOpenActions = {
                                scope.launch { pagerState.animateScrollToPage(WatchPage.Actions.ordinal) }
                            },
                        )
                        WatchPage.Actions -> TimerActionsContent(
                            state = state,
                            feedback = feedback,
                            onExtend = { extendTimer(it) },
                            onCancel = { cancelTimer() },
                            onOpenTimer = {
                                scope.launch { pagerState.animateScrollToPage(WatchPage.Focus.ordinal) }
                            },
                        )
                    }
                }
                else -> ReadyContent(
                    state = state,
                    now = now,
                    feedback = feedback,
                    notificationsAllowed = notificationsAllowed,
                    onAllowNotifications = {
                        val opened = requestWatchNotificationPermission(context)
                        showFeedback(watchNotificationPermissionFeedback(opened))
                    },
                )
            }
        }
    }
}

@Composable
private fun StartTimerPromptContent(
    state: WatchState,
    switching: Boolean,
    feedback: String,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val placeLabel = watchPromptDisplayPlaceLabel(
        promptPlaceLabel = state.promptPlaceLabel,
        placeLabel = state.placeLabel,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusDot(active = true)
        Spacer(Modifier.height(10.dp))
        Text(
            startPromptTitle(placeLabel, switching),
            style = MaterialTheme.typography.title2,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            startPromptSubtitle(placeLabel, switching),
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        StatusText(feedback.ifBlank { "${formatDurationMinutes(state.durationMinutes)} default" })
        Spacer(Modifier.height(10.dp))
        MiniPill(
            label = if (switching) "Switch timer" else "Start timer",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            important = true,
        )
        Spacer(Modifier.height(6.dp))
        MiniPill(
            label = "Not now",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActiveGlanceContent(
    state: WatchState,
    now: Long,
    feedback: String,
    onExtend: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenActions: () -> Unit,
) {
    val left = (state.timerEnd - now).coerceAtLeast(0L)
    val placeLabel = watchTimerDisplayPlaceLabel(
        timerPlaceLabel = state.timerPlaceLabel,
        placeLabel = state.placeLabel,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusDot(active = true)
        Spacer(Modifier.height(8.dp))
        Text(
            activeTimerTitle(placeLabel),
            style = MaterialTheme.typography.title2,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            activeTimerSubtitle(placeLabel),
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            formatRemaining(left),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            "Ends ${formatTime(state.timerEnd)}",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
        )
        StatusText(
            feedback.ifBlank {
                WatchSyncCopy.syncText(
                    lastUpdated = state.lastUpdated,
                    now = now,
                    activeTimer = true,
                )
            },
        )
        Spacer(Modifier.height(7.dp))
        PageDots(current = 0, total = 3)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MiniPill("+30m", onExtend, Modifier.weight(1f), important = true)
            MiniPill("Actions", onOpenActions, Modifier.weight(1f))
        }
        Spacer(Modifier.height(5.dp))
        MiniPill("Timer face", onOpenFocus, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FocusTimerContent(
    state: WatchState,
    now: Long,
    onOpenActions: () -> Unit,
) {
    val left = (state.timerEnd - now).coerceAtLeast(0L)
    val total = totalTimerMillis(state)
    val progress = if (total > 0L) left.toFloat() / total.toFloat() else 0f
    val placeLabel = watchTimerDisplayPlaceLabel(
        timerPlaceLabel = state.timerPlaceLabel,
        placeLabel = state.placeLabel,
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        BigTimerFace(
            progress = progress.coerceIn(0f, 1f),
            primary = formatRemaining(left),
            place = watchTimerPlaceLabel(placeLabel, fallback = "Dwell"),
            footer = "Ends ${formatTime(state.timerEnd)}",
            modifier = Modifier.fillMaxSize(),
            onClick = onOpenActions,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        ) {
            PageDots(current = 1, total = 3)
        }
    }
}

@Composable
private fun TimerActionsContent(
    state: WatchState,
    feedback: String,
    onExtend: (Int) -> Unit,
    onCancel: () -> Unit,
    onOpenTimer: () -> Unit,
) {
    val placeLabel = watchTimerDisplayPlaceLabel(
        timerPlaceLabel = state.timerPlaceLabel,
        placeLabel = state.placeLabel,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Extend", style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold)
        Text(
            watchTimerPlaceLabel(placeLabel, fallback = "Dwell timer"),
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        StatusText(feedback.ifBlank { "Ends ${formatTime(state.timerEnd)}" })
        Spacer(Modifier.height(8.dp))
        ExtendRow(onExtend = onExtend)
        Spacer(Modifier.height(8.dp))
        PageDots(current = 2, total = 3)
        Spacer(Modifier.height(8.dp))
        MiniPill(
            label = "Cancel timer",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        MiniPill(
            label = "Timer",
            onClick = onOpenTimer,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LeavingEarlyContent(
    state: WatchState,
    feedback: String,
    onKeep: () -> Unit,
    onCancel: () -> Unit,
) {
    val placeLabel = watchTimerDisplayPlaceLabel(
        timerPlaceLabel = state.timerPlaceLabel,
        placeLabel = state.placeLabel,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusDot(active = true)
        Spacer(Modifier.height(10.dp))
        Text(
            "Leaving ${placeLabel.shortPlace()}?",
            style = MaterialTheme.typography.title2,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            "Timer ends ${formatTime(state.timerEnd)}",
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
        StatusText(feedback.ifBlank { "Keep is safest for GPS drift" })
        Spacer(Modifier.height(10.dp))
        MiniPill(
            label = "Keep timer",
            onClick = onKeep,
            modifier = Modifier.fillMaxWidth(),
            important = true,
        )
        Spacer(Modifier.height(6.dp))
        MiniPill(
            label = "Cancel timer",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TimeUpContent(
    state: WatchState,
    feedback: String,
    onDone: () -> Unit,
    onExtend: (Int) -> Unit,
) {
    val placeLabel = watchTimerDisplayPlaceLabel(
        timerPlaceLabel = state.timerPlaceLabel,
        placeLabel = state.placeLabel,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            timeUpScreenTitle(placeLabel),
            style = MaterialTheme.typography.title1,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            timeUpScreenSubtitle(placeLabel),
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        StatusText(feedback.ifBlank { "Extend or mark done" })
        Spacer(Modifier.height(10.dp))
        MiniPill(
            label = "Extend 30m",
            onClick = { onExtend(30) },
            modifier = Modifier.fillMaxWidth(),
            important = true,
        )
        Spacer(Modifier.height(6.dp))
        MiniPill(
            label = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            important = true,
        )
    }
}

@Composable
private fun ReadyContent(
    state: WatchState,
    now: Long,
    feedback: String,
    notificationsAllowed: Boolean,
    onAllowNotifications: () -> Unit,
) {
    val title = watchReadyTitle(
        hasPlace = state.hasPlace,
        armed = state.armed,
        needsSetup = state.needsSetup,
        lastUpdated = state.lastUpdated,
        now = now,
        livePlaceCount = state.registeredPlaceCount,
    )
    val detail = watchReadyDetail(
        hasPlace = state.hasPlace,
        armed = state.armed,
        needsSetup = state.needsSetup,
        lastUpdated = state.lastUpdated,
        now = now,
        livePlaceCount = state.registeredPlaceCount,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusDot(active = state.armed && !state.needsSetup)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold)
        Text(
            detail,
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
        if (state.hasPlace) {
            Spacer(Modifier.height(8.dp))
            Text(
                watchReadyPlaceLabel(state.placeLabel),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                readyMetaText(state),
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        StatusText(
            feedback.ifBlank {
                WatchSyncCopy.syncText(
                    lastUpdated = state.lastUpdated,
                    now = now,
                    activeTimer = false,
                )
            },
        )
        val notificationAction = watchNotificationPermissionActionLabel(notificationsAllowed)
        if (notificationAction.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            MiniPill(
                label = notificationAction,
                onClick = onAllowNotifications,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TimerRing(
    progress: Float,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val active = MaterialTheme.colors.primary
    val track = MaterialTheme.colors.onSurface.copy(alpha = 0.16f)
    val ringModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)

    Box(contentAlignment = Alignment.Center, modifier = ringModifier) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            drawCircle(
                color = track,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = active,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                primary,
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                secondary,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BigTimerFace(
    progress: Float,
    primary: String,
    place: String,
    footer: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val active = MaterialTheme.colors.primary
    val track = MaterialTheme.colors.onSurface.copy(alpha = 0.14f)
    val faceModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)

    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = faceModifier) {
        val ringSize = (minOf(maxWidth, maxHeight) - 10.dp).coerceAtLeast(148.dp)
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 9.dp.toPx()
            drawCircle(
                color = track,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = active,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(ringSize)
                .padding(horizontal = 22.dp),
        ) {
            Text(
                primary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                place.shortPlace(),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                footer,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PageDots(
    current: Int,
    total: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == current) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                    ),
            )
        }
    }
}

@Composable
private fun ExtendRow(onExtend: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(15, 30, 60).forEach { minutes ->
            MiniPill(
                label = "+${minutes}m",
                onClick = { onExtend(minutes) },
                modifier = Modifier.weight(1f),
                important = minutes == 30,
            )
        }
    }
}

@Composable
private fun MiniPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    important: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(
                if (important) MaterialTheme.colors.primary
                else MaterialTheme.colors.surface,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.caption1,
            fontWeight = FontWeight.Bold,
            color = if (important) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colors.primary
                else MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
            ),
    )
}

@Composable
private fun StatusText(text: String) {
    if (text.isBlank()) return
    Spacer(Modifier.height(4.dp))
    Text(
        text,
        style = MaterialTheme.typography.caption2,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun totalTimerMillis(state: WatchState): Long {
    if (state.timerStartedAt in 1 until state.timerEnd) {
        return state.timerEnd - state.timerStartedAt
    }
    return state.durationMinutes.coerceAtLeast(1) * 60_000L
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}:${minutes.toString().padStart(2, '0')}"
        minutes > 0 -> "${minutes}:${seconds.toString().padStart(2, '0')}"
        else -> "${seconds}s"
    }
}

private fun formatDurationMinutes(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(1)
    val hours = safeMinutes / 60
    val remainingMinutes = safeMinutes % 60
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
        hours > 0 -> "${hours}h"
        else -> "${safeMinutes}m"
    }
}

internal fun startPromptTitle(placeLabel: String, switching: Boolean): String {
    val place = placeLabel.shortPlaceOrBlank()
    return when {
        switching && place.isNotBlank() -> "Switch to $place?"
        switching -> "Switch timer?"
        place.isNotBlank() -> "Start at $place?"
        else -> "Start timer?"
    }
}

internal fun startPromptSubtitle(placeLabel: String, switching: Boolean): String {
    val place = placeLabel.shortPlaceOrBlank()
    return when {
        switching && place.isNotBlank() -> "New timer place"
        switching -> "Choose new timer"
        place.isNotBlank() -> place
        else -> "Arrived"
    }
}

internal fun watchPromptDisplayPlaceLabel(
    promptPlaceLabel: String,
    placeLabel: String,
): String =
    promptPlaceLabel.ifBlank { placeLabel }

internal fun watchTimerDisplayPlaceLabel(
    timerPlaceLabel: String,
    placeLabel: String,
): String =
    timerPlaceLabel.ifBlank { placeLabel }

internal fun timeUpScreenTitle(placeLabel: String): String {
    val place = placeLabel.shortPlaceOrBlank()
    return if (place.isNotBlank()) "Time's up at $place" else "Time's up"
}

internal fun timeUpScreenSubtitle(placeLabel: String): String =
    if (placeLabel.shortPlaceOrBlank().isNotBlank()) "Timer complete" else "Dwell timer"

internal fun activeTimerTitle(placeLabel: String): String {
    val place = placeLabel.shortPlaceOrBlank()
    return if (place.isNotBlank()) "$place timer" else "Timer active"
}

internal fun activeTimerSubtitle(placeLabel: String): String =
    if (placeLabel.shortPlaceOrBlank().isNotBlank()) "Timer active" else "Dwell timer"

internal fun watchReadyPlaceLabel(placeLabel: String): String =
    placeLabel.shortPlace()

internal fun watchTimerPlaceLabel(placeLabel: String, fallback: String): String =
    placeLabel.shortPlaceOrBlank().ifBlank { fallback }

private fun readyMetaText(state: WatchState): String =
    watchReadyMetaText(
        needsSetup = state.needsSetup,
        monitoringError = state.monitoringError,
        registeredPlaceCount = state.registeredPlaceCount,
        armedPlaceCount = state.armedPlaceCount,
        durationMinutes = state.durationMinutes,
    )

internal fun watchReadyMetaText(
    needsSetup: Boolean,
    monitoringError: String,
    registeredPlaceCount: Int,
    armedPlaceCount: Int,
    durationMinutes: Int,
): String =
    when {
        needsSetup && monitoringError.isNotBlank() -> monitoringError
        needsSetup -> "Needs setup"
        registeredPlaceCount > 1 -> "$registeredPlaceCount places registered"
        armedPlaceCount > 1 -> "$armedPlaceCount places monitoring"
        else -> "${formatDurationMinutes(durationMinutes)} default"
    }

private fun formatTime(timeMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timeMillis))

private fun String.shortPlaceOrBlank(): String =
    substringBefore(",")
        .trim()
        .takeUnless { placeholderPlaceLabels.contains(it) }
        .orEmpty()

private fun String.shortPlace(): String =
    ifBlank { "this place" }
        .substringBefore(",")
        .trim()
        .takeUnless { placeholderPlaceLabels.contains(it) }
        .orEmpty()
        .ifBlank { "this place" }
