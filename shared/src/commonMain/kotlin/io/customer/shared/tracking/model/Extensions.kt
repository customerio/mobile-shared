package io.customer.shared.tracking.model

import io.customer.shared.tracking.constant.Action
import io.customer.shared.tracking.constant.QueueTaskStatus
import kotlinx.serialization.SerialName

/**
 * Extension property to extract sealed class type defined in [SerialName] annotation.
 */
internal val Activity.action: String
    get() = when (this) {
        is Activity.AddDevice -> Action.ADD_DEVICE
        is Activity.DeleteDevice -> Action.DELETE_DEVICE
        is Activity.IdentifyProfile -> Action.IDENTIFY
        is Activity.Event -> Action.EVENT
        is Activity.Metric -> Action.METRIC
        is Activity.Page -> Action.PAGE
        is Activity.Screen -> Action.SCREEN
    }

/**
 * Determines whether the task update should be counted as result of failed attempt or not. We do
 * not count successful attempts in retry count.
 */
internal val TaskResponse.shouldCountAsRetry: Boolean
    get() = taskStatus != QueueTaskStatus.SENT
