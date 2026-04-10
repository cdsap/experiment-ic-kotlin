package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReactiveStream5658<T> {

    @FunctionalInterface
    public interface Subscriber<T> {
        void onNext(T item);
        default void onError(Exception e) {}
        default void onComplete() {}
    }

    public static class Subscription {
        private final Runnable cancelAction;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        Subscription(Runnable cancelAction) { this.cancelAction = cancelAction; }
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) cancelAction.run();
        }
        public boolean isCancelled() { return cancelled.get(); }
    }

    private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();
    private final List<T> buffer = new ArrayList<>();
    private final AtomicLong emittedCount = new AtomicLong(0);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private int bufferSize = 256;
    private boolean replayOnSubscribe = false;

    public ReactiveStream5658<T> withBufferSize(int size) { this.bufferSize = size; return this; }
    public ReactiveStream5658<T> withReplay(boolean replay) { this.replayOnSubscribe = replay; return this; }

    public Subscription subscribe(Subscriber<T> subscriber) {
        subscribers.add(subscriber);
        if (replayOnSubscribe) {
            synchronized (buffer) {
                buffer.forEach(subscriber::onNext);
            }
        }
        return new Subscription(() -> subscribers.remove(subscriber));
    }

    public Subscription subscribe(Consumer<T> onNext) {
        return subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) { onNext.accept(item); }
        });
    }

    public void emit(T item) {
        if (completed.get()) throw new IllegalStateException("Stream is completed");
        emittedCount.incrementAndGet();
        synchronized (buffer) {
            buffer.add(item);
            while (buffer.size() > bufferSize) buffer.remove(0);
        }
        subscribers.forEach(s -> {
            try { s.onNext(item); } catch (Exception e) { s.onError(e); }
        });
    }

    public void complete() {
        if (completed.compareAndSet(false, true)) {
            subscribers.forEach(Subscriber::onComplete);
        }
    }

    public void error(Exception e) {
        subscribers.forEach(s -> s.onError(e));
    }

    public <R> ReactiveStream5658<R> map(Function<T, R> mapper) {
        ReactiveStream5658<R> mapped = new ReactiveStream5658<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) { mapped.emit(mapper.apply(item)); }
            @Override public void onError(Exception e) { mapped.error(e); }
            @Override public void onComplete() { mapped.complete(); }
        });
        return mapped;
    }

    public ReactiveStream5658<T> filter(Predicate<T> predicate) {
        ReactiveStream5658<T> filtered = new ReactiveStream5658<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) { if (predicate.test(item)) filtered.emit(item); }
            @Override public void onError(Exception e) { filtered.error(e); }
            @Override public void onComplete() { filtered.complete(); }
        });
        return filtered;
    }

    public <R> ReactiveStream5658<R> flatMap(Function<T, ReactiveStream5658<R>> mapper) {
        ReactiveStream5658<R> flat = new ReactiveStream5658<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) {
                mapper.apply(item).subscribe(new Subscriber<R>() {
                    @Override public void onNext(R r) { flat.emit(r); }
                });
            }
            @Override public void onComplete() { flat.complete(); }
        });
        return flat;
    }

    public <R> ReactiveStream5658<R> scan(R seed, BiFunction<R, T, R> accumulator) {
        ReactiveStream5658<R> scanned = new ReactiveStream5658<>();
        final Object[] state = { seed };
        subscribe(new Subscriber<T>() {
            @SuppressWarnings("unchecked")
            @Override public void onNext(T item) {
                R current = accumulator.apply((R) state[0], item);
                state[0] = current;
                scanned.emit(current);
            }
            @Override public void onComplete() { scanned.complete(); }
        });
        return scanned;
    }

    public ReactiveStream5658<List<T>> window(int size) {
        ReactiveStream5658<List<T>> windowed = new ReactiveStream5658<>();
        final List<T> currentWindow = new ArrayList<>();
        subscribe(new Subscriber<T>() {
            @Override public void onNext(T item) {
                currentWindow.add(item);
                if (currentWindow.size() >= size) {
                    windowed.emit(new ArrayList<>(currentWindow));
                    currentWindow.clear();
                }
            }
            @Override public void onComplete() {
                if (!currentWindow.isEmpty()) windowed.emit(new ArrayList<>(currentWindow));
                windowed.complete();
            }
        });
        return windowed;
    }

    public long getEmittedCount() { return emittedCount.get(); }
    public int getSubscriberCount() { return subscribers.size(); }
    public boolean isCompleted() { return completed.get(); }
}
