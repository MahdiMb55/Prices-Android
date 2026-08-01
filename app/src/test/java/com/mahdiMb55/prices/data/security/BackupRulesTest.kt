package com.mahdiMb55.prices.data.security

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {
    @Test fun securePreferencesAreExcludedFromAllBackupModes() {
        val root = File(System.getProperty("user.dir"), "src/main/res/xml")
        val backup = parse(File(root, "backup_rules.xml"))
        val extraction = parse(File(root, "data_extraction_rules.xml"))

        assertEquals("prices_secure_session.xml", backup.documentElement.firstElement("exclude").getAttribute("path"))
        val cloud = extraction.documentElement.firstElement("cloud-backup")
        val device = extraction.documentElement.firstElement("device-transfer")
        assertEquals("prices_secure_session.xml", cloud.firstElement("exclude").getAttribute("path"))
        assertEquals("prices_secure_session.xml", device.firstElement("exclude").getAttribute("path"))
        assertEquals("prices_secure_session", SharedPreferencesSecureTokenRecordStore.DEFAULT_PREFERENCES_NAME)
    }

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun org.w3c.dom.Element.firstElement(name: String): org.w3c.dom.Element {
        val nodes = getElementsByTagName(name)
        assertTrue(nodes.length > 0)
        val element = nodes.item(0) as? org.w3c.dom.Element
        assertNotNull(element)
        return element!!
    }
}
