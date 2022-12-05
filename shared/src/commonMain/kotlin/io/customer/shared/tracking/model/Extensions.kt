package io.customer.shared.tracking.model

import io.customer.shared.tracking.constant.ActivityType
import io.customer.shared.tracking.constant.QueueTaskStatus
import kotlinx.serialization.SerialName

/**
 * Extension property to extract sealed class type defined in [SerialName] annotation.
 */
internal val Activity.type: String
    get() = when (this) {
        is Activity.AddDevice -> ActivityType.ADD_DEVICE
        is Activity.DeleteDevice -> ActivityType.DELETE_DEVICE
        is Activity.IdentifyProfile -> ActivityType.IDENTIFY
        is Activity.Event -> ActivityType.EVENT
        is Activity.Metric -> ActivityType.METRIC
        is Activity.Page -> ActivityType.PAGE
        is Activity.Screen -> ActivityType.SCREEN
    }

/**
 * Determines whether the task update should be counted as result of failed attempt or not. We do
 * not count successful attempts in retry count.
 */
internal val TaskResponse.shouldCountAsRetry: Boolean
    get() = taskStatus != QueueTaskStatus.SENT
