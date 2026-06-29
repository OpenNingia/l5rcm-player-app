package com.l5rcm.companion.data.catalog

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Extracts a `.l5rcmpack`/`.zip` archive with a **zip-slip path-traversal guard**:
 * every entry must resolve inside [destDir]. Replicates the desktop's security
 * posture (see docs plan §3 and l5rdal). Directory entries and the malicious
 * `../` cases are rejected before any byte is written outside [destDir].
 */
object ZipExtractor {

    /** Thrown when an archive entry would escape the destination directory. */
    class ZipSlipException(entryName: String) :
        Exception("blocked path-traversal entry: $entryName")

    fun extract(stream: InputStream, destDir: File) {
        if (!destDir.exists()) destDir.mkdirs()
        val canonicalDest = destDir.canonicalFile
        val prefix = canonicalDest.path + File.separator

        ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(canonicalDest, entry.name).canonicalFile
                // Guard: the resolved target must live under destDir.
                if (target.path != canonicalDest.path && !target.path.startsWith(prefix)) {
                    throw ZipSlipException(entry.name)
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
