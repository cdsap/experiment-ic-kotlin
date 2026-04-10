package com.awesomeapp.cart.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CircuitBreaker2212<T> {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    public static class CircuitBreakerConfig {
        private int failureThreshold = 5;
        private long openDurationMs = 60000;
        private int halfOpenMaxAttempts = 3;
        private double failureRateThreshold = 0.5;
        private int slidingWindowSize = 10;

        public CircuitBreakerConfig failureThreshold(int t) { this.failureThreshold = t; return this; }
        public CircuitBreakerConfig openDuration(long ms) { this.openDurationMs = ms; return this; }
        public CircuitBreakerConfig halfOpenMax(int max) { this.halfOpenMaxAttempts = max; return this; }
        public CircuitBreakerConfig failureRate(double rate) { this.failureRateThreshold = rate; return this; }
        public CircuitBreakerConfig windowSize(int size) { this.slidingWindowSize = size; return this; }
    }

    public static class CallResult<T> {
        private final T value;
        private final Exception error;
        private final State circuitState;
        private final long durationMs;

        CallResult(T value, Exception error, State state, long duration) {
            this.value = value; this.error = error; this.circuitState = state; this.durationMs = duration;
        }

        public Optional<T> getValue() { return Optional.ofNullable(value); }
        public Optional<Exception> getError() { return Optional.ofNullable(error); }
        public State getCircuitState() { return circuitState; }
        public boolean isSuccess() { return error == null; }
        public long getDurationMs() { return durationMs; }
    }

    private final CircuitBreakerConfig config;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalRejected = new AtomicLong(0);
    private final List<Boolean> slidingWindow = new ArrayList<>();
    private final List<Consumer<State>> stateListeners = new ArrayList<>();
    private Predicate<Exception> recordableException = e -> true;
    private Callable<T> fallback;

    public CircuitBreaker2212(CircuitBreakerConfig config) {
        this.config = config;
    }

    public CircuitBreaker2212<T> onStateChange(Consumer<State> listener) {
        stateListeners.add(listener);
        return this;
    }

    public CircuitBreaker2212<T> recordExceptions(Predicate<Exception> predicate) {
        this.recordableException = predicate;
        return this;
    }

    public CircuitBreaker2212<T> withFallback(Callable<T> fallback) {
        this.fallback = fallback;
        return this;
    }

    public CallResult<T> execute(Callable<T> callable) {
        totalCalls.incrementAndGet();
        State current = state.get();

        if (current == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() >= config.openDurationMs) {
                transitionTo(State.HALF_OPEN);
                halfOpenAttempts.set(0);
            } else {
                totalRejected.incrementAndGet();
                return executeFallback();
            }
        }

        if (state.get() == State.HALF_OPEN && halfOpenAttempts.incrementAndGet() > config.halfOpenMaxAttempts) {
            transitionTo(State.OPEN);
            totalRejected.incrementAndGet();
            return executeFallback();
        }

        long start = System.currentTimeMillis();
        try {
            T result = callable.call();
            long duration = System.currentTimeMillis() - start;
            recordSuccess();
            return new CallResult<>(result, null, state.get(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            if (recordableException.test(e)) recordFailure();
            return new CallResult<>(null, e, state.get(), duration);
        }
    }

    private void recordSuccess() {
        totalSuccess.incrementAndGet();
        consecutiveFailures.set(0);
        addToWindow(true);
        if (state.get() == State.HALF_OPEN) transitionTo(State.CLOSED);
    }

    private void recordFailure() {
        totalFailures.incrementAndGet();
        consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        addToWindow(false);
        if (consecutiveFailures.get() >= config.failureThreshold || getFailureRate() >= config.failureRateThreshold) {
            transitionTo(State.OPEN);
        }
    }

    private synchronized void addToWindow(boolean success) {
        slidingWindow.add(success);
        while (slidingWindow.size() > config.slidingWindowSize) slidingWindow.remove(0);
    }

    private double getFailureRate() {
        if (slidingWindow.size() < config.slidingWindowSize) return 0;
        long failures = slidingWindow.stream().filter(b -> !b).count();
        return (double) failures / slidingWindow.size();
    }

    private void transitionTo(State newState) {
        State old = state.getAndSet(newState);
        if (old != newState) stateListeners.forEach(l -> l.accept(newState));
    }

    private CallResult<T> executeFallback() {
        if (fallback != null) {
            try { return new CallResult<>(fallback.call(), null, state.get(), 0); }
            catch (Exception e) { return new CallResult<>(null, e, state.get(), 0); }
        }
        return new CallResult<>(null, new RuntimeException("Circuit is open"), state.get(), 0);
    }

    public State getState() { return state.get(); }
    public long getTotalCalls() { return totalCalls.get(); }
    public long getTotalFailures() { return totalFailures.get(); }
    public long getTotalRejected() { return totalRejected.get(); }
}
