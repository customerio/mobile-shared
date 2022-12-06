package io.customer.shared.serializer

import io.customer.shared.serializer.CustomAttributeContextualSerializer.Companion.DEFAULT_FALLBACK_VALUE
import io.customer.shared.util.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Parses an object to json, null if no platform specific parsing is needed. This is called
 * initially for all non-null values and does not need to handle common parsing like e.g. String,
 * Int, etc.
 *
 * @see [parseAnyToJsonOrNull] responsible for calling this.
 */
internal expect fun parseFromAnyToJsonOrNull(value: Any): JsonElement?

/**
 * Transforms json to type object, null if no platform specific parsing is needed. This is called
 * initially to for all non-null String values and does not need to handle common transformation.
 *
 * @see [parseJsonToAnyOrNull] responsible for calling this.
 */
internal expect fun parseFromJsonToAnyOrNull(jsonPrimitive: JsonPrimitive): Any?

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
        val jsonElement = parseAnyToJsonOrNull(value = value).also { json ->
            if (json == null) {
                logger.error("Unable to serialize $value, replacing with $DEFAULT_FALLBACK_VALUE")
            }
        } ?: DEFAULT_FALLBACK_VALUE
        encoder.encodeSerializableValue(delegateSerializer, jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonElement = decoder.decodeSerializableValue(delegateSerializer)
        return parseJsonToAnyOrNull(jsonElement = jsonElement) ?: DEFAULT_FALLBACK_VALUE
    }

    companion object {
        val DEFAULT_FALLBACK_VALUE = JsonNull
    }
}

/**
 * Maps objects to json elements. Basic primitive types and collections are supported. All
 * unknown object types mapped using [CustomAttributeSerializer] if available.
 *
 * @return mapped [JsonElement] if successful; null otherwise.
 */
private fun CustomAttributeContextualSerializer.parseAnyToJsonOrNull(value: Any?): JsonElement? {
    return if (value == null) JsonNull
    else parseFromAnyToJsonOrNull(value = value) ?: when (value) {
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value = value)
        is Number -> JsonPrimitive(value = value)
        is String -> JsonPrimitive(value = value)
        is Enum<*> -> JsonPrimitive(value = value.name)
        is Array<*> -> JsonArray(
            value.map { item -> parseAnyToJsonOrNull(item) ?: JsonNull },
        )
        is Iterable<*> -> JsonArray(
            value.map { item -> parseAnyToJsonOrNull(item) ?: JsonNull },
        )
        is Map<*, *> -> {
            JsonObject(
                value.entries.associate { (key, item) ->
                    key.toString() to (parseAnyToJsonOrNull(item) ?: JsonNull)
                },
            )
        }
        else -> serializer?.serialize(value = value)?.let { json -> json.parseToJsonElement() }
    }
}

/**
 * Used for transforming objects from custom serializer to json classes without forcing client
 * apps to use Kotlin JSON classes.
 */
private fun Any?.parseToJsonElement(): JsonElement = if (this != null)
    when (this) {
        is Boolean -> JsonPrimitive(value = this)
        is Number -> JsonPrimitive(value = this)
        is String -> JsonPrimitive(value = this)
        is Map<*, *> -> JsonObject(
            entries.associate { (key, item) -> key.toString() to item.parseToJsonElement() },
        )
        else -> JsonPrimitive(value = this.toString())
    } else JsonNull

/**
 * The method tries to map [JsonElement] to best matching Kotlin primitive types. Mainly
 * responsible for mapping collections, all non-null primitive types are mapped using
 * [parseJsonToAnyOrNull] for [JsonPrimitive].
 *
 * @return mapped object if successful; null otherwise.
 */
private fun CustomAttributeContextualSerializer.parseJsonToAnyOrNull(jsonElement: JsonElement): Any? {
    return when (jsonElement) {
        is JsonNull -> null
        is JsonPrimitive -> parseJsonToAnyOrNull(jsonPrimitive = jsonElement)
        is JsonObject -> jsonElement.entries.associate { item ->
            item.key to parseJsonToAnyOrNull(item.value)
        }
        is JsonArray -> jsonElement.map { item -> parseJsonToAnyOrNull(item) }
    }
}

/**
 * The method tries to map [JsonPrimitive] to best matching Kotlin primitive types, if it fails to
 * map the value to any type, the value is passed to [CustomAttributeSerializer] to add
 * support for unknown objects.
 *
 * @return mapped object if successful; null otherwise.
 */
private fun CustomAttributeContextualSerializer.parseJsonToAnyOrNull(jsonPrimitive: JsonPrimitive): Any? {
    val content = jsonPrimitive.content
    return when {
        jsonPrimitive.isString -> content
        content.equals(other = "null", ignoreCase = true) -> null
        else -> parseFromJsonToAnyOrNull(jsonPrimitive = jsonPrimitive)
            ?: content.toBooleanStrictOrNull()
            ?: content.toIntOrNull()
            ?: content.toLongOrNull()
            ?: content.toFloatOrNull()
            ?: content.toDoubleOrNull()
            ?: serializer?.deserialize(json = content)
    }
}
