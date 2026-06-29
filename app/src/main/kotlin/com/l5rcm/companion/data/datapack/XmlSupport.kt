package com.l5rcm.companion.data.datapack

import org.xmlpull.v1.XmlPullParser

/**
 * Supplies a fresh [XmlPullParser]. Production wires this to
 * `android.util.Xml.newPullParser()`; JVM unit tests wire it to a kxml2 parser.
 * This keeps the parser code device-free and unit-testable.
 */
fun interface XmlParserFactory {
    fun create(): XmlPullParser
}

// --- Small reader helpers shared by the entity readers. ---

internal fun XmlPullParser.attr(name: String): String? = getAttributeValue(null, name)

internal fun XmlPullParser.attrInt(name: String, default: Int = 0): Int =
    attr(name)?.trim()?.toIntOrNull() ?: default

internal fun XmlPullParser.attrDouble(name: String, default: Double = 0.0): Double =
    attr(name)?.trim()?.toDoubleOrNull() ?: default

internal fun XmlPullParser.attrBool(name: String, default: Boolean = false): Boolean =
    attr(name)?.trim()?.lowercase()?.let { it == "true" || it == "1" } ?: default

/**
 * Iterates the direct children of the element the parser is currently positioned on
 * (must be a START_TAG). For each child START_TAG, invokes [block] with the tag name;
 * after [block] returns, advances past that child regardless of how much it consumed.
 */
internal inline fun XmlPullParser.forEachChild(block: (tag: String) -> Unit) {
    val parentDepth = depth
    while (true) {
        val ev = next()
        if (ev == XmlPullParser.END_DOCUMENT) return
        if (ev == XmlPullParser.END_TAG && depth == parentDepth) return
        if (ev == XmlPullParser.START_TAG) {
            val childDepth = depth
            block(name)
            skipToEndOf(childDepth)
        }
    }
}

/** Advances until the END_TAG at [childDepth] (no-op if already there). */
internal fun XmlPullParser.skipToEndOf(childDepth: Int) {
    while (!(eventType == XmlPullParser.END_TAG && depth == childDepth)) {
        if (eventType == XmlPullParser.END_DOCUMENT) return
        next()
    }
}

/** Reads the concatenated text content of the current element, leaving the parser on its END_TAG. */
internal fun XmlPullParser.readText(): String {
    val sb = StringBuilder()
    val d = depth
    while (true) {
        val ev = next()
        when {
            ev == XmlPullParser.TEXT -> sb.append(text)
            ev == XmlPullParser.END_TAG && depth == d -> break
            ev == XmlPullParser.END_DOCUMENT -> break
        }
    }
    return sb.toString().trim()
}

/** Reads a `<Tags><Tag>…</Tag></Tags>` container into a list of strings. */
internal fun XmlPullParser.readTags(): List<String> {
    val out = ArrayList<String>()
    forEachChild { tag -> if (tag == "Tag") out.add(readText()) }
    return out
}
