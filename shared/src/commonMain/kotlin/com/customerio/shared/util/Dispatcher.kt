package com.customerio.shared.util

import kotlinx.coroutines.CoroutineDispatcher

interface Dispatcher {
    fun dispatcher(): CoroutineDispatcher
}

internal expect fun applicationDispatcher(): Dispatcher
