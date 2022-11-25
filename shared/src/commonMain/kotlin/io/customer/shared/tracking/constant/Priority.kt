package io.customer.shared.tracking.constant

/**
 * Sets the priority of tasks. Tasks with negative value are higher in property as it is easier to
 * sort ascending in our current queries.
 */
object Priority {
    const val DEFAULT = 0L
    const val HIGH = -1L
    const val LOW = 1L
}
