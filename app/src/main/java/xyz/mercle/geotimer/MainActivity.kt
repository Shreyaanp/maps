package xyz.mercle.geotimer

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
        Notifications.ensureChannels(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    GeoTimerScreen()
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

private class ZoneOverlays {
    var marker: Marker? = null
    var circle: Polygon? = null
    var myLocation: MyLocationNewOverlay? = null
}

@Composable
fun GeoTimerScreen() {
    val context = LocalContext.current

    var pin by remember {
        mutableStateOf(
            if (Prefs.hasPlace(context))
                GeoPoint(Prefs.getLat(context), Prefs.getLon(context))
            else null
        )
    }
    var radius by remember { mutableStateOf(Prefs.getRadius(context)) }
    var durationText by remember {
        val h = Prefs.getDurationMinutes(context) / 60.0
        val rounded = (Math.round(h * 100) / 100.0).toString()
            .trimEnd('0').trimEnd('.')
        mutableStateOf(rounded.ifEmpty { "4.5" })
    }
    var armed by remember { mutableStateOf(Prefs.isArmed(context)) }
    var timerEnd by remember { mutableLongStateOf(Prefs.getTimerEnd(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var permVersion by remember { mutableIntStateOf(0) }

    // Tick every second: drives the countdown and picks up changes made by
    // the broadcast receivers (timer started by geofence, cancelled, etc.)
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            timerEnd = Prefs.getTimerEnd(context)
            armed = Prefs.isArmed(context)
            delay(1000)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permVersion++ }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permVersion++ }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(if (Prefs.hasPlace(context)) 16.0 else 3.0)
            controller.setCenter(pin ?: GeoPoint(20.0, 0.0))
        }
    }
    val overlays = remember { ZoneOverlays() }

    DisposableEffect(Unit) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    pin = GeoPoint(p.latitude, p.longitude)
                    // Persist immediately so the pin survives activity recreation.
                    Prefs.savePlace(context, p.latitude, p.longitude)
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
    }

    // Redraw the pin + radius circle whenever they change.
    LaunchedEffect(pin, radius) {
        overlays.marker?.let { mapView.overlays.remove(it) }
        overlays.circle?.let { mapView.overlays.remove(it) }
        overlays.marker = null
        overlays.circle = null
        pin?.let { p ->
            val circle = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(p, radius.toDouble())
                outlinePaint.color = Color.argb(200, 26, 115, 232)
                outlinePaint.strokeWidth = 3f
                fillPaint.color = Color.argb(40, 26, 115, 232)
            }
            val marker = Marker(mapView).apply {
                position = p
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

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun parseDurationMinutes(): Int? {
        val hours = durationText.toDoubleOrNull() ?: return null
        if (hours <= 0 || hours > 48) return null
        return (hours * 60).roundToInt().coerceAtLeast(1)
    }

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
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                toast("Choose \"Allow all the time\" so arrival works in the background, then tap Arm again")
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
                Prefs.savePlace(context, p.latitude, p.longitude)
                Prefs.setRadius(context, radius)
                Prefs.setDurationMinutes(context, durationMin)
                GeofenceManager.arm(context, p.latitude, p.longitude, radius) { ok, err ->
                    armed = ok
                    toast(
                        if (ok) "Armed — the timer will start when you arrive"
                        else "Failed to arm geofence: ${err ?: "unknown error"}"
                    )
                }
            }
        }
    }

    val timerActive = timerEnd > now
    val statusText = when {
        timerActive -> {
            val left = timerEnd - now
            val h = left / 3_600_000
            val m = (left / 60_000) % 60
            val s = (left / 1000) % 60
            val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timerEnd))
            "Timer running: ${h}h ${m}m ${s}s left (ends $endsAt)"
        }
        armed -> "Armed — waiting for you to arrive at the pin"
        else -> "Not armed. Long-press the map to set your place."
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(statusText, style = MaterialTheme.typography.bodyLarge)
            Text("Radius: ${radius.roundToInt()} m")
            Slider(
                value = radius,
                onValueChange = { radius = it },
                onValueChangeFinished = { Prefs.setRadius(context, radius) },
                valueRange = 50f..500f
            )
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it },
                label = { Text("Timer duration (hours)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (armed) {
                            GeofenceManager.disarm(context) { armed = !it }
                        } else {
                            armGeofence()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (armed) "Disarm" else "Arm geofence")
                }
                OutlinedButton(
                    onClick = {
                        if (timerActive) {
                            TimerController.cancelTimer(context)
                            Notifications.notifyTimerCancelled(context)
                            timerEnd = 0L
                        } else {
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
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (timerActive) "Cancel timer" else "Start timer now")
                }
            }
        }
    }
}
