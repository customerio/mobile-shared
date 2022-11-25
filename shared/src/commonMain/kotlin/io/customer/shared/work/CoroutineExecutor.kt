package io.customer.shared.work

import io.customer.shared.common.LazyReference
import io.customer.shared.util.Dispatcher
import io.customer.shared.util.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Helper class to simplify running operations on coroutines and make exception handling easier.
 * Every class should have its own instance of [CoroutineExecutor] to avoid sharing of coroutines
 * with other classes.
 */
internal interface CoroutineExecutor {
    fun launchShared(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job
}

internal class CoroutineExecutorImpl(
    private val logger: Logger,
    private val dispatcher: Dispatcher,
) : CoroutineExecutor {
    private val coroutineScopeHolder: LazyReference<CoroutineScope> = LazyReference()

    override fun launchShared(
        onError: (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            logger.fatal("Coroutine execution scope failed with error: ${exception.message}, restarting scope")
            coroutineScopeHolder.clearInstance()
            onError(exception)
        }

        return coroutineScopeHolder.initializeAndGet {
            CoroutineScope(context = dispatcher.background())
        }.launch(context = exceptionHandler) {
            val result = kotlin.runCatching {
                block()
            }
            result.onFailure { ex ->
                logger.fatal("Coroutine execution failed with error: ${ex.message}")
                onError(ex)
            }
        }
    }
}
