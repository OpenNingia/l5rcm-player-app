package com.l5rcm.companion.data.save

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Deserializes the save's open `properties` map keeping only **string-valued** entries.
 *
 * The desktop's `properties` is an open dict and, in practice, carries non-string junk
 * (`"money": [0,0,0]`, `"equip": []`) alongside the personal-info strings. Those array
 * values would break a plain `Map<String, String>`, so we drop any non-primitive value.
 */
object LenientStringMapSerializer : KSerializer<Map<String, String>> {
    private val delegate = MapSerializer(String.serializer(), String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, String> {
        val json = decoder as? JsonDecoder ?: return emptyMap()
        val obj = json.decodeJsonElement().jsonObject
        return buildMap {
            obj.forEach { (key, value) ->
                if (value is JsonPrimitive) put(key, value.content)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, String>) {
        val json = encoder as? JsonEncoder ?: return
        json.encodeJsonElement(
            JsonObject(value.mapValues { JsonPrimitive(it.value) }),
        )
    }
}
