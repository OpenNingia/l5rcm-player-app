package com.l5rcm.companion.data.save

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** Raised when a `.l5r` payload lacks the stable `uuid` identity the companion requires. */
class MissingUuidException : Exception(
    "This character has no id yet. Save it again with the latest version of L5RCM, " +
        "or import it via QR code, then try again.",
)

/**
 * Reads the stable `uuid` identity from a raw `.l5r` JSON document (docs/FILE_FORMAT_L5R.md §2.1).
 *
 * The companion requires every imported character to already carry a `uuid` (assigned/back-filled
 * by the desktop app) so its local `<uuid>.l5r` copy and future data overlay stay aligned across
 * edits, renames and re-shares. Saves without one are rejected rather than tagged locally.
 *
 * Pure JVM — unit-testable.
 */
object SaveIdentity {

    /** Filenames are `<uuid>.l5r`; keep the id to a safe, traversal-proof token. */
    private val SAFE_UUID = Regex("[A-Za-z0-9._-]{1,64}")

    /** The save's `uuid`, or null if absent, blank, non-string, or not a safe filename token. */
    fun uuidOf(rawJson: String): String? =
        (SaveParser.json.parseToJsonElement(rawJson).jsonObject["uuid"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.matches(SAFE_UUID) }

    /** The save's `uuid`, or throws [MissingUuidException] if it is absent/invalid. */
    fun requireUuid(rawJson: String): String = uuidOf(rawJson) ?: throw MissingUuidException()
}
