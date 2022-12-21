package io.customer.shared.serializer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal actual fun parseFromAnyToJsonOrNull(value: Any): JsonElement? {
    TODO("Not yet implemented")
}

internal actual fun parseFromJsonToAnyOrNull(jsonPrimitive: JsonPrimitive): Any? {
    TODO("Not yet implemented")
}
