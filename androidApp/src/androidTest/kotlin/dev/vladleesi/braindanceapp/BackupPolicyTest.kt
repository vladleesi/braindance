// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class BackupPolicyTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun legacyBackupRequiresEncryptionAndNeverIncludesPrivateStorage() {
        assertTrue(context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
        val rules = readRules(R.xml.backup_rules)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            assertFalse(rules.any { it.tag == "include" })
            val excludedDomains =
                rules
                    .filter { it.tag == "exclude" && it.attributes["path"] == "." }
                    .map { it.attributes["domain"] }
                    .toSet()
            assertTrue(excludedDomains.containsAll(privateDomains))
        } else {
            val includes = assertOnlyApprovedExports(rules, "full-backup-content")
            assertTrue(includes.all { it.attributes["requireFlags"] == "clientSideEncryption" })
        }
    }

    @Test
    fun cloudBackupAndDeviceTransferUseSeparateRestrictiveAllowlists() {
        val rules = readRules(R.xml.data_extraction_rules)
        assertEquals("true", rules.single { it.tag == "cloud-backup" }.attributes["disableIfNoEncryptionCapabilities"])
        assertOnlyApprovedExports(rules, "cloud-backup")
        assertOnlyApprovedExports(rules, "device-transfer")
    }

    private fun assertOnlyApprovedExports(
        rules: List<Rule>,
        parent: String,
    ): List<Rule> {
        val includes = rules.filter { it.parent == parent && it.tag == "include" }
        assertTrue("An absent include allowlist would back up all app data", includes.isNotEmpty())
        assertTrue(includes.all { it.attributes["domain"] == "file" && it.attributes["path"] == "backup_exports/" })
        return includes
    }

    private fun readRules(resource: Int): List<Rule> {
        val rules = mutableListOf<Rule>()
        val parents = mutableListOf<String>()
        context.resources.getXml(resource).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        val attributes =
                            (0 until parser.attributeCount).associate { index ->
                                parser.getAttributeName(index) to parser.getAttributeValue(index)
                            }
                        rules += Rule(parser.name, parents.lastOrNull(), attributes)
                        parents += parser.name
                    }
                    XmlPullParser.END_TAG -> parents.removeAt(parents.lastIndex)
                }
                parser.next()
            }
        }
        return rules
    }

    private data class Rule(
        val tag: String,
        val parent: String?,
        val attributes: Map<String, String>,
    )

    private companion object {
        val privateDomains =
            setOf(
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
