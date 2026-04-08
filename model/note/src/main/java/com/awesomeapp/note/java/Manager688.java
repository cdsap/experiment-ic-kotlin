package com.awesomeapp.note.java;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Manager688 {
    private final Map<String, Object> registry = new LinkedHashMap<>();
    private final int maxCapacity;

    public Manager688() { this(1000); }
    public Manager688(int maxCapacity) { this.maxCapacity = maxCapacity; }

    public void register(String key, Object item) {
        if (registry.size() >= maxCapacity) {
            throw new IllegalStateException("Registry full: " + maxCapacity);
        }
        registry.put(key, item);
    }

    public void unregister(String key) { registry.remove(key); }

    public Optional<Object> get(String key) { return Optional.ofNullable(registry.get(key)); }

    public boolean isRegistered(String key) { return registry.containsKey(key); }

    public Set<String> getKeys() { return Collections.unmodifiableSet(registry.keySet()); }

    public int getCount() { return registry.size(); }

    public int getMaxCapacity() { return maxCapacity; }

    public void clear() { registry.clear(); }

    public Map<String, Object> snapshot() { return Collections.unmodifiableMap(new LinkedHashMap<>(registry)); }
}
