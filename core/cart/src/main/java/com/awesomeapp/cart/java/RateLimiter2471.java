package com.awesomeapp.cart.java;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter2471 {

    public enum LimitStrategy { FIXED_WINDOW, SLIDING_WINDOW, TOKEN_BUCKET }

    public static class RateLimitResult {
        private final boolean allowed;
        private final int remainingTokens;
        private final long retryAfterMs;

        public RateLimitResult(boolean allowed, int remaining, long retryAfterMs) {
            this.allowed = allowed; this.remainingTokens = remaining; this.retryAfterMs = retryAfterMs;
        }

        public boolean isAllowed() { return allowed; }
        public int getRemainingTokens() { return remainingTokens; }
        public long getRetryAfterMs() { return retryAfterMs; }
    }

    private static class SlidingWindowCounter {
        private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();
        private final int maxRequests;
        private final long windowMs;

        SlidingWindowCounter(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests; this.windowMs = windowMs;
        }

        synchronized RateLimitResult tryAcquire() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowMs;
            while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                timestamps.poll();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.offer(now);
                return new RateLimitResult(true, maxRequests - timestamps.size(), 0);
            }
            long oldest = timestamps.peek();
            long retryAfter = oldest + windowMs - now;
            return new RateLimitResult(false, 0, retryAfter);
        }
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final double refillRate;
        private double currentTokens;
        private long lastRefillTime;

        TokenBucket(int maxTokens, double refillRate) {
            this.maxTokens = maxTokens; this.refillRate = refillRate;
            this.currentTokens = maxTokens; this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized RateLimitResult tryAcquire() {
            refill();
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                return new RateLimitResult(true, (int) currentTokens, 0);
            }
            long retryAfter = (long) ((1.0 - currentTokens) / refillRate * 1000);
            return new RateLimitResult(false, 0, retryAfter);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefillTime) / 1000.0;
            currentTokens = Math.min(maxTokens, currentTokens + elapsed * refillRate);
            lastRefillTime = now;
        }
    }

    private final Map<String, SlidingWindowCounter> slidingWindows = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private final LimitStrategy strategy;
    private final int maxRequests;
    private final long windowMs;
    private final AtomicLong totalAllowed = new AtomicLong(0);
    private final AtomicLong totalDenied = new AtomicLong(0);

    public RateLimiter2471(LimitStrategy strategy, int maxRequests, long windowMs) {
        this.strategy = strategy; this.maxRequests = maxRequests; this.windowMs = windowMs;
    }

    public RateLimitResult tryAcquire(String clientId) {
        RateLimitResult result;
        switch (strategy) {
            case TOKEN_BUCKET:
                result = tokenBuckets.computeIfAbsent(clientId,
                    k -> new TokenBucket(maxRequests, (double) maxRequests / (windowMs / 1000.0)))
                    .tryAcquire();
                break;
            default:
                result = slidingWindows.computeIfAbsent(clientId,
                    k -> new SlidingWindowCounter(maxRequests, windowMs))
                    .tryAcquire();
        }
        if (result.isAllowed()) totalAllowed.incrementAndGet(); else totalDenied.incrementAndGet();
        return result;
    }

    public void resetClient(String clientId) {
        slidingWindows.remove(clientId);
        tokenBuckets.remove(clientId);
    }

    public long getTotalAllowed() { return totalAllowed.get(); }
    public long getTotalDenied() { return totalDenied.get(); }
    public int getActiveClients() {
        return strategy == LimitStrategy.TOKEN_BUCKET ? tokenBuckets.size() : slidingWindows.size();
    }
}
