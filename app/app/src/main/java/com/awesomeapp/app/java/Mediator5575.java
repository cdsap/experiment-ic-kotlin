package com.awesomeapp.app.java;

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

public class Mediator5575 {

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
