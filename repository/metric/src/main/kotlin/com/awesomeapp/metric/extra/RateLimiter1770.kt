package com.awesomeapp.metric.extra

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class RateLimiter1770(private val strategy: Strategy) {

    sealed class Strategy {
        data class TokenBucket(val capacity: Long, val refillRate: Double, val refillIntervalMs: Long = 1000) : Strategy()
        data class SlidingWindow(val maxRequests: Long, val windowMs: Long) : Strategy()
        data class FixedWindow(val maxRequests: Long, val windowMs: Long) : Strategy()
        data class LeakyBucket(val capacity: Long, val leakRate: Double, val leakIntervalMs: Long = 1000) : Strategy()
    }

    data class RateLimitResult(
        val allowed: Boolean,
        val remainingTokens: Long,
        val retryAfterMs: Long = 0,
        val currentUsage: Long = 0
    )

    data class LimiterStats(
        val allowed: Long = 0,
        val denied: Long = 0,
        val totalRequests: Long = 0,
        val currentUsage: Long = 0,
        val capacity: Long = 0
    ) {
        val denyRate: Double get() = if (totalRequests == 0L) 0.0 else denied.toDouble() / totalRequests
    }

    private val mutex = Mutex()
    private var tokens: Double = when (strategy) {
        is Strategy.TokenBucket -> strategy.capacity.toDouble()
        is Strategy.LeakyBucket -> 0.0
        else -> 0.0
    }
    private var lastRefillTime = System.currentTimeMillis()
    private val slidingWindowLog = LinkedList<Long>()
    private var fixedWindowStart = System.currentTimeMillis()
    private var fixedWindowCount = 0L
    private val allowedCount = AtomicLong(0)
    private val deniedCount = AtomicLong(0)

    private val perKeyLimiters = ConcurrentHashMap<String, RateLimiter1770>()

    suspend fun tryAcquire(permits: Long = 1): RateLimitResult = mutex.withLock {
        when (strategy) {
            is Strategy.TokenBucket -> acquireTokenBucket(permits, strategy)
            is Strategy.SlidingWindow -> acquireSlidingWindow(permits, strategy)
            is Strategy.FixedWindow -> acquireFixedWindow(permits, strategy)
            is Strategy.LeakyBucket -> acquireLeakyBucket(permits, strategy)
        }
    }

    private fun acquireTokenBucket(permits: Long, config: Strategy.TokenBucket): RateLimitResult {
        refillTokens(config)
        return if (tokens >= permits) {
            tokens -= permits
            allowedCount.incrementAndGet()
            RateLimitResult(true, tokens.toLong(), currentUsage = (config.capacity - tokens.toLong()))
        } else {
            deniedCount.incrementAndGet()
            val waitMs = ((permits - tokens) / config.refillRate * config.refillIntervalMs).toLong()
            RateLimitResult(false, tokens.toLong(), waitMs, currentUsage = (config.capacity - tokens.toLong()))
        }
    }

    private fun refillTokens(config: Strategy.TokenBucket) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        val tokensToAdd = (elapsed.toDouble() / config.refillIntervalMs) * config.refillRate
        tokens = (tokens + tokensToAdd).coerceAtMost(config.capacity.toDouble())
        lastRefillTime = now
    }

    private fun acquireSlidingWindow(permits: Long, config: Strategy.SlidingWindow): RateLimitResult {
        val now = System.currentTimeMillis()
        val windowStart = now - config.windowMs
        while (slidingWindowLog.isNotEmpty() && slidingWindowLog.peek() < windowStart) {
            slidingWindowLog.poll()
        }
        val currentCount = slidingWindowLog.size.toLong()
        return if (currentCount + permits <= config.maxRequests) {
            repeat(permits.toInt()) { slidingWindowLog.add(now) }
            allowedCount.incrementAndGet()
            RateLimitResult(true, config.maxRequests - currentCount - permits, currentUsage = currentCount + permits)
        } else {
            deniedCount.incrementAndGet()
            val oldestInWindow = slidingWindowLog.peek() ?: now
            val retryAfter = oldestInWindow + config.windowMs - now
            RateLimitResult(false, 0, retryAfter, currentUsage = currentCount)
        }
    }

    private fun acquireFixedWindow(permits: Long, config: Strategy.FixedWindow): RateLimitResult {
        val now = System.currentTimeMillis()
        if (now - fixedWindowStart >= config.windowMs) {
            fixedWindowStart = now
            fixedWindowCount = 0
        }
        return if (fixedWindowCount + permits <= config.maxRequests) {
            fixedWindowCount += permits
            allowedCount.incrementAndGet()
            RateLimitResult(true, config.maxRequests - fixedWindowCount, currentUsage = fixedWindowCount)
        } else {
            deniedCount.incrementAndGet()
            val retryAfter = fixedWindowStart + config.windowMs - now
            RateLimitResult(false, 0, retryAfter, currentUsage = fixedWindowCount)
        }
    }

    private fun acquireLeakyBucket(permits: Long, config: Strategy.LeakyBucket): RateLimitResult {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        val leaked = (elapsed.toDouble() / config.leakIntervalMs) * config.leakRate
        tokens = (tokens - leaked).coerceAtLeast(0.0)
        lastRefillTime = now

        return if (tokens + permits <= config.capacity) {
            tokens += permits
            allowedCount.incrementAndGet()
            RateLimitResult(true, (config.capacity - tokens.toLong()), currentUsage = tokens.toLong())
        } else {
            deniedCount.incrementAndGet()
            RateLimitResult(false, 0, config.leakIntervalMs, currentUsage = tokens.toLong())
        }
    }

    suspend fun acquire(permits: Long = 1) {
        while (true) {
            val result = tryAcquire(permits)
            if (result.allowed) return
            delay(result.retryAfterMs.coerceAtLeast(10))
        }
    }

    fun forKey(key: String): RateLimiter1770 = perKeyLimiters.getOrPut(key) { RateLimiter1770(strategy) }

    fun getStats(): LimiterStats {
        val total = allowedCount.get() + deniedCount.get()
        return LimiterStats(allowedCount.get(), deniedCount.get(), total, tokens.toLong(),
            when (strategy) {
                is Strategy.TokenBucket -> strategy.capacity
                is Strategy.SlidingWindow -> strategy.maxRequests
                is Strategy.FixedWindow -> strategy.maxRequests
                is Strategy.LeakyBucket -> strategy.capacity
            })
    }
}
