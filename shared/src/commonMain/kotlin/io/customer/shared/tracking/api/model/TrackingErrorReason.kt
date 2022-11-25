package io.customer.shared.tracking.api.model

import kotlinx.serialization.SerialName

/**
 * Response error reasons enum to determine next action on failed tracking events.
 */
@kotlinx.serialization.Serializable
enum class TrackingErrorReason {
    @SerialName("invalid")
    INVALID,

    @SerialName("parse_error")
    PARSE_ERROR,

    @SerialName("required")
    REQUIRED,
}
