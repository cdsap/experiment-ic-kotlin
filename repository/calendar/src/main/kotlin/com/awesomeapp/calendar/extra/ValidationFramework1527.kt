package com.awesomeapp.calendar.extra

class ValidationFramework1527<T : Any> {

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val violations: List<Violation>) : ValidationResult() {
            override val isValid: Boolean get() = false
        }

        open val isValid: Boolean get() = this is Valid

        fun merge(other: ValidationResult): ValidationResult = when {
            this is Valid && other is Valid -> Valid
            this is Invalid && other is Invalid -> Invalid(this.violations + other.violations)
            this is Invalid -> this
            else -> other
        }
    }

    data class Violation(
        val field: String,
        val message: String,
        val code: String,
        val severity: Severity = Severity.ERROR,
        val value: Any? = null
    )

    enum class Severity { WARNING, ERROR, CRITICAL }

    interface Rule<T> {
        val name: String
        val field: String
        fun validate(value: T): ValidationResult
    }

    class FieldRule<T, V>(
        override val name: String,
        override val field: String,
        private val extractor: (T) -> V,
        private val predicate: (V) -> Boolean,
        private val message: String,
        private val code: String,
        private val severity: Severity = Severity.ERROR
    ) : Rule<T> {
        override fun validate(value: T): ValidationResult {
            val fieldValue = extractor(value)
            return if (predicate(fieldValue)) ValidationResult.Valid
            else ValidationResult.Invalid(listOf(Violation(field, message, code, severity, fieldValue)))
        }
    }

    class CompositeRule<T>(
        override val name: String,
        override val field: String,
        private val rules: List<Rule<T>>,
        private val mode: CompositionMode = CompositionMode.ALL
    ) : Rule<T> {
        enum class CompositionMode { ALL, ANY, NONE }

        override fun validate(value: T): ValidationResult {
            val results = rules.map { it.validate(value) }
            return when (mode) {
                CompositionMode.ALL -> results.fold(ValidationResult.Valid as ValidationResult) { acc, r -> acc.merge(r) }
                CompositionMode.ANY -> if (results.any { it.isValid }) ValidationResult.Valid
                    else results.fold(ValidationResult.Valid as ValidationResult) { acc, r -> acc.merge(r) }
                CompositionMode.NONE -> if (results.none { it.isValid }) ValidationResult.Valid
                    else ValidationResult.Invalid(listOf(Violation(field, "Expected no rules to pass", "NONE_EXPECTED")))
            }
        }
    }

    class ConditionalRule<T>(
        override val name: String,
        override val field: String,
        private val condition: (T) -> Boolean,
        private val thenRule: Rule<T>,
        private val elseRule: Rule<T>? = null
    ) : Rule<T> {
        override fun validate(value: T): ValidationResult =
            if (condition(value)) thenRule.validate(value)
            else elseRule?.validate(value) ?: ValidationResult.Valid
    }

    private val rules = mutableListOf<Rule<T>>()
    private val groupedRules = mutableMapOf<String, MutableList<Rule<T>>>()

    fun addRule(rule: Rule<T>): ValidationFramework1527<T> { rules.add(rule); return this }

    fun <V> field(
        name: String, field: String, extractor: (T) -> V,
        predicate: (V) -> Boolean, message: String, code: String,
        severity: Severity = Severity.ERROR
    ): ValidationFramework1527<T> {
        rules.add(FieldRule(name, field, extractor, predicate, message, code, severity))
        return this
    }

    fun addGroup(group: String, vararg groupRules: Rule<T>): ValidationFramework1527<T> {
        groupedRules.getOrPut(group) { mutableListOf() }.addAll(groupRules)
        return this
    }

    fun validate(value: T): ValidationResult =
        rules.map { it.validate(value) }.fold(ValidationResult.Valid as ValidationResult) { acc, r -> acc.merge(r) }

    fun validateGroup(value: T, group: String): ValidationResult =
        (groupedRules[group] ?: emptyList()).map { it.validate(value) }
            .fold(ValidationResult.Valid as ValidationResult) { acc, r -> acc.merge(r) }

    fun validateAll(values: List<T>): Map<Int, ValidationResult> =
        values.mapIndexed { index, value -> index to validate(value) }
            .filter { !it.second.isValid }.toMap()

    fun getRuleCount(): Int = rules.size + groupedRules.values.sumOf { it.size }
    fun getGroups(): Set<String> = groupedRules.keys.toSet()
}
