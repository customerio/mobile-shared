package com.customerio.shared.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidDispatcher : Dispatcher {
    override fun dispatcher(): CoroutineDispatcher = Dispatchers.IO
}

actual fun applicationDispatcher(): Dispatcher = AndroidDispatcher()
