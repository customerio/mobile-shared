package io.customer.shared.tracking.queue

/**
 * The source identifier that triggers the request for queue. It can be helpful in decide whether
 * to skip the call or wait when the queue is running. e.g. we can skip queue requests from [TIMER]
 * when queue is already running but we should not skip requests from [DATABASE] as it may contain
 * notification for tasks that can trigger the queue run.
 */
internal enum class QueueTriggerSource {
    DATABASE,
    QUEUE,
    TIMER,
}
