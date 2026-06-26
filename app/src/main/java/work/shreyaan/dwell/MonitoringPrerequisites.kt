package work.shreyaan.dwell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MonitoringPrerequisites {
    internal data class SetupIssue(
        val error: String,
        val detail: String,
    )

    internal fun issueForContext(context: Context): SetupIssue? =
        issueFor(
            hasLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
            hasBackgroundLocation = Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED,
            hasNotifications = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
            hasMotion = ActivityRecognitionManager.hasPermission(context),
        )

    internal fun issueFor(
        hasLocation: Boolean,
        hasBackgroundLocation: Boolean,
        hasNotifications: Boolean,
        hasMotion: Boolean,
    ): SetupIssue? =
        when {
            !hasLocation -> SetupIssue(
                error = "Location permission is needed",
                detail = "location permission missing",
            )
            !hasNotifications -> SetupIssue(
                error = "Notification permission is needed",
                detail = "notification permission missing",
            )
            !hasMotion -> SetupIssue(
                error = "Physical activity permission is needed",
                detail = "activity recognition permission missing",
            )
            !hasBackgroundLocation -> SetupIssue(
                error = "Background location permission is needed",
                detail = "background location missing",
            )
            else -> null
        }

    internal fun markSetupNeeded(
        context: Context,
        source: String,
        issue: SetupIssue,
    ) {
        ArrivalProbeReceiver.cancel(context)
        MonitoringReliabilityReceiver.cancel(context)
        Prefs.clearRegisteredPlaces(context)
        Prefs.clearArrivalRuntime(context)
        Prefs.setMonitoringError(context, issue.error)
        ActivityRecognitionManager.stop(context)
        DwellDiagnostics.logLifecycle(
            context,
            source = source,
            decision = "setup-needed",
            detail = issue.detail,
        )
        Notifications.notifySetupNeeded(context)
        WearSync.pushState(context)
    }
}
