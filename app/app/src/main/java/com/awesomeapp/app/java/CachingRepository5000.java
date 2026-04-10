package com.awesomeapp.app.java;

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

public class CachingRepository5000<K, V> {

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

    public CachingRepository5000(int maxSize, EvictionPolicy policy) {
        this.maxSize = maxSize;
        this.policy = policy;
    }

    public CachingRepository5000<K, V> withLoader(Function<K, V> loader) {
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
