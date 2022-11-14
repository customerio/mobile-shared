package io.customer.shared.tracking.api.model

import kotlinx.serialization.SerialName

/**
 * Response error class to be used when making tracking request. The class will is not limited to
 * network layer only and can be used by background queue to determine next steps.
 */
@kotlinx.serialization.Serializable
internal data class TrackingError(
    @SerialName("batch_index") val batchIndex: Int?,
    @SerialName("reason") val reason: TrackingErrorReason,
    @SerialName("field") val field: String,
    @SerialName("message") val message: String,
)
