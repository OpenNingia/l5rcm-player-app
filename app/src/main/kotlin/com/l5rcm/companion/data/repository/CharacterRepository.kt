package com.l5rcm.companion.data.repository

import android.content.Context
import android.net.Uri
import com.l5rcm.companion.data.save.SaveIdentity
import com.l5rcm.companion.data.save.SaveModel
import com.l5rcm.companion.data.save.SaveParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/** A parsed character plus the stable [uuid] its local `<uuid>.l5r` copy is keyed by. */
data class StoredCharacter(val uuid: String, val save: SaveModel)

/**
 * Imports `.l5r` saves — from a SAF file picker or from QR-reassembled JSON — and keeps a local
 * copy named `<uuid>.l5r` so both paths persist identically and can be resumed on next launch.
 * Saves that lack a stable `uuid` are rejected ([SaveIdentity] / MissingUuidException).
 */
class CharacterRepository(
    private val context: Context,
    private val prefs: AppPreferences,
) {
    private val charactersDir: File get() = File(context.filesDir, "characters")

    val lastCharacterUuid: Flow<String?> = prefs.lastCharacterUuid

    /** Imports the `.l5r` document at [uri] (SAF content URI), storing a local copy. */
    suspend fun importFromUri(uri: Uri): StoredCharacter = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalArgumentException("cannot open $uri")
        store(text)
    }

    /** Imports a `.l5r` document reassembled from a QR scan, storing a local copy. */
    suspend fun importFromJson(rawJson: String): StoredCharacter = withContext(Dispatchers.IO) {
        store(rawJson)
    }

    /** Re-loads the last opened character from its local copy, or null if none/missing. */
    suspend fun resumeLast(): StoredCharacter? = withContext(Dispatchers.IO) {
        val uuid = prefs.lastCharacterUuid.first() ?: return@withContext null
        val file = File(charactersDir, "$uuid.l5r")
        if (!file.exists()) return@withContext null
        StoredCharacter(uuid, SaveParser.parse(file.readText()))
    }

    suspend fun rememberLast(uuid: String?) = prefs.setLastCharacterUuid(uuid)

    /**
     * Writes the verbatim JSON to `<uuid>.l5r` and returns the parsed character.
     * @throws com.l5rcm.companion.data.save.MissingUuidException if the save carries no stable id.
     */
    private fun store(rawJson: String): StoredCharacter {
        val uuid = SaveIdentity.requireUuid(rawJson)
        charactersDir.mkdirs()
        File(charactersDir, "$uuid.l5r").writeText(rawJson)
        return StoredCharacter(uuid, SaveParser.parse(rawJson))
    }
}
