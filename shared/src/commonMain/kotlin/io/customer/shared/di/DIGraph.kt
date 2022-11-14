package io.customer.shared.di

import kotlin.reflect.KClass

/**
 * Base dependency injection class to satisfy dependencies without adding any external dependency.
 * Any DI component created within the SDK should inherit this class.
 *
 * @see [getNewInstance] and [getSingletonInstance] to get more details on how to use this class.
 */
abstract class DIGraph {
    /**
     * Map of overridden dependencies, helpful in mocking for test functions.
     */
    val overrides: MutableMap<String, Any> = mutableMapOf()

    /**
     * Call when you want to override a dependency with a mock in test functions.
     * `di.overrideDependency(CustomerIOInstance::class.java, customerIOMock)`
     *
     * See `CustomerIOTest` (or any other class that is top-level of the module) for an example on use.
     */
    fun <DEP : Any> overrideDependency(dependency: KClass<DEP>, value: DEP) {
        overrides[dependency.getClassNameOrThrow(value = value)] = value as Any
    }

    /**
     * Call in Digraph property getters to get allow mock overriding before trying to get actual instance.
     * We strongly recommend to get instances using methods exposed by Digraph for the purpose, so
     * it can take responsibility of providing overridden instances where needed.
     * However, if needed, actual instance should be accessed using the following:
     * ```
     * val fileStorage: FileStorage
     *   get() = override() ?: FileStorage(siteId, context)
     * ```
     */
    inline fun <reified DEP> override(): DEP? = overrides[DEP::class.simpleName] as? DEP

    /**
     * We prefer to have all of the SDK's singleton instances held in the dependency injection graph.
     * This makes it easier for automated tests to be able to delete all singletons between each
     * test function and prevent test flakiness.
     */
    val singletons: MutableMap<String, Any> = mutableMapOf()

    /**
     * In the graph, if you have any dependency that should be a singleton:
     * ```
     * val queue: Queue
     *   get() = getSingletonInstance { QueueImpl(...) }
     * ```
     * Note: Don't forget to include the interface in the property declaration:
     * ```
     * val foo: InterfaceOfFoo
     *          ^^^^^^^^^^^^^^
     * ```
     * Or you may not be able to successfully mock in test functions.
     */
    inline fun <reified INST : Any> getSingletonInstance(crossinline newInstanceCreator: () -> INST): INST {
        val singletonKey = INST::class.getClassNameOrThrow()
        val newInstance = { override() ?: newInstanceCreator() }
        return singletons[singletonKey] as? INST ?: newInstance().also { instance ->
            singletons[singletonKey] = instance
        }
    }

    /**
     * In the graph, if you have any dependency that should create new instance every time:
     * ```
     * val queue: Queue
     *   get() = getNewInstance { QueueImpl(...) }
     * ```
     * Note: Don't forget to include the interface in the property declaration:
     * ```
     * val foo: InterfaceOfFoo
     *          ^^^^^^^^^^^^^^
     * ```
     * Or you may not be able to successfully mock in test functions.
     */
    inline fun <reified INST : Any> getNewInstance(newInstanceCreator: () -> INST): INST {
        return override() ?: newInstanceCreator()
    }

    /**
     * Call to delete instances held by the graph. This is meant to be called in between automated
     * tests but can also be called to reset that state of the SDK at runtime.
     */
    fun reset() {
        overrides.clear()
        singletons.clear()
    }

    /**
     * Obtains key using name for the given class.
     */
    fun <DEP : Any> KClass<DEP>.getClassNameOrThrow(value: DEP? = null): String {
        val valueClass = { if (value == null) null else value::class }
        return simpleName ?: valueClass()?.simpleName
        ?: throw RuntimeException("Cannot obtain class name for $valueClass")
    }
}
