package io.customer.shared.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher class to switch between main and background threads conveniently.
 */
internal interface Dispatcher {
    fun main(): CoroutineDispatcher
    fun background(): CoroutineDispatcher
}

internal expect fun applicationDispatcher(): Dispatcher

/**
 * Wrapper class to provide dispatchers using simple DIGraph and don't use actual/expect methods
 * directly so that it is easier to maintain tests.
 */
internal class KMMDispatcher(
    appDispatcher: Dispatcher = applicationDispatcher(),
) : Dispatcher by appDispatcher
