package com.awesomeapp.cart.java;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectPool2013<T> {

    public enum PoolState { ACTIVE, DRAINING, CLOSED }

    public static class PoolStats {
        private final int totalCreated;
        private final int currentIdle;
        private final int currentActive;
        private final long totalBorrows;
        private final long totalReturns;

        public PoolStats(int created, int idle, int active, long borrows, long returns) {
            this.totalCreated = created; this.currentIdle = idle; this.currentActive = active;
            this.totalBorrows = borrows; this.totalReturns = returns;
        }

        public int getTotalCreated() { return totalCreated; }
        public int getCurrentIdle() { return currentIdle; }
        public int getCurrentActive() { return currentActive; }
        public long getTotalBorrows() { return totalBorrows; }
        public long getTotalReturns() { return totalReturns; }
        public double getUtilization() {
            int total = currentIdle + currentActive;
            return total == 0 ? 0 : (double) currentActive / total;
        }
    }

    private final Supplier<T> factory;
    private final Consumer<T> destroyer;
    private final Consumer<T> validator;
    private final Queue<T> idlePool = new ConcurrentLinkedQueue<>();
    private final Set<T> activeSet = ConcurrentHashMap.newKeySet();
    private final Semaphore semaphore;
    private final int minIdle;
    private final int maxSize;
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicLong totalBorrows = new AtomicLong(0);
    private final AtomicLong totalReturns = new AtomicLong(0);
    private volatile PoolState state = PoolState.ACTIVE;

    public ObjectPool2013(Supplier<T> factory, Consumer<T> destroyer, Consumer<T> validator, int minIdle, int maxSize) {
        this.factory = factory; this.destroyer = destroyer; this.validator = validator;
        this.minIdle = minIdle; this.maxSize = maxSize;
        this.semaphore = new Semaphore(maxSize);
        initializePool();
    }

    private void initializePool() {
        for (int i = 0; i < minIdle; i++) {
            T obj = createObject();
            if (obj != null) idlePool.offer(obj);
        }
    }

    private T createObject() {
        T obj = factory.get();
        totalCreated.incrementAndGet();
        return obj;
    }

    public T borrow(long timeoutMs) throws InterruptedException {
        if (state != PoolState.ACTIVE) throw new IllegalStateException("Pool is not active");
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Timeout waiting for pool object");
        }
        T obj = idlePool.poll();
        if (obj == null) {
            obj = createObject();
        } else {
            try {
                if (validator != null) validator.accept(obj);
            } catch (Exception e) {
                obj = createObject();
            }
        }
        activeSet.add(obj);
        totalBorrows.incrementAndGet();
        return obj;
    }

    public void release(T obj) {
        if (!activeSet.remove(obj)) return;
        totalReturns.incrementAndGet();
        if (state == PoolState.ACTIVE) {
            idlePool.offer(obj);
        } else {
            if (destroyer != null) destroyer.accept(obj);
        }
        semaphore.release();
    }

    public void evict() {
        int currentIdle = idlePool.size();
        int toRemove = currentIdle - minIdle;
        for (int i = 0; i < toRemove; i++) {
            T obj = idlePool.poll();
            if (obj != null && destroyer != null) destroyer.accept(obj);
        }
    }

    public void close() {
        state = PoolState.DRAINING;
        T obj;
        while ((obj = idlePool.poll()) != null) {
            if (destroyer != null) destroyer.accept(obj);
        }
        state = PoolState.CLOSED;
    }

    public PoolStats getStats() {
        return new PoolStats(totalCreated.get(), idlePool.size(), activeSet.size(), totalBorrows.get(), totalReturns.get());
    }

    public PoolState getState() { return state; }
}
