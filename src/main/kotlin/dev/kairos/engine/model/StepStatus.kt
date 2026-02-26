package dev.kairos.engine.model

/**
 * Represents the runtime state of a single step within a workflow execution.
 *
 * State transitions for a step:
 *
 * ```
 * PENDING ──> RUNNING ──> COMPLETED   (happy path)
 *                │
 *                └──> FAILED          (action threw an exception)
 *
 * PENDING ──> SKIPPED                 (upstream step failed, this step was never attempted)
 * ```
 *
 * ## Relationship with [WorkflowStatus]
 * - All steps COMPLETED → workflow COMPLETED
 * - Any step FAILED → workflow FAILED, remaining steps become SKIPPED
 * - Steps that never ran stay PENDING until marked SKIPPED by the engine
 */
enum class StepStatus {

    /** Step is waiting to be executed. Dependencies may not have completed yet. */
    PENDING,

    /** Step action is currently executing. */
    RUNNING,

    /** Step action completed without throwing an exception. */
    COMPLETED,

    /** Step action threw an exception. */
    FAILED,

    /**
     * Step was never executed because an upstream step failed.
     * This is not an error — it means the engine correctly stopped
     * execution before reaching this step.
     */
    SKIPPED;

    /** Returns true if this is a terminal state (no further transitions possible). */
    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED || this == SKIPPED
}