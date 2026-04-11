package com.awesomeapp.comment.extra

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class EventDispatcher261(private val scope: CoroutineScope) {

    sealed interface AppEvent {
        val timestamp: Long
        val source: String

        data class DataChanged(
            override val timestamp: Long = System.currentTimeMillis(),
            override val source: String,
            val entityType: String,
            val entityId: String,
            val changeType: ChangeType
        ) : AppEvent

        data class UserAction(
            override val timestamp: Long = System.currentTimeMillis(),
            override val source: String,
            val action: String,
            val metadata: Map<String, Any> = emptyMap()
        ) : AppEvent

        data class ErrorOccurred(
            override val timestamp: Long = System.currentTimeMillis(),
            override val source: String,
            val error: Throwable,
            val severity: Severity
        ) : AppEvent

        data class StateTransition(
            override val timestamp: Long = System.currentTimeMillis(),
            override val source: String,
            val fromState: String,
            val toState: String,
            val trigger: String
        ) : AppEvent
    }

    enum class ChangeType { CREATED, UPDATED, DELETED, BULK_UPDATE }
    enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }

    data class Subscription<T : AppEvent>(
        val id: Long,
        val eventType: Class<T>,
        val priority: Int,
        val filter: (T) -> Boolean,
        val handler: suspend (T) -> Unit
    )

    @PublishedApi internal val _events = MutableSharedFlow<AppEvent>(replay = 0, extraBufferCapacity = 64)
    @PublishedApi internal val subscriptions = ConcurrentHashMap<Long, Subscription<*>>()
    private val eventHistory = mutableListOf<AppEvent>()
    private val mutex = Mutex()
    @PublishedApi internal val nextId = AtomicLong(0)
    private val publishedCount = AtomicLong(0)
    private var maxHistorySize = 1000
    private var recordHistory = false

    fun withHistory(record: Boolean, maxSize: Int = 1000): EventDispatcher261 {
        this.recordHistory = record
        this.maxHistorySize = maxSize
        return this
    }

    init {
        scope.launch {
            _events.collect { event ->
                dispatchToSubscribers(event)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun dispatchToSubscribers(event: AppEvent) {
        val sortedSubs = subscriptions.values
            .filter { sub -> sub.eventType.isInstance(event) }
            .sortedByDescending { it.priority }

        for (sub in sortedSubs) {
            val typedSub = sub as Subscription<AppEvent>
            if (typedSub.filter(event)) {
                typedSub.handler(event)
            }
        }
    }

    inline fun <reified T : AppEvent> subscribe(
        priority: Int = 0,
        noinline filter: (T) -> Boolean = { true },
        noinline handler: suspend (T) -> Unit
    ): Long {
        val id = nextId.incrementAndGet()
        subscriptions[id] = Subscription(id, T::class.java, priority, filter, handler)
        return id
    }

    fun unsubscribe(id: Long) { subscriptions.remove(id) }

    suspend fun publish(event: AppEvent) {
        publishedCount.incrementAndGet()
        if (recordHistory) {
            mutex.withLock {
                eventHistory.add(event)
                while (eventHistory.size > maxHistorySize) {
                    eventHistory.removeFirst()
                }
            }
        }
        _events.emit(event)
    }

    fun observeEvents(): Flow<AppEvent> = _events.asSharedFlow()

    inline fun <reified T : AppEvent> observeEventsOfType(): Flow<T> =
        _events.asSharedFlow().filterIsInstance<T>()

    suspend fun getHistory(): List<AppEvent> = mutex.withLock { eventHistory.toList() }
    fun getPublishedCount(): Long = publishedCount.get()
    fun getSubscriptionCount(): Int = subscriptions.size
}
