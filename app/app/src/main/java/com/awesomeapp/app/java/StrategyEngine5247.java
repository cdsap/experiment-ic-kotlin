package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StrategyEngine5247<T, R> {

    @FunctionalInterface
    public interface Strategy<T, R> {
        R apply(T input);
        default String getName() { return getClass().getSimpleName(); }
    }

    public static class WeightedStrategy<T, R> implements Strategy<T, R> {
        private final Strategy<T, R> delegate;
        private final double weight;
        private final String name;

        public WeightedStrategy(String name, Strategy<T, R> delegate, double weight) {
            this.name = name; this.delegate = delegate; this.weight = weight;
        }

        @Override public R apply(T input) { return delegate.apply(input); }
        @Override public String getName() { return name; }
        public double getWeight() { return weight; }
    }

    public static class StrategyResult<R> {
        private final String strategyName;
        private final R result;
        private final long durationNanos;
        private final boolean success;

        public StrategyResult(String name, R result, long duration, boolean success) {
            this.strategyName = name; this.result = result; this.durationNanos = duration; this.success = success;
        }

        public String getStrategyName() { return strategyName; }
        public R getResult() { return result; }
        public long getDurationNanos() { return durationNanos; }
        public boolean isSuccess() { return success; }
    }

    private final Map<String, Strategy<T, R>> registry = new HashMap<>();
    private String defaultStrategy;
    private BiFunction<List<StrategyResult<R>>, T, R> combiner;

    public StrategyEngine5247<T, R> register(String name, Strategy<T, R> strategy) {
        registry.put(name, strategy);
        if (defaultStrategy == null) defaultStrategy = name;
        return this;
    }

    public StrategyEngine5247<T, R> setDefault(String name) {
        if (!registry.containsKey(name)) throw new IllegalArgumentException("Strategy not found: " + name);
        this.defaultStrategy = name;
        return this;
    }

    public StrategyEngine5247<T, R> withCombiner(BiFunction<List<StrategyResult<R>>, T, R> combiner) {
        this.combiner = combiner;
        return this;
    }

    public Optional<R> execute(T input) {
        return execute(defaultStrategy, input);
    }

    public Optional<R> execute(String strategyName, T input) {
        Strategy<T, R> strategy = registry.get(strategyName);
        if (strategy == null) return Optional.empty();
        return Optional.ofNullable(strategy.apply(input));
    }

    public List<StrategyResult<R>> executeAll(T input) {
        return registry.entrySet().stream().map(entry -> {
            long start = System.nanoTime();
            try {
                R result = entry.getValue().apply(input);
                return new StrategyResult<>(entry.getKey(), result, System.nanoTime() - start, true);
            } catch (Exception e) {
                return new StrategyResult<R>(entry.getKey(), null, System.nanoTime() - start, false);
            }
        }).collect(Collectors.toList());
    }

    public Optional<R> executeBest(T input, Function<List<StrategyResult<R>>, StrategyResult<R>> selector) {
        List<StrategyResult<R>> results = executeAll(input);
        return Optional.ofNullable(selector.apply(results)).map(StrategyResult::getResult);
    }

    public Optional<R> executeCombined(T input) {
        if (combiner == null) return execute(input);
        List<StrategyResult<R>> results = executeAll(input);
        return Optional.ofNullable(combiner.apply(results, input));
    }

    public List<String> getRegisteredStrategies() { return new ArrayList<>(registry.keySet()); }
    public int getStrategyCount() { return registry.size(); }
}
