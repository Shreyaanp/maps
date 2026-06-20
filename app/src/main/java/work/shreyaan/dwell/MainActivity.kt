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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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

private fun openDwellAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    )
}

private fun formatHoursInput(hours: Double): String =
    (Math.round(hours * 100) / 100.0).toString()
        .trimEnd('0')
        .trimEnd('.')

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

private enum class AppRoute {
    Home,
    Insights,
    Settings,
    SavedZones,
}

private enum class PlaceSelectionMode {
    CreateNew,
    EditSelected,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DwellScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(Prefs.isSignedIn(context)) }
    var route by remember { mutableStateOf(AppRoute.Home) }
    var authInFlight by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var permVersion by remember { mutableIntStateOf(0) }
    var diagnosticsRefresh by remember { mutableIntStateOf(0) }
    var insightsRefresh by remember { mutableIntStateOf(0) }
    var locateAfterPermission by remember { mutableStateOf(false) }
    var locateAfterPermissionExpandDock by remember { mutableStateOf(true) }
    var centerAfterStartupPermission by remember { mutableStateOf(false) }
    var startupPermissionPrompted by remember { mutableStateOf(false) }
    var showBackgroundLocationDisclosure by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permVersion++ }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permVersion++ }

    LaunchedEffect(Unit) {
        if (startupPermissionPrompted) return@LaunchedEffect
        startupPermissionPrompted = true
        delay(350L)

        val startupPermissions = buildList {
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
        }

        if (startupPermissions.isNotEmpty()) {
            centerAfterStartupPermission = true
            locateAfterPermission = false
            permissionLauncher.launch(startupPermissions.toTypedArray())
        }
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
    var selectedPlaceId by remember { mutableStateOf(Prefs.getActivePlace(context)?.id.orEmpty()) }
    var placeSelectionMode by remember { mutableStateOf(PlaceSelectionMode.CreateNew) }
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
    var lastSearchAt by remember { mutableLongStateOf(0L) }
    val searchCache = remember { mutableMapOf<String, CachedLocationSearch>() }
    var radius by remember { mutableFloatStateOf(Prefs.getRadius(context)) }
    var durationText by remember {
        val h = Prefs.getDurationMinutes(context) / 60.0
        val rounded = formatHoursInput(h)
        mutableStateOf(rounded.ifEmpty { "4.5" })
    }
    var armed by remember { mutableStateOf(Prefs.isArmed(context)) }
    var timerEnd by remember { mutableLongStateOf(Prefs.getTimerEnd(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun applyActivePlace(place: DwellPlace) {
        Prefs.setActivePlace(context, place.id)
        selectedPlaceId = place.id
        pin = MapPoint(place.latitude, place.longitude)
        selectedPlaceLabel = place.safeLabel
        radius = place.radiusMeters
        durationText = formatHoursInput(place.durationMinutes / 60.0)
    }

    fun refreshPlaces(syncActivePlace: Boolean = false) {
        places = Prefs.getPlaces(context)
        registeredPlaceIds = Prefs.getRegisteredPlaceIds(context)
        monitoringError = Prefs.getMonitoringError(context)
        armed = Prefs.isArmed(context)
        if (syncActivePlace) {
            val active = Prefs.getActivePlace(context)
            if (active != null) {
                applyActivePlace(active)
            } else {
                selectedPlaceId = ""
                pin = null
                selectedPlaceLabel = ""
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
            refreshPlaces(syncActivePlace = true)
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

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

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
                    .title("Timer zone")
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

    fun durationMinutesFromText(text: String): Int? {
        val hours = text.toDoubleOrNull() ?: return null
        if (hours <= 0 || hours > 48) return null
        return (hours * 60).roundToInt().coerceAtLeast(1)
    }

    fun currentDurationMinutes(): Int {
        return durationMinutesFromText(durationText) ?: Prefs.getDurationMinutes(context)
    }

    fun syncSelectedZone(isArmed: Boolean = armed) {
        val selected = pin ?: return
        scope.launch {
            BackendClient.savePrimaryZone(
                context = context,
                label = selectedPlaceLabel.ifBlank { "Selected zone" },
                lat = selected.latitude,
                lon = selected.longitude,
                radiusMeters = radius,
                durationMinutes = currentDurationMinutes(),
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
        durationText = text
        val durationMin = durationMinutesFromText(text) ?: return
        Prefs.setDurationMinutes(context, durationMin)
        WearSync.pushState(context)
        syncSelectedZone(isArmed = armed)
    }

    fun commitRadiusChange(fitMap: Boolean = false) {
        val requestedRadius = DwellRadius.normalize(radius)
        val active = Prefs.getActivePlace(context)
        val currentRadius = active?.radiusMeters ?: Prefs.getRadius(context)
        val activeMonitoring = active?.monitoringEnabled == true

        if (activeMonitoring && requestedRadius > currentRadius + 0.5f) {
            radius = currentRadius
            toast("Pause this place before increasing its radius.")
            pin?.let { fitMapToBoundary(it, currentRadius) }
            return
        }

        radius = requestedRadius
        Prefs.setRadius(context, requestedRadius)
        WearSync.pushState(context)
        pin?.let { prewarmZoneMap(it, requestedRadius) }

        if (activeMonitoring) {
            GeofenceManager.refresh(context) { ok, err ->
                refreshPlaces(syncActivePlace = true)
                if (ok) {
                    syncSelectedZone(isArmed = true)
                    pin?.takeIf { fitMap }?.let { fitMapToBoundary(it, requestedRadius) }
                    toast("Radius tightened to ${requestedRadius.roundToInt()} m")
                } else {
                    toast("Could not update monitoring radius: ${err ?: "unknown error"}")
                }
            }
        } else {
            refreshPlaces(syncActivePlace = true)
            syncSelectedZone(isArmed = false)
            pin?.takeIf { fitMap }?.let { fitMapToBoundary(it, requestedRadius) }
        }
    }

    fun commitGeofencePoint(
        point: MapPoint,
        label: String,
        center: Boolean = true,
        expandDock: Boolean = false,
        analyticsSource: String,
    ) {
        val committedPlace = when (placeSelectionMode) {
            PlaceSelectionMode.CreateNew -> {
                Prefs.createPlace(
                    context,
                    label = label,
                    lat = point.latitude,
                    lon = point.longitude,
                    radiusMeters = radius,
                    durationMinutes = currentDurationMinutes(),
                )
            }
            PlaceSelectionMode.EditSelected -> {
                Prefs.savePlace(context, point.latitude, point.longitude, label)
                Prefs.getActivePlace(context) ?: Prefs.createPlace(
                    context,
                    label = label,
                    lat = point.latitude,
                    lon = point.longitude,
                    radiusMeters = radius,
                    durationMinutes = currentDurationMinutes(),
                )
            }
        }
        placeSelectionMode = PlaceSelectionMode.EditSelected
        pin = MapPoint(committedPlace.latitude, committedPlace.longitude)
        selectedPlaceId = committedPlace.id
        selectedPlaceLabel = committedPlace.safeLabel
        radius = committedPlace.radiusMeters
        durationText = formatHoursInput(committedPlace.durationMinutes / 60.0)
        refreshPlaces()
        WearSync.pushState(context)
        prewarmZoneMap(point)
        if (center) centerMapOn(point)
        homeDockExpanded = expandDock
        syncSelectedZone(isArmed = false)
        scope.launch {
            BackendClient.trackEvent(
                context,
                "location_selected",
                mapOf("source" to analyticsSource),
            )
        }
    }

    fun selectGeofencePoint(
        point: MapPoint,
        label: String,
        center: Boolean = true,
        expandDock: Boolean = false,
        analyticsSource: String,
    ) {
        val changed = pin?.let { distanceMeters(it, point) > 1f } ?: true
        val monitoredActivePlace = Prefs.getActivePlace(context)?.takeIf { it.monitoringEnabled }

        if (
            changed &&
            monitoredActivePlace != null &&
            placeSelectionMode == PlaceSelectionMode.EditSelected
        ) {
            toast("Pause ${monitoredActivePlace.safeLabel} before changing its location")
            return
        }

        commitGeofencePoint(
            point = point,
            label = label,
            center = center,
            expandDock = expandDock,
            analyticsSource = analyticsSource,
        )
    }

    LaunchedEffect(Unit) {
        if (Prefs.hasPlace(context)) return@LaunchedEffect
        val restored = BackendClient.loadPrimaryZone(context) ?: return@LaunchedEffect
        val point = MapPoint(restored.lat, restored.lon)
        pin = point
        selectedPlaceLabel = restored.label
        radius = DwellRadius.normalize(restored.radiusMeters)
        durationText = formatHoursInput(restored.durationMinutes / 60.0)
        Prefs.savePlace(context, restored.lat, restored.lon, restored.label)
        Prefs.setRadius(context, radius)
        Prefs.setDurationMinutes(context, restored.durationMinutes)
        refreshPlaces(syncActivePlace = true)
        WearSync.pushState(context)
        prewarmZoneMap(point, radius)
        fitMapToBoundary(point, radius)
        toast("Saved zone restored")
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

    fun requestCurrentLocation(
        selectAsZone: Boolean = true,
        showErrors: Boolean = true,
        expandDock: Boolean = false,
    ) {
        if (!hasFineLocation(context)) {
            locateAfterPermission = selectAsZone
            locateAfterPermissionExpandDock = expandDock
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            if (showErrors) toast("Grant location permission to show your position")
            return
        }

        locating = true
        fetchCurrentLocation(preferLiveFix = true) { location ->
            locating = false
            if (location == null) {
                if (showErrors) toast("Could not get current location. Check that Location is on.")
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
                if (searchCacheKey(searchText) == queryKey) {
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
            if (searchCacheKey(searchText) != queryKey) {
                return@launch
            }

            submittedSearchKey = queryKey
            if (places.isEmpty()) {
                searchResults = emptyList()
                searchSuggestions = emptyList()
                if (showValidationToast) toast("No places found")
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

    LaunchedEffect(searchText, searchPanelExpanded) {
        val queryKey = searchCacheKey(searchText)
        if (!searchPanelExpanded || queryKey.length < 3 || submittedSearchKey == queryKey) {
            return@LaunchedEffect
        }

        delay(LOCATION_SEARCH_DEBOUNCE_MS)
        if (
            searchPanelExpanded &&
            mobileSearchConfig.networkAutocomplete &&
            searchCacheKey(searchText) == queryKey &&
            submittedSearchKey != queryKey &&
            !searching
        ) {
            performSearch(showValidationToast = false)
        }
    }

    fun maybeStartTimerIfAlreadyInside(
        zone: MapPoint,
        radiusMeters: Float,
        durationMin: Int,
        placeId: String?,
        onChecked: (ArrivalDecision) -> Unit,
    ) {
        if (!hasFineLocation(context)) {
            onChecked(ArrivalDecision.WAIT)
            return
        }

        fetchCurrentLocation(preferLiveFix = true) { location ->
            if (location == null) {
                onChecked(ArrivalDecision.WAIT)
                return@fetchCurrentLocation
            }

            val current = MapPoint(location.latitude, location.longitude)
            val inside = distanceMeters(current, zone) <= radiusMeters
            if (!inside || TimerController.isRunning(context)) {
                onChecked(ArrivalDecision.WAIT)
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
                    Prefs.setWatchPrompt(context, Prefs.WATCH_PROMPT_START_TIMER, placeId)
                    Notifications.notifyArrivalQuestion(context, adjustedConfidence.score)
                    WearSync.pushState(context)
                }
                ArrivalDecision.WAIT -> Unit
            }
            onChecked(adjustedConfidence.decision)
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
                if (!homeDockExpanded) {
                    placeSelectionMode = PlaceSelectionMode.CreateNew
                }
                selectGeofencePoint(
                    point = MapPoint(latLng.latitude, latLng.longitude),
                    label = "Dropped pin",
                    analyticsSource = "map_long_press",
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
            locateAfterPermission = false
            val expandAfterPermission = locateAfterPermissionExpandDock
            locateAfterPermissionExpandDock = true
            if (hasFineLocation(context)) {
                requestCurrentLocation(
                    selectAsZone = true,
                    expandDock = expandAfterPermission,
                )
            }
        }
        if (centerAfterStartupPermission) {
            centerAfterStartupPermission = false
            if (hasFineLocation(context) && !Prefs.hasPlace(context)) {
                requestCurrentLocation(selectAsZone = false, showErrors = false)
            }
        }
        if (
            permVersion > 0 &&
            Prefs.getArmedPlaces(context).isNotEmpty() &&
            MonitoringPrerequisites.issueForContext(context) == null
        ) {
            GeofenceManager.refreshOnAppOpen(context) { _, _ ->
                refreshPlaces(syncActivePlace = true)
            }
        }
    }

    // Redraw the pin + radius circle whenever they change.
    LaunchedEffect(pin, radius) {
        redrawZoneOverlay()
    }

    fun parseDurationMinutes(): Int? {
        return durationMinutesFromText(durationText)
    }

    @SuppressLint("InlinedApi")
    fun armGeofence() {
        val p = pin ?: run {
            toast("Long-press the map to set the place first")
            return
        }
        val durationMin = parseDurationMinutes() ?: run {
            toast("Enter a valid duration in hours, e.g. 4.5")
            return
        }
        when {
            !hasFineLocation(context) ||
                !hasNotifications(context) ||
                !hasActivityRecognition(context) -> {
                val perms = buildList {
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
                }
                permissionLauncher.launch(perms.toTypedArray())
                toast("Grant location, motion, and notification permissions, then tap Arm again")
            }
            !hasBackgroundLocation(context) -> {
                showBackgroundLocationDisclosure = true
            }
            else -> {
                Prefs.savePlace(
                    context,
                    p.latitude,
                    p.longitude,
                    selectedPlaceLabel.ifBlank { "Dropped pin" },
                )
                Prefs.setRadius(context, radius)
                Prefs.setDurationMinutes(context, durationMin)
                WearSync.pushState(context)
                prewarmZoneMap(p, radius)
                GeofenceManager.arm(context, p.latitude, p.longitude, radius) { ok, err ->
                    refreshPlaces(syncActivePlace = true)
                    armed = Prefs.isArmed(context)
                    if (!ok) {
                        toast("Failed to arm geofence: ${err ?: "unknown error"}")
                        return@arm
                    }

                    val armedPlace = Prefs.getActivePlace(context)
                    val batteryStatus = BatteryReliability.status(context)
                    if (
                        !batteryStatus.isIgnoringOptimizations &&
                        batteryStatus.isKnownAggressiveOem
                    ) {
                        toast("${batteryStatus.manufacturer} may delay background arrival. Use unrestricted battery in Settings.")
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
                        armedPlace?.id,
                    ) { decision ->
                        if (decision == ArrivalDecision.START_TIMER) {
                            scope.launch {
                                BackendClient.trackEvent(
                                    context,
                                    "timer_auto_started",
                                    mapOf("durationMinutes" to durationMin),
                                )
                            }
                        }
                        toast(
                            when (decision) {
                                ArrivalDecision.START_TIMER ->
                                    "You are already here - timer started"
                                ArrivalDecision.ASK_TO_START ->
                                    "Dwell thinks you are here - confirm start from the notification"
                                ArrivalDecision.WAIT ->
                                    "Armed - the timer will start when you arrive"
                            }
                        )
                    }
                }
            }
        }
    }

    val timerActive = timerEnd > now
    val activePlace = places.firstOrNull { it.id == selectedPlaceId } ?: Prefs.getActivePlace(context)
    val activePlaceArmed = activePlace?.monitoringEnabled == true
    val activePlaceRegistered = activePlace?.let { registeredPlaceIds.contains(it.id) } == true
    val activePlaceNeedsSetup = activePlaceArmed && !activePlaceRegistered
    val activePlaceAutoStart = activePlace?.autoStart ?: true
    val setupIssue = MonitoringPrerequisites.issueForContext(context)
    val selectedPlaceSetupIssue = setupIssue?.takeIf { pin != null }
    val batteryStatus = BatteryReliability.status(context)
    val batteryWarning = if (
        activePlaceArmed &&
        activePlaceRegistered &&
        !batteryStatus.isIgnoringOptimizations &&
        batteryStatus.isKnownAggressiveOem
    ) {
        batteryStatus.detail
    } else {
        ""
    }
    val setupNotice = when {
        activePlaceNeedsSetup -> monitoringError.ifBlank {
            "Background arrival detection needs attention."
        }
        selectedPlaceSetupIssue != null -> selectedPlaceSetupIssue.error
        else -> ""
    }
    val armedPlaceCount = places.count { it.monitoringEnabled }
    val livePlaceCount = places.count { it.monitoringEnabled && registeredPlaceIds.contains(it.id) }
    val durationMinutes = parseDurationMinutes() ?: Prefs.getDurationMinutes(context)
    val statusTitle = when {
        timerActive -> "Timer running"
        activePlaceNeedsSetup -> "Monitoring needs setup"
        selectedPlaceSetupIssue != null -> "Setup needed"
        activePlaceArmed -> "Monitoring live"
        armed -> "$armedPlaceCount places monitoring"
        pin != null -> "Ready to monitor"
        else -> "Choose a place"
    }
    val statusDetail = when {
        timerActive -> {
            val left = timerEnd - now
            val h = left / 3_600_000
            val m = (left / 60_000) % 60
            val s = (left / 1000) % 60
            val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
            "${h}h ${m}m ${s}s left - ends $endsAt"
        }
        activePlaceNeedsSetup -> monitoringError.ifBlank { "Open setup to restore arrival detection" }
        selectedPlaceSetupIssue != null -> selectedPlaceSetupIssue.error
        batteryWarning.isNotBlank() -> batteryWarning
        activePlaceArmed -> "Timer starts when you arrive"
        armed -> "Other saved places are monitoring arrivals"
        pin != null -> "Selected place is ready"
        else -> "Choose a place on the map"
    }
    val placeLabel = if (pin == null) "No place selected" else selectedPlaceLabel.ifBlank {
        "Dropped pin"
    }

    fun startOrCancelTimer() {
        if (timerActive) {
            TimerController.cancelTimer(context)
            Notifications.notifyTimerCancelled(context)
            timerEnd = 0L
            insightsRefresh += 1
            scope.launch {
                BackendClient.trackEvent(context, "timer_cancelled")
            }
            return
        }

        val durationMin = parseDurationMinutes()
        if (durationMin == null) {
            toast("Enter a valid duration in hours, e.g. 4.5")
        } else if (!hasNotifications(context)) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            )
            toast("Grant notifications, then try again")
        } else {
            Prefs.setDurationMinutes(context, durationMin)
            TimerController.startTimer(context, durationMin, Prefs.getActivePlace(context)?.id)
            timerEnd = Prefs.getTimerEnd(context)
            scope.launch {
                BackendClient.trackEvent(
                    context,
                    "timer_manual_started",
                    mapOf("durationMinutes" to durationMin),
                )
            }
        }
    }

    fun pauseSelectedPlace() {
        val place = activePlace ?: run {
            toast("Choose a place first")
            return
        }
        GeofenceManager.setPlaceMonitoring(context, place.id, false) { ok, err ->
            refreshPlaces(syncActivePlace = true)
            if (ok) {
                syncSelectedZone(isArmed = false)
                toast("${place.safeLabel} paused")
                scope.launch {
                    BackendClient.trackEvent(context, "geofence_disarmed")
                }
            } else {
                toast("Could not pause this place: ${err ?: "unknown error"}")
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
                    "Dwell collects location data to detect arrivals, start timers, and show leave-zone prompts even when the app is closed or not in use. Your selected places may be stored with Dwell to sync timer state. Location is not used for ads."
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
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                toast("Choose \"Allow all the time\" so arrivals work in the background, then tap Arm again")
                            }
                            BackgroundLocationFlow.OpenAppSettings -> {
                                openDwellAppSettings(context)
                                toast("Open Permissions > Location, choose \"Allow all the time\", then return to Dwell")
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

    fun openSearchOnMap(mode: PlaceSelectionMode = PlaceSelectionMode.CreateNew) {
        placeSelectionMode = mode
        route = AppRoute.Home
        homeDockExpanded = false
        searchPanelExpanded = true
        searchFocusRequest += 1
    }

    fun closeSearchPanel() {
        searchPanelExpanded = false
        focusManager.clearFocus()
    }

    fun clearSearchState(clearText: Boolean = true) {
        if (clearText) searchText = ""
        searchResults = emptyList()
        searchSuggestions = emptyList()
        submittedSearchKey = ""
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

    when (route) {
        AppRoute.Settings -> SettingsScreen(
            radius = radius,
            durationText = durationText,
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
                radius = it
            },
            onRadiusChangeFinished = {
                commitRadiusChange()
            },
            onDurationChange = { persistDurationText(it) },
            onDurationPreset = { hours ->
                durationText = formatHoursInput(hours)
                Prefs.setDurationMinutes(context, (hours * 60).roundToInt())
                WearSync.pushState(context)
                syncSelectedZone(isArmed = armed)
            },
            onOpenAppSettings = {
                openDwellAppSettings(context)
            },
            onOpenBatterySettings = {
                val opened = BatteryReliability.openSettings(context)
                if (!opened) toast("Could not open battery settings on this device")
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
            onSignOut = {
                Prefs.setSignedIn(context, false)
                signedIn = false
                route = AppRoute.Home
            },
            onDeleteAppData = {
                scope.launch {
                    val deleted = BackendClient.deleteAppData(context) != null
                    if (!deleted) {
                        toast("Could not delete server data. Check connection and try again.")
                        return@launch
                    }

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
                    pin = null
                    selectedPlaceLabel = ""
                    radius = Prefs.getRadius(context)
                    durationText = formatHoursInput(Prefs.getDurationMinutes(context) / 60.0)
                    timerEnd = 0L
                    WearSync.pushState(context)
                    route = AppRoute.Home
                    toast("App data deleted")
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
                    pin = null
                    selectedPlaceLabel = ""
                    radius = Prefs.getRadius(context)
                    durationText = formatHoursInput(Prefs.getDurationMinutes(context) / 60.0)
                    timerEnd = 0L
                    signedIn = false
                    WearSync.pushState(context)
                    route = AppRoute.Home
                    toast("Account deleted")
                }
            },
        )
        AppRoute.SavedZones -> SavedZonesScreen(
            places = places,
            registeredPlaceIds = registeredPlaceIds,
            monitoringError = monitoringError,
            activePlaceId = selectedPlaceId,
            timerPlaceId = Prefs.getTimerPlaceId(context),
            timerActive = timerActive,
            onBack = { route = AppRoute.Home },
            onCreateZone = {
                openSearchOnMap(PlaceSelectionMode.CreateNew)
                toast("Search or long-press the map to add a place")
            },
            onViewPlace = { place ->
                applyActivePlace(place)
                refreshPlaces()
                route = AppRoute.Home
                centerMapOn(MapPoint(place.latitude, place.longitude))
            },
            onEditPlace = { place ->
                placeSelectionMode = PlaceSelectionMode.EditSelected
                applyActivePlace(place)
                refreshPlaces()
                route = AppRoute.Home
                centerMapOn(MapPoint(place.latitude, place.longitude))
                homeDockExpanded = true
            },
            onToggleMonitoring = toggleMonitoring@ { place, enabled ->
                applyActivePlace(place)
                if (enabled) {
                    when {
                        !hasFineLocation(context) ||
                            !hasNotifications(context) ||
                            !hasActivityRecognition(context) -> {
                            val perms = buildList {
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
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                            toast("Grant location, motion, and notification permissions, then enable again")
                            return@toggleMonitoring
                        }
                        !hasBackgroundLocation(context) -> {
                            showBackgroundLocationDisclosure = true
                            toast("Allow all-the-time location, then enable this place again")
                            return@toggleMonitoring
                        }
                    }
                }

                GeofenceManager.setPlaceMonitoring(context, place.id, enabled) { ok, err ->
                    refreshPlaces(syncActivePlace = true)
                    if (!ok) {
                        toast(
                            if (enabled) {
                                "Saved, but monitoring is not live yet: ${err ?: "unknown error"}"
                            } else {
                                "Could not update monitoring: ${err ?: "unknown error"}"
                            }
                        )
                        return@setPlaceMonitoring
                    }

                    if (enabled) {
                        val updated = Prefs.getPlace(context, place.id) ?: place
                        maybeStartTimerIfAlreadyInside(
                            MapPoint(updated.latitude, updated.longitude),
                            updated.radiusMeters,
                            updated.durationMinutes,
                            updated.id,
                        ) { decision ->
                            toast(
                                when (decision) {
                                    ArrivalDecision.START_TIMER ->
                                        "You are already at ${updated.safeLabel} - timer started"
                                    ArrivalDecision.ASK_TO_START ->
                                        "Dwell thinks you are at ${updated.safeLabel} - confirm start"
                                    ArrivalDecision.WAIT ->
                                        "${updated.safeLabel} is monitoring arrivals"
                                }
                            )
                        }
                    } else {
                        toast("${place.safeLabel} paused")
                    }
                }
            },
            onDeletePlace = { place ->
                scope.launch {
                    if (timerActive && Prefs.getTimerPlaceId(context) == place.id) {
                        TimerController.cancelTimer(context)
                        Notifications.notifyTimerCancelled(context)
                        timerEnd = 0L
                        insightsRefresh += 1
                    }
                    if (place.monitoringEnabled) {
                        GeofenceManager.setPlaceMonitoring(context, place.id, false) { _, _ -> }
                    }
                    Prefs.deletePlace(context, place.id)
                    if (Prefs.getPlaces(context).isEmpty()) {
                        BackendClient.deletePrimaryZone(context)
                    }
                    GeofenceManager.refresh(context) { _, _ -> }
                    Notifications.clearExitQuestion(context)
                    WearSync.pushState(context)
                    BackendClient.trackEvent(context, "zone_deleted")
                    refreshPlaces(syncActivePlace = true)
                    route = AppRoute.Home
                    toast("Saved place removed")
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
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                MapHeader(
                    placeLabel = placeLabel,
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
                        if (!homeDockExpanded) {
                            placeSelectionMode = PlaceSelectionMode.CreateNew
                        }
                        homeDockExpanded = false
                        searchPanelExpanded = true
                        updateSearchText(it)
                    },
                    onSubmit = {
                        if (!homeDockExpanded) {
                            placeSelectionMode = PlaceSelectionMode.CreateNew
                        }
                        homeDockExpanded = false
                        searchPanelExpanded = true
                        performSearch()
                    },
                    onClearSearch = {
                        clearSearchState()
                    },
                    onSelectResult = ::selectSearchResult,
                    onUseCurrentLocation = {
                        closeSearchPanel()
                        clearSearchState()
                        homeDockExpanded = false
                        if (!locating) {
                            placeSelectionMode = PlaceSelectionMode.CreateNew
                            requestCurrentLocation(selectAsZone = true)
                        }
                    },
                    onOpenSavedZones = {
                        closeSearchPanel()
                        clearSearchState()
                        homeDockExpanded = false
                        route = AppRoute.SavedZones
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
                        onCurrentLocation = {
                            closeSearchPanel()
                            homeDockExpanded = false
                            if (!locating) {
                                requestCurrentLocation(selectAsZone = false)
                            }
                        },
                        onSavedZones = {
                            closeSearchPanel()
                            homeDockExpanded = false
                            route = AppRoute.SavedZones
                        },
                        onInsights = {
                            closeSearchPanel()
                            homeDockExpanded = false
                            route = AppRoute.Insights
                        },
                        onSettings = {
                            closeSearchPanel()
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
                    placeLabel = placeLabel,
                    radius = radius,
                    durationText = durationText,
                    durationMinutes = durationMinutes,
                    timerActive = timerActive,
                    activePlaceArmed = activePlaceArmed,
                    activePlaceRegistered = activePlaceRegistered,
                    activePlaceNeedsSetup = activePlaceNeedsSetup,
                    activePlaceAutoStart = activePlaceAutoStart,
                    armedPlaceCount = armedPlaceCount,
                    livePlaceCount = livePlaceCount,
                    monitoringError = monitoringError,
                    setupNotice = setupNotice,
                    batteryWarning = batteryWarning,
                    onPrimaryAction = {
                        when {
                            timerActive -> extendTimer(30)
                            pin == null -> {
                                placeSelectionMode = PlaceSelectionMode.CreateNew
                                requestCurrentLocation(selectAsZone = true)
                            }
                            selectedPlaceSetupIssue != null -> armGeofence()
                            activePlaceNeedsSetup -> armGeofence()
                            activePlaceArmed -> pauseSelectedPlace()
                            else -> armGeofence()
                        }
                    },
                    onSecondaryAction = {
                        if (timerActive) {
                            startOrCancelTimer()
                        } else {
                            route = AppRoute.SavedZones
                        }
                    },
                    expanded = homeDockExpanded,
                    onExpandedChange = { homeDockExpanded = it },
                    onRadiusChange = { radius = it },
                    onRadiusChangeFinished = {
                        commitRadiusChange(fitMap = true)
                    },
                    onDurationChange = { persistDurationText(it) },
                    onDurationPreset = { hours ->
                        durationText = formatHoursInput(hours)
                        Prefs.setDurationMinutes(context, (hours * 60).roundToInt())
                        WearSync.pushState(context)
                        syncSelectedZone(isArmed = activePlaceArmed)
                    },
                    onAutoStartChange = { enabled ->
                        val place = activePlace ?: return@HomeStatusDock
                        if (Prefs.setPlaceAutoStart(context, place.id, enabled)) {
                            refreshPlaces(syncActivePlace = true)
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
                        if (activePlaceArmed) {
                            pauseSelectedPlace()
                        } else {
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
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
            Text(
                "Dwell",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Zones, timers, and watch sync.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    onSelectResult: (LocationSearchResult) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenSavedZones: () -> Unit,
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
    val cleanedQuery = cleanSearchQuery(searchText)
    val dropdownVisible = expanded && (
        searchFocused ||
            cleanedQuery.isNotBlank() ||
            searchResults.isNotEmpty() ||
            searchSuggestions.isNotEmpty() ||
            searching
        )
    val trailingIcon: (@Composable () -> Unit)? = when {
        searching -> {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        searchText.isNotBlank() -> {
            {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Clear search")
                }
            }
        }
        locating -> {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        else -> null
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
                        if (placeLabel == "No place selected") "Search place or address" else "Search places",
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
                onSelectResult = onSelectResult,
                onUseCurrentLocation = onUseCurrentLocation,
                onOpenSavedZones = onOpenSavedZones,
            )
        }
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
    onSelectResult: (LocationSearchResult) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenSavedZones: () -> Unit,
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
            when {
                cleanedQuery.isBlank() -> {
                    SearchSectionLabel("Quick actions")
                    SearchActionRow(
                        icon = Icons.Filled.MyLocation,
                        title = "Use current location",
                        subtitle = "Drop a zone where you are now",
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
                        subtitle = "Fastest way to create a nearby zone",
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
                        detail = "Try a nearby landmark, area, or full address.",
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
                contentDescription = "Current location",
                loading = locating,
                emphasized = true,
                onClick = onCurrentLocation,
            )
            MapRailButton(
                icon = Icons.Filled.Bookmark,
                contentDescription = "Saved zones",
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
    radius: Float,
    durationText: String,
    durationMinutes: Int,
    timerActive: Boolean,
    activePlaceArmed: Boolean,
    activePlaceRegistered: Boolean,
    activePlaceNeedsSetup: Boolean,
    activePlaceAutoStart: Boolean,
    armedPlaceCount: Int,
    livePlaceCount: Int,
    monitoringError: String,
    setupNotice: String,
    batteryWarning: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
    onDurationChange: (String) -> Unit,
    onDurationPreset: (Double) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onArmClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPlace = placeLabel != "No place selected"
    val tone = when {
        timerActive -> MaterialTheme.colorScheme.primary
        activePlaceNeedsSetup || setupNotice.isNotBlank() -> MaterialTheme.colorScheme.tertiary
        activePlaceArmed && activePlaceRegistered -> MaterialTheme.colorScheme.secondary
        hasPlace -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val primaryLabel = when {
        timerActive -> "+30m"
        !hasPlace -> "Use current"
        setupNotice.isNotBlank() -> "Setup"
        activePlaceNeedsSetup -> "Fix"
        activePlaceArmed -> "Pause"
        else -> "Monitor"
    }
    val primaryIcon = when {
        timerActive -> Icons.Filled.AccessTime
        !hasPlace -> Icons.Filled.MyLocation
        setupNotice.isNotBlank() -> Icons.Filled.Info
        activePlaceNeedsSetup -> Icons.Filled.Info
        activePlaceArmed -> Icons.Filled.Cancel
        else -> Icons.Filled.NotificationsActive
    }
    val secondaryLabel = if (timerActive) "Cancel" else "Places"
    val secondaryIcon = if (timerActive) Icons.Filled.Cancel else Icons.Filled.Bookmark
    val liveText = when {
        livePlaceCount > 0 -> "$livePlaceCount live"
        armedPlaceCount > 0 -> "$armedPlaceCount needs setup"
        else -> "Not live"
    }
    val stateLabel = when {
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
                            timerActive -> statusDetail
                            !expanded && hasPlace -> "$stateLabel | ${Notifications.formatDuration(durationMinutes)} | ${radius.roundToInt()} m | $placeLabel"
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
                    onClick = onPrimaryAction,
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

                    if (timerActive || setupNotice.isNotBlank()) {
                        DockNoticeStrip(
                            icon = if (timerActive) Icons.Filled.Timer else Icons.Filled.Info,
                            title = if (timerActive) "Timer is running" else "Monitoring needs setup",
                            detail = if (timerActive) statusDetail else setupNotice,
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
                            autoStart = activePlaceAutoStart,
                            onAutoStartChange = onAutoStartChange,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onArmClick,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    if (activePlaceArmed) Icons.Filled.Cancel else Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (activePlaceArmed) "Pause" else "Monitor",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            FilledTonalButton(
                                onClick = onTimerClick,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    if (timerActive) Icons.Filled.Cancel else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (timerActive) "Cancel" else "Start now",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (activePlaceNeedsSetup && monitoringError.isNotBlank()) {
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
private fun RadiusControl(
    radius: Float,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
) {
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
                "${radius.roundToInt()} m",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = radius,
            onValueChange = onRadiusChange,
            onValueChangeFinished = onRadiusChangeFinished,
            valueRange = DwellRadius.MIN_METERS..DwellRadius.MAX_METERS,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationControl(
    durationText: String,
    onDurationChange: (String) -> Unit,
    onDurationPreset: (Double) -> Unit,
) {
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
            listOf(1.0, 2.0, 4.5, 8.0).forEach { hours ->
                val value = formatHoursInput(hours)
                FilterChip(
                    selected = durationText == value,
                    onClick = { onDurationPreset(hours) },
                    label = { Text("${value}h") },
                )
            }
        }

        OutlinedTextField(
            value = durationText,
            onValueChange = onDurationChange,
            label = { Text("Duration in hours") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ArrivalModeControl(
    autoStart: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                if (autoStart) "Auto-start" else "Confirm first",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (autoStart) {
                    "High-confidence arrivals start the timer."
                } else {
                    "Dwell asks before starting here."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = autoStart,
            onCheckedChange = onAutoStartChange,
        )
    }
}

@Composable
private fun SettingsScreen(
    radius: Float,
    durationText: String,
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
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onClearMapCache: () -> Unit,
    onClearSearchCache: () -> Unit,
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

        SettingsSection(title = "Data controls") {
            SettingsRow(
                icon = Icons.Filled.Delete,
                title = "Delete app data",
                detail = "Removes saved zones and analytics from this install, while keeping the session.",
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

        SettingsSection(title = "Defaults") {
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
        }

        SettingsSection(title = "Permissions") {
            PermissionRow("Location", locationGranted)
            PermissionRow("Background location", backgroundGranted)
            PermissionRow("Notifications", notificationsGranted)
            PermissionRow("Motion", motionGranted)
            PermissionRow("Exact alarms", exactAlarmAllowed)
            OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Android settings")
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
                        "Allow unrestricted battery"
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
                detail = "Phone owns geofence detection; watch mirrors the timer",
            )
        }
    }

    if (confirmDeleteData) {
        AlertDialog(
            onDismissRequest = { confirmDeleteData = false },
            title = { Text("Delete app data?") },
            text = {
                Text("Saved zones, timer defaults, and analytics for this install will be deleted. Your session stays signed in.")
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
    activePlaceId: String,
    timerPlaceId: String,
    timerActive: Boolean,
    onBack: () -> Unit,
    onCreateZone: () -> Unit,
    onViewPlace: (DwellPlace) -> Unit,
    onEditPlace: (DwellPlace) -> Unit,
    onToggleMonitoring: (DwellPlace, Boolean) -> Unit,
    onDeletePlace: (DwellPlace) -> Unit,
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
            title = "Places",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onCreateZone) {
                    Icon(Icons.Filled.Place, contentDescription = "Add place")
                }
            },
        )

        if (places.isEmpty()) {
            EmptyState(
                title = "No saved places",
                detail = "Create places from search, current location, or the map.",
                actionLabel = "Create place",
                onAction = onCreateZone,
            )
        } else {
            PlacesSummaryRow(
                places = places,
                registeredPlaceIds = registeredPlaceIds,
                monitoringError = monitoringError,
            )
            places.forEach { place ->
                PlaceRow(
                    place = place,
                    isSelected = place.id == activePlaceId,
                    isRegistered = registeredPlaceIds.contains(place.id),
                    isTimerPlace = timerActive && place.id == timerPlaceId,
                    onView = { onViewPlace(place) },
                    onEdit = { onEditPlace(place) },
                    onToggleMonitoring = { enabled -> onToggleMonitoring(place, enabled) },
                    onDelete = { onDeletePlace(place) },
                )
            }
            OutlinedButton(onClick = onCreateZone, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add place")
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
                    when {
                        monitored == 0 -> "No places monitoring arrivals"
                        needsSetup == 0 -> "$live live arrival${if (live == 1) "" else "s"}"
                        live == 0 -> "$needsSetup needs setup"
                        else -> "$live live, $needsSetup needs setup"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
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

@Composable
private fun PlaceRow(
    place: DwellPlace,
    isSelected: Boolean,
    isRegistered: Boolean,
    isTimerPlace: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onToggleMonitoring: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(timerActive = isTimerPlace, armed = place.monitoringEnabled && isRegistered)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            place.safeLabel,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isSelected) {
                            DockStatePill(
                                text = "Selected",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        when {
                            isTimerPlace -> "Timer running here"
                            place.monitoringEnabled && isRegistered -> "Monitoring live"
                            place.monitoringEnabled -> "Needs setup to monitor"
                            else -> "Paused"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = place.monitoringEnabled,
                    onCheckedChange = onToggleMonitoring,
                )
            }

            Text(
                "${place.radiusMeters.roundToInt()} m radius | ${Notifications.formatDuration(place.durationMinutes)} timer",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onView, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View")
                }
                FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
            }

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Remove")
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
        detail = if (granted) "Allowed" else "Needs attention",
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
