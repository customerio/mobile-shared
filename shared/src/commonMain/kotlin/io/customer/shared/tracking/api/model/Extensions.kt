package io.customer.shared.tracking.api.model

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

internal fun Activity.toTrackingRequest(profileIdentifier: String) = when (this) {
    is Activity.IdentifyProfile -> TrackingRequest(
        profileIdentifier = profileIdentifier,
    )
    is Activity.AddDevice -> TrackingRequest(
        profileIdentifier = profileIdentifier,
        device = device,
    )
    is Activity.DeleteDevice -> TrackingRequest(
        profileIdentifier = profileIdentifier,
        device = device,
    )
    is Activity.Metric -> TrackingRequest(
        profileIdentifier = profileIdentifier,
        metric = Metric(
            deliveryID = deliveryId,
            deviceToken = deviceToken,
            event = metricEvent,
            timestamp = timestamp,
        ),
    )
    is Activity.Page -> TrackingRequest(
        profileIdentifier = profileIdentifier,
        name = name,
    )
    is Activity.Screen -> TrackingRequest(
        profileIdentifier = profileIdentifier,
        name = name,
    )
    is Activity.Event -> TrackingRequest(
        profileIdentifier = profileIdentifier,
        name = name,
    )
}

private fun Activity.TrackingRequest(
    profileIdentifier: String,
    name: String? = null,
    device: Device? = null,
    metric: Metric? = null,
) = TrackingRequest(
    type = type,
    timestamp = timestamp,
    identifiers = mapOf("id" to profileIdentifier),
    attributes = attributes,
    name = name,
    device = device,
    metric = metric,
)
