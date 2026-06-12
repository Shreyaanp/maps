package work.shreyaan.dwell

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.view.WindowCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
            setMapViewHardwareAccelerated(true)
            setMapTileDownloaderFollowRedirects(true)
            setCacheMapTileCount(36.toShort())
            setCacheMapTileOvershoot(18.toShort())
            setTileDownloadThreads(4.toShort())
            setTileDownloadMaxQueueSize(80.toShort())
            setTileFileSystemThreads(2.toShort())
            setTileFileSystemMaxQueueSize(80.toShort())
            setTileFileSystemCacheMaxBytes(128L * 1024L * 1024L)
            setTileFileSystemCacheTrimBytes(96L * 1024L * 1024L)
        }
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

private fun canExactAlarm(c: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) return true
    return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

private fun formatHoursInput(hours: Double): String =
    (Math.round(hours * 100) / 100.0).toString()
        .trimEnd('0')
        .trimEnd('.')

private data class LocationSearchResult(
    val label: String,
    val point: GeoPoint,
)

private suspend fun searchOpenStreetMap(
    query: String,
    userAgent: String,
): List<LocationSearchResult> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
    val url = URL(
        "https://nominatim.openstreetmap.org/search" +
            "?format=jsonv2&limit=5&q=$encoded"
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
                    add(LocationSearchResult(label, GeoPoint(lat, lon)))
                }
            }
        }
    } finally {
        conn.disconnect()
    }
}

private fun distanceMeters(a: GeoPoint, b: GeoPoint): Float {
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
    var marker: Marker? = null
    var circle: Polygon? = null
    var myLocation: MyLocationNewOverlay? = null
}

private enum class AppRoute {
    Home,
    Settings,
    SavedZones,
}

private enum class ActiveSheet {
    Search,
    ZoneSetup,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DwellScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(Prefs.isSignedIn(context)) }
    var route by remember { mutableStateOf(AppRoute.Home) }
    var authInFlight by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var permVersion by remember { mutableIntStateOf(0) }
    var locateAfterPermission by remember { mutableStateOf(false) }
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

    var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
    var pin by remember {
        mutableStateOf(
            if (Prefs.hasPlace(context))
                GeoPoint(Prefs.getLat(context), Prefs.getLon(context))
            else null
        )
    }
    var selectedPlaceLabel by remember {
        mutableStateOf(if (Prefs.hasPlace(context)) Prefs.getPlaceLabel(context) else "")
    }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var searching by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var lastSearchAt by remember { mutableLongStateOf(0L) }
    var radius by remember { mutableFloatStateOf(Prefs.getRadius(context)) }
    var durationText by remember {
        val h = Prefs.getDurationMinutes(context) / 60.0
        val rounded = formatHoursInput(h)
        mutableStateOf(rounded.ifEmpty { "4.5" })
    }
    var armed by remember { mutableStateOf(Prefs.isArmed(context)) }
    var timerEnd by remember { mutableLongStateOf(Prefs.getTimerEnd(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Tick every second: drives the countdown and picks up changes made by
    // the broadcast receivers (timer started by geofence, cancelled, etc.)
    LaunchedEffect(Unit) {
        BackendClient.trackEvent(context, "app_open")
        while (true) {
            now = System.currentTimeMillis()
            timerEnd = Prefs.getTimerEnd(context)
            armed = Prefs.isArmed(context)
            delay(1000)
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setTilesScaledToDpi(true)
            setUseDataConnection(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            setFlingEnabled(true)
            setMinZoomLevel(3.0)
            setMaxZoomLevel(19.0)
            setHorizontalMapRepetitionEnabled(false)
            setVerticalMapRepetitionEnabled(false)
            controller.setZoom(if (Prefs.hasPlace(context)) 16.0 else 3.0)
            controller.setCenter(pin ?: GeoPoint(20.0, 0.0))
        }
    }
    val overlays = remember { ZoneOverlays() }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun geoBoundsFor(point: GeoPoint, radiusMeters: Float): GeoBounds =
        GeofenceMapBounds.forCircle(
            latitude = point.latitude,
            longitude = point.longitude,
            radiusMeters = radiusMeters,
        )

    fun boundaryBoxFor(bounds: GeoBounds): BoundingBox {
        return BoundingBox(
            bounds.north,
            bounds.east,
            bounds.south,
            bounds.west,
        )
    }

    fun fitMapToBoundary(point: GeoPoint, radiusMeters: Float = radius) {
        val bounds = geoBoundsFor(point, radiusMeters)
        val target = GeoPoint(
            (bounds.north + bounds.south) / 2.0,
            (bounds.east + bounds.west) / 2.0,
        )
        val boundary = boundaryBoxFor(bounds)

        fun moveMap(animated: Boolean) {
            mapView.controller.stopPanning()
            mapView.controller.stopAnimation(true)
            if (mapView.width > 0 && mapView.height > 0) {
                mapView.zoomToBoundingBox(
                    boundary,
                    animated,
                    96,
                    18.5,
                    if (animated) 350L else null,
                )
            } else if (mapView.zoomLevelDouble < 15.0) {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(target)
            }
            mapView.invalidate()
        }

        if (mapView.width > 0 && mapView.height > 0) {
            moveMap(animated = true)
        } else {
            mapView.post { moveMap(animated = false) }
        }
    }

    fun centerMapOn(point: GeoPoint) = fitMapToBoundary(point)

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

    fun persistDurationText(text: String) {
        durationText = text
        val durationMin = durationMinutesFromText(text) ?: return
        Prefs.setDurationMinutes(context, durationMin)
        WearSync.pushState(context)
        syncSelectedZone(isArmed = armed)
    }

    fun commitGeofencePoint(
        point: GeoPoint,
        label: String,
        center: Boolean = true,
        analyticsSource: String,
    ) {
        pin = GeoPoint(point.latitude, point.longitude)
        selectedPlaceLabel = label
        Prefs.savePlace(context, point.latitude, point.longitude, label)
        WearSync.pushState(context)
        if (center) centerMapOn(point)
        activeSheet = ActiveSheet.ZoneSetup
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
        point: GeoPoint,
        label: String,
        center: Boolean = true,
        analyticsSource: String,
    ) {
        val changed = pin?.let { distanceMeters(it, point) > 1f } ?: true

        if (changed && armed) {
            GeofenceManager.disarm(context) { ok ->
                if (!ok) {
                    toast("Could not disarm current zone. Try again before changing place.")
                    return@disarm
                }

                armed = false
                commitGeofencePoint(point, label, center, analyticsSource)
                toast("Place updated - arm the new zone when ready")
            }
            return
        }

        commitGeofencePoint(point, label, center, analyticsSource)
    }

    LaunchedEffect(Unit) {
        if (Prefs.hasPlace(context)) return@LaunchedEffect
        val restored = BackendClient.loadPrimaryZone(context) ?: return@LaunchedEffect
        val point = GeoPoint(restored.lat, restored.lon)
        pin = point
        selectedPlaceLabel = restored.label
        radius = restored.radiusMeters.coerceIn(50f, 500f)
        durationText = formatHoursInput(restored.durationMinutes / 60.0)
        Prefs.savePlace(context, restored.lat, restored.lon, restored.label)
        Prefs.setRadius(context, radius)
        Prefs.setDurationMinutes(context, restored.durationMinutes)
        WearSync.pushState(context)
        fitMapToBoundary(point, radius)
        toast("Saved zone restored")
    }

    fun fetchCurrentLocation(onResult: (Location?) -> Unit) {
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
        val handler = Handler(Looper.getMainLooper())
        val cancellation = CancellationTokenSource()
        var fallbackLocation: Location? = null
        var delivered = false
        lateinit var timeout: Runnable

        fun locationAgeMs(location: Location): Long =
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L

        fun locationAccuracy(location: Location): Float =
            if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE

        fun isUsableLocation(location: Location?): Boolean {
            if (location == null) return false
            return LocationQuality.isUsable(
                latitude = location.latitude,
                longitude = location.longitude,
                ageMs = locationAgeMs(location),
                accuracyMeters = locationAccuracy(location),
                isMock = LocationCompat.isMock(location),
            )
        }

        fun isImmediateLocation(location: Location?): Boolean {
            if (location == null) return false
            return LocationQuality.isImmediate(
                latitude = location.latitude,
                longitude = location.longitude,
                ageMs = locationAgeMs(location),
                accuracyMeters = locationAccuracy(location),
                isMock = LocationCompat.isMock(location),
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
            val locationManager = context.getSystemService(LocationManager::class.java)
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

        fun deliver(location: Location?) {
            if (delivered) return
            delivered = true
            cancellation.cancel()
            handler.removeCallbacks(timeout)
            onResult(location?.takeIf(::isUsableLocation) ?: fallbackLocation)
        }

        timeout = Runnable { deliver(fallbackLocation) }
        handler.postDelayed(timeout, 5_000L)
        updateFallback(platformLastKnownLocation())
        if (isImmediateLocation(fallbackLocation)) {
            deliver(fallbackLocation)
            return
        }

        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    updateFallback(location)
                    if (isImmediateLocation(location)) {
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
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellation.token,
            )
                .addOnSuccessListener { location ->
                    if (isUsableLocation(location)) {
                        deliver(location)
                    } else {
                        deliver(fallbackLocation)
                    }
                }
                .addOnFailureListener { deliver(fallbackLocation) }
        } catch (_: SecurityException) {
            deliver(fallbackLocation)
        }
    }

    fun requestCurrentLocation(selectAsZone: Boolean = true, showErrors: Boolean = true) {
        if (!hasFineLocation(context)) {
            locateAfterPermission = selectAsZone
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
        fetchCurrentLocation { location ->
            locating = false
            if (location == null) {
                if (showErrors) toast("Could not get current location. Check that Location is on.")
                return@fetchCurrentLocation
            }

            val point = GeoPoint(location.latitude, location.longitude)
            if (selectAsZone) {
                selectGeofencePoint(
                    point = point,
                    label = "Current location",
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

    fun performSearch() {
        val query = searchText.trim()
        if (query.length < 2) {
            toast("Type a place or address to search")
            return
        }
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastSearchAt < 1_000L) {
            toast("Give search a second before trying again")
            return
        }

        lastSearchAt = nowMs
        searching = true
        scope.launch {
            val result = runCatching {
                searchOpenStreetMap(
                    query = query,
                    userAgent = "${context.packageName}/1.0 Android",
                )
            }
            searching = false

            val places = result.getOrElse {
                toast("Search failed. Try again in a moment.")
                emptyList()
            }
            if (places.isEmpty()) {
                searchResults = emptyList()
                toast("No places found")
                BackendClient.trackEvent(
                    context,
                    "location_search",
                    mapOf("resultCount" to 0),
                )
                return@launch
            }

            searchResults = places
            BackendClient.trackEvent(
                context,
                "location_search",
                mapOf("resultCount" to places.size),
            )
        }
    }

    fun maybeStartTimerIfAlreadyInside(
        zone: GeoPoint,
        radiusMeters: Float,
        durationMin: Int,
        onChecked: (Boolean) -> Unit,
    ) {
        if (!hasFineLocation(context)) {
            onChecked(false)
            return
        }

        fetchCurrentLocation { location ->
            if (location == null) {
                onChecked(false)
                return@fetchCurrentLocation
            }

            val current = GeoPoint(location.latitude, location.longitude)
            val inside = distanceMeters(current, zone) <= radiusMeters
            if (inside && !TimerController.isRunning(context)) {
                TimerController.startTimer(context, durationMin)
                timerEnd = Prefs.getTimerEnd(context)
                onChecked(true)
            } else {
                onChecked(false)
            }
        }
    }

    DisposableEffect(Unit) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selectGeofencePoint(
                        point = GeoPoint(p.latitude, p.longitude),
                        label = "Dropped pin",
                        analyticsSource = "map_long_press",
                    )
                }
                return true
            }
        })
        mapView.overlays.add(eventsOverlay)
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // Show the blue "you are here" dot once location permission is granted.
    LaunchedEffect(permVersion) {
        if (hasFineLocation(context) && overlays.myLocation == null) {
            val o = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            o.enableMyLocation()
            mapView.overlays.add(o)
            overlays.myLocation = o
            mapView.invalidate()
        }
        if (locateAfterPermission) {
            locateAfterPermission = false
            if (hasFineLocation(context)) requestCurrentLocation(selectAsZone = true)
        }
        if (centerAfterStartupPermission) {
            centerAfterStartupPermission = false
            if (hasFineLocation(context) && !Prefs.hasPlace(context)) {
                requestCurrentLocation(selectAsZone = false, showErrors = false)
            }
        }
    }

    // Redraw the pin + radius circle whenever they change.
    LaunchedEffect(pin, radius) {
        overlays.marker?.let { mapView.overlays.remove(it) }
        overlays.circle?.let { mapView.overlays.remove(it) }
        overlays.marker = null
        overlays.circle = null
        pin?.let { p ->
            val circle = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(p, radius.coerceIn(50f, 500f).toDouble())
                outlinePaint.color = Color.argb(220, 0, 107, 94)
                outlinePaint.strokeWidth = 3f
                fillPaint.color = Color.argb(44, 0, 107, 94)
            }
            val marker = Marker(mapView).apply {
                position = p
                icon = ContextCompat.getDrawable(context, R.drawable.ic_zone_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Timer zone"
            }
            mapView.overlays.add(circle)
            mapView.overlays.add(marker)
            overlays.circle = circle
            overlays.marker = marker
        }
        mapView.invalidate()
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
            !hasFineLocation(context) || !hasNotifications(context) -> {
                val perms = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= 33) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(perms.toTypedArray())
                toast("Grant the permissions, then tap Arm again")
            }
            !hasBackgroundLocation(context) -> {
                showBackgroundLocationDisclosure = true
            }
            !canExactAlarm(context) -> {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}")
                    )
                )
                toast("Allow exact alarms, then tap Arm again")
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
                GeofenceManager.arm(context, p.latitude, p.longitude, radius) { ok, err ->
                    armed = ok
                    if (!ok) {
                        toast("Failed to arm geofence: ${err ?: "unknown error"}")
                        return@arm
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
                    maybeStartTimerIfAlreadyInside(p, radius, durationMin) { started ->
                        if (started) {
                            scope.launch {
                                BackendClient.trackEvent(
                                    context,
                                    "timer_auto_started",
                                    mapOf("durationMinutes" to durationMin),
                                )
                            }
                        }
                        toast(
                            if (started) "You are already here - timer started"
                            else "Armed - the timer will start when you arrive"
                        )
                    }
                }
            }
        }
    }

    val timerActive = timerEnd > now
    val durationMinutes = parseDurationMinutes() ?: Prefs.getDurationMinutes(context)
    val statusTitle = when {
        timerActive -> "Timer running"
        armed -> "Geofence armed"
        else -> "Ready to set"
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
        armed -> "Waiting for arrival at the selected place"
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
            TimerController.startTimer(context, durationMin)
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
                    "Dwell collects location data to enable arrival detection, timer starts, and leave-zone prompts even when the app is closed or not in use. Your selected zone may be stored with Dwell to sync your timer experience. Location is not used for ads."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationDisclosure = false
                        if (Build.VERSION.SDK_INT >= 29) {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            toast("Choose \"Allow all the time\" so arrival works in the background, then tap Arm again")
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
            exactAlarmAllowed = canExactAlarm(context),
            onBack = { route = AppRoute.Home },
            onRadiusChange = {
                radius = it
            },
            onRadiusChangeFinished = {
                if (armed) {
                    radius = Prefs.getRadius(context)
                    toast("Disarm the zone before changing its radius.")
                    return@SettingsScreen
                }
                Prefs.setRadius(context, radius)
                WearSync.pushState(context)
                syncSelectedZone(isArmed = false)
            },
            onDurationChange = { persistDurationText(it) },
            onDurationPreset = { hours ->
                durationText = formatHoursInput(hours)
                Prefs.setDurationMinutes(context, (hours * 60).roundToInt())
                WearSync.pushState(context)
                syncSelectedZone(isArmed = armed)
            },
            onOpenAppSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
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
                    }
                    Notifications.clearAll(context)
                    Prefs.clearAppData(context, keepSession = true)
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
                    }
                    Notifications.clearAll(context)
                    Prefs.clearAppData(context, keepSession = false)
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
            hasZone = pin != null,
            placeLabel = placeLabel,
            radius = radius,
            durationMinutes = durationMinutes,
            armed = armed,
            timerActive = timerActive,
            onBack = { route = AppRoute.Home },
            onCreateZone = {
                route = AppRoute.Home
                activeSheet = ActiveSheet.Search
            },
            onViewZone = {
                route = AppRoute.Home
                pin?.let { centerMapOn(it) }
            },
            onEditZone = {
                route = AppRoute.Home
                activeSheet = ActiveSheet.ZoneSetup
            },
            onDeleteZone = {
                scope.launch {
                    val deleted = BackendClient.deletePrimaryZone(context) != null
                    if (!deleted) {
                        toast("Could not remove server zone. Check connection and try again.")
                        return@launch
                    }

                    GeofenceManager.disarm(context) { armed = false }
                    Prefs.clearPlace(context)
                    Notifications.clearExitQuestion(context)
                    WearSync.pushState(context)
                    BackendClient.trackEvent(context, "zone_deleted")
                    pin = null
                    selectedPlaceLabel = ""
                    route = AppRoute.Home
                    toast("Saved zone removed")
                }
            },
        )
        AppRoute.Home -> {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                MapHeader(
                    placeLabel = placeLabel,
                    onSearchClick = { activeSheet = ActiveSheet.Search },
                    onSettingsClick = { route = AppRoute.Settings },
                )

                MapActionRail(
                    locating = locating,
                    hasPin = pin != null,
                    onCurrentLocation = {
                        if (!locating) requestCurrentLocation(selectAsZone = true)
                    },
                    onSavedZones = { route = AppRoute.SavedZones },
                    onZoneSetup = { activeSheet = ActiveSheet.ZoneSetup },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )

                AttributionPill(Modifier.align(Alignment.BottomStart))

                HomeStatusCard(
                    statusTitle = statusTitle,
                    statusDetail = statusDetail,
                    placeLabel = placeLabel,
                    radius = radius,
                    durationMinutes = durationMinutes,
                    timerActive = timerActive,
                    armed = armed,
                    onConfigure = { activeSheet = ActiveSheet.ZoneSetup },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                if (activeSheet != null) {
                    ModalBottomSheet(
                        onDismissRequest = { activeSheet = null },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        when (activeSheet) {
                            ActiveSheet.Search -> SearchLocationSheet(
                                searchText = searchText,
                                searchResults = searchResults,
                                searching = searching,
                                hasSavedZone = pin != null,
                                savedPlaceLabel = placeLabel,
                                onSearchTextChange = { searchText = it },
                                onSubmit = { performSearch() },
                                onUseCurrentLocation = {
                                    activeSheet = null
                                    requestCurrentLocation(selectAsZone = true)
                                },
                                onSelectSaved = {
                                    activeSheet = null
                                    pin?.let { centerMapOn(it) }
                                },
                                onSelectResult = { result ->
                                    searchResults = emptyList()
                                    selectGeofencePoint(
                                        point = result.point,
                                        label = result.label,
                                        analyticsSource = "search_result",
                                    )
                                },
                            )
                            ActiveSheet.ZoneSetup -> ZoneSetupSheet(
                                title = statusTitle,
                                detail = statusDetail,
                                placeLabel = placeLabel,
                                radius = radius,
                                durationText = durationText,
                                timerActive = timerActive,
                                armed = armed,
                                onSearchClick = { activeSheet = ActiveSheet.Search },
                                onUseCurrentLocation = { requestCurrentLocation(selectAsZone = true) },
                                onRadiusChange = { radius = it },
                                onRadiusChangeFinished = {
                                    if (armed) {
                                        radius = Prefs.getRadius(context)
                                        toast("Disarm the zone before changing its radius.")
                                        return@ZoneSetupSheet
                                    }
                                    Prefs.setRadius(context, radius)
                                    WearSync.pushState(context)
                                    syncSelectedZone(isArmed = false)
                                    pin?.let { fitMapToBoundary(it, radius) }
                                },
                                onDurationChange = { persistDurationText(it) },
                                onDurationPreset = { hours ->
                                    durationText = formatHoursInput(hours)
                                    Prefs.setDurationMinutes(context, (hours * 60).roundToInt())
                                    WearSync.pushState(context)
                                    syncSelectedZone(isArmed = armed)
                                },
                                onArmClick = {
                                    if (armed) {
                                        GeofenceManager.disarm(context) { ok ->
                                            armed = !ok
                                            if (ok) {
                                                syncSelectedZone(isArmed = false)
                                                scope.launch {
                                                    BackendClient.trackEvent(context, "geofence_disarmed")
                                                }
                                            }
                                        }
                                    } else {
                                        armGeofence()
                                    }
                                },
                                onTimerClick = { startOrCancelTimer() },
                            )
                            null -> Unit
                        }
                    }
                }
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
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Dwell",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSearchClick),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (placeLabel == "No place selected") "Search place" else placeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        SmallFloatingActionButton(
            onClick = onSettingsClick,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun MapActionRail(
    locating: Boolean,
    hasPin: Boolean,
    onCurrentLocation: () -> Unit,
    onSavedZones: () -> Unit,
    onZoneSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmallFloatingActionButton(
            onClick = onCurrentLocation,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            if (locating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = "Current location")
            }
        }
        SmallFloatingActionButton(
            onClick = onSavedZones,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(Icons.Filled.Bookmark, contentDescription = "Saved zones")
        }
        SmallFloatingActionButton(
            onClick = onZoneSetup,
            containerColor = if (hasPin) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (hasPin) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ) {
            Icon(Icons.Filled.Timer, contentDescription = "Zone setup")
        }
    }
}

@Composable
private fun AttributionPill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(start = 10.dp, bottom = 158.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
    ) {
        Text(
            "OpenStreetMap",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun HomeStatusCard(
    statusTitle: String,
    statusDetail: String,
    placeLabel: String,
    radius: Float,
    durationMinutes: Int,
    timerActive: Boolean,
    armed: Boolean,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusDot(
                timerActive = timerActive,
                armed = armed,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    statusDetail,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${radius.roundToInt()} m radius | ${Notifications.formatDuration(durationMinutes)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (placeLabel != "No place selected") {
                    Text(
                        placeLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            TextButton(onClick = onConfigure) {
                Text(if (placeLabel == "No place selected") "Set" else "Edit")
            }
        }
    }
}

@Composable
private fun SearchLocationSheet(
    searchText: String,
    searchResults: List<LocationSearchResult>,
    searching: Boolean,
    hasSavedZone: Boolean,
    savedPlaceLabel: String,
    onSearchTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSelectSaved: () -> Unit,
    onSelectResult: (LocationSearchResult) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                label = { Text("Place or address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSubmit,
                enabled = !searching && searchText.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUseCurrentLocation) {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Current")
            }
            if (hasSavedZone) {
                OutlinedButton(onClick = onSelectSaved) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saved")
                }
            }
        }

        if (hasSavedZone) {
            SearchRow(
                icon = Icons.Filled.Bookmark,
                title = "Saved zone",
                subtitle = savedPlaceLabel,
                onClick = onSelectSaved,
            )
        }

        if (searching) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Searching places")
            }
        } else if (searchResults.isEmpty()) {
            Text(
                "Results appear here after search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            searchResults.forEach { result ->
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

@Composable
private fun SearchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
}

@Composable
private fun ZoneSetupSheet(
    title: String,
    detail: String,
    placeLabel: String,
    radius: Float,
    durationText: String,
    timerActive: Boolean,
    armed: Boolean,
    onSearchClick: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
    onDurationChange: (String) -> Unit,
    onDurationPreset: (Double) -> Unit,
    onArmClick: () -> Unit,
    onTimerClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusHeader(
            title = title,
            detail = detail,
            timerActive = timerActive,
            armed = armed,
        )

        InfoRow(
            icon = {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            },
            label = "Place",
            value = placeLabel,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSearchClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search")
            }
            OutlinedButton(onClick = onUseCurrentLocation, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Current")
            }
        }

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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onArmClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 13.dp),
            ) {
                Icon(
                    if (armed) Icons.Filled.Cancel else Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (armed) "Disarm" else "Arm zone")
            }

            FilledTonalButton(
                onClick = onTimerClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 13.dp),
            ) {
                Icon(
                    if (timerActive) Icons.Filled.Cancel else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (timerActive) "Cancel" else "Start now")
            }
        }
    }
}

@Composable
private fun StatusHeader(
    title: String,
    detail: String,
    timerActive: Boolean,
    armed: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(timerActive = timerActive, armed = armed)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                    Icons.Filled.Timer,
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
            valueRange = 50f..500f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun InfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    exactAlarmAllowed: Boolean,
    onBack: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onRadiusChangeFinished: () -> Unit,
    onDurationChange: (String) -> Unit,
    onDurationPreset: (Double) -> Unit,
    onOpenAppSettings: () -> Unit,
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
            PermissionRow("Exact alarms", exactAlarmAllowed)
            OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Android settings")
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
private fun SavedZonesScreen(
    hasZone: Boolean,
    placeLabel: String,
    radius: Float,
    durationMinutes: Int,
    armed: Boolean,
    timerActive: Boolean,
    onBack: () -> Unit,
    onCreateZone: () -> Unit,
    onViewZone: () -> Unit,
    onEditZone: () -> Unit,
    onDeleteZone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(title = "Saved zones", onBack = onBack)

        if (!hasZone) {
            EmptyState(
                title = "No saved zones",
                detail = "Create a zone from search, current location, or the map.",
                actionLabel = "Create zone",
                onAction = onCreateZone,
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(timerActive = timerActive, armed = armed)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Primary zone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                placeLabel,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "${radius.roundToInt()} m radius | ${Notifications.formatDuration(durationMinutes)} timer",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onViewZone, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("View")
                        }
                        FilledTonalButton(onClick = onEditZone, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit")
                        }
                    }
                    OutlinedButton(
                        onClick = onDeleteZone,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove saved zone")
                    }
                }
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
