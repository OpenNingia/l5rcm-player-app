package com.l5rcm.companion.data.catalog

/** A downloadable datapack asset from the official GitHub release. */
data class CatalogEntry(
    val name: String,
    val version: String,
    val url: String,
    val size: Long,
) {
    val isCore: Boolean get() = name.lowercase().startsWith("core")
}

/** Failures the catalog/download surfaces, mapped to user messages by the UI. */
sealed class CatalogException(message: String) : Exception(message) {
    /** Could not reach the host (DNS / connection / timeout). */
    class Offline(message: String = "offline") : CatalogException(message)

    /** GitHub returned 403 with the rate limit exhausted. */
    class RateLimited(message: String = "rate limited") : CatalogException(message)

    /** Any other catalog failure (bad JSON, disallowed host, HTTP error, bad archive). */
    class Generic(message: String) : CatalogException(message)
}
