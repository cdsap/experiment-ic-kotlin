package com.awesomeapp.search.extra

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TreeMap

class TimeSeriesStore417<V : Any>(
    private val maxSize: Int = 100_000,
    private val retentionMs: Long = 86_400_000
) {
    data class DataPoint<V>(val timestamp: Long, val value: V, val tags: Map<String, String> = emptyMap())

    data class TimeRange(val startMs: Long, val endMs: Long) {
        val durationMs: Long get() = endMs - startMs
        operator fun contains(timestamp: Long): Boolean = timestamp in startMs..endMs
    }

    data class Aggregation(
        val count: Int, val sum: Double, val min: Double, val max: Double,
        val avg: Double, val p50: Double, val p90: Double, val p99: Double,
        val stdDev: Double, val timeRange: TimeRange
    )

    data class Bucket<V>(val timeRange: TimeRange, val points: List<DataPoint<V>>, val aggregation: Aggregation?)

    data class StoreStats(
        val totalPoints: Int, val oldestTimestamp: Long?, val newestTimestamp: Long?,
        val tagsCount: Int, val memoryEstimateBytes: Long
    )

    private val data = TreeMap<Long, MutableList<DataPoint<V>>>()
    private val tagIndex = mutableMapOf<String, MutableMap<String, MutableSet<Long>>>()
    private val mutex = Mutex()
    private val _stats = MutableStateFlow(StoreStats(0, null, null, 0, 0))
    val stats: StateFlow<StoreStats> = _stats.asStateFlow()

    suspend fun add(point: DataPoint<V>) = mutex.withLock {
        data.getOrPut(point.timestamp) { mutableListOf() }.add(point)
        point.tags.forEach { (key, value) ->
            tagIndex.getOrPut(key) { mutableMapOf() }.getOrPut(value) { mutableSetOf() }.add(point.timestamp)
        }
        enforceRetention()
        enforceMaxSize()
        updateStats()
    }

    suspend fun addAll(points: List<DataPoint<V>>) = mutex.withLock {
        points.forEach { point ->
            data.getOrPut(point.timestamp) { mutableListOf() }.add(point)
            point.tags.forEach { (key, value) ->
                tagIndex.getOrPut(key) { mutableMapOf() }.getOrPut(value) { mutableSetOf() }.add(point.timestamp)
            }
        }
        enforceRetention()
        enforceMaxSize()
        updateStats()
    }

    fun query(range: TimeRange, tagFilters: Map<String, String> = emptyMap()): List<DataPoint<V>> {
        val points = data.subMap(range.startMs, true, range.endMs, true).values.flatten()
        return if (tagFilters.isEmpty()) points
        else points.filter { point -> tagFilters.all { (k, v) -> point.tags[k] == v } }
    }

    fun aggregate(range: TimeRange, valueExtractor: (V) -> Double, tagFilters: Map<String, String> = emptyMap()): Aggregation? {
        val points = query(range, tagFilters)
        if (points.isEmpty()) return null
        val values = points.map { valueExtractor(it.value) }.sorted()
        val sum = values.sum()
        val avg = sum / values.size
        val variance = values.map { (it - avg) * (it - avg) }.sum() / values.size

        return Aggregation(
            count = values.size, sum = sum,
            min = values.first(), max = values.last(), avg = avg,
            p50 = percentile(values, 50.0), p90 = percentile(values, 90.0),
            p99 = percentile(values, 99.0),
            stdDev = kotlin.math.sqrt(variance),
            timeRange = range
        )
    }

    fun bucketize(range: TimeRange, bucketSizeMs: Long, valueExtractor: (V) -> Double): List<Bucket<V>> {
        val buckets = mutableListOf<Bucket<V>>()
        var start = range.startMs
        while (start < range.endMs) {
            val end = (start + bucketSizeMs).coerceAtMost(range.endMs)
            val bucketRange = TimeRange(start, end)
            val points = query(bucketRange)
            val agg = if (points.isNotEmpty()) aggregate(bucketRange, valueExtractor) else null
            buckets.add(Bucket(bucketRange, points, agg))
            start = end
        }
        return buckets
    }

    fun latest(n: Int = 1): List<DataPoint<V>> = data.descendingMap().values.flatten().take(n)
    fun oldest(n: Int = 1): List<DataPoint<V>> = data.values.flatten().take(n)
    fun count(): Int = data.values.sumOf { it.size }
    fun countInRange(range: TimeRange): Int = query(range).size

    fun getTagValues(tagKey: String): Set<String> = tagIndex[tagKey]?.keys ?: emptySet()
    fun getTags(): Set<String> = tagIndex.keys.toSet()

    private fun percentile(sortedValues: List<Double>, percentile: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val index = (percentile / 100.0 * (sortedValues.size - 1)).toInt()
        return sortedValues[index.coerceIn(0, sortedValues.size - 1)]
    }

    private fun enforceRetention() {
        val cutoff = System.currentTimeMillis() - retentionMs
        val keysToRemove = data.headMap(cutoff, false).keys.toList()
        keysToRemove.forEach { data.remove(it) }
    }

    private fun enforceMaxSize() {
        while (count() > maxSize && data.isNotEmpty()) {
            val oldestKey = data.firstKey()
            data[oldestKey]?.removeLastOrNull()
            if (data[oldestKey]?.isEmpty() == true) data.remove(oldestKey)
        }
    }

    private fun updateStats() {
        val totalPoints = count()
        _stats.value = StoreStats(
            totalPoints = totalPoints,
            oldestTimestamp = data.firstEntry()?.key,
            newestTimestamp = data.lastEntry()?.key,
            tagsCount = tagIndex.size,
            memoryEstimateBytes = (totalPoints * 64).toLong()
        )
    }

    suspend fun clear() = mutex.withLock {
        data.clear(); tagIndex.clear(); updateStats()
    }
}
