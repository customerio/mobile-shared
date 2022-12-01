@file:UseContextualSerialization(Any::class)

package io.customer.shared.tracking.api.model

import io.customer.shared.common.CustomAttributes
import kotlinx.serialization.SerialName
import kotlinx.serialization.UseContextualSerialization

/**
 * Request class to be used for individual tracking requests.
 */
// TODO: Find ways to break down into smaller classes and overcome kotlin type issue in serialization
// of sealed classes with generic parsing
@kotlinx.serialization.Serializable
internal class TrackingRequest(
    @SerialName("type") val type: String,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("identifiers") val identifiers: CustomAttributes? = null,
    @SerialName("attributes") val attributes: CustomAttributes? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("device") val device: Device? = null,
    @SerialName("metric") val metric: Metric? = null,
)
