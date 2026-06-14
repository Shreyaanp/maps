package work.shreyaan.dwell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DwellDiagnosticEntry(
    val happenedAt: Long,
    val source: String,
    val decision: String,
    val score: Int?,
    val detail: String,
) {
    fun label(): String {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(happenedAt))
        val scoreText = score?.let { " score $it" }.orEmpty()
        return "$time - $source - $decision$scoreText - $detail"
    }
}

data class DwellDiagnosticSnapshot(
    val source: String,
    val confidence: ArrivalConfidence,
    val distanceMeters: Float?,
    val accuracyMeters: Float?,
    val locationAgeMs: Long?,
    val speedMetersPerSecond: Float?,
    val observedInsideDurationMs: Long?,
    val motion: DwellMotion,
    val geofenceEnter: Boolean,
    val alreadyInsideCheck: Boolean,
)

object DwellDiagnostics {
    private const val TAG = "DwellDiagnostics"
    private const val KEY = "diagnostics_log"
    private const val MAX_ENTRIES = 18
    private val NumericFieldRegex =
        Regex("""(?i)(["']?[A-Za-z][A-Za-z0-9_.-]*["']?)\s*(?:[:=]|\s)\s*-?\d{1,3}(?:\.\d+)?""")
    private val CoordinatePairRegex =
        Regex("""\b-?\d{1,3}\.\d{4,}\s*,\s*-?\d{1,3}\.\d{4,}\b""")
    private val UrlRegex = Regex("""https?://\S+""")

    fun logArrival(context: Context, snapshot: DwellDiagnosticSnapshot) {
        add(
            context,
            DwellDiagnosticEntry(
                happenedAt = System.currentTimeMillis(),
                source = snapshot.source,
                decision = snapshot.confidence.decision.name.lowercase(Locale.ROOT),
                score = snapshot.confidence.score,
                detail = buildList {
                    add("distance ${bucketDistance(snapshot.distanceMeters)}")
                    add("accuracy ${bucketAccuracy(snapshot.accuracyMeters)}")
                    add("age ${bucketAge(snapshot.locationAgeMs)}")
                    add("speed ${bucketSpeed(snapshot.speedMetersPerSecond)}")
                    add("inside ${bucketDuration(snapshot.observedInsideDurationMs)}")
                    add("motion ${snapshot.motion.name.lowercase(Locale.ROOT)}")
                    if (snapshot.geofenceEnter) add("geofence")
                    if (snapshot.alreadyInsideCheck) add("armed-inside")
                }.joinToString(", "),
            )
        )
    }

    fun logExitPrompt(
        context: Context,
        prompted: Boolean,
        distanceMeters: Float?,
        accuracyMeters: Float?,
        locationAgeMs: Long?,
        motion: DwellMotion,
    ) {
        add(
            context,
            DwellDiagnosticEntry(
                happenedAt = System.currentTimeMillis(),
                source = "exit",
                decision = if (prompted) "prompt" else "suppress",
                score = null,
                detail = buildList {
                    add("distance ${bucketDistance(distanceMeters)}")
                    add("accuracy ${bucketAccuracy(accuracyMeters)}")
                    add("age ${bucketAge(locationAgeMs)}")
                    add("motion ${motion.name.lowercase(Locale.ROOT)}")
                }.joinToString(", "),
            )
        )
    }

    fun logLifecycle(
        context: Context,
        source: String,
        decision: String,
        detail: String,
    ) {
        add(
            context,
            DwellDiagnosticEntry(
                happenedAt = System.currentTimeMillis(),
                source = source.take(32),
                decision = decision.take(32),
                score = null,
                detail = sanitizeDiagnosticText(detail).take(180),
            ),
        )
    }

    fun entries(context: Context): List<DwellDiagnosticEntry> =
        decode(context.getSharedPreferences("dwell", Context.MODE_PRIVATE).getString(KEY, null))

    fun exportText(context: Context, entries: List<DwellDiagnosticEntry>): String =
        exportText(entries, fieldContextLines(context), System.currentTimeMillis())

    fun exportText(entries: List<DwellDiagnosticEntry>): String =
        exportText(entries, emptyList(), System.currentTimeMillis())

    internal fun exportText(
        entries: List<DwellDiagnosticEntry>,
        contextLines: List<String>,
        generatedAtMillis: Long = System.currentTimeMillis(),
    ): String =
        buildString {
            appendLine("Dwell diagnostics")
            appendLine("Generated UTC: ${formatExportTimestamp(generatedAtMillis)}")
            appendLine("No coordinates are included.")
            if (contextLines.isNotEmpty()) {
                appendLine()
                appendLine("Context")
                contextLines.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("Recent decisions")
            if (entries.isEmpty()) {
                appendLine("- No recent decisions")
            } else {
                entries.forEach { entry ->
                    appendLine("- ${entry.copy(detail = sanitizeDiagnosticText(entry.detail)).label()}")
                }
            }
        }.trimEnd()

    internal fun providerContextLines(
        searchConfig: MobileSearchConfig,
        mapConfig: MobileMapConfig,
    ): List<String> =
        listOf(
            "map host=${hostForDiagnostics(mapConfig.styleUrl)} attributionOsm=${mapConfig.attributionLabel.contains("OpenStreetMap", ignoreCase = true)}",
            "search host=${hostForDiagnostics(searchConfig.baseUrl)} networkAutocomplete=${searchConfig.networkAutocomplete}",
        )

    fun clear(context: Context) {
        context.getSharedPreferences("dwell", Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }

    private fun add(context: Context, entry: DwellDiagnosticEntry) {
        val prefs = context.getSharedPreferences("dwell", Context.MODE_PRIVATE)
        val safeEntry = entry.copy(detail = sanitizeDiagnosticText(entry.detail))
        val next = (listOf(safeEntry) + entries(context)).take(MAX_ENTRIES)
        prefs.edit().putString(KEY, encode(next)).apply()
        Log.i(TAG, safeEntry.label())
    }

    internal fun sanitizeDiagnosticText(text: String): String =
        text
            .replace(UrlRegex, "[url]")
            .replace(NumericFieldRegex) { matchResult ->
                val key = matchResult.groupValues[1]
                if (isCoordinateLikeDiagnosticKey(key)) {
                    "$key=[coordinate]"
                } else {
                    matchResult.value
                }
            }
            .replace(CoordinatePairRegex, "[coordinates]")

    private fun isCoordinateLikeDiagnosticKey(key: String): Boolean {
        val normalized = key
            .trim()
            .trim('"', '\'')
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase(Locale.ROOT)
        if (normalized.isBlank()) return false

        val coordinateParts = setOf(
            "lat",
            "latitude",
            "lon",
            "lng",
            "long",
            "longitude",
            "coordinate",
            "coordinates",
            "coord",
            "coords",
        )
        return normalized
            .split("_")
            .filter { it.isNotBlank() }
            .any { it in coordinateParts }
    }

    private fun formatExportTimestamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))

    private fun encode(entries: List<DwellDiagnosticEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("at", entry.happenedAt)
                    .put("source", entry.source)
                    .put("decision", entry.decision)
                    .put("score", entry.score ?: JSONObject.NULL)
                    .put("detail", entry.detail)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<DwellDiagnosticEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        DwellDiagnosticEntry(
                            happenedAt = item.optLong("at", 0L),
                            source = item.optString("source", "engine"),
                            decision = item.optString("decision", "unknown"),
                            score = if (item.isNull("score")) null else item.optInt("score"),
                            detail = item.optString("detail", ""),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun fieldContextLines(context: Context): List<String> {
        val battery = BatteryReliability.status(context)
        val monitoredCount = Prefs.getArmedPlaces(context).size
        val registeredCount = Prefs.getRegisteredPlaceIds(context).size
        return listOf(
            "app ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            "device ${Build.MANUFACTURER} ${Build.MODEL}, sdk ${Build.VERSION.SDK_INT}",
            "permissions location=${permissionState(context, Manifest.permission.ACCESS_FINE_LOCATION)} background=${backgroundLocationState(context)} notifications=${notificationState(context)} motion=${motionState(context)}",
            "monitoring monitored=$monitoredCount registered=$registeredCount timer=${if (TimerController.isRunning(context)) "running" else "idle"}",
            "battery ${battery.label}, aggressiveOem=${battery.isKnownAggressiveOem}",
        ) + providerContextLines(
            searchConfig = Prefs.getMobileSearchConfig(context),
            mapConfig = Prefs.getMobileMapConfig(context),
        )
    }

    private fun hostForDiagnostics(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return "unknown"
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return runCatching {
            URI(withScheme).host.orEmpty()
        }.getOrDefault("")
            .trimEnd('.')
            .lowercase(Locale.ROOT)
            .ifBlank { "unknown" }
    }

    private fun permissionState(context: Context, permission: String): String =
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            "on"
        } else {
            "off"
        }

    private fun backgroundLocationState(context: Context): String =
        if (Build.VERSION.SDK_INT < 29) {
            "on"
        } else {
            permissionState(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

    private fun notificationState(context: Context): String =
        if (Build.VERSION.SDK_INT < 33) {
            "on"
        } else {
            permissionState(context, Manifest.permission.POST_NOTIFICATIONS)
        }

    private fun motionState(context: Context): String =
        if (ActivityRecognitionManager.hasPermission(context)) "on" else "off"

    private fun bucketDistance(value: Float?): String =
        when {
            value == null -> "unknown"
            value < 50f -> "<50m"
            value < 150f -> "50-150m"
            value < 300f -> "150-300m"
            else -> "300m+"
        }

    private fun bucketAccuracy(value: Float?): String =
        when {
            value == null -> "unknown"
            value <= 35f -> "<=35m"
            value <= 100f -> "35-100m"
            value <= 250f -> "100-250m"
            else -> "250m+"
        }

    private fun bucketAge(value: Long?): String =
        when {
            value == null -> "unknown"
            value <= 15_000L -> "<=15s"
            value <= 60_000L -> "15-60s"
            else -> "60s+"
        }

    private fun bucketSpeed(value: Float?): String =
        when {
            value == null -> "unknown"
            value <= 0.8f -> "still"
            value <= 2.4f -> "walking"
            value <= 6f -> "moving"
            else -> "fast"
        }

    private fun bucketDuration(value: Long?): String =
        when {
            value == null -> "unknown"
            value <= 20_000L -> "<=20s"
            value <= 60_000L -> "20-60s"
            value <= 120_000L -> "1-2m"
            else -> "2m+"
        }
}
