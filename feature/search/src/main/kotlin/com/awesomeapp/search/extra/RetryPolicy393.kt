package com.awesomeapp.search.extra

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class RetryPolicy393 private constructor(
    private val maxRetries: Int,
    private val strategy: BackoffStrategy,
    private val retryOn: (Throwable) -> Boolean,
    private val onRetry: suspend (RetryContext) -> Unit,
    private val onExhausted: suspend (RetryContext) -> Unit
) {
    sealed class BackoffStrategy {
        data class Fixed(val delayMs: Long) : BackoffStrategy()
        data class Linear(val initialDelayMs: Long, val incrementMs: Long, val maxDelayMs: Long = Long.MAX_VALUE) : BackoffStrategy()
        data class Exponential(val initialDelayMs: Long, val multiplier: Double = 2.0, val maxDelayMs: Long = 60_000) : BackoffStrategy()
        data class ExponentialWithJitter(
            val initialDelayMs: Long, val multiplier: Double = 2.0,
            val maxDelayMs: Long = 60_000, val jitterFactor: Double = 0.5
        ) : BackoffStrategy()
        data class Custom(val delayProvider: (Int) -> Long) : BackoffStrategy()
    }

    data class RetryContext(
        val attempt: Int,
        val totalAttempts: Int,
        val lastError: Throwable?,
        val totalElapsedMs: Long,
        val nextDelayMs: Long
    )

    sealed class RetryResult<out T> {
        data class Success<T>(val value: T, val attempts: Int, val totalTimeMs: Long) : RetryResult<T>()
        data class Exhausted(val lastError: Throwable, val attempts: Int, val totalTimeMs: Long) : RetryResult<Nothing>()
    }

    data class RetryStats(
        val totalExecutions: Long = 0,
        val totalRetries: Long = 0,
        val successAfterRetry: Long = 0,
        val exhausted: Long = 0,
        val firstAttemptSuccess: Long = 0
    )

    private val _stats = MutableStateFlow(RetryStats())
    val stats: StateFlow<RetryStats> = _stats.asStateFlow()

    suspend fun <T> execute(action: suspend () -> T): RetryResult<T> {
        val startTime = System.currentTimeMillis()
        var lastError: Throwable? = null
        var currentStats = _stats.value

        for (attempt in 0..maxRetries) {
            try {
                val result = action()
                val totalTime = System.currentTimeMillis() - startTime
                _stats.value = if (attempt == 0) {
                    currentStats.copy(totalExecutions = currentStats.totalExecutions + 1, firstAttemptSuccess = currentStats.firstAttemptSuccess + 1)
                } else {
                    currentStats.copy(totalExecutions = currentStats.totalExecutions + 1, successAfterRetry = currentStats.successAfterRetry + 1, totalRetries = currentStats.totalRetries + attempt)
                }
                return RetryResult.Success(result, attempt + 1, totalTime)
            } catch (e: Throwable) {
                lastError = e
                if (attempt >= maxRetries || !retryOn(e)) break

                val delayMs = calculateDelay(attempt)
                val elapsed = System.currentTimeMillis() - startTime
                val context = RetryContext(attempt + 1, maxRetries + 1, e, elapsed, delayMs)
                onRetry(context)
                delay(delayMs)
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        val context = RetryContext(maxRetries + 1, maxRetries + 1, lastError, totalTime, 0)
        onExhausted(context)
        currentStats = _stats.value
        _stats.value = currentStats.copy(totalExecutions = currentStats.totalExecutions + 1, exhausted = currentStats.exhausted + 1, totalRetries = currentStats.totalRetries + maxRetries)
        return RetryResult.Exhausted(lastError!!, maxRetries + 1, totalTime)
    }

    private fun calculateDelay(attempt: Int): Long = when (strategy) {
        is BackoffStrategy.Fixed -> strategy.delayMs
        is BackoffStrategy.Linear -> min(strategy.initialDelayMs + strategy.incrementMs * attempt, strategy.maxDelayMs)
        is BackoffStrategy.Exponential -> min((strategy.initialDelayMs * strategy.multiplier.pow(attempt)).toLong(), strategy.maxDelayMs)
        is BackoffStrategy.ExponentialWithJitter -> {
            val base = min((strategy.initialDelayMs * strategy.multiplier.pow(attempt)).toLong(), strategy.maxDelayMs)
            val jitter = (base * strategy.jitterFactor * Random.nextDouble()).toLong()
            base + jitter
        }
        is BackoffStrategy.Custom -> strategy.delayProvider(attempt)
    }

    class Builder {
        private var maxRetries: Int = 3
        private var strategy: BackoffStrategy = BackoffStrategy.ExponentialWithJitter(1000)
        private var retryOn: (Throwable) -> Boolean = { true }
        private var onRetry: suspend (RetryContext) -> Unit = {}
        private var onExhausted: suspend (RetryContext) -> Unit = {}

        fun maxRetries(n: Int) = apply { maxRetries = n }
        fun strategy(s: BackoffStrategy) = apply { strategy = s }
        fun retryOn(predicate: (Throwable) -> Boolean) = apply { retryOn = predicate }
        fun onRetry(handler: suspend (RetryContext) -> Unit) = apply { onRetry = handler }
        fun onExhausted(handler: suspend (RetryContext) -> Unit) = apply { onExhausted = handler }
        fun build() = RetryPolicy393(maxRetries, strategy, retryOn, onRetry, onExhausted)
    }

    companion object {
        fun builder() = Builder()
        fun simple(maxRetries: Int = 3, delayMs: Long = 1000) = Builder().maxRetries(maxRetries).strategy(BackoffStrategy.Fixed(delayMs)).build()
        fun exponential(maxRetries: Int = 3, initialDelayMs: Long = 1000) = Builder().maxRetries(maxRetries).strategy(BackoffStrategy.Exponential(initialDelayMs)).build()
    }
}
