package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class TaskScheduler5519 {

    public enum Priority { LOW(0), NORMAL(5), HIGH(10), CRITICAL(20);
        private final int weight;
        Priority(int weight) { this.weight = weight; }
        public int getWeight() { return weight; }
    }

    public enum TaskState { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

    public static class ScheduledTask implements Comparable<ScheduledTask> {
        private static final AtomicLong ID_GEN = new AtomicLong(0);
        private final long id;
        private final String name;
        private final Runnable action;
        private final Priority priority;
        private final long createdAt;
        private volatile TaskState state;
        private long startedAt;
        private long completedAt;
        private Exception error;

        public ScheduledTask(String name, Runnable action, Priority priority) {
            this.id = ID_GEN.incrementAndGet(); this.name = name; this.action = action;
            this.priority = priority; this.createdAt = System.currentTimeMillis();
            this.state = TaskState.PENDING;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            int cmp = Integer.compare(other.priority.weight, this.priority.weight);
            return cmp != 0 ? cmp : Long.compare(this.createdAt, other.createdAt);
        }

        public long getId() { return id; }
        public String getName() { return name; }
        public Priority getPriority() { return priority; }
        public TaskState getState() { return state; }
        public Optional<Exception> getError() { return Optional.ofNullable(error); }
        public long getDurationMs() { return completedAt > 0 ? completedAt - startedAt : 0; }
        public long getWaitTimeMs() { return startedAt > 0 ? startedAt - createdAt : System.currentTimeMillis() - createdAt; }
    }

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();
    private final Map<Long, ScheduledTask> taskMap = new HashMap<>();
    private final List<Consumer<ScheduledTask>> completionListeners = new ArrayList<>();
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private int maxConcurrent = 1;
    private int currentlyRunning = 0;

    public TaskScheduler5519 withMaxConcurrent(int max) { this.maxConcurrent = max; return this; }

    public TaskScheduler5519 onCompletion(Consumer<ScheduledTask> listener) {
        completionListeners.add(listener);
        return this;
    }

    public ScheduledTask schedule(String name, Runnable action, Priority priority) {
        ScheduledTask task = new ScheduledTask(name, action, priority);
        synchronized (queue) {
            queue.offer(task);
            taskMap.put(task.getId(), task);
        }
        return task;
    }

    public ScheduledTask schedule(String name, Runnable action) {
        return schedule(name, action, Priority.NORMAL);
    }

    public int runNext() {
        int ran = 0;
        while (currentlyRunning < maxConcurrent) {
            ScheduledTask task;
            synchronized (queue) {
                task = queue.poll();
            }
            if (task == null) break;
            executeTask(task);
            ran++;
        }
        return ran;
    }

    public int runAll() {
        int total = 0;
        while (!queue.isEmpty()) { total += runNext(); }
        return total;
    }

    private void executeTask(ScheduledTask task) {
        task.state = TaskState.RUNNING;
        task.startedAt = System.currentTimeMillis();
        currentlyRunning++;
        try {
            task.action.run();
            task.state = TaskState.COMPLETED;
            completedCount.incrementAndGet();
        } catch (Exception e) {
            task.state = TaskState.FAILED;
            task.error = e;
            failedCount.incrementAndGet();
        } finally {
            task.completedAt = System.currentTimeMillis();
            currentlyRunning--;
            completionListeners.forEach(l -> l.accept(task));
        }
    }

    public boolean cancel(long taskId) {
        ScheduledTask task = taskMap.get(taskId);
        if (task != null && task.state == TaskState.PENDING) {
            task.state = TaskState.CANCELLED;
            synchronized (queue) { queue.remove(task); }
            return true;
        }
        return false;
    }

    public Optional<ScheduledTask> getTask(long id) { return Optional.ofNullable(taskMap.get(id)); }
    public int getPendingCount() { return queue.size(); }
    public int getCompletedCount() { return completedCount.get(); }
    public int getFailedCount() { return failedCount.get(); }
}
