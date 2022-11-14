package io.customer.shared.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class IOSDispatcher : Dispatcher {
    override fun background(): CoroutineDispatcher = Dispatchers.Default
    override fun main(): CoroutineDispatcher = Dispatchers.Main
}

internal actual fun applicationDispatcher(): Dispatcher = IOSDispatcher()
