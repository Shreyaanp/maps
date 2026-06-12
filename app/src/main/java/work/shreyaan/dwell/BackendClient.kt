package work.shreyaan.dwell

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class BackendZone(
    val label: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Float,
    val durationMinutes: Int,
)

object BackendClient {
    private const val TAG = "DwellBackend"
    private val baseUrl: String = BuildConfig.DWELL_API_BASE_URL.trimEnd('/')

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
                radiusMeters = radius.toFloat(),
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
            put("radiusMeters", radiusMeters.toDouble())
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
            put("properties", JSONObject().apply {
                properties.forEach { (key, value) ->
                    put(key, value)
                }
            })
        },
    )

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
    ): String? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            Log.w(TAG, "Backend base URL is blank; skipping $method $path")
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
                    Log.w(TAG, "$method $path failed with HTTP ${conn.responseCode}: $response")
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.onFailure { e ->
            Log.w(TAG, "$method $path failed", e)
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
