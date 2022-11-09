package io.customer.shared.tracking.api.model

import io.customer.shared.common.CustomAttributes
import io.customer.shared.getPlatform
import kotlinx.serialization.SerialName

/**
 * Device model to be used when making device tracking request.
 */
@kotlinx.serialization.Serializable
data class Device(
    @SerialName("token") val token: String,
    @SerialName("lastUsed") val lastUsed: Long? = null,
    @SerialName("attributes") val attributes: CustomAttributes? = null,
    @SerialName("platform") val platform: String? = getPlatform().name,
)
