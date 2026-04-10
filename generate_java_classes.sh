#!/bin/bash

CART_DIR="/Users/inakivillar/reports/projects_generated/ic_kotlin_experiment/project_kts/core/cart/src/main/java/com/awesomeapp/cart/java"
APP_DIR="/Users/inakivillar/reports/projects_generated/ic_kotlin_experiment/project_kts/app/app/src/main/java/com/awesomeapp/app/java"

mkdir -p "$CART_DIR"
mkdir -p "$APP_DIR"

CART_START=2000
APP_START=5000

generate_class() {
    local dir="$1"
    local pkg="$2"
    local idx="$3"
    local type_idx=$((idx % 20))

    case $type_idx in
    0)
        # Generic repository with caching, eviction, and stats
        cat > "$dir/CachingRepository${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CachingRepository${idx}<K, V> {

    public enum EvictionPolicy { LRU, LFU, FIFO, RANDOM }

    public static class CacheEntry<V> {
        private final V value;
        private final long createdAt;
        private long lastAccessedAt;
        private final AtomicLong accessCount = new AtomicLong(0);

        public CacheEntry(V value) {
            this.value = value;
            this.createdAt = System.nanoTime();
            this.lastAccessedAt = this.createdAt;
        }

        public V getValue() { lastAccessedAt = System.nanoTime(); accessCount.incrementAndGet(); return value; }
        public long getCreatedAt() { return createdAt; }
        public long getLastAccessedAt() { return lastAccessedAt; }
        public long getAccessCount() { return accessCount.get(); }
    }

    public static class CacheStats {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final AtomicLong puts = new AtomicLong(0);

        public void recordHit() { hits.incrementAndGet(); }
        public void recordMiss() { misses.incrementAndGet(); }
        public void recordEviction() { evictions.incrementAndGet(); }
        public void recordPut() { puts.incrementAndGet(); }
        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getEvictions() { return evictions.get(); }
        public long getPuts() { return puts.get(); }
        public double getHitRate() {
            long total = hits.get() + misses.get();
            return total == 0 ? 0.0 : (double) hits.get() / total;
        }

        @Override
        public String toString() {
            return "CacheStats{hits=" + hits + ", misses=" + misses + ", evictions=" + evictions + ", hitRate=" + String.format("%.2f", getHitRate()) + "}";
        }
    }

    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final int maxSize;
    private final EvictionPolicy policy;
    private final CacheStats stats = new CacheStats();
    private Function<K, V> loader;

    public CachingRepository${idx}(int maxSize, EvictionPolicy policy) {
        this.maxSize = maxSize;
        this.policy = policy;
    }

    public CachingRepository${idx}<K, V> withLoader(Function<K, V> loader) {
        this.loader = loader;
        return this;
    }

    public Optional<V> get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry != null) {
            stats.recordHit();
            return Optional.of(entry.getValue());
        }
        stats.recordMiss();
        if (loader != null) {
            V loaded = loader.apply(key);
            if (loaded != null) {
                put(key, loaded);
                return Optional.of(loaded);
            }
        }
        return Optional.empty();
    }

    public void put(K key, V value) {
        if (store.size() >= maxSize && !store.containsKey(key)) {
            evict();
        }
        store.put(key, new CacheEntry<>(value));
        stats.recordPut();
    }

    private void evict() {
        if (store.isEmpty()) return;
        K keyToEvict = selectEvictionCandidate();
        if (keyToEvict != null) {
            store.remove(keyToEvict);
            stats.recordEviction();
        }
    }

    private K selectEvictionCandidate() {
        switch (policy) {
            case LRU:
                return store.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().getLastAccessedAt()))
                    .map(Map.Entry::getKey).orElse(null);
            case LFU:
                return store.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().getAccessCount()))
                    .map(Map.Entry::getKey).orElse(null);
            case FIFO:
                return store.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().getCreatedAt()))
                    .map(Map.Entry::getKey).orElse(null);
            default:
                return store.keySet().iterator().next();
        }
    }

    public List<V> findAll(Predicate<V> predicate) {
        return store.values().stream()
            .map(CacheEntry::getValue)
            .filter(predicate)
            .collect(Collectors.toList());
    }

    public void invalidate(K key) { store.remove(key); }
    public void invalidateAll() { store.clear(); }
    public int size() { return store.size(); }
    public CacheStats getStats() { return stats; }
    public boolean containsKey(K key) { return store.containsKey(key); }
}
JAVAEOF
        ;;
    1)
        # Event bus with typed events, subscribers, priority
        cat > "$dir/EventBus${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventBus${idx} {

    public interface Event {
        String getType();
        long getTimestamp();
    }

    public static class BaseEvent implements Event {
        private final String type;
        private final long timestamp;
        private final Map<String, Object> payload;

        public BaseEvent(String type, Map<String, Object> payload) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.payload = payload != null ? new ConcurrentHashMap<>(payload) : new ConcurrentHashMap<>();
        }

        @Override public String getType() { return type; }
        @Override public long getTimestamp() { return timestamp; }
        public Object get(String key) { return payload.get(key); }
        public <T> T get(String key, Class<T> clazz) { return clazz.cast(payload.get(key)); }
    }

    public static class Subscription<E extends Event> {
        private final Class<E> eventType;
        private final Consumer<E> handler;
        private final int priority;
        private final Predicate<E> filter;
        private final AtomicLong invocationCount = new AtomicLong(0);

        public Subscription(Class<E> eventType, Consumer<E> handler, int priority, Predicate<E> filter) {
            this.eventType = eventType;
            this.handler = handler;
            this.priority = priority;
            this.filter = filter;
        }

        public boolean matches(Event event) {
            return eventType.isInstance(event) && (filter == null || filter.test(eventType.cast(event)));
        }

        @SuppressWarnings("unchecked")
        public void invoke(Event event) {
            handler.accept((E) event);
            invocationCount.incrementAndGet();
        }

        public int getPriority() { return priority; }
        public long getInvocationCount() { return invocationCount.get(); }
    }

    private final List<Subscription<?>> subscriptions = new CopyOnWriteArrayList<>();
    private final List<Event> eventHistory = new CopyOnWriteArrayList<>();
    private final AtomicLong publishedCount = new AtomicLong(0);
    private boolean recordHistory = false;
    private int maxHistorySize = 1000;

    public EventBus${idx} withHistory(boolean record, int maxSize) {
        this.recordHistory = record;
        this.maxHistorySize = maxSize;
        return this;
    }

    public <E extends Event> Subscription<E> subscribe(Class<E> eventType, Consumer<E> handler) {
        return subscribe(eventType, handler, 0, null);
    }

    public <E extends Event> Subscription<E> subscribe(Class<E> eventType, Consumer<E> handler, int priority, Predicate<E> filter) {
        Subscription<E> sub = new Subscription<>(eventType, handler, priority, filter);
        subscriptions.add(sub);
        subscriptions.sort(Comparator.comparingInt(Subscription::getPriority).reversed());
        return sub;
    }

    public void unsubscribe(Subscription<?> subscription) {
        subscriptions.remove(subscription);
    }

    public void publish(Event event) {
        publishedCount.incrementAndGet();
        if (recordHistory) {
            eventHistory.add(event);
            while (eventHistory.size() > maxHistorySize) {
                eventHistory.remove(0);
            }
        }
        for (Subscription<?> sub : subscriptions) {
            if (sub.matches(event)) {
                sub.invoke(event);
            }
        }
    }

    public List<Event> getHistory() { return new ArrayList<>(eventHistory); }
    public long getPublishedCount() { return publishedCount.get(); }
    public int getSubscriptionCount() { return subscriptions.size(); }
}
JAVAEOF
        ;;
    2)
        # State machine with transitions, guards, actions
        cat > "$dir/StateMachine${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class StateMachine${idx}<S, E> {

    public static class Transition<S, E> {
        private final S from;
        private final E event;
        private final S to;
        private final Predicate<E> guard;
        private final Consumer<E> action;

        public Transition(S from, E event, S to, Predicate<E> guard, Consumer<E> action) {
            this.from = from;
            this.event = event;
            this.to = to;
            this.guard = guard;
            this.action = action;
        }

        public S getFrom() { return from; }
        public E getEvent() { return event; }
        public S getTo() { return to; }
        public boolean isAllowed(E evt) { return guard == null || guard.test(evt); }
        public void execute(E evt) { if (action != null) action.accept(evt); }
    }

    public static class TransitionResult<S> {
        private final boolean success;
        private final S previousState;
        private final S currentState;
        private final String errorMessage;

        private TransitionResult(boolean success, S prev, S curr, String error) {
            this.success = success; this.previousState = prev; this.currentState = curr; this.errorMessage = error;
        }

        public static <S> TransitionResult<S> success(S prev, S curr) { return new TransitionResult<>(true, prev, curr, null); }
        public static <S> TransitionResult<S> failure(S current, String error) { return new TransitionResult<>(false, current, current, error); }

        public boolean isSuccess() { return success; }
        public S getPreviousState() { return previousState; }
        public S getCurrentState() { return currentState; }
        public Optional<String> getError() { return Optional.ofNullable(errorMessage); }
    }

    private S currentState;
    private final Map<S, Map<E, List<Transition<S, E>>>> transitionTable = new HashMap<>();
    private final List<Consumer<TransitionResult<S>>> listeners = new ArrayList<>();
    private final List<TransitionResult<S>> history = new ArrayList<>();
    private boolean recordHistory = true;

    public StateMachine${idx}(S initialState) {
        this.currentState = initialState;
    }

    public StateMachine${idx}<S, E> addTransition(S from, E event, S to) {
        return addTransition(from, event, to, null, null);
    }

    public StateMachine${idx}<S, E> addTransition(S from, E event, S to, Predicate<E> guard, Consumer<E> action) {
        transitionTable
            .computeIfAbsent(from, k -> new HashMap<>())
            .computeIfAbsent(event, k -> new ArrayList<>())
            .add(new Transition<>(from, event, to, guard, action));
        return this;
    }

    public StateMachine${idx}<S, E> onTransition(Consumer<TransitionResult<S>> listener) {
        listeners.add(listener);
        return this;
    }

    public TransitionResult<S> fire(E event) {
        Map<E, List<Transition<S, E>>> stateTransitions = transitionTable.get(currentState);
        if (stateTransitions == null) {
            return fail("No transitions defined for state: " + currentState);
        }
        List<Transition<S, E>> candidates = stateTransitions.get(event);
        if (candidates == null || candidates.isEmpty()) {
            return fail("No transition for event " + event + " in state " + currentState);
        }
        for (Transition<S, E> t : candidates) {
            if (t.isAllowed(event)) {
                S prev = currentState;
                t.execute(event);
                currentState = t.getTo();
                TransitionResult<S> result = TransitionResult.success(prev, currentState);
                if (recordHistory) history.add(result);
                listeners.forEach(l -> l.accept(result));
                return result;
            }
        }
        return fail("All guards failed for event " + event + " in state " + currentState);
    }

    private TransitionResult<S> fail(String msg) {
        TransitionResult<S> result = TransitionResult.failure(currentState, msg);
        if (recordHistory) history.add(result);
        return result;
    }

    public S getCurrentState() { return currentState; }
    public List<TransitionResult<S>> getHistory() { return new ArrayList<>(history); }
    public boolean isInState(S state) { return currentState.equals(state); }
}
JAVAEOF
        ;;
    3)
        # Pipeline with typed stages, error handling, metrics
        cat > "$dir/Pipeline${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class Pipeline${idx}<I, O> {

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
    public <T> Pipeline${idx}<I, T> addStage(String name, Stage<?, ?> stage) {
        stageNames.add(name);
        stages.add((Stage<Object, Object>) stage);
        metrics.put(name, new StageMetrics(name));
        return (Pipeline${idx}<I, T>) this;
    }

    public Pipeline${idx}<I, O> withEarlyExit(Predicate<Object> condition) {
        this.earlyExit = condition;
        return this;
    }

    public Pipeline${idx}<I, O> withErrorRecovery(Function<Exception, Object> recovery) {
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
JAVAEOF
        ;;
    4)
        # Builder pattern with validation and immutable result
        cat > "$dir/ConfigBuilder${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConfigBuilder${idx} {

    public enum LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }
    public enum Environment { DEV, STAGING, PRODUCTION }

    public static final class Config {
        private final String name;
        private final Environment environment;
        private final LogLevel logLevel;
        private final int maxRetries;
        private final long timeoutMs;
        private final boolean enableMetrics;
        private final Map<String, String> properties;
        private final List<String> features;

        private Config(Builder builder) {
            this.name = builder.name;
            this.environment = builder.environment;
            this.logLevel = builder.logLevel;
            this.maxRetries = builder.maxRetries;
            this.timeoutMs = builder.timeoutMs;
            this.enableMetrics = builder.enableMetrics;
            this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
            this.features = Collections.unmodifiableList(new ArrayList<>(builder.features));
        }

        public String getName() { return name; }
        public Environment getEnvironment() { return environment; }
        public LogLevel getLogLevel() { return logLevel; }
        public int getMaxRetries() { return maxRetries; }
        public long getTimeoutMs() { return timeoutMs; }
        public boolean isMetricsEnabled() { return enableMetrics; }
        public Map<String, String> getProperties() { return properties; }
        public List<String> getFeatures() { return features; }
        public Optional<String> getProperty(String key) { return Optional.ofNullable(properties.get(key)); }
        public boolean hasFeature(String feature) { return features.contains(feature); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Config)) return false;
            Config c = (Config) o;
            return maxRetries == c.maxRetries && timeoutMs == c.timeoutMs && enableMetrics == c.enableMetrics
                && Objects.equals(name, c.name) && environment == c.environment && logLevel == c.logLevel;
        }

        @Override
        public int hashCode() { return Objects.hash(name, environment, logLevel, maxRetries, timeoutMs, enableMetrics); }

        @Override
        public String toString() {
            return "Config{name='" + name + "', env=" + environment + ", log=" + logLevel +
                ", retries=" + maxRetries + ", timeout=" + timeoutMs + "ms, features=" + features.size() + "}";
        }
    }

    public static class Builder {
        private String name;
        private Environment environment = Environment.DEV;
        private LogLevel logLevel = LogLevel.INFO;
        private int maxRetries = 3;
        private long timeoutMs = 30000;
        private boolean enableMetrics = false;
        private final Map<String, String> properties = new HashMap<>();
        private final List<String> features = new ArrayList<>();

        public Builder(String name) { this.name = Objects.requireNonNull(name, "name must not be null"); }
        public Builder environment(Environment env) { this.environment = env; return this; }
        public Builder logLevel(LogLevel level) { this.logLevel = level; return this; }
        public Builder maxRetries(int retries) { this.maxRetries = retries; return this; }
        public Builder timeoutMs(long timeout) { this.timeoutMs = timeout; return this; }
        public Builder enableMetrics(boolean enable) { this.enableMetrics = enable; return this; }
        public Builder property(String key, String value) { properties.put(key, value); return this; }
        public Builder feature(String feature) { features.add(feature); return this; }

        public Config build() {
            validate();
            return new Config(this);
        }

        private void validate() {
            if (name.isEmpty()) throw new IllegalStateException("Name cannot be empty");
            if (maxRetries < 0) throw new IllegalStateException("Max retries must be >= 0");
            if (timeoutMs <= 0) throw new IllegalStateException("Timeout must be > 0");
            if (environment == Environment.PRODUCTION && logLevel == LogLevel.TRACE) {
                throw new IllegalStateException("TRACE logging not allowed in PRODUCTION");
            }
        }
    }

    public static Builder builder(String name) { return new Builder(name); }
}
JAVAEOF
        ;;
    5)
        # Observer pattern with typed notifications and filtering
        cat > "$dir/ObservableStore${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ObservableStore${idx}<K, V> {

    public enum ChangeType { ADDED, UPDATED, REMOVED, CLEARED }

    public static class ChangeEvent<K, V> {
        private final ChangeType type;
        private final K key;
        private final V oldValue;
        private final V newValue;
        private final long timestamp;

        public ChangeEvent(ChangeType type, K key, V oldValue, V newValue) {
            this.type = type; this.key = key; this.oldValue = oldValue; this.newValue = newValue;
            this.timestamp = System.currentTimeMillis();
        }

        public ChangeType getType() { return type; }
        public K getKey() { return key; }
        public Optional<V> getOldValue() { return Optional.ofNullable(oldValue); }
        public Optional<V> getNewValue() { return Optional.ofNullable(newValue); }
        public long getTimestamp() { return timestamp; }
    }

    @FunctionalInterface
    public interface StoreObserver<K, V> {
        void onChanged(ChangeEvent<K, V> event);
    }

    private final Map<K, V> store = new HashMap<>();
    private final List<StoreObserver<K, V>> observers = new CopyOnWriteArrayList<>();
    private final Map<StoreObserver<K, V>, Predicate<ChangeEvent<K, V>>> filters = new HashMap<>();
    private final AtomicLong version = new AtomicLong(0);
    private final List<ChangeEvent<K, V>> changelog = new ArrayList<>();
    private boolean trackChanges = false;

    public ObservableStore${idx}<K, V> withChangeTracking(boolean track) {
        this.trackChanges = track;
        return this;
    }

    public void addObserver(StoreObserver<K, V> observer) {
        observers.add(observer);
    }

    public void addObserver(StoreObserver<K, V> observer, Predicate<ChangeEvent<K, V>> filter) {
        observers.add(observer);
        filters.put(observer, filter);
    }

    public void removeObserver(StoreObserver<K, V> observer) {
        observers.remove(observer);
        filters.remove(observer);
    }

    public V put(K key, V value) {
        V old = store.put(key, value);
        ChangeType type = old == null ? ChangeType.ADDED : ChangeType.UPDATED;
        notifyAll(new ChangeEvent<>(type, key, old, value));
        version.incrementAndGet();
        return old;
    }

    public Optional<V> get(K key) { return Optional.ofNullable(store.get(key)); }

    public V remove(K key) {
        V old = store.remove(key);
        if (old != null) {
            notifyAll(new ChangeEvent<>(ChangeType.REMOVED, key, old, null));
            version.incrementAndGet();
        }
        return old;
    }

    public void clear() {
        store.clear();
        notifyAll(new ChangeEvent<>(ChangeType.CLEARED, null, null, null));
        version.incrementAndGet();
    }

    private void notifyAll(ChangeEvent<K, V> event) {
        if (trackChanges) changelog.add(event);
        for (StoreObserver<K, V> obs : observers) {
            Predicate<ChangeEvent<K, V>> filter = filters.get(obs);
            if (filter == null || filter.test(event)) {
                obs.onChanged(event);
            }
        }
    }

    public Map<K, V> snapshot() { return Collections.unmodifiableMap(new HashMap<>(store)); }
    public int size() { return store.size(); }
    public long getVersion() { return version.get(); }
    public List<ChangeEvent<K, V>> getChangelog() { return Collections.unmodifiableList(changelog); }

    public List<V> query(Predicate<V> predicate) {
        return store.values().stream().filter(predicate).collect(Collectors.toList());
    }

    public void forEach(BiConsumer<K, V> action) { store.forEach(action); }
}
JAVAEOF
        ;;
    6)
        # Command pattern with undo/redo stack
        cat > "$dir/CommandExecutor${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class CommandExecutor${idx} {

    public interface Command {
        String getName();
        void execute() throws Exception;
        void undo() throws Exception;
        boolean isReversible();
    }

    public static abstract class AbstractCommand implements Command {
        private final String name;
        protected AbstractCommand(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public boolean isReversible() { return true; }
    }

    public static class CompositeCommand implements Command {
        private final String name;
        private final List<Command> commands;
        private int executedCount = 0;

        public CompositeCommand(String name, List<Command> commands) {
            this.name = name;
            this.commands = new ArrayList<>(commands);
        }

        @Override public String getName() { return name; }
        @Override public boolean isReversible() { return commands.stream().allMatch(Command::isReversible); }

        @Override
        public void execute() throws Exception {
            for (Command cmd : commands) {
                cmd.execute();
                executedCount++;
            }
        }

        @Override
        public void undo() throws Exception {
            for (int i = executedCount - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
            executedCount = 0;
        }
    }

    public static class ExecutionRecord {
        private final Command command;
        private final long executedAt;
        private final boolean success;
        private final String error;

        public ExecutionRecord(Command cmd, boolean success, String error) {
            this.command = cmd; this.executedAt = System.currentTimeMillis();
            this.success = success; this.error = error;
        }

        public Command getCommand() { return command; }
        public long getExecutedAt() { return executedAt; }
        public boolean isSuccess() { return success; }
        public Optional<String> getError() { return Optional.ofNullable(error); }
    }

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final List<ExecutionRecord> history = new ArrayList<>();
    private final AtomicLong executionCount = new AtomicLong(0);
    private int maxUndoDepth = 100;

    public CommandExecutor${idx} withMaxUndoDepth(int depth) {
        this.maxUndoDepth = depth;
        return this;
    }

    public boolean execute(Command command) {
        try {
            command.execute();
            history.add(new ExecutionRecord(command, true, null));
            if (command.isReversible()) {
                undoStack.push(command);
                if (undoStack.size() > maxUndoDepth) {
                    undoStack.removeLast();
                }
            }
            redoStack.clear();
            executionCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            history.add(new ExecutionRecord(command, false, e.getMessage()));
            return false;
        }
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command cmd = undoStack.pop();
        try {
            cmd.undo();
            redoStack.push(cmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command cmd = redoStack.pop();
        try {
            cmd.execute();
            undoStack.push(cmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public List<ExecutionRecord> getHistory() { return new ArrayList<>(history); }
    public long getExecutionCount() { return executionCount.get(); }
}
JAVAEOF
        ;;
    7)
        # Strategy pattern with registry and composite strategies
        cat > "$dir/StrategyEngine${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StrategyEngine${idx}<T, R> {

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

    public StrategyEngine${idx}<T, R> register(String name, Strategy<T, R> strategy) {
        registry.put(name, strategy);
        if (defaultStrategy == null) defaultStrategy = name;
        return this;
    }

    public StrategyEngine${idx}<T, R> setDefault(String name) {
        if (!registry.containsKey(name)) throw new IllegalArgumentException("Strategy not found: " + name);
        this.defaultStrategy = name;
        return this;
    }

    public StrategyEngine${idx}<T, R> withCombiner(BiFunction<List<StrategyResult<R>>, T, R> combiner) {
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
JAVAEOF
        ;;
    8)
        # Tree structure with traversal algorithms
        cat > "$dir/TreeNode${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TreeNode${idx}<T> {

    public enum TraversalOrder { PRE_ORDER, POST_ORDER, BREADTH_FIRST }

    private T data;
    private TreeNode${idx}<T> parent;
    private final List<TreeNode${idx}<T>> children = new ArrayList<>();
    private final Map<String, Object> metadata = new java.util.HashMap<>();

    public TreeNode${idx}(T data) { this.data = data; }

    public TreeNode${idx}<T> addChild(T childData) {
        TreeNode${idx}<T> child = new TreeNode${idx}<>(childData);
        child.parent = this;
        children.add(child);
        return child;
    }

    public void removeChild(TreeNode${idx}<T> child) {
        children.remove(child);
        child.parent = null;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Optional<TreeNode${idx}<T>> getParent() { return Optional.ofNullable(parent); }
    public List<TreeNode${idx}<T>> getChildren() { return Collections.unmodifiableList(children); }
    public boolean isLeaf() { return children.isEmpty(); }
    public boolean isRoot() { return parent == null; }

    public int getDepth() {
        int depth = 0;
        TreeNode${idx}<T> current = this;
        while (current.parent != null) { depth++; current = current.parent; }
        return depth;
    }

    public int getHeight() {
        if (isLeaf()) return 0;
        return 1 + children.stream().mapToInt(TreeNode${idx}::getHeight).max().orElse(0);
    }

    public int size() {
        return 1 + children.stream().mapToInt(TreeNode${idx}::size).sum();
    }

    public void traverse(TraversalOrder order, Consumer<T> visitor) {
        switch (order) {
            case PRE_ORDER: preOrder(visitor); break;
            case POST_ORDER: postOrder(visitor); break;
            case BREADTH_FIRST: breadthFirst(visitor); break;
        }
    }

    private void preOrder(Consumer<T> visitor) {
        visitor.accept(data);
        children.forEach(c -> c.preOrder(visitor));
    }

    private void postOrder(Consumer<T> visitor) {
        children.forEach(c -> c.postOrder(visitor));
        visitor.accept(data);
    }

    private void breadthFirst(Consumer<T> visitor) {
        Queue<TreeNode${idx}<T>> queue = new ArrayDeque<>();
        queue.offer(this);
        while (!queue.isEmpty()) {
            TreeNode${idx}<T> node = queue.poll();
            visitor.accept(node.data);
            queue.addAll(node.children);
        }
    }

    public Optional<TreeNode${idx}<T>> find(Predicate<T> predicate) {
        if (predicate.test(data)) return Optional.of(this);
        for (TreeNode${idx}<T> child : children) {
            Optional<TreeNode${idx}<T>> found = child.find(predicate);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    public <R> TreeNode${idx}<R> map(Function<T, R> mapper) {
        TreeNode${idx}<R> mapped = new TreeNode${idx}<>(mapper.apply(data));
        for (TreeNode${idx}<T> child : children) {
            TreeNode${idx}<R> mappedChild = child.map(mapper);
            mappedChild.parent = mapped;
            mapped.children.add(mappedChild);
        }
        return mapped;
    }

    public List<T> toList(TraversalOrder order) {
        List<T> result = new ArrayList<>();
        traverse(order, result::add);
        return result;
    }

    public List<T> getPath() {
        List<T> path = new ArrayList<>();
        TreeNode${idx}<T> current = this;
        while (current != null) { path.add(current.data); current = current.parent; }
        Collections.reverse(path);
        return path;
    }
}
JAVAEOF
        ;;
    9)
        # Retry mechanism with backoff strategies
        cat > "$dir/RetryPolicy${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class RetryPolicy${idx}<T> {

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

    public RetryPolicy${idx}<T> maxAttempts(int max) { this.maxAttempts = max; return this; }
    public RetryPolicy${idx}<T> baseDelay(long ms) { this.baseDelayMs = ms; return this; }
    public RetryPolicy${idx}<T> maxDelay(long ms) { this.maxDelayMs = ms; return this; }
    public RetryPolicy${idx}<T> multiplier(double m) { this.multiplier = m; return this; }
    public RetryPolicy${idx}<T> backoffStrategy(BackoffStrategy s) { this.backoff = s; return this; }
    public RetryPolicy${idx}<T> retryOn(Predicate<Exception> predicate) { this.retryableException = predicate; return this; }
    public RetryPolicy${idx}<T> retryIf(Predicate<T> predicate) { this.retryableResult = predicate; return this; }

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
JAVAEOF
        ;;
    10)
        # Graph with BFS/DFS
        cat > "$dir/Graph${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Graph${idx}<V> {

    public static class Edge<V> {
        private final V source;
        private final V target;
        private final double weight;

        public Edge(V source, V target, double weight) {
            this.source = source; this.target = target; this.weight = weight;
        }

        public V getSource() { return source; }
        public V getTarget() { return target; }
        public double getWeight() { return weight; }
    }

    private final Map<V, Set<Edge<V>>> adjacencyList = new HashMap<>();
    private final boolean directed;

    public Graph${idx}(boolean directed) { this.directed = directed; }

    public void addVertex(V vertex) {
        adjacencyList.putIfAbsent(vertex, new LinkedHashSet<>());
    }

    public void addEdge(V source, V target, double weight) {
        addVertex(source);
        addVertex(target);
        adjacencyList.get(source).add(new Edge<>(source, target, weight));
        if (!directed) {
            adjacencyList.get(target).add(new Edge<>(target, source, weight));
        }
    }

    public void addEdge(V source, V target) { addEdge(source, target, 1.0); }

    public Set<V> getVertices() { return Collections.unmodifiableSet(adjacencyList.keySet()); }

    public Set<Edge<V>> getEdges(V vertex) {
        return adjacencyList.getOrDefault(vertex, Collections.emptySet());
    }

    public List<V> getNeighbors(V vertex) {
        return getEdges(vertex).stream().map(Edge::getTarget).collect(Collectors.toList());
    }

    public void bfs(V start, Consumer<V> visitor) {
        Set<V> visited = new HashSet<>();
        Queue<V> queue = new ArrayDeque<>();
        visited.add(start);
        queue.offer(start);
        while (!queue.isEmpty()) {
            V current = queue.poll();
            visitor.accept(current);
            for (Edge<V> edge : getEdges(current)) {
                if (visited.add(edge.getTarget())) {
                    queue.offer(edge.getTarget());
                }
            }
        }
    }

    public void dfs(V start, Consumer<V> visitor) {
        Set<V> visited = new HashSet<>();
        dfsRecursive(start, visited, visitor);
    }

    private void dfsRecursive(V vertex, Set<V> visited, Consumer<V> visitor) {
        visited.add(vertex);
        visitor.accept(vertex);
        for (Edge<V> edge : getEdges(vertex)) {
            if (!visited.contains(edge.getTarget())) {
                dfsRecursive(edge.getTarget(), visited, visitor);
            }
        }
    }

    public Optional<List<V>> findPath(V from, V to) {
        Map<V, V> parentMap = new HashMap<>();
        Set<V> visited = new HashSet<>();
        Queue<V> queue = new ArrayDeque<>();
        visited.add(from);
        queue.offer(from);
        while (!queue.isEmpty()) {
            V current = queue.poll();
            if (current.equals(to)) {
                return Optional.of(reconstructPath(parentMap, from, to));
            }
            for (Edge<V> edge : getEdges(current)) {
                if (visited.add(edge.getTarget())) {
                    parentMap.put(edge.getTarget(), current);
                    queue.offer(edge.getTarget());
                }
            }
        }
        return Optional.empty();
    }

    private List<V> reconstructPath(Map<V, V> parentMap, V from, V to) {
        List<V> path = new ArrayList<>();
        V current = to;
        while (!current.equals(from)) { path.add(current); current = parentMap.get(current); }
        path.add(from);
        Collections.reverse(path);
        return path;
    }

    public boolean hasCycle() {
        Set<V> visited = new HashSet<>();
        Set<V> inStack = new HashSet<>();
        for (V vertex : adjacencyList.keySet()) {
            if (!visited.contains(vertex) && hasCycleDfs(vertex, visited, inStack)) return true;
        }
        return false;
    }

    private boolean hasCycleDfs(V vertex, Set<V> visited, Set<V> inStack) {
        visited.add(vertex);
        inStack.add(vertex);
        for (Edge<V> edge : getEdges(vertex)) {
            if (inStack.contains(edge.getTarget())) return true;
            if (!visited.contains(edge.getTarget()) && hasCycleDfs(edge.getTarget(), visited, inStack)) return true;
        }
        inStack.remove(vertex);
        return false;
    }

    public int getVertexCount() { return adjacencyList.size(); }
    public int getEdgeCount() {
        int count = adjacencyList.values().stream().mapToInt(Set::size).sum();
        return directed ? count : count / 2;
    }
    public int getDegree(V vertex) { return getEdges(vertex).size(); }
}
JAVAEOF
        ;;
    11)
        # Rate limiter with sliding window
        cat > "$dir/RateLimiter${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter${idx} {

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

    public RateLimiter${idx}(LimitStrategy strategy, int maxRequests, long windowMs) {
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
JAVAEOF
        ;;
    12)
        # Circuit breaker
        cat > "$dir/CircuitBreaker${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CircuitBreaker${idx}<T> {

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

    public CircuitBreaker${idx}(CircuitBreakerConfig config) {
        this.config = config;
    }

    public CircuitBreaker${idx}<T> onStateChange(Consumer<State> listener) {
        stateListeners.add(listener);
        return this;
    }

    public CircuitBreaker${idx}<T> recordExceptions(Predicate<Exception> predicate) {
        this.recordableException = predicate;
        return this;
    }

    public CircuitBreaker${idx}<T> withFallback(Callable<T> fallback) {
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
JAVAEOF
        ;;
    13)
        # Object pool with lifecycle management
        cat > "$dir/ObjectPool${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectPool${idx}<T> {

    public enum PoolState { ACTIVE, DRAINING, CLOSED }

    public static class PoolStats {
        private final int totalCreated;
        private final int currentIdle;
        private final int currentActive;
        private final long totalBorrows;
        private final long totalReturns;

        public PoolStats(int created, int idle, int active, long borrows, long returns) {
            this.totalCreated = created; this.currentIdle = idle; this.currentActive = active;
            this.totalBorrows = borrows; this.totalReturns = returns;
        }

        public int getTotalCreated() { return totalCreated; }
        public int getCurrentIdle() { return currentIdle; }
        public int getCurrentActive() { return currentActive; }
        public long getTotalBorrows() { return totalBorrows; }
        public long getTotalReturns() { return totalReturns; }
        public double getUtilization() {
            int total = currentIdle + currentActive;
            return total == 0 ? 0 : (double) currentActive / total;
        }
    }

    private final Supplier<T> factory;
    private final Consumer<T> destroyer;
    private final Consumer<T> validator;
    private final Queue<T> idlePool = new ConcurrentLinkedQueue<>();
    private final Set<T> activeSet = ConcurrentHashMap.newKeySet();
    private final Semaphore semaphore;
    private final int minIdle;
    private final int maxSize;
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicLong totalBorrows = new AtomicLong(0);
    private final AtomicLong totalReturns = new AtomicLong(0);
    private volatile PoolState state = PoolState.ACTIVE;

    public ObjectPool${idx}(Supplier<T> factory, Consumer<T> destroyer, Consumer<T> validator, int minIdle, int maxSize) {
        this.factory = factory; this.destroyer = destroyer; this.validator = validator;
        this.minIdle = minIdle; this.maxSize = maxSize;
        this.semaphore = new Semaphore(maxSize);
        initializePool();
    }

    private void initializePool() {
        for (int i = 0; i < minIdle; i++) {
            T obj = createObject();
            if (obj != null) idlePool.offer(obj);
        }
    }

    private T createObject() {
        T obj = factory.get();
        totalCreated.incrementAndGet();
        return obj;
    }

    public T borrow(long timeoutMs) throws InterruptedException {
        if (state != PoolState.ACTIVE) throw new IllegalStateException("Pool is not active");
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Timeout waiting for pool object");
        }
        T obj = idlePool.poll();
        if (obj == null) {
            obj = createObject();
        } else {
            try {
                if (validator != null) validator.accept(obj);
            } catch (Exception e) {
                obj = createObject();
            }
        }
        activeSet.add(obj);
        totalBorrows.incrementAndGet();
        return obj;
    }

    public void release(T obj) {
        if (!activeSet.remove(obj)) return;
        totalReturns.incrementAndGet();
        if (state == PoolState.ACTIVE) {
            idlePool.offer(obj);
        } else {
            if (destroyer != null) destroyer.accept(obj);
        }
        semaphore.release();
    }

    public void evict() {
        int currentIdle = idlePool.size();
        int toRemove = currentIdle - minIdle;
        for (int i = 0; i < toRemove; i++) {
            T obj = idlePool.poll();
            if (obj != null && destroyer != null) destroyer.accept(obj);
        }
    }

    public void close() {
        state = PoolState.DRAINING;
        T obj;
        while ((obj = idlePool.poll()) != null) {
            if (destroyer != null) destroyer.accept(obj);
        }
        state = PoolState.CLOSED;
    }

    public PoolStats getStats() {
        return new PoolStats(totalCreated.get(), idlePool.size(), activeSet.size(), totalBorrows.get(), totalReturns.get());
    }

    public PoolState getState() { return state; }
}
JAVAEOF
        ;;
    14)
        # Specification pattern
        cat > "$dir/Specification${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Specification${idx}<T> {

    public abstract boolean isSatisfiedBy(T candidate);
    public abstract String describe();

    public Specification${idx}<T> and(Specification${idx}<T> other) {
        return new AndSpecification<>(this, other);
    }

    public Specification${idx}<T> or(Specification${idx}<T> other) {
        return new OrSpecification<>(this, other);
    }

    public Specification${idx}<T> not() {
        return new NotSpecification<>(this);
    }

    public List<T> filter(Collection<T> candidates) {
        return candidates.stream().filter(this::isSatisfiedBy).collect(Collectors.toList());
    }

    public long count(Collection<T> candidates) {
        return candidates.stream().filter(this::isSatisfiedBy).count();
    }

    public boolean anyMatch(Collection<T> candidates) {
        return candidates.stream().anyMatch(this::isSatisfiedBy);
    }

    public boolean allMatch(Collection<T> candidates) {
        return candidates.stream().allMatch(this::isSatisfiedBy);
    }

    public static <T> Specification${idx}<T> of(String description, Predicate<T> predicate) {
        return new PredicateSpecification<>(description, predicate);
    }

    public static <T, V extends Comparable<V>> Specification${idx}<T> greaterThan(String field, Function<T, V> extractor, V value) {
        return of(field + " > " + value, t -> extractor.apply(t).compareTo(value) > 0);
    }

    public static <T, V extends Comparable<V>> Specification${idx}<T> lessThan(String field, Function<T, V> extractor, V value) {
        return of(field + " < " + value, t -> extractor.apply(t).compareTo(value) < 0);
    }

    public static <T> Specification${idx}<T> equalTo(String field, Function<T, ?> extractor, Object value) {
        return of(field + " == " + value, t -> Objects.equals(extractor.apply(t), value));
    }

    public static <T> Specification${idx}<T> in(String field, Function<T, ?> extractor, Collection<?> values) {
        return of(field + " in " + values, t -> values.contains(extractor.apply(t)));
    }

    private static class PredicateSpecification<T> extends Specification${idx}<T> {
        private final String description;
        private final Predicate<T> predicate;

        PredicateSpecification(String desc, Predicate<T> pred) { this.description = desc; this.predicate = pred; }
        @Override public boolean isSatisfiedBy(T candidate) { return predicate.test(candidate); }
        @Override public String describe() { return description; }
    }

    private static class AndSpecification<T> extends Specification${idx}<T> {
        private final Specification${idx}<T> left, right;
        AndSpecification(Specification${idx}<T> left, Specification${idx}<T> right) { this.left = left; this.right = right; }
        @Override public boolean isSatisfiedBy(T candidate) { return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate); }
        @Override public String describe() { return "(" + left.describe() + " AND " + right.describe() + ")"; }
    }

    private static class OrSpecification<T> extends Specification${idx}<T> {
        private final Specification${idx}<T> left, right;
        OrSpecification(Specification${idx}<T> left, Specification${idx}<T> right) { this.left = left; this.right = right; }
        @Override public boolean isSatisfiedBy(T candidate) { return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate); }
        @Override public String describe() { return "(" + left.describe() + " OR " + right.describe() + ")"; }
    }

    private static class NotSpecification<T> extends Specification${idx}<T> {
        private final Specification${idx}<T> spec;
        NotSpecification(Specification${idx}<T> spec) { this.spec = spec; }
        @Override public boolean isSatisfiedBy(T candidate) { return !spec.isSatisfiedBy(candidate); }
        @Override public String describe() { return "NOT(" + spec.describe() + ")"; }
    }
}
JAVAEOF
        ;;
    15)
        # Mediator pattern
        cat > "$dir/Mediator${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Mediator${idx} {

    public interface Request<R> {
        Class<R> getResponseType();
    }

    public interface RequestHandler<T extends Request<R>, R> {
        R handle(T request);
    }

    public interface Notification {
        String getChannel();
    }

    public interface NotificationHandler<T extends Notification> {
        void handle(T notification);
    }

    public static class MediatorStats {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong notificationCount = new AtomicLong(0);
        private final Map<String, AtomicLong> requestTypeCounts = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> channelCounts = new ConcurrentHashMap<>();

        public void recordRequest(String type) {
            requestCount.incrementAndGet();
            requestTypeCounts.computeIfAbsent(type, k -> new AtomicLong()).incrementAndGet();
        }

        public void recordNotification(String channel) {
            notificationCount.incrementAndGet();
            channelCounts.computeIfAbsent(channel, k -> new AtomicLong()).incrementAndGet();
        }

        public long getRequestCount() { return requestCount.get(); }
        public long getNotificationCount() { return notificationCount.get(); }
        public Map<String, AtomicLong> getRequestTypeCounts() { return new HashMap<>(requestTypeCounts); }
        public Map<String, AtomicLong> getChannelCounts() { return new HashMap<>(channelCounts); }
    }

    @SuppressWarnings("rawtypes")
    private final Map<Class, RequestHandler> requestHandlers = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private final Map<String, List<NotificationHandler>> notificationHandlers = new HashMap<>();
    private final List<Function<Object, Object>> requestMiddleware = new ArrayList<>();
    private final MediatorStats stats = new MediatorStats();

    @SuppressWarnings("unchecked")
    public <T extends Request<R>, R> void registerHandler(Class<T> requestType, RequestHandler<T, R> handler) {
        requestHandlers.put(requestType, handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends Notification> void subscribe(String channel, NotificationHandler<T> handler) {
        notificationHandlers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void addMiddleware(Function<Object, Object> middleware) {
        requestMiddleware.add(middleware);
    }

    @SuppressWarnings("unchecked")
    public <T extends Request<R>, R> R send(T request) {
        stats.recordRequest(request.getClass().getSimpleName());
        Object processed = request;
        for (Function<Object, Object> mw : requestMiddleware) {
            processed = mw.apply(processed);
        }
        RequestHandler<T, R> handler = requestHandlers.get(request.getClass());
        if (handler == null) throw new IllegalStateException("No handler for: " + request.getClass().getSimpleName());
        return handler.handle((T) processed);
    }

    @SuppressWarnings("unchecked")
    public <T extends Notification> void publish(T notification) {
        stats.recordNotification(notification.getChannel());
        List<NotificationHandler> handlers = notificationHandlers.get(notification.getChannel());
        if (handlers != null) {
            for (NotificationHandler handler : handlers) {
                handler.handle(notification);
            }
        }
    }

    public MediatorStats getStats() { return stats; }
    public int getRegisteredHandlers() { return requestHandlers.size(); }
    public int getSubscriptionCount() { return notificationHandlers.values().stream().mapToInt(List::size).sum(); }
}
JAVAEOF
        ;;
    16)
        # Fluent query builder
        cat > "$dir/QueryBuilder${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class QueryBuilder${idx} {

    public enum JoinType { INNER, LEFT, RIGHT, FULL, CROSS }
    public enum SortDirection { ASC, DESC }
    public enum AggregateFunction { COUNT, SUM, AVG, MIN, MAX }

    public static class Column {
        private final String table;
        private final String name;
        private final String alias;

        public Column(String table, String name, String alias) {
            this.table = table; this.name = name; this.alias = alias;
        }

        public String toSql() {
            String col = table != null ? table + "." + name : name;
            return alias != null ? col + " AS " + alias : col;
        }
    }

    public static class Condition {
        private final String expression;
        private final List<Object> params;

        public Condition(String expression, Object... params) {
            this.expression = expression;
            this.params = params != null ? Arrays.asList(params) : Collections.emptyList();
        }

        public String getExpression() { return expression; }
        public List<Object> getParams() { return params; }
    }

    private final List<Column> selectColumns = new ArrayList<>();
    private String fromTable;
    private String fromAlias;
    private final List<String> joins = new ArrayList<>();
    private final List<Condition> whereConditions = new ArrayList<>();
    private final List<String> groupByColumns = new ArrayList<>();
    private final List<Condition> havingConditions = new ArrayList<>();
    private final List<String> orderByColumns = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean distinct = false;

    public QueryBuilder${idx} select(String... columns) {
        for (String c : columns) selectColumns.add(new Column(null, c, null));
        return this;
    }

    public QueryBuilder${idx} select(String table, String column, String alias) {
        selectColumns.add(new Column(table, column, alias));
        return this;
    }

    public QueryBuilder${idx} selectAggregate(AggregateFunction fn, String column, String alias) {
        selectColumns.add(new Column(null, fn.name() + "(" + column + ")", alias));
        return this;
    }

    public QueryBuilder${idx} distinct() { this.distinct = true; return this; }

    public QueryBuilder${idx} from(String table) { this.fromTable = table; return this; }

    public QueryBuilder${idx} from(String table, String alias) {
        this.fromTable = table; this.fromAlias = alias; return this;
    }

    public QueryBuilder${idx} join(JoinType type, String table, String onCondition) {
        joins.add(type.name().replace('_', ' ') + " JOIN " + table + " ON " + onCondition);
        return this;
    }

    public QueryBuilder${idx} where(String expression, Object... params) {
        whereConditions.add(new Condition(expression, params));
        return this;
    }

    public QueryBuilder${idx} groupBy(String... columns) {
        groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    public QueryBuilder${idx} having(String expression, Object... params) {
        havingConditions.add(new Condition(expression, params));
        return this;
    }

    public QueryBuilder${idx} orderBy(String column, SortDirection dir) {
        orderByColumns.add(column + " " + dir.name());
        return this;
    }

    public QueryBuilder${idx} limit(int limit) { this.limit = limit; return this; }
    public QueryBuilder${idx} offset(int offset) { this.offset = offset; return this; }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        if (selectColumns.isEmpty()) {
            sb.append("*");
        } else {
            StringJoiner sj = new StringJoiner(", ");
            selectColumns.forEach(c -> sj.add(c.toSql()));
            sb.append(sj);
        }
        sb.append(" FROM ").append(fromTable);
        if (fromAlias != null) sb.append(" ").append(fromAlias);
        joins.forEach(j -> sb.append(" ").append(j));
        if (!whereConditions.isEmpty()) {
            sb.append(" WHERE ");
            StringJoiner wj = new StringJoiner(" AND ");
            whereConditions.forEach(c -> wj.add(c.getExpression()));
            sb.append(wj);
        }
        if (!groupByColumns.isEmpty()) sb.append(" GROUP BY ").append(String.join(", ", groupByColumns));
        if (!havingConditions.isEmpty()) {
            sb.append(" HAVING ");
            StringJoiner hj = new StringJoiner(" AND ");
            havingConditions.forEach(c -> hj.add(c.getExpression()));
            sb.append(hj);
        }
        if (!orderByColumns.isEmpty()) sb.append(" ORDER BY ").append(String.join(", ", orderByColumns));
        if (limit != null) sb.append(" LIMIT ").append(limit);
        if (offset != null) sb.append(" OFFSET ").append(offset);
        return sb.toString();
    }

    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        whereConditions.forEach(c -> params.addAll(c.getParams()));
        havingConditions.forEach(c -> params.addAll(c.getParams()));
        return params;
    }
}
JAVAEOF
        ;;
    17)
        # Dependency injection container
        cat > "$dir/Container${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Container${idx} {

    public enum Lifecycle { TRANSIENT, SINGLETON, SCOPED }

    private static class Registration<T> {
        private final Class<T> type;
        private final Supplier<T> factory;
        private final Lifecycle lifecycle;
        private volatile T singletonInstance;

        Registration(Class<T> type, Supplier<T> factory, Lifecycle lifecycle) {
            this.type = type; this.factory = factory; this.lifecycle = lifecycle;
        }

        @SuppressWarnings("unchecked")
        T resolve(Map<Class<?>, Object> scopedInstances) {
            switch (lifecycle) {
                case SINGLETON:
                    if (singletonInstance == null) {
                        synchronized (this) {
                            if (singletonInstance == null) singletonInstance = factory.get();
                        }
                    }
                    return singletonInstance;
                case SCOPED:
                    return (T) scopedInstances.computeIfAbsent(type, k -> factory.get());
                default:
                    return factory.get();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, Registration> registrations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<?>, Object>> scopes = new ConcurrentHashMap<>();
    private final Map<Class<?>, Class<?>> interfaceBindings = new HashMap<>();

    public <T> Container${idx} registerSingleton(Class<T> type, Supplier<T> factory) {
        registrations.put(type, new Registration<>(type, factory, Lifecycle.SINGLETON));
        return this;
    }

    public <T> Container${idx} registerTransient(Class<T> type, Supplier<T> factory) {
        registrations.put(type, new Registration<>(type, factory, Lifecycle.TRANSIENT));
        return this;
    }

    public <T> Container${idx} registerScoped(Class<T> type, Supplier<T> factory) {
        registrations.put(type, new Registration<>(type, factory, Lifecycle.SCOPED));
        return this;
    }

    public <I, T extends I> Container${idx} bind(Class<I> iface, Class<T> impl) {
        interfaceBindings.put(iface, impl);
        return this;
    }

    public <T> Container${idx} registerInstance(Class<T> type, T instance) {
        registrations.put(type, new Registration<>(type, () -> instance, Lifecycle.SINGLETON));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        return resolve(type, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type, String scopeId) {
        Map<Class<?>, Object> scopedInstances = scopes.computeIfAbsent(scopeId, k -> new ConcurrentHashMap<>());
        return resolve(type, scopedInstances);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolve(Class<T> type, Map<Class<?>, Object> scopedInstances) {
        Registration<T> reg = registrations.get(type);
        if (reg == null) {
            Class<?> impl = interfaceBindings.get(type);
            if (impl != null) reg = registrations.get(impl);
        }
        if (reg == null) throw new IllegalStateException("No registration found for: " + type.getSimpleName());
        return reg.resolve(scopedInstances);
    }

    public <T> Optional<T> tryResolve(Class<T> type) {
        try { return Optional.of(resolve(type)); } catch (Exception e) { return Optional.empty(); }
    }

    public void createScope(String scopeId) { scopes.put(scopeId, new ConcurrentHashMap<>()); }
    public void destroyScope(String scopeId) { scopes.remove(scopeId); }
    public boolean isRegistered(Class<?> type) { return registrations.containsKey(type) || interfaceBindings.containsKey(type); }
    public int getRegistrationCount() { return registrations.size(); }
}
JAVAEOF
        ;;
    18)
        # Reactive stream / observable
        cat > "$dir/ReactiveStream${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReactiveStream${idx}<T> {

    @FunctionalInterface
    public interface Subscriber<T> {
        void onNext(T item);
        default void onError(Exception e) {}
        default void onComplete() {}
    }

    public static class Subscription {
        private final Runnable cancelAction;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        Subscription(Runnable cancelAction) { this.cancelAction = cancelAction; }
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) cancelAction.run();
        }
        public boolean isCancelled() { return cancelled.get(); }
    }

    private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();
    private final List<T> buffer = new ArrayList<>();
    private final AtomicLong emittedCount = new AtomicLong(0);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private int bufferSize = 256;
    private boolean replayOnSubscribe = false;

    public ReactiveStream${idx}<T> withBufferSize(int size) { this.bufferSize = size; return this; }
    public ReactiveStream${idx}<T> withReplay(boolean replay) { this.replayOnSubscribe = replay; return this; }

    public Subscription subscribe(Subscriber<T> subscriber) {
        subscribers.add(subscriber);
        if (replayOnSubscribe) {
            synchronized (buffer) {
                buffer.forEach(subscriber::onNext);
            }
        }
        return new Subscription(() -> subscribers.remove(subscriber));
    }

    public Subscription subscribe(Consumer<T> onNext) {
        return subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) { onNext.accept(item); }
        });
    }

    public void emit(T item) {
        if (completed.get()) throw new IllegalStateException("Stream is completed");
        emittedCount.incrementAndGet();
        synchronized (buffer) {
            buffer.add(item);
            while (buffer.size() > bufferSize) buffer.remove(0);
        }
        subscribers.forEach(s -> {
            try { s.onNext(item); } catch (Exception e) { s.onError(e); }
        });
    }

    public void complete() {
        if (completed.compareAndSet(false, true)) {
            subscribers.forEach(Subscriber::onComplete);
        }
    }

    public void error(Exception e) {
        subscribers.forEach(s -> s.onError(e));
    }

    public <R> ReactiveStream${idx}<R> map(Function<T, R> mapper) {
        ReactiveStream${idx}<R> mapped = new ReactiveStream${idx}<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) { mapped.emit(mapper.apply(item)); }
            @Override public void onError(Exception e) { mapped.error(e); }
            @Override public void onComplete() { mapped.complete(); }
        });
        return mapped;
    }

    public ReactiveStream${idx}<T> filter(Predicate<T> predicate) {
        ReactiveStream${idx}<T> filtered = new ReactiveStream${idx}<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) { if (predicate.test(item)) filtered.emit(item); }
            @Override public void onError(Exception e) { filtered.error(e); }
            @Override public void onComplete() { filtered.complete(); }
        });
        return filtered;
    }

    public <R> ReactiveStream${idx}<R> flatMap(Function<T, ReactiveStream${idx}<R>> mapper) {
        ReactiveStream${idx}<R> flat = new ReactiveStream${idx}<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) {
                mapper.apply(item).subscribe(new Subscriber<R>() {
                    @Override public void onNext(R r) { flat.emit(r); }
                });
            }
            @Override public void onComplete() { flat.complete(); }
        });
        return flat;
    }

    public <R> ReactiveStream${idx}<R> scan(R seed, BiFunction<R, T, R> accumulator) {
        ReactiveStream${idx}<R> scanned = new ReactiveStream${idx}<>();
        final Object[] state = { seed };
        subscribe(new Subscriber<T>() {
            @SuppressWarnings("unchecked")
            @Override public void onNext(T item) {
                R current = accumulator.apply((R) state[0], item);
                state[0] = current;
                scanned.emit(current);
            }
            @Override public void onComplete() { scanned.complete(); }
        });
        return scanned;
    }

    public ReactiveStream${idx}<List<T>> window(int size) {
        ReactiveStream${idx}<List<T>> windowed = new ReactiveStream${idx}<>();
        final List<T> currentWindow = new ArrayList<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) {
                currentWindow.add(item);
                if (currentWindow.size() >= size) {
                    windowed.emit(new ArrayList<>(currentWindow));
                    currentWindow.clear();
                }
            }
            @Override public void onComplete() {
                if (!currentWindow.isEmpty()) windowed.emit(new ArrayList<>(currentWindow));
                windowed.complete();
            }
        });
        return windowed;
    }

    public long getEmittedCount() { return emittedCount.get(); }
    public int getSubscriberCount() { return subscribers.size(); }
    public boolean isCompleted() { return completed.get(); }
}
JAVAEOF
        ;;
    19)
        # Weighted scheduler with priority queues
        cat > "$dir/TaskScheduler${idx}.java" <<JAVAEOF
package ${pkg};

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class TaskScheduler${idx} {

    public enum Priority { LOW(0), NORMAL(5), HIGH(10), CRITICAL(20);
        private final int weight;
        Priority(int weight) { this.weight = weight; }
        public int getWeight() { return weight; }
    }

    public enum TaskState { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

    public static class ScheduledTask implements Comparable<ScheduledTask> {
        private static final AtomicLong ID_GEN = new AtomicLong(0);
        private final long id;
        private final String name;
        private final Runnable action;
        private final Priority priority;
        private final long createdAt;
        private volatile TaskState state;
        private long startedAt;
        private long completedAt;
        private Exception error;

        public ScheduledTask(String name, Runnable action, Priority priority) {
            this.id = ID_GEN.incrementAndGet(); this.name = name; this.action = action;
            this.priority = priority; this.createdAt = System.currentTimeMillis();
            this.state = TaskState.PENDING;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            int cmp = Integer.compare(other.priority.weight, this.priority.weight);
            return cmp != 0 ? cmp : Long.compare(this.createdAt, other.createdAt);
        }

        public long getId() { return id; }
        public String getName() { return name; }
        public Priority getPriority() { return priority; }
        public TaskState getState() { return state; }
        public Optional<Exception> getError() { return Optional.ofNullable(error); }
        public long getDurationMs() { return completedAt > 0 ? completedAt - startedAt : 0; }
        public long getWaitTimeMs() { return startedAt > 0 ? startedAt - createdAt : System.currentTimeMillis() - createdAt; }
    }

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();
    private final Map<Long, ScheduledTask> taskMap = new HashMap<>();
    private final List<Consumer<ScheduledTask>> completionListeners = new ArrayList<>();
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private int maxConcurrent = 1;
    private int currentlyRunning = 0;

    public TaskScheduler${idx} withMaxConcurrent(int max) { this.maxConcurrent = max; return this; }

    public TaskScheduler${idx} onCompletion(Consumer<ScheduledTask> listener) {
        completionListeners.add(listener);
        return this;
    }

    public ScheduledTask schedule(String name, Runnable action, Priority priority) {
        ScheduledTask task = new ScheduledTask(name, action, priority);
        synchronized (queue) {
            queue.offer(task);
            taskMap.put(task.getId(), task);
        }
        return task;
    }

    public ScheduledTask schedule(String name, Runnable action) {
        return schedule(name, action, Priority.NORMAL);
    }

    public int runNext() {
        int ran = 0;
        while (currentlyRunning < maxConcurrent) {
            ScheduledTask task;
            synchronized (queue) {
                task = queue.poll();
            }
            if (task == null) break;
            executeTask(task);
            ran++;
        }
        return ran;
    }

    public int runAll() {
        int total = 0;
        while (!queue.isEmpty()) { total += runNext(); }
        return total;
    }

    private void executeTask(ScheduledTask task) {
        task.state = TaskState.RUNNING;
        task.startedAt = System.currentTimeMillis();
        currentlyRunning++;
        try {
            task.action.run();
            task.state = TaskState.COMPLETED;
            completedCount.incrementAndGet();
        } catch (Exception e) {
            task.state = TaskState.FAILED;
            task.error = e;
            failedCount.incrementAndGet();
        } finally {
            task.completedAt = System.currentTimeMillis();
            currentlyRunning--;
            completionListeners.forEach(l -> l.accept(task));
        }
    }

    public boolean cancel(long taskId) {
        ScheduledTask task = taskMap.get(taskId);
        if (task != null && task.state == TaskState.PENDING) {
            task.state = TaskState.CANCELLED;
            synchronized (queue) { queue.remove(task); }
            return true;
        }
        return false;
    }

    public Optional<ScheduledTask> getTask(long id) { return Optional.ofNullable(taskMap.get(id)); }
    public int getPendingCount() { return queue.size(); }
    public int getCompletedCount() { return completedCount.get(); }
    public int getFailedCount() { return failedCount.get(); }
}
JAVAEOF
        ;;
    esac
}

echo "Generating 500 Java classes in cart module..."
for i in $(seq 0 499); do
    idx=$((CART_START + i))
    generate_class "$CART_DIR" "com.awesomeapp.cart.java" "$idx"
done

echo "Generating 700 Java classes in app module..."
for i in $(seq 0 699); do
    idx=$((APP_START + i))
    generate_class "$APP_DIR" "com.awesomeapp.app.java" "$idx"
done

echo "Done!"
echo "Cart classes: $(find "$CART_DIR" -name "*.java" -newer "$0" | wc -l) new files"
echo "App classes: $(find "$APP_DIR" -name "*.java" | wc -l) files"
