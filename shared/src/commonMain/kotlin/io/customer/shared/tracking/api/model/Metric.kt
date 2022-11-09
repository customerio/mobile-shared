package io.customer.shared.tracking.api.model

import io.customer.shared.tracking.constant.MetricEvent
import kotlinx.serialization.SerialName

/**
 * Metric model to be used when making metrics tracking request.
 */
@kotlinx.serialization.Serializable
internal data class Metric(
    @SerialName("delivery_id") val deliveryID: String,
    @SerialName("device_id") val deviceToken: String,
    @SerialName("event") val event: MetricEvent,
    @SerialName("timestamp") val timestamp: Long,
)
