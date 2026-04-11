package com.awesomeapp.post.extra

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MessageBroker174(private val scope: CoroutineScope) {

    data class Message<T>(
        val id: Long,
        val topic: String,
        val payload: T,
        val headers: Map<String, String> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis(),
        val replyTo: String? = null
    )

    data class Subscriber<T>(
        val id: Long,
        val topic: String,
        val group: String? = null,
        val filter: (Message<T>) -> Boolean = { true },
        val handler: suspend (Message<T>) -> Unit
    )

    data class TopicStats(
        val topic: String,
        val subscriberCount: Int,
        val publishedCount: Long,
        val deliveredCount: Long,
        val errorCount: Long
    )

    data class BrokerStats(
        val totalTopics: Int,
        val totalSubscribers: Int,
        val totalPublished: Long,
        val totalDelivered: Long,
        val totalErrors: Long,
        val topicStats: List<TopicStats>
    )

    private val topicFlows = ConcurrentHashMap<String, MutableSharedFlow<Message<Any>>>()
    private val subscribers = ConcurrentHashMap<Long, Subscriber<Any>>()
    private val topicSubscribers = ConcurrentHashMap<String, MutableSet<Long>>()
    private val mutex = Mutex()
    private val nextId = AtomicLong(0)
    private val nextMessageId = AtomicLong(0)
    private val publishCounts = ConcurrentHashMap<String, AtomicLong>()
    private val deliverCounts = ConcurrentHashMap<String, AtomicLong>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val deadLetterQueue = mutableListOf<Pair<Message<Any>, Throwable>>()
    private var deadLetterHandler: (suspend (Message<Any>, Throwable) -> Unit)? = null

    fun onDeadLetter(handler: suspend (Message<Any>, Throwable) -> Unit) { deadLetterHandler = handler }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> subscribe(
        topic: String,
        group: String? = null,
        filter: (Message<T>) -> Boolean = { true },
        handler: suspend (Message<T>) -> Unit
    ): Long = mutex.withLock {
        val id = nextId.incrementAndGet()
        val sub = Subscriber(id, topic, group, filter as (Message<Any>) -> Boolean, handler as suspend (Message<Any>) -> Unit)
        subscribers[id] = sub
        topicSubscribers.getOrPut(topic) { mutableSetOf() }.add(id)

        val flow = getOrCreateFlow(topic)
        scope.launch {
            flow.collect { message ->
                if (sub.filter(message)) {
                    try {
                        sub.handler(message)
                        deliverCounts.getOrPut(topic) { AtomicLong(0) }.incrementAndGet()
                    } catch (e: Throwable) {
                        errorCounts.getOrPut(topic) { AtomicLong(0) }.incrementAndGet()
                        deadLetterQueue.add(message to e)
                        deadLetterHandler?.invoke(message, e)
                    }
                }
            }
        }
        id
    }

    suspend fun unsubscribe(id: Long) = mutex.withLock {
        val sub = subscribers.remove(id) ?: return@withLock
        topicSubscribers[sub.topic]?.remove(id)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> publish(topic: String, payload: T, headers: Map<String, String> = emptyMap(), replyTo: String? = null) {
        val message = Message(nextMessageId.incrementAndGet(), topic, payload as Any, headers, replyTo = replyTo)
        publishCounts.getOrPut(topic) { AtomicLong(0) }.incrementAndGet()
        getOrCreateFlow(topic).emit(message)
    }

    fun <T : Any> observe(topic: String): Flow<Message<T>> {
        @Suppress("UNCHECKED_CAST")
        return getOrCreateFlow(topic).asSharedFlow() as Flow<Message<T>>
    }

    private fun getOrCreateFlow(topic: String): MutableSharedFlow<Message<Any>> =
        topicFlows.getOrPut(topic) { MutableSharedFlow(extraBufferCapacity = 64) }

    fun getTopicStats(topic: String): TopicStats = TopicStats(
        topic = topic,
        subscriberCount = topicSubscribers[topic]?.size ?: 0,
        publishedCount = publishCounts[topic]?.get() ?: 0,
        deliveredCount = deliverCounts[topic]?.get() ?: 0,
        errorCount = errorCounts[topic]?.get() ?: 0
    )

    fun getBrokerStats(): BrokerStats {
        val topics = (topicFlows.keys + topicSubscribers.keys).toSet()
        val topicStats = topics.map { getTopicStats(it) }
        return BrokerStats(
            totalTopics = topics.size,
            totalSubscribers = subscribers.size,
            totalPublished = publishCounts.values.sumOf { it.get() },
            totalDelivered = deliverCounts.values.sumOf { it.get() },
            totalErrors = errorCounts.values.sumOf { it.get() },
            topicStats = topicStats
        )
    }

    fun getTopics(): Set<String> = topicFlows.keys.toSet()
    fun getSubscriberCount(topic: String): Int = topicSubscribers[topic]?.size ?: 0
    fun getDeadLetterQueue(): List<Pair<Message<Any>, Throwable>> = deadLetterQueue.toList()
}
