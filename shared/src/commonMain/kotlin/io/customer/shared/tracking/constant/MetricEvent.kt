package io.customer.shared.tracking.constant

import kotlinx.serialization.SerialName

/**
 * Metric event enum to determine possible metric events.
 */
@kotlinx.serialization.Serializable
enum class MetricEvent {
    @SerialName("delivered")
    DELIVERED,
    @SerialName("opened")
    OPENED,
    @SerialName("converted")
    CONVERTED,
    @SerialName("clicked")
    CLICKED,
}
