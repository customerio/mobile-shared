package io.customer.shared.serializer

import io.customer.shared.serializer.CustomAttributeContextualSerializer.Companion.DEFAULT_FALLBACK_VALUE
import io.customer.shared.util.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Contextual serializer for Kotlin serialization to add support for generic objects. The
 * serializer assures the parsing is safe and fallbacks to [DEFAULT_FALLBACK_VALUE] where it
 * fails to process.
 *
 * To support [Any] object in the class, the class file should add the following annotation
 *
 * [@file:UseContextualSerialization(Any::class)]
 */
internal class CustomAttributeContextualSerializer(
    private val logger: Logger,
    internal val serializer: CustomAttributeSerializer?,
) : KSerializer<Any> {
    private val delegateSerializer = JsonElement.serializer()
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonElement = toJsonElementOrNull(value = value).also { json ->
            if (json == null) {
                logger.error("Unable to serialize $value, replacing with $DEFAULT_FALLBACK_VALUE")
            }
        } ?: JsonPrimitive(value = DEFAULT_FALLBACK_VALUE)
        encoder.encodeSerializableValue(delegateSerializer, jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonElement = decoder.decodeSerializableValue(delegateSerializer)
        return toAnyValueOrNull(jsonElement = jsonElement) ?: DEFAULT_FALLBACK_VALUE
    }

    companion object {
        const val DEFAULT_FALLBACK_VALUE = "NULL"
    }
}

/**
 * Maps objects to json elements. Basic primitive types and collections are supported. All
 * unknown object types mapped using [CustomAttributeSerializer] if available.
 *
 * @return mapped [JsonElement] if successful; null otherwise.
 */
private fun CustomAttributeContextualSerializer.toJsonElementOrNull(value: Any?): JsonElement? {
    return when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value = value)
        is Number -> JsonPrimitive(value = value)
        is String -> JsonPrimitive(value = value)
        is Iterable<*> -> JsonArray(value.mapNotNull { item -> toJsonElementOrNull(item) })
        is Map<*, *> -> {
            JsonObject(
                value.mapNotNull { item ->
                    toJsonElementOrNull(item.value)?.let { result -> item.key.toString() to result }
                }.toMap(),
            )
        }
        else -> serializer?.serialize(value = value)?.let { json -> JsonPrimitive(value = json) }
    }
}

/**
 * The method tries to map [JsonElement] to best matching Kotlin primitive types. Mainly
 * responsible for mapping collections, all non-null primitive types are mapped using
 * [toAnyValueOrNull] for [JsonPrimitive].
 *
 * @return mapped object if successful; null otherwise.
 */
private fun CustomAttributeContextualSerializer.toAnyValueOrNull(jsonElement: JsonElement): Any? {
    return when (jsonElement) {
        is JsonNull -> null
        is JsonPrimitive -> toAnyValueOrNull(jsonPrimitive = jsonElement)
        is JsonObject -> jsonElement.entries.associate { item ->
            item.key to toAnyValueOrNull(item.value)
        }
        is JsonArray -> jsonElement.map { item -> toAnyValueOrNull(item) }
    }
}

/**
 * The method tries to map [JsonPrimitive] to best matching Kotlin primitive types, if it fails to
 * map the value to any type, the value is passed to [CustomAttributeSerializer] to add
 * support for unknown objects.
 *
 * @return mapped object if successful; null otherwise.
 */
private fun CustomAttributeContextualSerializer.toAnyValueOrNull(jsonPrimitive: JsonPrimitive): Any? {
    val content = jsonPrimitive.content
    return when {
        jsonPrimitive.isString -> content
        content.equals(other = "null", ignoreCase = true) -> null
        else -> content.toBooleanStrictOrNull()
            ?: content.toIntOrNull()
            ?: content.toLongOrNull()
            ?: content.toFloatOrNull()
            ?: content.toDoubleOrNull()
            ?: serializer?.deserialize(json = content)
    }
}
