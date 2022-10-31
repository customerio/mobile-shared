package com.customerio.shared.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class IOSDispatcher : Dispatcher {
    override fun dispatcher(): CoroutineDispatcher = Dispatchers.Default
}

actual fun applicationDispatcher(): Dispatcher = IOSDispatcher()
