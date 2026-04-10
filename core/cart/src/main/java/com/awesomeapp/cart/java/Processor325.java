package com.awesomeapp.cart.java;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Complex processor class 325 with generics, functional interfaces, and state management.
 */
public class Processor325<T> {
    private final List<T> items = new ArrayList<>();
    private final Deque<T> pendingQueue = new ArrayDeque<>();
    private final Map<String, Consumer<T>> handlers = new LinkedHashMap<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private Predicate<T> filter;
    private Function<T, String> classifier;

    public Processor325() {
        this.filter = t -> true;
        this.classifier = Object::toString;
    }

    public Processor325<T> withFilter(Predicate<T> filter) {
        this.filter = filter;
        return this;
    }

    public Processor325<T> withClassifier(Function<T, String> classifier) {
        this.classifier = classifier;
        return this;
    }

    public void registerHandler(String category, Consumer<T> handler) {
        handlers.put(category, handler);
    }

    public void submit(T item) {
        if (filter.test(item)) {
            pendingQueue.offer(item);
        }
    }

    public void submitAll(List<T> batch) {
        batch.stream().filter(filter).forEach(pendingQueue::offer);
    }

    public int processBatch(int maxItems) {
        int count = 0;
        while (!pendingQueue.isEmpty() && count < maxItems) {
            T item = pendingQueue.poll();
            String category = classifier.apply(item);
            Consumer<T> handler = handlers.get(category);
            if (handler != null) {
                handler.accept(item);
            }
            items.add(item);
            processedCount.incrementAndGet();
            count++;
        }
        return count;
    }

    public void processAll() {
        while (!pendingQueue.isEmpty()) {
            processBatch(Integer.MAX_VALUE);
        }
    }

    public List<T> getProcessedItems() {
        return new ArrayList<>(items);
    }

    public int getPendingCount() {
        return pendingQueue.size();
    }

    public int getTotalProcessed() {
        return processedCount.get();
    }

    public Map<String, List<T>> groupByCategory() {
        Map<String, List<T>> groups = new LinkedHashMap<>();
        for (T item : items) {
            String cat = classifier.apply(item);
            groups.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    public <R> List<R> transform(Function<T, R> mapper) {
        List<R> results = new ArrayList<>();
        for (T item : items) {
            results.add(mapper.apply(item));
        }
        return results;
    }
}
