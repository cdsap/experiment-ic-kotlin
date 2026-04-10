package com.awesomeapp.cart.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class RetryPolicy2069<T> {

    public enum BackoffStrategy { FIXED, LINEAR, EXPONENTIAL, FIBONACCI }

    public static class RetryAttempt {
        private final int attemptNumber;
        private final long delayMs;
        private final Exception error;
        private final long durationMs;

        public RetryAttempt(int attemptNumber, long delayMs, Exception error, long durationMs) {
            this.attemptNumber = attemptNumber; this.delayMs = delayMs;
            this.error = error; this.durationMs = durationMs;
        }

        public int getAttemptNumber() { return attemptNumber; }
        public long getDelayMs() { return delayMs; }
        public Exception getError() { return error; }
        public long getDurationMs() { return durationMs; }
    }

    public static class RetryResult<T> {
        private final T value;
        private final boolean success;
        private final List<RetryAttempt> attempts;
        private final long totalDurationMs;

        public RetryResult(T value, boolean success, List<RetryAttempt> attempts, long totalDurationMs) {
            this.value = value; this.success = success;
            this.attempts = attempts; this.totalDurationMs = totalDurationMs;
        }

        public Optional<T> getValue() { return Optional.ofNullable(value); }
        public boolean isSuccess() { return success; }
        public List<RetryAttempt> getAttempts() { return attempts; }
        public int getAttemptCount() { return attempts.size(); }
        public long getTotalDurationMs() { return totalDurationMs; }
    }

    private int maxAttempts = 3;
    private long baseDelayMs = 1000;
    private long maxDelayMs = 30000;
    private double multiplier = 2.0;
    private BackoffStrategy backoff = BackoffStrategy.EXPONENTIAL;
    private Predicate<Exception> retryableException = e -> true;
    private Predicate<T> retryableResult = r -> false;

    public RetryPolicy2069<T> maxAttempts(int max) { this.maxAttempts = max; return this; }
    public RetryPolicy2069<T> baseDelay(long ms) { this.baseDelayMs = ms; return this; }
    public RetryPolicy2069<T> maxDelay(long ms) { this.maxDelayMs = ms; return this; }
    public RetryPolicy2069<T> multiplier(double m) { this.multiplier = m; return this; }
    public RetryPolicy2069<T> backoffStrategy(BackoffStrategy s) { this.backoff = s; return this; }
    public RetryPolicy2069<T> retryOn(Predicate<Exception> predicate) { this.retryableException = predicate; return this; }
    public RetryPolicy2069<T> retryIf(Predicate<T> predicate) { this.retryableResult = predicate; return this; }

    public RetryResult<T> execute(Callable<T> callable) {
        List<RetryAttempt> attempts = new ArrayList<>();
        long totalStart = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                T result = callable.call();
                long duration = System.currentTimeMillis() - start;
                if (!retryableResult.test(result) || attempt == maxAttempts) {
                    attempts.add(new RetryAttempt(attempt, 0, null, duration));
                    return new RetryResult<>(result, true, attempts, System.currentTimeMillis() - totalStart);
                }
                long delay = calculateDelay(attempt);
                attempts.add(new RetryAttempt(attempt, delay, null, duration));
                sleep(delay);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                long delay = calculateDelay(attempt);
                attempts.add(new RetryAttempt(attempt, delay, e, duration));
                if (attempt == maxAttempts || !retryableException.test(e)) {
                    return new RetryResult<>(null, false, attempts, System.currentTimeMillis() - totalStart);
                }
                sleep(delay);
            }
        }
        return new RetryResult<>(null, false, attempts, System.currentTimeMillis() - totalStart);
    }

    private long calculateDelay(int attempt) {
        long delay;
        switch (backoff) {
            case FIXED: delay = baseDelayMs; break;
            case LINEAR: delay = baseDelayMs * attempt; break;
            case EXPONENTIAL: delay = (long) (baseDelayMs * Math.pow(multiplier, attempt - 1)); break;
            case FIBONACCI: delay = baseDelayMs * fibonacci(attempt); break;
            default: delay = baseDelayMs;
        }
        return Math.min(delay, maxDelayMs);
    }

    private long fibonacci(int n) {
        if (n <= 1) return 1;
        long a = 1, b = 1;
        for (int i = 2; i < n; i++) { long tmp = a + b; a = b; b = tmp; }
        return b;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
