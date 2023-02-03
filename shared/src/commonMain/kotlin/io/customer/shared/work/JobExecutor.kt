package io.customer.shared.work

import io.customer.shared.util.Dispatcher
import io.customer.shared.util.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Helper class to simplify running operations on coroutines and make exception handling easier.
 * Every class should have its own instance of [JobExecutor] to avoid sharing coroutines with other
 * classes.
 */
internal interface JobExecutor {
    /**
     * Launches the given block on background dispatcher.
     *
     * @param onError called when coroutine crashes.
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     * @return reference to the coroutine Job.
     */
    fun launchOnBackground(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    /**
     * Launches the given block on background dispatcher.
     *
     * @param onError called when coroutine crashes.
     * @param block suspended block to run the code on background.
     * @return reference to the coroutine Job.
     */
    fun launchOnMain(
        onError: (Throwable) -> Unit = {},
        block: CoroutineScope.() -> Unit,
    ): Job
}

internal class JobExecutorImpl(
    private val logger: Logger,
    private val dispatcher: Dispatcher,
) : JobExecutor {
    private var _backgroundScope: CoroutineScope? = null
    private val backgroundScope: CoroutineScope
        get() = _backgroundScope ?: CoroutineScope(
            dispatcher.background(),
        ).apply { _backgroundScope = this }

    private var _mainScope: CoroutineScope? = null
    private val mainScope: CoroutineScope
        get() = _mainScope ?: CoroutineScope(dispatcher.main()).apply { _mainScope = this }

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
        block = block,
    )

    override fun launchOnMain(
        onError: (Throwable) -> Unit,
        block: CoroutineScope.() -> Unit,
    ): Job = mainScope.launch(
        context = CoroutineExceptionHandler { _, exception ->
            _mainScope = null
            logger.fatal(
                message = "Main coroutine execution scope crashed with error: ${exception.message}, restarting scope",
                exception = exception,
            )
            onError(exception)
        },
        block = block,
    )
}
