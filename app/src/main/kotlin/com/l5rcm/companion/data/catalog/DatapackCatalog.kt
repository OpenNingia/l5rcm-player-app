package com.l5rcm.companion.data.catalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.zip.ZipFile

/**
 * Talks to the official datapack repository's GitHub Releases API and installs
 * packs into app-internal storage. Mirrors `l5r/util/datapack_catalog.py`:
 * pinned repo, host allow-list on the redirect target, zip-slip-guarded extraction.
 */
class DatapackCatalog(
    private val client: OkHttpClient,
    /** App-internal `filesDir/datapacks` root where packs are installed. */
    private val datapacksDir: File,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    @Serializable
    private data class Release(val assets: List<Asset> = emptyList())

    @Serializable
    private data class Asset(
        val name: String = "",
        val browser_download_url: String = "",
        val size: Long = 0,
    )

    @Serializable
    private data class PackManifest(val id: String = "")

    /** Fetches the official datapacks, sorted Core-first then alphabetically. */
    suspend fun fetchCatalog(): List<CatalogEntry> = withContext(Dispatchers.IO) {
        require(hostAllowed(RELEASES_API)) { "releases API host is not allow-listed" }
        val request = Request.Builder()
            .url(RELEASES_API)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .build()

        val body = try {
            client.newCall(request).execute().use { resp ->
                if (resp.code == 403 && isRateLimited(resp.header("X-RateLimit-Remaining"))) {
                    throw CatalogException.RateLimited()
                }
                if (!resp.isSuccessful) throw CatalogException.Generic("HTTP ${resp.code}")
                resp.body?.string() ?: throw CatalogException.Generic("empty response")
            }
        } catch (e: CatalogException) {
            throw e
        } catch (e: IOException) {
            throw CatalogException.Offline(e.message ?: "offline")
        }

        val release = try {
            json.decodeFromString(Release.serializer(), body)
        } catch (e: Exception) {
            throw CatalogException.Generic("malformed response")
        }

        release.assets
            .filter { asset -> PACK_SUFFIXES.any { asset.name.lowercase().endsWith(it) } }
            .filter { it.browser_download_url.isNotEmpty() }
            .map { CatalogEntry(it.name, parseVersion(it.name), it.browser_download_url, it.size) }
            .sortedWith(compareBy({ !it.isCore }, { it.name.lowercase() }))
    }

    /**
     * Downloads [entry] and installs it under `datapacks/<pack_id>/`.
     * Returns the installed pack directory. Re-validates the final host after
     * any redirect and guards extraction against zip-slip.
     */
    suspend fun downloadAndInstall(entry: CatalogEntry): File = withContext(Dispatchers.IO) {
        if (!hostAllowed(entry.url)) throw CatalogException.Generic("download host is not allow-listed")

        val tmp = File.createTempFile("l5rpack", ".l5rcmpack", datapacksDir.also { it.mkdirs() })
        try {
            val request = Request.Builder()
                .url(entry.url)
                .header("User-Agent", USER_AGENT)
                .build()
            try {
                client.newCall(request).execute().use { resp ->
                    if (resp.code == 403 && isRateLimited(resp.header("X-RateLimit-Remaining"))) {
                        throw CatalogException.RateLimited()
                    }
                    if (!resp.isSuccessful) throw CatalogException.Generic("HTTP ${resp.code}")
                    // Re-validate where we actually ended up after redirects.
                    if (!hostAllowed(resp.request.url.toString())) {
                        throw CatalogException.Generic("redirected to a non-allow-listed host")
                    }
                    val sink = resp.body ?: throw CatalogException.Generic("empty download")
                    tmp.outputStream().use { out -> sink.byteStream().copyTo(out) }
                }
            } catch (e: CatalogException) {
                throw e
            } catch (e: IOException) {
                throw CatalogException.Offline(e.message ?: "offline")
            }

            installFromArchive(tmp)
        } finally {
            tmp.delete()
        }
    }

    /** Installs an already-downloaded archive file (also used for "import datapack" later). */
    fun installFromArchive(archive: File): File {
        val packId = readPackId(archive)
            ?: throw CatalogException.Generic("not a valid datapack (no manifest)")
        val dest = File(datapacksDir, packId)
        if (dest.exists()) dest.deleteRecursively()
        try {
            archive.inputStream().use { ZipExtractor.extract(it, dest) }
        } catch (e: ZipExtractor.ZipSlipException) {
            dest.deleteRecursively()
            throw CatalogException.Generic("unsafe archive: ${e.message}")
        } catch (e: Exception) {
            dest.deleteRecursively()
            throw CatalogException.Generic("cannot extract datapack")
        }
        return dest
    }

    private fun readPackId(archive: File): String? = try {
        ZipFile(archive).use { zip ->
            val entry = zip.getEntry("manifest") ?: return null
            val text = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            json.decodeFromString(PackManifest.serializer(), text).id.ifEmpty { null }
        }
    } catch (e: Exception) {
        null
    }

    companion object {
        const val OFFICIAL_REPO = "OpenNingia/l5rcm-data-packs"
        const val RELEASES_API = "https://api.github.com/repos/$OFFICIAL_REPO/releases/latest"
        const val USER_AGENT = "L5RCM-datapack-catalog"

        val ALLOWED_HOSTS = setOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "codeload.github.com",
        )

        private val PACK_SUFFIXES = listOf(".l5rcmpack", ".zip")
        private val VERSION_RE = Regex("""-(\d[\d.]*)\.(?:l5rcmpack|zip)$""", RegexOption.IGNORE_CASE)

        fun hostAllowed(url: String): Boolean = try {
            (URI(url).host ?: "").lowercase() in ALLOWED_HOSTS
        } catch (e: Exception) {
            false
        }

        fun parseVersion(assetName: String): String =
            VERSION_RE.find(assetName)?.groupValues?.get(1).orEmpty()

        private fun isRateLimited(remaining: String?): Boolean =
            remaining == "0" || remaining == null
    }
}
