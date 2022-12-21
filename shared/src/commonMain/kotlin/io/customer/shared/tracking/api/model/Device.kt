/**
 * Kotlin serialization does not allow parsing of generic objects without custom/contextual
 * serializers.
 *
 * See CustomAttributeContextualSerializer for more details on this issue.
 */
@file:UseContextualSerialization(Any::class)

package io.customer.shared.tracking.api.model

import io.customer.shared.common.CustomAttributes
import kotlinx.serialization.SerialName
import kotlinx.serialization.UseContextualSerialization

/**
 * Device model to be used when making device tracking request.
 */
@kotlinx.serialization.Serializable
data class Device(
    @SerialName("token") val token: String,
    @SerialName("lastUsed") val lastUsed: Long? = null,
    @SerialName("attributes") val attributes: CustomAttributes? = null,
    @SerialName("platform") val platform: String? = null,
)
