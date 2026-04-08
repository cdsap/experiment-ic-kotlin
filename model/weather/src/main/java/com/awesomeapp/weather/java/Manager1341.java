package com.awesomeapp.weather.java;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Manager1341 {
    private final Queue<String> queue = new ArrayDeque<>();
    private final List<String> completed = new ArrayList<>();
    private final AtomicInteger processed = new AtomicInteger(0);
    private boolean running = false;

    public void enqueue(String item) {
        if (item != null && !item.isEmpty()) { queue.offer(item); }
    }

    public void enqueueAll(List<String> items) {
        if (items != null) { items.forEach(this::enqueue); }
    }

    public String processNext() {
        String item = queue.poll();
        if (item != null) { completed.add(item); processed.incrementAndGet(); }
        return item;
    }

    public List<String> processAll() {
        List<String> results = new ArrayList<>();
        String item;
        while ((item = processNext()) != null) { results.add(item); }
        return results;
    }

    public int getPendingCount() { return queue.size(); }
    public int getProcessedCount() { return processed.get(); }
    public List<String> getCompleted() { return new ArrayList<>(completed); }

    public void start() { running = true; }
    public void stop() { running = false; }
    public boolean isRunning() { return running; }
}
