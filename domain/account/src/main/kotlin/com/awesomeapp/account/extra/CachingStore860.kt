package com.awesomeapp.account.extra

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class CachingStore860<K : Any, V : Any>(
    private val maxSize: Int,
    private val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU
) {
    enum class EvictionPolicy { LRU, LFU, FIFO, RANDOM }

    data class CacheEntry<V>(
        val value: V,
        val createdAt: Long = System.nanoTime(),
        var lastAccessedAt: Long = System.nanoTime(),
        val accessCount: AtomicLong = AtomicLong(0)
    ) {
        fun touch(): V {
            lastAccessedAt = System.nanoTime()
            accessCount.incrementAndGet()
            return value
        }
    }

    data class CacheStats(
        val hits: Long = 0,
        val misses: Long = 0,
        val evictions: Long = 0,
        val puts: Long = 0
    ) {
        val hitRate: Double get() = if (hits + misses == 0L) 0.0 else hits.toDouble() / (hits + misses)
        val totalRequests: Long get() = hits + misses
        override fun toString(): String = "CacheStats(hits=$hits, misses=$misses, evictions=$evictions, hitRate=${"%.2f".format(hitRate)})"
    }

    private val store = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()
    private val _stats = MutableStateFlow(CacheStats())
    private var loader: (suspend (K) -> V?)? = null

    val stats: Flow<CacheStats> = _stats

    fun withLoader(loader: suspend (K) -> V?): CachingStore860<K, V> {
        this.loader = loader
        return this
    }

    suspend fun get(key: K): V? {
        val entry = store[key]
        if (entry != null) {
            _stats.value = _stats.value.copy(hits = _stats.value.hits + 1)
            return entry.touch()
        }
        _stats.value = _stats.value.copy(misses = _stats.value.misses + 1)
        val loaded = loader?.invoke(key)
        if (loaded != null) {
            put(key, loaded)
        }
        return loaded
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        if (store.size >= maxSize && !store.containsKey(key)) {
            evict()
        }
        store[key] = CacheEntry(value)
        _stats.value = _stats.value.copy(puts = _stats.value.puts + 1)
    }

    private fun evict() {
        if (store.isEmpty()) return
        val keyToEvict = when (evictionPolicy) {
            EvictionPolicy.LRU -> store.entries.minByOrNull { it.value.lastAccessedAt }?.key
            EvictionPolicy.LFU -> store.entries.minByOrNull { it.value.accessCount.get() }?.key
            EvictionPolicy.FIFO -> store.entries.minByOrNull { it.value.createdAt }?.key
            EvictionPolicy.RANDOM -> store.keys.randomOrNull()
        }
        keyToEvict?.let {
            store.remove(it)
            _stats.value = _stats.value.copy(evictions = _stats.value.evictions + 1)
        }
    }

    fun findAll(predicate: (V) -> Boolean): List<V> =
        store.values.map { it.value }.filter(predicate)

    fun invalidate(key: K) { store.remove(key) }
    fun invalidateAll() { store.clear() }
    fun size(): Int = store.size
    fun containsKey(key: K): Boolean = store.containsKey(key)
    fun keys(): Set<K> = store.keys.toSet()
    fun values(): List<V> = store.values.map { it.value }

    fun snapshot(): Map<K, V> = store.mapValues { it.value.value }

    fun <R> fold(initial: R, operation: (R, V) -> R): R =
        store.values.fold(initial) { acc, entry -> operation(acc, entry.value) }
}
