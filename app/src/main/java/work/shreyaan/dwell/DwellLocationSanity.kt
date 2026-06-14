package work.shreyaan.dwell

import android.location.Location
import android.os.Build
import androidx.core.location.LocationCompat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DwellLocationSanity {
    private const val ANDROID_EMULATOR_DEFAULT_LAT = 37.4219983
    private const val ANDROID_EMULATOR_DEFAULT_LON = -122.084
    private const val ANDROID_EMULATOR_DEFAULT_RADIUS_METERS = 300f
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun hasValidCoordinates(location: Location?): Boolean =
        location != null &&
            location.latitude.isFinite() &&
            location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 &&
            location.longitude in -180.0..180.0

    fun isSuspiciousPhysicalEmulatorDefault(location: Location): Boolean =
        isSuspiciousPhysicalEmulatorDefault(
            latitude = location.latitude,
            longitude = location.longitude,
            isLikelyAndroidEmulatorDevice = isLikelyAndroidEmulatorDevice(),
        )

    fun isUsableDwellLocation(location: Location?, allowMock: Boolean = false): Boolean =
        hasValidCoordinates(location) &&
            location != null &&
            mockPolicyAllows(LocationCompat.isMock(location), allowMock) &&
            !isSuspiciousPhysicalEmulatorDefault(location)

    internal fun mockPolicyAllows(isMock: Boolean, allowMock: Boolean): Boolean =
        !isMock || allowMock

    internal fun isSuspiciousPhysicalEmulatorDefault(
        latitude: Double,
        longitude: Double,
        isLikelyAndroidEmulatorDevice: Boolean,
    ): Boolean =
        !isLikelyAndroidEmulatorDevice && isNearAndroidEmulatorDefault(latitude, longitude)

    internal fun isNearAndroidEmulatorDefault(latitude: Double, longitude: Double): Boolean {
        if (
            !latitude.isFinite() ||
            !longitude.isFinite() ||
            latitude !in -90.0..90.0 ||
            longitude !in -180.0..180.0
        ) {
            return false
        }

        return distanceMeters(
            startLatitude = latitude,
            startLongitude = longitude,
            endLatitude = ANDROID_EMULATOR_DEFAULT_LAT,
            endLongitude = ANDROID_EMULATOR_DEFAULT_LON,
        ) <= ANDROID_EMULATOR_DEFAULT_RADIUS_METERS
    }

    private fun isLikelyAndroidEmulatorDevice(): Boolean {
        val deviceSignals = listOf(
            Build.FINGERPRINT,
            Build.HARDWARE,
            Build.MODEL,
            Build.MANUFACTURER,
            Build.PRODUCT,
            Build.DEVICE,
            Build.BRAND,
        ).joinToString("|").lowercase(Locale.ROOT)
        return listOf("generic", "emulator", "sdk", "goldfish", "ranchu").any(deviceSignals::contains)
    }

    private fun distanceMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val lat1 = Math.toRadians(startLatitude)
        val lat2 = Math.toRadians(endLatitude)
        val deltaLat = Math.toRadians(endLatitude - startLatitude)
        val deltaLon = Math.toRadians(endLongitude - startLongitude)
        val a = sin(deltaLat / 2).pow(2.0) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
