package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BootReceiverTest {
    @Test
    fun monitoringSetupIssueReturnsNullWhenPermissionsAreReady() {
        assertNull(
            BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = true,
                hasNotifications = true,
                hasMotion = true,
            )
        )
    }

    @Test
    fun monitoringSetupIssuePrioritizesPermissionRecoverySteps() {
        assertSetupIssue(
            expectedError = "Location permission is needed",
            expectedDetail = "location permission missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = false,
                hasBackgroundLocation = false,
                hasNotifications = false,
                hasMotion = false,
            ),
        )
        assertSetupIssue(
            expectedError = "Background location permission is needed",
            expectedDetail = "background location missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = false,
                hasNotifications = false,
                hasMotion = false,
            ),
        )
        assertSetupIssue(
            expectedError = "Notification permission is needed",
            expectedDetail = "notification permission missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = true,
                hasNotifications = false,
                hasMotion = false,
            ),
        )
        assertSetupIssue(
            expectedError = "Motion permission is needed",
            expectedDetail = "activity recognition permission missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = true,
                hasNotifications = true,
                hasMotion = false,
            ),
        )
    }

    @Test
    fun bootReceiverUsesSharedMonitoringPrerequisites() {
        assertEquals(
            MonitoringPrerequisites.issueFor(
                hasLocation = true,
                hasBackgroundLocation = true,
                hasNotifications = false,
                hasMotion = true,
            ),
            BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = true,
                hasNotifications = false,
                hasMotion = true,
            ),
        )
    }

    private fun assertSetupIssue(
        expectedError: String,
        expectedDetail: String,
        issue: MonitoringPrerequisites.SetupIssue?,
    ) {
        assertEquals(expectedError, issue?.error)
        assertEquals(expectedDetail, issue?.detail)
    }
}
