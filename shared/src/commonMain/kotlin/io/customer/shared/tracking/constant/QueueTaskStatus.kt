package io.customer.shared.tracking.constant

/**
 * Task status to determine current status of tasks in queue.
 */
enum class QueueTaskStatus {
    /**
     * Initial status of newly added tasks.
     */
    PENDING,

    /**
     * Tasks being processed by the queue.
     */
    QUEUED,

    /**
     * Tasks that qualified for batching and are being sent to the server.
     */
    SENDING,

    /**
     * Tasks that have been sent successfully to server. The tasks do not require to be processed
     * any further and can be deleted safely.
     */
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
