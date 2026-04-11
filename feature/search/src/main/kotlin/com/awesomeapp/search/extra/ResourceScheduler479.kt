package com.awesomeapp.search.extra

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ResourceScheduler479<R : Any>(private val strategy: SchedulingStrategy = SchedulingStrategy.WEIGHTED_ROUND_ROBIN) {

    enum class SchedulingStrategy { ROUND_ROBIN, WEIGHTED_ROUND_ROBIN, LEAST_CONNECTIONS, RANDOM, PRIORITY_BASED }

    data class Resource<R>(
        val id: String,
        val value: R,
        val weight: Int = 1,
        val priority: Int = 0,
        val maxConcurrent: Int = Int.MAX_VALUE,
        var enabled: Boolean = true
    )

    data class ResourceStats(
        val resourceId: String,
        val totalAssigned: Long,
        val currentActive: Int,
        val totalCompleted: Long,
        val totalFailed: Long,
        val avgDurationMs: Double
    )

    data class SchedulerStats(
        val totalResources: Int,
        val enabledResources: Int,
        val totalAssigned: Long,
        val totalCompleted: Long,
        val totalFailed: Long,
        val resourceStats: List<ResourceStats>
    )

    sealed class AssignmentResult<R> {
        data class Assigned<R>(val resource: Resource<R>, val assignmentId: Long) : AssignmentResult<R>()
        data class NoResourceAvailable<R>(val reason: String) : AssignmentResult<R>()
    }

    private val resources = ConcurrentHashMap<String, Resource<R>>()
    private val activeAssignments = ConcurrentHashMap<Long, Pair<String, Long>>()
    private val resourceCounters = ConcurrentHashMap<String, AtomicLong>()
    private val activeCounters = ConcurrentHashMap<String, AtomicLong>()
    private val completedCounters = ConcurrentHashMap<String, AtomicLong>()
    private val failedCounters = ConcurrentHashMap<String, AtomicLong>()
    private val durationAccumulators = ConcurrentHashMap<String, AtomicLong>()
    private val mutex = Mutex()
    private val nextAssignmentId = AtomicLong(0)
    private var roundRobinIndex = 0
    private var weightedIndex = 0
    private var weightedCounter = 0

    private val _stats = MutableStateFlow(SchedulerStats(0, 0, 0, 0, 0, emptyList()))
    val stats: StateFlow<SchedulerStats> = _stats.asStateFlow()

    fun addResource(resource: Resource<R>): ResourceScheduler479<R> {
        resources[resource.id] = resource
        resourceCounters[resource.id] = AtomicLong(0)
        activeCounters[resource.id] = AtomicLong(0)
        completedCounters[resource.id] = AtomicLong(0)
        failedCounters[resource.id] = AtomicLong(0)
        durationAccumulators[resource.id] = AtomicLong(0)
        updateStats()
        return this
    }

    fun removeResource(id: String) {
        resources.remove(id)
        updateStats()
    }

    fun enableResource(id: String) { resources[id]?.enabled = true; updateStats() }
    fun disableResource(id: String) { resources[id]?.enabled = false; updateStats() }

    suspend fun acquire(): AssignmentResult<R> = mutex.withLock {
        val available = resources.values.filter { it.enabled && (activeCounters[it.id]?.get() ?: 0) < it.maxConcurrent }
        if (available.isEmpty()) return@withLock AssignmentResult.NoResourceAvailable("No resources available")

        val selected = when (strategy) {
            SchedulingStrategy.ROUND_ROBIN -> {
                roundRobinIndex = (roundRobinIndex + 1) % available.size
                available[roundRobinIndex]
            }
            SchedulingStrategy.WEIGHTED_ROUND_ROBIN -> selectWeighted(available)
            SchedulingStrategy.LEAST_CONNECTIONS -> available.minByOrNull { activeCounters[it.id]?.get() ?: 0 }!!
            SchedulingStrategy.RANDOM -> available.random()
            SchedulingStrategy.PRIORITY_BASED -> available.maxByOrNull { it.priority }!!
        }

        val assignmentId = nextAssignmentId.incrementAndGet()
        activeAssignments[assignmentId] = selected.id to System.currentTimeMillis()
        resourceCounters[selected.id]?.incrementAndGet()
        activeCounters[selected.id]?.incrementAndGet()
        updateStats()
        AssignmentResult.Assigned(selected, assignmentId)
    }

    suspend fun release(assignmentId: Long, success: Boolean = true) = mutex.withLock {
        val (resourceId, startTime) = activeAssignments.remove(assignmentId) ?: return@withLock
        activeCounters[resourceId]?.decrementAndGet()
        val duration = System.currentTimeMillis() - startTime
        durationAccumulators[resourceId]?.addAndGet(duration)
        if (success) completedCounters[resourceId]?.incrementAndGet()
        else failedCounters[resourceId]?.incrementAndGet()
        updateStats()
    }

    private fun selectWeighted(available: List<Resource<R>>): Resource<R> {
        val totalWeight = available.sumOf { it.weight }
        weightedCounter = (weightedCounter + 1) % totalWeight
        var sum = 0
        for (resource in available) {
            sum += resource.weight
            if (weightedCounter < sum) return resource
        }
        return available.last()
    }

    private fun updateStats() {
        val resourceStatsList = resources.values.map { r ->
            val total = resourceCounters[r.id]?.get() ?: 0
            val completed = completedCounters[r.id]?.get() ?: 0
            val totalDuration = durationAccumulators[r.id]?.get() ?: 0
            ResourceStats(
                resourceId = r.id,
                totalAssigned = total,
                currentActive = activeCounters[r.id]?.get()?.toInt() ?: 0,
                totalCompleted = completed,
                totalFailed = failedCounters[r.id]?.get() ?: 0,
                avgDurationMs = if (completed == 0L) 0.0 else totalDuration.toDouble() / completed
            )
        }
        _stats.value = SchedulerStats(
            totalResources = resources.size,
            enabledResources = resources.values.count { it.enabled },
            totalAssigned = resourceCounters.values.sumOf { it.get() },
            totalCompleted = completedCounters.values.sumOf { it.get() },
            totalFailed = failedCounters.values.sumOf { it.get() },
            resourceStats = resourceStatsList
        )
    }

    fun getResource(id: String): Resource<R>? = resources[id]
    fun getResourceIds(): Set<String> = resources.keys.toSet()
    fun getActiveAssignmentCount(): Int = activeAssignments.size
}
