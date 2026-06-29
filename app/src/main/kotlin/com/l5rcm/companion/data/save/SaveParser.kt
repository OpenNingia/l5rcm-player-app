package com.l5rcm.companion.data.save

import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * Parses `.l5r` JSON into a [SaveModel].
 *
 * Lenient by design (docs/FILE_FORMAT_L5R.md §1): unknown/legacy keys are ignored,
 * numbers may be quoted, and every field has a default. Pure JVM — unit-testable
 * without an Android device.
 */
object SaveParser {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(text: String): SaveModel = json.decodeFromString(SaveModel.serializer(), text)

    fun parse(stream: InputStream): SaveModel =
        parse(stream.readBytes().toString(Charsets.UTF_8))
}
