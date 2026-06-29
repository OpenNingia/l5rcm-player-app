package com.l5rcm.companion.data.datapack

import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/** Production [XmlParserFactory] backed by the platform pull parser. */
object AndroidXmlParserFactory : XmlParserFactory {
    override fun create(): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    }
}
