package com.l5rcm.companion.data.repository

import com.l5rcm.companion.data.catalog.CatalogEntry
import com.l5rcm.companion.data.catalog.DatapackCatalog
import com.l5rcm.companion.data.datapack.Datapack
import com.l5rcm.companion.data.datapack.DatapackParser
import com.l5rcm.companion.data.datapack.DatapackSet
import com.l5rcm.companion.data.save.PackRef
import com.l5rcm.companion.data.save.SaveModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the installed datapacks: parsing them off disk into a merged [DatapackSet],
 * downloading new ones from the official catalog, and the enable/disable registry.
 */
class DatapackRepository(
    private val datapacksDir: File,
    private val parser: DatapackParser,
    private val catalog: DatapackCatalog,
    private val prefs: AppPreferences,
) {
    val installedPacks: Flow<List<InstalledPack>> = prefs.installedPacks

    /** Parses all enabled installed packs (core first) into one merged set. */
    suspend fun loadEnabledSet(): DatapackSet = withContext(Dispatchers.IO) {
        val packs = prefs.installedPacksNow()
            .filter { it.enabled }
            .sortedBy { if (it.id == "core") 0 else 1 } // core first; later packs win on dupes
            .mapNotNull { parsePackDir(it.id) }
        DatapackSet(packs)
    }

    private fun parsePackDir(id: String): Datapack? {
        val dir = File(datapacksDir, id)
        return if (parser.isPack(dir)) parser.parsePack(dir) else null
    }

    suspend fun fetchCatalog(): List<CatalogEntry> = catalog.fetchCatalog()

    /** Downloads, extracts and registers a pack; returns its registry entry. */
    suspend fun install(entry: CatalogEntry): InstalledPack = withContext(Dispatchers.IO) {
        val dir = catalog.downloadAndInstall(entry)
        val pack = InstalledPack(id = dir.name, version = entry.version, enabled = true)
        prefs.upsertPack(pack)
        pack
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = prefs.setPackEnabled(id, enabled)

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        File(datapacksDir, id).deleteRecursively()
        prefs.removePack(id)
    }

    /** Pack refs in [save] that are not currently installed (mirrors get_missing_dependencies). */
    suspend fun missingDependencies(save: SaveModel): List<PackRef> {
        val installedIds = prefs.installedPacksNow().map { it.id }.toSet()
        return save.pack_refs.filter { it.id.isNotEmpty() && it.id !in installedIds }
    }
}
