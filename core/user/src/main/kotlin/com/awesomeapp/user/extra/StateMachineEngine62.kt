package com.awesomeapp.user.extra

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StateMachineEngine62<S : Any, E : Any>(initialState: S) {

    data class Transition<S, E>(
        val from: S,
        val event: E,
        val to: S,
        val guard: () -> Boolean = { true },
        val action: suspend (TransitionContext<S, E>) -> Unit = {}
    )

    data class TransitionContext<S, E>(
        val fromState: S,
        val toState: S,
        val event: E,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class TransitionRecord<S, E>(
        val from: S,
        val to: S,
        val event: E,
        val timestamp: Long,
        val success: Boolean
    )

    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<S> = _currentState.asStateFlow()

    private val transitions = mutableListOf<Transition<S, E>>()
    private val onEnterHandlers = mutableMapOf<S, suspend (S) -> Unit>()
    private val onExitHandlers = mutableMapOf<S, suspend (S) -> Unit>()
    private val history = mutableListOf<TransitionRecord<S, E>>()
    private val mutex = Mutex()
    private var maxHistorySize = 500
    private val globalListeners = mutableListOf<suspend (TransitionRecord<S, E>) -> Unit>()

    fun addTransition(from: S, event: E, to: S, guard: () -> Boolean = { true }, action: suspend (TransitionContext<S, E>) -> Unit = {}) {
        transitions.add(Transition(from, event, to, guard, action))
    }

    fun onEnter(state: S, handler: suspend (S) -> Unit) { onEnterHandlers[state] = handler }
    fun onExit(state: S, handler: suspend (S) -> Unit) { onExitHandlers[state] = handler }
    fun addListener(listener: suspend (TransitionRecord<S, E>) -> Unit) { globalListeners.add(listener) }

    suspend fun processEvent(event: E): Boolean = mutex.withLock {
        val current = _currentState.value
        val transition = transitions.find { it.from == current && it.event == event && it.guard() }
            ?: run {
                history.add(TransitionRecord(current, current, event, System.currentTimeMillis(), false))
                return false
            }

        onExitHandlers[current]?.invoke(current)

        val context = TransitionContext(current, transition.to, event)
        transition.action(context)

        _currentState.value = transition.to
        onEnterHandlers[transition.to]?.invoke(transition.to)

        val record = TransitionRecord(current, transition.to, event, System.currentTimeMillis(), true)
        history.add(record)
        trimHistory()
        globalListeners.forEach { it(record) }
        true
    }

    fun canProcess(event: E): Boolean {
        val current = _currentState.value
        return transitions.any { it.from == current && it.event == event && it.guard() }
    }

    fun availableEvents(): List<E> {
        val current = _currentState.value
        return transitions.filter { it.from == current && it.guard() }.map { it.event }
    }

    fun getHistory(): List<TransitionRecord<S, E>> = history.toList()

    fun getTransitionCount(): Int = history.count { it.success }

    fun getStateVisitCounts(): Map<S, Int> {
        val counts = mutableMapOf<S, Int>()
        history.filter { it.success }.forEach {
            counts[it.to] = (counts[it.to] ?: 0) + 1
        }
        return counts
    }

    private fun trimHistory() {
        while (history.size > maxHistorySize) history.removeFirst()
    }

    fun withMaxHistory(size: Int): StateMachineEngine62<S, E> {
        maxHistorySize = size
        return this
    }

    fun reset(state: S) {
        _currentState.value = state
        history.clear()
    }
}
