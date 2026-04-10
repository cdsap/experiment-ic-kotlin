package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventBus5541 {

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

    public EventBus5541 withHistory(boolean record, int maxSize) {
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
