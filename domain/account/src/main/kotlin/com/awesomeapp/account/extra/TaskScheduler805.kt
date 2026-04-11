package com.awesomeapp.account.extra

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class TaskScheduler805(private val scope: CoroutineScope, private val concurrency: Int = 4) {

    enum class TaskState { PENDING, READY, RUNNING, COMPLETED, FAILED, CANCELLED }
    enum class Priority(val value: Int) { LOW(0), NORMAL(1), HIGH(2), CRITICAL(3) }

    data class ScheduledTask(
        val id: String,
        val name: String,
        val priority: Priority,
        val dependencies: Set<String> = emptySet(),
        val timeoutMs: Long = 30_000,
        val maxRetries: Int = 0,
        val action: suspend () -> Any?
    )

    data class TaskResult(
        val taskId: String,
        val state: TaskState,
        val result: Any? = null,
        val error: Throwable? = null,
        val durationMs: Long = 0,
        val retryCount: Int = 0
    )

    data class SchedulerStats(
        val pending: Int, val running: Int, val completed: Int,
        val failed: Int, val cancelled: Int, val totalDurationMs: Long
    )

    private val tasks = ConcurrentHashMap<String, ScheduledTask>()
    private val taskStates = ConcurrentHashMap<String, TaskState>()
    private val taskResults = ConcurrentHashMap<String, TaskResult>()
    private val mutex = Mutex()
    private val runningCount = AtomicLong(0)
    private val _progress = MutableStateFlow(SchedulerStats(0, 0, 0, 0, 0, 0))
    val progress: StateFlow<SchedulerStats> = _progress.asStateFlow()

    private val readyQueue = PriorityQueue<ScheduledTask>(compareByDescending { it.priority.value })

    suspend fun submit(task: ScheduledTask) = mutex.withLock {
        tasks[task.id] = task
        taskStates[task.id] = TaskState.PENDING
        checkReady(task)
        updateStats()
    }

    private fun checkReady(task: ScheduledTask) {
        if (task.dependencies.all { taskStates[it] == TaskState.COMPLETED }) {
            taskStates[task.id] = TaskState.READY
            readyQueue.add(task)
        }
    }

    suspend fun executeAll(): Map<String, TaskResult> {
        while (hasPendingWork()) {
            mutex.withLock {
                while (readyQueue.isNotEmpty() && runningCount.get() < concurrency) {
                    val task = readyQueue.poll() ?: break
                    launchTask(task)
                }
            }
            delay(10)
        }
        return taskResults.toMap()
    }

    private fun launchTask(task: ScheduledTask) {
        taskStates[task.id] = TaskState.RUNNING
        runningCount.incrementAndGet()
        scope.launch {
            val startTime = System.currentTimeMillis()
            var retryCount = 0
            var lastError: Throwable? = null

            while (retryCount <= task.maxRetries) {
                try {
                    val result = withTimeout(task.timeoutMs) { task.action() }
                    val duration = System.currentTimeMillis() - startTime
                    taskStates[task.id] = TaskState.COMPLETED
                    taskResults[task.id] = TaskResult(task.id, TaskState.COMPLETED, result, durationMs = duration, retryCount = retryCount)
                    runningCount.decrementAndGet()
                    onTaskCompleted(task.id)
                    return@launch
                } catch (e: Throwable) {
                    lastError = e
                    retryCount++
                }
            }

            val duration = System.currentTimeMillis() - startTime
            taskStates[task.id] = TaskState.FAILED
            taskResults[task.id] = TaskResult(task.id, TaskState.FAILED, error = lastError, durationMs = duration, retryCount = retryCount - 1)
            runningCount.decrementAndGet()
            onTaskCompleted(task.id)
        }
    }

    private suspend fun onTaskCompleted(completedTaskId: String) = mutex.withLock {
        tasks.values
            .filter { it.dependencies.contains(completedTaskId) }
            .filter { taskStates[it.id] == TaskState.PENDING }
            .forEach { checkReady(it) }
        updateStats()
    }

    suspend fun cancel(taskId: String) = mutex.withLock {
        if (taskStates[taskId] in setOf(TaskState.PENDING, TaskState.READY)) {
            taskStates[taskId] = TaskState.CANCELLED
            taskResults[taskId] = TaskResult(taskId, TaskState.CANCELLED)
            readyQueue.removeIf { it.id == taskId }
            updateStats()
        }
    }

    private fun hasPendingWork(): Boolean =
        taskStates.values.any { it in setOf(TaskState.PENDING, TaskState.READY, TaskState.RUNNING) }

    private fun updateStats() {
        val states = taskStates.values
        _progress.value = SchedulerStats(
            pending = states.count { it == TaskState.PENDING || it == TaskState.READY },
            running = states.count { it == TaskState.RUNNING },
            completed = states.count { it == TaskState.COMPLETED },
            failed = states.count { it == TaskState.FAILED },
            cancelled = states.count { it == TaskState.CANCELLED },
            totalDurationMs = taskResults.values.sumOf { it.durationMs }
        )
    }

    fun getResult(taskId: String): TaskResult? = taskResults[taskId]
    fun getState(taskId: String): TaskState? = taskStates[taskId]
    fun getStats(): SchedulerStats = _progress.value
}
