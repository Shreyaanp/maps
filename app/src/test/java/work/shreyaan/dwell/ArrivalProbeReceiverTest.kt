package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalProbeReceiverTest {
    @Test
    fun followUpRequestCodeIsStableForSamePlace() {
        assertEquals(
            ArrivalProbeReceiver.requestCodeFor("office"),
            ArrivalProbeReceiver.requestCodeFor("office"),
        )
    }

    @Test
    fun followUpRequestCodeIsScopedByPlace() {
        assertTrue(
            ArrivalProbeReceiver.requestCodeFor("office") !=
                ArrivalProbeReceiver.requestCodeFor("gym"),
        )
    }

    @Test
    fun blankPlaceUsesLegacyRequestCode() {
        assertEquals(
            ArrivalProbeReceiver.requestCodeFor(null),
            ArrivalProbeReceiver.requestCodeFor(""),
        )
    }

    @Test
    fun followUpDataIdentityIsStableForSamePlace() {
        assertEquals(
            ArrivalProbeReceiver.dataStringFor("office"),
            ArrivalProbeReceiver.dataStringFor("office"),
        )
    }

    @Test
    fun followUpDataIdentityIsScopedByPlace() {
        assertTrue(
            ArrivalProbeReceiver.dataStringFor("office") !=
                ArrivalProbeReceiver.dataStringFor("gym"),
        )
    }

    @Test
    fun followUpDataIdentityEncodesUnusualPlaceIds() {
        val data = ArrivalProbeReceiver.dataStringFor("home/second floor")

        assertTrue(data.contains("home%2Fsecond+floor"))
    }

    @Test
    fun blankPlaceUsesLegacyDataIdentity() {
        assertEquals(
            ArrivalProbeReceiver.dataStringFor(null),
            ArrivalProbeReceiver.dataStringFor(""),
        )
    }

    @Test
    fun scheduleFailureDetailMarksScopedFollowUps() {
        val detail = ArrivalProbeReceiver.scheduleFailureDetail(
            "office",
            SecurityException("exact alarm denied"),
        )

        assertTrue(detail.contains("scoped=true"))
        assertTrue(detail.contains("SecurityException"))
        assertTrue(detail.contains("exact alarm denied"))
    }

    @Test
    fun scheduleFailureDetailMarksLegacyFollowUps() {
        val detail = ArrivalProbeReceiver.scheduleFailureDetail(
            null,
            IllegalStateException(),
        )

        assertTrue(detail.contains("scoped=false"))
        assertTrue(detail.contains("IllegalStateException"))
        assertTrue(detail.contains("alarm schedule failed"))
    }
}
