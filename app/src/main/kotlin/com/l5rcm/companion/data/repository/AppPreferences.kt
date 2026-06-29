package com.l5rcm.companion.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** A pack the user has installed locally. */
@Serializable
data class InstalledPack(
    val id: String,
    val version: String,
    val enabled: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "l5rcm_companion")

/**
 * Persistent app preferences: the installed-pack registry and the last-opened
 * character URI (for resume-on-launch). Backed by Preferences DataStore.
 */
class AppPreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val installedPacks: Flow<List<InstalledPack>> = context.dataStore.data.map { prefs ->
        prefs[KEY_INSTALLED_PACKS]?.let { decodePacks(it) } ?: emptyList()
    }

    val lastCharacterUri: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_CHARACTER] }

    suspend fun installedPacksNow(): List<InstalledPack> = installedPacks.first()

    suspend fun upsertPack(pack: InstalledPack) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_INSTALLED_PACKS]?.let { decodePacks(it) } ?: emptyList()
            val updated = current.filterNot { it.id == pack.id } + pack
            prefs[KEY_INSTALLED_PACKS] = encodePacks(updated)
        }
    }

    suspend fun setPackEnabled(id: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_INSTALLED_PACKS]?.let { decodePacks(it) } ?: return@edit
            prefs[KEY_INSTALLED_PACKS] =
                encodePacks(current.map { if (it.id == id) it.copy(enabled = enabled) else it })
        }
    }

    suspend fun removePack(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_INSTALLED_PACKS]?.let { decodePacks(it) } ?: return@edit
            prefs[KEY_INSTALLED_PACKS] = encodePacks(current.filterNot { it.id == id })
        }
    }

    suspend fun setLastCharacterUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(KEY_LAST_CHARACTER) else prefs[KEY_LAST_CHARACTER] = uri
        }
    }

    private fun encodePacks(packs: List<InstalledPack>): String =
        json.encodeToString(ListSerializer(InstalledPack.serializer()), packs)

    private fun decodePacks(text: String): List<InstalledPack> = try {
        json.decodeFromString(ListSerializer(InstalledPack.serializer()), text)
    } catch (e: Exception) {
        emptyList()
    }

    private companion object {
        val KEY_INSTALLED_PACKS = stringPreferencesKey("installed_packs")
        val KEY_LAST_CHARACTER = stringPreferencesKey("last_character_uri")
    }
}
