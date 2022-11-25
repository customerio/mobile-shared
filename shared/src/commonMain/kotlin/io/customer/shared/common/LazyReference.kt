package io.customer.shared.common

/**
 * Wrapper around Kotlin object to initialize the instance later when accessing the object.
 *
 * The class currently does not support atomic operations and may initialize the object multiple
 * times if accessed from different threads simultaneously.
 *
 * @param T generic type of object class.
 */
// TODO: Support atomic operations
internal class LazyReference<T : Any>(defaultValue: T? = null) {
    // The referenced value, can be null
    var value: T? = defaultValue
        private set

    /**
     * Returns current instance if initialized, else assigns it with new value using initializer
     * and then returns it.
     */
    fun initializeAndGet(initializer: () -> T): T {
        return value ?: initializer.invoke().also { value = it }
    }

    /**
     * Clears currently set instance
     */
    fun clearInstance() {
        value = null
    }
}
