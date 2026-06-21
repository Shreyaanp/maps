package work.shreyaan.dwell

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.view.WindowCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polygon
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        MapLibre.getInstance(this)
        MapCacheManager.configure(this)
        Notifications.ensureChannels(this)
        setContent {
            DwellTheme {
                Surface(Modifier.fillMaxSize()) {
                    DwellScreen()
                }
            }
        }
    }
}

private fun hasFineLocation(c: Context) = ContextCompat.checkSelfPermission(
    c, Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED

private fun hasBackgroundLocation(c: Context) = Build.VERSION.SDK_INT < 29 ||
    ContextCompat.checkSelfPermission(
        c, Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

private fun hasNotifications(c: Context) = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(
        c, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

private fun hasActivityRecognition(c: Context) = ActivityRecognitionManager.hasPermission(c)

private fun canExactAlarm(c: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) return true
    return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

internal enum class BackgroundLocationFlow {
    AlreadyAllowed,
    RequestPermission,
    OpenAppSettings,
}

internal fun backgroundLocationFlowForSdk(sdkInt: Int): BackgroundLocationFlow = when {
    sdkInt < 29 -> BackgroundLocationFlow.AlreadyAllowed
    sdkInt == 29 -> BackgroundLocationFlow.RequestPermission
    else -> BackgroundLocationFlow.OpenAppSettings
}

private fun dwellAppSettingsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )

private fun dwellExactAlarmSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= 31) {
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )
    } else {
        dwellAppSettingsIntent(context)
    }

private fun openDwellAppSettings(context: Context) {
    context.startActivity(
        dwellAppSettingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun formatHoursInput(hours: Double): String =
    (Math.round(hours * 100) / 100.0).toString()
        .trimEnd('0')
        .trimEnd('.')

internal fun durationMinutesFromText(text: String): Int? {
    val hours = text.trim().toDoubleOrNull() ?: return null
    if (!hours.isFinite() || hours <= 0 || hours > 48) return null
    return (hours * 60).roundToInt().coerceAtLeast(DwellPlace.MIN_DURATION_MINUTES)
}

internal fun durationInputError(text: String): String? {
    val cleaned = text.trim()
    if (cleaned.isBlank()) return "Enter a timer duration."
    val hours = cleaned.toDoubleOrNull()
        ?: return "Use hours like 1, 1.5, or 4.5."
    if (!hours.isFinite()) return "Use hours like 1, 1.5, or 4.5."
    if (hours <= 0) return "Timer must be greater than 0 hours."
    if (hours > 48) return "Timer can be up to 48 hours."
    return null
}

internal fun durationActionErrorMessage(text: String): String =
    durationInputError(text) ?: "Enter a valid duration in hours, e.g. 4.5."

internal fun primarySetupActionBlockedByDuration(
    durationText: String,
    pendingPlacePreview: Boolean,
    activePlaceArmed: Boolean,
): Boolean =
    durationInputError(durationText) != null &&
        (pendingPlacePreview || !activePlaceArmed)

internal fun secondaryTimerActionBlockedByDuration(
    durationText: String,
    pendingPlacePreview: Boolean,
): Boolean =
    durationInputError(durationText) != null && !pendingPlacePreview

internal fun durationFixActionLabel(): String = "Fix duration"

internal fun durationFixCollapsedDetail(placeLabel: String): String =
    listOf("Fix duration", placeLabel)
        .filter { it.isNotBlank() }
        .joinToString(" | ")

internal fun actionDurationMinutes(
    durationText: String,
    durationInputVisible: Boolean,
    actionPlaceDurationMinutes: Int?,
    defaultDurationMinutes: Int,
): Int? {
    val typedDuration = durationMinutesFromText(durationText)
    return if (durationInputVisible) {
        typedDuration
    } else {
        actionPlaceDurationMinutes ?: typedDuration ?: defaultDurationMinutes
    }
}

internal fun monitoringActionDurationMinutes(
    durationText: String,
    durationInputVisible: Boolean,
    actionPlaceDurationMinutes: Int?,
    actionPlaceAlreadyMonitoring: Boolean,
    defaultDurationMinutes: Int,
): Int? =
    if (actionPlaceAlreadyMonitoring && actionPlaceDurationMinutes != null) {
        actionPlaceDurationMinutes
    } else {
        actionDurationMinutes(
            durationText = durationText,
            durationInputVisible = durationInputVisible,
            actionPlaceDurationMinutes = actionPlaceDurationMinutes,
            defaultDurationMinutes = defaultDurationMinutes,
        )
    }

internal data class DurationPresetOption(
    val hours: Double,
    val value: String,
    val label: String,
    val selected: Boolean,
)

internal fun durationPresetOptions(durationText: String): List<DurationPresetOption> =
    listOf(
        0.05 to "3m",
        0.25 to "15m",
        0.5 to "30m",
        1.0 to "1h",
        2.0 to "2h",
        4.5 to "4.5h",
        8.0 to "8h",
    ).map { (hours, label) ->
        val value = formatHoursInput(hours)
        DurationPresetOption(
            hours = hours,
            value = value,
            label = label,
            selected = durationText == value,
        )
    }

internal data class RadiusControlState(
    val valueMeters: Float,
    val maxMeters: Float,
    val sliderEnabled: Boolean,
    val helperText: String?,
)

internal data class RadiusPresetOption(
    val meters: Float,
    val label: String,
    val selected: Boolean,
    val enabled: Boolean,
)

internal fun radiusControlState(
    radiusMeters: Float,
    monitoredRadiusLimitMeters: Float?,
): RadiusControlState {
    val normalizedRadius = DwellRadius.normalize(radiusMeters)
    val monitoredLimit = monitoredRadiusLimitMeters?.let { DwellRadius.normalize(it) }
    if (monitoredLimit == null) {
        return RadiusControlState(
            valueMeters = normalizedRadius,
            maxMeters = DwellRadius.MAX_METERS,
            sliderEnabled = true,
            helperText = null,
        )
    }

    val canTighten = monitoredLimit > DwellRadius.MIN_METERS + 0.5f
    return if (canTighten) {
        RadiusControlState(
            valueMeters = normalizedRadius.coerceIn(DwellRadius.MIN_METERS, monitoredLimit),
            maxMeters = monitoredLimit,
            sliderEnabled = true,
            helperText = "Monitoring is live. You can tighten radius; pause to increase above ${monitoredLimit.roundToInt()} m.",
        )
    } else {
        RadiusControlState(
            valueMeters = normalizedRadius,
            maxMeters = DwellRadius.MAX_METERS,
            sliderEnabled = false,
            helperText = "Monitoring is live at the 50 m minimum. Pause monitoring to increase radius.",
        )
    }
}

internal fun radiusPresetOptions(
    radiusMeters: Float,
    maxMeters: Float,
    controlsEnabled: Boolean,
): List<RadiusPresetOption> {
    val current = DwellRadius.normalize(radiusMeters).roundToInt()
    val normalizedMax = DwellRadius.normalize(maxMeters)
    return listOf(50f, 100f, 150f, 250f).map { preset ->
        RadiusPresetOption(
            meters = preset,
            label = "${preset.roundToInt()} m",
            selected = current == preset.roundToInt(),
            enabled = controlsEnabled && preset <= normalizedMax,
        )
    }
}

private const val LOCATION_SEARCH_NETWORK_COOLDOWN_MS = 1_500L
private const val LOCATION_SEARCH_DEBOUNCE_MS = 650L
private const val LOCATION_SEARCH_CACHE_TTL_MS = 30 * 60 * 1_000L
private const val LOCATION_SEARCH_CACHE_PREFS = "dwell_location_search_cache"
private const val LOCATION_SEARCH_CACHE_MAX_ENTRIES = 24
private const val LOCATION_SEARCH_DROPDOWN_MAX_RESULTS = 5
private const val CURRENT_LOCATION_LIVE_FIX_TIMEOUT_MS = 8_000L

private val SearchWhitespaceRegex = Regex("\\s+")

private data class MapPoint(
    val latitude: Double,
    val longitude: Double,
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

private data class LocationSearchResult(
    val label: String,
    val point: MapPoint,
)

private data class CachedLocationSearch(
    val fetchedAtMillis: Long,
    val results: List<LocationSearchResult>,
)

private fun cleanSearchQuery(query: String): String =
    query.trim().replace(SearchWhitespaceRegex, " ")

internal fun searchCacheKey(query: String): String =
    cleanSearchQuery(query).lowercase(Locale.ROOT)

private val PersistentSearchKeyRegex = Regex("""^q_[0-9a-f]{64}$""")

internal fun persistentSearchKey(queryKey: String): String =
    "q_" + sha256Hex(queryKey)

private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

internal fun isPrivacyPreservingPersistentSearchKey(key: String): Boolean =
    PersistentSearchKeyRegex.matches(key)

private fun decodePersistentSearch(raw: String?): CachedLocationSearch? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val json = JSONObject(raw)
        val results = json.getJSONArray("results")
        CachedLocationSearch(
            fetchedAtMillis = json.optLong("fetchedAt", 0L),
            results = buildList {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val label = item.optString("label")
                    val lat = item.optDouble("lat", Double.NaN)
                    val lon = item.optDouble("lon", Double.NaN)
                    if (
                        label.isNotBlank() &&
                        lat.isFinite() &&
                        lon.isFinite() &&
                        lat in -90.0..90.0 &&
                        lon in -180.0..180.0
                    ) {
                        add(LocationSearchResult(label, MapPoint(lat, lon)))
                    }
                }
            },
        )
    }.getOrNull()
}

private fun encodePersistentSearch(cache: CachedLocationSearch): String {
    val results = JSONArray()
    cache.results.forEach { result ->
        results.put(
            JSONObject()
                .put("label", result.label)
                .put("lat", result.point.latitude)
                .put("lon", result.point.longitude),
        )
    }
    return JSONObject()
        .put("fetchedAt", cache.fetchedAtMillis)
        .put("results", results)
        .toString()
}

private fun readPersistentSearchCache(
    context: Context,
    queryKey: String,
    nowMs: Long,
): CachedLocationSearch? {
    val prefs = context.getSharedPreferences(LOCATION_SEARCH_CACHE_PREFS, Context.MODE_PRIVATE)
    val key = persistentSearchKey(queryKey)
    val cached = decodePersistentSearch(prefs.getString(key, null)) ?: return null
    if (
        cached.fetchedAtMillis <= 0L ||
        nowMs - cached.fetchedAtMillis >= LOCATION_SEARCH_CACHE_TTL_MS
    ) {
        prefs.edit().remove(key).apply()
        return null
    }
    return cached
}

internal fun isFreshPersistentSearchEntry(
    fetchedAtMillis: Long,
    nowMs: Long,
    ttlMs: Long = LOCATION_SEARCH_CACHE_TTL_MS,
): Boolean =
    fetchedAtMillis > 0L &&
        nowMs >= fetchedAtMillis &&
        nowMs - fetchedAtMillis < ttlMs

private fun prunePersistentSearchCache(
    context: Context,
    nowMs: Long = System.currentTimeMillis(),
) {
    val prefs = context.getSharedPreferences(LOCATION_SEARCH_CACHE_PREFS, Context.MODE_PRIVATE)
    val expiredKeys = prefs.all.mapNotNull { (key, value) ->
        if (!key.startsWith("q_")) return@mapNotNull null
        if (!isPrivacyPreservingPersistentSearchKey(key)) return@mapNotNull key
        val cache = decodePersistentSearch(value as? String)
        if (cache == null || !isFreshPersistentSearchEntry(cache.fetchedAtMillis, nowMs)) {
            key
        } else {
            null
        }
    }
    if (expiredKeys.isNotEmpty()) {
        prefs.edit().also { editor ->
            expiredKeys.forEach(editor::remove)
        }.apply()
    }
}

private fun writePersistentSearchCache(
    context: Context,
    queryKey: String,
    cache: CachedLocationSearch,
) {
    val prefs = context.getSharedPreferences(LOCATION_SEARCH_CACHE_PREFS, Context.MODE_PRIVATE)
    val cacheKey = persistentSearchKey(queryKey)
    val nowMs = System.currentTimeMillis()
    val editor = prefs.edit().putString(cacheKey, encodePersistentSearch(cache))
    val cachedEntries = prefs.all.mapNotNull { (key, value) ->
        if (!key.startsWith("q_")) return@mapNotNull null
        if (!isPrivacyPreservingPersistentSearchKey(key)) {
            editor.remove(key)
            return@mapNotNull null
        }
        val fetchedAt = decodePersistentSearch(value as? String)?.fetchedAtMillis
        if (fetchedAt == null || !isFreshPersistentSearchEntry(fetchedAt, nowMs)) {
            editor.remove(key)
            null
        } else {
            key to fetchedAt
        }
    }
    val oldest = cachedEntries
        .filter { it.first != cacheKey }
        .sortedByDescending { it.second }
        .drop(LOCATION_SEARCH_CACHE_MAX_ENTRIES - 1)
    oldest.forEach { (key, _) -> editor.remove(key) }
    editor.apply()
}

private fun resultIdentity(result: LocationSearchResult): String =
    "${result.label.lowercase(Locale.ROOT)}:${result.point.latitude}:${result.point.longitude}"

private fun readPersistentSearchSuggestions(
    context: Context,
    queryKey: String,
    nowMs: Long,
): List<LocationSearchResult> {
    if (queryKey.length < 3) return emptyList()
    val prefs = context.getSharedPreferences(LOCATION_SEARCH_CACHE_PREFS, Context.MODE_PRIVATE)
    val expiredKeys = mutableListOf<String>()
    val suggestions = prefs.all
        .asSequence()
        .mapNotNull { (key, value) ->
            if (!key.startsWith("q_")) return@mapNotNull null
            if (!isPrivacyPreservingPersistentSearchKey(key)) {
                expiredKeys += key
                return@mapNotNull null
            }
            val cache = decodePersistentSearch(value as? String) ?: return@mapNotNull null
            if (!isFreshPersistentSearchEntry(cache.fetchedAtMillis, nowMs)) {
                expiredKeys += key
                return@mapNotNull null
            }
            key to cache
        }
        .sortedByDescending { it.second.fetchedAtMillis }
        .flatMap { (_, cache) ->
            cache.results.asSequence().filter { result ->
                result.label.lowercase(Locale.ROOT).contains(queryKey)
            }
        }
        .distinctBy(::resultIdentity)
        .take(LOCATION_SEARCH_DROPDOWN_MAX_RESULTS)
        .toList()

    if (expiredKeys.isNotEmpty()) {
        prefs.edit().also { editor ->
            expiredKeys.forEach(editor::remove)
        }.apply()
    }
    return suggestions
}

private fun clearPersistentSearchCache(context: Context) {
    context.getSharedPreferences(LOCATION_SEARCH_CACHE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

private suspend fun searchOpenStreetMap(
    query: String,
    baseUrl: String,
    userAgent: String,
): List<LocationSearchResult> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
    val url = URL(
        "${MobileSearchConfig.searchEndpoint(baseUrl)}?format=jsonv2&limit=5&q=$encoded"
    )
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", userAgent)
    }

    try {
        if (conn.responseCode !in 200..299) return@withContext emptyList()
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONArray(body)
        buildList {
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val lat = item.optString("lat").toDoubleOrNull()
                val lon = item.optString("lon").toDoubleOrNull()
                val label = item.optString("display_name").ifBlank { query }
                if (lat != null && lon != null) {
                    add(LocationSearchResult(label, MapPoint(lat, lon)))
                }
            }
        }
    } finally {
        conn.disconnect()
    }
}

private fun distanceMeters(a: MapPoint, b: MapPoint): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        a.latitude,
        a.longitude,
        b.latitude,
        b.longitude,
        results,
    )
    return results[0]
}

private class ZoneOverlays {
    var map: MapLibreMap? = null
    var marker: Marker? = null
    var circle: Polygon? = null
    var userMarker: Marker? = null
    var pendingZonePoint: MapPoint? = null
    var pendingUserPoint: MapPoint? = null
}

internal enum class AppRoute {
    Home,
    Insights,
    Settings,
    SetupChecks,
    Tutorial,
    SavedZones,
}

internal enum class OnboardingPage {
    Intro,
    Permissions,
    Guide,
}

internal enum class PlaceSelectionMode {
    CreateNew,
    ViewSelected,
    EditSelected,
}

private data class PendingPlacePreview(
    val point: MapPoint,
    val sourceLabel: String,
    val placeName: String,
    val mode: PlaceSelectionMode,
    val targetPlaceId: String?,
    val targetPlaceLabel: String,
    val returnFocus: PendingPreviewReturnFocus?,
    val analyticsSource: String,
    val autoStart: Boolean,
)

private data class CommitGeofencePointResult(
    val place: DwellPlace,
    val selectedExistingDuplicate: Boolean,
)

private data class PendingMonitoringResume(
    val placeId: String?,
)

internal enum class MonitoringResumeTargetAction {
    ArmCurrentSelection,
    MonitorSavedPlace,
    StopMissingPlace,
}

internal data class MonitoringResumeTargetPlan(
    val action: MonitoringResumeTargetAction,
    val placeId: String?,
)

internal fun monitoringResumeTargetAction(
    requestedPlaceId: String?,
    savedPlaceExists: Boolean,
): MonitoringResumeTargetAction {
    val scopedPlaceId = requestedPlaceId?.takeIf { it.isNotBlank() }
    return when {
        scopedPlaceId == null -> MonitoringResumeTargetAction.ArmCurrentSelection
        savedPlaceExists -> MonitoringResumeTargetAction.MonitorSavedPlace
        else -> MonitoringResumeTargetAction.StopMissingPlace
    }
}

internal fun monitoringResumeTargetPlan(
    requestedPlaceId: String?,
    savedPlaceExists: Boolean,
    currentSelectionPlaceId: String?,
): MonitoringResumeTargetPlan {
    val scopedPlaceId = requestedPlaceId?.takeIf { it.isNotBlank() }
    val currentPlaceId = currentSelectionPlaceId?.takeIf { it.isNotBlank() }
    val action = monitoringResumeTargetAction(
        requestedPlaceId = scopedPlaceId,
        savedPlaceExists = savedPlaceExists,
    )
    return MonitoringResumeTargetPlan(
        action = action,
        placeId = when (action) {
            MonitoringResumeTargetAction.MonitorSavedPlace -> scopedPlaceId
            MonitoringResumeTargetAction.ArmCurrentSelection -> currentPlaceId
            MonitoringResumeTargetAction.StopMissingPlace -> null
        },
    )
}

internal fun missingMonitoringResumePlaceMessage(): String =
    "That saved place is no longer available. Pick a place and tap Monitor again."

private data class DeletedPlaceUndo(
    val place: DwellPlace,
    val restoreFocusOnUndo: Boolean,
    val removedAtMillis: Long = System.currentTimeMillis(),
)

internal fun mapSearchPlaceholder(
    hasPlace: Boolean,
    editingSelectedPlace: Boolean,
): String = when {
    !hasPlace -> "Search place or address"
    editingSelectedPlace -> "Search to move selected place"
    else -> "Search places"
}

internal data class SearchFieldActions(
    val showProgress: Boolean,
    val showClear: Boolean,
    val showClose: Boolean,
) {
    val hasAny: Boolean
        get() = showProgress || showClear || showClose
}

internal fun searchFieldActions(
    expanded: Boolean,
    searching: Boolean,
    locating: Boolean,
    searchText: String,
): SearchFieldActions =
    SearchFieldActions(
        showProgress = searching || locating,
        showClear = searchText.isNotBlank(),
        showClose = expanded,
    )

internal fun searchDropdownVisible(
    expanded: Boolean,
    searchFocused: Boolean,
    searchText: String,
    hasResults: Boolean,
    hasSuggestions: Boolean,
    searching: Boolean,
): Boolean =
    expanded && (
        searchFocused ||
            cleanSearchQuery(searchText).isNotBlank() ||
            hasResults ||
            hasSuggestions ||
            searching
        )

internal data class SearchRuntimeState(
    val searching: Boolean,
    val searchingQueryKey: String,
)

internal fun clearedSearchRuntimeState(): SearchRuntimeState =
    SearchRuntimeState(
        searching = false,
        searchingQueryKey = "",
    )

internal fun searchCompletionShouldUpdateUi(
    searchPanelExpanded: Boolean,
    currentSearchText: String,
    completedQueryKey: String,
): Boolean =
    searchPanelExpanded && searchCacheKey(currentSearchText) == completedQueryKey

internal fun shouldRunSearchAutocomplete(
    searchPanelExpanded: Boolean,
    networkAutocomplete: Boolean,
    currentSearchText: String,
    pendingQueryKey: String,
    submittedSearchKey: String,
    searching: Boolean,
): Boolean =
    searchPanelExpanded &&
        networkAutocomplete &&
        searchCacheKey(currentSearchText) == pendingQueryKey &&
        submittedSearchKey != pendingQueryKey &&
        !searching

internal data class SearchPanelClosePlan(
    val expanded: Boolean,
    val clearText: Boolean,
    val clearFocus: Boolean,
)

internal fun searchPanelClosePlan(): SearchPanelClosePlan =
    SearchPanelClosePlan(
        expanded = false,
        clearText = true,
        clearFocus = true,
    )

internal fun currentLocationActionSubtitle(
    editingSelectedPlace: Boolean,
    selectsPlace: Boolean = true,
    compact: Boolean = false,
): String = when {
    !selectsPlace && compact -> "Center the map nearby"
    !selectsPlace -> "Center the map where you are"
    editingSelectedPlace && compact -> "Move selected place nearby"
    editingSelectedPlace -> "Move selected place where you are now"
    compact -> "Fastest way to create a nearby place"
    else -> "Drop a place where you are now"
}

internal fun mapCurrentLocationSelectsPlace(
    selectionMode: PlaceSelectionMode,
    hasEditingPlace: Boolean,
): Boolean = when (selectionMode) {
    PlaceSelectionMode.CreateNew -> true
    PlaceSelectionMode.EditSelected -> hasEditingPlace
    PlaceSelectionMode.ViewSelected -> false
}

internal fun mapCurrentLocationActionDescription(
    selectsPlace: Boolean,
    editingSelectedPlace: Boolean,
): String = when {
    selectsPlace && editingSelectedPlace -> "Use current location to move selected place"
    selectsPlace -> "Use current location to add a place"
    else -> "Center map on current location"
}

internal data class MapPointSelectionBehavior(
    val label: String,
    val analyticsSource: String,
    val forceCreateNew: Boolean,
    val expandDock: Boolean,
)

internal fun longPressMapSelectionBehavior(): MapPointSelectionBehavior =
    MapPointSelectionBehavior(
        label = "Dropped pin",
        analyticsSource = "map_long_press",
        forceCreateNew = true,
        expandDock = true,
    )

internal fun currentLocationPermissionPrompt(selectAsZone: Boolean): String =
    if (selectAsZone) {
        "Allow location to use your current place."
    } else {
        "Allow location to show your position."
    }

internal fun currentLocationPermissionAlreadyActiveMessage(
    backgroundDisclosureVisible: Boolean,
): String =
    monitoringPermissionUiAlreadyActiveMessage(backgroundDisclosureVisible)

internal fun currentLocationUnavailableMessage(selectAsZone: Boolean): String =
    if (selectAsZone) {
        "Could not get current location. Check Location, or search/long-press instead."
    } else {
        "Could not get current location. Check that Location is on."
    }

internal fun currentLocationPermissionDeniedMessage(selectAsZone: Boolean): String =
    if (selectAsZone) {
        "Location is blocked. Open app settings to allow it, or search/long-press the map."
    } else {
        "Location is blocked. Open app settings to allow it, or move the map manually."
    }

internal fun permissionRecoveryActionLabel(): String = "Open app settings"

internal fun noSearchResultsDetail(
    editingSelectedPlace: Boolean,
    currentLocationSelectsPlace: Boolean = true,
): String = when {
    !currentLocationSelectsPlace ->
        "Try another landmark, or tap Add place to create a new place."
    editingSelectedPlace ->
        "Try a landmark, or use current location to move this place."
    else ->
        "Try a landmark, or use current location to add a place."
}

internal fun noSearchResultsToast(
    editingSelectedPlace: Boolean,
    currentLocationSelectsPlace: Boolean = true,
): String = when {
    !currentLocationSelectsPlace ->
        "No places found. Tap Add place to create a new place."
    editingSelectedPlace ->
        "No places found. Use current location to move this place."
    else ->
        "No places found. Use current location to add a place."
}

internal fun arrivalModeLabel(autoStart: Boolean): String =
    if (autoStart) "Auto-start" else "Confirm first"

internal fun arrivalModeDetail(autoStart: Boolean): String =
    if (autoStart) {
        "High-confidence arrivals start the timer."
    } else {
        "Dwell asks before starting here."
    }

internal data class ArrivalModeOption(
    val autoStart: Boolean,
    val label: String,
    val detail: String,
    val selected: Boolean,
)

internal fun arrivalModeOptions(autoStart: Boolean): List<ArrivalModeOption> =
    listOf(true, false).map { optionAutoStart ->
        ArrivalModeOption(
            autoStart = optionAutoStart,
            label = arrivalModeLabel(optionAutoStart),
            detail = arrivalModeDetail(optionAutoStart),
            selected = optionAutoStart == autoStart,
        )
    }

internal data class OnboardingPermissionStatus(
    val locationGranted: Boolean,
    val backgroundGranted: Boolean,
    val notificationsGranted: Boolean,
    val motionGranted: Boolean,
) {
    val allMajorGranted: Boolean
        get() = locationGranted && backgroundGranted && notificationsGranted && motionGranted
}

internal fun onboardingPermissionHelp(status: OnboardingPermissionStatus): String =
    when {
        !status.locationGranted ->
            "Allow location so Dwell can find your current place and monitor arrivals."
        !status.notificationsGranted ->
            "Allow notifications so Dwell can ask before starting and alert when time is up."
        !status.motionGranted ->
            "Allow physical activity so Dwell can avoid starting timers during pass-through movement."
        !status.backgroundGranted ->
            "Allow all-the-time location so arrivals work after the app is closed."
        else ->
            "Core permissions are ready. You can create and monitor your first place."
    }

internal fun monitoringPermissionUiAlreadyActive(
    permissionRequestInFlight: Boolean,
    backgroundDisclosureVisible: Boolean,
): Boolean =
    permissionRequestInFlight || backgroundDisclosureVisible

internal fun monitoringPermissionUiAlreadyActiveMessage(
    backgroundDisclosureVisible: Boolean,
): String =
    if (backgroundDisclosureVisible) {
        "Finish the open background location setup first."
    } else {
        "Finish the open Android permission prompt first."
    }

internal enum class MonitoringSetupRecoveryStep {
    ForegroundPermissions,
    BackgroundLocation,
}

internal fun monitoringSetupRecoveryStep(
    foregroundPermissionsMissing: Boolean,
    backgroundLocationMissing: Boolean,
): MonitoringSetupRecoveryStep? = when {
    foregroundPermissionsMissing -> MonitoringSetupRecoveryStep.ForegroundPermissions
    backgroundLocationMissing -> MonitoringSetupRecoveryStep.BackgroundLocation
    else -> null
}

internal fun shouldShowMonitoringSetupRecovery(
    nextStep: MonitoringSetupRecoveryStep?,
    alreadyShownStep: MonitoringSetupRecoveryStep?,
): Boolean =
    nextStep != null && nextStep != alreadyShownStep

private fun humanList(items: List<String>): String =
    when (items.size) {
        0 -> ""
        1 -> items.first()
        2 -> "${items[0]} and ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + ", and ${items.last()}"
    }

internal fun monitoringSetupForegroundRecoveryMessage(
    permissionStatus: OnboardingPermissionStatus,
): String {
    val missing = buildList {
        if (!permissionStatus.locationGranted) add("location")
        if (!permissionStatus.notificationsGranted) add("notifications")
        if (!permissionStatus.motionGranted) add("physical activity")
    }
    return if (missing.isEmpty()) {
        "Foreground permissions are ready."
    } else if (missing.size == 1) {
        onboardingPermissionHelp(permissionStatus)
    } else {
        "Allow ${humanList(missing)} so monitoring can start."
    }
}

internal fun monitoringSetupForegroundRecoveryActionLabel(
    permissionStatus: OnboardingPermissionStatus,
): String =
    when {
        !permissionStatus.locationGranted &&
            permissionStatus.notificationsGranted &&
            permissionStatus.motionGranted -> "Allow location"
        permissionStatus.locationGranted &&
            !permissionStatus.notificationsGranted &&
            permissionStatus.motionGranted -> "Allow notifications"
        permissionStatus.locationGranted &&
            permissionStatus.notificationsGranted &&
            !permissionStatus.motionGranted -> "Allow physical activity"
        !permissionStatus.locationGranted ||
            !permissionStatus.notificationsGranted ||
            !permissionStatus.motionGranted -> "Allow permissions"
        else -> ""
    }

internal fun onboardingPrimaryActionLabel(
    page: OnboardingPage,
    permissionStatus: OnboardingPermissionStatus,
): String = when (page) {
    OnboardingPage.Intro -> "Set up"
    OnboardingPage.Permissions -> when {
        permissionStatus.allMajorGranted -> "Continue"
        !permissionStatus.locationGranted -> "Allow location"
        !permissionStatus.notificationsGranted -> "Allow notifications"
        !permissionStatus.motionGranted -> "Allow physical activity"
        !permissionStatus.backgroundGranted -> "Allow background location"
        else -> "Continue"
    }
    OnboardingPage.Guide -> "Add first place"
}

internal fun onboardingPermissionRecoveryButtonLabel(
    permissionStatus: OnboardingPermissionStatus,
): String =
    onboardingPrimaryActionLabel(
        page = OnboardingPage.Permissions,
        permissionStatus = permissionStatus,
    )

internal fun onboardingGuideSteps(): List<String> = listOf(
    "Tap Add place, then pick Home, Office, Gym, or any exact spot.",
    "Use search for an address, current location for where you are, or a long-press for a precise map point.",
    "Review the unsaved name, radius, timer duration, and arrival mode before it changes anything.",
    "Tap Save this place, then Monitor. Repeat for every place you want watched live.",
)

internal data class TutorialFlowStep(
    val title: String,
    val detail: String,
)

internal fun appTutorialFlowSteps(): List<TutorialFlowStep> = listOf(
    TutorialFlowStep(
        title = "Set up once",
        detail = "Finish location, notifications, physical activity, alarms, and battery checks once.",
    ),
    TutorialFlowStep(
        title = "Add a place",
        detail = "Tap Add place, then use search, current location, or a long-press on the map.",
    ),
    TutorialFlowStep(
        title = "Save the preview",
        detail = "Name it, choose radius, timer, and arrival mode, then tap Save this place.",
    ),
    TutorialFlowStep(
        title = "Monitor that row",
        detail = "Turn on Monitor for the saved place. Dwell can watch it after the app is closed.",
    ),
    TutorialFlowStep(
        title = "Add more places",
        detail = "Repeat for Home, Office, Gym, or more. Each row keeps its own settings.",
    ),
    TutorialFlowStep(
        title = "Edit safely",
        detail = "View map is read-only. Edit settings changes only the selected saved row.",
    ),
    TutorialFlowStep(
        title = "Arrive and respond",
        detail = "Auto-start begins the timer. Confirm first asks before starting or switching places.",
    ),
    TutorialFlowStep(
        title = "Use it daily",
        detail = "Places is the dashboard: monitor, pause, start now, edit one row, or finish setup when needed.",
    ),
    TutorialFlowStep(
        title = "Use the watch",
        detail = "Phone notifications, watch app, and Tile should name the same active place.",
    ),
)

internal fun appTutorialPickPlaceSteps(): List<TutorialFlowStep> = listOf(
    TutorialFlowStep(
        title = "Search",
        detail = "Use for addresses or landmarks. Choosing a result creates an unsaved preview.",
    ),
    TutorialFlowStep(
        title = "Current location",
        detail = "In Add or Edit it chooses the phone's spot. In View map it only centers the map.",
    ),
    TutorialFlowStep(
        title = "Long-press map",
        detail = "Drops a new unsaved preview at the pressed point. It will not silently move a saved place.",
    ),
)

internal fun appTutorialExampleSteps(): List<TutorialFlowStep> = listOf(
    TutorialFlowStep(
        title = "Home",
        detail = "Use a 50 m radius, a short timer, and Confirm first if nearby places overlap.",
    ),
    TutorialFlowStep(
        title = "Office",
        detail = "Use its own radius and work timer. Office changes should not affect Home.",
    ),
    TutorialFlowStep(
        title = "Gym",
        detail = "Give it a separate timer and turn on Monitor alongside Home and Office.",
    ),
    TutorialFlowStep(
        title = "When you arrive",
        detail = "Dwell uses the place you entered, even if another place is open on the map.",
    ),
)

internal fun appTutorialMultiplePlaceRules(): List<TutorialFlowStep> = listOf(
    TutorialFlowStep(
        title = "Monitor several rows",
        detail = "Home, Office, Gym, and other saved places can all stay live together.",
    ),
    TutorialFlowStep(
        title = "Settings stay separate",
        detail = "Name, radius, timer duration, and arrival mode belong to the selected saved row.",
    ),
    TutorialFlowStep(
        title = "The entered place wins",
        detail = "Arrival prompts use the place you entered, even if another place is open on the map.",
    ),
)

internal fun appTutorialStuckStateSteps(): List<TutorialFlowStep> = listOf(
    TutorialFlowStep(
        title = "Unsaved preview",
        detail = unsavedRuntimeActionsBlockedDetail(),
    ),
    TutorialFlowStep(
        title = "Search suggestions",
        detail = "Choose a result, clear text, tap close, tap outside the panel, or press Back.",
    ),
    TutorialFlowStep(
        title = "Duplicate place",
        detail = "If the spot already exists, Dwell opens that saved place and keeps its settings.",
    ),
    TutorialFlowStep(
        title = "Needs setup",
        detail = "Tap Finish setup when background location, notifications, alarms, or battery block monitoring.",
    ),
    TutorialFlowStep(
        title = "Monitoring limit",
        detail = "If the live-place limit appears, pause another monitored place before monitoring a new row.",
    ),
)

internal fun setupChecksIntroDetail(
    permissionStatus: OnboardingPermissionStatus,
    exactAlarmAllowed: Boolean,
    batteryReliabilityStatus: BatteryReliabilityStatus,
): String = when {
    !permissionStatus.allMajorGranted ->
        onboardingPermissionHelp(permissionStatus)
    !exactAlarmAllowed ->
        "Allow exact alarms so timers can alert on time."
    batteryNeedsReliabilityReview(batteryReliabilityStatus) ->
        "Open app info, then Battery, and choose Unrestricted so background arrivals are not delayed."
    else ->
        "Setup checks are ready for background monitoring."
}

internal fun setupChecksPermissionActionLabel(
    permissionStatus: OnboardingPermissionStatus,
): String? = when {
    !permissionStatus.locationGranted -> "Allow location"
    !permissionStatus.notificationsGranted -> "Allow notifications"
    !permissionStatus.motionGranted -> "Allow physical activity"
    !permissionStatus.backgroundGranted -> "Allow background location"
    else -> null
}

internal enum class SetupCheckPermissionAction {
    Permissions,
    ExactAlarm,
}

internal data class SetupCheckPermissionButton(
    val action: SetupCheckPermissionAction,
    val label: String,
)

internal fun setupChecksPermissionButtons(
    permissionStatus: OnboardingPermissionStatus,
    exactAlarmAllowed: Boolean,
): List<SetupCheckPermissionButton> = buildList {
    setupChecksPermissionActionLabel(permissionStatus)?.let { label ->
        add(
            SetupCheckPermissionButton(
                action = SetupCheckPermissionAction.Permissions,
                label = label,
            )
        )
    }
    if (!exactAlarmAllowed) {
        add(
            SetupCheckPermissionButton(
                action = SetupCheckPermissionAction.ExactAlarm,
                label = "Allow exact alarms",
            )
        )
    }
}

internal fun setupChecksBatteryActionLabel(
    batteryReliabilityStatus: BatteryReliabilityStatus,
): String? =
    if (batteryNeedsReliabilityReview(batteryReliabilityStatus)) {
        batteryHelpActionLabel()
    } else {
        null
    }

internal fun batteryNeedsReliabilityReview(status: BatteryReliabilityStatus): Boolean =
    status.isKnownAggressiveOem && !status.isIgnoringOptimizations

internal fun backgroundLocationHelpMessage(): String =
    "Allow all-the-time location so Dwell can detect arrivals after you leave the app."

internal fun backgroundLocationHelpActionLabel(): String = "Open app settings"

internal fun batteryHelpMessage(status: BatteryReliabilityStatus): String =
    if (status.isKnownAggressiveOem) {
        "${status.manufacturer} may delay background arrivals. Open app info, then Battery, and choose Unrestricted."
    } else {
        "Android may delay background arrivals while battery optimization is enabled. Open app info, then Battery, and choose Unrestricted."
    }

internal fun batteryHelpActionLabel(): String = "Open app info"

internal fun appDataDeletedMessage(): String = "App data deleted"

internal fun appDataServerCleanupFailedMessage(): String =
    "Server cleanup did not confirm. Local data is deleted."

internal data class AppDataClearUiReset(
    val onboardingComplete: Boolean = false,
    val route: AppRoute = AppRoute.Home,
    val placeSelectionMode: PlaceSelectionMode = PlaceSelectionMode.CreateNew,
    val selectedPlaceId: String = "",
    val viewingPlaceId: String = "",
    val editingPlaceId: String = "",
    val selectedPlaceLabel: String = "",
    val timerEndMillis: Long = 0L,
    val dismissCurrentSnackbar: Boolean = true,
    val closeSearchPanel: Boolean = true,
    val clearPendingMonitoringResume: Boolean = true,
    val clearPendingLocationResume: Boolean = true,
)

internal fun appDataClearUiReset(): AppDataClearUiReset = AppDataClearUiReset()

internal fun longActionMessageKey(message: String, actionLabel: String): String =
    "${message.trim()}|${actionLabel.trim()}"

internal fun shouldEnqueueLongActionMessage(
    activeLongActionKey: String?,
    nextLongActionKey: String,
): Boolean =
    activeLongActionKey != nextLongActionKey

internal fun permissionRowDetail(
    title: String,
    granted: Boolean,
): String =
    if (granted) {
        "Allowed"
    } else {
        when (title) {
            "Location" -> "Allow location"
            "Background location" -> "Allow all-the-time location"
            "Notifications" -> "Allow notifications"
            "Physical activity" -> "Allow physical activity"
            "Exact alarms" -> "Allow exact alarms"
            else -> "Not allowed yet"
        }
    }

internal fun onboardingPermissionRowStatus(
    title: String,
    granted: Boolean,
): String =
    if (granted) {
        "Ready"
    } else {
        permissionRowDetail(title, granted)
    }

internal data class OnboardingCompletionAction(
    val route: AppRoute,
    val beginCreatePlace: Boolean,
    val openSearchPanel: Boolean,
    val focusSearch: Boolean,
    val toastMessage: String?,
)

internal fun onboardingCompletionAction(hasSavedPlace: Boolean): OnboardingCompletionAction =
    if (hasSavedPlace) {
        OnboardingCompletionAction(
            route = AppRoute.SavedZones,
            beginCreatePlace = false,
            openSearchPanel = false,
            focusSearch = false,
            toastMessage = null,
        )
    } else {
        OnboardingCompletionAction(
            route = AppRoute.Home,
            beginCreatePlace = true,
            openSearchPanel = true,
            focusSearch = true,
            toastMessage = addPlacePromptMessage(),
        )
    }

internal fun addPlacePromptMessage(): String =
    "Search, use current location, or long-press the map to add a place"

internal fun choosePlaceForMonitoringMessage(): String =
    "Search, use current location, or long-press the map to choose a place first"

internal fun choosePlaceToPauseMessage(): String =
    "Open Places to pause monitoring for a place"

internal fun pauseMonitoringBeforeIncreasingRadiusMessage(): String =
    "Pause monitoring before increasing this place's radius."

internal fun pauseMonitoringBeforeChangingLocationMessage(placeLabel: String): String =
    displayablePlaceLabel(placeLabel)
        ?.let { "Pause monitoring for $it before changing its location." }
        ?: "Pause monitoring before changing this place's location."

internal fun pendingPlacePrimaryActionLabel(
    editingSelectedPlace: Boolean,
    targetLabel: String,
): String =
    if (editingSelectedPlace) {
        "Move ${displayPlaceName(targetLabel)}"
    } else {
        "Save this place"
    }

internal fun pendingPlaceStatusTitle(
    editingSelectedPlace: Boolean,
    targetLabel: String,
): String =
    if (editingSelectedPlace) {
        "Move ${displayPlaceName(targetLabel)}?"
    } else {
        "Save this place?"
    }

internal fun pendingPlaceStatusDetail(editingSelectedPlace: Boolean): String =
    if (editingSelectedPlace) {
        "This move is not saved yet. Adjust details, then save."
    } else {
        "This place is not saved yet. Adjust details, then save."
    }

internal fun pendingPlaceCancelMessage(
    editingSelectedPlace: Boolean,
    targetPlaceLabel: String,
    targetPlaceAvailable: Boolean = true,
): String =
    when {
        !editingSelectedPlace -> "Unsaved place canceled"
        !targetPlaceAvailable -> "Move canceled. Saved place is no longer available."
        else -> "${displayPlaceName(targetPlaceLabel)} move canceled. Original place kept."
    }

internal fun mapPreviewPlaceName(
    existingPlaceLabel: String?,
    sourceLabel: String,
): String =
    existingPlaceLabel
        ?.takeIf { it.isNotBlank() }
        ?: sourceLabel.ifBlank { "Saved place" }

internal fun mapPreviewPlaceNameForPointChange(
    existingPlaceLabel: String?,
    sourceLabel: String,
    previousPreviewPlaceName: String?,
    previousPreviewSourceLabel: String?,
): String {
    val existing = existingPlaceLabel?.takeIf { it.isNotBlank() }
    if (existing != null) return existing

    val previousName = previousPreviewPlaceName?.trim().orEmpty()
    val previousSourceName = mapPreviewPlaceName(
        existingPlaceLabel = null,
        sourceLabel = previousPreviewSourceLabel.orEmpty(),
    )
    if (previousName.isNotBlank() && previousName != previousSourceName) {
        return previousName
    }

    return mapPreviewPlaceName(
        existingPlaceLabel = null,
        sourceLabel = sourceLabel,
    )
}

internal fun placeNameInputValue(name: String): String =
    name.take(DwellPlace.MAX_LABEL_LENGTH)

internal fun placeNameSupportingText(
    placeName: String,
    fallbackLabel: String,
): String? {
    val fallback = displayablePlaceLabel(fallbackLabel)
        ?.take(DwellPlace.MAX_LABEL_LENGTH)
        .orEmpty()
    val remaining = (DwellPlace.MAX_LABEL_LENGTH - placeName.length).coerceAtLeast(0)
    return when {
        placeName.isBlank() && fallback.isNotBlank() ->
            "Leave blank to use $fallback."
        placeName.isBlank() ->
            "Add a name like Home, Office, or Gym."
        remaining == 0 ->
            "Name limit reached."
        remaining <= 20 ->
            "$remaining characters left."
        else -> null
    }
}

internal fun selectedPlaceDisplayLabel(
    hasPin: Boolean,
    typedPlaceLabel: String,
    fallbackLabel: String,
): String =
    if (!hasPin) {
        "No place selected"
    } else {
        typedPlaceLabel.ifBlank {
            displayablePlaceLabel(fallbackLabel) ?: "Dropped pin"
        }
    }

internal fun hasMapSearchContext(
    hasPin: Boolean,
    hasSavedPlace: Boolean,
    hasPendingPlacePreview: Boolean,
): Boolean =
    hasPin || hasSavedPlace || hasPendingPlacePreview

internal fun hasActionableDockPlace(
    hasPin: Boolean,
    hasActivePlace: Boolean,
    timerActive: Boolean,
    promptActive: Boolean,
): Boolean =
    hasPin || hasActivePlace || timerActive || promptActive

internal fun pendingPlaceCommitLabel(
    typedPlaceLabel: String,
    previewPlaceName: String,
    sourceLabel: String,
    targetPlaceLabel: String,
    editingSelectedPlace: Boolean,
): String {
    val fallback = if (editingSelectedPlace) {
        targetPlaceLabel.ifBlank { previewPlaceName }.ifBlank { sourceLabel }
    } else {
        previewPlaceName.ifBlank { sourceLabel }
    }
    return typedPlaceLabel.ifBlank { fallback }.ifBlank { "Saved place" }
}

internal fun pendingPlaceNameFallbackLabel(
    previewPlaceName: String,
    sourceLabel: String,
    targetPlaceLabel: String,
    editingSelectedPlace: Boolean,
): String =
    pendingPlaceCommitLabel(
        typedPlaceLabel = "",
        previewPlaceName = previewPlaceName,
        sourceLabel = sourceLabel,
        targetPlaceLabel = targetPlaceLabel,
        editingSelectedPlace = editingSelectedPlace,
    )

internal fun pendingPlaceSavedMessage(
    placeLabel: String,
    editingSelectedPlace: Boolean,
    selectedExistingDuplicate: Boolean = false,
): String =
    when {
        selectedExistingDuplicate ->
            "${sentencePlaceName(placeLabel)} already exists. Opened saved place; settings kept."
        editingSelectedPlace ->
            "${sentencePlaceName(placeLabel)} moved"
        else ->
            "${sentencePlaceName(placeLabel)} saved. Tap Monitor to watch arrivals."
    }

internal fun duplicateEditBlockedMessage(
    editedPlaceLabel: String,
    duplicatePlaceLabel: String,
): String {
    val edited = sentencePlaceName(editedPlaceLabel)
    val duplicate = sentencePlaceName(duplicatePlaceLabel)
    return "$edited already overlaps $duplicate. Move it farther away or cancel this move."
}

internal fun duplicatePlaceForEditCommit(
    existingPlaces: List<DwellPlace>,
    editedPlaceId: String?,
    candidate: DwellPlace,
): DwellPlace? {
    val editedId = editedPlaceId?.takeIf { it.isNotBlank() }
    return existingPlaces.firstOrNull { existing ->
        existing.id != editedId && DwellPlace.isDuplicateSavedPlace(existing, candidate)
    }
}

internal data class PendingPlaceCommitFeedback(
    val message: String,
    val expandDock: Boolean,
)

internal fun pendingPlaceCommitFeedback(
    placeLabel: String,
    editingSelectedPlace: Boolean,
    selectedExistingDuplicate: Boolean,
): PendingPlaceCommitFeedback =
    PendingPlaceCommitFeedback(
        message = pendingPlaceSavedMessage(
            placeLabel = placeLabel,
            editingSelectedPlace = editingSelectedPlace,
            selectedExistingDuplicate = selectedExistingDuplicate,
        ),
        expandDock = selectedExistingDuplicate,
    )

internal fun selectedZoneSyncLabel(
    typedPlaceLabel: String,
    fallbackPlaceLabel: String,
): String =
    typedPlaceLabel.ifBlank { fallbackPlaceLabel }.ifBlank { "Selected place" }

internal fun selectedZoneSyncDurationMinutes(
    durationText: String,
    fallbackPlaceDurationMinutes: Int?,
    defaultDurationMinutes: Int,
): Int =
    durationMinutesFromText(durationText)
        ?: fallbackPlaceDurationMinutes
        ?: defaultDurationMinutes

private val placeholderPlaceLabels = setOf(
    "Selected place",
    "Saved place",
    "No place selected",
)

private fun displayablePlaceLabel(placeLabel: String): String? =
    placeLabel
        .trim()
        .takeIf { it.isNotBlank() }
        ?.takeUnless { placeholderPlaceLabels.contains(it) }

private fun displayPlaceName(placeLabel: String): String =
    displayablePlaceLabel(placeLabel) ?: "place"

private fun sentencePlaceName(placeLabel: String): String =
    displayablePlaceLabel(placeLabel) ?: "Place"

internal fun placeModeMoveLabel(placeLabel: String): String =
    "Move ${displayPlaceName(placeLabel)}"

internal fun placeModeViewLabel(placeLabel: String): String =
    "Viewing ${displayPlaceName(placeLabel)}"

internal fun placeModePrimaryLabel(
    placeLabel: String,
    addingNewPlace: Boolean,
    editingSelectedPlace: Boolean,
): String =
    if (addingNewPlace || editingSelectedPlace) {
        placeModeMoveLabel(placeLabel)
    } else {
        placeModeViewLabel(placeLabel)
    }

internal fun savedPlaceModeChipSelected(addingNewPlace: Boolean): Boolean =
    !addingNewPlace

internal fun savedPlaceModeChipCanSwitchToEdit(addingNewPlace: Boolean): Boolean =
    addingNewPlace

internal fun mapSearchBlockedDestinationLabel(mode: PlaceSelectionMode): String =
    when (mode) {
        PlaceSelectionMode.CreateNew -> "Add place"
        PlaceSelectionMode.EditSelected -> "Move place"
        PlaceSelectionMode.ViewSelected -> "View map"
    }

internal fun initialPlaceSelectionMode(hasSavedPlace: Boolean): PlaceSelectionMode =
    if (hasSavedPlace) PlaceSelectionMode.ViewSelected else PlaceSelectionMode.CreateNew

internal fun previewModeForMapPoint(
    selectionMode: PlaceSelectionMode,
    hasSelectedExistingPlace: Boolean,
    forceCreateNew: Boolean,
): PlaceSelectionMode =
    if (
        !forceCreateNew &&
        selectionMode == PlaceSelectionMode.EditSelected &&
        hasSelectedExistingPlace
    ) {
        PlaceSelectionMode.EditSelected
    } else {
        PlaceSelectionMode.CreateNew
    }

internal fun shouldCarryPendingPreviewDraft(
    previousPreviewMode: PlaceSelectionMode?,
    previewMode: PlaceSelectionMode,
): Boolean =
    previousPreviewMode == previewMode

internal fun shouldResetPreviewSettingsToDefaults(
    previousPreviewMode: PlaceSelectionMode?,
    previewMode: PlaceSelectionMode,
): Boolean =
    previewMode == PlaceSelectionMode.CreateNew &&
        previousPreviewMode != PlaceSelectionMode.CreateNew

internal fun pendingPreviewAutoStartForPointChange(
    existingPlaceAutoStart: Boolean?,
    previousPreviewAutoStart: Boolean?,
    carryPreviousDraft: Boolean,
    defaultAutoStart: Boolean,
): Boolean =
    existingPlaceAutoStart ?: when {
        carryPreviousDraft && previousPreviewAutoStart != null -> previousPreviewAutoStart
        else -> defaultAutoStart
    }

internal data class PendingPreviewReturnFocus(
    val selectionMode: PlaceSelectionMode,
    val placeId: String,
)

internal fun pendingPreviewReturnFocus(
    selectionMode: PlaceSelectionMode,
    selectedPlaceId: String,
    viewingPlaceId: String,
    editingPlaceId: String,
): PendingPreviewReturnFocus? {
    val returnSelectionMode = when (selectionMode) {
        PlaceSelectionMode.ViewSelected -> PlaceSelectionMode.ViewSelected
        PlaceSelectionMode.EditSelected -> PlaceSelectionMode.EditSelected
        PlaceSelectionMode.CreateNew -> return null
    }
    val placeId = when (returnSelectionMode) {
        PlaceSelectionMode.ViewSelected -> listOf(viewingPlaceId, selectedPlaceId, editingPlaceId)
        PlaceSelectionMode.EditSelected -> listOf(editingPlaceId, selectedPlaceId, viewingPlaceId)
        PlaceSelectionMode.CreateNew -> emptyList()
    }
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?: return null
    return PendingPreviewReturnFocus(
        selectionMode = returnSelectionMode,
        placeId = placeId,
    )
}

internal fun initialEditingPlaceId(
    selectionMode: PlaceSelectionMode,
    initialSelectedPlaceId: String,
): String =
    if (selectionMode == PlaceSelectionMode.EditSelected) {
        initialSelectedPlaceId
    } else {
        ""
    }

internal data class SavedPlaceFocusState(
    val selectionMode: PlaceSelectionMode,
    val selectedPlaceId: String,
    val viewingPlaceId: String,
    val editingPlaceId: String,
)

internal fun savedPlaceFocusState(placeId: String): SavedPlaceFocusState {
    val cleanedPlaceId = placeId.trim()
    return SavedPlaceFocusState(
        selectionMode = PlaceSelectionMode.ViewSelected,
        selectedPlaceId = cleanedPlaceId,
        viewingPlaceId = cleanedPlaceId,
        editingPlaceId = "",
    )
}

internal enum class SettingsPersistenceTarget {
    PendingPreview,
    EditingPlace,
    ReadOnlyPlace,
    DefaultSettings,
}

internal fun settingsPersistenceTarget(
    hasPendingPlacePreview: Boolean,
    selectionMode: PlaceSelectionMode,
    hasEditingPlace: Boolean,
): SettingsPersistenceTarget =
    when {
        hasPendingPlacePreview -> SettingsPersistenceTarget.PendingPreview
        selectionMode == PlaceSelectionMode.EditSelected && !hasEditingPlace ->
            SettingsPersistenceTarget.ReadOnlyPlace
        selectionMode == PlaceSelectionMode.ViewSelected && hasEditingPlace ->
            SettingsPersistenceTarget.ReadOnlyPlace
        selectionMode == PlaceSelectionMode.EditSelected && hasEditingPlace ->
            SettingsPersistenceTarget.EditingPlace
        else -> SettingsPersistenceTarget.DefaultSettings
    }

internal fun durationPresetPersistenceTarget(
    hasPendingPlacePreview: Boolean,
    selectionMode: PlaceSelectionMode,
    hasSettingsPlace: Boolean,
    hasSelectedPlace: Boolean,
): SettingsPersistenceTarget =
    settingsPersistenceTarget(
        hasPendingPlacePreview = hasPendingPlacePreview,
        selectionMode = selectionMode,
        hasEditingPlace = hasSettingsPlace ||
            (selectionMode == PlaceSelectionMode.ViewSelected && hasSelectedPlace),
    )

internal fun settingsLocalChangeAllowed(target: SettingsPersistenceTarget): Boolean =
    target != SettingsPersistenceTarget.ReadOnlyPlace

internal fun viewingOnlyNoticeDetail(placeLabel: String): String =
    "Tap Edit settings to change ${displayPlaceName(placeLabel)}."

internal fun isViewingSavedPlaceReadOnly(
    selectionMode: PlaceSelectionMode,
    hasPendingPlacePreview: Boolean,
    hasSavedPlaceSelected: Boolean,
): Boolean =
    selectionMode == PlaceSelectionMode.ViewSelected &&
        !hasPendingPlacePreview &&
        hasSavedPlaceSelected

internal fun editingPlaceStatusTitle(placeLabel: String): String =
    "Editing ${displayPlaceName(placeLabel)}"

internal fun editingPlaceStatusDetail(monitoringEnabled: Boolean): String =
    if (monitoringEnabled) {
        "Review settings. Pause monitoring before moving this place or increasing radius."
    } else {
        "Review radius, timer, and arrival mode before monitoring."
    }

internal fun idleHomeStatusTitle(
    hasSelectedPlace: Boolean,
    hasPin: Boolean,
    armedPlaceCount: Int,
): String =
    when {
        hasSelectedPlace || hasPin -> "Ready to monitor"
        armedPlaceCount == 1 -> "1 place monitoring"
        armedPlaceCount > 1 -> "$armedPlaceCount places monitoring"
        else -> "Choose a place"
    }

internal fun idleHomeStatusDetail(
    hasSelectedPlace: Boolean,
    hasPin: Boolean,
    armedPlaceCount: Int,
): String =
    when {
        hasSelectedPlace && armedPlaceCount > 0 ->
            "Tap Monitor for this place. Other places stay live."
        hasSelectedPlace ->
            "Tap Monitor to watch arrivals here."
        hasPin ->
            "Selected place is ready"
        armedPlaceCount > 0 ->
            "Other saved places are monitoring arrivals"
        else ->
            "Search, use current location, or long-press the map."
    }

internal fun homeMonitorActionLabel(
    monitoringNeedsSetup: Boolean,
    activePlaceArmed: Boolean,
): String =
    when {
        monitoringNeedsSetup -> monitoringSetupActionLabel()
        activePlaceArmed -> "Pause monitoring"
        else -> "Monitor"
    }

internal fun homeSetupRecoveryDetail(): String =
    "Tap ${monitoringSetupActionLabel()} to restore arrival detection."

internal enum class HomeMonitorActionTarget {
    OpenSetupChecks,
    PauseMonitoring,
    StartMonitoring,
}

internal fun homeMonitorActionTarget(
    monitoringNeedsSetup: Boolean,
    activePlaceArmed: Boolean,
): HomeMonitorActionTarget =
    when {
        monitoringNeedsSetup -> HomeMonitorActionTarget.OpenSetupChecks
        activePlaceArmed -> HomeMonitorActionTarget.PauseMonitoring
        else -> HomeMonitorActionTarget.StartMonitoring
    }

internal fun pendingPreviewMutationBlockedMessage(
    destinationLabel: String? = null,
    editingSelectedPlace: Boolean = false,
): String {
    val action = if (editingSelectedPlace) "Move or cancel the preview" else "Save or cancel the preview"
    return destinationLabel
        ?.takeIf { it.isNotBlank() }
        ?.let { "$action before opening $it." }
        ?: "$action first"
}

internal fun shouldBlockMapModeSwitch(hasPendingPlacePreview: Boolean): Boolean =
    hasPendingPlacePreview

internal fun shouldBlockMapPointSelection(
    pendingPreviewMode: PlaceSelectionMode?,
    nextPreviewMode: PlaceSelectionMode,
): Boolean =
    pendingPreviewMode != null && pendingPreviewMode != nextPreviewMode

internal fun placesRowMutationEnabled(hasPendingPlacePreview: Boolean): Boolean =
    !hasPendingPlacePreview

internal fun placesRowMutationLockDetail(
    hasPendingPlacePreview: Boolean,
    editingSelectedPlace: Boolean = false,
): String? =
    if (hasPendingPlacePreview) {
        if (editingSelectedPlace) {
            "Move or cancel the preview before changing saved rows or setup."
        } else {
            "Save or cancel the preview before changing saved rows or setup."
        }
    } else {
        null
    }

internal fun placesRowSetupRecoveryEnabled(hasPendingPlacePreview: Boolean): Boolean =
    !hasPendingPlacePreview

internal data class PlacesRowActionAvailability(
    val viewMapEnabled: Boolean,
    val editSettingsEnabled: Boolean,
    val monitoringToggleEnabled: Boolean,
    val startNowEnabled: Boolean,
    val removeEnabled: Boolean,
    val setupRecoveryEnabled: Boolean,
    val lockDetail: String?,
)

internal fun placesRowActionAvailability(
    hasPendingPlacePreview: Boolean,
    timerActive: Boolean = false,
    editingSelectedPlace: Boolean = false,
): PlacesRowActionAvailability =
    PlacesRowActionAvailability(
        viewMapEnabled = placesRowMutationEnabled(hasPendingPlacePreview),
        editSettingsEnabled = placesRowMutationEnabled(hasPendingPlacePreview),
        monitoringToggleEnabled = placesRowMutationEnabled(hasPendingPlacePreview),
        startNowEnabled = placesRowMutationEnabled(hasPendingPlacePreview) && !timerActive,
        removeEnabled = placesRowMutationEnabled(hasPendingPlacePreview),
        setupRecoveryEnabled = placesRowSetupRecoveryEnabled(hasPendingPlacePreview),
        lockDetail = placesRowMutationLockDetail(
            hasPendingPlacePreview = hasPendingPlacePreview,
            editingSelectedPlace = editingSelectedPlace,
        ),
    )

internal data class PlacesRowTimerAction(
    val label: String,
    val enabled: Boolean,
    val cancelTimer: Boolean,
    val detail: String? = null,
)

internal fun placesRowActiveTimerBlockDetail(timerPlaceLabel: String = ""): String =
    displayablePlaceLabel(timerPlaceLabel)
        ?.let { "Cancel the $it timer before starting another place." }
        ?: "Cancel the running timer before starting another place."

internal fun placesRowTimerAction(
    isTimerPlace: Boolean,
    actionAvailability: PlacesRowActionAvailability,
    timerPlaceLabel: String = "",
): PlacesRowTimerAction =
    if (isTimerPlace) {
        PlacesRowTimerAction(
            label = "Cancel timer",
            enabled = true,
            cancelTimer = true,
        )
    } else {
        PlacesRowTimerAction(
            label = "Start now",
            enabled = actionAvailability.startNowEnabled,
            cancelTimer = false,
            detail = if (!actionAvailability.startNowEnabled && actionAvailability.lockDetail == null) {
                placesRowActiveTimerBlockDetail(timerPlaceLabel)
            } else {
                null
            },
        )
    }

internal fun homeDockSecondaryActionLabel(
    promptSecondaryLabel: String? = null,
    timerActive: Boolean,
    pendingPlacePreview: Boolean,
    pendingPlaceMove: Boolean,
): String = when {
    !promptSecondaryLabel.isNullOrBlank() -> promptSecondaryLabel
    pendingPlacePreview -> placesPreviewDiscardActionLabel(editingSelectedPlace = pendingPlaceMove)
    timerActive -> "Cancel timer"
    else -> "Places"
}

internal fun monitoringHealthActionEnabled(
    action: MonitoringHealthAction,
    hasPendingPlacePreview: Boolean,
): Boolean = when {
    action == MonitoringHealthAction.None -> false
    hasPendingPlacePreview -> false
    else -> true
}

internal fun monitoringHealthActionDisabledDetail(
    actionLabel: String,
    hasPendingPlacePreview: Boolean,
    editingSelectedPlace: Boolean = false,
): String? {
    if (!hasPendingPlacePreview || actionLabel.isBlank()) return null
    val action = if (editingSelectedPlace) "Move or cancel the preview" else "Save or cancel the preview"
    return "$action before using $actionLabel."
}

internal data class PlacesEmptyStateCopy(
    val title: String,
    val detail: String,
    val actionLabel: String,
)

internal fun placesEmptyStateCopy(hasPendingPlacePreview: Boolean): PlacesEmptyStateCopy =
    if (hasPendingPlacePreview) {
        PlacesEmptyStateCopy(
            title = "Unsaved place waiting",
            detail = "Review it on the map or cancel it before adding another place.",
            actionLabel = "Review on map",
        )
    } else {
        PlacesEmptyStateCopy(
            title = "No saved places",
            detail = "Add places from search, current location, or the map.",
            actionLabel = "Add place",
        )
    }

internal fun placesAddActionLabel(hasPendingPlacePreview: Boolean): String =
    if (hasPendingPlacePreview) "Review unsaved place" else "Add place"

internal fun placesBackShouldExpandPendingPreview(hasPendingPlacePreview: Boolean): Boolean =
    hasPendingPlacePreview

internal enum class HomeBackAction {
    CloseSearch,
    CollapseDock,
    ExpandPendingPreview,
    LetSystemHandle,
}

internal fun homeBackAction(
    searchPanelExpanded: Boolean,
    homeDockExpanded: Boolean,
    hasPendingPlacePreview: Boolean,
): HomeBackAction = when {
    searchPanelExpanded -> HomeBackAction.CloseSearch
    homeDockExpanded -> HomeBackAction.CollapseDock
    hasPendingPlacePreview -> HomeBackAction.ExpandPendingPreview
    else -> HomeBackAction.LetSystemHandle
}

private fun placeCountPhrase(count: Int): String =
    "$count ${if (count == 1) "place" else "places"}"

internal fun placesSummaryStatusText(
    monitoredCount: Int,
    liveCount: Int,
): String {
    val monitored = monitoredCount.coerceAtLeast(0)
    val live = liveCount.coerceIn(0, monitored)
    val needsSetup = (monitored - live).coerceAtLeast(0)
    return when {
        monitored == 0 -> "No places monitoring arrivals"
        needsSetup == 0 -> "${placeCountPhrase(live)} live"
        live == 0 -> "${placeCountPhrase(needsSetup)} needs setup"
        else -> "${placeCountPhrase(live)} live, $needsSetup needs setup"
    }
}

private fun compactPlaceLabelList(labels: List<String>, maxNames: Int): String {
    val cleanLabels = labels
        .map { it.trim().ifBlank { "Saved place" } }
        .filter { it.isNotBlank() }
    val visibleCount = maxNames.coerceAtLeast(1)
    val visibleLabels = cleanLabels.take(visibleCount)
    val remaining = (cleanLabels.size - visibleLabels.size).coerceAtLeast(0)
    return when {
        visibleLabels.isEmpty() -> ""
        remaining == 0 -> visibleLabels.joinToString(", ")
        visibleLabels.size == 1 -> "${visibleLabels.single()} and $remaining more"
        else -> "${visibleLabels.joinToString(", ")}, and $remaining more"
    }
}

internal fun placesSummaryPlaceNamesText(
    places: List<DwellPlace>,
    registeredPlaceIds: Set<String>,
    maxNamesPerGroup: Int = 3,
): String? {
    val monitoredPlaces = places.filter { it.monitoringEnabled }
    if (monitoredPlaces.isEmpty()) return null

    val liveLabels = monitoredPlaces
        .filter { registeredPlaceIds.contains(it.id) }
        .map { it.safeLabel }
    val setupLabels = monitoredPlaces
        .filterNot { registeredPlaceIds.contains(it.id) }
        .map { it.safeLabel }
    val liveText = compactPlaceLabelList(liveLabels, maxNamesPerGroup)
    val setupText = compactPlaceLabelList(setupLabels, maxNamesPerGroup)

    return when {
        liveText.isNotBlank() && setupText.isNotBlank() ->
            "Live: $liveText; needs setup: $setupText"
        liveText.isNotBlank() -> "Live: $liveText"
        setupText.isNotBlank() -> "Needs setup: $setupText"
        else -> null
    }
}

internal fun homeDockMonitoringMetaText(
    monitoredCount: Int,
    liveCount: Int,
): String {
    val monitored = monitoredCount.coerceAtLeast(0)
    val live = liveCount.coerceIn(0, monitored)
    val needsSetup = (monitored - live).coerceAtLeast(0)
    return when {
        monitored == 0 -> "Not live"
        needsSetup == 0 -> "$live live"
        live == 0 && needsSetup == 1 -> "1 needs setup"
        live == 0 -> "$needsSetup need setup"
        else -> "$live live, $needsSetup setup"
    }
}

internal fun monitoringStartFailureMessage(
    error: String?,
    placeLabel: String? = null,
): String {
    val label = placeLabel?.let(::displayablePlaceLabel)
    return when {
        error == GeofenceManager.monitoredPlaceLimitMessage() && label != null ->
            "Dwell can monitor up to ${DwellPlace.MAX_MONITORED_PLACES} places. Pause another place before monitoring $label."
        error == GeofenceManager.monitoredPlaceLimitMessage() ->
            error
        label != null ->
            "Could not monitor $label: ${error ?: "unknown error"}"
        placeLabel != null ->
            "Could not monitor this place: ${error ?: "unknown error"}"
        else ->
            "Could not start monitoring: ${error ?: "unknown error"}"
    }
}

internal fun monitoringPauseFailureMessage(
    placeLabel: String,
    error: String?,
): String {
    val label = displayablePlaceLabel(placeLabel)
    val reason = error ?: "unknown error"
    return if (label == null) {
        "Could not pause this place: $reason"
    } else {
        "Could not pause $label: $reason"
    }
}

internal fun monitoringStartFailureActionLabel(
    error: String?,
    alreadyOnPlaces: Boolean,
): String? =
    if (!alreadyOnPlaces && error == GeofenceManager.monitoredPlaceLimitMessage()) {
        "Open Places"
    } else {
        null
    }

internal fun routeAfterSavedPlaceDeletion(currentRoute: AppRoute): AppRoute =
    if (currentRoute == AppRoute.SavedZones) {
        AppRoute.SavedZones
    } else {
        currentRoute
    }

internal enum class SavedPlaceDeletionFocusAction {
    PreserveCurrentPlace,
    FocusNextPlace,
    ClearOpenPlace,
}

internal data class SavedPlaceDeletionUiTransition(
    val route: AppRoute,
    val focusAction: SavedPlaceDeletionFocusAction,
    val nextPlaceId: String?,
)

internal fun savedPlaceDeletionUiTransition(
    currentRoute: AppRoute,
    deletedPlaceWasOpen: Boolean,
    nextPlaceId: String?,
): SavedPlaceDeletionUiTransition {
    val cleanedNextPlaceId = nextPlaceId?.trim()?.takeIf { it.isNotBlank() }
    val focusAction = when {
        !deletedPlaceWasOpen -> SavedPlaceDeletionFocusAction.PreserveCurrentPlace
        cleanedNextPlaceId != null -> SavedPlaceDeletionFocusAction.FocusNextPlace
        else -> SavedPlaceDeletionFocusAction.ClearOpenPlace
    }
    return SavedPlaceDeletionUiTransition(
        route = routeAfterSavedPlaceDeletion(currentRoute),
        focusAction = focusAction,
        nextPlaceId = cleanedNextPlaceId
            .takeIf { focusAction == SavedPlaceDeletionFocusAction.FocusNextPlace },
    )
}

internal fun shouldRestoreDeletedPlaceFocusOnUndo(deletedPlaceWasOpen: Boolean): Boolean =
    deletedPlaceWasOpen

internal fun dockSetupIssue(
    setupIssue: String?,
    activePlaceArmed: Boolean,
    hasPendingPlacePreview: Boolean,
): String? =
    setupIssue
        ?.takeIf { activePlaceArmed && !hasPendingPlacePreview && it.isNotBlank() }

internal fun homeSetupStatusTitle(
    activePlaceNeedsSetup: Boolean,
    hasActivePlaceSetupIssue: Boolean,
): String =
    when {
        activePlaceNeedsSetup -> "Monitoring needs setup"
        hasActivePlaceSetupIssue -> "Needs setup"
        else -> ""
    }

internal fun isOpenPlaceDeletion(
    deletedPlaceId: String,
    selectedPlaceId: String,
    viewingPlaceId: String,
    editingPlaceId: String,
): Boolean {
    val deleted = deletedPlaceId.takeIf { it.isNotBlank() } ?: return false
    return deleted == selectedPlaceId ||
        deleted == viewingPlaceId ||
        deleted == editingPlaceId
}

internal fun placesPreviewBannerTitle(
    editingSelectedPlace: Boolean,
    targetLabel: String,
): String =
    if (editingSelectedPlace) {
        "Unsaved move for ${displayPlaceName(targetLabel)}"
    } else {
        "Unsaved place"
    }

internal fun placesPreviewBannerDetail(editingSelectedPlace: Boolean): String =
    if (editingSelectedPlace) {
        "Review the move on the map to save it, or cancel it."
    } else {
        "Review it on the map to save it, or cancel it."
    }

internal fun placesPreviewDiscardActionLabel(editingSelectedPlace: Boolean): String =
    if (editingSelectedPlace) "Cancel move" else "Cancel preview"

internal fun timerRunningStatusTitle(placeLabel: String): String =
    displayablePlaceLabel(placeLabel)
        ?.let { "Timer running at $it" }
        ?: "Timer running"

internal enum class HomePromptKind {
    Arrival,
    SwitchPlace,
    LeaveEarly,
    TimeUp,
}

internal data class HomePromptState(
    val kind: HomePromptKind,
    val title: String,
    val detail: String,
    val placeLabel: String,
    val primaryLabel: String,
    val secondaryLabel: String,
)

internal data class HomePromptActionScope(
    val kind: HomePromptKind,
    val prompt: String,
    val promptPlaceId: String,
    val promptUpdated: Long,
    val timerPlaceId: String,
    val timerStartedAt: Long,
    val timerEnd: Long,
)

internal data class TimerCancelActionScope(
    val timerPlaceId: String,
    val timerStartedAt: Long,
    val timerEnd: Long,
    val timerPlaceLabel: String,
)

internal enum class PlaceRenameNotificationRefresh {
    None,
    RunningTimer,
    ArrivalPrompt,
    SwitchPrompt,
    LeavePrompt,
    TimeUpPrompt,
}

internal fun placeRenameNotificationRefresh(
    renamedPlaceId: String,
    prompt: String,
    promptPlaceId: String,
    timerPlaceId: String,
    timerRunning: Boolean,
): PlaceRenameNotificationRefresh {
    if (renamedPlaceId.isBlank()) return PlaceRenameNotificationRefresh.None
    return when (prompt) {
        Prefs.WATCH_PROMPT_START_TIMER -> when {
            timerRunning && (promptPlaceId == renamedPlaceId || timerPlaceId == renamedPlaceId) ->
                PlaceRenameNotificationRefresh.SwitchPrompt
            !timerRunning && promptPlaceId == renamedPlaceId ->
                PlaceRenameNotificationRefresh.ArrivalPrompt
            else -> PlaceRenameNotificationRefresh.None
        }
        Prefs.WATCH_PROMPT_LEAVE_EARLY -> when {
            timerRunning && (promptPlaceId == renamedPlaceId || timerPlaceId == renamedPlaceId) ->
                PlaceRenameNotificationRefresh.LeavePrompt
            else -> PlaceRenameNotificationRefresh.None
        }
        Prefs.WATCH_PROMPT_TIME_UP -> when {
            timerPlaceId == renamedPlaceId || promptPlaceId == renamedPlaceId ->
                PlaceRenameNotificationRefresh.TimeUpPrompt
            else -> PlaceRenameNotificationRefresh.None
        }
        else -> when {
            timerRunning && timerPlaceId == renamedPlaceId ->
                PlaceRenameNotificationRefresh.RunningTimer
            else -> PlaceRenameNotificationRefresh.None
        }
    }
}

internal fun acceptsTimerCancelConfirmation(
    scope: TimerCancelActionScope?,
    currentTimerPlaceId: String,
    currentTimerStartedAt: Long,
    currentTimerEnd: Long,
    now: Long,
): Boolean {
    val actionScope = scope ?: return false
    if (currentTimerEnd <= now) return false
    return NotificationActionReceiver.acceptsScopedTimerAction(
        currentTimerPlaceId = currentTimerPlaceId,
        currentTimerStartedAt = currentTimerStartedAt,
        currentTimerEnd = currentTimerEnd,
        actionTimerPlaceId = actionScope.timerPlaceId.takeIf { it.isNotBlank() },
        actionTimerStartedAt = actionScope.timerStartedAt,
        actionTimerEnd = actionScope.timerEnd,
    )
}

internal fun acceptsHomePromptAction(
    scope: HomePromptActionScope?,
    currentPrompt: String,
    currentPromptPlaceId: String,
    currentPromptUpdated: Long,
    currentTimerPlaceId: String,
    currentTimerStartedAt: Long,
    currentTimerEnd: Long,
    now: Long,
): Boolean {
    val actionScope = scope ?: return false
    val promptMatches = NotificationActionReceiver.acceptsScopedPromptAction(
        currentPrompt = currentPrompt,
        currentPromptPlaceId = currentPromptPlaceId,
        currentPromptUpdated = currentPromptUpdated,
        actionPrompt = actionScope.prompt,
        actionPromptUpdated = actionScope.promptUpdated,
        actionPlaceId = actionScope.promptPlaceId.takeIf { it.isNotBlank() },
    )
    if (!promptMatches) return false

    val timerRunning = currentTimerEnd > now
    return when (actionScope.kind) {
        HomePromptKind.Arrival -> !timerRunning
        HomePromptKind.SwitchPlace,
        HomePromptKind.LeaveEarly ->
            timerRunning &&
                NotificationActionReceiver.acceptsScopedTimerAction(
                    currentTimerPlaceId = currentTimerPlaceId,
                    currentTimerStartedAt = currentTimerStartedAt,
                    currentTimerEnd = currentTimerEnd,
                    actionTimerPlaceId = actionScope.timerPlaceId.takeIf { it.isNotBlank() },
                    actionTimerStartedAt = actionScope.timerStartedAt,
                    actionTimerEnd = actionScope.timerEnd,
                )
        HomePromptKind.TimeUp ->
            !timerRunning &&
                currentTimerPlaceId == actionScope.timerPlaceId
    }
}

internal fun homePromptState(
    prompt: String,
    promptPlaceLabel: String,
    timerRunning: Boolean,
    timerPlaceLabel: String,
    durationMinutes: Int,
    timerEnd: Long,
    now: Long,
): HomePromptState? {
    val promptPlace = promptPlaceLabel.takeIf { it.isNotBlank() }
    val timerPlace = timerPlaceLabel.takeIf { it.isNotBlank() }
    return when {
        prompt == Prefs.WATCH_PROMPT_START_TIMER && timerRunning -> HomePromptState(
            kind = HomePromptKind.SwitchPlace,
            title = Notifications.switchQuestionTitle(promptPlace),
            detail = Notifications.switchQuestionText(promptPlace, timerPlace),
            placeLabel = promptPlace ?: "New place",
            primaryLabel = "Switch",
            secondaryLabel = "Keep current",
        )
        prompt == Prefs.WATCH_PROMPT_START_TIMER -> HomePromptState(
            kind = HomePromptKind.Arrival,
            title = Notifications.arrivalQuestionTitle(promptPlace),
            detail = "${Notifications.formatDuration(durationMinutes)} timer for ${promptPlace ?: "this place"}.",
            placeLabel = promptPlace ?: "Arrived place",
            primaryLabel = "Start",
            secondaryLabel = "Not now",
        )
        prompt == Prefs.WATCH_PROMPT_LEAVE_EARLY && timerRunning -> {
            val remaining = (timerEnd - now).coerceAtLeast(0L)
            HomePromptState(
                kind = HomePromptKind.LeaveEarly,
                title = Notifications.exitQuestionTitle(timerPlace ?: promptPlace),
                detail = "Keep the ${(timerPlace ?: promptPlace) ?: "current"} timer? ${formatRemainingPromptDuration(remaining)} left.",
                placeLabel = timerPlace ?: promptPlace ?: "Timer place",
                primaryLabel = "Keep timer",
                secondaryLabel = "Cancel timer",
            )
        }
        prompt == Prefs.WATCH_PROMPT_TIME_UP -> {
            val place = timerPlace ?: promptPlace
            HomePromptState(
                kind = HomePromptKind.TimeUp,
                title = place?.let { "Time's up at $it" } ?: "Time's up",
                detail = "${Notifications.formatDuration(durationMinutes)} timer complete. Extend or mark done.",
                placeLabel = place ?: "Dwell timer",
                primaryLabel = "Extend 30m",
                secondaryLabel = "Done",
            )
        }
        else -> null
    }
}

internal fun formatRemainingPromptDuration(remainingMillis: Long): String {
    val totalMinutes = (remainingMillis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        remainingMillis in 1 until 60_000L -> "<1m"
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }
}

internal fun alreadyInsideResultMessage(
    placeLabel: String,
    decision: ArrivalDecision,
    timerRunningForPlace: Boolean,
    waitReason: AlreadyInsideWaitReason? = null,
): String {
    val label = placeLabel.takeIf { it.isNotBlank() }
    return when {
        decision == ArrivalDecision.START_TIMER ->
            label?.let { "You are already at $it - timer started" }
                ?: "You are already here - timer started"
        decision == ArrivalDecision.ASK_TO_START ->
            label?.let { "Dwell thinks you are at $it. Confirm start from Home or the notification." }
                ?: "Dwell thinks you are here. Confirm start from Home or the notification."
        timerRunningForPlace ->
            label?.let { "Timer already running at $it" }
                ?: "Timer already running here"
        waitReason == AlreadyInsideWaitReason.PLACE_NOT_MONITORED ->
            label?.let { "$it is paused. Turn on Monitor to watch arrivals." }
                ?: "This place is paused. Turn on Monitor to watch arrivals."
        waitReason == AlreadyInsideWaitReason.LOCATION_PERMISSION_MISSING ->
            label?.let { "$it is monitoring arrivals. Allow location to check if you are already there." }
                ?: "Monitoring on. Allow location to check if you are already there."
        waitReason == AlreadyInsideWaitReason.LOCATION_UNAVAILABLE ->
            label?.let { "$it is monitoring arrivals. Current location was unavailable, so Dwell will keep watching." }
                ?: "Monitoring on. Current location was unavailable, so Dwell will keep watching."
        waitReason == AlreadyInsideWaitReason.OUTSIDE_PLACE ->
            label?.let { "$it is monitoring arrivals. Dwell will start when you arrive." }
                ?: "Monitoring on. Dwell will start when you arrive."
        waitReason == AlreadyInsideWaitReason.TIMER_ALREADY_RUNNING ->
            label?.let { "$it is monitoring arrivals, but another timer is already running." }
                ?: "Monitoring on, but another timer is already running."
        waitReason == AlreadyInsideWaitReason.LOW_CONFIDENCE ->
            label?.let { "$it is monitoring arrivals. Dwell will wait for a stronger location signal." }
                ?: "Monitoring on. Dwell will wait for a stronger location signal."
        else ->
            label?.let { "$it is monitoring arrivals. Other monitored places stay live." }
                ?: "Monitoring on - the timer will start when you arrive"
    }
}

internal enum class AlreadyInsideWaitReason {
    PLACE_NOT_MONITORED,
    LOCATION_PERMISSION_MISSING,
    LOCATION_UNAVAILABLE,
    OUTSIDE_PLACE,
    TIMER_ALREADY_RUNNING,
    LOW_CONFIDENCE,
}

internal data class AlreadyInsideCheckResult(
    val decision: ArrivalDecision,
    val waitReason: AlreadyInsideWaitReason? = null,
)

internal fun shouldRunAlreadyInsideCheck(placeId: String?, place: DwellPlace?): Boolean {
    val scopedPlaceId = placeId?.takeIf { it.isNotBlank() } ?: return true
    return place?.id == scopedPlaceId && place.monitoringEnabled
}

internal fun shouldEvaluateAlreadyInsideConfidence(
    distanceMeters: Float,
    radiusMeters: Float,
    accuracyMeters: Float?,
): Boolean {
    val distance = distanceMeters.takeIf { it.isFinite() && it >= 0f } ?: return false
    val radius = DwellRadius.normalize(radiusMeters)
    val accuracyAllowance = (accuracyMeters?.takeIf { it.isFinite() && it >= 0f } ?: 150f)
        .coerceIn(50f, 150f)
    return distance <= radius + accuracyAllowance
}

internal data class LocationPermissionResume(
    val selectAsZone: Boolean,
    val expandDock: Boolean,
    val selectionMode: PlaceSelectionMode? = null,
    val targetPlaceId: String? = null,
)

internal const val MANUAL_TIMER_START_RESUME_TTL_MS: Long = 2 * 60 * 1000L

internal data class ManualTimerStartRequest(
    val placeId: String?,
    val editablePlaceId: String?,
    val durationMinutes: Int,
    val requestedAtMillis: Long,
)

internal fun locationPermissionResume(
    requested: Boolean,
    selectAsZone: Boolean,
    expandDock: Boolean,
    selectionMode: PlaceSelectionMode? = null,
    targetPlaceId: String? = null,
    hasFineLocation: Boolean,
): LocationPermissionResume? =
    if (requested && hasFineLocation) {
        LocationPermissionResume(
            selectAsZone = selectAsZone,
            expandDock = expandDock,
            selectionMode = selectionMode,
            targetPlaceId = targetPlaceId?.takeIf { it.isNotBlank() },
        )
    } else {
        null
    }

internal fun shouldLaunchCurrentLocationPermissionRequest(
    hasFineLocation: Boolean,
    permissionUiAlreadyActive: Boolean,
): Boolean =
    !hasFineLocation && !permissionUiAlreadyActive

internal fun currentLocationResultStillCurrent(
    requestGeneration: Long,
    activeGeneration: Long,
): Boolean = requestGeneration == activeGeneration

internal fun shouldInvalidateCurrentLocationForSelection(analyticsSource: String): Boolean =
    analyticsSource != "current_location"

internal fun shouldLaunchManualTimerNotificationPermissionRequest(
    notificationsGranted: Boolean,
    permissionUiAlreadyActive: Boolean,
): Boolean =
    !notificationsGranted && !permissionUiAlreadyActive

internal fun manualTimerNotificationPermissionPrompt(
    permissionUiAlreadyActive: Boolean,
): String =
    if (permissionUiAlreadyActive) {
        "Finish the open Android permission prompt first."
    } else {
        "Grant notifications to start the timer"
    }

internal fun manualTimerNotificationPermissionStillPending(
    notificationsGranted: Boolean,
    permissionRequestInFlight: Boolean,
    requestAgeMillis: Long,
    ttlMillis: Long = MANUAL_TIMER_START_RESUME_TTL_MS,
): Boolean =
    !notificationsGranted &&
        permissionRequestInFlight &&
        requestAgeMillis in 0..ttlMillis

internal fun manualTimerNotificationDeniedMessage(): String =
    "Allow notifications to start the timer"

internal fun missingManualTimerStartPlaceMessage(): String =
    "That saved place is no longer available. Pick a place and tap Start now again."

internal fun manualTimerStartBlockedAfterPermissionMessage(
    targetPlaceExists: Boolean,
    timerRunning: Boolean,
    pendingPlaceId: String?,
    runningTimerPlaceId: String,
    runningTimerPlaceLabel: String,
    notificationsGranted: Boolean,
    requestAgeMillis: Long,
    ttlMillis: Long = MANUAL_TIMER_START_RESUME_TTL_MS,
): String? {
    if (!targetPlaceExists) return missingManualTimerStartPlaceMessage()
    if (timerRunning) {
        val pendingPlace = pendingPlaceId?.takeIf { it.isNotBlank() }
        val runningPlace = runningTimerPlaceId.takeIf { it.isNotBlank() }
        return if (pendingPlace != null && pendingPlace == runningPlace) {
            timerRunningStatusTitle(runningTimerPlaceLabel)
        } else {
            placesRowActiveTimerBlockDetail(runningTimerPlaceLabel)
        }
    }
    return if (!notificationsGranted && requestAgeMillis <= ttlMillis) {
        manualTimerNotificationDeniedMessage()
    } else {
        null
    }
}

internal fun manualTimerStartAfterNotificationPermission(
    request: ManualTimerStartRequest?,
    notificationsGranted: Boolean,
    timerRunning: Boolean,
    targetPlaceExists: Boolean,
    nowMillis: Long,
    ttlMillis: Long = MANUAL_TIMER_START_RESUME_TTL_MS,
): ManualTimerStartRequest? =
    request?.takeIf {
        notificationsGranted &&
            !timerRunning &&
            targetPlaceExists &&
            nowMillis - it.requestedAtMillis in 0..ttlMillis
    }

internal fun placeMonitoringStatusLabel(
    monitoringEnabled: Boolean,
    isRegistered: Boolean,
    isTimerPlace: Boolean,
): String = when {
    isTimerPlace -> "Timer here"
    monitoringEnabled && isRegistered -> "Monitoring live"
    monitoringEnabled -> "Needs setup"
    else -> "Paused"
}

internal fun placeNeedsMonitoringSetup(
    monitoringEnabled: Boolean,
    isRegistered: Boolean,
): Boolean =
    monitoringEnabled && !isRegistered

internal fun placeRoleLabels(
    isViewing: Boolean,
    isEditing: Boolean,
    isTimerPlace: Boolean,
): List<String> = buildList {
    if (isTimerPlace) add("Timer here")
    if (isEditing) add("Editing")
    if (isViewing && !isEditing) add("Viewing")
}

internal fun timerCancelTitle(placeLabel: String): String =
    displayablePlaceLabel(placeLabel)
        ?.let { "Cancel timer for $it?" }
        ?: "Cancel timer?"

internal fun timerCancelDialogPlaceLabel(
    scope: TimerCancelActionScope,
    currentTimerPlaceId: String,
    currentTimerPlaceLabel: String,
): String {
    val scopedPlaceId = scope.timerPlaceId.takeIf { it.isNotBlank() }
    val currentPlaceId = currentTimerPlaceId.takeIf { it.isNotBlank() }
    return if (scopedPlaceId != null && scopedPlaceId == currentPlaceId && currentTimerPlaceLabel.isNotBlank()) {
        currentTimerPlaceLabel
    } else {
        scope.timerPlaceLabel
    }
}

internal fun placeRemovalTitle(placeLabel: String): String =
    "Remove ${displayablePlaceLabel(placeLabel) ?: "place"}?"

internal fun placeRemovalDetail(
    placeLabel: String,
    monitoringEnabled: Boolean,
    isTimerPlace: Boolean,
): String = buildList {
    val label = displayablePlaceLabel(placeLabel) ?: "this place"
    add("This removes $label from Places.")
    if (monitoringEnabled) add("Arrival monitoring for $label stops.")
    if (isTimerPlace) add("The running timer here will be canceled.")
}.joinToString(" ")

internal fun placeRemovedMessage(placeLabel: String): String =
    "${displayablePlaceLabel(placeLabel) ?: "Place"} removed from Places"

internal data class PlaceDeleteUndoSnackbarPlan(
    val message: String,
    val actionLabel: String,
    val dismissCurrentSnackbar: Boolean,
)

internal fun placeDeleteUndoSnackbarPlan(
    placeLabel: String,
    currentSnackbarVisible: Boolean,
): PlaceDeleteUndoSnackbarPlan =
    PlaceDeleteUndoSnackbarPlan(
        message = placeRemovedMessage(placeLabel),
        actionLabel = "Undo",
        dismissCurrentSnackbar = currentSnackbarVisible,
    )

internal fun placeRestorePausedByMonitoringLimit(
    deletedPlaceWasMonitoring: Boolean,
    restoredPlaceMonitoring: Boolean,
): Boolean =
    deletedPlaceWasMonitoring && !restoredPlaceMonitoring

internal fun placeRestoredMessage(
    placeLabel: String,
    monitoringPausedByLimit: Boolean = false,
    focusRestored: Boolean = false,
): String {
    val base = if (focusRestored) {
        "${displayablePlaceLabel(placeLabel) ?: "Place"} restored and selected"
    } else {
        "${displayablePlaceLabel(placeLabel) ?: "Place"} restored to Places"
    }
    return if (monitoringPausedByLimit) {
        "$base. Monitoring is paused because the live-place limit is full."
    } else {
        base
    }
}

internal fun placePausedMessage(placeLabel: String): String =
    "${displayablePlaceLabel(placeLabel) ?: "Place"} paused. Other monitored places stay live."

internal enum class PlacesBackAction {
    DismissMonitorDialog,
    DismissDeleteDialog,
    LeavePlaces,
}

internal fun placesBackAction(
    monitorDialogVisible: Boolean = false,
    deleteDialogVisible: Boolean,
): PlacesBackAction =
    when {
        monitorDialogVisible -> PlacesBackAction.DismissMonitorDialog
        deleteDialogVisible -> PlacesBackAction.DismissDeleteDialog
        else -> PlacesBackAction.LeavePlaces
    }

internal fun latestDialogPlace(
    pendingPlaceId: String?,
    places: List<DwellPlace>,
): DwellPlace? {
    val placeId = pendingPlaceId?.takeIf { it.isNotBlank() } ?: return null
    return places.firstOrNull { it.id == placeId }
}

internal data class PlacesMonitoringConfirmationCopy(
    val title: String,
    val detail: String,
    val confirmLabel: String,
    val dismissLabel: String,
)

internal fun placesMonitoringConfirmationCopy(place: DwellPlace): PlacesMonitoringConfirmationCopy =
    PlacesMonitoringConfirmationCopy(
        title = "Monitor ${displayPlaceName(place.safeLabel)}?",
        detail = "Dwell will monitor ${displayPlaceName(place.safeLabel)} with a ${place.radiusMeters.roundToInt()} m radius, ${Notifications.formatDuration(place.durationMinutes)} timer, and ${arrivalModeLabel(place.autoStart)} arrival mode.",
        confirmLabel = "Start monitoring",
        dismissLabel = "Not now",
    )

internal enum class MonitoringHealthAction {
    None,
    OpenSettings,
    RefreshMonitoring,
    OpenExactAlarm,
    OpenBattery,
}

internal data class MonitoringHealth(
    val title: String,
    val detail: String,
    val stateLabel: String,
    val healthy: Boolean,
    val actionLabel: String,
    val action: MonitoringHealthAction,
)

private fun countText(count: Int, singular: String): String =
    "$count $singular${if (count == 1) "" else "s"}"

internal fun monitoringSetupActionLabel(): String = "Finish setup"

internal fun placeSetupActionLabel(): String = "Fix setup"

internal fun timerRiskActionLabel(): String = "Allow alarms"

internal fun batteryRiskActionLabel(): String = "Review battery"

internal fun unsavedRuntimeActionsBlockedDetail(): String =
    "Save, move, or cancel the preview before Monitor, Start now, Remove, or Pause monitoring can run."

internal fun monitoringHealthState(
    placesCount: Int,
    monitoredCount: Int,
    liveCount: Int,
    setupIssue: String?,
    monitoringError: String,
    exactAlarmAllowed: Boolean,
    batteryReliabilityStatus: BatteryReliabilityStatus,
): MonitoringHealth {
    val issue = setupIssue
        ?.takeIf { it.isNotBlank() }
        ?: monitoringError.takeIf { it.isNotBlank() }
    val batteryRisk = batteryNeedsReliabilityReview(batteryReliabilityStatus)

    return when {
        placesCount <= 0 -> MonitoringHealth(
            title = "No places yet",
            detail = "Add a place to start arrival detection.",
            stateLabel = "No places",
            healthy = false,
            actionLabel = "",
            action = MonitoringHealthAction.None,
        )
        monitoredCount <= 0 -> MonitoringHealth(
            title = "Monitoring paused",
            detail = "Turn on Monitor for a place when you want Dwell to watch arrivals.",
            stateLabel = "Paused",
            healthy = false,
            actionLabel = "",
            action = MonitoringHealthAction.None,
        )
        issue != null && Prefs.isMonitoringLimitNormalizationMessage(issue) -> MonitoringHealth(
            title = "Some places paused",
            detail = issue,
            stateLabel = "Limit",
            healthy = false,
            actionLabel = "",
            action = MonitoringHealthAction.None,
        )
        issue != null -> MonitoringHealth(
            title = "Monitoring needs setup",
            detail = issue,
            stateLabel = "Needs setup",
            healthy = false,
            actionLabel = monitoringSetupActionLabel(),
            action = MonitoringHealthAction.OpenSettings,
        )
        liveCount <= 0 -> MonitoringHealth(
            title = "Monitoring needs setup",
            detail = "${countText(monitoredCount, "place")} enabled, but none are live yet. Tap ${monitoringSetupActionLabel()} to restore arrival detection.",
            stateLabel = "Needs setup",
            healthy = false,
            actionLabel = monitoringSetupActionLabel(),
            action = MonitoringHealthAction.RefreshMonitoring,
        )
        liveCount < monitoredCount -> MonitoringHealth(
            title = "Some places need setup",
            detail = "${countText(liveCount, "place")} live; ${countText(monitoredCount - liveCount, "place")} need setup. Tap ${monitoringSetupActionLabel()} to restore arrival detection.",
            stateLabel = "Partial",
            healthy = false,
            actionLabel = monitoringSetupActionLabel(),
            action = MonitoringHealthAction.RefreshMonitoring,
        )
        !exactAlarmAllowed -> MonitoringHealth(
            title = "Timers may be delayed",
            detail = "Exact alarms are off. Arrival detection can work, but time-up alerts may arrive late.",
            stateLabel = "Timer risk",
            healthy = false,
            actionLabel = timerRiskActionLabel(),
            action = MonitoringHealthAction.OpenExactAlarm,
        )
        batteryRisk -> MonitoringHealth(
            title = "Monitoring live, battery may delay",
            detail = batteryReliabilityStatus.detail,
            stateLabel = "Battery risk",
            healthy = false,
            actionLabel = batteryRiskActionLabel(),
            action = MonitoringHealthAction.OpenBattery,
        )
        else -> MonitoringHealth(
            title = "Monitoring live",
            detail = "${countText(liveCount, "place")} live. Phone owns arrival detection; watch mirrors timers.",
            stateLabel = "Healthy",
            healthy = true,
            actionLabel = "",
            action = MonitoringHealthAction.None,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DwellScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(Prefs.isSignedIn(context)) }
    var onboardingComplete by remember { mutableStateOf(Prefs.isOnboardingComplete(context)) }
    var route by remember { mutableStateOf(AppRoute.Home) }
    var setupChecksBackRoute by remember { mutableStateOf(AppRoute.Settings) }
    var authInFlight by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var permVersion by remember { mutableIntStateOf(0) }
    var diagnosticsRefresh by remember { mutableIntStateOf(0) }
    var insightsRefresh by remember { mutableIntStateOf(0) }
    val restoredCurrentLocationResume = remember { Prefs.getPendingCurrentLocationResume(context) }
    var locateAfterPermission by remember { mutableStateOf(restoredCurrentLocationResume != null) }
    var locateAfterPermissionSelectAsZone by remember {
        mutableStateOf(restoredCurrentLocationResume?.selectAsZone ?: true)
    }
    var locateAfterPermissionExpandDock by remember {
        mutableStateOf(restoredCurrentLocationResume?.expandDock ?: true)
    }
    var locateAfterPermissionSelectionMode by remember {
        mutableStateOf(
            restoredCurrentLocationResume?.selectionModeName
                ?.let { runCatching { PlaceSelectionMode.valueOf(it) }.getOrNull() }
        )
    }
    var locateAfterPermissionTargetPlaceId by remember {
        mutableStateOf(restoredCurrentLocationResume?.targetPlaceId)
    }
    var showBackgroundLocationDisclosure by remember { mutableStateOf(false) }
    var pendingMonitoringResume by remember {
        mutableStateOf(
            Prefs.getPendingMonitoringResume(context)
                ?.let { PendingMonitoringResume(it.placeId) }
        )
    }
    var shownPendingMonitoringSetupStep by remember {
        mutableStateOf<MonitoringSetupRecoveryStep?>(null)
    }
    var pendingManualTimerStart by remember {
        mutableStateOf(
            Prefs.getPendingManualTimerStart(context)
                ?.let {
                    ManualTimerStartRequest(
                        placeId = it.placeId,
                        editablePlaceId = it.editablePlaceId,
                        durationMinutes = it.durationMinutes,
                        requestedAtMillis = it.requestedAtMillis,
                    )
                }
        )
    }
    var monitoringPermissionRequestInFlight by remember { mutableStateOf(false) }
    var appSettingsReturnInFlight by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        monitoringPermissionRequestInFlight = false
        permVersion++
    }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        monitoringPermissionRequestInFlight = false
        permVersion++
    }
    val appSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        appSettingsReturnInFlight = false
        permVersion++
    }

    if (!signedIn) {
        AuthScreen(
            authInFlight = authInFlight,
            authError = authError,
            onGoogleSignIn = {
                if (!authInFlight) {
                    scope.launch {
                        authInFlight = true
                        authError = null
                        runCatching {
                            GoogleAuth.signIn(context)
                        }.onSuccess { account ->
                            val saved = BackendClient.upsertSession(
                                context = context,
                                provider = "google",
                                displayName = account.displayName,
                                email = account.email,
                                googleSubject = account.googleSubject,
                                googleIdToken = account.idToken,
                            )
                            if (!saved) {
                                authError = "Google sign-in worked, but Dwell could not save the session. Check connection and try again."
                                BackendClient.trackEvent(context, "auth_google_backend_failed")
                                authInFlight = false
                                return@onSuccess
                            }
                            Prefs.setSignedIn(context, true)
                            Prefs.saveAccount(
                                context,
                                provider = "google",
                                displayName = account.displayName,
                                email = account.email,
                            )
                            signedIn = true
                            onboardingComplete = Prefs.isOnboardingComplete(context)
                            route = AppRoute.Home
                            BackendClient.trackEvent(context, "auth_continue_google")
                        }.onFailure { error ->
                            authError = error.message ?: "Google sign-in failed."
                            BackendClient.trackEvent(
                                context,
                                "auth_google_failed",
                                mapOf("message" to authError),
                            )
                        }
                        authInFlight = false
                    }
                }
            },
            onContinueLocal = {
                if (!authInFlight) {
                    Prefs.setSignedIn(context, true)
                    Prefs.saveAccount(context, provider = "local")
                    signedIn = true
                    onboardingComplete = Prefs.isOnboardingComplete(context)
                    route = AppRoute.Home
                    scope.launch {
                        BackendClient.upsertSession(context, provider = "local")
                        BackendClient.trackEvent(context, "auth_continue_local")
                    }
                }
            },
        )
        return
    }

    var homeDockExpanded by remember { mutableStateOf(false) }
    var places by remember { mutableStateOf(Prefs.getPlaces(context)) }
    var registeredPlaceIds by remember { mutableStateOf(Prefs.getRegisteredPlaceIds(context)) }
    var monitoringError by remember { mutableStateOf(Prefs.getMonitoringError(context)) }
    val initialSelectedPlaceId = remember { Prefs.getActivePlace(context)?.id.orEmpty() }
    var selectedPlaceId by remember { mutableStateOf(initialSelectedPlaceId) }
    var placeSelectionMode by remember {
        mutableStateOf(initialPlaceSelectionMode(Prefs.hasPlace(context)))
    }
    var pin by remember {
        mutableStateOf(
            if (Prefs.hasPlace(context))
                MapPoint(Prefs.getLat(context), Prefs.getLon(context))
            else null
        )
    }
    var selectedPlaceLabel by remember {
        mutableStateOf(if (Prefs.hasPlace(context)) Prefs.getPlaceLabel(context) else "")
    }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var searchSuggestions by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var searchPanelExpanded by remember { mutableStateOf(false) }
    var searchFocusRequest by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var searchingQueryKey by remember { mutableStateOf("") }
    var submittedSearchKey by remember { mutableStateOf("") }
    var mobileSearchConfig by remember { mutableStateOf(Prefs.getMobileSearchConfig(context)) }
    var mobileMapConfig by remember { mutableStateOf(Prefs.getMobileMapConfig(context)) }
    var mapReadyVersion by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    var currentLocationRequestGeneration by remember { mutableLongStateOf(0L) }
    var lastSearchAt by remember { mutableLongStateOf(0L) }
    val searchCache = remember { mutableMapOf<String, CachedLocationSearch>() }
    var radius by remember { mutableFloatStateOf(Prefs.getRadius(context)) }
    var defaultRadius by remember { mutableFloatStateOf(Prefs.getDefaultRadius(context)) }
    var durationText by remember {
        val h = Prefs.getDurationMinutes(context) / 60.0
        val rounded = formatHoursInput(h)
        mutableStateOf(rounded.ifEmpty { "4.5" })
    }
    var defaultDurationText by remember {
        val h = Prefs.getDefaultDurationMinutes(context) / 60.0
        val rounded = formatHoursInput(h)
        mutableStateOf(rounded.ifEmpty { "4.5" })
    }
    var defaultAutoStart by remember { mutableStateOf(Prefs.getDefaultAutoStart(context)) }
    var armed by remember { mutableStateOf(Prefs.isArmed(context)) }
    var timerEnd by remember { mutableLongStateOf(Prefs.getTimerEnd(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingPlacePreview by remember { mutableStateOf<PendingPlacePreview?>(null) }
    var viewingPlaceId by remember { mutableStateOf(selectedPlaceId) }
    var editingPlaceId by remember {
        mutableStateOf(initialEditingPlaceId(placeSelectionMode, initialSelectedPlaceId))
    }
    var pendingTimerCancelScope by remember { mutableStateOf<TimerCancelActionScope?>(null) }
    var deletedPlaceUndo by remember { mutableStateOf<DeletedPlaceUndo?>(null) }
    var activeLongActionKey by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun launchAppSettings() {
        appSettingsReturnInFlight = true
        runCatching {
            appSettingsLauncher.launch(dwellAppSettingsIntent(context))
        }.onFailure {
            appSettingsReturnInFlight = false
            openDwellAppSettings(context)
        }
    }

    fun openExactAlarmSettings() {
        runCatching {
            appSettingsLauncher.launch(dwellExactAlarmSettingsIntent(context))
        }.onFailure {
            launchAppSettings()
        }
    }

    fun showLongActionMessage(
        message: String,
        actionLabel: String,
        onAction: () -> Unit,
    ) {
        val key = longActionMessageKey(message, actionLabel)
        if (
            !shouldEnqueueLongActionMessage(
                activeLongActionKey = activeLongActionKey,
                nextLongActionKey = key,
            )
        ) {
            return
        }
        activeLongActionKey = key
        scope.launch {
            try {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onAction()
                }
            } finally {
                if (activeLongActionKey == key) {
                    activeLongActionKey = null
                }
            }
        }
    }

    fun showBackgroundLocationHelp() {
        showLongActionMessage(
            message = backgroundLocationHelpMessage(),
            actionLabel = backgroundLocationHelpActionLabel(),
            onAction = ::launchAppSettings,
        )
    }

    fun showBatteryHelp(status: BatteryReliabilityStatus = BatteryReliability.status(context)) {
        showLongActionMessage(
            message = batteryHelpMessage(status),
            actionLabel = batteryHelpActionLabel(),
            onAction = {
                val opened = BatteryReliability.openSettings(context)
                if (!opened) toast("Could not open battery settings on this device")
            },
        )
    }

    fun showMonitoringStartFailure(
        error: String?,
        alreadyOnPlaces: Boolean = false,
        placeLabel: String? = null,
    ) {
        val message = monitoringStartFailureMessage(error, placeLabel)
        val actionLabel = monitoringStartFailureActionLabel(
            error = error,
            alreadyOnPlaces = alreadyOnPlaces,
        )
        if (actionLabel == null) {
            toast(message)
        } else {
            showLongActionMessage(
                message = message,
                actionLabel = actionLabel,
                onAction = {
                    searchPanelExpanded = false
                    focusManager.clearFocus()
                    homeDockExpanded = false
                    route = AppRoute.SavedZones
                },
            )
        }
    }

    fun pendingPreviewBlockedMessage(destinationLabel: String? = null): String =
        pendingPreviewMutationBlockedMessage(
            destinationLabel = destinationLabel,
            editingSelectedPlace = pendingPlacePreview?.mode == PlaceSelectionMode.EditSelected,
        )

    fun blockPendingPlacePreview(destinationLabel: String? = null): Boolean {
        if (!shouldBlockMapModeSwitch(pendingPlacePreview != null)) return false
        homeDockExpanded = true
        toast(pendingPreviewBlockedMessage(destinationLabel))
        return true
    }

    fun applyActivePlace(place: DwellPlace) {
        pendingPlacePreview = null
        Prefs.setActivePlace(context, place.id)
        selectedPlaceId = place.id
        viewingPlaceId = place.id
        editingPlaceId = if (placeSelectionMode == PlaceSelectionMode.EditSelected) place.id else ""
        pin = MapPoint(place.latitude, place.longitude)
        selectedPlaceLabel = place.safeLabel
        radius = place.radiusMeters
        durationText = formatHoursInput(place.durationMinutes / 60.0)
    }

    fun viewPlaceOnMap(
        place: DwellPlace,
        forceDiscardPreview: Boolean = false,
        blockedDestinationLabel: String? = null,
    ): Boolean {
        if (!forceDiscardPreview && blockPendingPlacePreview(blockedDestinationLabel)) return false
        pendingPlacePreview = null
        placeSelectionMode = PlaceSelectionMode.ViewSelected
        selectedPlaceId = place.id
        viewingPlaceId = place.id
        editingPlaceId = ""
        pin = MapPoint(place.latitude, place.longitude)
        selectedPlaceLabel = place.safeLabel
        radius = place.radiusMeters
        durationText = formatHoursInput(place.durationMinutes / 60.0)
        return true
    }

    fun beginCreatePlace(
        forceDiscardPreview: Boolean = false,
        blockedDestinationLabel: String? = null,
    ): Boolean {
        if (!forceDiscardPreview && blockPendingPlacePreview(blockedDestinationLabel)) return false
        pendingPlacePreview = null
        placeSelectionMode = PlaceSelectionMode.CreateNew
        selectedPlaceId = ""
        viewingPlaceId = ""
        editingPlaceId = ""
        pin = null
        selectedPlaceLabel = ""
        radius = Prefs.getDefaultRadius(context)
        durationText = formatHoursInput(Prefs.getDefaultDurationMinutes(context) / 60.0)
        return true
    }

    fun clearOpenPlaceAfterDeletion() {
        pendingPlacePreview = null
        placeSelectionMode = PlaceSelectionMode.CreateNew
        selectedPlaceId = ""
        viewingPlaceId = ""
        editingPlaceId = ""
        pin = null
        selectedPlaceLabel = ""
        radius = Prefs.getDefaultRadius(context)
        durationText = formatHoursInput(Prefs.getDefaultDurationMinutes(context) / 60.0)
    }

    fun beginEditPlace(
        place: DwellPlace,
        expandDock: Boolean = false,
        forceDiscardPreview: Boolean = false,
        blockedDestinationLabel: String? = null,
    ): Boolean {
        if (!forceDiscardPreview && blockPendingPlacePreview(blockedDestinationLabel)) return false
        placeSelectionMode = PlaceSelectionMode.EditSelected
        applyActivePlace(place)
        editingPlaceId = place.id
        homeDockExpanded = expandDock
        return true
    }

    fun beginEditActivePlace(
        expandDock: Boolean = false,
        blockedDestinationLabel: String? = null,
    ): Boolean {
        val target = selectedPlaceId
            .takeIf { it.isNotBlank() }
            ?.let { Prefs.getPlace(context, it) }
            ?: viewingPlaceId
                .takeIf { it.isNotBlank() }
                ?.let { Prefs.getPlace(context, it) }
            ?: Prefs.getActivePlace(context)
            ?: places.firstOrNull()
            ?: return false
        return beginEditPlace(
            target,
            expandDock = expandDock,
            blockedDestinationLabel = blockedDestinationLabel,
        )
    }

    fun refreshPlaces(syncActivePlace: Boolean = false) {
        places = Prefs.getPlaces(context)
        registeredPlaceIds = Prefs.getRegisteredPlaceIds(context)
        monitoringError = Prefs.getMonitoringError(context)
        armed = Prefs.isArmed(context)
        if (syncActivePlace) {
            val openPlaceStillExists =
                pendingPlacePreview == null &&
                    placeSelectionMode != PlaceSelectionMode.CreateNew &&
                    selectedPlaceId.isNotBlank() &&
                    places.any { it.id == selectedPlaceId }
            if (openPlaceStillExists) return

            val active = Prefs.getActivePlace(context)
            if (active != null) {
                applyActivePlace(active)
            } else {
                selectedPlaceId = ""
                viewingPlaceId = ""
                editingPlaceId = ""
                pin = null
                selectedPlaceLabel = ""
                placeSelectionMode = PlaceSelectionMode.CreateNew
                radius = Prefs.getRadius(context)
                durationText = formatHoursInput(Prefs.getDurationMinutes(context) / 60.0)
            }
        }
    }

    // Tick every second: drives the countdown and picks up changes made by
    // the broadcast receivers (timer started by geofence, cancelled, etc.)
    LaunchedEffect(Unit) {
        BackendClient.trackEvent(context, "app_open")
        prunePersistentSearchCache(context)
        GeofenceManager.refreshOnAppOpen(context) { _, _ ->
            refreshPlaces()
        }
        while (true) {
            now = System.currentTimeMillis()
            timerEnd = Prefs.getTimerEnd(context)
            armed = Prefs.isArmed(context)
            places = Prefs.getPlaces(context)
            registeredPlaceIds = Prefs.getRegisteredPlaceIds(context)
            monitoringError = Prefs.getMonitoringError(context)
            delay(1000)
        }
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }
    val overlays = remember { ZoneOverlays() }

    fun setPendingMonitoringResume(resume: PendingMonitoringResume?) {
        if (pendingMonitoringResume != resume) {
            shownPendingMonitoringSetupStep = null
        }
        pendingMonitoringResume = resume
        if (resume == null) {
            Prefs.clearPendingMonitoringResume(context)
        } else {
            Prefs.savePendingMonitoringResume(context, resume.placeId)
        }
    }

    fun setPendingManualTimerStart(request: ManualTimerStartRequest?) {
        pendingManualTimerStart = request
        if (request == null) {
            Prefs.clearPendingManualTimerStart(context)
        } else {
            Prefs.savePendingManualTimerStart(
                context,
                placeId = request.placeId,
                editablePlaceId = request.editablePlaceId,
                durationMinutes = request.durationMinutes,
                requestedAtMillis = request.requestedAtMillis,
            )
        }
    }

    fun setPendingCurrentLocationResume(
        requested: Boolean,
        selectAsZone: Boolean = true,
        expandDock: Boolean = true,
        selectionMode: PlaceSelectionMode? = null,
        targetPlaceId: String? = null,
    ) {
        locateAfterPermission = requested
        locateAfterPermissionSelectAsZone = if (requested) selectAsZone else true
        locateAfterPermissionExpandDock = if (requested) expandDock else true
        locateAfterPermissionSelectionMode = if (requested) selectionMode else null
        locateAfterPermissionTargetPlaceId = if (requested) targetPlaceId?.takeIf { it.isNotBlank() } else null
        if (requested) {
            Prefs.savePendingCurrentLocationResume(
                context,
                selectAsZone = selectAsZone,
                expandDock = expandDock,
                selectionModeName = selectionMode?.name,
                targetPlaceId = targetPlaceId,
            )
        } else {
            Prefs.clearPendingCurrentLocationResume(context)
        }
    }

    fun showCurrentLocationPermissionDeniedHelp(
        selectAsZone: Boolean,
        expandDock: Boolean,
        selectionMode: PlaceSelectionMode?,
        targetPlaceId: String?,
    ) {
        showLongActionMessage(
            message = currentLocationPermissionDeniedMessage(selectAsZone),
            actionLabel = permissionRecoveryActionLabel(),
            onAction = {
                setPendingCurrentLocationResume(
                    requested = true,
                    selectAsZone = selectAsZone,
                    expandDock = expandDock,
                    selectionMode = selectionMode,
                    targetPlaceId = targetPlaceId,
                )
                launchAppSettings()
            },
        )
    }

    fun showBackgroundLocationDisclosureOnceForPendingResume() {
        shownPendingMonitoringSetupStep = MonitoringSetupRecoveryStep.BackgroundLocation
        showBackgroundLocationDisclosure = true
    }

    fun missingForegroundMonitoringPermissions(): Array<String> = buildList {
        if (!hasFineLocation(context)) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasNotifications(context) && Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasActivityRecognition(context) && Build.VERSION.SDK_INT >= 29) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }.toTypedArray()

    fun requestMonitoringPermissions(
        resume: PendingMonitoringResume? = null,
        showAlreadyActiveFeedback: Boolean = false,
    ) {
        if (resume != null) {
            setPendingMonitoringResume(resume)
        }
        if (
            monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = monitoringPermissionRequestInFlight || appSettingsReturnInFlight,
                backgroundDisclosureVisible = showBackgroundLocationDisclosure,
            )
        ) {
            if (showAlreadyActiveFeedback) {
                toast(
                    monitoringPermissionUiAlreadyActiveMessage(
                        backgroundDisclosureVisible = showBackgroundLocationDisclosure,
                    )
                )
            }
            return
        }
        val foregroundPermissions = missingForegroundMonitoringPermissions()
        when {
            foregroundPermissions.isNotEmpty() -> {
                monitoringPermissionRequestInFlight = true
                permissionLauncher.launch(foregroundPermissions)
            }
            !hasBackgroundLocation(context) -> {
                showBackgroundLocationDisclosureOnceForPendingResume()
            }
            resume == null -> {
                launchAppSettings()
            }
        }
    }

    fun openMonitoringSetup() {
        requestMonitoringPermissions(showAlreadyActiveFeedback = true)
    }

    fun refreshMonitoringFromHealthCard() {
        GeofenceManager.refresh(context) { ok, err ->
            refreshPlaces()
            registeredPlaceIds = Prefs.getRegisteredPlaceIds(context)
            monitoringError = Prefs.getMonitoringError(context)
            if (ok) {
                toast("Monitoring setup refreshed")
            } else {
                showMonitoringStartFailure(err, alreadyOnPlaces = true)
            }
        }
    }

    fun openSetupChecks(backRoute: AppRoute) {
        setupChecksBackRoute = backRoute
        route = AppRoute.SetupChecks
    }

    fun openBatterySettings() {
        val opened = BatteryReliability.openSettings(context)
        if (!opened) toast("Could not open battery settings on this device")
    }

    fun geoBoundsFor(point: MapPoint, radiusMeters: Float): GeoBounds =
        GeofenceMapBounds.forCircle(
            latitude = point.latitude,
            longitude = point.longitude,
            radiusMeters = radiusMeters,
        )

    fun boundaryBoxFor(bounds: GeoBounds): LatLngBounds =
        LatLngBounds.Builder()
            .include(LatLng(bounds.south, bounds.west))
            .include(LatLng(bounds.north, bounds.east))
            .build()

    fun circlePoints(center: MapPoint, radiusMeters: Float): List<LatLng> {
        val earthRadiusMeters = 6_371_008.8
        val lat = Math.toRadians(center.latitude)
        val lon = Math.toRadians(center.longitude)
        val angularDistance = DwellRadius.normalize(radiusMeters) / earthRadiusMeters
        return (0..72).map { step ->
            val bearing = 2.0 * Math.PI * step / 72.0
            val pointLat = kotlin.math.asin(
                kotlin.math.sin(lat) * kotlin.math.cos(angularDistance) +
                    kotlin.math.cos(lat) * kotlin.math.sin(angularDistance) * kotlin.math.cos(bearing)
            )
            val pointLon = lon + kotlin.math.atan2(
                kotlin.math.sin(bearing) * kotlin.math.sin(angularDistance) * kotlin.math.cos(lat),
                kotlin.math.cos(angularDistance) - kotlin.math.sin(lat) * kotlin.math.sin(pointLat)
            )
            LatLng(Math.toDegrees(pointLat), Math.toDegrees(pointLon))
        }
    }

    fun redrawZoneOverlay() {
        val map = overlays.map ?: return
        overlays.marker?.let { map.removeMarker(it) }
        overlays.circle?.let { map.removePolygon(it) }
        overlays.marker = null
        overlays.circle = null
        pin?.let { p ->
            overlays.circle = map.addPolygon(
                PolygonOptions()
                    .addAll(circlePoints(p, radius))
                    .fillColor(Color.argb(44, 0, 107, 94))
                    .strokeColor(Color.argb(220, 0, 107, 94))
            )
            overlays.marker = map.addMarker(
                MarkerOptions()
                    .position(p.toLatLng())
                    .title(if (pendingPlacePreview != null) "Unsaved place" else "Place radius")
            )
        }
    }

    fun updateUserLocationMarker(point: MapPoint) {
        val map = overlays.map
        if (map == null) {
            overlays.pendingUserPoint = point
            return
        }
        overlays.userMarker?.let { map.removeMarker(it) }
        overlays.userMarker = map.addMarker(
            MarkerOptions()
                .position(point.toLatLng())
                .title("Current location")
        )
        overlays.pendingUserPoint = null
    }

    fun fitMapToBoundary(point: MapPoint, radiusMeters: Float = radius) {
        val map = overlays.map
        if (map == null) {
            overlays.pendingZonePoint = point
            return
        }
        val bounds = geoBoundsFor(point, radiusMeters)
        val target = MapPoint(
            (bounds.north + bounds.south) / 2.0,
            (bounds.east + bounds.west) / 2.0,
        )
        val boundary = boundaryBoxFor(bounds)

        fun moveMap(animated: Boolean) {
            val update = if (mapView.width > 0 && mapView.height > 0) {
                CameraUpdateFactory.newLatLngBounds(boundary, 96)
            } else {
                CameraUpdateFactory.newLatLngZoom(target.toLatLng(), 16.0)
            }
            if (animated) {
                map.easeCamera(update, 350)
            } else {
                map.moveCamera(update)
            }
        }

        if (mapView.width > 0 && mapView.height > 0) {
            moveMap(animated = true)
        } else {
            mapView.post { moveMap(animated = false) }
        }
    }

    fun centerMapOn(point: MapPoint) = fitMapToBoundary(point)

    fun currentEditingPlace(): DwellPlace? {
        if (placeSelectionMode != PlaceSelectionMode.EditSelected) return null
        val editIds = listOf(editingPlaceId, selectedPlaceId)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return editIds.firstNotNullOfOrNull { Prefs.getPlace(context, it) }
    }

    fun currentSelectedPlace(): DwellPlace? {
        val selectedIds = listOf(selectedPlaceId, viewingPlaceId, editingPlaceId)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return selectedIds.firstNotNullOfOrNull { Prefs.getPlace(context, it) }
            ?: Prefs.getActivePlace(context)
    }

    fun currentSettingsPlace(): DwellPlace? {
        if (pendingPlacePreview != null || placeSelectionMode != PlaceSelectionMode.EditSelected) {
            return null
        }
        return currentEditingPlace() ?: currentSelectedPlace()
    }

    fun currentActionPlace(): DwellPlace? {
        if (pendingPlacePreview != null || placeSelectionMode == PlaceSelectionMode.CreateNew) {
            return null
        }
        return currentEditingPlace() ?: currentSelectedPlace()
    }

    fun hasSavedPlaceForSettingsTarget(settingsPlace: DwellPlace?): Boolean =
        settingsPlace != null ||
            (placeSelectionMode == PlaceSelectionMode.ViewSelected && currentSelectedPlace() != null)

    fun currentDurationMinutes(): Int {
        return durationMinutesFromText(durationText)
            ?: currentSettingsPlace()?.durationMinutes
            ?: Prefs.getDefaultDurationMinutes(context)
    }

    fun syncSelectedZone(isArmed: Boolean = armed) {
        val selected = pin ?: return
        val fallbackPlace = currentSettingsPlace()
            ?: currentActionPlace()
            ?: currentSelectedPlace()
        val fallbackLabel = pendingPlacePreview
            ?.placeName
            ?.takeIf { it.isNotBlank() }
            ?: fallbackPlace?.safeLabel
            ?: ""
        scope.launch {
            BackendClient.savePrimaryZone(
                context = context,
                label = selectedZoneSyncLabel(
                    typedPlaceLabel = selectedPlaceLabel,
                    fallbackPlaceLabel = fallbackLabel,
                ),
                lat = selected.latitude,
                lon = selected.longitude,
                radiusMeters = radius,
                durationMinutes = selectedZoneSyncDurationMinutes(
                    durationText = durationText,
                    fallbackPlaceDurationMinutes = fallbackPlace?.durationMinutes,
                    defaultDurationMinutes = Prefs.getDefaultDurationMinutes(context),
                ),
                armed = isArmed,
            )
        }
    }

    fun prewarmZoneMap(point: MapPoint, radiusMeters: Float = radius) {
        MapCacheManager.prewarmZone(
            context = context,
            styleUrl = mobileMapConfig.styleUrl,
            latitude = point.latitude,
            longitude = point.longitude,
            radiusMeters = radiusMeters,
        )
    }

    fun persistDurationText(text: String) {
        val place = currentSettingsPlace()
        val target = settingsPersistenceTarget(
            hasPendingPlacePreview = pendingPlacePreview != null,
            selectionMode = placeSelectionMode,
            hasEditingPlace = hasSavedPlaceForSettingsTarget(place),
        )
        if (!settingsLocalChangeAllowed(target)) return
        durationText = text
        val durationMin = durationMinutesFromText(text) ?: return
        when (target) {
            SettingsPersistenceTarget.PendingPreview -> return
            SettingsPersistenceTarget.EditingPlace -> {
                Prefs.upsertPlace(
                    context,
                    place!!.withTimerDefaults(place.radiusMeters, durationMin),
                    makeActive = false,
                )
            }
            SettingsPersistenceTarget.DefaultSettings -> {
                Prefs.setDefaultDurationMinutes(context, durationMin)
                defaultDurationText = text
            }
            SettingsPersistenceTarget.ReadOnlyPlace -> return
        }
        WearSync.pushState(context)
        syncSelectedZone(isArmed = armed)
    }

    fun commitRadiusChange(fitMap: Boolean = false) {
        val requestedRadius = DwellRadius.normalize(radius)
        if (pendingPlacePreview != null) {
            radius = requestedRadius
            pin?.takeIf { fitMap }?.let { fitMapToBoundary(it, requestedRadius) }
            return
        }

        val settingsPlace = currentSettingsPlace()
        val currentRadius = settingsPlace?.radiusMeters ?: Prefs.getRadius(context)
        val activeMonitoring = settingsPlace?.monitoringEnabled == true
        val target = settingsPersistenceTarget(
            hasPendingPlacePreview = false,
            selectionMode = placeSelectionMode,
            hasEditingPlace = hasSavedPlaceForSettingsTarget(settingsPlace),
        )

        if (!settingsLocalChangeAllowed(target)) {
            radius = currentRadius
            pin?.takeIf { fitMap }?.let { fitMapToBoundary(it, currentRadius) }
            return
        }

        if (activeMonitoring && requestedRadius > currentRadius + 0.5f) {
            radius = currentRadius
            toast(pauseMonitoringBeforeIncreasingRadiusMessage())
            pin?.let { fitMapToBoundary(it, currentRadius) }
            return
        }

        radius = requestedRadius
        when (target) {
            SettingsPersistenceTarget.PendingPreview -> return
            SettingsPersistenceTarget.EditingPlace -> {
                Prefs.upsertPlace(
                    context,
                    settingsPlace!!.withTimerDefaults(requestedRadius, settingsPlace.durationMinutes),
                    makeActive = false,
                )
            }
            SettingsPersistenceTarget.DefaultSettings -> {
                Prefs.setDefaultRadius(context, requestedRadius)
                defaultRadius = requestedRadius
            }
            SettingsPersistenceTarget.ReadOnlyPlace -> return
        }
        WearSync.pushState(context)
        pin?.let { prewarmZoneMap(it, requestedRadius) }

        if (activeMonitoring) {
            GeofenceManager.refresh(context) { ok, err ->
                refreshPlaces()
                if (ok) {
                    syncSelectedZone(isArmed = true)
                    pin?.takeIf { fitMap }?.let { fitMapToBoundary(it, requestedRadius) }
                    toast("Radius tightened to ${requestedRadius.roundToInt()} m")
                } else {
                    toast("Could not update monitoring radius: ${err ?: "unknown error"}")
                }
            }
        } else {
            refreshPlaces()
            syncSelectedZone(isArmed = false)
            pin?.takeIf { fitMap }?.let { fitMapToBoundary(it, requestedRadius) }
        }
    }

    fun invalidateCurrentLocationRequest() {
        currentLocationRequestGeneration += 1L
        locating = false
    }

    fun commitGeofencePoint(
        point: MapPoint,
        label: String,
        center: Boolean = true,
        expandDock: Boolean = false,
        analyticsSource: String,
        autoStart: Boolean = pendingPlacePreview?.autoStart ?: defaultAutoStart,
        durationMinutes: Int = currentDurationMinutes(),
    ): CommitGeofencePointResult {
        val creatingPlace = placeSelectionMode != PlaceSelectionMode.EditSelected
        val duplicatePlace = if (creatingPlace) {
            val candidate = DwellPlace.create(
                label = label,
                latitude = point.latitude,
                longitude = point.longitude,
                radiusMeters = radius,
                durationMinutes = durationMinutes,
                autoStart = autoStart,
            )
            Prefs.getPlaces(context).firstOrNull {
                DwellPlace.isDuplicateSavedPlace(it, candidate)
            }
        } else {
            null
        }
        val committedPlace = when (placeSelectionMode) {
            PlaceSelectionMode.CreateNew,
            PlaceSelectionMode.ViewSelected -> {
                Prefs.createPlace(
                    context,
                    label = label,
                    lat = point.latitude,
                    lon = point.longitude,
                    radiusMeters = radius,
                    durationMinutes = durationMinutes,
                    autoStart = autoStart,
                )
            }
            PlaceSelectionMode.EditSelected -> {
                val existingPlace = currentEditingPlace()
                if (existingPlace != null) {
                    Prefs.upsertPlace(
                        context,
                        Prefs.placeForUpdate(
                            active = existingPlace,
                            lat = point.latitude,
                            lon = point.longitude,
                            label = label,
                            radiusMeters = radius,
                            durationMinutes = durationMinutes,
                            autoStart = autoStart,
                        ),
                        makeActive = false,
                    )
                } else {
                    Prefs.createPlace(
                        context,
                        label = label,
                        lat = point.latitude,
                        lon = point.longitude,
                        radiusMeters = radius,
                        durationMinutes = durationMinutes,
                        autoStart = autoStart,
                    )
                }
            }
        }
        val focusState = savedPlaceFocusState(committedPlace.id)
        val committedPoint = MapPoint(committedPlace.latitude, committedPlace.longitude)
        pendingPlacePreview = null
        placeSelectionMode = focusState.selectionMode
        pin = committedPoint
        selectedPlaceId = focusState.selectedPlaceId
        viewingPlaceId = focusState.viewingPlaceId
        editingPlaceId = focusState.editingPlaceId
        selectedPlaceLabel = committedPlace.safeLabel
        radius = committedPlace.radiusMeters
        durationText = formatHoursInput(committedPlace.durationMinutes / 60.0)
        refreshPlaces()
        WearSync.pushState(context)
        prewarmZoneMap(committedPoint)
        if (center) centerMapOn(committedPoint)
        homeDockExpanded = expandDock
        syncSelectedZone(isArmed = committedPlace.monitoringEnabled)
        scope.launch {
            BackendClient.trackEvent(
                context,
                "location_selected",
                mapOf("source" to analyticsSource),
            )
        }
        return CommitGeofencePointResult(
            place = committedPlace,
            selectedExistingDuplicate = duplicatePlace != null && duplicatePlace.id == committedPlace.id,
        )
    }

    fun selectGeofencePoint(
        point: MapPoint,
        label: String,
        center: Boolean = true,
        expandDock: Boolean = false,
        analyticsSource: String,
        forceCreateNew: Boolean = false,
    ) {
        if (shouldInvalidateCurrentLocationForSelection(analyticsSource)) {
            invalidateCurrentLocationRequest()
        }
        val changed = pin?.let { distanceMeters(it, point) > 1f } ?: true
        val previousPreview = pendingPlacePreview
        val returnFocus = previousPreview?.returnFocus ?: pendingPreviewReturnFocus(
            selectionMode = placeSelectionMode,
            selectedPlaceId = selectedPlaceId,
            viewingPlaceId = viewingPlaceId,
            editingPlaceId = editingPlaceId,
        )
        val selectedExistingPlace = if (!forceCreateNew && placeSelectionMode == PlaceSelectionMode.EditSelected) {
            currentEditingPlace()
        } else {
            null
        }
        val monitoredActivePlace = selectedExistingPlace?.takeIf { it.monitoringEnabled }

        if (
            changed &&
            monitoredActivePlace != null &&
            placeSelectionMode == PlaceSelectionMode.EditSelected
        ) {
            toast(pauseMonitoringBeforeChangingLocationMessage(monitoredActivePlace.safeLabel))
            return
        }

        val previewMode = previewModeForMapPoint(
            selectionMode = placeSelectionMode,
            hasSelectedExistingPlace = selectedExistingPlace != null,
            forceCreateNew = forceCreateNew,
        )
        if (
            shouldBlockMapPointSelection(
                pendingPreviewMode = previousPreview?.mode,
                nextPreviewMode = previewMode,
            )
        ) {
            homeDockExpanded = true
            toast(pendingPreviewBlockedMessage())
            return
        }
        val carryPreviousDraft = shouldCarryPendingPreviewDraft(
            previousPreviewMode = previousPreview?.mode,
            previewMode = previewMode,
        )
        if (
            shouldResetPreviewSettingsToDefaults(
                previousPreviewMode = previousPreview?.mode,
                previewMode = previewMode,
            )
        ) {
            radius = Prefs.getDefaultRadius(context)
            durationText = formatHoursInput(Prefs.getDefaultDurationMinutes(context) / 60.0)
        }
        val previewPlaceName = mapPreviewPlaceNameForPointChange(
            existingPlaceLabel = selectedExistingPlace?.safeLabel,
            sourceLabel = label,
            previousPreviewPlaceName = previousPreview?.placeName.takeIf { carryPreviousDraft },
            previousPreviewSourceLabel = previousPreview?.sourceLabel.takeIf { carryPreviousDraft },
        )
        placeSelectionMode = previewMode
        pendingPlacePreview = PendingPlacePreview(
            point = point,
            sourceLabel = label,
            placeName = previewPlaceName,
            mode = previewMode,
            targetPlaceId = selectedExistingPlace?.id,
            targetPlaceLabel = selectedExistingPlace?.safeLabel.orEmpty(),
            returnFocus = returnFocus,
            analyticsSource = analyticsSource,
            autoStart = pendingPreviewAutoStartForPointChange(
                existingPlaceAutoStart = selectedExistingPlace?.autoStart,
                previousPreviewAutoStart = previousPreview?.autoStart,
                carryPreviousDraft = carryPreviousDraft,
                defaultAutoStart = defaultAutoStart,
            ),
        )
        pin = point
        selectedPlaceLabel = previewPlaceName
        selectedPlaceId = selectedExistingPlace?.id.orEmpty()
        viewingPlaceId = selectedExistingPlace?.id.orEmpty()
        editingPlaceId = if (previewMode == PlaceSelectionMode.EditSelected) {
            selectedExistingPlace?.id.orEmpty()
        } else {
            ""
        }
        if (center) centerMapOn(point)
        homeDockExpanded = true
        scope.launch {
            BackendClient.trackEvent(
                context,
                "location_previewed",
                mapOf("source" to analyticsSource),
            )
        }
    }

    fun commitPendingPlacePreview() {
        val preview = pendingPlacePreview ?: return
        val durationMin = durationMinutesFromText(durationText) ?: run {
            homeDockExpanded = true
            toast(durationActionErrorMessage(durationText))
            return
        }
        placeSelectionMode = preview.mode
        if (preview.mode == PlaceSelectionMode.EditSelected && !preview.targetPlaceId.isNullOrBlank()) {
            if (Prefs.getPlace(context, preview.targetPlaceId) == null) {
                pendingPlacePreview = null
                editingPlaceId = ""
                refreshPlaces(syncActivePlace = true)
                toast("That saved place is no longer available")
                return
            }
            Prefs.setActivePlace(context, preview.targetPlaceId)
        }
        val commitLabel = pendingPlaceCommitLabel(
            typedPlaceLabel = selectedPlaceLabel,
            previewPlaceName = preview.placeName,
            sourceLabel = preview.sourceLabel,
            targetPlaceLabel = preview.targetPlaceLabel,
            editingSelectedPlace = preview.mode == PlaceSelectionMode.EditSelected,
        )
        if (preview.mode == PlaceSelectionMode.EditSelected) {
            val duplicatePlace = duplicatePlaceForEditCommit(
                existingPlaces = Prefs.getPlaces(context),
                editedPlaceId = preview.targetPlaceId,
                candidate = DwellPlace.create(
                    label = commitLabel,
                    latitude = preview.point.latitude,
                    longitude = preview.point.longitude,
                    radiusMeters = radius,
                    durationMinutes = durationMin,
                    autoStart = preview.autoStart,
                ),
            )
            if (duplicatePlace != null) {
                homeDockExpanded = true
                toast(
                    duplicateEditBlockedMessage(
                        editedPlaceLabel = commitLabel,
                        duplicatePlaceLabel = duplicatePlace.safeLabel,
                    )
                )
                return
            }
        }
        val commitResult = commitGeofencePoint(
            point = preview.point,
            label = commitLabel,
            center = true,
            expandDock = false,
            analyticsSource = preview.analyticsSource,
            autoStart = preview.autoStart,
            durationMinutes = durationMin,
        )
        val feedback = pendingPlaceCommitFeedback(
            placeLabel = commitResult.place.safeLabel,
            editingSelectedPlace = preview.mode == PlaceSelectionMode.EditSelected,
            selectedExistingDuplicate = commitResult.selectedExistingDuplicate,
        )
        if (feedback.expandDock) homeDockExpanded = true
        toast(feedback.message)
    }

    fun cancelPendingPlacePreview() {
        val preview = pendingPlacePreview ?: return
        pendingPlacePreview = null
        if (preview.mode == PlaceSelectionMode.EditSelected && !preview.targetPlaceId.isNullOrBlank()) {
            val restoredPlace = Prefs.getPlace(context, preview.targetPlaceId)
            if (restoredPlace != null) {
                applyActivePlace(restoredPlace)
                editingPlaceId = restoredPlace.id
                centerMapOn(MapPoint(restoredPlace.latitude, restoredPlace.longitude))
                toast(
                    pendingPlaceCancelMessage(
                        editingSelectedPlace = true,
                        targetPlaceLabel = restoredPlace.safeLabel,
                    )
                )
            } else {
                editingPlaceId = ""
                refreshPlaces(syncActivePlace = true)
                toast(
                    pendingPlaceCancelMessage(
                        editingSelectedPlace = true,
                        targetPlaceLabel = preview.targetPlaceLabel,
                        targetPlaceAvailable = false,
                    )
                )
            }
        } else {
            val restoredPlace = preview.returnFocus
                ?.let { focus -> Prefs.getPlace(context, focus.placeId)?.let { focus to it } }
            if (restoredPlace != null) {
                val (focus, place) = restoredPlace
                when (focus.selectionMode) {
                    PlaceSelectionMode.EditSelected -> {
                        beginEditPlace(place, forceDiscardPreview = true)
                    }
                    PlaceSelectionMode.ViewSelected -> {
                        viewPlaceOnMap(place, forceDiscardPreview = true)
                    }
                    PlaceSelectionMode.CreateNew -> {
                        beginCreatePlace()
                    }
                }
                centerMapOn(MapPoint(place.latitude, place.longitude))
            } else {
                beginCreatePlace()
            }
            toast(
                pendingPlaceCancelMessage(
                    editingSelectedPlace = false,
                    targetPlaceLabel = preview.targetPlaceLabel,
                )
            )
        }
        homeDockExpanded = false
    }

    LaunchedEffect(Unit) {
        if (Prefs.hasPlace(context)) return@LaunchedEffect
        val restored = BackendClient.loadPrimaryZone(context) ?: return@LaunchedEffect
        val point = MapPoint(restored.lat, restored.lon)
        pin = point
        selectedPlaceLabel = restored.label
        radius = DwellRadius.normalize(restored.radiusMeters)
        durationText = formatHoursInput(restored.durationMinutes / 60.0)
        Prefs.savePlace(
            context,
            restored.lat,
            restored.lon,
            restored.label,
            radiusMeters = radius,
            durationMinutes = restored.durationMinutes,
        )
        refreshPlaces(syncActivePlace = true)
        placeSelectionMode = PlaceSelectionMode.ViewSelected
        editingPlaceId = ""
        WearSync.pushState(context)
        prewarmZoneMap(point, radius)
        fitMapToBoundary(point, radius)
        toast("Saved place restored")
    }

    fun fetchCurrentLocation(
        preferLiveFix: Boolean = false,
        onResult: (Location?) -> Unit,
    ) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null)
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val locationManager = context.getSystemService(LocationManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        val cancellation = CancellationTokenSource()
        var fallbackLocation: Location? = null
        var delivered = false
        var fusedUpdates: LocationCallback? = null
        var platformUpdates: android.location.LocationListener? = null
        lateinit var timeout: Runnable

        fun locationAgeMs(location: Location): Long =
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L

        fun locationAccuracy(location: Location): Float =
            if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE

        fun isUsableLocation(location: Location?): Boolean {
            if (location == null) return false
            if (!DwellLocationSanity.hasValidCoordinates(location)) return false
            if (DwellLocationSanity.isSuspiciousPhysicalEmulatorDefault(location)) {
                Log.w(
                    "DwellLocation",
                    "Ignoring Android emulator default coordinate on physical device",
                )
                return false
            }
            return LocationQuality.isUsable(
                latitude = location.latitude,
                longitude = location.longitude,
                ageMs = locationAgeMs(location),
                accuracyMeters = locationAccuracy(location),
                isMock = LocationCompat.isMock(location),
                allowMock = BuildConfig.DEBUG,
            )
        }

        fun isImmediateLocation(location: Location?): Boolean {
            if (location == null || !isUsableLocation(location)) return false
            return LocationQuality.isImmediate(
                latitude = location.latitude,
                longitude = location.longitude,
                ageMs = locationAgeMs(location),
                accuracyMeters = locationAccuracy(location),
                isMock = LocationCompat.isMock(location),
                allowMock = BuildConfig.DEBUG,
            )
        }

        fun betterLocation(a: Location?, b: Location?): Location? {
            val left = a?.takeIf(::isUsableLocation)
            val right = b?.takeIf(::isUsableLocation)
            if (left == null) return right
            if (right == null) return left
            val aAge = locationAgeMs(left)
            val bAge = locationAgeMs(right)
            val aAccuracy = locationAccuracy(left)
            val bAccuracy = locationAccuracy(right)
            return when {
                bAge + 30_000L < aAge -> right
                aAge + 30_000L < bAge -> left
                bAccuracy < aAccuracy -> right
                else -> left
            }
        }

        fun updateFallback(location: Location?) {
            fallbackLocation = betterLocation(fallbackLocation, location)
        }

        fun platformLastKnownLocation(): Location? {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            return providers.fold(null as Location?) { best, provider ->
                val location = runCatching {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }.getOrNull()
                betterLocation(best, location)
            }
        }

        fun stopLiveUpdates() {
            fusedUpdates?.let { client.removeLocationUpdates(it) }
            fusedUpdates = null
            platformUpdates?.let { runCatching { locationManager.removeUpdates(it) } }
            platformUpdates = null
        }

        fun deliver(location: Location?) {
            if (delivered) return
            delivered = true
            cancellation.cancel()
            handler.removeCallbacks(timeout)
            stopLiveUpdates()
            onResult(location?.takeIf(::isUsableLocation) ?: fallbackLocation)
        }

        timeout = Runnable { deliver(fallbackLocation) }
        handler.postDelayed(timeout, CURRENT_LOCATION_LIVE_FIX_TIMEOUT_MS.toLong())
        updateFallback(platformLastKnownLocation())
        if (!preferLiveFix && isImmediateLocation(fallbackLocation)) {
            deliver(fallbackLocation)
            return
        }

        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    updateFallback(location)
                    if (!preferLiveFix && isImmediateLocation(location)) {
                        deliver(location)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DwellLocation", "Last known location lookup failed", e)
                }
        } catch (_: SecurityException) {
            deliver(null)
            return
        }

        try {
            val liveRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1_000L,
            )
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdates(6)
                .setDurationMillis(CURRENT_LOCATION_LIVE_FIX_TIMEOUT_MS.toLong())
                .setWaitForAccurateLocation(true)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach(::updateFallback)
                    if (isImmediateLocation(fallbackLocation)) {
                        deliver(fallbackLocation)
                    }
                }
            }
            fusedUpdates = callback
            client.requestLocationUpdates(liveRequest, callback, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    Log.w("DwellLocation", "Live fused location updates failed", e)
                }
        } catch (e: SecurityException) {
            Log.w("DwellLocation", "Live fused location update denied", e)
        }

        try {
            val listener = android.location.LocationListener { location ->
                updateFallback(location)
                if (isImmediateLocation(fallbackLocation)) {
                    deliver(fallbackLocation)
                }
            }
            var requestedProvider = false
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
            ).forEach { provider ->
                val requested = runCatching {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(
                            provider,
                            1_000L,
                            0f,
                            listener,
                            Looper.getMainLooper(),
                        )
                        true
                    } else {
                        false
                    }
                }.getOrDefault(false)
                requestedProvider = requestedProvider || requested
            }
            if (requestedProvider) {
                platformUpdates = listener
            }
        } catch (e: SecurityException) {
            Log.w("DwellLocation", "Raw platform location update denied", e)
        }

        try {
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellation.token,
            )
                .addOnSuccessListener { location ->
                    if (isUsableLocation(location)) {
                        if (!preferLiveFix || isImmediateLocation(location)) {
                            deliver(location)
                        } else {
                            updateFallback(location)
                        }
                    } else {
                        updateFallback(location)
                        if (!preferLiveFix) deliver(fallbackLocation)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DwellLocation", "Current location lookup failed", e)
                    if (!preferLiveFix) deliver(fallbackLocation)
                }
        } catch (_: SecurityException) {
            deliver(fallbackLocation)
        }
    }

    fun applyCurrentLocationResumeSelection(resume: LocationPermissionResume): Boolean {
        if (!resume.selectAsZone) return true
        return when (resume.selectionMode) {
            PlaceSelectionMode.CreateNew,
            null -> {
                if (resume.selectionMode == PlaceSelectionMode.CreateNew) {
                    beginCreatePlace(forceDiscardPreview = true)
                }
                true
            }
            PlaceSelectionMode.ViewSelected -> {
                val place = Prefs.getSavedPlace(context, resume.targetPlaceId)
                if (place != null) {
                    viewPlaceOnMap(place, forceDiscardPreview = true)
                }
                true
            }
            PlaceSelectionMode.EditSelected -> {
                val place = Prefs.getSavedPlace(context, resume.targetPlaceId)
                if (place == null) {
                    toast(missingMonitoringResumePlaceMessage())
                    false
                } else {
                    beginEditPlace(
                        place,
                        expandDock = resume.expandDock,
                        forceDiscardPreview = true,
                    )
                    true
                }
            }
        }
    }

    fun requestCurrentLocation(
        selectAsZone: Boolean = true,
        showErrors: Boolean = true,
        expandDock: Boolean = false,
    ) {
        if (!hasFineLocation(context)) {
            val permissionUiActive = monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = monitoringPermissionRequestInFlight || appSettingsReturnInFlight,
                backgroundDisclosureVisible = showBackgroundLocationDisclosure,
            )
            if (
                !shouldLaunchCurrentLocationPermissionRequest(
                    hasFineLocation = false,
                    permissionUiAlreadyActive = permissionUiActive,
                )
            ) {
                if (showErrors) {
                    toast(
                        currentLocationPermissionAlreadyActiveMessage(
                            backgroundDisclosureVisible = showBackgroundLocationDisclosure,
                        )
                    )
                }
                return
            }
            setPendingCurrentLocationResume(
                requested = true,
                selectAsZone = selectAsZone,
                expandDock = expandDock,
                selectionMode = if (selectAsZone) placeSelectionMode else null,
                targetPlaceId = if (selectAsZone) {
                    when (placeSelectionMode) {
                        PlaceSelectionMode.EditSelected -> currentEditingPlace()?.id ?: editingPlaceId
                        PlaceSelectionMode.ViewSelected -> currentSelectedPlace()?.id ?: viewingPlaceId
                        PlaceSelectionMode.CreateNew -> null
                    }
                } else {
                    null
                },
            )
            monitoringPermissionRequestInFlight = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            if (showErrors) toast(currentLocationPermissionPrompt(selectAsZone))
            return
        }

        currentLocationRequestGeneration += 1L
        val requestGeneration = currentLocationRequestGeneration
        locating = true
        fetchCurrentLocation(preferLiveFix = true) { location ->
            if (
                !currentLocationResultStillCurrent(
                    requestGeneration = requestGeneration,
                    activeGeneration = currentLocationRequestGeneration,
                )
            ) {
                return@fetchCurrentLocation
            }
            locating = false
            if (location == null) {
                if (showErrors) toast(currentLocationUnavailableMessage(selectAsZone))
                return@fetchCurrentLocation
            }

            val point = MapPoint(location.latitude, location.longitude)
            updateUserLocationMarker(point)
            if (selectAsZone) {
                selectGeofencePoint(
                    point = point,
                    label = "Current location",
                    expandDock = expandDock,
                    analyticsSource = "current_location",
                )
            } else {
                centerMapOn(point)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!Prefs.hasPlace(context) && hasFineLocation(context)) {
            requestCurrentLocation(selectAsZone = false, showErrors = false)
        }
    }

    LaunchedEffect(Unit) {
        BackendClient.loadMobileConfig(context)?.let { config ->
            Prefs.saveMobileConfig(context, config)
            mobileSearchConfig = config.search
            mobileMapConfig = config.map
        }
    }

    fun cachedSearch(queryKey: String, nowMs: Long): CachedLocationSearch? {
        val cached = searchCache[queryKey]
        if (cached != null && nowMs - cached.fetchedAtMillis < LOCATION_SEARCH_CACHE_TTL_MS) {
            return cached
        }
        val persisted = readPersistentSearchCache(context, queryKey, nowMs) ?: return null
        searchCache[queryKey] = persisted
        return persisted
    }

    fun cachedSearchSuggestions(queryKey: String, nowMs: Long): List<LocationSearchResult> {
        if (queryKey.length < 3) return emptyList()
        val memorySuggestions = searchCache
            .asSequence()
            .filter { (key, cache) ->
                key.startsWith(queryKey) &&
                    nowMs - cache.fetchedAtMillis < LOCATION_SEARCH_CACHE_TTL_MS
            }
            .sortedByDescending { it.value.fetchedAtMillis }
            .flatMap { it.value.results.asSequence() }
        return (memorySuggestions + readPersistentSearchSuggestions(context, queryKey, nowMs).asSequence())
            .filter { it.label.lowercase(Locale.ROOT).contains(queryKey) }
            .distinctBy(::resultIdentity)
            .take(LOCATION_SEARCH_DROPDOWN_MAX_RESULTS)
            .toList()
    }

    fun updateSearchText(text: String) {
        searchText = text
        val queryKey = searchCacheKey(text)
        if (queryKey.length < 3) {
            searchResults = emptyList()
            searchSuggestions = emptyList()
            submittedSearchKey = ""
            return
        }

        val nowMs = System.currentTimeMillis()
        val cached = cachedSearch(queryKey, nowMs)
        if (cached != null) {
            searchResults = cached.results
            searchSuggestions = emptyList()
            submittedSearchKey = queryKey
        } else {
            searchResults = emptyList()
            searchSuggestions = cachedSearchSuggestions(queryKey, nowMs)
            submittedSearchKey = ""
        }
    }

    fun performSearch(showValidationToast: Boolean = true) {
        val query = cleanSearchQuery(searchText)
        val queryKey = searchCacheKey(query)
        if (queryKey.length < 3) {
            searchResults = emptyList()
            searchSuggestions = emptyList()
            submittedSearchKey = ""
            if (showValidationToast) toast("Type at least 3 characters to search")
            return
        }
        if (searching) {
            if (showValidationToast) toast("Search is already running")
            return
        }

        val nowMs = System.currentTimeMillis()
        val cached = cachedSearch(queryKey, nowMs)
        if (cached != null) {
            searchResults = cached.results
            searchSuggestions = emptyList()
            submittedSearchKey = queryKey
            return
        }

        if (nowMs - lastSearchAt < LOCATION_SEARCH_NETWORK_COOLDOWN_MS) {
            if (showValidationToast) toast("Give search a moment before trying again")
            return
        }

        lastSearchAt = nowMs
        searching = true
        searchingQueryKey = queryKey
        searchSuggestions = emptyList()
        scope.launch {
            val result = runCatching {
                searchOpenStreetMap(
                    query = query,
                    baseUrl = mobileSearchConfig.baseUrl,
                    userAgent = mobileSearchConfig.userAgent,
                )
            }
            if (searchingQueryKey == queryKey) {
                searching = false
                searchingQueryKey = ""
            }

            val places = result.getOrElse {
                if (
                    searchCompletionShouldUpdateUi(
                        searchPanelExpanded = searchPanelExpanded,
                        currentSearchText = searchText,
                        completedQueryKey = queryKey,
                    )
                ) {
                    submittedSearchKey = ""
                    if (showValidationToast) toast("Search failed. Try again in a moment.")
                }
                return@launch
            }
            val cacheEntry = CachedLocationSearch(
                fetchedAtMillis = System.currentTimeMillis(),
                results = places,
            )
            searchCache[queryKey] = cacheEntry
            writePersistentSearchCache(context, queryKey, cacheEntry)
            if (
                !searchCompletionShouldUpdateUi(
                    searchPanelExpanded = searchPanelExpanded,
                    currentSearchText = searchText,
                    completedQueryKey = queryKey,
                )
            ) {
                return@launch
            }

            submittedSearchKey = queryKey
            if (places.isEmpty()) {
                searchResults = emptyList()
                searchSuggestions = emptyList()
                if (showValidationToast) {
                    toast(
                        noSearchResultsToast(
                            editingSelectedPlace = placeSelectionMode == PlaceSelectionMode.EditSelected &&
                                currentEditingPlace() != null,
                            currentLocationSelectsPlace = mapCurrentLocationSelectsPlace(
                                selectionMode = placeSelectionMode,
                                hasEditingPlace = currentEditingPlace() != null,
                            ),
                        )
                    )
                }
                BackendClient.trackEvent(
                    context,
                    "location_search",
                    mapOf("resultCount" to 0),
                )
                return@launch
            }

            searchResults = places
            searchSuggestions = emptyList()
            BackendClient.trackEvent(
                context,
                "location_search",
                mapOf("resultCount" to places.size),
            )
        }
    }

    LaunchedEffect(searchText, searchPanelExpanded, searching) {
        val queryKey = searchCacheKey(searchText)
        if (!searchPanelExpanded || queryKey.length < 3 || submittedSearchKey == queryKey) {
            return@LaunchedEffect
        }

        delay(LOCATION_SEARCH_DEBOUNCE_MS)
        if (
            shouldRunSearchAutocomplete(
                searchPanelExpanded = searchPanelExpanded,
                networkAutocomplete = mobileSearchConfig.networkAutocomplete,
                currentSearchText = searchText,
                pendingQueryKey = queryKey,
                submittedSearchKey = submittedSearchKey,
                searching = searching,
            )
        ) {
            performSearch(showValidationToast = false)
        }
    }

    fun maybeStartTimerIfAlreadyInside(
        zone: MapPoint,
        radiusMeters: Float,
        durationMin: Int,
        placeId: String?,
        onChecked: (AlreadyInsideCheckResult) -> Unit,
    ) {
        fun placeStillMonitored(): Boolean =
            shouldRunAlreadyInsideCheck(
                placeId = placeId,
                place = placeId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Prefs.getPlace(context, it) },
            )

        if (!placeStillMonitored()) {
            onChecked(
                AlreadyInsideCheckResult(
                    decision = ArrivalDecision.WAIT,
                    waitReason = AlreadyInsideWaitReason.PLACE_NOT_MONITORED,
                )
            )
            return
        }
        if (!hasFineLocation(context)) {
            onChecked(
                AlreadyInsideCheckResult(
                    decision = ArrivalDecision.WAIT,
                    waitReason = AlreadyInsideWaitReason.LOCATION_PERMISSION_MISSING,
                )
            )
            return
        }

        fetchCurrentLocation(preferLiveFix = true) { location ->
            if (!placeStillMonitored()) {
                onChecked(
                    AlreadyInsideCheckResult(
                        decision = ArrivalDecision.WAIT,
                        waitReason = AlreadyInsideWaitReason.PLACE_NOT_MONITORED,
                    )
                )
                return@fetchCurrentLocation
            }
            if (location == null) {
                onChecked(
                    AlreadyInsideCheckResult(
                        decision = ArrivalDecision.WAIT,
                        waitReason = AlreadyInsideWaitReason.LOCATION_UNAVAILABLE,
                    )
                )
                return@fetchCurrentLocation
            }

            val current = MapPoint(location.latitude, location.longitude)
            val distanceToZone = distanceMeters(current, zone)
            val accuracyMeters = location.takeIf { it.hasAccuracy() }?.accuracy
            if (
                !shouldEvaluateAlreadyInsideConfidence(
                    distanceMeters = distanceToZone,
                    radiusMeters = radiusMeters,
                    accuracyMeters = accuracyMeters,
                )
            ) {
                onChecked(
                    AlreadyInsideCheckResult(
                        decision = ArrivalDecision.WAIT,
                        waitReason = AlreadyInsideWaitReason.OUTSIDE_PLACE,
                    )
                )
                return@fetchCurrentLocation
            }
            if (TimerController.isRunning(context)) {
                onChecked(
                    AlreadyInsideCheckResult(
                        decision = ArrivalDecision.WAIT,
                        waitReason = AlreadyInsideWaitReason.TIMER_ALREADY_RUNNING,
                    )
                )
                return@fetchCurrentLocation
            }

            val confidence = DwellArrivalEngine.arrivalConfidence(
                context = context,
                placeId = placeId,
                location = location,
                source = "armed-inside",
                geofenceEnter = false,
                alreadyInsideCheck = true,
            )
            val adjustedConfidence = DwellArrivalEngine.applyPlacePolicy(
                place = Prefs.getPlace(context, placeId),
                confidence = confidence,
            )
            when (adjustedConfidence.decision) {
                ArrivalDecision.START_TIMER -> {
                    TimerController.startTimer(context, durationMin, placeId)
                    timerEnd = Prefs.getTimerEnd(context)
                }
                ArrivalDecision.ASK_TO_START -> {
                    val promptPlaceId = placeId?.takeIf { it.isNotBlank() }
                    if (promptPlaceId != null) {
                        DwellArrivalEngine.requestStartConfirmation(
                            context = context,
                            placeId = promptPlaceId,
                            score = adjustedConfidence.score,
                        )
                    } else {
                        Prefs.setWatchPrompt(
                            context,
                            Prefs.WATCH_PROMPT_START_TIMER,
                            confidenceScore = adjustedConfidence.score,
                        )
                        Notifications.notifyArrivalQuestion(context, adjustedConfidence.score)
                        WearSync.pushState(context)
                    }
                }
                ArrivalDecision.WAIT -> Unit
            }
            onChecked(
                AlreadyInsideCheckResult(
                    decision = adjustedConfidence.decision,
                    waitReason = if (adjustedConfidence.decision == ArrivalDecision.WAIT) {
                        AlreadyInsideWaitReason.LOW_CONFIDENCE
                    } else {
                        null
                    },
                )
            )
        }
    }

    fun applyMapStyle(map: MapLibreMap, styleUrl: String, moveToInitialPoint: Boolean) {
        map.setStyle(styleUrl) {
            if (moveToInitialPoint) {
                val initialPoint = pin ?: MapPoint(20.0, 0.0)
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        initialPoint.toLatLng(),
                        if (pin != null) 16.0 else 3.0,
                    )
                )
            }
            redrawZoneOverlay()
            overlays.pendingZonePoint?.let {
                overlays.pendingZonePoint = null
                fitMapToBoundary(it)
            }
            overlays.pendingUserPoint?.let {
                updateUserLocationMarker(it)
            }
        }
    }

    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()
        mapView.getMapAsync { map ->
            overlays.map = map
            map.uiSettings.setLogoEnabled(false)
            map.uiSettings.setAttributionEnabled(false)
            map.uiSettings.setCompassEnabled(false)
            map.uiSettings.setRotateGesturesEnabled(false)
            map.addOnMapLongClickListener { latLng ->
                val longPressBehavior = longPressMapSelectionBehavior()
                selectGeofencePoint(
                    point = MapPoint(latLng.latitude, latLng.longitude),
                    label = longPressBehavior.label,
                    expandDock = longPressBehavior.expandDock,
                    analyticsSource = longPressBehavior.analyticsSource,
                    forceCreateNew = longPressBehavior.forceCreateNew,
                )
                true
            }
            mapReadyVersion += 1
            applyMapStyle(map, mobileMapConfig.styleUrl, moveToInitialPoint = true)
        }
        onDispose {
            overlays.map = null
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mobileMapConfig.styleUrl, mapReadyVersion) {
        overlays.map?.let { map ->
            applyMapStyle(map, mobileMapConfig.styleUrl, moveToInitialPoint = false)
        }
    }

    LaunchedEffect(permVersion) {
        if (locateAfterPermission) {
            val selectAfterPermission = locateAfterPermissionSelectAsZone
            val expandAfterPermission = locateAfterPermissionExpandDock
            val selectionModeAfterPermission = locateAfterPermissionSelectionMode
            val targetPlaceIdAfterPermission = locateAfterPermissionTargetPlaceId
            val resume = locationPermissionResume(
                requested = true,
                selectAsZone = selectAfterPermission,
                expandDock = expandAfterPermission,
                selectionMode = selectionModeAfterPermission,
                targetPlaceId = targetPlaceIdAfterPermission,
                hasFineLocation = hasFineLocation(context),
            )
            if (resume != null) {
                if (!applyCurrentLocationResumeSelection(resume)) {
                    setPendingCurrentLocationResume(requested = false)
                    return@LaunchedEffect
                }
                setPendingCurrentLocationResume(requested = false)
                requestCurrentLocation(
                    selectAsZone = resume.selectAsZone,
                    expandDock = resume.expandDock,
                )
            } else if (permVersion > 0) {
                setPendingCurrentLocationResume(requested = false)
                showCurrentLocationPermissionDeniedHelp(
                    selectAsZone = selectAfterPermission,
                    expandDock = expandAfterPermission,
                    selectionMode = selectionModeAfterPermission,
                    targetPlaceId = targetPlaceIdAfterPermission,
                )
            }
        }
        if (
            permVersion > 0 &&
            Prefs.getArmedPlaces(context).isNotEmpty() &&
            MonitoringPrerequisites.issueForContext(context) == null
        ) {
            GeofenceManager.refreshOnAppOpen(context) { _, _ ->
                refreshPlaces()
            }
        }
    }

    // Redraw the pin + radius circle whenever they change.
    LaunchedEffect(pin, radius) {
        redrawZoneOverlay()
    }

    fun parseDurationMinutes(): Int? {
        val actionPlace = currentActionPlace()
        val durationInputVisible = !isViewingSavedPlaceReadOnly(
            selectionMode = placeSelectionMode,
            hasPendingPlacePreview = pendingPlacePreview != null,
            hasSavedPlaceSelected = actionPlace != null,
        )
        return actionDurationMinutes(
            durationText = durationText,
            durationInputVisible = durationInputVisible,
            actionPlaceDurationMinutes = actionPlace?.durationMinutes,
            defaultDurationMinutes = Prefs.getDefaultDurationMinutes(context),
        )
    }

    @SuppressLint("InlinedApi")
    fun armGeofence() {
        if (pendingPlacePreview != null) {
            homeDockExpanded = true
            toast(pendingPreviewBlockedMessage())
            return
        }
        val p = pin ?: run {
            toast(choosePlaceForMonitoringMessage())
            return
        }
        val actionPlace = currentActionPlace()
        val durationInputVisible = !isViewingSavedPlaceReadOnly(
            selectionMode = placeSelectionMode,
            hasPendingPlacePreview = pendingPlacePreview != null,
            hasSavedPlaceSelected = actionPlace != null,
        )
        val durationMin = monitoringActionDurationMinutes(
            durationText = durationText,
            durationInputVisible = durationInputVisible,
            actionPlaceDurationMinutes = actionPlace?.durationMinutes,
            actionPlaceAlreadyMonitoring = actionPlace?.monitoringEnabled == true,
            defaultDurationMinutes = Prefs.getDefaultDurationMinutes(context),
        ) ?: run {
            toast(durationActionErrorMessage(durationText))
            return
        }
        when {
            !hasFineLocation(context) ||
                !hasNotifications(context) ||
                !hasActivityRecognition(context) -> {
                requestMonitoringPermissions(
                    PendingMonitoringResume(currentActionPlace()?.id),
                    showAlreadyActiveFeedback = true,
                )
            }
            !hasBackgroundLocation(context) -> {
                setPendingMonitoringResume(PendingMonitoringResume(currentActionPlace()?.id))
                showBackgroundLocationDisclosureOnceForPendingResume()
            }
            else -> {
                GeofenceManager.armMonitoringUpdateError(
                    places = Prefs.getPlaces(context),
                    activePlaceId = actionPlace?.id,
                )?.let { error ->
                    showMonitoringStartFailure(
                        error = error,
                        placeLabel = actionPlace?.safeLabel ?: selectedPlaceLabel,
                    )
                    return
                }
                val monitoredPlace = if (actionPlace != null) {
                    Prefs.upsertPlace(
                        context,
                        Prefs.placeForUpdate(
                            active = actionPlace,
                            lat = p.latitude,
                            lon = p.longitude,
                            label = selectedPlaceLabel.ifBlank { actionPlace.safeLabel },
                            radiusMeters = radius,
                            durationMinutes = durationMin,
                            autoStart = actionPlace.autoStart,
                        ),
                        makeActive = false,
                    )
                } else {
                    Prefs.createPlace(
                        context,
                        label = selectedPlaceLabel.ifBlank { "Dropped pin" },
                        lat = p.latitude,
                        lon = p.longitude,
                        radiusMeters = radius,
                        durationMinutes = durationMin,
                    )
                }
                placeSelectionMode = PlaceSelectionMode.ViewSelected
                applyActivePlace(monitoredPlace)
                WearSync.pushState(context)
                prewarmZoneMap(p, radius)
                GeofenceManager.arm(
                    context,
                    p.latitude,
                    p.longitude,
                    radius,
                    placeId = monitoredPlace.id,
                ) { ok, err ->
                    refreshPlaces()
                    armed = Prefs.isArmed(context)
                    if (!ok) {
                        showMonitoringStartFailure(err, placeLabel = monitoredPlace.safeLabel)
                        return@arm
                    }

                    val armedPlace = Prefs.getPlace(context, monitoredPlace.id) ?: monitoredPlace
                    val batteryStatus = BatteryReliability.status(context)
                    if (batteryNeedsReliabilityReview(batteryStatus)) {
                        showBatteryHelp(batteryStatus)
                    }

                    syncSelectedZone(isArmed = true)
                    scope.launch {
                        BackendClient.trackEvent(
                            context,
                            "geofence_armed",
                            mapOf(
                                "radiusMeters" to radius.roundToInt(),
                                "durationMinutes" to durationMin,
                            ),
                        )
                    }
                    maybeStartTimerIfAlreadyInside(
                        p,
                        radius,
                        durationMin,
                        armedPlace.id,
                    ) { result ->
                        val timerRunningHere =
                            TimerController.isRunning(context) &&
                                Prefs.getTimerPlaceId(context) == armedPlace.id
                        if (result.decision == ArrivalDecision.START_TIMER) {
                            scope.launch {
                                BackendClient.trackEvent(
                                    context,
                                    "timer_auto_started",
                                    mapOf("durationMinutes" to durationMin),
                                )
                            }
                        }
                        toast(
                            alreadyInsideResultMessage(
                                placeLabel = armedPlace.safeLabel,
                                decision = result.decision,
                                timerRunningForPlace = timerRunningHere,
                                waitReason = result.waitReason,
                            )
                        )
                    }
                }
            }
        }
    }

    val timerActive = timerEnd > now
    val timerPlaceId = Prefs.getTimerPlaceId(context)
    val timerStartedAt = Prefs.getTimerStartedAt(context)
    val timerPlace = timerPlaceId
        .takeIf { it.isNotBlank() }
        ?.let { id -> places.firstOrNull { it.id == id } ?: Prefs.getPlace(context, id) }
    val timerPlaceLabel = timerPlace?.safeLabel.orEmpty()
    val selectedPlace = places.firstOrNull { it.id == selectedPlaceId }
    val editingPlace = currentEditingPlace()
    val activePlace = when (placeSelectionMode) {
        PlaceSelectionMode.CreateNew -> selectedPlace
        PlaceSelectionMode.ViewSelected -> selectedPlace
        PlaceSelectionMode.EditSelected -> editingPlace ?: selectedPlace
    }
    val pendingPreview = pendingPlacePreview
    val pendingPreviewEditing = pendingPreview?.mode == PlaceSelectionMode.EditSelected
    val editingCurrentPlace = placeSelectionMode == PlaceSelectionMode.EditSelected &&
        pendingPreview == null &&
        activePlace != null
    val monitoredRadiusLimit = currentSettingsPlace()
        ?.takeIf { it.monitoringEnabled }
        ?.radiusMeters
    val activePlaceArmed = activePlace?.monitoringEnabled == true
    val activePlaceRegistered = activePlace?.let { registeredPlaceIds.contains(it.id) } == true
    val activePlaceNeedsSetup = activePlaceArmed && !activePlaceRegistered
    val activePlaceAutoStart = pendingPreview?.autoStart ?: activePlace?.autoStart ?: defaultAutoStart
	    val viewingSavedPlaceReadOnly = isViewingSavedPlaceReadOnly(
	        selectionMode = placeSelectionMode,
	        hasPendingPlacePreview = pendingPreview != null,
	        hasSavedPlaceSelected = activePlace != null,
	    )
	    val setupIssue = MonitoringPrerequisites.issueForContext(context)
    val activePlaceSetupIssue = dockSetupIssue(
        setupIssue = setupIssue?.error,
        activePlaceArmed = activePlaceArmed,
        hasPendingPlacePreview = pendingPreview != null,
    )
    val batteryStatus = BatteryReliability.status(context)
    val exactAlarmAllowed = canExactAlarm(context)
    val batteryWarning = if (
        activePlaceArmed &&
        activePlaceRegistered &&
        batteryNeedsReliabilityReview(batteryStatus)
    ) {
        batteryStatus.detail
    } else {
        ""
    }
    val setupNotice = when {
        activePlaceNeedsSetup -> monitoringError.ifBlank {
            "Background arrival detection needs attention."
        }
        activePlaceSetupIssue != null -> activePlaceSetupIssue
        else -> ""
    }
    val armedPlaceCount = places.count { it.monitoringEnabled }
    val livePlaceCount = places.count { it.monitoringEnabled && registeredPlaceIds.contains(it.id) }
    val monitoringHealth = monitoringHealthState(
        placesCount = places.size,
        monitoredCount = armedPlaceCount,
        liveCount = livePlaceCount,
        setupIssue = setupIssue?.error,
        monitoringError = monitoringError,
        exactAlarmAllowed = exactAlarmAllowed,
        batteryReliabilityStatus = batteryStatus,
    )
    val homeMonitorTarget = homeMonitorActionTarget(
        monitoringNeedsSetup = setupNotice.isNotBlank() || activePlaceNeedsSetup,
        activePlaceArmed = activePlaceArmed,
    )
    val durationMinutes = parseDurationMinutes()
        ?: activePlace?.durationMinutes
        ?: Prefs.getDefaultDurationMinutes(context)
    val watchPrompt = Prefs.getWatchPrompt(context)
    val watchPromptUpdated = Prefs.getWatchPromptUpdated(context)
    val promptPlaceId = Prefs.getPromptPlaceId(context)
    val promptPlace = promptPlaceId
        .takeIf { it.isNotBlank() }
        ?.let { id -> places.firstOrNull { it.id == id } ?: Prefs.getPlace(context, id) }
    val promptPlaceLabel = promptPlace?.safeLabel.orEmpty()
    val promptState = homePromptState(
        prompt = watchPrompt,
        promptPlaceLabel = promptPlaceLabel,
        timerRunning = timerActive,
        timerPlaceLabel = timerPlaceLabel,
        durationMinutes = promptPlace?.durationMinutes ?: durationMinutes,
        timerEnd = timerEnd,
        now = now,
    )
    val promptActionScope = promptState?.let { state ->
        HomePromptActionScope(
            kind = state.kind,
            prompt = watchPrompt,
            promptPlaceId = promptPlaceId,
            promptUpdated = watchPromptUpdated,
            timerPlaceId = timerPlaceId,
            timerStartedAt = timerStartedAt,
            timerEnd = timerEnd,
        )
    }
    val placeNameFallback = pendingPreview
        ?.let {
            pendingPlaceNameFallbackLabel(
                previewPlaceName = it.placeName,
                sourceLabel = it.sourceLabel,
                targetPlaceLabel = it.targetPlaceLabel,
                editingSelectedPlace = pendingPreviewEditing,
            )
        }
        ?: activePlace?.safeLabel
        ?: selectedPlaceLabel
    val placeLabel = selectedPlaceDisplayLabel(
        hasPin = pin != null,
        typedPlaceLabel = selectedPlaceLabel,
        fallbackLabel = placeNameFallback,
    )
    val mapModePlaceLabel = when {
        pendingPreviewEditing -> pendingPreview?.targetPlaceLabel.orEmpty()
        placeSelectionMode == PlaceSelectionMode.CreateNew ->
            Prefs.getActivePlace(context)?.safeLabel ?: places.firstOrNull()?.safeLabel.orEmpty()
        else -> activePlace?.safeLabel ?: selectedPlaceLabel
    }
    val dockPlaceLabel = if (promptState != null) {
        promptState.placeLabel
    } else if (timerActive) {
        timerPlaceLabel.ifBlank { "Dwell timer" }
    } else {
        placeLabel
    }
    val dockRadius = when {
        promptState != null -> promptPlace?.radiusMeters ?: timerPlace?.radiusMeters ?: radius
        timerActive -> timerPlace?.radiusMeters ?: radius
        else -> radius
    }
    val dockDurationMinutes = when {
        promptState != null -> promptPlace?.durationMinutes ?: timerPlace?.durationMinutes ?: durationMinutes
        timerActive -> timerPlace?.durationMinutes ?: durationMinutes
        else -> durationMinutes
    }
    val dockAutoStart = when {
        promptState != null -> promptPlace?.autoStart ?: timerPlace?.autoStart ?: activePlaceAutoStart
        timerActive -> timerPlace?.autoStart ?: activePlaceAutoStart
        else -> activePlaceAutoStart
    }
    val statusTitle = when {
        promptState != null -> promptState.title
        pendingPreview != null -> pendingPlaceStatusTitle(
            editingSelectedPlace = pendingPreviewEditing,
            targetLabel = pendingPreview.targetPlaceLabel,
        )
        timerActive -> timerRunningStatusTitle(timerPlaceLabel)
        editingCurrentPlace -> editingPlaceStatusTitle(activePlace?.safeLabel.orEmpty())
        activePlaceNeedsSetup || activePlaceSetupIssue != null -> homeSetupStatusTitle(
            activePlaceNeedsSetup = activePlaceNeedsSetup,
            hasActivePlaceSetupIssue = activePlaceSetupIssue != null,
        )
        activePlaceArmed -> "Monitoring live"
        else -> idleHomeStatusTitle(
            hasSelectedPlace = activePlace != null,
            hasPin = pin != null,
            armedPlaceCount = armedPlaceCount,
        )
    }
    val statusDetail = when {
        promptState != null -> promptState.detail
        pendingPreview != null -> pendingPlaceStatusDetail(pendingPreviewEditing)
        timerActive -> {
            val left = timerEnd - now
            val h = left / 3_600_000
            val m = (left / 60_000) % 60
            val s = (left / 1000) % 60
            val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
            "${h}h ${m}m ${s}s left - ends $endsAt"
        }
        activePlaceNeedsSetup -> monitoringError.ifBlank { homeSetupRecoveryDetail() }
        activePlaceSetupIssue != null -> activePlaceSetupIssue
        editingCurrentPlace -> editingPlaceStatusDetail(activePlaceArmed)
        batteryWarning.isNotBlank() -> batteryWarning
        activePlaceArmed -> "Timer starts when you arrive"
        else -> idleHomeStatusDetail(
            hasSelectedPlace = activePlace != null,
            hasPin = pin != null,
            armedPlaceCount = armedPlaceCount,
        )
    }

    fun refreshNotificationsForRenamedPlace(renamed: DwellPlace) {
        val currentPrompt = Prefs.getWatchPrompt(context)
        val currentPromptPlaceId = Prefs.getPromptPlaceId(context)
        val currentTimerPlaceId = Prefs.getTimerPlaceId(context)
        val currentTimerRunning = TimerController.isRunning(context)
        when (
            placeRenameNotificationRefresh(
                renamedPlaceId = renamed.id,
                prompt = currentPrompt,
                promptPlaceId = currentPromptPlaceId,
                timerPlaceId = currentTimerPlaceId,
                timerRunning = currentTimerRunning,
            )
        ) {
            PlaceRenameNotificationRefresh.RunningTimer ->
                Notifications.notifyTimerRunning(context, Prefs.getTimerEnd(context))
            PlaceRenameNotificationRefresh.ArrivalPrompt ->
                Notifications.notifyArrivalQuestion(context, Prefs.getPromptConfidenceScore(context))
            PlaceRenameNotificationRefresh.SwitchPrompt -> {
                val promptPlaceLabel = currentPromptPlaceId
                    .takeIf { it.isNotBlank() }
                    ?.let { Prefs.getPlace(context, it)?.safeLabel }
                    ?: "new place"
                val timerPlaceLabel = currentTimerPlaceId
                    .takeIf { it.isNotBlank() }
                    ?.let { Prefs.getPlace(context, it)?.safeLabel }
                    ?: "current timer"
                Notifications.notifySwitchPlaceQuestion(
                    context,
                    newPlaceLabel = promptPlaceLabel,
                    currentPlaceLabel = timerPlaceLabel,
                )
            }
            PlaceRenameNotificationRefresh.LeavePrompt ->
                Notifications.notifyExitQuestion(context, Prefs.getTimerEnd(context))
            PlaceRenameNotificationRefresh.TimeUpPrompt ->
                Notifications.notifyTimerDone(
                    context,
                    TimerController.completionDurationMinutes(context),
                )
            PlaceRenameNotificationRefresh.None -> Unit
        }
    }

    fun updateSelectedPlaceName(name: String) {
        val cappedName = placeNameInputValue(name)
        pendingPlacePreview?.let {
            selectedPlaceLabel = cappedName
            pendingPlacePreview = it.copy(placeName = cappedName)
            return
        }
        val place = currentSettingsPlace()
        val target = settingsPersistenceTarget(
            hasPendingPlacePreview = false,
            selectionMode = placeSelectionMode,
            hasEditingPlace = hasSavedPlaceForSettingsTarget(place),
        )
        if (!settingsLocalChangeAllowed(target)) return
        selectedPlaceLabel = cappedName
        if (target == SettingsPersistenceTarget.DefaultSettings) return
        if (place == null) return
        val cleanName = cappedName.trim()
        if (cleanName.isBlank() || cleanName == place.safeLabel) return
        val renamed = Prefs.upsertPlace(
            context,
            place.copy(
                label = cleanName,
                updatedAtMillis = System.currentTimeMillis(),
            ).normalized(),
            makeActive = false,
        )
        places = Prefs.getPlaces(context)
        selectedPlaceId = renamed.id
        viewingPlaceId = renamed.id
        editingPlaceId = if (placeSelectionMode == PlaceSelectionMode.EditSelected) renamed.id else ""
        refreshNotificationsForRenamedPlace(renamed)
        WearSync.pushState(context)
        syncSelectedZone(isArmed = renamed.monitoringEnabled)
    }

    fun cancelRunningTimer() {
        TimerController.cancelTimer(context)
        Notifications.notifyTimerCancelled(context)
        timerEnd = 0L
        insightsRefresh += 1
        scope.launch {
            BackendClient.trackEvent(context, "timer_cancelled")
        }
    }

    fun requestTimerCancelConfirmation() {
        pendingTimerCancelScope = TimerCancelActionScope(
            timerPlaceId = timerPlaceId,
            timerStartedAt = timerStartedAt,
            timerEnd = timerEnd,
            timerPlaceLabel = timerPlaceLabel,
        )
    }

    fun startManualTimer(request: ManualTimerStartRequest, showStartedToast: Boolean = false) {
        val placeId = request.placeId?.takeIf { it.isNotBlank() }
        val timerPlace = placeId?.let { Prefs.getPlace(context, it) }
        if (placeId != null && timerPlace == null) {
            toast(missingManualTimerStartPlaceMessage())
            return
        }
        request.editablePlaceId
            ?.takeIf { it.isNotBlank() }
            ?.let { Prefs.getPlace(context, it) }
            ?.let { editablePlace ->
                Prefs.upsertPlace(
                    context,
                    editablePlace.withTimerDefaults(editablePlace.radiusMeters, request.durationMinutes),
                    makeActive = false,
                )
            }
            ?: run {
                if (placeId == null) {
                    Prefs.setDefaultDurationMinutes(context, request.durationMinutes)
                    defaultDurationText = formatHoursInput(request.durationMinutes / 60.0)
                }
            }
        TimerController.startTimer(
            context,
            request.durationMinutes,
            placeId,
            allowActivePlaceFallback = placeId != null,
        )
        timerEnd = Prefs.getTimerEnd(context)
        scope.launch {
            BackendClient.trackEvent(
                context,
                "timer_manual_started",
                mapOf("durationMinutes" to request.durationMinutes),
            )
        }
        if (showStartedToast) {
            toast(
                placeId
                    ?.let { "Timer started at ${timerPlace?.safeLabel ?: "place"}" }
                    ?: "Timer started"
            )
        }
    }

    fun startOrCancelTimer() {
        if (timerActive) {
            requestTimerCancelConfirmation()
            return
        }
        if (pendingPlacePreview != null) {
            homeDockExpanded = true
            toast(pendingPreviewBlockedMessage())
            return
        }
        val durationMin = parseDurationMinutes()
        if (durationMin == null) {
            toast(durationActionErrorMessage(durationText))
        } else if (!hasNotifications(context)) {
            val permissionUiActive = monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = monitoringPermissionRequestInFlight || appSettingsReturnInFlight,
                backgroundDisclosureVisible = showBackgroundLocationDisclosure,
            )
            if (
                !shouldLaunchManualTimerNotificationPermissionRequest(
                    notificationsGranted = false,
                    permissionUiAlreadyActive = permissionUiActive,
                )
            ) {
                toast(manualTimerNotificationPermissionPrompt(permissionUiAlreadyActive = true))
                return
            }
            setPendingManualTimerStart(
                ManualTimerStartRequest(
                    placeId = currentActionPlace()?.id,
                    editablePlaceId = currentSettingsPlace()?.id,
                    durationMinutes = durationMin,
                    requestedAtMillis = System.currentTimeMillis(),
                )
            )
            monitoringPermissionRequestInFlight = true
            permissionLauncher.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            )
            toast(manualTimerNotificationPermissionPrompt(permissionUiAlreadyActive = false))
        } else {
            startManualTimer(
                ManualTimerStartRequest(
                    placeId = currentActionPlace()?.id,
                    editablePlaceId = currentSettingsPlace()?.id,
                    durationMinutes = durationMin,
                    requestedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun startPlaceTimerFromPlaces(place: DwellPlace) {
        if (timerActive) {
            toast(placesRowActiveTimerBlockDetail(timerPlaceLabel))
            return
        }
        if (pendingPlacePreview != null) {
            homeDockExpanded = true
            toast(pendingPreviewBlockedMessage())
            return
        }
        if (!Prefs.hasSavedPlaceId(context, place.id)) {
            toast(missingManualTimerStartPlaceMessage())
            refreshPlaces()
            return
        }
        val request = ManualTimerStartRequest(
            placeId = place.id,
            editablePlaceId = null,
            durationMinutes = place.durationMinutes,
            requestedAtMillis = System.currentTimeMillis(),
        )
        if (!hasNotifications(context)) {
            val permissionUiActive = monitoringPermissionUiAlreadyActive(
                permissionRequestInFlight = monitoringPermissionRequestInFlight || appSettingsReturnInFlight,
                backgroundDisclosureVisible = showBackgroundLocationDisclosure,
            )
            if (
                !shouldLaunchManualTimerNotificationPermissionRequest(
                    notificationsGranted = false,
                    permissionUiAlreadyActive = permissionUiActive,
                )
            ) {
                toast(manualTimerNotificationPermissionPrompt(permissionUiAlreadyActive = true))
                return
            }
            setPendingManualTimerStart(request)
            monitoringPermissionRequestInFlight = true
            permissionLauncher.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            )
            toast(manualTimerNotificationPermissionPrompt(permissionUiAlreadyActive = false))
        } else {
            startManualTimer(request, showStartedToast = true)
        }
    }

    fun cancelPlaceTimerFromPlaces(place: DwellPlace) {
        if (!timerActive || timerPlaceId != place.id) {
            timerEnd = Prefs.getTimerEnd(context)
            WearSync.pushState(context)
            toast("That timer changed - review the latest state")
            return
        }
        requestTimerCancelConfirmation()
    }

    fun showManualTimerNotificationDeniedHelp(request: ManualTimerStartRequest) {
        showLongActionMessage(
            message = manualTimerNotificationDeniedMessage(),
            actionLabel = permissionRecoveryActionLabel(),
            onAction = {
                setPendingManualTimerStart(request)
                launchAppSettings()
            },
        )
    }

    LaunchedEffect(
        permVersion,
        pendingManualTimerStart,
        timerActive,
        monitoringPermissionRequestInFlight,
        appSettingsReturnInFlight,
    ) {
        val pending = pendingManualTimerStart ?: return@LaunchedEffect
        val targetPlaceExists = pending.placeId.isNullOrBlank() ||
            Prefs.getPlace(context, pending.placeId) != null
        val nowMillis = System.currentTimeMillis()
        val requestAgeMillis = nowMillis - pending.requestedAtMillis
        val resume = manualTimerStartAfterNotificationPermission(
            request = pending,
            notificationsGranted = hasNotifications(context),
            timerRunning = timerActive,
            targetPlaceExists = targetPlaceExists,
            nowMillis = nowMillis,
        )
        when {
            resume != null -> {
                setPendingManualTimerStart(null)
                startManualTimer(resume, showStartedToast = true)
            }
            manualTimerNotificationPermissionStillPending(
                notificationsGranted = hasNotifications(context),
                permissionRequestInFlight = monitoringPermissionRequestInFlight || appSettingsReturnInFlight,
                requestAgeMillis = requestAgeMillis,
            ) -> Unit
            else -> {
                setPendingManualTimerStart(null)
                val blockedMessage = manualTimerStartBlockedAfterPermissionMessage(
                    targetPlaceExists = targetPlaceExists,
                    timerRunning = timerActive,
                    pendingPlaceId = pending.placeId,
                    runningTimerPlaceId = timerPlaceId,
                    runningTimerPlaceLabel = timerPlaceLabel,
                    notificationsGranted = hasNotifications(context),
                    requestAgeMillis = requestAgeMillis,
                )
                if (blockedMessage == manualTimerNotificationDeniedMessage()) {
                    showManualTimerNotificationDeniedHelp(pending)
                } else {
                    blockedMessage?.let(::toast)
                }
            }
        }
    }

	    fun pauseSelectedPlace() {
	        val place = currentActionPlace() ?: run {
	            toast(choosePlaceToPauseMessage())
	            return
	        }
        GeofenceManager.setPlaceMonitoring(context, place.id, false) { ok, err ->
            refreshPlaces()
            if (ok) {
                syncSelectedZone(isArmed = false)
                toast(placePausedMessage(place.safeLabel))
                scope.launch {
                    BackendClient.trackEvent(context, "geofence_disarmed")
                }
            } else {
                toast(monitoringPauseFailureMessage(place.safeLabel, err))
            }
        }
    }

    fun extendTimer(minutes: Int) {
        if (!timerActive) {
            startOrCancelTimer()
            return
        }
        TimerController.extendTimer(context, minutes)
        timerEnd = Prefs.getTimerEnd(context)
        toast("+${minutes}m added")
        scope.launch {
            BackendClient.trackEvent(
                context,
                "timer_extended",
                mapOf("minutes" to minutes),
            )
        }
    }

    fun clearHomePromptNotifications(prompt: String) {
        when (prompt) {
            Prefs.WATCH_PROMPT_LEAVE_EARLY -> Notifications.clearExitQuestion(context)
            Prefs.WATCH_PROMPT_START_TIMER -> Notifications.clearArrivalQuestion(context)
            Prefs.WATCH_PROMPT_TIME_UP -> Notifications.clearDone(context)
        }
    }

    fun clearInvalidPromptFromDock(prompt: String) {
        Prefs.clearWatchPrompt(context)
        clearHomePromptNotifications(prompt)
        WearSync.pushState(context)
        toast("That prompt is no longer current")
    }

    fun promptScopeStillCurrent(scope: HomePromptActionScope?): Boolean =
        acceptsHomePromptAction(
            scope = scope,
            currentPrompt = Prefs.getWatchPrompt(context),
            currentPromptPlaceId = Prefs.getPromptPlaceId(context),
            currentPromptUpdated = Prefs.getWatchPromptUpdated(context),
            currentTimerPlaceId = Prefs.getTimerPlaceId(context),
            currentTimerStartedAt = Prefs.getTimerStartedAt(context),
            currentTimerEnd = Prefs.getTimerEnd(context),
            now = System.currentTimeMillis(),
        )

    fun rejectStaleHomePromptAction() {
        WearSync.pushState(context)
        homeDockExpanded = true
        toast("That prompt changed - review the latest state")
    }

    fun startPromptTimerFromDock(scope: HomePromptActionScope?) {
        if (scope?.prompt != Prefs.WATCH_PROMPT_START_TIMER || !promptScopeStillCurrent(scope)) {
            rejectStaleHomePromptAction()
            return
        }
        val prompt = scope.prompt
        val placeId = scope.promptPlaceId
        if (
            !Prefs.promptPlaceStillExists(
                placeId = placeId,
                placeExists = Prefs.hasSavedPlaceId(context, placeId),
            )
        ) {
            clearInvalidPromptFromDock(prompt)
            return
        }

        if (TimerController.isRunning(context)) {
            val currentTimerPlaceId = Prefs.getTimerPlaceId(context)
            if (placeId.isBlank() || currentTimerPlaceId == placeId) {
                Prefs.clearWatchPrompt(context)
                Notifications.clearArrivalQuestion(context)
                WearSync.pushState(context)
                timerEnd = Prefs.getTimerEnd(context)
                return
            }
            TimerController.cancelTimer(context)
        }

        TimerController.startTimer(
            context,
            Prefs.getDurationMinutes(context, placeId.takeIf { it.isNotBlank() }),
            placeId.takeIf { it.isNotBlank() },
        )
        timerEnd = Prefs.getTimerEnd(context)
        Notifications.clearArrivalQuestion(context)
        homeDockExpanded = false
        toast(if (placeId.isBlank()) "Timer started" else "Timer started at ${Prefs.getPlace(context, placeId)?.safeLabel ?: "place"}")
    }

    fun dismissArrivalPromptFromDock(scope: HomePromptActionScope?) {
        if (scope?.prompt != Prefs.WATCH_PROMPT_START_TIMER || !promptScopeStillCurrent(scope)) {
            rejectStaleHomePromptAction()
            return
        }
        val promptPlaceId = scope.promptPlaceId
        if (TimerController.isRunning(context)) {
            Prefs.markSwitchPromptKept(
                context,
                targetPlaceId = promptPlaceId,
                untilMillis = Prefs.getTimerEnd(context),
            )
        }
        Prefs.clearWatchPrompt(context)
        Notifications.clearArrivalQuestion(context)
        WearSync.pushState(context)
        homeDockExpanded = false
        toast(if (TimerController.isRunning(context)) "Keeping current timer" else "Not now")
    }

    fun keepLeavePromptFromDock(scope: HomePromptActionScope?) {
        if (scope?.prompt != Prefs.WATCH_PROMPT_LEAVE_EARLY || !promptScopeStillCurrent(scope)) {
            rejectStaleHomePromptAction()
            return
        }
        Prefs.markExitPromptKept(
            context,
            placeId = scope.promptPlaceId.takeIf { it.isNotBlank() }
                ?: scope.timerPlaceId.takeIf { it.isNotBlank() },
            untilMillis = Prefs.getTimerEnd(context),
        )
        Prefs.clearWatchPrompt(context)
        Notifications.clearExitQuestion(context)
        WearSync.pushState(context)
        homeDockExpanded = false
        toast("Keeping timer")
    }

    fun cancelLeavePromptTimerFromDock(scope: HomePromptActionScope?) {
        if (scope?.prompt != Prefs.WATCH_PROMPT_LEAVE_EARLY || !promptScopeStillCurrent(scope)) {
            rejectStaleHomePromptAction()
            return
        }
        Prefs.clearWatchPrompt(context)
        Notifications.clearExitQuestion(context)
        if (TimerController.isRunning(context)) {
            cancelRunningTimer()
        } else {
            WearSync.pushState(context)
        }
        homeDockExpanded = false
    }

    fun extendTimeUpPromptFromDock(scope: HomePromptActionScope?) {
        if (scope?.prompt != Prefs.WATCH_PROMPT_TIME_UP || !promptScopeStillCurrent(scope)) {
            rejectStaleHomePromptAction()
            return
        }
        val placeId = scope.timerPlaceId.takeIf { it.isNotBlank() }
        if (
            !Prefs.promptPlaceStillExists(
                placeId = placeId,
                placeExists = Prefs.hasSavedPlaceId(context, placeId),
            )
        ) {
            clearInvalidPromptFromDock(scope.prompt)
            return
        }
        Notifications.clearDone(context)
        TimerController.startTimer(
            context,
            30,
            placeId,
            allowActivePlaceFallback = placeId != null,
        )
        timerEnd = Prefs.getTimerEnd(context)
        homeDockExpanded = false
        toast(if (placeId == null) "Timer extended" else "Timer extended at ${Prefs.getPlace(context, placeId)?.safeLabel ?: "place"}")
    }

    fun markTimeUpDoneFromDock(scope: HomePromptActionScope?) {
        if (scope?.prompt != Prefs.WATCH_PROMPT_TIME_UP || !promptScopeStillCurrent(scope)) {
            rejectStaleHomePromptAction()
            return
        }
        Prefs.clearWatchPrompt(context)
        TimerController.clearCompletedTimer(context)
        Notifications.clearDone(context)
        timerEnd = Prefs.getTimerEnd(context)
        homeDockExpanded = false
        toast("Done")
    }

    fun handleHomePromptPrimary(scope: HomePromptActionScope?) {
        when (scope?.prompt) {
            Prefs.WATCH_PROMPT_START_TIMER -> startPromptTimerFromDock(scope)
            Prefs.WATCH_PROMPT_LEAVE_EARLY -> keepLeavePromptFromDock(scope)
            Prefs.WATCH_PROMPT_TIME_UP -> extendTimeUpPromptFromDock(scope)
            else -> WearSync.pushState(context)
        }
    }

    fun handleHomePromptSecondary(scope: HomePromptActionScope?) {
        when (scope?.prompt) {
            Prefs.WATCH_PROMPT_START_TIMER -> dismissArrivalPromptFromDock(scope)
            Prefs.WATCH_PROMPT_LEAVE_EARLY -> cancelLeavePromptTimerFromDock(scope)
            Prefs.WATCH_PROMPT_TIME_UP -> markTimeUpDoneFromDock(scope)
            else -> WearSync.pushState(context)
        }
    }

    LaunchedEffect(permVersion, pendingMonitoringResume) {
        val pending = pendingMonitoringResume ?: return@LaunchedEffect
        if (monitoringPermissionRequestInFlight) return@LaunchedEffect
        val foregroundPermissions = missingForegroundMonitoringPermissions()
        val recoveryStep = monitoringSetupRecoveryStep(
            foregroundPermissionsMissing = foregroundPermissions.isNotEmpty(),
            backgroundLocationMissing = !hasBackgroundLocation(context),
        )
        when {
            foregroundPermissions.isNotEmpty() -> {
                if (
                    shouldShowMonitoringSetupRecovery(
                        nextStep = recoveryStep,
                        alreadyShownStep = shownPendingMonitoringSetupStep,
                    )
                ) {
                    shownPendingMonitoringSetupStep = MonitoringSetupRecoveryStep.ForegroundPermissions
                    val permissionStatus = OnboardingPermissionStatus(
                        locationGranted = hasFineLocation(context),
                        backgroundGranted = hasBackgroundLocation(context),
                        notificationsGranted = hasNotifications(context),
                        motionGranted = hasActivityRecognition(context),
                    )
                    showLongActionMessage(
                        message = monitoringSetupForegroundRecoveryMessage(permissionStatus),
                        actionLabel = monitoringSetupForegroundRecoveryActionLabel(permissionStatus),
                        onAction = {
                            requestMonitoringPermissions(
                                pending,
                                showAlreadyActiveFeedback = true,
                            )
                        },
                    )
                }
            }
            !hasBackgroundLocation(context) -> {
                if (
                    shouldShowMonitoringSetupRecovery(
                        nextStep = recoveryStep,
                        alreadyShownStep = shownPendingMonitoringSetupStep,
                    )
                ) {
                    showBackgroundLocationDisclosureOnceForPendingResume()
                }
            }
            else -> {
                setPendingMonitoringResume(null)
                val requestedPlaceId = pending.placeId?.takeIf { it.isNotBlank() }
                val place = requestedPlaceId?.let { id -> Prefs.getPlace(context, id) }
                val resumePlan = monitoringResumeTargetPlan(
                    requestedPlaceId = requestedPlaceId,
                    savedPlaceExists = place != null,
                    currentSelectionPlaceId = currentActionPlace()?.id,
                )
                when (resumePlan.action) {
                    MonitoringResumeTargetAction.ArmCurrentSelection -> armGeofence()
                    MonitoringResumeTargetAction.StopMissingPlace -> {
                        toast(missingMonitoringResumePlaceMessage())
                    }
                    MonitoringResumeTargetAction.MonitorSavedPlace -> {
                        val savedPlace = resumePlan.placeId
                            ?.let { id -> place?.takeIf { it.id == id } ?: Prefs.getPlace(context, id) }
                        if (savedPlace == null) {
                            toast(missingMonitoringResumePlaceMessage())
                        } else {
                            GeofenceManager.setPlaceMonitoring(context, savedPlace.id, true) { ok, err ->
                                refreshPlaces()
                                if (!ok) {
                                    showMonitoringStartFailure(err, placeLabel = savedPlace.safeLabel)
                                    return@setPlaceMonitoring
                                }
                                maybeStartTimerIfAlreadyInside(
                                    MapPoint(savedPlace.latitude, savedPlace.longitude),
                                    savedPlace.radiusMeters,
                                    savedPlace.durationMinutes,
                                    savedPlace.id,
                                ) { result ->
                                    val timerRunningHere =
                                        TimerController.isRunning(context) &&
                                            Prefs.getTimerPlaceId(context) == savedPlace.id
                                    toast(
                                        alreadyInsideResultMessage(
                                            placeLabel = savedPlace.safeLabel,
                                            decision = result.decision,
                                            timerRunningForPlace = timerRunningHere,
                                            waitReason = result.waitReason,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBackgroundLocationDisclosure) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDisclosure = false },
            icon = {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
            },
            title = {
                Text("Background location")
            },
            text = {
                Text(
                    "Dwell collects location data to detect arrivals, start timers, and show leave-place prompts even when the app is closed or not in use. Your selected places may be stored with Dwell to sync timer state. Location is not used for ads."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationDisclosure = false
                        when (backgroundLocationFlowForSdk(Build.VERSION.SDK_INT)) {
                            BackgroundLocationFlow.AlreadyAllowed -> {
                                permVersion++
                            }
                            BackgroundLocationFlow.RequestPermission -> {
                                monitoringPermissionRequestInFlight = true
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                            BackgroundLocationFlow.OpenAppSettings -> {
                                launchAppSettings()
                                showBackgroundLocationHelp()
                            }
                        }
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationDisclosure = false }) {
                    Text("Not now")
                }
            },
        )
    }

    val onboardingPermissionStatus = OnboardingPermissionStatus(
        locationGranted = hasFineLocation(context),
        backgroundGranted = hasBackgroundLocation(context),
        notificationsGranted = hasNotifications(context),
        motionGranted = hasActivityRecognition(context),
    )

    if (!onboardingComplete) {
        OnboardingScreen(
            permissionStatus = onboardingPermissionStatus,
            batteryReliabilityStatus = BatteryReliability.status(context),
            onRequestPermissions = {
                requestMonitoringPermissions(showAlreadyActiveFeedback = true)
            },
            onOpenBatterySettings = ::openBatterySettings,
            onComplete = {
                Prefs.setOnboardingComplete(context, true)
                onboardingComplete = true
                homeDockExpanded = false
                val completionAction = onboardingCompletionAction(Prefs.hasPlace(context))
                route = completionAction.route
                if (completionAction.beginCreatePlace) {
                    beginCreatePlace(forceDiscardPreview = true)
                }
                searchPanelExpanded = completionAction.openSearchPanel
                if (completionAction.focusSearch) searchFocusRequest += 1
                completionAction.toastMessage?.let(::toast)
                scope.launch {
                    BackendClient.trackEvent(context, "onboarding_complete")
                }
            },
        )
        return
    }

    pendingTimerCancelScope?.let { cancelScope ->
        AlertDialog(
            onDismissRequest = { pendingTimerCancelScope = null },
            title = {
                Text(
                    timerCancelTitle(
                        timerCancelDialogPlaceLabel(
                            scope = cancelScope,
                            currentTimerPlaceId = timerPlaceId,
                            currentTimerPlaceLabel = timerPlaceLabel,
                        )
                    )
                )
            },
            text = {
                Text("This stops the current Dwell timer. Arrival monitoring for saved places stays the same.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingTimerCancelScope = null
                        if (
                            acceptsTimerCancelConfirmation(
                                scope = cancelScope,
                                currentTimerPlaceId = Prefs.getTimerPlaceId(context),
                                currentTimerStartedAt = Prefs.getTimerStartedAt(context),
                                currentTimerEnd = Prefs.getTimerEnd(context),
                                now = System.currentTimeMillis(),
                            )
                        ) {
                            cancelRunningTimer()
                        } else {
                            timerEnd = Prefs.getTimerEnd(context)
                            WearSync.pushState(context)
                            homeDockExpanded = true
                            toast("That timer changed - review the latest state")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Cancel timer")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTimerCancelScope = null }) {
                    Text("Keep timer")
                }
            },
        )
    }

    fun openSearchOnMap(mode: PlaceSelectionMode = PlaceSelectionMode.CreateNew): Boolean {
        val blockedDestinationLabel = mapSearchBlockedDestinationLabel(mode)
        val modeReady = if (mode == PlaceSelectionMode.CreateNew) {
            beginCreatePlace(blockedDestinationLabel = blockedDestinationLabel)
        } else {
            beginEditActivePlace(blockedDestinationLabel = blockedDestinationLabel)
        }
        if (!modeReady) return false
        route = AppRoute.Home
        homeDockExpanded = false
        searchPanelExpanded = true
        searchFocusRequest += 1
        return true
    }

    fun clearSearchState(clearText: Boolean = true) {
        val clearedRuntime = clearedSearchRuntimeState()
        if (clearText) searchText = ""
        searchResults = emptyList()
        searchSuggestions = emptyList()
        submittedSearchKey = ""
        searching = clearedRuntime.searching
        searchingQueryKey = clearedRuntime.searchingQueryKey
    }

    fun closeSearchPanel() {
        val plan = searchPanelClosePlan()
        searchPanelExpanded = plan.expanded
        clearSearchState(clearText = plan.clearText)
        if (plan.clearFocus) focusManager.clearFocus()
    }

    fun selectSearchResult(result: LocationSearchResult) {
        closeSearchPanel()
        clearSearchState()
        selectGeofencePoint(
            point = result.point,
            label = result.label,
            expandDock = false,
            analyticsSource = "search_result",
        )
    }

    LaunchedEffect(deletedPlaceUndo) {
        val undo = deletedPlaceUndo ?: return@LaunchedEffect
        val undoSnackbar = placeDeleteUndoSnackbarPlan(
            placeLabel = undo.place.safeLabel,
            currentSnackbarVisible = snackbarHostState.currentSnackbarData != null,
        )
        if (undoSnackbar.dismissCurrentSnackbar) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
        val result = snackbarHostState.showSnackbar(
            message = undoSnackbar.message,
            actionLabel = undoSnackbar.actionLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            val restored = Prefs.upsertPlace(
                c = context,
                place = undo.place,
                makeActive = undo.restoreFocusOnUndo,
            )
            homeDockExpanded = false
            prewarmZoneMap(MapPoint(restored.latitude, restored.longitude), restored.radiusMeters)
            if (undo.restoreFocusOnUndo) {
                applyActivePlace(restored)
                placeSelectionMode = PlaceSelectionMode.ViewSelected
                editingPlaceId = ""
                centerMapOn(MapPoint(restored.latitude, restored.longitude))
            }
            if (restored.monitoringEnabled) {
                GeofenceManager.refresh(context) { _, _ ->
                    refreshPlaces()
                    WearSync.pushState(context)
                }
            } else {
                refreshPlaces()
                WearSync.pushState(context)
            }
            if (undo.restoreFocusOnUndo) {
                syncSelectedZone(isArmed = restored.monitoringEnabled)
            }
            toast(
                placeRestoredMessage(
                    placeLabel = restored.safeLabel,
                    monitoringPausedByLimit = placeRestorePausedByMonitoringLimit(
                        deletedPlaceWasMonitoring = undo.place.monitoringEnabled,
                        restoredPlaceMonitoring = restored.monitoringEnabled,
                    ),
                    focusRestored = undo.restoreFocusOnUndo,
                )
            )
        }
        if (deletedPlaceUndo == undo) {
            deletedPlaceUndo = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (route) {
            AppRoute.Settings -> SettingsScreen(
            radius = defaultRadius,
            durationText = defaultDurationText,
            autoStart = defaultAutoStart,
            accountProvider = Prefs.getAccountProvider(context),
            accountDisplayName = Prefs.getAccountDisplayName(context),
            accountEmail = Prefs.getAccountEmail(context),
            locationGranted = hasFineLocation(context),
            backgroundGranted = hasBackgroundLocation(context),
            notificationsGranted = hasNotifications(context),
            motionGranted = hasActivityRecognition(context),
            exactAlarmAllowed = canExactAlarm(context),
            batteryReliabilityStatus = BatteryReliability.status(context),
            diagnosticsEntries = remember(diagnosticsRefresh) {
                DwellDiagnostics.entries(context)
            },
            onBack = { route = AppRoute.Home },
            onRadiusChange = {
                defaultRadius = it
            },
            onRadiusChangeFinished = {
                defaultRadius = DwellRadius.normalize(defaultRadius)
                Prefs.setDefaultRadius(context, defaultRadius)
                if (placeSelectionMode == PlaceSelectionMode.CreateNew || !Prefs.hasPlace(context)) {
                    radius = defaultRadius
                }
                toast("Default radius updated")
            },
            onDurationChange = {
                defaultDurationText = it
                durationMinutesFromText(it)?.let { minutes ->
                    Prefs.setDefaultDurationMinutes(context, minutes)
                    if (placeSelectionMode == PlaceSelectionMode.CreateNew || !Prefs.hasPlace(context)) {
                        durationText = defaultDurationText
                    }
                }
            },
            onDurationPreset = { hours ->
                defaultDurationText = formatHoursInput(hours)
                Prefs.setDefaultDurationMinutes(context, (hours * 60).roundToInt())
                if (placeSelectionMode == PlaceSelectionMode.CreateNew || !Prefs.hasPlace(context)) {
                    durationText = defaultDurationText
                }
            },
            onAutoStartChange = { enabled ->
                defaultAutoStart = enabled
                Prefs.setDefaultAutoStart(context, enabled)
                toast("Default arrival mode updated")
            },
	            onOpenAppSettings = {
	                openMonitoringSetup()
	            },
	            onOpenExactAlarmSettings = {
	                openExactAlarmSettings()
	            },
	            onOpenBatterySettings = {
	                openBatterySettings()
	            },
            onClearDiagnostics = {
                DwellDiagnostics.clear(context)
                diagnosticsRefresh += 1
                toast("Diagnostics cleared")
            },
            onCopyDiagnostics = {
                val entries = DwellDiagnostics.entries(context)
                val text = DwellDiagnostics.exportText(context, entries)
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Dwell diagnostics", text))
                toast("Diagnostics copied")
            },
            onClearMapCache = {
                MapCacheManager.clear(context) { ok ->
                    toast(if (ok) "Map cache cleared" else "Could not clear map cache")
                }
            },
            onClearSearchCache = {
                searchCache.clear()
                clearPersistentSearchCache(context)
                toast("Search cache cleared")
            },
            onOpenTutorial = {
                route = AppRoute.Tutorial
            },
            onSignOut = {
                Prefs.setSignedIn(context, false)
                signedIn = false
                route = AppRoute.Home
            },
            onDeleteAppData = {
                scope.launch {
                    GeofenceManager.disarm(context) { armed = false }
                    if (timerActive) {
                        TimerController.cancelTimer(context)
                        Notifications.notifyTimerCancelled(context)
                        insightsRefresh += 1
                    }
                    Notifications.clearAll(context)
                    Prefs.clearAppData(context, keepSession = true)
                    MapCacheManager.clear(context) { }
                    searchCache.clear()
                    clearPersistentSearchCache(context)
                    val reset = appDataClearUiReset()
                    if (reset.dismissCurrentSnackbar) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        activeLongActionKey = null
                    }
                    onboardingComplete = reset.onboardingComplete
                    places = emptyList()
                    registeredPlaceIds = emptySet()
                    monitoringError = ""
                    armed = false
                    if (reset.closeSearchPanel) {
                        searchPanelExpanded = false
                        clearSearchState()
                    }
                    if (reset.clearPendingMonitoringResume) {
                        setPendingMonitoringResume(null)
                    }
                    if (reset.clearPendingLocationResume) {
                        setPendingCurrentLocationResume(requested = false)
                        currentLocationRequestGeneration += 1L
                        locating = false
                    }
                    showBackgroundLocationDisclosure = false
                    deletedPlaceUndo = null
                    pin = null
                    pendingPlacePreview = null
                    pendingTimerCancelScope = null
                    setPendingManualTimerStart(null)
                    selectedPlaceLabel = reset.selectedPlaceLabel
                    selectedPlaceId = reset.selectedPlaceId
                    viewingPlaceId = reset.viewingPlaceId
                    editingPlaceId = reset.editingPlaceId
                    placeSelectionMode = reset.placeSelectionMode
                    radius = Prefs.getRadius(context)
                    durationText = formatHoursInput(Prefs.getDurationMinutes(context) / 60.0)
                    defaultRadius = Prefs.getDefaultRadius(context)
                    defaultDurationText = formatHoursInput(Prefs.getDefaultDurationMinutes(context) / 60.0)
                    defaultAutoStart = Prefs.getDefaultAutoStart(context)
                    timerEnd = reset.timerEndMillis
                    WearSync.pushState(context)
                    route = reset.route
                    toast(appDataDeletedMessage())
                    val serverDeleted = BackendClient.deleteAppData(context) != null
                    if (!serverDeleted) {
                        toast(appDataServerCleanupFailedMessage())
                    }
                }
            },
            onDeleteAccount = {
                scope.launch {
                    val deleted = BackendClient.deleteAccount(context) != null
                    if (!deleted) {
                        toast("Could not delete account. Check connection and try again.")
                        return@launch
                    }

                    GeofenceManager.disarm(context) { armed = false }
                    if (timerActive) {
                        TimerController.cancelTimer(context)
                        Notifications.notifyTimerCancelled(context)
                        insightsRefresh += 1
                    }
                    Notifications.clearAll(context)
                    Prefs.clearAppData(context, keepSession = false)
                    MapCacheManager.clear(context) { }
                    searchCache.clear()
                    clearPersistentSearchCache(context)
                    val reset = appDataClearUiReset()
                    onboardingComplete = reset.onboardingComplete
                    places = emptyList()
                    registeredPlaceIds = emptySet()
                    monitoringError = ""
                    armed = false
                    if (reset.closeSearchPanel) {
                        searchPanelExpanded = false
                        clearSearchState()
                    }
                    if (reset.clearPendingMonitoringResume) {
                        setPendingMonitoringResume(null)
                    }
                    if (reset.clearPendingLocationResume) {
                        setPendingCurrentLocationResume(requested = false)
                        currentLocationRequestGeneration += 1L
                        locating = false
                    }
                    showBackgroundLocationDisclosure = false
                    deletedPlaceUndo = null
                    pin = null
                    pendingPlacePreview = null
                    pendingTimerCancelScope = null
                    setPendingManualTimerStart(null)
                    selectedPlaceLabel = reset.selectedPlaceLabel
                    selectedPlaceId = reset.selectedPlaceId
                    viewingPlaceId = reset.viewingPlaceId
                    editingPlaceId = reset.editingPlaceId
                    placeSelectionMode = reset.placeSelectionMode
                    radius = Prefs.getRadius(context)
                    durationText = formatHoursInput(Prefs.getDurationMinutes(context) / 60.0)
                    defaultRadius = Prefs.getDefaultRadius(context)
                    defaultDurationText = formatHoursInput(Prefs.getDefaultDurationMinutes(context) / 60.0)
                    defaultAutoStart = Prefs.getDefaultAutoStart(context)
                    timerEnd = reset.timerEndMillis
                    signedIn = false
                    WearSync.pushState(context)
                    route = reset.route
                    toast("Account deleted")
                }
            },
        )
            AppRoute.Tutorial -> TutorialScreen(
                onBack = { route = AppRoute.Settings },
                onAddPlace = {
                    route = AppRoute.Home
                    if (beginCreatePlace(blockedDestinationLabel = "Add place")) {
                        searchPanelExpanded = true
                        searchFocusRequest += 1
                    }
                },
                onOpenPlaces = {
                    route = AppRoute.SavedZones
                },
                onOpenSetup = {
                    openSetupChecks(AppRoute.Tutorial)
                },
            )
            AppRoute.SetupChecks -> SetupChecksScreen(
                permissionStatus = OnboardingPermissionStatus(
                    locationGranted = hasFineLocation(context),
                    backgroundGranted = hasBackgroundLocation(context),
                    notificationsGranted = hasNotifications(context),
                    motionGranted = hasActivityRecognition(context),
                ),
                exactAlarmAllowed = canExactAlarm(context),
                batteryReliabilityStatus = BatteryReliability.status(context),
                onBack = { route = setupChecksBackRoute },
                onOpenAppSettings = ::openMonitoringSetup,
                onOpenExactAlarmSettings = ::openExactAlarmSettings,
                onOpenBatterySettings = ::openBatterySettings,
            )
            AppRoute.SavedZones -> SavedZonesScreen(
            places = places,
            registeredPlaceIds = registeredPlaceIds,
            monitoringError = monitoringError,
            monitoringHealth = monitoringHealth,
            viewingPlaceId = viewingPlaceId,
            editingPlaceId = editingPlaceId,
            timerPlaceId = timerPlaceId,
            timerPlaceLabel = timerPlaceLabel,
            timerActive = timerActive,
            pendingPlacePreview = pendingPreview,
	            onBack = {
	                route = AppRoute.Home
	                if (placesBackShouldExpandPendingPreview(pendingPreview != null)) {
	                    homeDockExpanded = true
	                }
	            },
	            onOpenMonitoringSetup = { openSetupChecks(AppRoute.SavedZones) },
            onRefreshMonitoring = ::refreshMonitoringFromHealthCard,
	            onOpenExactAlarmSettings = ::openExactAlarmSettings,
            onOpenBatterySettings = ::openBatterySettings,
            onReturnToPreview = {
                route = AppRoute.Home
                homeDockExpanded = true
                pendingPlacePreview?.point?.let(::centerMapOn)
            },
            onDiscardPreview = {
                cancelPendingPlacePreview()
                refreshPlaces()
            },
            onCreateZone = createZone@ {
                if (openSearchOnMap(PlaceSelectionMode.CreateNew)) {
                    toast(addPlacePromptMessage())
                }
            },
            onViewPlace = onViewPlace@ { place ->
                if (!viewPlaceOnMap(place, blockedDestinationLabel = "View map")) return@onViewPlace
                refreshPlaces()
                route = AppRoute.Home
                centerMapOn(MapPoint(place.latitude, place.longitude))
            },
            onEditPlace = onEditPlace@ { place ->
                if (!beginEditPlace(
                        place,
                        expandDock = true,
                        blockedDestinationLabel = "Edit settings",
                    )
                ) {
                    return@onEditPlace
                }
                refreshPlaces()
                route = AppRoute.Home
                centerMapOn(MapPoint(place.latitude, place.longitude))
            },
            onStartPlaceTimer = { place ->
                startPlaceTimerFromPlaces(place)
            },
            onCancelPlaceTimer = { place ->
                cancelPlaceTimerFromPlaces(place)
            },
            onToggleMonitoring = toggleMonitoring@ { place, enabled ->
                if (enabled) {
                    when {
                        !hasFineLocation(context) ||
                            !hasNotifications(context) ||
                        !hasActivityRecognition(context) -> {
                            requestMonitoringPermissions(
                                PendingMonitoringResume(place.id),
                                showAlreadyActiveFeedback = true,
                            )
                            return@toggleMonitoring
                        }
                        !hasBackgroundLocation(context) -> {
                            setPendingMonitoringResume(PendingMonitoringResume(place.id))
                            showBackgroundLocationDisclosureOnceForPendingResume()
                            return@toggleMonitoring
                        }
                    }
                }

                GeofenceManager.setPlaceMonitoring(context, place.id, enabled) { ok, err ->
                    refreshPlaces()
                    if (!ok) {
                        if (enabled) {
                            showMonitoringStartFailure(
                                error = err,
                                alreadyOnPlaces = true,
                                placeLabel = place.safeLabel,
                            )
                        } else {
                            toast(monitoringPauseFailureMessage(place.safeLabel, err))
                        }
                        return@setPlaceMonitoring
                    }

                    if (enabled) {
                        val updated = Prefs.getPlace(context, place.id) ?: place
                        val batteryStatus = BatteryReliability.status(context)
                        if (batteryNeedsReliabilityReview(batteryStatus)) {
                            showBatteryHelp(batteryStatus)
                        }
                        maybeStartTimerIfAlreadyInside(
                            MapPoint(updated.latitude, updated.longitude),
                            updated.radiusMeters,
                            updated.durationMinutes,
                            updated.id,
                        ) { result ->
                            val timerRunningHere =
                                TimerController.isRunning(context) &&
                                    Prefs.getTimerPlaceId(context) == updated.id
                            toast(
                                alreadyInsideResultMessage(
                                    placeLabel = updated.safeLabel,
                                    decision = result.decision,
                                    timerRunningForPlace = timerRunningHere,
                                    waitReason = result.waitReason,
                                )
                            )
                        }
                    } else {
                        toast(placePausedMessage(place.safeLabel))
                    }
                }
            },
            onDeletePlace = { place ->
                scope.launch {
                    val routeBeforeDeletion = route
                    val deletedPlaceWasOpen = isOpenPlaceDeletion(
                        deletedPlaceId = place.id,
                        selectedPlaceId = selectedPlaceId,
                        viewingPlaceId = viewingPlaceId,
                        editingPlaceId = editingPlaceId,
                    )
                    val deletedPlacePrompt = Prefs.getWatchPrompt(context).takeIf {
                        Prefs.shouldClearPromptForDeletedPlace(
                            prompt = it,
                            promptPlaceId = Prefs.getPromptPlaceId(context),
                            timerPlaceId = Prefs.getTimerPlaceId(context),
                            deletedPlaceId = place.id,
                        )
                    }
                    if (timerActive && Prefs.getTimerPlaceId(context) == place.id) {
                        cancelRunningTimer()
                    }
                    if (place.monitoringEnabled) {
                        GeofenceManager.setPlaceMonitoring(context, place.id, false) { _, _ -> }
                    }
                    Prefs.deletePlace(context, place.id)
                    if (Prefs.getPlaces(context).isEmpty()) {
                        BackendClient.deletePrimaryZone(context)
                    }
                    GeofenceManager.refresh(context) { _, _ -> }
                    when (deletedPlacePrompt) {
                        Prefs.WATCH_PROMPT_START_TIMER -> Notifications.clearArrivalQuestion(context)
                        Prefs.WATCH_PROMPT_LEAVE_EARLY -> Notifications.clearExitQuestion(context)
                        Prefs.WATCH_PROMPT_TIME_UP -> Notifications.clearDone(context)
                    }
                    WearSync.pushState(context)
                    BackendClient.trackEvent(context, "zone_deleted")
                    refreshPlaces()
                    val nextPlace = Prefs.getActivePlace(context)
                    val deletionTransition = savedPlaceDeletionUiTransition(
                        currentRoute = routeBeforeDeletion,
                        deletedPlaceWasOpen = deletedPlaceWasOpen,
                        nextPlaceId = nextPlace?.id,
                    )
                    when (deletionTransition.focusAction) {
                        SavedPlaceDeletionFocusAction.PreserveCurrentPlace -> Unit
                        SavedPlaceDeletionFocusAction.FocusNextPlace -> {
                            val focusPlace = deletionTransition.nextPlaceId
                                ?.let { Prefs.getPlace(context, it) }
                                ?: nextPlace
                            if (focusPlace != null) {
                                placeSelectionMode = PlaceSelectionMode.ViewSelected
                                applyActivePlace(focusPlace)
                                editingPlaceId = ""
                            } else {
                                clearOpenPlaceAfterDeletion()
                            }
                        }
                        SavedPlaceDeletionFocusAction.ClearOpenPlace -> {
                            clearOpenPlaceAfterDeletion()
                        }
                    }
                    deletedPlaceUndo = DeletedPlaceUndo(
                        place = place,
                        restoreFocusOnUndo = shouldRestoreDeletedPlaceFocusOnUndo(deletedPlaceWasOpen),
                    )
                    homeDockExpanded = false
                    route = deletionTransition.route
	                }
	            },
	        )
            AppRoute.Insights -> InsightsScreen(
            summary = remember(insightsRefresh, now / 60_000L) {
                DwellInsights.summaryFor(
                    sessions = DwellInsights.loadSessions(context),
                    nowMillis = now,
                )
            },
            onBack = { route = AppRoute.Home },
            onOpenPlaces = { route = AppRoute.SavedZones },
        )
            AppRoute.Home -> {
            val backAction = homeBackAction(
                searchPanelExpanded = searchPanelExpanded,
                homeDockExpanded = homeDockExpanded,
                hasPendingPlacePreview = pendingPreview != null,
            )
            BackHandler(enabled = backAction != HomeBackAction.LetSystemHandle) {
                when (backAction) {
                    HomeBackAction.CloseSearch -> closeSearchPanel()
                    HomeBackAction.CollapseDock -> homeDockExpanded = false
                    HomeBackAction.ExpandPendingPreview -> homeDockExpanded = true
                    HomeBackAction.LetSystemHandle -> Unit
                }
            }
            val railCurrentLocationSelectsPlace = mapCurrentLocationSelectsPlace(
                selectionMode = placeSelectionMode,
                hasEditingPlace = currentEditingPlace() != null,
            )
            val railCurrentLocationDescription = mapCurrentLocationActionDescription(
                selectsPlace = railCurrentLocationSelectsPlace,
                editingSelectedPlace = placeSelectionMode == PlaceSelectionMode.EditSelected,
            )
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                if (searchPanelExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { closeSearchPanel() }
                            }
                            .zIndex(3f),
                    )
                }

                MapHeader(
                    placeLabel = placeLabel,
                    modePlaceLabel = mapModePlaceLabel,
                    hasPlaceContext = hasMapSearchContext(
                        hasPin = pin != null,
                        hasSavedPlace = places.isNotEmpty(),
                        hasPendingPlacePreview = pendingPreview != null,
                    ),
                    hasSavedPlace = places.isNotEmpty(),
                    addingNewPlace = placeSelectionMode == PlaceSelectionMode.CreateNew,
                    editingSelectedPlace = placeSelectionMode == PlaceSelectionMode.EditSelected &&
                        selectedPlaceId.isNotBlank(),
                    currentLocationSelectsPlace = railCurrentLocationSelectsPlace,
                    searchText = searchText,
                    searchResults = searchResults,
                    searchSuggestions = searchSuggestions,
                    searching = searching,
                    searchingCurrentQuery = searching && searchingQueryKey == searchCacheKey(searchText),
                    hasSubmittedSearch = submittedSearchKey == searchCacheKey(searchText),
                    locating = locating,
                    focusRequest = searchFocusRequest,
                    expanded = searchPanelExpanded,
                    onExpandedChange = { searchPanelExpanded = it },
                    onSearchTextChange = {
                        homeDockExpanded = false
                        searchPanelExpanded = true
                        updateSearchText(it)
                    },
                    onSubmit = {
                        homeDockExpanded = false
                        searchPanelExpanded = true
                        performSearch()
                    },
                    onClearSearch = {
                        clearSearchState()
                    },
                    onCloseSearch = ::closeSearchPanel,
                    onSelectResult = ::selectSearchResult,
                    onUseCurrentLocation = {
                        closeSearchPanel()
                        clearSearchState()
                        homeDockExpanded = false
                        requestCurrentLocation(selectAsZone = railCurrentLocationSelectsPlace)
                    },
                    onOpenSavedZones = {
                        closeSearchPanel()
                        clearSearchState()
                        homeDockExpanded = false
                        route = AppRoute.SavedZones
                    },
                    onCreateMode = {
                        if (beginCreatePlace(blockedDestinationLabel = "Add place")) {
                            searchPanelExpanded = true
                            searchFocusRequest += 1
                        }
                    },
                    onEditSelectedMode = {
                        if (beginEditActivePlace(blockedDestinationLabel = "Move place")) {
                            searchPanelExpanded = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(4f),
                )

                AnimatedVisibility(
                    visible = !homeDockExpanded && !searchPanelExpanded,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(2f),
                ) {
                    MapActionRail(
                        locating = locating,
                        currentLocationContentDescription = railCurrentLocationDescription,
                        onCurrentLocation = {
                            closeSearchPanel()
                            homeDockExpanded = false
                            requestCurrentLocation(
                                selectAsZone = railCurrentLocationSelectsPlace,
                                expandDock = railCurrentLocationSelectsPlace,
                            )
                        },
                        onSavedZones = {
                            closeSearchPanel()
                            homeDockExpanded = false
                            route = AppRoute.SavedZones
                        },
                        onInsights = onInsights@ {
                            closeSearchPanel()
                            if (blockPendingPlacePreview("Insights")) return@onInsights
                            homeDockExpanded = false
                            route = AppRoute.Insights
                        },
                        onSettings = onSettings@ {
                            closeSearchPanel()
                            if (blockPendingPlacePreview("Settings")) return@onSettings
                            homeDockExpanded = false
                            route = AppRoute.Settings
                        },
                    )
                }

                AttributionPill(
                    label = mobileMapConfig.attributionLabel,
                    dockExpanded = homeDockExpanded,
                    modifier = Modifier.align(Alignment.BottomStart),
                )

                HomeStatusDock(
                    statusTitle = statusTitle,
                    statusDetail = statusDetail,
                    placeLabel = dockPlaceLabel,
                    hasPlace = hasActionableDockPlace(
                        hasPin = pin != null || pendingPreview != null,
                        hasActivePlace = activePlace != null,
                        timerActive = timerActive,
                        promptActive = promptState != null,
                    ),
                    placeName = selectedPlaceLabel,
                    placeNameFallback = placeNameFallback,
                    radius = dockRadius,
                    monitoredRadiusLimit = monitoredRadiusLimit,
                    durationText = durationText,
                    durationMinutes = dockDurationMinutes,
                    timerActive = timerActive,
                    activePlaceArmed = activePlaceArmed,
                    activePlaceRegistered = activePlaceRegistered,
	                    activePlaceNeedsSetup = activePlaceNeedsSetup,
	                    activePlaceAutoStart = dockAutoStart,
	                    promptState = promptState,
	                    viewingSavedPlaceReadOnly = viewingSavedPlaceReadOnly,
	                    pendingPlacePreview = pendingPreview != null,
	                    pendingPlaceMove = pendingPreviewEditing,
                    pendingPlaceActionLabel = pendingPreview?.let {
                        pendingPlacePrimaryActionLabel(
                            editingSelectedPlace = pendingPreviewEditing,
                            targetLabel = it.targetPlaceLabel,
                        )
                    } ?: "",
                    armedPlaceCount = armedPlaceCount,
                    livePlaceCount = livePlaceCount,
                    monitoringError = monitoringError,
                    setupNotice = setupNotice,
                    batteryWarning = batteryWarning,
                    onPrimaryAction = {
                        when {
                            promptState != null -> handleHomePromptPrimary(promptActionScope)
                            pendingPreview != null -> commitPendingPlacePreview()
                            timerActive -> extendTimer(30)
                            pin == null -> {
                                placeSelectionMode = PlaceSelectionMode.CreateNew
                                requestCurrentLocation(selectAsZone = true)
                            }
                            homeMonitorTarget == HomeMonitorActionTarget.OpenSetupChecks ->
                                openSetupChecks(AppRoute.Home)
                            homeMonitorTarget == HomeMonitorActionTarget.PauseMonitoring ->
                                pauseSelectedPlace()
                            else -> armGeofence()
                        }
                    },
	                    onSecondaryAction = {
	                        when {
	                            promptState != null -> handleHomePromptSecondary(promptActionScope)
	                            pendingPreview != null -> cancelPendingPlacePreview()
	                            timerActive -> startOrCancelTimer()
	                            else -> route = AppRoute.SavedZones
	                        }
	                    },
	                    onEditPlaceClick = {
	                        beginEditActivePlace(expandDock = true)
	                    },
	                    expanded = homeDockExpanded,
                    onExpandedChange = { homeDockExpanded = it },
                    onRadiusChange = { radius = it },
                    onRadiusChangeFinished = {
                        commitRadiusChange(fitMap = true)
                    },
                    onPlaceNameChange = { updateSelectedPlaceName(it) },
                    onDurationChange = {
                        persistDurationText(it)
                    },
                    onDurationPreset = { hours ->
                        val presetText = formatHoursInput(hours)
                        val durationMin = (hours * 60).roundToInt()
                        val place = currentSettingsPlace()
                        when (
                            durationPresetPersistenceTarget(
                                hasPendingPlacePreview = pendingPreview != null,
                                selectionMode = placeSelectionMode,
                                hasSettingsPlace = place != null,
                                hasSelectedPlace = currentSelectedPlace() != null,
                            )
                        ) {
                            SettingsPersistenceTarget.PendingPreview -> {
                                durationText = presetText
                                return@HomeStatusDock
                            }
                            SettingsPersistenceTarget.ReadOnlyPlace -> return@HomeStatusDock
                            SettingsPersistenceTarget.EditingPlace -> {
                                durationText = presetText
                                Prefs.upsertPlace(
                                    context,
                                    place!!.withTimerDefaults(place.radiusMeters, durationMin),
                                    makeActive = false,
                                )
                            }
                            SettingsPersistenceTarget.DefaultSettings -> {
                                durationText = presetText
                                Prefs.setDefaultDurationMinutes(context, durationMin)
                                defaultDurationText = durationText
                            }
                        }
                        WearSync.pushState(context)
                        syncSelectedZone(isArmed = activePlaceArmed)
                    },
                    onAutoStartChange = { enabled ->
                        pendingPlacePreview?.let {
                            pendingPlacePreview = it.copy(autoStart = enabled)
                            return@HomeStatusDock
                        }
                        val place = currentSettingsPlace() ?: return@HomeStatusDock
                        if (Prefs.setPlaceAutoStart(context, place.id, enabled)) {
                            refreshPlaces()
                            WearSync.pushState(context)
                            toast(
                                if (enabled) {
                                    "High-confidence arrivals will start automatically"
                                } else {
                                    "Dwell will ask before starting at this place"
                                }
                            )
                        }
                    },
                    onArmClick = {
                        when (homeMonitorTarget) {
                            HomeMonitorActionTarget.OpenSetupChecks ->
                                openSetupChecks(AppRoute.Home)
                            HomeMonitorActionTarget.PauseMonitoring ->
                                pauseSelectedPlace()
                            HomeMonitorActionTarget.StartMonitoring ->
                                armGeofence()
                        }
                    },
                    onTimerClick = {
                        startOrCancelTimer()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
                )
            }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 100.dp)
                .zIndex(5f),
        )
    }
}

@Composable
private fun OnboardingScreen(
    permissionStatus: OnboardingPermissionStatus,
    batteryReliabilityStatus: BatteryReliabilityStatus,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onComplete: () -> Unit,
) {
    var page by remember { mutableStateOf(OnboardingPage.Intro) }

    BackHandler(enabled = page != OnboardingPage.Intro) {
        page = when (page) {
            OnboardingPage.Intro -> OnboardingPage.Intro
            OnboardingPage.Permissions -> OnboardingPage.Intro
            OnboardingPage.Guide -> OnboardingPage.Permissions
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(26.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Welcome to Dwell",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Set up place timers that run from your phone and watch.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OnboardingStepPill("1", "About", page == OnboardingPage.Intro)
                OnboardingStepPill("2", "Permissions", page == OnboardingPage.Permissions)
                OnboardingStepPill("3", "Save", page == OnboardingPage.Guide)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (page) {
                        OnboardingPage.Intro -> OnboardingIntroPage()
                        OnboardingPage.Permissions -> OnboardingPermissionsPage(
                            permissionStatus = permissionStatus,
                            batteryReliabilityStatus = batteryReliabilityStatus,
                            onRequestPermissions = onRequestPermissions,
                            onOpenBatterySettings = onOpenBatterySettings,
                        )
                        OnboardingPage.Guide -> OnboardingGuidePage(permissionStatus)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (page != OnboardingPage.Intro) {
                    OutlinedButton(
                        onClick = {
                            page = when (page) {
                                OnboardingPage.Intro -> OnboardingPage.Intro
                                OnboardingPage.Permissions -> OnboardingPage.Intro
                                OnboardingPage.Guide -> OnboardingPage.Permissions
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Button(
                    onClick = {
                        when (page) {
                            OnboardingPage.Intro -> page = OnboardingPage.Permissions
                            OnboardingPage.Permissions -> {
                                if (permissionStatus.allMajorGranted) {
                                    page = OnboardingPage.Guide
                                } else {
                                    onRequestPermissions()
                                }
                            }
                            OnboardingPage.Guide -> onComplete()
                        }
                    },
                    enabled = page != OnboardingPage.Guide || permissionStatus.allMajorGranted,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        onboardingPrimaryActionLabel(
                            page = page,
                            permissionStatus = permissionStatus,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingIntroPage() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "What Dwell does",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Dwell watches saved places like Home, Office, Gym, or Library. When you arrive, it can start a timer automatically or ask first.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OnboardingGuideStep(
            number = 1,
            icon = Icons.Filled.Place,
            title = "Save places",
            detail = "Each place keeps its own radius, timer duration, and arrival mode.",
        )
        OnboardingGuideStep(
            number = 2,
            icon = Icons.Filled.NotificationsActive,
            title = "Monitor arrivals",
            detail = "Dwell detects arrivals in the background after permissions are ready.",
        )
        OnboardingGuideStep(
            number = 3,
            icon = Icons.Filled.Watch,
            title = "Use phone and watch",
            detail = "Prompts, timers, leave-early checks, and time-up alerts stay synced.",
        )
    }
}

@Composable
private fun OnboardingPermissionsPage(
    permissionStatus: OnboardingPermissionStatus,
    batteryReliabilityStatus: BatteryReliabilityStatus,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Permissions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            onboardingPermissionHelp(permissionStatus),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OnboardingPermissionRow(
            title = "Location",
            detail = "Find your current place and validate arrivals.",
            granted = permissionStatus.locationGranted,
        )
        OnboardingPermissionRow(
            title = "Background location",
            detail = "Detect arrivals after the app is closed.",
            granted = permissionStatus.backgroundGranted,
        )
        OnboardingPermissionRow(
            title = "Notifications",
            detail = "Ask to start, show running timers, and alert when time is up.",
            granted = permissionStatus.notificationsGranted,
        )
        OnboardingPermissionRow(
            title = "Physical activity",
            detail = "Avoid auto-starting during pass-through movement.",
            granted = permissionStatus.motionGranted,
        )
        if (!permissionStatus.allMajorGranted) {
            FilledTonalButton(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    onboardingPermissionRecoveryButtonLabel(permissionStatus),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (batteryNeedsReliabilityReview(batteryReliabilityStatus)) {
            FilledTonalButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    setupChecksBatteryActionLabel(batteryReliabilityStatus) ?: batteryHelpActionLabel(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OnboardingGuidePage(permissionStatus: OnboardingPermissionStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Create your first place",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (permissionStatus.allMajorGranted) {
                "Now Dwell can guide you to add a place, review it, save it, and turn on monitoring."
            } else {
                "Finish permissions first, then Dwell will open Add place mode."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        onboardingGuideSteps().forEachIndexed { index, step ->
            OnboardingGuideStep(
                number = index + 1,
                icon = when (index) {
                    0 -> Icons.Filled.Search
                    1 -> Icons.Filled.Map
                    else -> Icons.Filled.CheckCircle
                },
                title = when (index) {
                    0 -> "Pick a location"
                    1 -> "Review settings"
                    else -> "Save and monitor"
                },
                detail = step,
            )
        }
    }
}

@Composable
private fun OnboardingStepPill(
    number: String,
    label: String,
    selected: Boolean,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                number,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OnboardingPermissionRow(
    title: String,
    detail: String,
    granted: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (granted) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                onboardingPermissionRowStatus(title, granted),
                color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun OnboardingGuideStep(
    number: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "$number. $title",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AuthScreen(
    authInFlight: Boolean,
    authError: String?,
    onGoogleSignIn: () -> Unit,
    onContinueLocal: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Dwell",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Places, timers, and watch sync.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OnboardingGuideStep(
                    number = 1,
                    icon = Icons.Filled.Place,
                    title = "Save places",
                    detail = "Create Home, Office, Gym, or anywhere you want timed.",
                )
                OnboardingGuideStep(
                    number = 2,
                    icon = Icons.Filled.NotificationsActive,
                    title = "Arrive and start",
                    detail = "Dwell can auto-start or ask before starting each place timer.",
                )
                OnboardingGuideStep(
                    number = 3,
                    icon = Icons.Filled.Watch,
                    title = "Stay synced",
                    detail = "Phone and watch show the same prompts, countdowns, and alerts.",
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onGoogleSignIn,
                enabled = !authInFlight,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                if (authInFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "G",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(if (authInFlight) "Signing in..." else "Continue with Google")
            }
            authError?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(onClick = onContinueLocal, enabled = !authInFlight) {
                Text("Continue locally")
            }
        }
    }
}

@Composable
private fun MapHeader(
    placeLabel: String,
    modePlaceLabel: String,
    hasPlaceContext: Boolean,
    hasSavedPlace: Boolean,
    addingNewPlace: Boolean,
    editingSelectedPlace: Boolean,
    currentLocationSelectsPlace: Boolean,
    searchText: String,
    searchResults: List<LocationSearchResult>,
    searchSuggestions: List<LocationSearchResult>,
    searching: Boolean,
    searchingCurrentQuery: Boolean,
    hasSubmittedSearch: Boolean,
    locating: Boolean,
    focusRequest: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSelectResult: (LocationSearchResult) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenSavedZones: () -> Unit,
    onCreateMode: () -> Unit,
    onEditSelectedMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) {
            onExpandedChange(true)
            focusRequester.requestFocus()
        }
    }
    val dropdownVisible = searchDropdownVisible(
        expanded = expanded,
        searchFocused = searchFocused,
        searchText = searchText,
        hasResults = searchResults.isNotEmpty(),
        hasSuggestions = searchSuggestions.isNotEmpty(),
        searching = searching,
    )
    val searchActions = searchFieldActions(
        expanded = expanded,
        searching = searching,
        locating = locating,
        searchText = searchText,
    )
    val trailingIcon: (@Composable () -> Unit)? =
        if (searchActions.hasAny) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchActions.showProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    if (searchActions.showClear) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Clear search text")
                        }
                    }
                    if (searchActions.showClose) {
                        IconButton(onClick = onCloseSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        }
                    }
                }
            }
        } else {
            null
        }

    Column(
        modifier = modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = if (searchFocused) 0.28f else 0.14f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = {
                    Text(
                        mapSearchPlaceholder(
                            hasPlace = hasPlaceContext,
                            editingSelectedPlace = editingSelectedPlace,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = trailingIcon,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        searchFocused = focusState.isFocused
                        if (focusState.isFocused) onExpandedChange(true)
                },
            )
        }

        if (hasSavedPlace) {
            PlaceModeSelector(
                placeLabel = modePlaceLabel,
                addingNewPlace = addingNewPlace,
                editingSelectedPlace = editingSelectedPlace,
                onEditSelectedMode = onEditSelectedMode,
                onCreateMode = onCreateMode,
            )
        }

        AnimatedVisibility(
            visible = dropdownVisible,
        ) {
            MapSearchDropdown(
                searchText = searchText,
                searchResults = searchResults,
                searchSuggestions = searchSuggestions,
                searching = searching,
                searchingCurrentQuery = searchingCurrentQuery,
                hasSubmittedSearch = hasSubmittedSearch,
                locating = locating,
                onSubmit = onSubmit,
                onCloseSearch = onCloseSearch,
                onSelectResult = onSelectResult,
                onUseCurrentLocation = onUseCurrentLocation,
                onOpenSavedZones = onOpenSavedZones,
                editingSelectedPlace = editingSelectedPlace,
                currentLocationSelectsPlace = currentLocationSelectsPlace,
            )
        }
    }
}

@Composable
private fun PlaceModeSelector(
    placeLabel: String,
    addingNewPlace: Boolean,
    editingSelectedPlace: Boolean,
    onEditSelectedMode: () -> Unit,
    onCreateMode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = savedPlaceModeChipSelected(addingNewPlace),
            onClick = {
                if (savedPlaceModeChipCanSwitchToEdit(addingNewPlace)) {
                    onEditSelectedMode()
                }
            },
            leadingIcon = {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = {
                Text(
                    placeModePrimaryLabel(
                        placeLabel = placeLabel,
                        addingNewPlace = addingNewPlace,
                        editingSelectedPlace = editingSelectedPlace,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = addingNewPlace,
            onClick = onCreateMode,
            leadingIcon = {
                Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = {
                Text(
                    "Add place",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MapSearchDropdown(
    searchText: String,
    searchResults: List<LocationSearchResult>,
    searchSuggestions: List<LocationSearchResult>,
    searching: Boolean,
    searchingCurrentQuery: Boolean,
    hasSubmittedSearch: Boolean,
    locating: Boolean,
    onSubmit: () -> Unit,
    onCloseSearch: () -> Unit,
    onSelectResult: (LocationSearchResult) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenSavedZones: () -> Unit,
    editingSelectedPlace: Boolean,
    currentLocationSelectsPlace: Boolean,
) {
    val cleanedQuery = cleanSearchQuery(searchText)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 252.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Search",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Close search")
                }
            }
            when {
                cleanedQuery.isBlank() -> {
                    SearchSectionLabel("Quick actions")
                    SearchActionRow(
                        icon = Icons.Filled.MyLocation,
                        title = "Use current location",
                        subtitle = currentLocationActionSubtitle(
                            editingSelectedPlace = editingSelectedPlace,
                            selectsPlace = currentLocationSelectsPlace,
                        ),
                        loading = locating,
                        onClick = onUseCurrentLocation,
                    )
                    SearchActionRow(
                        icon = Icons.Filled.Bookmark,
                        title = "Saved places",
                        subtitle = "View and manage monitored places",
                        onClick = onOpenSavedZones,
                    )
                    SearchHintRow(
                        icon = Icons.Filled.Search,
                        title = "Search by place or address",
                        detail = "Type at least 3 characters.",
                    )
                }
                cleanedQuery.length < 3 -> {
                    SearchActionRow(
                        icon = Icons.Filled.MyLocation,
                        title = "Use current location",
                        subtitle = currentLocationActionSubtitle(
                            editingSelectedPlace = editingSelectedPlace,
                            selectsPlace = currentLocationSelectsPlace,
                            compact = true,
                        ),
                        loading = locating,
                        onClick = onUseCurrentLocation,
                    )
                    SearchHintRow(
                        icon = Icons.Filled.Search,
                        title = "Keep typing",
                        detail = "Enter at least 3 characters.",
                    )
                }
                searchingCurrentQuery -> {
                    SearchHintRow(
                        loading = true,
                        title = "Searching places",
                        detail = "Checking place results.",
                    )
                }
                searching -> {
                    SearchHintRow(
                        loading = true,
                        title = "Searching places",
                        detail = "Finishing current search.",
                    )
                }
                searchResults.isNotEmpty() -> {
                    SearchSectionLabel("Results")
                    searchResults.forEach { result ->
                        SearchRow(
                            icon = Icons.Filled.Place,
                            title = result.label.substringBefore(","),
                            subtitle = result.label,
                            onClick = { onSelectResult(result) },
                        )
                    }
                }
                hasSubmittedSearch -> {
                    SearchHintRow(
                        icon = Icons.Filled.Place,
                        title = "No places found",
                        detail = noSearchResultsDetail(
                            editingSelectedPlace = editingSelectedPlace,
                            currentLocationSelectsPlace = currentLocationSelectsPlace,
                        ),
                    )
                    SearchActionRow(
                        icon = Icons.Filled.MyLocation,
                        title = "Use current location",
                        subtitle = currentLocationActionSubtitle(
                            editingSelectedPlace = editingSelectedPlace,
                            selectsPlace = currentLocationSelectsPlace,
                            compact = true,
                        ),
                        loading = locating,
                        onClick = onUseCurrentLocation,
                    )
                }
                else -> {
                    SearchRow(
                        icon = Icons.Filled.Search,
                        title = "Search \"$cleanedQuery\"",
                        subtitle = "Find places and addresses",
                        onClick = onSubmit,
                    )
                    if (searchSuggestions.isNotEmpty()) {
                        SearchSectionLabel("Recent")
                    }
                    searchSuggestions.forEach { result ->
                        SearchRow(
                            icon = Icons.Filled.Place,
                            title = result.label.substringBefore(","),
                            subtitle = result.label,
                            onClick = { onSelectResult(result) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapActionRail(
    locating: Boolean,
    currentLocationContentDescription: String,
    onCurrentLocation: () -> Unit,
    onSavedZones: () -> Unit,
    onInsights: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(end = 12.dp, bottom = 112.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MapRailButton(
                icon = Icons.Filled.MyLocation,
                contentDescription = currentLocationContentDescription,
                loading = locating,
                emphasized = true,
                onClick = onCurrentLocation,
            )
            MapRailButton(
                icon = Icons.Filled.Bookmark,
                contentDescription = "Places",
                onClick = onSavedZones,
            )
            MapRailButton(
                icon = Icons.Filled.Insights,
                contentDescription = "Insights",
                onClick = onInsights,
            )
            MapRailButton(
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun MapRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    emphasized: Boolean = false,
) {
    val container = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
    }
    val content = if (emphasized) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = container,
    ) {
        IconButton(
            onClick = onClick,
            enabled = !loading,
            modifier = Modifier
                .size(42.dp)
                .semantics { this.contentDescription = contentDescription },
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AttributionPill(
    label: String,
    dockExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = if (dockExpanded) 398.dp else 106.dp
    Surface(
        modifier = modifier.padding(start = 10.dp, bottom = bottomPadding),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeStatusDock(
    statusTitle: String,
    statusDetail: String,
    placeLabel: String,
    hasPlace: Boolean,
    placeName: String,
    placeNameFallback: String,
    radius: Float,
    monitoredRadiusLimit: Float?,
    durationText: String,
    durationMinutes: Int,
    timerActive: Boolean,
    activePlaceArmed: Boolean,
    activePlaceRegistered: Boolean,
    activePlaceNeedsSetup: Boolean,
    activePlaceAutoStart: Boolean,
    promptState: HomePromptState?,
    viewingSavedPlaceReadOnly: Boolean,
    pendingPlacePreview: Boolean,
    pendingPlaceMove: Boolean,
    pendingPlaceActionLabel: String,
    armedPlaceCount: Int,
    livePlaceCount: Int,
    monitoringError: String,
    setupNotice: String,
    batteryWarning: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onEditPlaceClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
    onPlaceNameChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onDurationPreset: (Double) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onArmClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = when {
        promptState != null -> MaterialTheme.colorScheme.primary
        pendingPlacePreview -> MaterialTheme.colorScheme.primary
        timerActive -> MaterialTheme.colorScheme.primary
        activePlaceNeedsSetup || setupNotice.isNotBlank() -> MaterialTheme.colorScheme.tertiary
        activePlaceArmed && activePlaceRegistered -> MaterialTheme.colorScheme.secondary
        hasPlace -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val durationError = durationInputError(durationText).takeIf {
        hasPlace &&
            promptState == null &&
            !timerActive &&
            !viewingSavedPlaceReadOnly
    }
    val primaryBlockedByDuration =
        durationError != null &&
            primarySetupActionBlockedByDuration(
                durationText = durationText,
                pendingPlacePreview = pendingPlacePreview,
                activePlaceArmed = activePlaceArmed,
            )
    val timerBlockedByDuration =
        durationError != null &&
            secondaryTimerActionBlockedByDuration(
                durationText = durationText,
                pendingPlacePreview = pendingPlacePreview,
            )
    val monitoringNeedsSetup = setupNotice.isNotBlank() || activePlaceNeedsSetup
    val monitorActionLabel = homeMonitorActionLabel(
        monitoringNeedsSetup = monitoringNeedsSetup,
        activePlaceArmed = activePlaceArmed,
    )
    val primaryLabel = when {
        promptState != null -> promptState.primaryLabel
        !expanded && primaryBlockedByDuration -> durationFixActionLabel()
        pendingPlacePreview -> pendingPlaceActionLabel.ifBlank {
            pendingPlacePrimaryActionLabel(
                editingSelectedPlace = pendingPlaceMove,
                targetLabel = "",
            )
        }
        timerActive -> "+30m"
        !hasPlace -> "Use current"
        else -> monitorActionLabel
    }
    val primaryIcon = when {
        promptState?.kind == HomePromptKind.LeaveEarly -> Icons.Filled.CheckCircle
        promptState?.kind == HomePromptKind.TimeUp -> Icons.Filled.AccessTime
        promptState != null -> Icons.Filled.PlayArrow
        !expanded && primaryBlockedByDuration -> Icons.Filled.Info
        pendingPlacePreview -> Icons.Filled.CheckCircle
        timerActive -> Icons.Filled.AccessTime
        !hasPlace -> Icons.Filled.MyLocation
        monitoringNeedsSetup -> Icons.Filled.Info
        activePlaceArmed -> Icons.Filled.Cancel
        else -> Icons.Filled.NotificationsActive
    }
    val secondaryLabel = homeDockSecondaryActionLabel(
        promptSecondaryLabel = promptState?.secondaryLabel,
        timerActive = timerActive,
        pendingPlacePreview = pendingPlacePreview,
        pendingPlaceMove = pendingPlaceMove,
    )
    val secondaryIcon = when {
        promptState?.kind == HomePromptKind.TimeUp -> Icons.Filled.CheckCircle
        promptState != null -> Icons.Filled.Cancel
        timerActive || pendingPlacePreview -> Icons.Filled.Cancel
        else -> Icons.Filled.Bookmark
    }
    val liveText = homeDockMonitoringMetaText(
        monitoredCount = armedPlaceCount,
        liveCount = livePlaceCount,
    )
    val stateLabel = when {
        promptState?.kind == HomePromptKind.SwitchPlace -> "Switch"
        promptState?.kind == HomePromptKind.LeaveEarly -> "Leaving"
        promptState?.kind == HomePromptKind.TimeUp -> "Done"
        promptState != null -> "Arrived"
        pendingPlacePreview -> if (pendingPlaceMove) "Unsaved move" else "Unsaved"
        timerActive -> "Active"
        activePlaceNeedsSetup || setupNotice.isNotBlank() -> "Needs setup"
        activePlaceArmed && activePlaceRegistered -> "Live"
        hasPlace -> "Ready"
        else -> "Setup"
    }
    val swipeThresholdPx = with(LocalDensity.current) { 24.dp.toPx() }

    fun Modifier.dockSwipeTarget(enabled: Boolean = true): Modifier =
        if (!enabled) {
            this
        } else {
            pointerInput(expanded, swipeThresholdPx) {
        var totalDrag = 0f
        detectVerticalDragGestures(
            onDragStart = { totalDrag = 0f },
            onVerticalDrag = { change, dragAmount ->
                totalDrag += dragAmount
                val isIntendedDirection = if (expanded) dragAmount > 0f else dragAmount < 0f
                if (isIntendedDirection) {
                    change.consume()
                }
            },
            onDragEnd = {
                when {
                    totalDrag <= -swipeThresholdPx -> onExpandedChange(true)
                    totalDrag >= swipeThresholdPx -> onExpandedChange(false)
                }
                totalDrag = 0f
            },
            onDragCancel = {
                totalDrag = 0f
            },
        )
            }
    }

    Surface(
        modifier = modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .dockSwipeTarget(enabled = !expanded)
            .navigationBarsPadding()
            .heightIn(
                min = if (expanded) 0.dp else 78.dp,
                max = if (expanded) 460.dp else 92.dp,
            )
            .animateContentSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (expanded) 6.dp else 4.dp,
        shadowElevation = if (expanded) 10.dp else 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (expanded) 0.14f else 0.10f),
        ),
    ) {
        val contentModifier = if (expanded) {
            Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier
                .dockSwipeTarget()
                .padding(horizontal = 9.dp, vertical = 7.dp)
        }
        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(if (expanded) 10.dp else 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .semantics {
                        contentDescription = if (expanded) {
                            "Swipe down to collapse"
                        } else {
                            "Swipe up to expand"
                        }
                    }
                    .dockSwipeTarget(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dockSwipeTarget(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DockStatusOrb(
                    color = tone,
                    modifier = Modifier.clickable { onExpandedChange(!expanded) },
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onExpandedChange(!expanded) }
                        .padding(vertical = 3.dp),
                ) {
                    Text(
                        statusTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        when {
                            promptState != null -> statusDetail
                            timerActive -> statusDetail
                            !expanded && durationError != null -> durationFixCollapsedDetail(placeLabel)
                            !expanded && hasPlace -> "$stateLabel | ${arrivalModeLabel(activePlaceAutoStart)} | ${Notifications.formatDuration(durationMinutes)} | ${radius.roundToInt()} m | $placeLabel"
                            else -> statusDetail
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                DockActionButton(
                    icon = primaryIcon,
                    label = primaryLabel,
                    contentDescription = primaryLabel,
                    onClick = if (primaryBlockedByDuration) {
                        { onExpandedChange(true) }
                    } else {
                        onPrimaryAction
                    },
                    emphasized = true,
                    color = tone,
                )
                DockActionButton(
                    icon = secondaryIcon,
                    contentDescription = secondaryLabel,
                    onClick = onSecondaryAction,
                )
                DockActionButton(
                    icon = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse status" else "Expand status",
                    onClick = { onExpandedChange(!expanded) },
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DockStatePill(
                            text = stateLabel,
                            color = tone,
                        )
                        if (hasPlace) {
                            Text(
                                placeLabel,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DockMetaChip(
                            icon = Icons.Filled.Place,
                            text = "${radius.roundToInt()} m",
                            modifier = Modifier.weight(1f),
                        )
                        DockMetaChip(
                            icon = Icons.Filled.AccessTime,
                            text = Notifications.formatDuration(durationMinutes),
                            modifier = Modifier.weight(1f),
                        )
                        DockMetaChip(
                            icon = Icons.Filled.NotificationsActive,
                            text = liveText,
                            modifier = Modifier.weight(1f),
                        )
                    }

	                    if (
	                        promptState != null ||
	                        pendingPlacePreview ||
	                        timerActive ||
	                        setupNotice.isNotBlank() ||
	                        viewingSavedPlaceReadOnly
	                    ) {
	                        DockNoticeStrip(
	                            icon = when {
	                                promptState?.kind == HomePromptKind.LeaveEarly -> Icons.Filled.Timer
	                                promptState != null -> Icons.Filled.NotificationsActive
	                                pendingPlacePreview -> Icons.Filled.Place
	                                timerActive -> Icons.Filled.Timer
	                                viewingSavedPlaceReadOnly -> Icons.Filled.Map
	                                else -> Icons.Filled.Info
	                            },
	                            title = when {
	                                promptState?.kind == HomePromptKind.SwitchPlace -> "Place conflict"
	                                promptState?.kind == HomePromptKind.LeaveEarly -> "Leaving early"
	                                promptState != null -> "Arrival check"
	                                pendingPlacePreview -> if (pendingPlaceMove) "Move not saved" else "Not saved yet"
	                                timerActive -> "Timer is running"
	                                viewingSavedPlaceReadOnly -> "Viewing only"
	                                else -> "Monitoring needs setup"
	                            },
	                            detail = when {
	                                promptState != null -> promptState.detail
	                                pendingPlacePreview -> statusDetail
	                                timerActive -> statusDetail
	                                viewingSavedPlaceReadOnly -> viewingOnlyNoticeDetail(placeLabel)
	                                else -> setupNotice
	                            },
	                            color = tone,
                        )
                    }

                    if (batteryWarning.isNotBlank()) {
                        DockNoticeStrip(
                            icon = Icons.Filled.Info,
                            title = "Battery reliability",
                            detail = batteryWarning,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    if (hasPlace) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

                        if (promptState != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onPrimaryAction,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                ) {
                                    Icon(primaryIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        promptState.primaryLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onSecondaryAction,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                ) {
                                    Icon(secondaryIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        promptState.secondaryLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else if (timerActive) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onPrimaryAction,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                ) {
                                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "+30m",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onSecondaryAction,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                ) {
                                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Cancel timer",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
	                        } else if (viewingSavedPlaceReadOnly) {
	                            Button(
	                                onClick = onEditPlaceClick,
	                                modifier = Modifier.fillMaxWidth(),
	                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
	                            ) {
	                                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
	                                Spacer(Modifier.width(8.dp))
	                                Text(
	                                    "Edit settings",
	                                    maxLines = 1,
	                                    overflow = TextOverflow.Ellipsis,
	                                )
	                            }
	                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
	                                FilledTonalButton(
	                                    onClick = onArmClick,
	                                    modifier = Modifier.weight(1f),
	                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
	                                ) {
	                                    Icon(
	                                        when {
	                                            monitoringNeedsSetup -> Icons.Filled.Info
	                                            activePlaceArmed -> Icons.Filled.Cancel
	                                            else -> Icons.Filled.NotificationsActive
	                                        },
	                                        contentDescription = null,
	                                        modifier = Modifier.size(18.dp),
	                                    )
	                                    Spacer(Modifier.width(8.dp))
	                                    Text(
	                                        monitorActionLabel,
	                                        maxLines = 1,
	                                        overflow = TextOverflow.Ellipsis,
	                                    )
	                                }
	                                FilledTonalButton(
	                                    onClick = onTimerClick,
	                                    modifier = Modifier.weight(1f),
	                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
	                                ) {
	                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
	                                    Spacer(Modifier.width(8.dp))
	                                    Text(
	                                        "Start now",
	                                        maxLines = 1,
	                                        overflow = TextOverflow.Ellipsis,
	                                    )
	                                }
	                            }
	                        } else {
	                            PlaceNameControl(
	                                placeName = placeName,
	                                fallbackLabel = placeNameFallback,
	                                onPlaceNameChange = onPlaceNameChange,
                            )

                            RadiusControl(
                                radius = radius,
                                monitoredRadiusLimit = monitoredRadiusLimit,
                                onRadiusChange = onRadiusChange,
                                onRadiusChangeFinished = onRadiusChangeFinished,
                            )

                            DurationControl(
                                durationText = durationText,
                                onDurationChange = onDurationChange,
                                onDurationPreset = onDurationPreset,
                            )

                            ArrivalModeControl(
                                autoStart = activePlaceAutoStart,
                                onAutoStartChange = onAutoStartChange,
                            )

	                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
	                                Button(
	                                    onClick = if (pendingPlacePreview) onPrimaryAction else onArmClick,
	                                    enabled = !primaryBlockedByDuration,
	                                    modifier = Modifier.weight(1f),
	                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
	                                ) {
                                    Icon(
                                        when {
                                            pendingPlacePreview -> Icons.Filled.CheckCircle
                                            monitoringNeedsSetup -> Icons.Filled.Info
                                            activePlaceArmed -> Icons.Filled.Cancel
                                            else -> Icons.Filled.NotificationsActive
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        when {
                                            pendingPlacePreview -> pendingPlaceActionLabel.ifBlank {
                                                pendingPlacePrimaryActionLabel(
                                                    editingSelectedPlace = pendingPlaceMove,
                                                    targetLabel = "",
                                                )
                                            }
                                            else -> monitorActionLabel
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

	                                FilledTonalButton(
	                                    onClick = if (pendingPlacePreview) onSecondaryAction else onTimerClick,
	                                    enabled = !timerBlockedByDuration,
	                                    modifier = Modifier.weight(1f),
	                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
	                                ) {
                                    Icon(
                                        when {
                                            pendingPlacePreview -> Icons.Filled.Cancel
                                            timerActive -> Icons.Filled.Cancel
                                            else -> Icons.Filled.PlayArrow
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        when {
                                            pendingPlacePreview -> placesPreviewDiscardActionLabel(
                                                editingSelectedPlace = pendingPlaceMove,
                                            )
                                            timerActive -> "Cancel timer"
                                            else -> "Start now"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    if (!timerActive && activePlaceNeedsSetup && monitoringError.isNotBlank()) {
                        Text(
                            monitoringError,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String? = null,
    contentDescription: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    val container = if (emphasized) {
        color
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    val content = if (emphasized) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = container,
        tonalElevation = if (emphasized) 3.dp else 1.dp,
    ) {
        if (label != null) {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 82.dp, max = 116.dp)
                    .clickable(onClick = onClick)
                    .semantics { this.contentDescription = contentDescription }
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { this.contentDescription = contentDescription },
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun DockStatusOrb(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun DockNoticeStrip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = color,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    detail,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DockStatePill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DockMetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SearchSectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 14.dp, top = 7.dp, end = 14.dp, bottom = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SearchHintRow(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Search,
    loading: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(17.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(7.dp)
                        .size(17.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(17.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(7.dp)
                    .size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.ifBlank { "Place" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusDot(
    timerActive: Boolean,
    armed: Boolean,
) {
    val tone = when {
        timerActive -> MaterialTheme.colorScheme.primary
        armed -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(tone),
    )
}

@Composable
private fun PlaceNameControl(
    placeName: String,
    fallbackLabel: String,
    onPlaceNameChange: (String) -> Unit,
) {
    val helperText = placeNameSupportingText(placeName, fallbackLabel)
    OutlinedTextField(
        value = placeName,
        onValueChange = { onPlaceNameChange(placeNameInputValue(it)) },
        label = { Text("Place name") },
        singleLine = true,
        supportingText = helperText?.let { text ->
            { Text(text) }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        leadingIcon = {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RadiusControl(
    radius: Float,
    monitoredRadiusLimit: Float? = null,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
) {
    val state = radiusControlState(radius, monitoredRadiusLimit)
    val presets = radiusPresetOptions(
        radiusMeters = state.valueMeters,
        maxMeters = state.maxMeters,
        controlsEnabled = state.sliderEnabled,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Radius",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "${state.valueMeters.roundToInt()} m",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = preset.selected,
                    enabled = preset.enabled,
                    onClick = {
                        onRadiusChange(preset.meters)
                        onRadiusChangeFinished()
                    },
                    label = { Text(preset.label) },
                )
            }
        }
        Slider(
            value = state.valueMeters,
            onValueChange = { onRadiusChange(it.coerceAtMost(state.maxMeters)) },
            onValueChangeFinished = onRadiusChangeFinished,
            valueRange = DwellRadius.MIN_METERS..state.maxMeters,
            enabled = state.sliderEnabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        state.helperText?.let { helper ->
            Text(
                helper,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationControl(
    durationText: String,
    onDurationChange: (String) -> Unit,
    onDurationPreset: (Double) -> Unit,
) {
    val durationError = durationInputError(durationText)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Timer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            durationPresetOptions(durationText).forEach { preset ->
                FilterChip(
                    selected = preset.selected,
                    onClick = { onDurationPreset(preset.hours) },
                    label = { Text(preset.label) },
                )
            }
        }

        OutlinedTextField(
            value = durationText,
            onValueChange = onDurationChange,
            label = { Text("Duration in hours") },
            singleLine = true,
            isError = durationError != null,
            supportingText = {
                Text(durationError ?: "Use decimals for partial hours, like 4.5.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArrivalModeControl(
    autoStart: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
) {
    val options = arrivalModeOptions(autoStart)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Arrival mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    options.first { it.selected }.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.selected,
                    onClick = { onAutoStartChange(option.autoStart) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    radius: Float,
    durationText: String,
    autoStart: Boolean,
    accountProvider: String,
    accountDisplayName: String,
    accountEmail: String,
    locationGranted: Boolean,
    backgroundGranted: Boolean,
    notificationsGranted: Boolean,
    motionGranted: Boolean,
    exactAlarmAllowed: Boolean,
    batteryReliabilityStatus: BatteryReliabilityStatus,
    diagnosticsEntries: List<DwellDiagnosticEntry>,
    onBack: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
	    onDurationChange: (String) -> Unit,
	    onDurationPreset: (Double) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
	    onOpenAppSettings: () -> Unit,
	    onOpenExactAlarmSettings: () -> Unit,
	    onOpenBatterySettings: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onClearMapCache: () -> Unit,
    onClearSearchCache: () -> Unit,
    onOpenTutorial: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAppData: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var confirmDeleteData by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    val signedInWithGoogle = accountProvider == "google"
    val accountTitle = if (signedInWithGoogle) "Google account" else "Local session"
    val accountDetail = when {
        accountEmail.isNotBlank() -> accountEmail
        accountDisplayName.isNotBlank() -> accountDisplayName
        signedInWithGoogle -> "Signed in with Google"
        else -> "Device-only session"
    }
    val accountBadge = if (signedInWithGoogle) "Google" else "Local"
    val permissionButtons = setupChecksPermissionButtons(
        OnboardingPermissionStatus(
            locationGranted = locationGranted,
            backgroundGranted = backgroundGranted,
            notificationsGranted = notificationsGranted,
            motionGranted = motionGranted,
        ),
        exactAlarmAllowed = exactAlarmAllowed,
    )

    BackHandler(enabled = !confirmDeleteData && !confirmDeleteAccount) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Settings",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onSignOut) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                }
            },
        )

        SettingsSection(title = "Account") {
            SettingsRow(
                icon = Icons.Filled.AccountCircle,
                title = accountTitle,
                detail = accountDetail,
                trailing = {
                    Text(
                        accountBadge,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }

        SettingsSection(title = "Help") {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "How to use Dwell",
                detail = "Setup, places, monitoring, editing, and watch states in one short guide.",
            )
            OutlinedButton(
                onClick = onOpenTutorial,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open tutorial")
            }
        }

        SettingsSection(title = "Data controls") {
            SettingsRow(
                icon = Icons.Filled.Delete,
                title = "Delete app data",
                detail = "Removes saved places and analytics from this install, while keeping the session.",
            )
            OutlinedButton(
                onClick = { confirmDeleteData = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete app data")
            }
            SettingsRow(
                icon = Icons.Filled.AccountCircle,
                title = "Delete account",
                detail = "Deletes account/session data, clears this app, and signs you out.",
            )
            OutlinedButton(
                onClick = { confirmDeleteAccount = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete account")
            }
        }

        SettingsSection(title = "Maps") {
            SettingsRow(
                icon = Icons.Filled.Map,
                title = "Map cache",
                detail = "Keeps recently viewed tiles on this device, up to about 128 MB.",
            )
            OutlinedButton(
                onClick = onClearMapCache,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear map cache")
            }
            SettingsRow(
                icon = Icons.Filled.Search,
                title = "Place search cache",
                detail = "Keeps recent search results for 30 minutes to reduce public API requests.",
            )
            OutlinedButton(
                onClick = onClearSearchCache,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear search cache")
            }
        }

        SettingsSection(title = "New place defaults") {
            RadiusControl(
                radius = radius,
                onRadiusChange = onRadiusChange,
                onRadiusChangeFinished = onRadiusChangeFinished,
            )
            DurationControl(
                durationText = durationText,
                onDurationChange = onDurationChange,
                onDurationPreset = onDurationPreset,
            )
            ArrivalModeControl(
                autoStart = autoStart,
                onAutoStartChange = onAutoStartChange,
            )
        }

        SettingsSection(title = "Permissions") {
            PermissionRow("Location", locationGranted)
	            PermissionRow("Background location", backgroundGranted)
	            PermissionRow("Notifications", notificationsGranted)
	            PermissionRow("Physical activity", motionGranted)
	            PermissionRow("Exact alarms", exactAlarmAllowed)
	            permissionButtons.forEach { button ->
	                OutlinedButton(
	                    onClick = when (button.action) {
	                        SetupCheckPermissionAction.Permissions -> onOpenAppSettings
	                        SetupCheckPermissionAction.ExactAlarm -> onOpenExactAlarmSettings
	                    },
	                    modifier = Modifier.fillMaxWidth(),
	                ) {
	                    Icon(
	                        if (button.action == SetupCheckPermissionAction.ExactAlarm) {
	                            Icons.Filled.AccessTime
	                        } else {
	                            Icons.Filled.Settings
	                        },
	                        contentDescription = null,
	                        modifier = Modifier.size(18.dp),
	                    )
	                    Spacer(Modifier.width(8.dp))
	                    Text(button.label)
	                }
            }
        }

        SettingsSection(title = "Reliability") {
            SettingsRow(
                icon = Icons.Filled.NotificationsActive,
                title = "Battery background access",
                detail = batteryReliabilityStatus.detail,
                trailing = {
                    Text(
                        batteryReliabilityStatus.label,
                        color = if (batteryReliabilityStatus.isIgnoringOptimizations) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (batteryReliabilityStatus.isIgnoringOptimizations) {
                        "Review battery settings"
                    } else {
                        batteryHelpActionLabel()
                    }
                )
            }
        }

        SettingsSection(title = "Diagnostics") {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Arrival engine",
                detail = "Local field-test log with no coordinates",
            )
            if (diagnosticsEntries.isEmpty()) {
                Text(
                    "No recent decisions",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                diagnosticsEntries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        entry.label(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            OutlinedButton(
                onClick = onCopyDiagnostics,
                enabled = diagnosticsEntries.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy diagnostics")
            }
            OutlinedButton(
                onClick = onClearDiagnostics,
                enabled = diagnosticsEntries.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear diagnostics")
            }
        }

        SettingsSection(title = "Watch") {
            SettingsRow(
                icon = Icons.Filled.Watch,
                title = "Wear sync",
                detail = "Phone owns arrival detection; watch mirrors the timer",
            )
        }
    }

    if (confirmDeleteData) {
        AlertDialog(
            onDismissRequest = { confirmDeleteData = false },
            title = { Text("Delete app data?") },
            text = {
                Text("Saved places, timer defaults, and analytics for this install will be deleted. Your session stays signed in.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteData = false
                        onDeleteAppData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Delete data")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteData = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            title = { Text("Delete account?") },
            text = {
                Text("This deletes account/session data and associated app data, clears this device, and signs you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteAccount = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Delete account")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAccount = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SetupChecksScreen(
    permissionStatus: OnboardingPermissionStatus,
    exactAlarmAllowed: Boolean,
    batteryReliabilityStatus: BatteryReliabilityStatus,
    onBack: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    val permissionButtons = setupChecksPermissionButtons(
        permissionStatus = permissionStatus,
        exactAlarmAllowed = exactAlarmAllowed,
    )
    val batteryActionLabel = setupChecksBatteryActionLabel(batteryReliabilityStatus)

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Setup checks",
            onBack = onBack,
        )

        SettingsSection(title = "Status") {
            SettingsRow(
                icon = if (
                    permissionStatus.allMajorGranted &&
                    exactAlarmAllowed &&
                    !batteryNeedsReliabilityReview(batteryReliabilityStatus)
                ) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Info
                },
                title = "Background monitoring",
                detail = setupChecksIntroDetail(
                    permissionStatus = permissionStatus,
                    exactAlarmAllowed = exactAlarmAllowed,
                    batteryReliabilityStatus = batteryReliabilityStatus,
                ),
            )
        }

        SettingsSection(title = "Permissions") {
            PermissionRow("Location", permissionStatus.locationGranted)
            PermissionRow("Background location", permissionStatus.backgroundGranted)
            PermissionRow("Notifications", permissionStatus.notificationsGranted)
            PermissionRow("Physical activity", permissionStatus.motionGranted)
            PermissionRow("Exact alarms", exactAlarmAllowed)
            permissionButtons.forEach { button ->
                OutlinedButton(
                    onClick = when (button.action) {
                        SetupCheckPermissionAction.Permissions -> onOpenAppSettings
                        SetupCheckPermissionAction.ExactAlarm -> onOpenExactAlarmSettings
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (button.action == SetupCheckPermissionAction.ExactAlarm) {
                            Icons.Filled.AccessTime
                        } else {
                            Icons.Filled.Settings
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(button.label)
                }
            }
        }

        SettingsSection(title = "Battery") {
            SettingsRow(
                icon = Icons.Filled.NotificationsActive,
                title = "Background access",
                detail = batteryReliabilityStatus.detail,
                trailing = {
                    Text(
                        batteryReliabilityStatus.label,
                        color = if (batteryReliabilityStatus.isIgnoringOptimizations) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
            if (batteryActionLabel != null) {
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(batteryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun TutorialScreen(
    onBack: () -> Unit,
    onAddPlace: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "How to use Dwell",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onOpenPlaces) {
                    Icon(Icons.Filled.Bookmark, contentDescription = "Places")
                }
            },
        )

        SettingsSection(title = "Main Flow") {
            appTutorialFlowSteps().forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider()
                SettingsRow(
                    icon = when (index) {
                        0 -> Icons.Filled.CheckCircle
                        1 -> Icons.Filled.Map
                        2 -> Icons.Filled.Place
                        3 -> Icons.Filled.NotificationsActive
                        4 -> Icons.Filled.Bookmark
                        5 -> Icons.Filled.Settings
                        6 -> Icons.Filled.Timer
                        7 -> Icons.Filled.Bookmark
                        else -> Icons.Filled.Watch
                    },
                    title = step.title,
                    detail = step.detail,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddPlace,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add place", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = onOpenPlaces,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Places", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        SettingsSection(title = "Pick The Spot") {
            appTutorialPickPlaceSteps().forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider()
                SettingsRow(
                    icon = when (index) {
                        0 -> Icons.Filled.Search
                        1 -> Icons.Filled.MyLocation
                        else -> Icons.Filled.Place
                    },
                    title = step.title,
                    detail = step.detail,
                )
            }
        }

        SettingsSection(title = "Example: Home, Office, Gym") {
            appTutorialExampleSteps().forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider()
                SettingsRow(
                    icon = when (index) {
                        0 -> Icons.Filled.Home
                        1 -> Icons.Filled.Business
                        2 -> Icons.Filled.FitnessCenter
                        else -> Icons.Filled.NotificationsActive
                    },
                    title = step.title,
                    detail = step.detail,
                )
            }
        }

        SettingsSection(title = "Multiple Places") {
            appTutorialMultiplePlaceRules().forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider()
                SettingsRow(
                    icon = when (index) {
                        0 -> Icons.Filled.NotificationsActive
                        1 -> Icons.Filled.Settings
                        else -> Icons.Filled.Timer
                    },
                    title = step.title,
                    detail = step.detail,
                )
            }
        }

        SettingsSection(title = "If You Get Stuck") {
            appTutorialStuckStateSteps().forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider()
                SettingsRow(
                    icon = when (index) {
                        0 -> Icons.Filled.Place
                        1 -> Icons.Filled.Search
                        2 -> Icons.Filled.Bookmark
                        3 -> Icons.Filled.Settings
                        else -> Icons.Filled.NotificationsActive
                    },
                    title = step.title,
                    detail = step.detail,
                )
            }
        }

        SettingsSection(title = "Place States") {
            SettingsRow(
                icon = Icons.Filled.Place,
                title = "Unsaved",
                detail = unsavedRuntimeActionsBlockedDetail(),
            )
            HorizontalDivider()
            SettingsRow(
                icon = Icons.Filled.Map,
                title = "Viewing",
                detail = "Read-only map check. Use Edit settings when a saved place should change.",
            )
            HorizontalDivider()
            SettingsRow(
                icon = Icons.Filled.Settings,
                title = "Editing",
                detail = "Only the row marked Editing receives name, radius, timer, mode, or move changes.",
            )
        }

        SettingsSection(title = "Arrival And Watch") {
            SettingsRow(
                icon = Icons.Filled.Timer,
                title = "Timer owner",
                detail = "Prompts, notifications, watch app, and Tile should all name the same place.",
            )
            HorizontalDivider()
            SettingsRow(
                icon = Icons.Filled.Watch,
                title = "Watch state",
                detail = "The watch mirrors setup, live monitoring count, prompts, timer, and time-up alerts.",
            )
        }

        SettingsSection(title = "Recovery") {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Needs setup",
                detail = "Use Finish setup when background location, notifications, alarms, or battery are blocking monitoring.",
            )
            OutlinedButton(
                onClick = onOpenSetup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(monitoringSetupActionLabel())
            }
        }
    }
}

@Composable
private fun InsightsScreen(
    summary: DwellInsightsSummary,
    onBack: () -> Unit,
    onOpenPlaces: () -> Unit,
) {
    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Insights",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onOpenPlaces) {
                    Icon(Icons.Filled.Bookmark, contentDescription = "Places")
                }
            },
        )

        if (summary.recentSessions.isEmpty()) {
            EmptyState(
                title = "No sessions yet",
                detail = "Complete a place timer and Dwell will show weekly time, sessions, and place streaks here.",
                actionLabel = "Open places",
                onAction = onOpenPlaces,
            )
            return@Column
        }

        InsightHeroCard(summary)

        summary.bestPlace?.let { best ->
            DockNoticeStrip(
                icon = Icons.Filled.Insights,
                title = "Top place this week",
                detail = "${formatInsightMinutes(best.weekMinutes)} at ${best.label} across ${best.completedSessionsThisWeek} ${sessionWord(best.completedSessionsThisWeek)}.",
                color = MaterialTheme.colorScheme.primary,
            )
        }

        SettingsSection(title = "Place streaks") {
            if (summary.placeInsights.isEmpty()) {
                Text(
                    "Completed timers will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                summary.placeInsights.forEachIndexed { index, place ->
                    if (index > 0) HorizontalDivider()
                    PlaceInsightRow(place)
                }
            }
        }

        SettingsSection(title = "Recent sessions") {
            summary.recentSessions.forEachIndexed { index, session ->
                if (index > 0) HorizontalDivider()
                SessionHistoryRow(session)
            }
        }
    }
}

@Composable
private fun InsightHeroCard(summary: DwellInsightsSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                ) {
                    Icon(
                        Icons.Filled.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "This week",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formatInsightMinutes(summary.weekMinutes),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightStatTile(
                    label = "Today",
                    value = formatInsightMinutes(summary.todayMinutes),
                    modifier = Modifier.weight(1f),
                )
                InsightStatTile(
                    label = "Sessions",
                    value = summary.completedSessionsThisWeek.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            WeeklyDwellBars(summary.dayInsights)
        }
    }
}

@Composable
private fun WeeklyDwellBars(dayInsights: List<DwellDayInsight>) {
    val maxMinutes = dayInsights.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Last 7 days",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "completed timers",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                dayInsights.forEach { day ->
                    val fraction = day.minutes.toFloat() / maxMinutes
                    val barHeight = (14f + 54f * fraction.coerceIn(0f, 1f)).dp
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(68.dp)
                                .width(14.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(barHeight)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (day.minutes > 0) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    ),
                            )
                        }
                        Text(
                            dayLabel(day.dayStartMillis),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaceInsightRow(place: DwellPlaceInsight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f),
        ) {
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                place.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${place.completedSessionsThisWeek} ${sessionWord(place.completedSessionsThisWeek)} this week - latest ${formatInsightDate(place.latestSessionAtMillis)}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatInsightMinutes(place.weekMinutes),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (place.todayMinutes > 0) {
                Text(
                    "${formatInsightMinutes(place.todayMinutes)} today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryRow(session: DwellSession) {
    val completed = session.outcome == DwellSessionOutcome.Completed
    val tone = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = tone.copy(alpha = 0.14f),
        ) {
            Icon(
                if (completed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = tone,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                session.placeLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${formatInsightDate(session.endedAtMillis)} - planned ${Notifications.formatDuration(session.plannedDurationMinutes)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatInsightMinutes(session.elapsedMinutes),
                color = tone,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (completed) "Done" else "Cancelled",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun formatInsightMinutes(minutes: Int): String =
    if (minutes <= 0) "0m" else Notifications.formatDuration(minutes)

private fun formatInsightDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

private fun dayLabel(millis: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))

private fun sessionWord(count: Int): String =
    if (count == 1) "session" else "sessions"

@Composable
private fun SavedZonesScreen(
    places: List<DwellPlace>,
    registeredPlaceIds: Set<String>,
    monitoringError: String,
    monitoringHealth: MonitoringHealth,
    viewingPlaceId: String,
    editingPlaceId: String,
    timerPlaceId: String,
    timerPlaceLabel: String,
    timerActive: Boolean,
    pendingPlacePreview: PendingPlacePreview?,
	    onBack: () -> Unit,
	    onOpenMonitoringSetup: () -> Unit,
    onRefreshMonitoring: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onReturnToPreview: () -> Unit,
    onDiscardPreview: () -> Unit,
    onCreateZone: () -> Unit,
    onViewPlace: (DwellPlace) -> Unit,
    onEditPlace: (DwellPlace) -> Unit,
    onStartPlaceTimer: (DwellPlace) -> Unit,
    onCancelPlaceTimer: (DwellPlace) -> Unit,
    onToggleMonitoring: (DwellPlace, Boolean) -> Unit,
    onDeletePlace: (DwellPlace) -> Unit,
) {
    var pendingMonitorPlace by remember { mutableStateOf<DwellPlace?>(null) }
    var pendingDeletePlace by remember { mutableStateOf<DwellPlace?>(null) }
    val hasPendingPreview = pendingPlacePreview != null
    val placesPrimaryAction = if (hasPendingPreview) onReturnToPreview else onCreateZone
    val pendingMonitorDialogPlace = latestDialogPlace(pendingMonitorPlace?.id, places)
    val pendingDeleteDialogPlace = latestDialogPlace(pendingDeletePlace?.id, places)

    LaunchedEffect(pendingMonitorPlace?.id, pendingMonitorDialogPlace?.id) {
        if (pendingMonitorPlace != null && pendingMonitorDialogPlace == null) {
            pendingMonitorPlace = null
        }
    }

    LaunchedEffect(pendingDeletePlace?.id, pendingDeleteDialogPlace?.id) {
        if (pendingDeletePlace != null && pendingDeleteDialogPlace == null) {
            pendingDeletePlace = null
        }
    }

    fun handleBack() {
        when (
            placesBackAction(
                monitorDialogVisible = pendingMonitorDialogPlace != null,
                deleteDialogVisible = pendingDeleteDialogPlace != null,
            )
        ) {
            PlacesBackAction.DismissMonitorDialog -> pendingMonitorPlace = null
            PlacesBackAction.DismissDeleteDialog -> pendingDeletePlace = null
            PlacesBackAction.LeavePlaces -> onBack()
        }
    }

    BackHandler { handleBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Places",
            onBack = ::handleBack,
            trailing = {
                IconButton(onClick = placesPrimaryAction) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = placesAddActionLabel(hasPendingPreview),
                    )
                }
            },
        )

        pendingPlacePreview?.let { preview ->
            PendingPlacePreviewCard(
                editingSelectedPlace = preview.mode == PlaceSelectionMode.EditSelected,
                targetLabel = preview.targetPlaceLabel,
                onReturn = onReturnToPreview,
                onDiscard = onDiscardPreview,
            )
        }

        MonitoringHealthCard(
            health = monitoringHealth,
            actionEnabled = monitoringHealthActionEnabled(
                action = monitoringHealth.action,
                hasPendingPlacePreview = hasPendingPreview,
            ),
            actionDisabledDetail = monitoringHealthActionDisabledDetail(
                actionLabel = monitoringHealth.actionLabel,
                hasPendingPlacePreview = hasPendingPreview,
                editingSelectedPlace = pendingPlacePreview?.mode == PlaceSelectionMode.EditSelected,
            ),
            onAction = {
	                    when (monitoringHealth.action) {
	                    MonitoringHealthAction.OpenSettings -> onOpenMonitoringSetup()
                        MonitoringHealthAction.RefreshMonitoring -> onRefreshMonitoring()
	                    MonitoringHealthAction.OpenExactAlarm -> onOpenExactAlarmSettings()
	                    MonitoringHealthAction.OpenBattery -> onOpenBatterySettings()
	                    MonitoringHealthAction.None -> Unit
	                }
            },
        )

        if (places.isEmpty()) {
            val emptyState = placesEmptyStateCopy(hasPendingPreview)
            EmptyState(
                title = emptyState.title,
                detail = emptyState.detail,
                actionLabel = emptyState.actionLabel,
                onAction = placesPrimaryAction,
            )
        } else {
            PlacesSummaryRow(
                places = places,
                registeredPlaceIds = registeredPlaceIds,
                monitoringError = monitoringError,
            )
            val rowActionAvailability = placesRowActionAvailability(
                hasPendingPlacePreview = pendingPlacePreview != null,
                timerActive = timerActive,
                editingSelectedPlace = pendingPlacePreview?.mode == PlaceSelectionMode.EditSelected,
            )
            places.forEach { place ->
                PlaceRow(
                    place = place,
                    isViewing = place.id == viewingPlaceId,
                    isEditing = place.id == editingPlaceId,
                    isRegistered = registeredPlaceIds.contains(place.id),
	                    isTimerPlace = timerActive && place.id == timerPlaceId,
	                    actionAvailability = rowActionAvailability,
                    timerPlaceLabel = timerPlaceLabel,
		                    onView = { onViewPlace(place) },
		                    onEdit = { onEditPlace(place) },
		                    onStartNow = { onStartPlaceTimer(place) },
		                    onCancelTimer = { onCancelPlaceTimer(place) },
		                    onFixMonitoringSetup = onOpenMonitoringSetup,
		                    onToggleMonitoring = { enabled ->
                                if (enabled) {
                                    pendingMonitorPlace = place
                                } else {
                                    onToggleMonitoring(place, false)
                                }
                            },
		                    onDelete = { pendingDeletePlace = place },
		                )
            }
            OutlinedButton(onClick = placesPrimaryAction, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(placesAddActionLabel(hasPendingPreview))
            }
        }
    }

    pendingMonitorDialogPlace?.let { place ->
        val copy = placesMonitoringConfirmationCopy(place)
        AlertDialog(
            onDismissRequest = { pendingMonitorPlace = null },
            title = { Text(copy.title) },
            text = { Text(copy.detail) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingMonitorPlace = null
                        onToggleMonitoring(place, true)
                    },
                ) {
                    Text(copy.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMonitorPlace = null }) {
                    Text(copy.dismissLabel)
                }
            },
        )
    }

    pendingDeleteDialogPlace?.let { place ->
        val isTimerPlace = timerActive && place.id == timerPlaceId
        AlertDialog(
            onDismissRequest = { pendingDeletePlace = null },
            title = { Text(placeRemovalTitle(place.safeLabel)) },
            text = {
                Text(
                    placeRemovalDetail(
                        placeLabel = place.safeLabel,
                        monitoringEnabled = place.monitoringEnabled,
                        isTimerPlace = isTimerPlace,
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeletePlace = null
                        onDeletePlace(place)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Remove place")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlace = null }) {
                    Text("Keep place")
                }
            },
        )
    }
}

@Composable
private fun PendingPlacePreviewCard(
    editingSelectedPlace: Boolean,
    targetLabel: String,
    onReturn: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        placesPreviewBannerTitle(
                            editingSelectedPlace = editingSelectedPlace,
                            targetLabel = targetLabel,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        placesPreviewBannerDetail(editingSelectedPlace),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReturn,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Review on map", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        placesPreviewDiscardActionLabel(editingSelectedPlace),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitoringHealthCard(
    health: MonitoringHealth,
    actionEnabled: Boolean,
    actionDisabledDetail: String?,
    onAction: () -> Unit,
) {
	    val tone = when (health.action) {
	        MonitoringHealthAction.OpenBattery -> MaterialTheme.colorScheme.tertiary
	        MonitoringHealthAction.OpenExactAlarm -> MaterialTheme.colorScheme.tertiary
	        MonitoringHealthAction.OpenSettings -> MaterialTheme.colorScheme.tertiary
            MonitoringHealthAction.RefreshMonitoring -> MaterialTheme.colorScheme.tertiary
        MonitoringHealthAction.None -> if (health.healthy) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.outline
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = tone.copy(alpha = if (health.healthy) 0.12f else 0.10f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = tone.copy(alpha = 0.16f),
                ) {
                    Icon(
	                        when {
	                            health.healthy -> Icons.Filled.CheckCircle
	                            health.action == MonitoringHealthAction.OpenBattery -> Icons.Filled.NotificationsActive
	                            health.action == MonitoringHealthAction.OpenExactAlarm -> Icons.Filled.AccessTime
	                            else -> Icons.Filled.Info
	                        },
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            health.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        DockStatePill(
                            text = health.stateLabel,
                            color = tone,
                        )
                    }
                    Text(
                        health.detail,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (health.action != MonitoringHealthAction.None && health.actionLabel.isNotBlank()) {
                OutlinedButton(
                    onClick = onAction,
                    enabled = actionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(health.actionLabel)
                }
                if (!actionEnabled && !actionDisabledDetail.isNullOrBlank()) {
                    Text(
                        actionDisabledDetail,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlacesSummaryRow(
    places: List<DwellPlace>,
    registeredPlaceIds: Set<String>,
    monitoringError: String,
) {
    val monitored = places.count { it.monitoringEnabled }
    val live = places.count { it.monitoringEnabled && registeredPlaceIds.contains(it.id) }
    val needsSetup = (monitored - live).coerceAtLeast(0)
    val placeNamesText = placesSummaryPlaceNamesText(
        places = places,
        registeredPlaceIds = registeredPlaceIds,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Bookmark, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${places.size} saved ${if (places.size == 1) "place" else "places"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    placesSummaryStatusText(
                        monitoredCount = monitored,
                        liveCount = live,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                placeNamesText?.let { names ->
                    Text(
                        names,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (monitoringError.isNotBlank() && needsSetup > 0) {
                    Text(
                        monitoringError,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceRow(
    place: DwellPlace,
    isViewing: Boolean,
    isEditing: Boolean,
    isRegistered: Boolean,
    isTimerPlace: Boolean,
    actionAvailability: PlacesRowActionAvailability,
    timerPlaceLabel: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onStartNow: () -> Unit,
    onCancelTimer: () -> Unit,
    onFixMonitoringSetup: () -> Unit,
    onToggleMonitoring: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val rowEmphasized = isViewing || isEditing || isTimerPlace
    val monitoringNeedsSetup = placeNeedsMonitoringSetup(
        monitoringEnabled = place.monitoringEnabled,
        isRegistered = isRegistered,
    )
    val roleLabels = placeRoleLabels(
        isViewing = isViewing,
        isEditing = isEditing,
        isTimerPlace = isTimerPlace,
    )
    val timerAction = placesRowTimerAction(
        isTimerPlace = isTimerPlace,
        actionAvailability = actionAvailability,
        timerPlaceLabel = timerPlaceLabel,
    )
    val mutationLockDetail = actionAvailability.lockDetail
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (rowEmphasized) 3.dp else 1.dp,
        shadowElevation = if (rowEmphasized) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(timerActive = isTimerPlace, armed = place.monitoringEnabled && isRegistered)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            place.safeLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (roleLabels.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                roleLabels.forEach { label ->
                                    DockStatePill(
                                        text = label,
                                        color = when (label) {
                                            "Timer here" -> MaterialTheme.colorScheme.primary
                                            "Editing" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.secondary
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        placeMonitoringStatusLabel(
                            monitoringEnabled = place.monitoringEnabled,
                            isRegistered = isRegistered,
                            isTimerPlace = isTimerPlace,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = place.monitoringEnabled,
                    onCheckedChange = onToggleMonitoring,
                    enabled = actionAvailability.monitoringToggleEnabled,
                )
            }

            mutationLockDetail?.let { detail ->
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (monitoringNeedsSetup) {
                OutlinedButton(
                    onClick = onFixMonitoringSetup,
                    enabled = actionAvailability.setupRecoveryEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(placeSetupActionLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Text(
                "${place.radiusMeters.roundToInt()} m radius | ${Notifications.formatDuration(place.durationMinutes)} timer | ${arrivalModeLabel(place.autoStart)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onView,
                    enabled = actionAvailability.viewMapEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View map")
                }
                FilledTonalButton(
                    onClick = onEdit,
                    enabled = actionAvailability.editSettingsEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit settings")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = if (timerAction.cancelTimer) onCancelTimer else onStartNow,
                    enabled = timerAction.enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (timerAction.cancelTimer) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
                ) {
                    Icon(
                        if (timerAction.cancelTimer) Icons.Filled.Cancel else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(timerAction.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(
                    onClick = onDelete,
                    enabled = actionAvailability.removeEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            timerAction.detail?.let { detail ->
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        trailing?.invoke()
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
) {
    SettingsRow(
        icon = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Info,
        title = title,
        detail = permissionRowDetail(title, granted),
        trailing = {
            Text(
                if (granted) "On" else "Off",
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                detail,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun EmptyState(
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
