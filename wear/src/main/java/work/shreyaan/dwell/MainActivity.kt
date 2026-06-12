package work.shreyaan.dwell

import android.Manifest
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
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
        }
        setContent {
            MaterialTheme {
                WatchScreen()
            }
        }
    }
}

private data class WatchState(
    val hasPlace: Boolean,
    val placeLabel: String,
    val armed: Boolean,
    val timerEnd: Long,
    val timerStartedAt: Long,
    val durationMinutes: Int,
    val prompt: String,
    val promptUpdated: Long,
    val lastUpdated: Long,
)

private const val PROMPT_NONE = "none"
private const val PROMPT_LEAVE_EARLY = "leave_early"
private const val PROMPT_TIME_UP = "time_up"

private enum class WatchPage {
    Glance,
    Focus,
    Actions,
}

private fun prefs(c: Context): SharedPreferences =
    c.getSharedPreferences("dwell", Context.MODE_PRIVATE)

private fun readWatchState(c: Context): WatchState {
    val p = prefs(c)
    return WatchState(
        hasPlace = p.getBoolean("has_place", false),
        placeLabel = p.getString("place_label", "") ?: "",
        armed = p.getBoolean("armed", false),
        timerEnd = p.getLong("timer_end", 0L),
        timerStartedAt = p.getLong("timer_started_at", 0L),
        durationMinutes = p.getInt("duration_min", 270),
        prompt = p.getString("prompt", PROMPT_NONE) ?: PROMPT_NONE,
        promptUpdated = p.getLong("prompt_updated", 0L),
        lastUpdated = p.getLong("updated", 0L),
    )
}

private fun persistDataMap(c: Context, map: DataMap) {
    prefs(c).edit()
        .putBoolean("has_place", map.getBoolean("has_place", false))
        .putString("place_label", map.getString("place_label", ""))
        .putBoolean("armed", map.getBoolean("armed", false))
        .putLong("timer_end", map.getLong("end", 0L))
        .putLong("timer_started_at", map.getLong("started_at", 0L))
        .putInt("duration_min", map.getInt("duration_min", 270))
        .putString("prompt", map.getString("prompt", PROMPT_NONE))
        .putLong("prompt_updated", map.getLong("prompt_updated", 0L))
        .putLong("updated", map.getLong("updated", System.currentTimeMillis()))
        .apply()
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

@Composable
fun WatchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(readWatchState(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf("") }
    var feedbackKey by remember { mutableLongStateOf(0L) }
    val pagerState = rememberPagerState(pageCount = { WatchPage.entries.size })

    fun showFeedback(message: String) {
        feedback = message
        feedbackKey += 1L
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
            val sent = sendPhoneCommand(context, "/dwell/cancel")
            showFeedback(if (sent) "Cancelled" else "Phone not nearby")
            if (sent) {
                prefs(context).edit()
                    .putLong("timer_end", 0L)
                    .putLong("timer_started_at", 0L)
                    .apply()
                state = readWatchState(context)
            }
        }
    }

    fun keepTimer() {
        showFeedback("Keeping timer...")
        prefs(context).edit()
            .putString("prompt", PROMPT_NONE)
            .putLong("prompt_updated", System.currentTimeMillis())
            .apply()
        state = readWatchState(context)
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/keep")
            showFeedback(if (sent) "Keeping timer" else "Still counting down")
        }
    }

    fun markDone() {
        showFeedback("Marking done...")
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/done")
            showFeedback(if (sent) "Done" else "Phone not nearby")
            if (sent) {
                prefs(context).edit()
                    .putString("prompt", PROMPT_NONE)
                    .putLong("prompt_updated", System.currentTimeMillis())
                    .putLong("timer_end", 0L)
                    .putLong("timer_started_at", 0L)
                    .apply()
                WatchNotifications.clearTimer(context)
                state = readWatchState(context)
            }
        }
    }

    fun extendTimer(minutes: Int) {
        showFeedback("Extending...")
        scope.launch {
            val sent = sendPhoneCommand(context, "/dwell/extend", minutes.toString())
            showFeedback(if (sent) "+${minutes}m added" else "Phone not nearby")
            if (sent) {
                val base = maxOf(state.timerEnd, System.currentTimeMillis())
                prefs(context).edit()
                    .putLong("timer_end", base + minutes * 60_000L)
                    .putString("prompt", PROMPT_NONE)
                    .putLong("prompt_updated", System.currentTimeMillis())
                    .apply()
                state = readWatchState(context)
            }
        }
    }

    val running = state.timerEnd > now
    val finishedLocally = state.timerEnd > 0L && state.timerEnd <= now
    val leavingEarly = state.prompt == PROMPT_LEAVE_EARLY && running
    val timeUp = state.prompt == PROMPT_TIME_UP || finishedLocally

    BackHandler(enabled = pagerState.currentPage != WatchPage.Glance.ordinal) {
        scope.launch { pagerState.animateScrollToPage(WatchPage.Glance.ordinal) }
    }

    LaunchedEffect(running, leavingEarly, timeUp) {
        if (!running || leavingEarly || timeUp) {
            pagerState.scrollToPage(WatchPage.Glance.ordinal)
        }
    }

    val currentPage = WatchPage.entries[pagerState.currentPage]

    Scaffold(timeText = { if (currentPage != WatchPage.Focus) TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
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
                )
            }
        }
    }
}

@Composable
private fun ActiveGlanceContent(
    state: WatchState,
    now: Long,
    feedback: String,
    onOpenFocus: () -> Unit,
    onOpenActions: () -> Unit,
) {
    val left = (state.timerEnd - now).coerceAtLeast(0L)
    val total = totalTimerMillis(state)
    val progress = if (total > 0L) left.toFloat() / total.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TimerRing(
            progress = progress.coerceIn(0f, 1f),
            primary = formatRemaining(left),
            secondary = state.placeLabel.ifBlank { "Dwell" },
            modifier = Modifier.size(118.dp),
            onClick = onOpenFocus,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "Ends ${formatTime(state.timerEnd)}",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
        )
        if (feedback.isNotBlank()) {
            StatusText(feedback)
        }
        Spacer(Modifier.height(8.dp))
        PageDots(current = 0, total = 3)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MiniPill("Timer", onOpenFocus, Modifier.weight(1f))
            MiniPill("Actions", onOpenActions, Modifier.weight(1f))
        }
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BigTimerFace(
            progress = progress.coerceIn(0f, 1f),
            primary = formatRemaining(left),
            secondary = "Ends ${formatTime(state.timerEnd)}",
        )
        Spacer(Modifier.height(7.dp))
        PageDots(current = 1, total = 3)
        Spacer(Modifier.height(7.dp))
        MiniPill(
            label = "Actions",
            onClick = onOpenActions,
            modifier = Modifier.fillMaxWidth(),
        )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Actions", style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold)
        Text(
            state.placeLabel.ifBlank { "Dwell timer" },
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        StatusText(feedback.ifBlank { "Ends ${formatTime(state.timerEnd)}" })
        Spacer(Modifier.height(8.dp))
        ExtendRow(onExtend = onExtend)
        Spacer(Modifier.height(6.dp))
        MiniPill(
            label = "Cancel timer",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        PageDots(current = 2, total = 3)
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusDot(active = true)
        Spacer(Modifier.height(10.dp))
        Text(
            "Leaving ${state.placeLabel.shortPlace()}?",
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Time's up", style = MaterialTheme.typography.title1, fontWeight = FontWeight.Bold)
        Text(
            state.placeLabel.ifBlank { "Dwell timer" },
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
        )
        Spacer(Modifier.height(6.dp))
        MiniPill(
            label = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReadyContent(
    state: WatchState,
    now: Long,
    feedback: String,
) {
    val title = when {
        state.armed -> "Ready"
        state.hasPlace -> "Setup paused"
        else -> "Setup needed"
    }
    val detail = when {
        state.armed -> "Waiting for arrival"
        state.hasPlace -> "Arm it on your phone"
        else -> "Choose a place on phone"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusDot(active = state.armed)
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
                state.placeLabel.ifBlank { "Selected place" },
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                "${formatDurationMinutes(state.durationMinutes)} default",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
        StatusText(feedback.ifBlank { syncText(state.lastUpdated, now) })
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
    secondary: String,
) {
    val active = MaterialTheme.colors.primary
    val track = MaterialTheme.colors.onSurface.copy(alpha = 0.14f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(154.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
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
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(
                primary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                secondary,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
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
            )
        }
    }
}

@Composable
private fun MiniPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.caption1, fontWeight = FontWeight.Bold)
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

private fun formatTime(timeMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timeMillis))

private fun syncText(lastUpdated: Long, now: Long): String {
    if (lastUpdated <= 0L) return "No phone sync yet"
    val ageMinutes = ((now - lastUpdated) / 60_000L).coerceAtLeast(0)
    return if (ageMinutes < 2) {
        "Synced just now"
    } else {
        "Synced ${formatTime(lastUpdated)}"
    }
}

private fun String.shortPlace(): String =
    ifBlank { "this place" }
        .substringBefore(",")
        .trim()
        .ifBlank { "this place" }
