package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSearchCachePolicyTest {
    @Test
    fun persistentSearchKeyDoesNotExposeRawQueryText() {
        val query = "221B Baker Street London"
        val key = persistentSearchKey(searchCacheKey(query))

        assertTrue(isPrivacyPreservingPersistentSearchKey(key))
        assertFalse(key.contains("221", ignoreCase = true))
        assertFalse(key.contains("baker", ignoreCase = true))
        assertFalse(key.contains("street", ignoreCase = true))
        assertEquals(66, key.length)
    }

    @Test
    fun legacyUrlEncodedSearchKeysAreNotPrivacyPreserving() {
        assertFalse(isPrivacyPreservingPersistentSearchKey("q_221b+baker+street"))
        assertFalse(isPrivacyPreservingPersistentSearchKey("q_home%20address"))
        assertFalse(isPrivacyPreservingPersistentSearchKey("q_"))
    }

    @Test
    fun freshSearchEntryMustHavePositiveTimestampWithinTtl() {
        assertTrue(
            isFreshPersistentSearchEntry(
                fetchedAtMillis = 1_000L,
                nowMs = 1_500L,
                ttlMs = 1_000L,
            )
        )
        assertFalse(
            isFreshPersistentSearchEntry(
                fetchedAtMillis = 0L,
                nowMs = 1_500L,
                ttlMs = 1_000L,
            )
        )
    }

    @Test
    fun searchEntryExpiresAtTtlBoundary() {
        assertFalse(
            isFreshPersistentSearchEntry(
                fetchedAtMillis = 1_000L,
                nowMs = 2_000L,
                ttlMs = 1_000L,
            )
        )
    }

    @Test
    fun searchEntryIsRejectedWhenDeviceClockMovesBehindFetchTime() {
        assertFalse(
            isFreshPersistentSearchEntry(
                fetchedAtMillis = 2_000L,
                nowMs = 1_000L,
                ttlMs = 1_000L,
            )
        )
    }
}
