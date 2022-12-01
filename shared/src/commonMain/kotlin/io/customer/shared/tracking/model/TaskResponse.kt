package io.customer.shared.tracking.model

import io.customer.shared.tracking.api.model.TrackingErrorReason
import io.customer.shared.tracking.constant.QueueTaskStatus

/**
 * Data class to hold attributes to be updated in DB model after the task was attempted for sending.
 */
internal data class TaskResponse(
    val uuid: String,
    val taskStatus: QueueTaskStatus,
    val statusCode: Long? = null,
    val errorReason: TrackingErrorReason? = null,
)
