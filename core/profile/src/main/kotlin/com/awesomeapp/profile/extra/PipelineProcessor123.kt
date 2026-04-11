package com.awesomeapp.profile.extra

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class PipelineProcessor123<I : Any, O : Any> private constructor(
    private val stages: List<Stage<*, *>>,
    private val errorHandler: suspend (Throwable, StageInfo) -> ErrorAction
) {
    enum class ErrorAction { SKIP, RETRY, ABORT, FALLBACK }

    data class StageInfo(val name: String, val index: Int, val retryCount: Int = 0)

    sealed class StageResult<out T> {
        data class Success<T>(val value: T) : StageResult<T>()
        data class Failure(val error: Throwable, val stage: StageInfo) : StageResult<Nothing>()
        data class Skipped(val reason: String) : StageResult<Nothing>()
    }

    data class PipelineMetrics(
        val processed: Long = 0,
        val succeeded: Long = 0,
        val failed: Long = 0,
        val skipped: Long = 0,
        val totalDurationMs: Long = 0,
        val stageDurations: Map<String, Long> = emptyMap()
    ) {
        val successRate: Double get() = if (processed == 0L) 0.0 else succeeded.toDouble() / processed
        val avgDurationMs: Double get() = if (processed == 0L) 0.0 else totalDurationMs.toDouble() / processed
    }

    interface Stage<I, O> {
        val name: String
        val maxRetries: Int get() = 0
        suspend fun process(input: I): O
        suspend fun onError(input: I, error: Throwable): O? = null
    }

    private val _metrics = MutableStateFlow(PipelineMetrics())
    val metrics: StateFlow<PipelineMetrics> = _metrics.asStateFlow()
    private val mutex = Mutex()
    private val processedCount = AtomicLong(0)

    @Suppress("UNCHECKED_CAST")
    suspend fun execute(input: I): StageResult<O> {
        val startTime = System.currentTimeMillis()
        var current: Any = input
        val stageDurations = mutableMapOf<String, Long>()

        for ((index, stage) in stages.withIndex()) {
            val typedStage = stage as Stage<Any, Any>
            val info = StageInfo(stage.name, index)
            val stageStart = System.currentTimeMillis()

            var retryCount = 0
            var stageCompleted = false

            while (!stageCompleted && retryCount <= typedStage.maxRetries) {
                try {
                    current = typedStage.process(current)
                    stageDurations[stage.name] = System.currentTimeMillis() - stageStart
                    stageCompleted = true
                } catch (e: Throwable) {
                    val retryInfo = info.copy(retryCount = retryCount)
                    when (errorHandler(e, retryInfo)) {
                        ErrorAction.RETRY -> {
                            retryCount++
                            if (retryCount > typedStage.maxRetries) {
                                updateMetricsFailed(startTime, stageDurations)
                                return StageResult.Failure(e, retryInfo)
                            }
                        }
                        ErrorAction.SKIP -> {
                            updateMetricsSkipped()
                            return StageResult.Skipped("Skipped at stage ${stage.name}")
                        }
                        ErrorAction.FALLBACK -> {
                            val fallback = typedStage.onError(current, e)
                            if (fallback != null) {
                                current = fallback
                                stageCompleted = true
                            } else {
                                updateMetricsFailed(startTime, stageDurations)
                                return StageResult.Failure(e, retryInfo)
                            }
                        }
                        ErrorAction.ABORT -> {
                            updateMetricsFailed(startTime, stageDurations)
                            return StageResult.Failure(e, retryInfo)
                        }
                    }
                }
            }
        }

        updateMetricsSuccess(startTime, stageDurations)
        return StageResult.Success(current as O)
    }

    fun processStream(inputs: Flow<I>): Flow<StageResult<O>> = inputs.map { execute(it) }

    private suspend fun updateMetricsSuccess(startTime: Long, stageDurations: Map<String, Long>) = mutex.withLock {
        val duration = System.currentTimeMillis() - startTime
        val current = _metrics.value
        _metrics.value = current.copy(
            processed = current.processed + 1, succeeded = current.succeeded + 1,
            totalDurationMs = current.totalDurationMs + duration,
            stageDurations = mergeDurations(current.stageDurations, stageDurations)
        )
    }

    private suspend fun updateMetricsFailed(startTime: Long, stageDurations: Map<String, Long>) = mutex.withLock {
        val duration = System.currentTimeMillis() - startTime
        val current = _metrics.value
        _metrics.value = current.copy(
            processed = current.processed + 1, failed = current.failed + 1,
            totalDurationMs = current.totalDurationMs + duration,
            stageDurations = mergeDurations(current.stageDurations, stageDurations)
        )
    }

    private suspend fun updateMetricsSkipped() = mutex.withLock {
        val current = _metrics.value
        _metrics.value = current.copy(processed = current.processed + 1, skipped = current.skipped + 1)
    }

    private fun mergeDurations(existing: Map<String, Long>, newDurations: Map<String, Long>): Map<String, Long> {
        val merged = existing.toMutableMap()
        newDurations.forEach { (k, v) -> merged[k] = (merged[k] ?: 0L) + v }
        return merged
    }

    class Builder<I : Any, O : Any> {
        private val stages = mutableListOf<Stage<*, *>>()
        private var errorHandler: suspend (Throwable, StageInfo) -> ErrorAction = { _, _ -> ErrorAction.ABORT }

        fun <T : Any> addStage(stage: Stage<*, T>): Builder<I, O> { stages.add(stage); return this }
        fun onError(handler: suspend (Throwable, StageInfo) -> ErrorAction): Builder<I, O> { errorHandler = handler; return this }
        fun build(): PipelineProcessor123<I, O> = PipelineProcessor123(stages, errorHandler)
    }
}
