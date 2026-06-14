package work.shreyaan.dwell

object DwellRadius {
    const val MIN_METERS = 100f
    const val DEFAULT_METERS = 150f
    const val MAX_METERS = 500f
    const val APPROACH_MIN_METERS = 450f
    const val APPROACH_MAX_METERS = 1_000f

    fun normalize(radiusMeters: Float): Float =
        radiusMeters.takeIf { it.isFinite() }
            ?.coerceIn(MIN_METERS, MAX_METERS)
            ?: DEFAULT_METERS

    fun approachRadius(radiusMeters: Float): Float =
        (normalize(radiusMeters) * 3f)
            .coerceIn(APPROACH_MIN_METERS, APPROACH_MAX_METERS)
}
