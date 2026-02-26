package dev.kairos.engine.context

import java.util.concurrent.ConcurrentHashMap

/**
 * Shared mutable context for passing data between workflow steps.
 *
 * Each workflow execution gets its own context instance. Steps write their
 * outputs here, and downstream steps read from it. Thread-safe via
 * [ConcurrentHashMap] to support future parallel step execution.
 *
 * This is a minimal stub. Full implementation (type-safe accessors,
 * snapshot, concurrent access guarantees) will follow in Phase 2.
 */
class WorkflowContext(initialData: Map<String, Any> = emptyMap()) {

    private val data = ConcurrentHashMap<String, Any>(initialData)

    fun put(key: String, value: Any) {
        data[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T =
        data[key] as? T
            ?: throw IllegalArgumentException("Key '$key' not found or type mismatch")

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrNull(key: String): T? = data[key] as? T

    fun contains(key: String): Boolean = data.containsKey(key)

    /**
     * Returns an immutable snapshot of the current context state.
     * Useful for logging, debugging, and result reporting.
     */
    fun snapshot(): Map<String, Any> = data.toMap()
}