package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ManifestReliabilityTest {
    @Test
    fun mainActivityIsPortraitLocked() {
        val manifest = xmlFile("src/main/AndroidManifest.xml")

        val activity = manifest.documentElement
            .getElementsByTagName("activity")
            .asElements()
            .first { it.androidAttr("name") == ".MainActivity" }

        assertEquals("portrait", activity.androidAttr("screenOrientation"))
    }

    @Test
    fun bootReceiverCanRearmMonitoringAfterRebootAndAppUpdate() {
        val manifest = xmlFile("src/main/AndroidManifest.xml")

        val bootPermissionDeclared = manifest.documentElement
            .getElementsByTagName("uses-permission")
            .asElements()
            .any { it.androidAttr("name") == "android.permission.RECEIVE_BOOT_COMPLETED" }
        assertTrue("Boot completed permission must be declared", bootPermissionDeclared)

        val receiver = manifest.documentElement
            .getElementsByTagName("receiver")
            .asElements()
            .first { it.androidAttr("name") == ".BootReceiver" }

        assertEquals("false", receiver.androidAttr("exported"))

        val actions = receiver
            .getElementsByTagName("action")
            .asElements()
            .map { it.androidAttr("name") }
            .toSet()

        assertTrue(actions.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(actions.contains("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun appManifestDeclaresBackgroundReliabilityPermissions() {
        val manifest = xmlFile("src/main/AndroidManifest.xml")

        val permissions = manifest.documentElement
            .getElementsByTagName("uses-permission")
            .asElements()
            .map { it.androidAttr("name") }
            .toSet()

        assertTrue(permissions.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(permissions.contains("android.permission.ACCESS_COARSE_LOCATION"))
        assertTrue(permissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION"))
        assertTrue(permissions.contains("android.permission.ACTIVITY_RECOGNITION"))
        assertTrue(permissions.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(permissions.contains("android.permission.SCHEDULE_EXACT_ALARM"))
    }

    @Test
    fun wearManifestCanReceivePhoneStateAndShowAlerts() {
        val manifest = xmlFile("wear/src/main/AndroidManifest.xml")

        val permissions = manifest.documentElement
            .getElementsByTagName("uses-permission")
            .asElements()
            .map { it.androidAttr("name") }
            .toSet()
        assertTrue(permissions.contains("android.permission.POST_NOTIFICATIONS"))

        val watchDataService = manifest.documentElement
            .getElementsByTagName("service")
            .asElements()
            .first { it.androidAttr("name") == ".WatchDataService" }
        val actions = watchDataService
            .getElementsByTagName("action")
            .asElements()
            .map { it.androidAttr("name") }
            .toSet()

        assertTrue(actions.contains("com.google.android.gms.wearable.DATA_CHANGED"))
        assertTrue(actions.contains("com.google.android.gms.wearable.MESSAGE_RECEIVED"))
    }

    private fun xmlFile(path: String) =
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(projectFile(path))

    private fun projectFile(path: String): File =
        listOf(
            File(path),
            File("app", path),
            File("../app", path),
            File("..", path),
        ).firstOrNull { it.isFile } ?: error("Could not find $path from ${File(".").absolutePath}")

    private fun Element.androidAttr(name: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", name)

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        buildList {
            for (i in 0 until length) {
                add(item(i) as Element)
            }
        }
}
