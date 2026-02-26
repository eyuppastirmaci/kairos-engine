package dev.kairos.engine.model

import dev.kairos.engine.context.WorkflowContext

/**
 * Type alias for the suspend function that a step executes.
 *
 * Receives the shared [WorkflowContext] to read inputs from previous steps
 * and write outputs for downstream steps.
 */
typealias StepAction = suspend (WorkflowContext) -> Unit

/**
 * Immutable definition of a single step within a workflow.
 *
 * A step is the smallest unit of work in Kairos. It has a unique [name]
 * within its workflow, an optional set of [dependencies] (names of steps
 * that must complete before this step can run), and an [action] that
 * contains the actual business logic.
 *
 * ## Identity
 * Two steps are considered equal if they share the same [name], regardless
 * of their action or dependencies. This is intentional: within a single
 * workflow, step names are unique identifiers, and lambda equality is
 * not meaningful in Kotlin.
 *
 * ## Why not a data class?
 * [action] is a lambda, which does not have structural equality. Using a
 * data class would produce misleading equals/hashCode/copy behavior.
 * Instead, equality and hashing are based solely on [name].
 */
class StepDefinition(
    val name: String,
    val dependencies: Set<String> = emptySet(),
    val action: StepAction
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StepDefinition) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String =
        "StepDefinition(name='$name', dependencies=$dependencies)"
}