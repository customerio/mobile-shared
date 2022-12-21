package io.customer.shared.serializer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

internal actual fun parseFromAnyToJsonOrNull(value: Any): JsonElement? = when (value) {
    is Date -> JsonPrimitive(value.time)
    is BigInteger -> JsonPrimitive(value)
    is BigDecimal -> JsonPrimitive(value)
    else -> null
}

internal actual fun parseFromJsonToAnyOrNull(jsonPrimitive: JsonPrimitive): Any? {
    val content = jsonPrimitive.content
    return jsonPrimitive.longOrNull?.let { Date(it) }
        ?: content.toBigIntegerOrNull()
        ?: content.toBigDecimalOrNull()
}
