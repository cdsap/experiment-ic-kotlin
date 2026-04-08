package com.awesomeapp.metric.java;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Factory437 {
    private final Map<String, Object> properties = new HashMap<>();
    private String type = "default";

    public Factory437 withType(String type) { this.type = type; return this; }

    public Factory437 withProperty(String key, Object value) {
        properties.put(key, value); return this;
    }

    public Factory437 withProperties(Map<String, Object> props) {
        properties.putAll(props); return this;
    }

    public String getType() { return type; }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key) { return (T) properties.get(key); }

    public boolean hasProperty(String key) { return properties.containsKey(key); }

    public Map<String, Object> build() {
        Map<String, Object> result = new HashMap<>(properties);
        result.put("_type", type);
        return result;
    }

    public static <T> T createOrDefault(Supplier<T> supplier, T defaultValue) {
        try { return supplier.get(); } catch (Exception e) { return defaultValue; }
    }

    public Factory437 reset() { properties.clear(); type = "default"; return this; }
}
