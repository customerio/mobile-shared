package io.customer.shared.serializer

import io.customer.shared.common.CustomAttributes

/**
 * Serializer interface to support custom data models in [CustomAttributes] that are not available
 * and/or cannot be parsed by the SDK.
 */
interface CustomAttributeSerializer {
    /**
     * Parses json object to an object; can return null if no mapping is available
     */
    fun deserialize(json: String): Any?

    /**
     * Parses object to json; returning null here will result in the custom attribute original value
     * being dropped from the attributes.
     *
     * All primitive types are passed on as it is, while JSON Object can be passed by returning
     * map here.
     */
    fun serialize(value: Any): Any?
}
