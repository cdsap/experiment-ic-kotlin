package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class Pipeline5183<I, O> {

    @FunctionalInterface
    public interface Stage<IN, OUT> {
        OUT process(IN input) throws Exception;
    }

    public static class StageMetrics {
        private final String name;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private long totalDurationNanos = 0;

        public StageMetrics(String name) { this.name = name; }
        public void recordSuccess(long durationNanos) { successCount.incrementAndGet(); totalDurationNanos += durationNanos; }
        public void recordFailure() { failureCount.incrementAndGet(); }
        public String getName() { return name; }
        public int getSuccessCount() { return successCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
        public double getAvgDurationMs() {
            int total = successCount.get();
            return total == 0 ? 0 : (totalDurationNanos / 1_000_000.0) / total;
        }
    }

    public static class PipelineResult<T> {
        private final T value;
        private final Exception error;
        private final String failedStage;

        private PipelineResult(T value, Exception error, String failedStage) {
            this.value = value; this.error = error; this.failedStage = failedStage;
        }

        public static <T> PipelineResult<T> success(T value) { return new PipelineResult<>(value, null, null); }
        public static <T> PipelineResult<T> failure(Exception e, String stage) { return new PipelineResult<>(null, e, stage); }

        public boolean isSuccess() { return error == null; }
        public Optional<T> getValue() { return Optional.ofNullable(value); }
        public Optional<Exception> getError() { return Optional.ofNullable(error); }
        public Optional<String> getFailedStage() { return Optional.ofNullable(failedStage); }
    }

    private final List<String> stageNames = new ArrayList<>();
    private final List<Stage<Object, Object>> stages = new ArrayList<>();
    private final Map<String, StageMetrics> metrics = new LinkedHashMap<>();
    private Predicate<Object> earlyExit;
    private Function<Exception, Object> errorRecovery;

    @SuppressWarnings("unchecked")
    public <T> Pipeline5183<I, T> addStage(String name, Stage<?, ?> stage) {
        stageNames.add(name);
        stages.add((Stage<Object, Object>) stage);
        metrics.put(name, new StageMetrics(name));
        return (Pipeline5183<I, T>) this;
    }

    public Pipeline5183<I, O> withEarlyExit(Predicate<Object> condition) {
        this.earlyExit = condition;
        return this;
    }

    public Pipeline5183<I, O> withErrorRecovery(Function<Exception, Object> recovery) {
        this.errorRecovery = recovery;
        return this;
    }

    @SuppressWarnings("unchecked")
    public PipelineResult<O> execute(I input) {
        Object current = input;
        for (int i = 0; i < stages.size(); i++) {
            if (earlyExit != null && earlyExit.test(current)) {
                return PipelineResult.success((O) current);
            }
            String name = stageNames.get(i);
            StageMetrics m = metrics.get(name);
            long start = System.nanoTime();
            try {
                current = stages.get(i).process(current);
                m.recordSuccess(System.nanoTime() - start);
            } catch (Exception e) {
                m.recordFailure();
                if (errorRecovery != null) {
                    current = errorRecovery.apply(e);
                } else {
                    return PipelineResult.failure(e, name);
                }
            }
        }
        return PipelineResult.success((O) current);
    }

    public Map<String, StageMetrics> getMetrics() { return new LinkedHashMap<>(metrics); }
    public int getStageCount() { return stages.size(); }
}
