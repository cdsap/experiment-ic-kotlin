package com.awesomeapp.metric.extra

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class ServiceContainer1478 {

    enum class Lifecycle { SINGLETON, TRANSIENT, SCOPED }

    data class ServiceDescriptor(
        val type: Class<*>,
        val lifecycle: Lifecycle,
        val factory: suspend (ServiceContainer1478) -> Any,
        val name: String? = null,
        val tags: Set<String> = emptySet(),
        val priority: Int = 0
    )

    data class ContainerStats(
        val registeredServices: Int,
        val singletonInstances: Int,
        val scopedInstances: Int,
        val resolutionCount: Long,
        val resolutionErrors: Long
    )

    sealed class ResolutionResult<out T> {
        data class Success<T>(val instance: T) : ResolutionResult<T>()
        data class Error(val message: String, val cause: Throwable? = null) : ResolutionResult<Nothing>()
    }

    private val descriptors = ConcurrentHashMap<String, ServiceDescriptor>()
    private val singletons = ConcurrentHashMap<String, Any>()
    private val scopedInstances = ConcurrentHashMap<String, MutableMap<String, Any>>()
    private val mutex = Mutex()
    private var resolutionCount = 0L
    private var resolutionErrors = 0L
    private val resolutionStack = ThreadLocal.withInitial { mutableSetOf<String>() }
    private val interceptors = mutableListOf<suspend (String, Any) -> Any>()

    fun <T : Any> register(
        type: Class<T>, lifecycle: Lifecycle = Lifecycle.SINGLETON,
        name: String? = null, tags: Set<String> = emptySet(),
        priority: Int = 0, factory: suspend (ServiceContainer1478) -> T
    ): ServiceContainer1478 {
        val key = resolveKey(type, name)
        descriptors[key] = ServiceDescriptor(type, lifecycle, factory, name, tags, priority)
        return this
    }

    inline fun <reified T : Any> register(
        lifecycle: Lifecycle = Lifecycle.SINGLETON,
        name: String? = null, tags: Set<String> = emptySet(),
        noinline factory: suspend (ServiceContainer1478) -> T
    ) = register(T::class.java, lifecycle, name, tags, factory = factory)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> resolve(type: Class<T>, name: String? = null): T {
        val key = resolveKey(type, name)
        resolutionCount++

        val stack = resolutionStack.get()
        if (key in stack) throw IllegalStateException("Circular dependency detected: $key -> ${stack.joinToString(" -> ")}")
        stack.add(key)

        try {
            val descriptor = descriptors[key] ?: throw IllegalStateException("Service not registered: $key")
            val instance = when (descriptor.lifecycle) {
                Lifecycle.SINGLETON -> singletons.getOrPut(key) { descriptor.factory(this) }
                Lifecycle.TRANSIENT -> descriptor.factory(this)
                Lifecycle.SCOPED -> throw IllegalStateException("Scoped resolution requires a scope")
            }
            var result = instance as T
            for (interceptor in interceptors) {
                result = interceptor(key, result) as T
            }
            return result
        } catch (e: Throwable) {
            resolutionErrors++
            throw e
        } finally {
            stack.remove(key)
        }
    }

    suspend inline fun <reified T : Any> resolve(name: String? = null): T = resolve(T::class.java, name)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> resolveScoped(type: Class<T>, scopeId: String, name: String? = null): T {
        val key = resolveKey(type, name)
        resolutionCount++
        val descriptor = descriptors[key] ?: throw IllegalStateException("Service not registered: $key")
        val scopeMap = scopedInstances.getOrPut(scopeId) { mutableMapOf() }

        return mutex.withLock {
            scopeMap.getOrPut(key) { descriptor.factory(this) } as T
        }
    }

    suspend fun destroyScope(scopeId: String) = mutex.withLock {
        scopedInstances.remove(scopeId)
    }

    fun <T : Any> resolveAll(type: Class<T>): List<ServiceDescriptor> =
        descriptors.values.filter { type.isAssignableFrom(it.type) }.sortedByDescending { it.priority }

    fun resolveByTag(tag: String): List<ServiceDescriptor> =
        descriptors.values.filter { tag in it.tags }.sortedByDescending { it.priority }

    fun addInterceptor(interceptor: suspend (String, Any) -> Any) { interceptors.add(interceptor) }

    fun isRegistered(type: Class<*>, name: String? = null): Boolean = descriptors.containsKey(resolveKey(type, name))

    fun getStats(): ContainerStats = ContainerStats(
        registeredServices = descriptors.size,
        singletonInstances = singletons.size,
        scopedInstances = scopedInstances.values.sumOf { it.size },
        resolutionCount = resolutionCount,
        resolutionErrors = resolutionErrors
    )

    private fun resolveKey(type: Class<*>, name: String?): String = name?.let { "${type.name}:$it" } ?: type.name

    fun getRegisteredTypes(): Set<String> = descriptors.keys.toSet()
    fun getScopeIds(): Set<String> = scopedInstances.keys.toSet()

    suspend fun clearSingletons() = mutex.withLock { singletons.clear() }
    suspend fun clearAll() = mutex.withLock { singletons.clear(); scopedInstances.clear() }
}
