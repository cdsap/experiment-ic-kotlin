package com.awesomeapp.cart.java;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class CommandExecutor2426 {

    public interface Command {
        String getName();
        void execute() throws Exception;
        void undo() throws Exception;
        boolean isReversible();
    }

    public static abstract class AbstractCommand implements Command {
        private final String name;
        protected AbstractCommand(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public boolean isReversible() { return true; }
    }

    public static class CompositeCommand implements Command {
        private final String name;
        private final List<Command> commands;
        private int executedCount = 0;

        public CompositeCommand(String name, List<Command> commands) {
            this.name = name;
            this.commands = new ArrayList<>(commands);
        }

        @Override public String getName() { return name; }
        @Override public boolean isReversible() { return commands.stream().allMatch(Command::isReversible); }

        @Override
        public void execute() throws Exception {
            for (Command cmd : commands) {
                cmd.execute();
                executedCount++;
            }
        }

        @Override
        public void undo() throws Exception {
            for (int i = executedCount - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
            executedCount = 0;
        }
    }

    public static class ExecutionRecord {
        private final Command command;
        private final long executedAt;
        private final boolean success;
        private final String error;

        public ExecutionRecord(Command cmd, boolean success, String error) {
            this.command = cmd; this.executedAt = System.currentTimeMillis();
            this.success = success; this.error = error;
        }

        public Command getCommand() { return command; }
        public long getExecutedAt() { return executedAt; }
        public boolean isSuccess() { return success; }
        public Optional<String> getError() { return Optional.ofNullable(error); }
    }

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final List<ExecutionRecord> history = new ArrayList<>();
    private final AtomicLong executionCount = new AtomicLong(0);
    private int maxUndoDepth = 100;

    public CommandExecutor2426 withMaxUndoDepth(int depth) {
        this.maxUndoDepth = depth;
        return this;
    }

    public boolean execute(Command command) {
        try {
            command.execute();
            history.add(new ExecutionRecord(command, true, null));
            if (command.isReversible()) {
                undoStack.push(command);
                if (undoStack.size() > maxUndoDepth) {
                    undoStack.removeLast();
                }
            }
            redoStack.clear();
            executionCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            history.add(new ExecutionRecord(command, false, e.getMessage()));
            return false;
        }
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command cmd = undoStack.pop();
        try {
            cmd.undo();
            redoStack.push(cmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command cmd = redoStack.pop();
        try {
            cmd.execute();
            undoStack.push(cmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public List<ExecutionRecord> getHistory() { return new ArrayList<>(history); }
    public long getExecutionCount() { return executionCount.get(); }
}
