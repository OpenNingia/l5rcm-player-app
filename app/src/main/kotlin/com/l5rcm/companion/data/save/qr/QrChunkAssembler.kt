package com.l5rcm.companion.data.save.qr

import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream

/** Raised when a recognised L5RQR transfer cannot be completed (bad version, integrity, decode). */
class QrTransferException(message: String) : Exception(message)

/**
 * Reassembles a `.l5r` payload from animated multi-frame QR codes.
 *
 * Wire format (see `../l5r-character-manager-3/docs/QR_IMPORT_FORMAT.md`):
 * ```
 * L5RQR1|<id>|<seq>|<total>|<crc>|<data>
 * ```
 * Each frame carries one slice of `Base64(gzip(jsonUtf8))`; `crc` is the CRC32 of the gzip
 * blob, repeated identically in every frame. Frames may arrive in any order and repeat.
 *
 * Pure JVM (`java.util.zip` + Base64) — no Android types, fully unit-testable. **Not
 * thread-safe**: drive it from a single camera-analysis thread.
 */
class QrChunkAssembler {

    private var id: String? = null
    private var total: Int = 0
    private var crc: String = ""
    private val chunks = HashMap<Int, String>()

    data class Progress(val collected: Int, val total: Int)

    /** Frames collected so far / total expected (total is 0 until the first valid frame). */
    val progress: Progress get() = Progress(chunks.size, total)

    /**
     * Offer one decoded QR text.
     *
     * @return the decoded JSON payload once the transfer is complete; `null` while still
     *   collecting, or for a foreign / malformed / inconsistent frame (ignored).
     * @throws QrTransferException for a recognised but unsupported or corrupt transfer.
     */
    fun offer(text: String): String? {
        if (!text.startsWith(TAG)) return null
        val f = text.split('|', limit = 6)
        if (f.size != 6) return null
        if (f[0] != MAGIC) throw QrTransferException("Unsupported QR format: ${f[0]}")

        val frameId = f[1]
        val seq = f[2].toIntOrNull() ?: return null
        val tot = f[3].toIntOrNull() ?: return null
        val frameCrc = f[4]
        val data = f[5]
        if (seq < 0 || tot < 1 || seq >= tot) return null

        if (id != null && frameId != id) reset()        // user switched to a different export
        if (id == null) {
            id = frameId
            total = tot
            crc = frameCrc
        }
        if (tot != total || frameCrc != crc) return null // stale/inconsistent frame for this id

        chunks[seq] = data                               // idempotent: duplicates are harmless
        if (chunks.size < total) return null
        return assemble()
    }

    /** Discard any in-progress transfer. */
    fun reset() {
        id = null
        total = 0
        crc = ""
        chunks.clear()
    }

    private fun assemble(): String {
        // size == total and keys are distinct in 0 until total ⇒ every slot is present.
        val b64 = buildString { for (i in 0 until total) append(chunks.getValue(i)) }
        val blob = try {
            Base64.getDecoder().decode(b64)
        } catch (e: IllegalArgumentException) {
            throw QrTransferException("Invalid QR data encoding")
        }
        val actual = CRC32().apply { update(blob) }.value.toString(16).padStart(8, '0')
        if (actual != crc) throw QrTransferException("QR integrity check failed")
        return try {
            GZIPInputStream(blob.inputStream()).use { it.readBytes() }.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw QrTransferException("Could not decompress QR data")
        }
    }

    private companion object {
        const val TAG = "L5RQR"   // recognises the family (any version); version checked below
        const val MAGIC = "L5RQR1" // the one version this reader supports
    }
}
