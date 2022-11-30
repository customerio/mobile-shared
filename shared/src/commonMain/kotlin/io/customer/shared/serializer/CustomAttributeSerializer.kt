package io.customer.shared.serializer

import io.customer.shared.common.CustomAttributes

/**
 * Serializer interface to support any type of data models in [CustomAttributes] from client app,
 * given that client app has provided serialization and deserialization implementation for the object.
 */
interface CustomAttributeSerializer {
    fun deserialize(json: String): Any?
    fun serialize(value: Any): String?
}
