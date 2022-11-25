package io.customer.shared.tracking.api.model

import kotlinx.serialization.SerialName

/**
 * Request class only to be used when making batch tracking request.
 */
@kotlinx.serialization.Serializable
internal data class BatchTrackingRequestBody(
    @SerialName("batch") val batch: List<TrackingRequest>,
)

/**
 * Response class only to be used when making batch tracking request.
 */
@kotlinx.serialization.Serializable
internal data class BatchTrackingResponseBody(
    @SerialName("meta") val meta: BatchTrackingResponseMeta?,
    @SerialName("errors") val errors: List<TrackingError>?,
)

@kotlinx.serialization.Serializable
internal data class BatchTrackingResponseMeta(
    val error: String?,
)

/**
 * Internal class to pass batch tracking response from network layer. The class is mostly
 * independent of response received from batch tracking api and network layer will be responsible
 * for mapping api response to this class.
 */
internal data class BatchTrackingResponse(
    val statusCode: Int,
    val errors: List<TrackingError>,
)
