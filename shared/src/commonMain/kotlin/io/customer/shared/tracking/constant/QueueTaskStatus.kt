package io.customer.shared.tracking.constant

/**
 * Task status to determine current status of tasks in queue.
 */
enum class QueueTaskStatus {
    PENDING,
    QUEUED,
    SENDING,
    SENT,

    /**
     * Tasks that could not be sent due to fixable reasons, and should be attempted to resend.
     */
    FAILED,

    /**
     * Tasks that failed due to non-fixable reason and could not be fixed automatically. We should
     * not try to resend them as they will fail everytime and clear from queue whenever possible.
     */
    INVALID,
}
