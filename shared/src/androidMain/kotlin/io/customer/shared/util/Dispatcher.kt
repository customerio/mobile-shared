package io.customer.shared.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidDispatcher : Dispatcher {
    override fun background(): CoroutineDispatcher = Dispatchers.IO
    override fun main(): CoroutineDispatcher = Dispatchers.Main
}

internal actual fun applicationDispatcher(): Dispatcher = AndroidDispatcher()
