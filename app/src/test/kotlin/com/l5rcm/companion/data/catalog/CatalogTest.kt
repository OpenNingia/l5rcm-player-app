package com.l5rcm.companion.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CatalogTest {

    @Test
    fun parseVersionExtractsTrailingVersion() {
        assertEquals("5.0", DatapackCatalog.parseVersion("core_pack-5.0.l5rcmpack"))
        assertEquals("1.3.1", DatapackCatalog.parseVersion("book_of_air-1.3.1.zip"))
        assertEquals("", DatapackCatalog.parseVersion("no_version.l5rcmpack"))
    }

    @Test
    fun hostAllowListAcceptsGithubHostsOnly() {
        assertTrue(DatapackCatalog.hostAllowed("https://api.github.com/repos/x/releases/latest"))
        assertTrue(DatapackCatalog.hostAllowed("https://objects.githubusercontent.com/x.zip"))
        assertFalse(DatapackCatalog.hostAllowed("https://evil.example.com/x.zip"))
        assertFalse(DatapackCatalog.hostAllowed("not a url"))
    }
}

class ZipExtractorTest {

    private fun newDir(): File = File.createTempFile("ziptest", "").let {
        it.delete(); it.mkdirs(); it
    }

    private fun zipWith(vararg entries: Pair<String, String>): ByteArrayInputStream {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bos.toByteArray())
    }

    @Test
    fun extractsNormalEntries() {
        val dest = newDir()
        ZipExtractor.extract(zipWith("manifest" to "{}", "core/clans.xml" to "<L5RCM/>"), dest)
        assertTrue(File(dest, "manifest").isFile)
        assertTrue(File(dest, "core/clans.xml").isFile)
    }

    @Test
    fun rejectsZipSlipEntries() {
        val dest = newDir()
        assertThrows(ZipExtractor.ZipSlipException::class.java) {
            ZipExtractor.extract(zipWith("../escape.txt" to "pwned"), dest)
        }
        assertFalse(File(dest.parentFile, "escape.txt").exists())
    }
}
