package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringReliabilityReceiverTest {
    @Test
    fun heartbeatRunsOnlyWhenMonitoringCanUsefullyCheckArrivals() {
        assertTrue(
            MonitoringReliabilityReceiver.shouldRunHeartbeat(
                monitoredCount = 1,
                timerRunning = false,
                hasSetupIssue = false,
            )
        )
        assertFalse(
            MonitoringReliabilityReceiver.shouldRunHeartbeat(
                monitoredCount = 0,
                timerRunning = false,
                hasSetupIssue = false,
            )
        )
        assertFalse(
            MonitoringReliabilityReceiver.shouldRunHeartbeat(
                monitoredCount = 1,
                timerRunning = true,
                hasSetupIssue = false,
            )
        )
        assertFalse(
            MonitoringReliabilityReceiver.shouldRunHeartbeat(
                monitoredCount = 1,
                timerRunning = false,
                hasSetupIssue = true,
            )
        )
    }

    @Test
    fun heartbeatStaysScheduledOnlyWhenPlacesAreMonitored() {
        assertTrue(MonitoringReliabilityReceiver.shouldKeepHeartbeatScheduled(monitoredCount = 1))
        assertFalse(MonitoringReliabilityReceiver.shouldKeepHeartbeatScheduled(monitoredCount = 0))
    }

    @Test
    fun heartbeatCadenceIsConservativeForBattery() {
        assertEquals(
            6 * 60 * 60 * 1_000L,
            MonitoringReliabilityReceiver.HEARTBEAT_INTERVAL_MS,
        )
    }
}
