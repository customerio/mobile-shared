package io.customer.shared.tracking.model

import io.customer.shared.tracking.api.model.TrackingErrorReason
import io.customer.shared.tracking.constant.QueueTaskStatus

internal data class TaskResponse(
    val taskStatus: QueueTaskStatus,
    val statusCode: Long?,
    val errorReason: TrackingErrorReason?,
    val id: String,
)
