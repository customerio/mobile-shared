package com.customerio.shared.util

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class AndroidDispatcher : Dispatcher {
    override fun dispatcher(): CoroutineContext = Dispatchers.IO
}

actual fun applicationDispatcher(): Dispatcher = AndroidDispatcher()
