package io.customer.shared.tracking.model

import io.customer.shared.common.CustomAttributes
import io.customer.shared.tracking.constant.ActivityType
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
 * Merges two maps together. For duplicate keys, the value in the receiver object will take
 * preference.
 */
internal fun CustomAttributes.merge(other: CustomAttributes?): CustomAttributes {
    return other.orEmpty() + this
}
