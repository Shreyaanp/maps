package work.shreyaan.dwell

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.util.Locale

private fun hostForMobilePolicy(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    return runCatching {
        URI(withScheme).host.orEmpty()
    }.getOrDefault("")
        .trimEnd('.')
        .lowercase(Locale.ROOT)
}

private fun isKnownPaidMapOrSearchHost(host: String): Boolean =
    host == "api.mapbox.com" ||
        host.endsWith(".mapbox.com") ||
        host == "maps.googleapis.com" ||
        host == "places.googleapis.com" ||
        host.endsWith(".googleapis.com") ||
        host == "maps.google.com" ||
        host == "www.google.com"

data class BackendZone(
    val label: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Float,
    val durationMinutes: Int,
)

data class MobileSearchConfig(
    val baseUrl: String,
    val userAgent: String,
    val networkAutocomplete: Boolean = false,
) {
    companion object {
        fun defaults(): MobileSearchConfig =
            MobileSearchConfig(
                baseUrl = BuildConfig.NOMINATIM_BASE_URL,
                userAgent = BuildConfig.NOMINATIM_USER_AGENT,
            )

        fun shouldAllowNetworkAutocomplete(
            baseUrl: String,
            requestedAutocomplete: Boolean,
        ): Boolean =
            requestedAutocomplete &&
                !isPublicNominatimEndpoint(baseUrl) &&
                !isKnownPaidMapOrSearchHost(hostForMobilePolicy(baseUrl))

        fun isAllowedBaseUrl(baseUrl: String): Boolean {
            val trimmed = baseUrl.trim()
            return trimmed.startsWith("https://") &&
                !isKnownPaidMapOrSearchHost(hostForMobilePolicy(trimmed))
        }

        fun searchEndpoint(baseUrl: String): String {
            val normalized = baseUrl
                .trim()
                .trimEnd('/')
                .ifBlank { "https://nominatim.openstreetmap.org" }
            return if (normalized.substringAfterLast('/') == "search") {
                normalized
            } else {
                "$normalized/search"
            }
        }

        private fun isPublicNominatimEndpoint(baseUrl: String): Boolean {
            return hostForMobilePolicy(baseUrl) == "nominatim.openstreetmap.org"
        }
    }
}

data class MobileMapConfig(
    val styleUrl: String,
    val attributionLabel: String,
) {
    companion object {
        private const val REQUIRED_OSM_ATTRIBUTION = "OpenStreetMap"

        fun defaults(): MobileMapConfig =
            MobileMapConfig(
                styleUrl = BuildConfig.MAP_STYLE_URL,
                attributionLabel = "OpenFreeMap | OpenStreetMap",
            )

        fun isAllowedStyleUrl(styleUrl: String): Boolean {
            val trimmed = styleUrl.trim()
            if (!trimmed.startsWith("https://") || !trimmed.contains("/styles/")) return false
            val host = hostForMobilePolicy(trimmed)
            return !isPublicOsmTileHost(host) &&
                !isKnownPaidMapOrSearchHost(host)
        }

        fun normalizeAttribution(label: String): String {
            val trimmed = label.trim()
            val base = trimmed.takeIf { it.isNotBlank() } ?: defaults().attributionLabel
            return if (base.contains(REQUIRED_OSM_ATTRIBUTION, ignoreCase = true)) {
                base
            } else {
                "$base | $REQUIRED_OSM_ATTRIBUTION"
            }
        }

        private fun isPublicOsmTileHost(host: String): Boolean {
            return host == "tile.openstreetmap.org" ||
                host.endsWith(".tile.openstreetmap.org")
        }
    }
}

data class MobileConfig(
    val search: MobileSearchConfig = MobileSearchConfig.defaults(),
    val map: MobileMapConfig = MobileMapConfig.defaults(),
)

object BackendClient {
    private const val TAG = "DwellBackend"
    private val baseUrl: String = BuildConfig.DWELL_API_BASE_URL.trimEnd('/')

    suspend fun loadMobileConfig(context: Context): MobileConfig? {
        val response = requestQuietly(
            context = context,
            method = "GET",
            path = "/api/mobile/config",
            body = null,
            logFailures = false,
        ) ?: return null

        return runCatching {
            MobileConfig(
                search = parseSearchConfig(JSONObject(response)),
                map = parseMapConfig(JSONObject(response)),
            )
        }.getOrNull()
    }

    suspend fun loadMobileSearchConfig(context: Context): MobileSearchConfig? =
        loadMobileConfig(context)?.search

    private fun parseSearchConfig(json: JSONObject): MobileSearchConfig {
        val fallback = MobileSearchConfig.defaults()
        return runCatching {
            val search = json.optJSONObject("search") ?: return@runCatching fallback
            val provider = search.optString("provider")
            val autocomplete = search.optBoolean("autocomplete", false)
            val config = MobileSearchConfig(
                baseUrl = search.optString("baseUrl").trim().trimEnd('/'),
                userAgent = search.optString("userAgent").trim(),
                networkAutocomplete = MobileSearchConfig.shouldAllowNetworkAutocomplete(
                    baseUrl = search.optString("baseUrl"),
                    requestedAutocomplete = autocomplete,
                ),
            )
            if (
                provider != "nominatim" ||
                !MobileSearchConfig.isAllowedBaseUrl(config.baseUrl) ||
                config.userAgent.length < 12
            ) {
                return@runCatching fallback
            }
            config
        }.getOrDefault(fallback)
    }

    private fun parseMapConfig(json: JSONObject): MobileMapConfig {
        val fallback = MobileMapConfig.defaults()
        return runCatching {
            val map = json.optJSONObject("map") ?: return@runCatching fallback
            val provider = map.optString("provider")
            val config = MobileMapConfig(
                styleUrl = map.optString("styleUrl").trim(),
                attributionLabel = MobileMapConfig.normalizeAttribution(
                    map.optString("attributionLabel"),
                ),
            )
            if (
                provider != "maplibre" ||
                !MobileMapConfig.isAllowedStyleUrl(config.styleUrl)
            ) {
                return@runCatching fallback
            }
            config
        }.getOrDefault(fallback)
    }

    suspend fun upsertSession(
        context: Context,
        provider: String,
        displayName: String = "",
        email: String = "",
        googleSubject: String = "",
        googleIdToken: String = "",
    ): Boolean {
        val response = postQuietly(
            context = context,
            path = "/api/mobile/session",
            body = JSONObject().apply {
                put("provider", provider)
                put("displayName", displayName)
                put("email", email)
                put("googleSubject", googleSubject)
                if (googleIdToken.isNotBlank()) {
                    put("googleIdToken", googleIdToken)
                }
            },
        )
        response?.let(::extractSessionToken)?.takeIf { it.isNotBlank() }?.let { token ->
            Prefs.setBackendSessionToken(context, token)
        }
        return response != null
    }

    suspend fun loadPrimaryZone(context: Context): BackendZone? {
        val response = requestQuietly(
            context = context,
            method = "GET",
            path = "/api/mobile/zones",
            body = null,
        ) ?: return null

        return runCatching {
            val zones = JSONObject(response).optJSONArray("zones") ?: return@runCatching null
            val zone = zones.optJSONObject(0) ?: return@runCatching null
            val lat = zone.optDouble("lat", Double.NaN)
            val lon = zone.optDouble("lon", Double.NaN)
            val radius = zone.optDouble("radiusMeters", Double.NaN)
            val duration = zone.optInt("durationMinutes", 0)
            if (
                !lat.isFinite() ||
                !lon.isFinite() ||
                !radius.isFinite() ||
                duration <= 0
            ) {
                return@runCatching null
            }
            BackendZone(
                label = zone.optString("label").ifBlank { "Saved zone" },
                lat = lat,
                lon = lon,
                radiusMeters = DwellRadius.normalize(radius.toFloat()),
                durationMinutes = duration.coerceIn(1, 2_880),
            )
        }.getOrNull()
    }

    suspend fun savePrimaryZone(
        context: Context,
        label: String,
        lat: Double,
        lon: Double,
        radiusMeters: Float,
        durationMinutes: Int,
        armed: Boolean,
    ) = requestQuietly(
        context = context,
        method = "PUT",
        path = "/api/mobile/zones",
        body = JSONObject().apply {
            put("label", label)
            put("lat", lat)
            put("lon", lon)
            put("radiusMeters", DwellRadius.normalize(radiusMeters).toDouble())
            put("durationMinutes", durationMinutes)
            put("armed", armed)
        },
    )

    suspend fun deletePrimaryZone(context: Context) = requestQuietly(
        context = context,
        method = "DELETE",
        path = "/api/mobile/zones",
        body = null,
    )

    suspend fun deleteAppData(context: Context) = requestQuietly(
        context = context,
        method = "DELETE",
        path = "/api/mobile/data",
        body = null,
    )

    suspend fun deleteAccount(context: Context) = requestQuietly(
        context = context,
        method = "DELETE",
        path = "/api/mobile/account",
        body = null,
    )

    suspend fun trackEvent(
        context: Context,
        type: String,
        properties: Map<String, Any?> = emptyMap(),
    ) = postQuietly(
        context = context,
        path = "/api/mobile/events",
        body = JSONObject().apply {
            put("type", type)
            put("timestamp", Instant.now().toString())
            put("properties", sanitizedEventPropertiesJson(properties))
        },
    )

    internal fun sanitizedEventProperties(properties: Map<String, Any?>): Map<String, Any?> =
        properties.mapValues { (key, value) -> sanitizeEventValue(key, value) }

    private fun sanitizedEventPropertiesJson(properties: Map<String, Any?>): JSONObject =
        JSONObject().apply {
            sanitizedEventProperties(properties).forEach { (key, value) ->
                put(key, jsonValue(value))
            }
        }

    private fun sanitizeEventValue(key: String, value: Any?): Any? {
        if (isCoordinateLikeKey(key)) return "[redacted]"
        if (isSensitiveLocationTextKey(key)) return "[redacted]"
        return when (value) {
            is String -> DwellDiagnostics.sanitizeDiagnosticText(value)
            is Map<*, *> -> sanitizedEventProperties(
                value.entries.associate { (nestedKey, nestedValue) ->
                    nestedKey.toString() to nestedValue
                },
            )
            is Iterable<*> -> value.map { item -> sanitizeEventValue("", item) }
            is Array<*> -> value.map { item -> sanitizeEventValue("", item) }
            else -> value
        }
    }

    private fun jsonValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> {
                JSONObject().apply {
                    value.forEach { (nestedKey, nestedValue) ->
                        put(nestedKey.toString(), jsonValue(nestedValue))
                    }
                }
            }
            is Iterable<*> -> {
                JSONArray().apply {
                    value.forEach { item -> put(jsonValue(item)) }
                }
            }
            is Array<*> -> {
                JSONArray().apply {
                    value.forEach { item -> put(jsonValue(item)) }
                }
            }
            else -> value
        }

    private fun isCoordinateLikeKey(key: String): Boolean {
        val normalized = key
            .trim()
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

    private fun isSensitiveLocationTextKey(key: String): Boolean {
        val normalized = key
            .trim()
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase(Locale.ROOT)
        if (normalized.isBlank()) return false

        val parts = normalized
            .split("_")
            .filter { it.isNotBlank() }
        if (parts.any { it in setOf("query", "address") }) return true

        return normalized in setOf(
            "search_text",
            "search_term",
            "search_phrase",
            "label",
            "place_label",
            "place_name",
            "location_label",
            "location_name",
            "result_label",
            "display_name",
        )
    }

    private suspend fun postQuietly(
        context: Context,
        path: String,
        body: JSONObject,
    ) = requestQuietly(context, "POST", path, body)

    private suspend fun requestQuietly(
        context: Context,
        method: String,
        path: String,
        body: JSONObject?,
        logFailures: Boolean = true,
    ): String? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            if (logFailures) {
                Log.w(TAG, "Backend base URL is blank; skipping $method $path")
            }
            return@withContext null
        }

        runCatching {
            val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Dwell-Install-Id", Prefs.getInstallId(context))
                Prefs.getBackendSessionToken(context).takeIf { it.isNotBlank() }?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (body != null) {
                    doOutput = true
                }
            }

            try {
                if (body != null) {
                    OutputStreamWriter(conn.outputStream).use { writer ->
                        writer.write(body.toString())
                    }
                }

                // Drain the response so connections can be reused.
                val stream = if (conn.responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (conn.responseCode in 200..299) {
                    response
                } else {
                    if (logFailures) {
                        Log.w(TAG, "$method $path failed with HTTP ${conn.responseCode}: ${response.take(240)}")
                    }
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.onFailure { e ->
            if (logFailures) {
                Log.w(TAG, "$method $path failed", e)
            }
        }.getOrNull()
    }

    private fun extractSessionToken(response: String): String? {
        if (response.isBlank()) return null
        return runCatching {
            val json = JSONObject(response)
            json.optString("sessionToken")
                .ifBlank { json.optString("token") }
                .ifBlank { json.optString("accessToken") }
        }.getOrNull()
    }
}
