package com.awesomeapp.task.extra

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RuleEngine1336<F : Any> {

    data class Rule<F>(
        val id: String,
        val name: String,
        val priority: Int = 0,
        val condition: (F) -> Boolean,
        val action: suspend (F, RuleContext) -> RuleResult,
        val description: String = "",
        val group: String = "default",
        val enabled: Boolean = true
    )

    data class RuleContext(
        val facts: MutableMap<String, Any> = mutableMapOf(),
        val firedRules: MutableList<String> = mutableListOf(),
        val startTime: Long = System.currentTimeMillis()
    ) {
        inline fun <reified T> getFact(key: String): T? = facts[key] as? T
        fun setFact(key: String, value: Any) { facts[key] = value }
        val elapsedMs: Long get() = System.currentTimeMillis() - startTime
    }

    sealed class RuleResult {
        data object Continue : RuleResult()
        data object Stop : RuleResult()
        data class ModifyFacts(val modifications: Map<String, Any>) : RuleResult()
        data class Error(val message: String, val cause: Throwable? = null) : RuleResult()
    }

    data class ExecutionResult(
        val firedRules: List<String>,
        val context: RuleContext,
        val totalTimeMs: Long,
        val rulesEvaluated: Int,
        val rulesFired: Int,
        val errors: List<Pair<String, Throwable>>
    )

    data class EngineStats(
        val totalExecutions: Long = 0,
        val totalRulesFired: Long = 0,
        val totalErrors: Long = 0,
        val avgExecutionTimeMs: Double = 0.0,
        val ruleFireCounts: Map<String, Long> = emptyMap()
    )

    enum class ConflictResolution { PRIORITY, FIRST_MATCH, ALL }

    private val rules = mutableListOf<Rule<F>>()
    private val ruleFireCounts = mutableMapOf<String, Long>()
    private var conflictResolution = ConflictResolution.PRIORITY
    private var maxIterations = 100
    private var totalExecutions = 0L
    private var totalTimeMs = 0L

    private val _stats = MutableStateFlow(EngineStats())
    val stats: StateFlow<EngineStats> = _stats.asStateFlow()

    fun addRule(rule: Rule<F>): RuleEngine1336<F> { rules.add(rule); return this }
    fun removeRule(id: String) { rules.removeIf { it.id == id } }
    fun enableRule(id: String) { rules.find { it.id == id }?.let { rules[rules.indexOf(it)] = it.copy(enabled = true) } }
    fun disableRule(id: String) { rules.find { it.id == id }?.let { rules[rules.indexOf(it)] = it.copy(enabled = false) } }
    fun setConflictResolution(strategy: ConflictResolution) { conflictResolution = strategy }
    fun setMaxIterations(max: Int) { maxIterations = max }

    suspend fun execute(facts: F): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val context = RuleContext()
        val errors = mutableListOf<Pair<String, Throwable>>()
        var rulesEvaluated = 0
        var rulesFired = 0
        var iteration = 0

        while (iteration < maxIterations) {
            val activeRules = rules.filter { it.enabled }
            val matchingRules = activeRules.filter {
                rulesEvaluated++
                try { it.condition(facts) } catch (_: Throwable) { false }
            }

            if (matchingRules.isEmpty()) break

            val toFire = when (conflictResolution) {
                ConflictResolution.PRIORITY -> listOf(matchingRules.maxByOrNull { it.priority }!!)
                ConflictResolution.FIRST_MATCH -> listOf(matchingRules.first())
                ConflictResolution.ALL -> matchingRules.sortedByDescending { it.priority }
            }

            var shouldStop = false
            for (rule in toFire) {
                try {
                    val result = rule.action(facts, context)
                    context.firedRules.add(rule.id)
                    rulesFired++
                    ruleFireCounts[rule.id] = (ruleFireCounts[rule.id] ?: 0) + 1

                    when (result) {
                        is RuleResult.Stop -> { shouldStop = true; break }
                        is RuleResult.ModifyFacts -> context.facts.putAll(result.modifications)
                        is RuleResult.Error -> { errors.add(rule.id to (result.cause ?: RuntimeException(result.message))); shouldStop = true; break }
                        is RuleResult.Continue -> {}
                    }
                } catch (e: Throwable) {
                    errors.add(rule.id to e)
                }
            }

            if (shouldStop) break
            iteration++
        }

        val totalTime = System.currentTimeMillis() - startTime
        totalExecutions++
        totalTimeMs += totalTime
        updateStats(rulesFired)

        return ExecutionResult(
            firedRules = context.firedRules,
            context = context,
            totalTimeMs = totalTime,
            rulesEvaluated = rulesEvaluated,
            rulesFired = rulesFired,
            errors = errors
        )
    }

    private fun updateStats(fired: Int) {
        _stats.value = EngineStats(
            totalExecutions = totalExecutions,
            totalRulesFired = _stats.value.totalRulesFired + fired,
            totalErrors = _stats.value.totalErrors,
            avgExecutionTimeMs = if (totalExecutions == 0L) 0.0 else totalTimeMs.toDouble() / totalExecutions,
            ruleFireCounts = ruleFireCounts.toMap()
        )
    }

    fun getRules(): List<Rule<F>> = rules.toList()
    fun getRulesByGroup(group: String): List<Rule<F>> = rules.filter { it.group == group }
    fun getGroups(): Set<String> = rules.map { it.group }.toSet()
}
