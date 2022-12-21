package io.customer.shared.tracking.api.model

import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.TrackingError

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
    get() = statusCode in 200..299

internal val BatchTrackingResponse.isServerUnavailable: Boolean
    get() = statusCode >= 500
