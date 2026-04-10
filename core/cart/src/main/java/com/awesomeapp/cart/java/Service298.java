package com.awesomeapp.cart.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class with moderate complexity - business logic and collections.
 */
public class Service298 {
    private final Map<String, Object> cache = new HashMap<>();
    private final List<String> processedIds = new ArrayList<>();
    private int operationCount = 0;

    public void addToCache(String key, Object model) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        cache.put(key, model);
        operationCount++;
    }

    public Optional<Object> getFromCache(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    public List<Object> getAllCached() {
        return Collections.unmodifiableList(new ArrayList<>(cache.values()));
    }

    public List<String> filterKeysByPrefix(String prefix) {
        return cache.keySet().stream()
            .filter(k -> k.startsWith(prefix))
            .collect(Collectors.toList());
    }

    public void processItem(String id) {
        if (!processedIds.contains(id)) {
            processedIds.add(id);
            operationCount++;
        }
    }

    public List<String> getProcessedIds() {
        return Collections.unmodifiableList(processedIds);
    }

    public int getOperationCount() {
        return operationCount;
    }

    public void clearCache() {
        cache.clear();
        operationCount++;
    }

    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("cacheSize", (long) cache.size());
        stats.put("processedCount", (long) processedIds.size());
        stats.put("operationCount", (long) operationCount);
        return stats;
    }
}
