package com.l5rcm.companion.data.repository

import android.content.Context
import android.net.Uri
import com.l5rcm.companion.data.save.SaveModel
import com.l5rcm.companion.data.save.SaveParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Reads `.l5r` saves selected via the Storage Access Framework and remembers the last one. */
class CharacterRepository(
    private val context: Context,
    private val prefs: AppPreferences,
) {
    val lastCharacterUri: Flow<String?> = prefs.lastCharacterUri

    /** Parses the `.l5r` document at [uri] (SAF content URI). */
    suspend fun load(uri: Uri): SaveModel = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { SaveParser.parse(it) }
            ?: throw IllegalArgumentException("cannot open $uri")
    }

    /** Persists a long-lived read permission so the character can be reloaded on next launch. */
    fun persistUriPermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Some providers don't grant persistable permission; resume will then fail gracefully.
        }
    }

    suspend fun rememberLast(uri: Uri?) = prefs.setLastCharacterUri(uri?.toString())
}
