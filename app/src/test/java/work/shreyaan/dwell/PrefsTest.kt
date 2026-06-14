package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTest {
    @Test
    fun approachProbeKeysAreGlobalWhenPlaceIsUnknown() {
        assertEquals(
            "approach_last_probe",
            Prefs.scopedApproachKey("approach_last_probe", null),
        )
        assertEquals(
            "approach_last_probe_motion",
            Prefs.scopedApproachKey("approach_last_probe_motion", ""),
        )
    }

    @Test
    fun approachProbeKeysAreScopedWhenPlaceIsKnown() {
        assertEquals(
            "approach_last_probe_office",
            Prefs.scopedApproachKey("approach_last_probe", "office"),
        )
        assertEquals(
            "approach_last_probe_motion_gym",
            Prefs.scopedApproachKey("approach_last_probe_motion", "gym"),
        )
    }

    @Test
    fun arrivalRuntimeKeysIncludeGlobalAndScopedPlaceState() {
        val keys = Prefs.arrivalRuntimeKeysForPlaces(listOf("office", "gym", "office", ""))

        assertTrue(keys.contains("arrival_inside_since"))
        assertTrue(keys.contains("arrival_last_observed"))
        assertTrue(keys.contains("arrival_follow_up_count"))
        assertTrue(keys.contains("arrival_follow_up_scheduled"))
        assertTrue(keys.contains("approach_last_probe"))
        assertTrue(keys.contains("approach_last_probe_motion"))
        assertTrue(keys.contains("arrival_inside_since_office"))
        assertTrue(keys.contains("arrival_last_observed_gym"))
        assertTrue(keys.contains("arrival_follow_up_count_office"))
        assertTrue(keys.contains("arrival_follow_up_scheduled_gym"))
        assertTrue(keys.contains("approach_last_probe_office"))
        assertTrue(keys.contains("approach_last_probe_motion_gym"))
        assertFalse(keys.contains("arrival_inside_since_"))
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun approachProbeCooldownBlocksRecentProbeWithoutBypass() {
        assertFalse(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 1_000L,
                now = 30_000L,
                cooldownMs = 90_000L,
                bypassCooldown = false,
            )
        )
    }

    @Test
    fun approachProbeCooldownAllowsExpiredMissingOrBypassedProbe() {
        assertTrue(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 0L,
                now = 30_000L,
                cooldownMs = 90_000L,
                bypassCooldown = false,
            )
        )
        assertTrue(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 1_000L,
                now = 100_000L,
                cooldownMs = 90_000L,
                bypassCooldown = false,
            )
        )
        assertTrue(
            Prefs.shouldAllowApproachProbe(
                lastProbeMillis = 1_000L,
                now = 30_000L,
                cooldownMs = 90_000L,
                bypassCooldown = true,
            )
        )
    }
}
