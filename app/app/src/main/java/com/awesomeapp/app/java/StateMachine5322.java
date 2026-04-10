package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class StateMachine5322<S, E> {

    public static class Transition<S, E> {
        private final S from;
        private final E event;
        private final S to;
        private final Predicate<E> guard;
        private final Consumer<E> action;

        public Transition(S from, E event, S to, Predicate<E> guard, Consumer<E> action) {
            this.from = from;
            this.event = event;
            this.to = to;
            this.guard = guard;
            this.action = action;
        }

        public S getFrom() { return from; }
        public E getEvent() { return event; }
        public S getTo() { return to; }
        public boolean isAllowed(E evt) { return guard == null || guard.test(evt); }
        public void execute(E evt) { if (action != null) action.accept(evt); }
    }

    public static class TransitionResult<S> {
        private final boolean success;
        private final S previousState;
        private final S currentState;
        private final String errorMessage;

        private TransitionResult(boolean success, S prev, S curr, String error) {
            this.success = success; this.previousState = prev; this.currentState = curr; this.errorMessage = error;
        }

        public static <S> TransitionResult<S> success(S prev, S curr) { return new TransitionResult<>(true, prev, curr, null); }
        public static <S> TransitionResult<S> failure(S current, String error) { return new TransitionResult<>(false, current, current, error); }

        public boolean isSuccess() { return success; }
        public S getPreviousState() { return previousState; }
        public S getCurrentState() { return currentState; }
        public Optional<String> getError() { return Optional.ofNullable(errorMessage); }
    }

    private S currentState;
    private final Map<S, Map<E, List<Transition<S, E>>>> transitionTable = new HashMap<>();
    private final List<Consumer<TransitionResult<S>>> listeners = new ArrayList<>();
    private final List<TransitionResult<S>> history = new ArrayList<>();
    private boolean recordHistory = true;

    public StateMachine5322(S initialState) {
        this.currentState = initialState;
    }

    public StateMachine5322<S, E> addTransition(S from, E event, S to) {
        return addTransition(from, event, to, null, null);
    }

    public StateMachine5322<S, E> addTransition(S from, E event, S to, Predicate<E> guard, Consumer<E> action) {
        transitionTable
            .computeIfAbsent(from, k -> new HashMap<>())
            .computeIfAbsent(event, k -> new ArrayList<>())
            .add(new Transition<>(from, event, to, guard, action));
        return this;
    }

    public StateMachine5322<S, E> onTransition(Consumer<TransitionResult<S>> listener) {
        listeners.add(listener);
        return this;
    }

    public TransitionResult<S> fire(E event) {
        Map<E, List<Transition<S, E>>> stateTransitions = transitionTable.get(currentState);
        if (stateTransitions == null) {
            return fail("No transitions defined for state: " + currentState);
        }
        List<Transition<S, E>> candidates = stateTransitions.get(event);
        if (candidates == null || candidates.isEmpty()) {
            return fail("No transition for event " + event + " in state " + currentState);
        }
        for (Transition<S, E> t : candidates) {
            if (t.isAllowed(event)) {
                S prev = currentState;
                t.execute(event);
                currentState = t.getTo();
                TransitionResult<S> result = TransitionResult.success(prev, currentState);
                if (recordHistory) history.add(result);
                listeners.forEach(l -> l.accept(result));
                return result;
            }
        }
        return fail("All guards failed for event " + event + " in state " + currentState);
    }

    private TransitionResult<S> fail(String msg) {
        TransitionResult<S> result = TransitionResult.failure(currentState, msg);
        if (recordHistory) history.add(result);
        return result;
    }

    public S getCurrentState() { return currentState; }
    public List<TransitionResult<S>> getHistory() { return new ArrayList<>(history); }
    public boolean isInState(S state) { return currentState.equals(state); }
}
