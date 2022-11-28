package io.customer.shared.common

typealias CustomAttributesCompat = Map<String, Any>
typealias QueueTaskResult = Boolean

/**
 * Code below this line is a temporary work around for the issue raised by Kotlin serialization.
 * ************************************************************************************************
 * ERROR: Serializer has not been found for type 'Any'. To use context serializer as fallback,
 * explicitly annotate type or property with @Contextual
 * ************************************************************************************************
 * Kotlin serialization does not allow generic serialization of Any classes. We will need to provide
 * custom serializers to support map with Any object in our SDK.
 * The names are given in way to have minimum possible changes once the issue is fixed. The final
 * file look like follows.
 * ************************************************
 * typealias CustomAttributes = Map<String, Any>
 * ************************************************
 */
// TODO: Add support for generic and optional serialization
internal typealias CustomAttributes = Map<String, String>

internal fun CustomAttributesCompat.fix(): CustomAttributes {
    return entries.associate { it.key to it.value.toString() }
}
