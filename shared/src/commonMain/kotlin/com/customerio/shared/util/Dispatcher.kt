package com.customerio.shared.util

import kotlin.coroutines.CoroutineContext

interface Dispatcher {
    fun dispatcher(): CoroutineContext
}

internal expect fun applicationDispatcher(): Dispatcher
