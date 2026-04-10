package com.awesomeapp.app.java;

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

public class ObservableStore5325<K, V> {

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

    public ObservableStore5325<K, V> withChangeTracking(boolean track) {
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
