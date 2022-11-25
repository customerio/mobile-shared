package io.customer.shared.work

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Interface to make execution with coroutines easy. The class depends on [CoroutineExecutor] to
 * run in background and exposes simpler methods to run on background and handle exceptions in
 * coroutines.
 */
internal interface CoroutineExecutable {
    val executor: CoroutineExecutor

    /**
     * Called whenever a coroutine fails with exception. Child classes can implement it to perform
     * operations expected to be performed after coroutine has failed. However, this is to prevent
     * unwanted crashes and behaviors. Child class should not rely on this and catch the exceptions
     * if they are expecting the operations to fail in their coroutines.
     */
    fun onCoroutineFailed(exception: Throwable) {}
}

internal fun CoroutineExecutable.runSuspended(
    block: suspend CoroutineScope.() -> Unit,
): Job = executor.launchShared(
    onError = { ex -> onCoroutineFailed((ex)) },
    block = block,
)
