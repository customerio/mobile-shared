package io.customer.shared.tracking.queue

import io.customer.shared.common.QueueTaskResult
import io.customer.shared.common.Success

/**
 * Listener class to get callbacks for asynchronous operations.
 */
fun interface TaskResultListener<TResult : Any?> {
    fun onComplete(result: Result<TResult>)
}

/**
 * Helper extension to mark queue tasks completion successful.
 */
internal fun TaskResultListener<QueueTaskResult>.success() {
    onComplete(Result.success(value = QueueTaskResult.Success()))
}

/**
 * Helper extension to pass results with reason for failed queue tasks.
 */
internal fun TaskResultListener<QueueTaskResult>.failure(exception: Throwable) {
    onComplete(Result.failure(exception = exception))
}
