package com.awesomeapp.calendar.extra

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

class ObjectPool1655<T : Any>(
    private val config: PoolConfig,
    private val factory: ObjectFactory<T>
) {
    data class PoolConfig(
        val minSize: Int = 0,
        val maxSize: Int = 10,
        val maxIdleTimeMs: Long = 300_000,
        val acquireTimeoutMs: Long = 30_000,
        val validationIntervalMs: Long = 60_000,
        val maxLifetimeMs: Long = 1_800_000
    )

    interface ObjectFactory<T> {
        suspend fun create(): T
        suspend fun destroy(obj: T)
        suspend fun validate(obj: T): Boolean
        suspend fun reset(obj: T): T
    }

    data class PooledObject<T>(
        val obj: T,
        val createdAt: Long = System.currentTimeMillis(),
        var lastUsedAt: Long = System.currentTimeMillis(),
        var lastValidatedAt: Long = System.currentTimeMillis(),
        val usageCount: AtomicLong = AtomicLong(0)
    ) {
        val age: Long get() = System.currentTimeMillis() - createdAt
        val idleTime: Long get() = System.currentTimeMillis() - lastUsedAt
    }

    data class PoolStats(
        val totalSize: Int,
        val idleSize: Int,
        val activeSize: Int,
        val totalCreated: Long,
        val totalDestroyed: Long,
        val totalAcquired: Long,
        val totalReleased: Long,
        val totalValidationFailures: Long,
        val waitingCount: Int
    )

    private val idleObjects = Channel<PooledObject<T>>(Channel.UNLIMITED)
    private val activeObjects = mutableSetOf<PooledObject<T>>()
    private val allObjects = mutableListOf<PooledObject<T>>()
    private val mutex = Mutex()

    private val totalCreated = AtomicLong(0)
    private val totalDestroyed = AtomicLong(0)
    private val totalAcquired = AtomicLong(0)
    private val totalReleased = AtomicLong(0)
    private val totalValidationFailures = AtomicLong(0)
    private var waitingCount = 0

    private val _stats = MutableStateFlow(PoolStats(0, 0, 0, 0, 0, 0, 0, 0, 0))
    val stats: StateFlow<PoolStats> = _stats.asStateFlow()

    suspend fun initialize() = mutex.withLock {
        repeat(config.minSize) {
            val pooled = createPooledObject()
            idleObjects.send(pooled)
        }
        updateStats()
    }

    suspend fun acquire(): T? = mutex.withLock {
        waitingCount++
        updateStats()
    }.let {
        try {
            val pooled = tryAcquireIdle() ?: tryCreateNew()
            if (pooled != null) {
                totalAcquired.incrementAndGet()
                pooled.lastUsedAt = System.currentTimeMillis()
                pooled.usageCount.incrementAndGet()
                mutex.withLock {
                    activeObjects.add(pooled)
                    waitingCount--
                    updateStats()
                }
                pooled.obj
            } else {
                mutex.withLock { waitingCount--; updateStats() }
                null
            }
        } catch (e: Throwable) {
            mutex.withLock { waitingCount--; updateStats() }
            throw e
        }
    }

    private suspend fun tryAcquireIdle(): PooledObject<T>? {
        val pooled = withTimeoutOrNull(100) { idleObjects.receiveCatching().getOrNull() } ?: return null

        if (pooled.age > config.maxLifetimeMs || pooled.idleTime > config.maxIdleTimeMs) {
            destroyPooledObject(pooled)
            return tryAcquireIdle()
        }

        if (!factory.validate(pooled.obj)) {
            totalValidationFailures.incrementAndGet()
            destroyPooledObject(pooled)
            return tryAcquireIdle()
        }

        return pooled
    }

    private suspend fun tryCreateNew(): PooledObject<T>? = mutex.withLock {
        if (allObjects.size < config.maxSize) {
            createPooledObject()
        } else null
    }

    suspend fun release(obj: T) = mutex.withLock {
        val pooled = activeObjects.find { it.obj === obj } ?: return@withLock
        activeObjects.remove(pooled)
        totalReleased.incrementAndGet()

        if (pooled.age > config.maxLifetimeMs) {
            destroyPooledObject(pooled)
        } else {
            try {
                val reset = pooled.copy(obj = factory.reset(pooled.obj), lastUsedAt = System.currentTimeMillis())
                idleObjects.send(reset)
            } catch (e: Throwable) {
                destroyPooledObject(pooled)
            }
        }
        updateStats()
    }

    suspend fun evictIdle() = mutex.withLock {
        val toEvict = mutableListOf<PooledObject<T>>()
        val remaining = mutableListOf<PooledObject<T>>()

        while (true) {
            val pooled = withTimeoutOrNull(10) { idleObjects.receiveCatching().getOrNull() } ?: break
            if (pooled.idleTime > config.maxIdleTimeMs || pooled.age > config.maxLifetimeMs) {
                toEvict.add(pooled)
            } else {
                remaining.add(pooled)
            }
        }
        remaining.forEach { idleObjects.send(it) }
        toEvict.forEach { destroyPooledObject(it) }
        updateStats()
    }

    private suspend fun createPooledObject(): PooledObject<T> {
        val obj = factory.create()
        val pooled = PooledObject(obj)
        allObjects.add(pooled)
        totalCreated.incrementAndGet()
        return pooled
    }

    private suspend fun destroyPooledObject(pooled: PooledObject<T>) {
        allObjects.remove(pooled)
        totalDestroyed.incrementAndGet()
        try { factory.destroy(pooled.obj) } catch (_: Throwable) {}
    }

    private fun updateStats() {
        _stats.value = PoolStats(
            totalSize = allObjects.size,
            idleSize = allObjects.size - activeObjects.size,
            activeSize = activeObjects.size,
            totalCreated = totalCreated.get(),
            totalDestroyed = totalDestroyed.get(),
            totalAcquired = totalAcquired.get(),
            totalReleased = totalReleased.get(),
            totalValidationFailures = totalValidationFailures.get(),
            waitingCount = waitingCount
        )
    }

    suspend fun shutdown() = mutex.withLock {
        while (true) {
            val pooled = withTimeoutOrNull(10) { idleObjects.receiveCatching().getOrNull() } ?: break
            destroyPooledObject(pooled)
        }
        activeObjects.toList().forEach { destroyPooledObject(it) }
        activeObjects.clear()
        updateStats()
    }

    fun getStats(): PoolStats = _stats.value
}
