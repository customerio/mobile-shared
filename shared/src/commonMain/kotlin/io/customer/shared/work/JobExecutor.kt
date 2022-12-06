package io.customer.shared.work

import io.customer.shared.util.Dispatcher
import io.customer.shared.util.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Helper class to simplify running operations on coroutines and make exception handling easier.
 * Every class should have its own instance of [JobExecutor] to avoid sharing of coroutines
 * with other classes.
 */
internal interface JobExecutor {
    fun launchOnBackground(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    fun launchOnMain(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job
}

internal class JobExecutorImpl(
    private val logger: Logger,
    private val dispatcher: Dispatcher,
) : JobExecutor {
    private var _backgroundScope: CoroutineScope? = null
    private val backgroundScope: CoroutineScope
        get() = _backgroundScope ?: CoroutineScope(
            context = dispatcher.background(),
        ).apply { _backgroundScope = this }

    private var _mainScope: CoroutineScope? = null
    private val mainScope: CoroutineScope
        get() = _mainScope ?: CoroutineScope(
            context = dispatcher.main(),
        ).apply { _mainScope = this }

    override fun launchOnBackground(
        onError: (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = backgroundScope.launch(
        context = CoroutineExceptionHandler { _, exception ->
            _backgroundScope = null
            logger.fatal(
                message = "Background coroutine execution scope crashed with error: ${exception.message}, restarting scope",
                exception = exception,
            )
            onError(exception)
        },
        block = {
            kotlin.runCatching {
                block()
            }.onFailure { ex ->
                logger.fatal(
                    message = "Background coroutine execution failed with error: ${ex.message}",
                    exception = ex,
                )
                onError(ex)
            }
        },
    )

    override fun launchOnMain(
        onError: (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = mainScope.launch(
        context = CoroutineExceptionHandler { _, exception ->
            _mainScope = null
            logger.fatal(
                message = "Main coroutine execution scope crashed with error: ${exception.message}, restarting scope",
                exception = exception,
            )
            onError(exception)
        },
        block = {
            kotlin.runCatching {
                block()
            }.onFailure { ex ->
                logger.fatal(
                    message = "Main coroutine execution failed with error: ${ex.message}",
                    exception = ex,
                )
                onError(ex)
                ex.printStackTrace()
            }
        },
    )
}
