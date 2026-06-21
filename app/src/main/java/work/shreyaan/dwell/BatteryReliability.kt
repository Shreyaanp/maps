package work.shreyaan.dwell

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

data class BatteryReliabilityStatus(
    val manufacturer: String,
    val isKnownAggressiveOem: Boolean,
    val isIgnoringOptimizations: Boolean,
) {
    val label: String
        get() = if (isIgnoringOptimizations) "Unrestricted" else "Optimized"

    val detail: String
        get() = when {
            isIgnoringOptimizations ->
                "Android is allowing Dwell to run without Doze battery restrictions."
            isKnownAggressiveOem ->
                "$manufacturer may delay background arrival checks. Open app info, then Battery, and choose Unrestricted."
            else ->
                "Android may delay background arrival checks while battery optimization is enabled. Open app info, then Battery, and choose Unrestricted."
        }
}

object BatteryReliability {
    private val aggressiveManufacturers = setOf(
        "asus",
        "honor",
        "huawei",
        "oneplus",
        "oppo",
        "realme",
        "samsung",
        "vivo",
        "xiaomi",
    )

    fun isKnownAggressiveManufacturer(manufacturer: String): Boolean =
        aggressiveManufacturers.any(manufacturer.lowercase(Locale.ROOT)::contains)

    fun status(context: Context): BatteryReliabilityStatus {
        val manufacturer = Build.MANUFACTURER
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifBlank { "This device" }
        return BatteryReliabilityStatus(
            manufacturer = manufacturer,
            isKnownAggressiveOem = isKnownAggressiveManufacturer(Build.MANUFACTURER),
            isIgnoringOptimizations = isIgnoringOptimizations(context),
        )
    }

    fun isIgnoringOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    internal fun settingsActionOrder(): List<String> =
        listOf(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
        )

    fun openSettings(context: Context): Boolean {
        val packageUri = Uri.parse("package:${context.packageName}")
        val intents = settingsActionOrder().map { action ->
            Intent(action).apply {
                if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                    data = packageUri
                }
            }
        }

        for (intent in intents) {
            val readyIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(readyIntent)
                return true
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: SecurityException) {
                continue
            }
        }
        return false
    }
}
