package work.shreyaan.dwell

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

object ActivityRecognitionManager {
    private const val TAG = "DwellActivity"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            400,
            Intent(context, ActivityRecognitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    private fun transition(activityType: Int, transitionType: Int): ActivityTransition =
        ActivityTransition.Builder()
            .setActivityType(activityType)
            .setActivityTransition(transitionType)
            .build()

    private fun request(): ActivityTransitionRequest =
        ActivityTransitionRequest(
            listOf(
                transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
                transition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
                transition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
                transition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
                transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            )
        )

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (!hasPermission(context)) {
            Prefs.setMotion(context, DwellMotion.UNKNOWN)
            DwellDiagnostics.logLifecycle(
                context,
                source = "motion",
                decision = "permission-missing",
                detail = "activity recognition permission missing",
            )
            return
        }

        runCatching {
            ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request(), pendingIntent(context))
                .addOnSuccessListener {
                    DwellDiagnostics.logLifecycle(
                        context,
                        source = "motion",
                        decision = "live",
                        detail = "activity transitions registered",
                    )
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Activity transition registration failed", e)
                    DwellDiagnostics.logLifecycle(
                        context,
                        source = "motion",
                        decision = "failed",
                        detail = (e.message ?: "activity transition registration failed").take(120),
                    )
                }
        }.onFailure { e ->
            Log.w(TAG, "Activity transition registration failed", e)
            DwellDiagnostics.logLifecycle(
                context,
                source = "motion",
                decision = "failed",
                detail = (e.message ?: "activity transition registration failed").take(120),
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun stop(context: Context) {
        if (!hasPermission(context)) {
            Prefs.setMotion(context, DwellMotion.UNKNOWN)
            DwellDiagnostics.logLifecycle(
                context,
                source = "motion",
                decision = "stopped",
                detail = "activity recognition permission missing",
            )
            return
        }

        runCatching {
            ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(pendingIntent(context))
                .addOnCompleteListener {
                    Prefs.setMotion(context, DwellMotion.UNKNOWN)
                    DwellDiagnostics.logLifecycle(
                        context,
                        source = "motion",
                        decision = "stopped",
                        detail = "activity transitions removed",
                    )
                }
        }.onFailure { e ->
            Log.w(TAG, "Activity transition removal failed", e)
            Prefs.setMotion(context, DwellMotion.UNKNOWN)
            DwellDiagnostics.logLifecycle(
                context,
                source = "motion",
                decision = "stop-failed",
                detail = (e.message ?: "activity transition removal failed").take(120),
            )
        }
    }

    fun motionFrom(activityType: Int, transitionType: Int): DwellMotion {
        if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
            return DwellMotion.UNKNOWN
        }
        return when (activityType) {
            DetectedActivity.STILL -> DwellMotion.STILL
            DetectedActivity.ON_FOOT -> DwellMotion.WALKING
            DetectedActivity.WALKING -> DwellMotion.WALKING
            DetectedActivity.RUNNING -> DwellMotion.RUNNING
            DetectedActivity.ON_BICYCLE -> DwellMotion.ON_BICYCLE
            DetectedActivity.IN_VEHICLE -> DwellMotion.IN_VEHICLE
            else -> DwellMotion.UNKNOWN
        }
    }
}

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val latest = result.transitionEvents.lastOrNull() ?: return
        val motion = ActivityRecognitionManager.motionFrom(
            latest.activityType,
            latest.transitionType,
        )
        Prefs.setMotion(
            context,
            motion,
        )
        DwellDiagnostics.logLifecycle(
            context,
            source = "motion",
            decision = motion.name.lowercase(),
            detail = "activity transition update",
        )
        if (DwellArrivalEngine.shouldProbeForGlobalMotion(motion)) {
            val pending = goAsync()
            DwellArrivalEngine.runApproachProbe(
                context = context.applicationContext,
                triggerMotion = motion,
            ) {
                pending.finish()
            }
        } else if (motion != DwellMotion.UNKNOWN) {
            DwellDiagnostics.logLifecycle(
                context,
                source = "approach",
                decision = "motion-only",
                detail = "${motion.name.lowercase()} stored without global location probe",
            )
        }
    }
}
