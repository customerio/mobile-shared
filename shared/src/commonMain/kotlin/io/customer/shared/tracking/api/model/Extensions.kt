package io.customer.shared.tracking.api.model

import io.customer.shared.sdk.meta.IdentityType
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.type

internal val TrackingError.taskStatus: QueueTaskStatus
    get() = if (reason.isFixable) QueueTaskStatus.FAILED
    else QueueTaskStatus.INVALID

internal val TrackingErrorReason.isFixable: Boolean
    get() = when (this) {
        TrackingErrorReason.REQUIRED -> true
        TrackingErrorReason.INVALID,
        TrackingErrorReason.PARSE_ERROR,
        -> false
    }

internal val BatchTrackingResponse.isSuccessful: Boolean
    get() = statusCode in 200..300

internal val BatchTrackingResponse.isServerUnavailable: Boolean
    get() = statusCode >= 500

internal fun Activity.toTrackingRequest(
    identityType: IdentityType,
    profileIdentifier: String,
) = when (this) {
    is Activity.IdentifyProfile -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
    )
    is Activity.AddDevice -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        device = device,
    )
    is Activity.DeleteDevice -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        device = device,
    )
    is Activity.Metric -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        metric = Metric(
            deliveryID = deliveryId,
            deviceToken = deviceToken,
            event = metricEvent,
            timestamp = timestamp,
        ),
    )
    is Activity.Page -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        name = name,
    )
    is Activity.Screen -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        name = name,
    )
    is Activity.Event -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        name = name,
    )
}

private val IdentityType.apiKey: String
    get() = when (this) {
        IdentityType.CIO_ID -> "cio_id"
        IdentityType.AUTO_IDENTIFY -> "auto"
        IdentityType.ID -> "id"
        IdentityType.EMAIL -> "email"
    }

private fun Activity.TrackingRequest(
    identityType: IdentityType,
    profileIdentifier: String,
    name: String? = null,
    device: Device? = null,
    metric: Metric? = null,
) = TrackingRequest(
    type = type,
    timestamp = timestamp,
    identifiers = mapOf(identityType.apiKey to profileIdentifier),
    attributes = attributes,
    name = name,
    device = device,
    metric = metric,
)
