package com.l5rcm.companion

import com.l5rcm.companion.data.datapack.XmlParserFactory
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser

/** JVM [XmlParserFactory] backed by kxml2 (test-only). */
val TestXmlParserFactory = XmlParserFactory {
    KXmlParser().apply { setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) }
}

/** Loads a classpath test resource as text. */
fun resourceText(path: String): String =
    object {}.javaClass.classLoader!!.getResourceAsStream(path)!!
        .readBytes().toString(Charsets.UTF_8)
