package io.customer.shared.work

import io.customer.shared.common.AtomicReference
import io.customer.shared.util.Dispatcher
import io.customer.shared.util.Logger
import kotlinx.coroutines.*

interface WorkDispatcher {
    fun launchShared(block: suspend CoroutineScope.() -> Unit): Job
    fun launchNew(block: suspend CoroutineScope.() -> Unit): Job
    fun launchUnique(key: String, block: suspend CoroutineScope.() -> Unit): Job
}

internal class WorkDispatcherImpl(
    private val logger: Logger,
    private val dispatcher: Dispatcher,
) : WorkDispatcher {
    private val coroutineScopeHolder: AtomicReference<CoroutineScope> = AtomicReference()
    private val defaultExceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.fatal("Coroutine failed with error: ${exception.message}, restarting scope")
        coroutineScopeHolder.clearInstance()
    }

    private fun createCoroutineScope(
        context: CoroutineDispatcher,
    ) = CoroutineScope(context = context)

    override fun launchShared(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScopeHolder.initializeAndGet {
            createCoroutineScope(context = dispatcher.background())
        }.launch(context = defaultExceptionHandler) {
            try {
                block()
            } catch (ex: Exception) {
                logger.fatal("Shared coroutine block failed with error: ${ex.message}")
            }
        }
    }

    override fun launchNew(block: suspend CoroutineScope.() -> Unit): Job {
        return createCoroutineScope(context = dispatcher.background()).launch(
            context = CoroutineExceptionHandler { _, exception ->
                logger.fatal("Coroutine failed with error: ${exception.message}, aborting scope")
            },
        ) {
            try {
                block()
            } catch (ex: Exception) {
                logger.fatal("Scoped coroutine block failed with error: ${ex.message}")
            }
        }
    }

    private val coroutineMap = mutableMapOf<String, CoroutineScope>()

    override fun launchUnique(key: String, block: suspend CoroutineScope.() -> Unit): Job {
        val coroutineScope = coroutineMap.getOrPut(key) {
            logger.debug("Coroutine scope for [$key] not found, creating new scope")
            createCoroutineScope(context = dispatcher.background())
        }
        return coroutineScope.launch(
            context = CoroutineExceptionHandler { _, exception ->
                logger.fatal("Coroutine failed with error: ${exception.message}, clearing scope $key")
            },
        ) {
            try {
                block()
            } catch (ex: Exception) {
                logger.fatal("[$key] coroutine block failed with error: ${ex.message}")
            }

        }
    }
}
