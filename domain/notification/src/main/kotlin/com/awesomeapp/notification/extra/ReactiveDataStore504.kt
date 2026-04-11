package com.awesomeapp.notification.extra

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReactiveDataStore504<T : Any>(
    private val primaryKeyExtractor: (T) -> String
) {
    data class Query<T>(
        val filter: (T) -> Boolean = { true },
        val comparator: Comparator<T>? = null,
        val limit: Int = Int.MAX_VALUE,
        val offset: Int = 0
    )

    data class StoreStats(
        val totalItems: Int,
        val indexCount: Int,
        val queryCount: Long,
        val insertCount: Long,
        val updateCount: Long,
        val deleteCount: Long
    )

    sealed class ChangeEvent<T> {
        data class Inserted<T>(val item: T) : ChangeEvent<T>()
        data class Updated<T>(val old: T, val new: T) : ChangeEvent<T>()
        data class Deleted<T>(val item: T) : ChangeEvent<T>()
        class BulkChange<T>(val inserts: Int, val updates: Int, val deletes: Int) : ChangeEvent<T>()
    }

    private val store = mutableMapOf<String, T>()
    private val indices = mutableMapOf<String, MutableMap<Any, MutableSet<String>>>()
    private val mutex = Mutex()
    private val _changes = MutableSharedFlow<ChangeEvent<T>>(extraBufferCapacity = 64)
    private val _data = MutableStateFlow<List<T>>(emptyList())

    private var queryCount = 0L
    private var insertCount = 0L
    private var updateCount = 0L
    private var deleteCount = 0L

    val changes: SharedFlow<ChangeEvent<T>> = _changes.asSharedFlow()
    val data: StateFlow<List<T>> = _data.asStateFlow()

    fun <V : Any> addIndex(name: String, extractor: (T) -> V) {
        indices[name] = mutableMapOf()
    }

    suspend fun insert(item: T): Boolean = mutex.withLock {
        val key = primaryKeyExtractor(item)
        if (store.containsKey(key)) return false
        store[key] = item
        insertCount++
        updateIndices(key, null, item)
        emitSnapshot()
        _changes.emit(ChangeEvent.Inserted(item))
        true
    }

    suspend fun update(item: T): Boolean = mutex.withLock {
        val key = primaryKeyExtractor(item)
        val old = store[key] ?: return false
        store[key] = item
        updateCount++
        updateIndices(key, old, item)
        emitSnapshot()
        _changes.emit(ChangeEvent.Updated(old, item))
        true
    }

    suspend fun upsert(item: T): Boolean = mutex.withLock {
        val key = primaryKeyExtractor(item)
        val old = store[key]
        store[key] = item
        if (old == null) insertCount++ else updateCount++
        updateIndices(key, old, item)
        emitSnapshot()
        if (old == null) _changes.emit(ChangeEvent.Inserted(item))
        else _changes.emit(ChangeEvent.Updated(old, item))
        true
    }

    suspend fun delete(key: String): T? = mutex.withLock {
        val item = store.remove(key) ?: return null
        deleteCount++
        removeFromIndices(key)
        emitSnapshot()
        _changes.emit(ChangeEvent.Deleted(item))
        item
    }

    suspend fun bulkInsert(items: List<T>): Int = mutex.withLock {
        var count = 0
        for (item in items) {
            val key = primaryKeyExtractor(item)
            if (!store.containsKey(key)) {
                store[key] = item
                insertCount++
                updateIndices(key, null, item)
                count++
            }
        }
        emitSnapshot()
        _changes.emit(ChangeEvent.BulkChange(count, 0, 0))
        count
    }

    fun get(key: String): T? = store[key]

    fun query(query: Query<T>): List<T> {
        queryCount++
        var result = store.values.filter(query.filter)
        query.comparator?.let { result = result.sortedWith(it) }
        return result.drop(query.offset).take(query.limit)
    }

    fun observe(query: Query<T>): Flow<List<T>> = _data.map { items ->
        queryCount++
        var result = items.filter(query.filter)
        query.comparator?.let { result = result.sortedWith(it) }
        result.drop(query.offset).take(query.limit)
    }.distinctUntilChanged()

    fun count(predicate: (T) -> Boolean = { true }): Int = store.values.count(predicate)

    fun getStats(): StoreStats = StoreStats(
        totalItems = store.size, indexCount = indices.size,
        queryCount = queryCount, insertCount = insertCount,
        updateCount = updateCount, deleteCount = deleteCount
    )

    private fun emitSnapshot() { _data.value = store.values.toList() }

    private fun updateIndices(key: String, old: T?, new: T) {
        if (old != null) removeFromIndices(key)
    }

    private fun removeFromIndices(key: String) {
        indices.values.forEach { index ->
            index.values.forEach { it.remove(key) }
        }
    }

    fun getAll(): List<T> = store.values.toList()
    fun keys(): Set<String> = store.keys.toSet()
    fun isEmpty(): Boolean = store.isEmpty()

    suspend fun clear() = mutex.withLock {
        val size = store.size
        store.clear()
        indices.values.forEach { it.clear() }
        deleteCount += size
        emitSnapshot()
        _changes.emit(ChangeEvent.BulkChange(0, 0, size))
    }
}
