package dev.kairos.engine.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Immutable result of a complete workflow execution.
 *
 * This is the primary return type of the execution engine. It contains
 * everything needed to understand what happened: overall status, per-step
 * results, timing information, the final context snapshot, and any error
 * that caused a failure.
 *
 * ## Usage
 * ```kotlin
 * val result = engine.execute(orderWorkflow, mapOf("orderId" to 42L))
 *
 * when (result.status) {
 *     WorkflowStatus.COMPLETED -> {
 *         val paymentId = result.contextSnapshot["paymentId"]
 *         println("Order processed, payment: $paymentId")
 *     }
 *     WorkflowStatus.FAILED -> {
 *         println("Failed at step: ${result.failedStepName}")
 *         println("Error: ${result.error?.message}")
 *     }
 *     else -> { /* PENDING, RUNNING should not appear in results */ }
 * }
 * ```
 *
 * @property executionId     Unique identifier for this execution instance.
 * @property workflowName    Name of the workflow that was executed.
 * @property workflowVersion Version of the workflow definition used.
 * @property status          Terminal status of the workflow execution.
 * @property stepResults     Per-step results in execution order.
 * @property contextSnapshot Immutable snapshot of the [WorkflowContext] at completion.
 * @property failedStepName  Name of the step that caused the failure. Null on success.
 * @property error           The root cause exception. Null on success.
 * @property startedAt       When the workflow execution began.
 * @property completedAt     When the workflow execution ended.
 * @property duration        Total wall-clock duration of the execution.
 */
data class WorkflowResult(
    val executionId: UUID,
    val workflowName: String,
    val workflowVersion: Int,
    val status: WorkflowStatus,
    val stepResults: Map<String, StepResult>,
    val contextSnapshot: Map<String, Any>,
    val failedStepName: String? = null,
    val error: Throwable? = null,
    val startedAt: Instant,
    val completedAt: Instant,
    val duration: Duration
) {

    /** Returns true if the workflow completed successfully. */
    fun isSuccess(): Boolean = status == WorkflowStatus.COMPLETED

    /** Returns true if the workflow failed. */
    fun isFailed(): Boolean = status == WorkflowStatus.FAILED

    /** Returns the number of steps that completed successfully. */
    fun completedStepCount(): Int =
        stepResults.values.count { it.status == StepStatus.COMPLETED }

    /** Returns the number of steps that were skipped. */
    fun skippedStepCount(): Int =
        stepResults.values.count { it.status == StepStatus.SKIPPED }

    /** Returns the total number of steps in the workflow. */
    fun totalStepCount(): Int = stepResults.size

    /**
     * Returns the result of a specific step by name.
     * @throws IllegalArgumentException if the step name does not exist in results.
     */
    fun stepResult(stepName: String): StepResult =
        stepResults[stepName]
            ?: throw IllegalArgumentException(
                "No result for step '$stepName'. Available: ${stepResults.keys}"
            )

    companion object {

        /**
         * Factory for a successful workflow result.
         * Calculates [duration] automatically.
         */
        fun success(
            executionId: UUID,
            workflowName: String,
            workflowVersion: Int,
            stepResults: Map<String, StepResult>,
            contextSnapshot: Map<String, Any>,
            startedAt: Instant,
            completedAt: Instant
        ): WorkflowResult =
            WorkflowResult(
                executionId = executionId,
                workflowName = workflowName,
                workflowVersion = workflowVersion,
                status = WorkflowStatus.COMPLETED,
                stepResults = stepResults,
                contextSnapshot = contextSnapshot,
                startedAt = startedAt,
                completedAt = completedAt,
                duration = Duration.between(startedAt, completedAt)
            )

        /**
         * Factory for a failed workflow result.
         * Calculates [duration] automatically.
         */
        fun failure(
            executionId: UUID,
            workflowName: String,
            workflowVersion: Int,
            stepResults: Map<String, StepResult>,
            contextSnapshot: Map<String, Any>,
            failedStepName: String,
            error: Throwable,
            startedAt: Instant,
            completedAt: Instant
        ): WorkflowResult =
            WorkflowResult(
                executionId = executionId,
                workflowName = workflowName,
                workflowVersion = workflowVersion,
                status = WorkflowStatus.FAILED,
                stepResults = stepResults,
                contextSnapshot = contextSnapshot,
                failedStepName = failedStepName,
                error = error,
                startedAt = startedAt,
                completedAt = completedAt,
                duration = Duration.between(startedAt, completedAt)
            )
    }
}