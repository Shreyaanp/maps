package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PrivacyBackupPolicyTest {
    @Test
    fun appManifestOptsOutOfBackupAndReferencesBackupRules() {
        val manifest = xmlFile("src/main/AndroidManifest.xml")
        val application = manifest.documentElement
            .getElementsByTagName("application")
            .item(0) as Element

        assertEquals("false", application.androidAttr("allowBackup"))
        assertEquals("@xml/backup_rules", application.androidAttr("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.androidAttr("dataExtractionRules"))
    }

    @Test
    fun backupRulesExcludeSensitiveAppStorage() {
        val rules = xmlFile("src/main/res/xml/backup_rules.xml")
        val root = rules.documentElement

        assertEquals("full-backup-content", root.tagName)
        assertExcludesDomains(root, requiredSensitiveDomains)
    }

    @Test
    fun dataExtractionRulesExcludeSensitiveAppStorageForCloudAndDeviceTransfer() {
        val rules = xmlFile("src/main/res/xml/data_extraction_rules.xml")
        val root = rules.documentElement

        assertEquals("data-extraction-rules", root.tagName)
        val cloudBackup = root.getElementsByTagName("cloud-backup").item(0) as Element
        val deviceTransfer = root.getElementsByTagName("device-transfer").item(0) as Element

        assertExcludesDomains(cloudBackup, requiredSensitiveDomains)
        assertExcludesDomains(deviceTransfer, requiredSensitiveDomains)
    }

    private fun assertExcludesDomains(parent: Element, requiredDomains: Set<String>) {
        val excludes = parent.getElementsByTagName("exclude")
        val excludedDomains = buildSet {
            for (i in 0 until excludes.length) {
                val exclude = excludes.item(i) as Element
                if (exclude.getAttribute("path") == ".") {
                    add(exclude.getAttribute("domain"))
                }
            }
        }

        requiredDomains.forEach { domain ->
            assertTrue("Expected $domain to be excluded", excludedDomains.contains(domain))
        }
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
        ).firstOrNull { it.isFile } ?: error("Could not find $path from ${File(".").absolutePath}")

    private fun Element.androidAttr(name: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", name)

    private companion object {
        val requiredSensitiveDomains = setOf(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref",
        )
    }
}
