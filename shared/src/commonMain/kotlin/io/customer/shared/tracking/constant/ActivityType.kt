package io.customer.shared.tracking.constant

/**
 * Object class to hold all supported event types for tracking.
 *
 * We cannot use enum instead as type is being used in annotation argument where only compile-time
 * constants are supported.
 */
internal object ActivityType {
    const val ADD_DEVICE = "add_device"
    const val DELETE_DEVICE = "delete_device"
    const val EVENT = "event"
    const val IDENTIFY = "identify"
    const val METRIC = "metric"
    const val PAGE = "page"
    const val SCREEN = "screen"
}
