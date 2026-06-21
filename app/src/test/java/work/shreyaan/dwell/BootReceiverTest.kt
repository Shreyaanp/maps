package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BootReceiverTest {
    @Test
    fun runningTimerRecoveryKeepsLeavePromptForTimerPlace() {
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.LeavePrompt,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "office",
                timerPlaceId = "office",
                promptPlaceExists = true,
            ),
        )
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.LeavePrompt,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "",
                timerPlaceId = "office",
                promptPlaceExists = false,
            ),
        )
    }

    @Test
    fun runningTimerRecoveryKeepsValidSwitchPrompt() {
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.SwitchPrompt,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                promptPlaceExists = true,
            ),
        )
    }

    @Test
    fun runningTimerRecoveryClearsStaleOrAmbiguousPrompt() {
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.ClearPromptAndRun,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "office",
                timerPlaceId = "office",
                promptPlaceExists = true,
            ),
        )
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.ClearPromptAndRun,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_START_TIMER,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                promptPlaceExists = false,
            ),
        )
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.ClearPromptAndRun,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_LEAVE_EARLY,
                promptPlaceId = "gym",
                timerPlaceId = "office",
                promptPlaceExists = true,
            ),
        )
    }

    @Test
    fun runningTimerRecoveryUsesPlainTimerWhenNoPromptIsLive() {
        assertEquals(
            BootReceiver.RunningTimerRecoveryAction.RunningTimer,
            BootReceiver.runningTimerRecoveryAction(
                prompt = Prefs.WATCH_PROMPT_NONE,
                promptPlaceId = "",
                timerPlaceId = "office",
                promptPlaceExists = false,
            ),
        )
    }

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
            expectedError = "Notification permission is needed",
            expectedDetail = "notification permission missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = false,
                hasNotifications = false,
                hasMotion = false,
            ),
        )
        assertSetupIssue(
            expectedError = "Physical activity permission is needed",
            expectedDetail = "activity recognition permission missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = false,
                hasNotifications = true,
                hasMotion = false,
            ),
        )
        assertSetupIssue(
            expectedError = "Background location permission is needed",
            expectedDetail = "background location missing",
            issue = BootReceiver.monitoringSetupIssue(
                hasLocation = true,
                hasBackgroundLocation = false,
                hasNotifications = true,
                hasMotion = true,
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
