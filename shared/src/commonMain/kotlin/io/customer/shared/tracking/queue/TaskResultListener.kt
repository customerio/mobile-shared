package io.customer.shared.tracking.queue

/**
 * Listener class to get callbacks for asynchronous operations.
 */
fun interface TaskResultListener<TResult : Any?> {
    fun onComplete(result: Result<TResult>)
}

/**
 * Helper extension to mark queue tasks completion successful.
 */
internal fun TaskResultListener<Boolean>.success() {
    onComplete(Result.success(value = true))
}

/**
 * Helper extension to pass results with reason for failed queue tasks.
 */
internal fun TaskResultListener<Boolean>.failure(exception: Throwable) {
    onComplete(Result.failure(exception = exception))
}
