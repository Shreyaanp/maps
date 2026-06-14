package work.shreyaan.dwell

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos

object MapCacheManager {
    private const val TAG = "DwellMapCache"

    private const val AMBIENT_CACHE_BYTES = 128L * 1024L * 1024L
    private const val PREFS_NAME = "dwell_map_cache"
    private const val LAST_PREWARM_KEY = "last_prewarm_key"
    private const val LAST_PREWARM_AT = "last_prewarm_at"
    private const val ZONE_CACHE_KIND = "dwell_primary_zone_cache_v1"
    private const val ZONE_CACHE_MIN_ZOOM = 12.0
    private const val ZONE_CACHE_MAX_ZOOM = 16.0
    private const val MIN_ZONE_CACHE_RADIUS_METERS = 1_000.0
    private const val MAX_ZONE_CACHE_RADIUS_METERS = 2_500.0
    private const val PREWARM_DEDUPE_WINDOW_MS = 15 * 60 * 1_000L

    fun configure(context: Context) {
        OfflineManager.getInstance(context.applicationContext)
            .setMaximumAmbientCacheSize(
                AMBIENT_CACHE_BYTES,
                object : OfflineManager.FileSourceCallback {
                    override fun onSuccess() = Unit

                    override fun onError(message: String) {
                        Log.w(TAG, "Could not configure map tile cache: $message")
                    }
                },
            )
    }

    fun prewarmZone(
        context: Context,
        styleUrl: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (!isValidPrewarmRequest(styleUrl, latitude, longitude)) {
            onComplete?.let { Handler(Looper.getMainLooper()).post { it(false) } }
            return
        }

        val appContext = context.applicationContext
        val prewarmKey = prewarmKeyFor(styleUrl, latitude, longitude, radiusMeters)
        val now = System.currentTimeMillis()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (
            !shouldStartPrewarm(
                lastKey = prefs.getString(LAST_PREWARM_KEY, null),
                lastAtMillis = prefs.getLong(LAST_PREWARM_AT, 0L),
                nextKey = prewarmKey,
                nowMillis = now,
            )
        ) {
            Log.d(TAG, "Skipping repeated zone map cache prewarm")
            onComplete?.let { Handler(Looper.getMainLooper()).post { it(true) } }
            return
        }
        prefs.edit()
            .putString(LAST_PREWARM_KEY, prewarmKey)
            .putLong(LAST_PREWARM_AT, now)
            .apply()

        val manager = OfflineManager.getInstance(appContext)
        val pixelRatio = appContext.resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            zoneBounds(latitude, longitude, radiusMeters),
            ZONE_CACHE_MIN_ZOOM,
            ZONE_CACHE_MAX_ZOOM,
            pixelRatio,
        )
        val metadata = JSONObject()
            .put("kind", ZONE_CACHE_KIND)
            .put("lat", latitude)
            .put("lon", longitude)
            .put("radiusMeters", radiusMeters.toDouble())
            .put("styleUrl", styleUrl)
            .put("createdAt", System.currentTimeMillis())
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        manager.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    deleteDwellZoneRegions(offlineRegions.orEmpty().toList()) {
                        manager.createOfflineRegion(
                            definition,
                            metadata,
                            object : OfflineManager.CreateOfflineRegionCallback {
                                override fun onCreate(offlineRegion: OfflineRegion) {
                                    startZoneDownload(offlineRegion, onComplete)
                                }

                                override fun onError(error: String) {
                                    Log.w(TAG, "Could not create zone map cache: $error")
                                    onComplete?.let { Handler(Looper.getMainLooper()).post { it(false) } }
                                }
                            },
                        )
                    }
                }

                override fun onError(error: String) {
                    Log.w(TAG, "Could not list map cache regions before prewarm: $error")
                    onComplete?.let { Handler(Looper.getMainLooper()).post { it(false) } }
                }
            },
        )
    }

    fun clear(context: Context, onComplete: (Boolean) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        val manager = OfflineManager.getInstance(context.applicationContext)
        manager.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    deleteDwellZoneRegions(offlineRegions.orEmpty().toList()) {
                        clearAmbient(context, mainHandler, onComplete)
                    }
                }

                override fun onError(error: String) {
                    Log.w(TAG, "Could not list map cache regions before clearing: $error")
                    clearAmbient(context, mainHandler, onComplete)
                }
            },
        )
    }

    fun clearZone(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val mainHandler = Handler(Looper.getMainLooper())
        val manager = OfflineManager.getInstance(context.applicationContext)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        manager.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    deleteDwellZoneRegions(offlineRegions.orEmpty().toList()) {
                        onComplete?.let { mainHandler.post { it(true) } }
                    }
                }

                override fun onError(error: String) {
                    Log.w(TAG, "Could not list map cache regions before clearing zone: $error")
                    onComplete?.let { mainHandler.post { it(false) } }
                }
            },
        )
    }

    private fun clearAmbient(
        context: Context,
        mainHandler: Handler,
        onComplete: (Boolean) -> Unit,
    ) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        OfflineManager.getInstance(context.applicationContext)
            .clearAmbientCache(
                object : OfflineManager.FileSourceCallback {
                    override fun onSuccess() {
                        configure(context)
                        mainHandler.post { onComplete(true) }
                    }

                    override fun onError(message: String) {
                        Log.w(TAG, "Could not clear map tile cache: $message")
                        mainHandler.post { onComplete(false) }
                    }
                },
            )
    }

    private fun startZoneDownload(
        region: OfflineRegion,
        onComplete: ((Boolean) -> Unit)?,
    ) {
        val delivered = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())

        fun finish(ok: Boolean) {
            if (!delivered.compareAndSet(false, true)) return
            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
            onComplete?.let { mainHandler.post { it(ok) } }
        }

        region.setObserver(
            object : OfflineRegion.OfflineRegionObserver {
                override fun onStatusChanged(status: OfflineRegionStatus) {
                    if (status.isComplete) {
                        Log.d(TAG, "Zone map cache warmed with ${status.completedTileCount} tiles")
                        finish(true)
                    }
                }

                override fun onError(error: OfflineRegionError) {
                    Log.w(TAG, "Zone map cache download failed: ${error.reason}: ${error.message}")
                    finish(false)
                }

                override fun mapboxTileCountLimitExceeded(limit: Long) {
                    Log.w(TAG, "Zone map cache tile limit exceeded: $limit")
                    finish(false)
                }
            },
        )
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun deleteDwellZoneRegions(
        regions: List<OfflineRegion>,
        onComplete: () -> Unit,
    ) {
        val remaining = regions.filter { it.isDwellZoneRegion() }
        if (remaining.isEmpty()) {
            onComplete()
            return
        }

        fun deleteAt(index: Int) {
            if (index >= remaining.size) {
                onComplete()
                return
            }
            remaining[index].delete(
                object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() {
                        deleteAt(index + 1)
                    }

                    override fun onError(error: String) {
                        Log.w(TAG, "Could not delete old zone map cache: $error")
                        deleteAt(index + 1)
                    }
                },
            )
        }

        deleteAt(0)
    }

    private fun OfflineRegion.isDwellZoneRegion(): Boolean =
        runCatching {
            val raw = String(metadata, StandardCharsets.UTF_8)
            JSONObject(raw).optString("kind") == ZONE_CACHE_KIND
        }.getOrDefault(false)

    private fun zoneBounds(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
    ): LatLngBounds {
        val cacheRadiusMeters = (radiusMeters * 3.0)
            .coerceAtLeast(MIN_ZONE_CACHE_RADIUS_METERS)
            .coerceAtMost(MAX_ZONE_CACHE_RADIUS_METERS)
        val latDelta = cacheRadiusMeters / 111_320.0
        val lonScale = abs(cos(Math.toRadians(latitude))).coerceAtLeast(0.01)
        val lonDelta = cacheRadiusMeters / (111_320.0 * lonScale)

        val north = (latitude + latDelta).coerceIn(-85.0, 85.0)
        val south = (latitude - latDelta).coerceIn(-85.0, 85.0)
        val east = (longitude + lonDelta).coerceIn(-180.0, 180.0)
        val west = (longitude - lonDelta).coerceIn(-180.0, 180.0)
        return LatLngBounds.from(north, east, south, west)
    }

    internal fun shouldStartPrewarm(
        lastKey: String?,
        lastAtMillis: Long,
        nextKey: String,
        nowMillis: Long,
        dedupeWindowMs: Long = PREWARM_DEDUPE_WINDOW_MS,
    ): Boolean {
        if (nextKey.isBlank()) return false
        if (lastKey != nextKey) return true
        if (lastAtMillis <= 0L) return true
        return nowMillis - lastAtMillis >= dedupeWindowMs
    }

    internal fun prewarmKeyFor(
        styleUrl: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
    ): String {
        val latBucket = (latitude * 1_000).toInt()
        val lonBucket = (longitude * 1_000).toInt()
        val radiusBucket = (radiusMeters.toInt() / 50) * 50
        return listOf(
            styleUrl.trim(),
            latBucket,
            lonBucket,
            radiusBucket,
        ).joinToString("|")
    }

    internal fun isValidPrewarmRequest(
        styleUrl: String,
        latitude: Double,
        longitude: Double,
    ): Boolean =
        MobileMapConfig.isAllowedStyleUrl(styleUrl) &&
            latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -85.0..85.0 &&
            longitude in -180.0..180.0
}
