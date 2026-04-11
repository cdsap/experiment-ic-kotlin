package com.awesomeapp.search.extra

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConfigManager389 {

    sealed class ConfigValue {
        data class StringValue(val value: String) : ConfigValue()
        data class IntValue(val value: Int) : ConfigValue()
        data class LongValue(val value: Long) : ConfigValue()
        data class DoubleValue(val value: Double) : ConfigValue()
        data class BooleanValue(val value: Boolean) : ConfigValue()
        data class ListValue(val value: List<ConfigValue>) : ConfigValue()
        data class MapValue(val value: Map<String, ConfigValue>) : ConfigValue()

        fun asString(): String? = (this as? StringValue)?.value
        fun asInt(): Int? = (this as? IntValue)?.value
        fun asLong(): Long? = (this as? LongValue)?.value
        fun asDouble(): Double? = (this as? DoubleValue)?.value
        fun asBoolean(): Boolean? = (this as? BooleanValue)?.value
    }

    data class ConfigEntry(
        val key: String,
        val value: ConfigValue,
        val source: String,
        val priority: Int = 0,
        val description: String? = null,
        val updatedAt: Long = System.currentTimeMillis()
    )

    data class ConfigChange(
        val key: String,
        val oldValue: ConfigValue?,
        val newValue: ConfigValue?,
        val source: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    interface ConfigSource {
        val name: String
        val priority: Int
        suspend fun load(): Map<String, ConfigValue>
        suspend fun reload(): Map<String, ConfigValue> = load()
    }

    private val entries = mutableMapOf<String, ConfigEntry>()
    private val sources = mutableListOf<ConfigSource>()
    private val mutex = Mutex()
    private val _changes = MutableSharedFlow<ConfigChange>(extraBufferCapacity = 32)
    private val _snapshot = MutableStateFlow<Map<String, ConfigValue>>(emptyMap())
    private val overrides = mutableMapOf<String, ConfigValue>()
    private val validators = mutableMapOf<String, (ConfigValue) -> Boolean>()
    private val changeHistory = mutableListOf<ConfigChange>()
    private var maxHistorySize = 200

    val changes: SharedFlow<ConfigChange> = _changes.asSharedFlow()
    val snapshot: StateFlow<Map<String, ConfigValue>> = _snapshot.asStateFlow()

    fun addSource(source: ConfigSource): ConfigManager389 {
        sources.add(source)
        sources.sortByDescending { it.priority }
        return this
    }

    fun addValidator(key: String, validator: (ConfigValue) -> Boolean): ConfigManager389 {
        validators[key] = validator
        return this
    }

    suspend fun load() = mutex.withLock {
        for (source in sources) {
            val values = source.load()
            for ((key, value) in values) {
                val existing = entries[key]
                if (existing == null || source.priority >= existing.priority) {
                    val validated = validators[key]?.invoke(value) ?: true
                    if (validated) {
                        val old = entries[key]?.value
                        entries[key] = ConfigEntry(key, value, source.name, source.priority)
                        if (old != value) {
                            val change = ConfigChange(key, old, value, source.name)
                            changeHistory.add(change)
                            trimHistory()
                            _changes.emit(change)
                        }
                    }
                }
            }
        }
        updateSnapshot()
    }

    suspend fun reload() = mutex.withLock {
        for (source in sources) {
            val values = source.reload()
            for ((key, value) in values) {
                val existing = entries[key]
                if (existing == null || source.priority >= existing.priority) {
                    val old = entries[key]?.value
                    entries[key] = ConfigEntry(key, value, source.name, source.priority)
                    if (old != value) {
                        val change = ConfigChange(key, old, value, source.name)
                        _changes.emit(change)
                    }
                }
            }
        }
        updateSnapshot()
    }

    suspend fun setOverride(key: String, value: ConfigValue) = mutex.withLock {
        val old = getResolved(key)
        overrides[key] = value
        val change = ConfigChange(key, old, value, "override")
        changeHistory.add(change)
        trimHistory()
        _changes.emit(change)
        updateSnapshot()
    }

    suspend fun removeOverride(key: String) = mutex.withLock {
        overrides.remove(key)
        updateSnapshot()
    }

    fun getString(key: String, default: String = ""): String = getResolved(key)?.asString() ?: default
    fun getInt(key: String, default: Int = 0): Int = getResolved(key)?.asInt() ?: default
    fun getLong(key: String, default: Long = 0L): Long = getResolved(key)?.asLong() ?: default
    fun getDouble(key: String, default: Double = 0.0): Double = getResolved(key)?.asDouble() ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = getResolved(key)?.asBoolean() ?: default

    fun observe(key: String): Flow<ConfigValue?> = _snapshot.map { it[key] }.distinctUntilChanged()

    private fun getResolved(key: String): ConfigValue? = overrides[key] ?: entries[key]?.value
    private fun updateSnapshot() { _snapshot.value = entries.mapValues { overrides[it.key] ?: it.value.value } }
    private fun trimHistory() { while (changeHistory.size > maxHistorySize) changeHistory.removeFirst() }

    fun getHistory(): List<ConfigChange> = changeHistory.toList()
    fun getAllKeys(): Set<String> = entries.keys.toSet()
    fun getEntry(key: String): ConfigEntry? = entries[key]
    fun getSources(): List<String> = sources.map { it.name }
    fun hasKey(key: String): Boolean = getResolved(key) != null
}
