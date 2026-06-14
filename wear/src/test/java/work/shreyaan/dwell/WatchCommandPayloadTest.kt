package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchCommandPayloadTest {
    @Test
    fun promptCommandPayloadCarriesPromptTimestamp() {
        assertEquals("42", promptCommandPayload(42L))
    }
}
