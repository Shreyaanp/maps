package work.shreyaan.dwell

object LocationQuality {
    private const val MAX_USABLE_AGE_MS = 60_000L
    private const val MAX_USABLE_ACCURACY_METERS = 250f
    private const val MAX_IMMEDIATE_AGE_MS = 15_000L
    private const val MAX_IMMEDIATE_ACCURACY_METERS = 100f

    fun isUsable(
        latitude: Double,
        longitude: Double,
        ageMs: Long,
        accuracyMeters: Float,
        isMock: Boolean,
        allowMock: Boolean = false,
    ): Boolean =
        (!isMock || allowMock) &&
            latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            ageMs in 0..MAX_USABLE_AGE_MS &&
            accuracyMeters.isFinite() &&
            accuracyMeters >= 0f &&
            accuracyMeters <= MAX_USABLE_ACCURACY_METERS

    fun isImmediate(
        latitude: Double,
        longitude: Double,
        ageMs: Long,
        accuracyMeters: Float,
        isMock: Boolean,
        allowMock: Boolean = false,
    ): Boolean =
        isUsable(
            latitude = latitude,
            longitude = longitude,
            ageMs = ageMs,
            accuracyMeters = accuracyMeters,
            isMock = isMock,
            allowMock = allowMock,
        ) &&
            ageMs <= MAX_IMMEDIATE_AGE_MS &&
            accuracyMeters <= MAX_IMMEDIATE_ACCURACY_METERS
}
