package com.awesomeapp.calendar.extra

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class CircuitBreaker1791(private val config: Config) {

    data class Config(
        val failureThreshold: Int = 5,
        val successThreshold: Int = 3,
        val timeoutMs: Long = 60_000,
        val halfOpenMaxCalls: Int = 3,
        val failureRateThreshold: Double = 0.5,
        val slidingWindowSize: Int = 10
    )

    enum class State { CLOSED, OPEN, HALF_OPEN }

    data class CircuitStats(
        val state: State,
        val failureCount: Int,
        val successCount: Int,
        val totalCalls: Long,
        val consecutiveFailures: Int,
        val consecutiveSuccesses: Int,
        val lastFailureTime: Long?,
        val lastSuccessTime: Long?,
        val failureRate: Double
    )

    sealed class CircuitResult<out T> {
        data class Success<T>(val value: T) : CircuitResult<T>()
        data class Failure(val error: Throwable) : CircuitResult<Nothing>()
        data class Rejected(val reason: String) : CircuitResult<Nothing>()
    }

    private val _state = MutableStateFlow(State.CLOSED)
    val state: StateFlow<State> = _state.asStateFlow()

    private val mutex = Mutex()
    private var consecutiveFailures = 0
    private var consecutiveSuccesses = 0
    private var halfOpenCalls = 0
    private var lastFailureTime: Long? = null
    private var lastSuccessTime: Long? = null
    private var openedAt: Long = 0
    private val totalCalls = AtomicLong(0)
    private val slidingWindow = ArrayDeque<Boolean>(config.slidingWindowSize)
    private val listeners = mutableListOf<suspend (State, State) -> Unit>()

    fun onStateChange(listener: suspend (State, State) -> Unit) { listeners.add(listener) }

    suspend fun <T> execute(action: suspend () -> T): CircuitResult<T> = mutex.withLock {
        val currentState = _state.value
        when (currentState) {
            State.OPEN -> {
                if (System.currentTimeMillis() - openedAt >= config.timeoutMs) {
                    transitionTo(State.HALF_OPEN)
                    executeInHalfOpen(action)
                } else {
                    CircuitResult.Rejected("Circuit is OPEN, retry after ${config.timeoutMs - (System.currentTimeMillis() - openedAt)}ms")
                }
            }
            State.HALF_OPEN -> {
                if (halfOpenCalls < config.halfOpenMaxCalls) {
                    executeInHalfOpen(action)
                } else {
                    CircuitResult.Rejected("HALF_OPEN max calls reached")
                }
            }
            State.CLOSED -> executeInClosed(action)
        }
    }

    private suspend fun <T> executeInClosed(action: suspend () -> T): CircuitResult<T> {
        totalCalls.incrementAndGet()
        return try {
            val result = action()
            recordSuccess()
            CircuitResult.Success(result)
        } catch (e: Throwable) {
            recordFailure()
            if (shouldTrip()) transitionTo(State.OPEN)
            CircuitResult.Failure(e)
        }
    }

    private suspend fun <T> executeInHalfOpen(action: suspend () -> T): CircuitResult<T> {
        totalCalls.incrementAndGet()
        halfOpenCalls++
        return try {
            val result = action()
            recordSuccess()
            if (consecutiveSuccesses >= config.successThreshold) {
                transitionTo(State.CLOSED)
            }
            CircuitResult.Success(result)
        } catch (e: Throwable) {
            recordFailure()
            transitionTo(State.OPEN)
            CircuitResult.Failure(e)
        }
    }

    private fun recordSuccess() {
        consecutiveSuccesses++
        consecutiveFailures = 0
        lastSuccessTime = System.currentTimeMillis()
        addToWindow(true)
    }

    private fun recordFailure() {
        consecutiveFailures++
        consecutiveSuccesses = 0
        lastFailureTime = System.currentTimeMillis()
        addToWindow(false)
    }

    private fun addToWindow(success: Boolean) {
        if (slidingWindow.size >= config.slidingWindowSize) slidingWindow.removeFirst()
        slidingWindow.addLast(success)
    }

    private fun shouldTrip(): Boolean {
        if (consecutiveFailures >= config.failureThreshold) return true
        if (slidingWindow.size >= config.slidingWindowSize) {
            val failureRate = slidingWindow.count { !it }.toDouble() / slidingWindow.size
            if (failureRate >= config.failureRateThreshold) return true
        }
        return false
    }

    private suspend fun transitionTo(newState: State) {
        val old = _state.value
        if (old == newState) return
        _state.value = newState
        when (newState) {
            State.OPEN -> { openedAt = System.currentTimeMillis(); halfOpenCalls = 0 }
            State.HALF_OPEN -> { halfOpenCalls = 0; consecutiveSuccesses = 0 }
            State.CLOSED -> { consecutiveFailures = 0; slidingWindow.clear() }
        }
        listeners.forEach { it(old, newState) }
    }

    fun getStats(): CircuitStats = CircuitStats(
        state = _state.value, failureCount = slidingWindow.count { !it },
        successCount = slidingWindow.count { it }, totalCalls = totalCalls.get(),
        consecutiveFailures = consecutiveFailures, consecutiveSuccesses = consecutiveSuccesses,
        lastFailureTime = lastFailureTime, lastSuccessTime = lastSuccessTime,
        failureRate = if (slidingWindow.isEmpty()) 0.0 else slidingWindow.count { !it }.toDouble() / slidingWindow.size
    )

    suspend fun reset() = mutex.withLock {
        transitionTo(State.CLOSED)
        consecutiveFailures = 0; consecutiveSuccesses = 0
        slidingWindow.clear()
    }
}
