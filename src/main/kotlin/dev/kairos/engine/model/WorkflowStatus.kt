package dev.kairos.engine.model

/**
 * Represents the runtime state of a workflow execution.
 *
 * State transitions follow a strict lifecycle:
 *
 * ```
 * PENDING ──> RUNNING ──> COMPLETED   (happy path)
 *                │
 *                └──> FAILED          (step failure, no compensation yet)
 * ```
 *
 * Future features will introduce additional states:
 * - COMPENSATING: Saga rollback in progress
 * - COMPENSATED: All compensations succeeded
 * - FAILED_WITH_INCONSISTENCY: Compensation itself failed, manual intervention needed
 * - PAUSED / CANCELLED: Operational control via API
 *
 * ## Terminal vs Non-Terminal
 * [COMPLETED] and [FAILED] are terminal states — once reached, no further
 * transitions are possible. [PENDING] and [RUNNING] are transient states
 * that will eventually resolve to a terminal state.
 */
enum class WorkflowStatus {

    /** Workflow has been created but execution has not started yet. */
    PENDING,

    /** Workflow is actively executing steps. */
    RUNNING,

    /** All steps completed successfully. Terminal state. */
    COMPLETED,

    /** One or more steps failed. Terminal state. */
    FAILED;

    /** Returns true if this is a terminal state (no further transitions possible). */
    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED
}