package dev.kairos.engine.model

import java.time.Duration
import java.time.Instant

/**
 * Immutable result of executing a single step.
 *
 * Created by the execution engine after a step completes (or fails/skips).
 * Collected into [WorkflowResult.stepResults] to provide a full execution trace.
 *
 * ## Usage
 * ```kotlin
 * val result = StepResult(
 *     stepName = "charge-payment",
 *     status = StepStatus.COMPLETED,
 *     startedAt = Instant.now(),
 *     completedAt = Instant.now(),
 *     duration = Duration.ofMillis(230)
 * )
 * ```
 *
 * @property stepName    Name of the step (matches [StepDefinition.name]).
 * @property status      Terminal status after execution.
 * @property startedAt   When the step began executing. Null if [StepStatus.SKIPPED].
 * @property completedAt When the step finished. Null if [StepStatus.SKIPPED].
 * @property duration    Wall-clock duration of the step execution. Null if [StepStatus.SKIPPED].
 * @property error       The exception that caused failure. Null unless [StepStatus.FAILED].
 */
data class StepResult(
    val stepName: String,
    val status: StepStatus,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val duration: Duration? = null,
    val error: Throwable? = null
) {

    /** Returns true if this step completed successfully. */
    fun isSuccess(): Boolean = status == StepStatus.COMPLETED

    /** Returns true if this step failed with an exception. */
    fun isFailed(): Boolean = status == StepStatus.FAILED

    /** Returns true if this step was skipped due to an upstream failure. */
    fun isSkipped(): Boolean = status == StepStatus.SKIPPED

    companion object {

        /**
         * Factory for a successful step result.
         * Calculates [duration] automatically from [startedAt] and [completedAt].
         */
        fun success(stepName: String, startedAt: Instant, completedAt: Instant): StepResult =
            StepResult(
                stepName = stepName,
                status = StepStatus.COMPLETED,
                startedAt = startedAt,
                completedAt = completedAt,
                duration = Duration.between(startedAt, completedAt)
            )

        /**
         * Factory for a failed step result.
         * Calculates [duration] automatically from [startedAt] and [completedAt].
         */
        fun failure(
            stepName: String,
            startedAt: Instant,
            completedAt: Instant,
            error: Throwable
        ): StepResult =
            StepResult(
                stepName = stepName,
                status = StepStatus.FAILED,
                startedAt = startedAt,
                completedAt = completedAt,
                duration = Duration.between(startedAt, completedAt),
                error = error
            )

        /**
         * Factory for a skipped step result.
         * No timing information since the step was never executed.
         */
        fun skipped(stepName: String): StepResult =
            StepResult(
                stepName = stepName,
                status = StepStatus.SKIPPED
            )
    }
}