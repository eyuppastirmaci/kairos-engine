package dev.kairos.engine.exception

/**
 * Base exception for all Kairos Engine errors.
 *
 * Sealed so that consumers can exhaustively match on error types
 * with a when-expression. Each subclass represents a distinct
 * failure category in the engine lifecycle.
 */
sealed class KairosException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Thrown when a workflow definition contains invalid structure.
 * Examples: missing step names, empty step list, action not defined.
 */
class WorkflowValidationException(message: String) : KairosException(message)

/**
 * Thrown when a step references a dependency that does not exist
 * in the workflow definition.
 */
class StepNotFoundException(message: String) : KairosException(message)

/**
 * Thrown when duplicate step names are detected within a single
 * workflow definition.
 */
class DuplicateStepException(message: String) : KairosException(message)

/**
 * Thrown when the dependency graph contains a cycle, making
 * topological ordering impossible.
 */
class CyclicDependencyException(message: String) : KairosException(message)

/**
 * Thrown when a step fails during execution.
 * Wraps the original cause for inspection and logging.
 */
class StepExecutionException(
    val stepName: String,
    message: String,
    cause: Throwable? = null
) : KairosException(message, cause)

/**
 * Thrown when a [WorkflowContext] read fails due to a missing key
 * or a type mismatch.
 */
class WorkflowContextException(message: String) : KairosException(message)