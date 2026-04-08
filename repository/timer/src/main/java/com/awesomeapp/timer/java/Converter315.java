package com.awesomeapp.timer.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Converter315 {
    private final Map<String, String> mappings = new HashMap<>();

    public void addMapping(String from, String to) { mappings.put(from, to); }

    public String convert(String input) {
        return mappings.getOrDefault(input, input);
    }

    public List<String> convertAll(List<String> inputs) {
        return inputs.stream().map(this::convert).collect(Collectors.toList());
    }

    public Map<String, String> getMappings() { return new HashMap<>(mappings); }

    public boolean hasMapping(String key) { return mappings.containsKey(key); }

    public void removeMapping(String key) { mappings.remove(key); }

    public void clearMappings() { mappings.clear(); }

    public int size() { return mappings.size(); }

    public Map<String, String> reverseMappings() {
        Map<String, String> reversed = new HashMap<>();
        mappings.forEach((k, v) -> reversed.put(v, k));
        return reversed;
    }
}
