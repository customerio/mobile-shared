package io.customer.shared.tracking.model

import io.customer.shared.tracking.api.model.TrackingErrorReason
import kotlinx.serialization.SerialName

/**
 * Response error class to be used when making tracking request. The class will is shared between
 * network and other layers and is used by background queue to determine next steps for the task.
 */
@kotlinx.serialization.Serializable
internal data class TrackingError(
    @SerialName("batch_index") val batchIndex: Int?,
    @SerialName("reason") val reason: TrackingErrorReason,
    @SerialName("field") val field: String,
    @SerialName("message") val message: String,
)
