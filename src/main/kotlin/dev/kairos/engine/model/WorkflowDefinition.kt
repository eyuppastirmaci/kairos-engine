package dev.kairos.engine.model

import dev.kairos.engine.exception.DuplicateStepException
import dev.kairos.engine.exception.StepNotFoundException
import dev.kairos.engine.exception.WorkflowValidationException

/**
 * Immutable blueprint of a workflow.
 *
 * A [WorkflowDefinition] describes *what* a workflow is: its name, version,
 * which steps it contains, and how those steps depend on each other. It does
 * not hold any runtime state; that belongs to the execution layer.
 *
 * ## Lifecycle
 * Definitions are created through the Kotlin DSL builder (Phase 3) and
 * registered into a [WorkflowRegistry] (Phase 6). The execution engine
 * reads a definition and creates a new execution instance each time a
 * workflow is triggered.
 *
 * ## Validation
 * All structural invariants are enforced in the [init] block. If any rule
 * is violated, construction fails immediately with a descriptive exception.
 * This means every [WorkflowDefinition] instance that exists is guaranteed
 * to be structurally valid.
 *
 * ## Invariants enforced at construction time
 * - Name must not be blank.
 * - Version must be a positive integer.
 * - At least one step is required.
 * - Step names must be unique within the workflow.
 * - Every dependency reference must point to an existing step.
 * - No step may depend on itself.
 *
 * ## Identity
 * Two definitions are considered equal if they share the same [name] and
 * [version]. This allows the registry to detect duplicate registrations
 * while ignoring differences in step actions (lambdas have no structural
 * equality in Kotlin).
 *
 * @property name     Unique identifier for this workflow (e.g., "order-processing").
 * @property version  Monotonically increasing version number. Defaults to 1.
 * @property steps    Ordered list of step definitions that make up this workflow.
 * @property metadata Optional descriptive metadata (description, creation time).
 */
class WorkflowDefinition(
    val name: String,
    val version: Int = 1,
    val steps: List<StepDefinition>,
    val metadata: WorkflowMetadata = WorkflowMetadata()
) {

    /**
     * Internal index for O(1) step lookup by name.
     * Derived from [steps], so it is always consistent.
     */
    private val stepIndex: Map<String, StepDefinition> = steps.associateBy { it.name }

    init {
        validate()
    }

    // -- Public API ----------------------------------------------------------

    /** Total number of steps in this workflow. */
    val stepCount: Int get() = steps.size

    /** Set of all step names in this workflow. */
    val stepNames: Set<String> get() = stepIndex.keys

    /**
     * Returns the step with the given [name].
     * @throws StepNotFoundException if no step with that name exists.
     */
    fun getStep(name: String): StepDefinition =
        stepIndex[name]
            ?: throw StepNotFoundException(
                "Step '$name' not found in workflow '${this.name}' v$version. " +
                        "Available steps: $stepNames"
            )

    /**
     * Returns the step with the given [name], or null if it does not exist.
     */
    fun getStepOrNull(name: String): StepDefinition? = stepIndex[name]

    /**
     * Returns true if a step with the given [name] exists in this workflow.
     */
    fun hasStep(name: String): Boolean = name in stepIndex

    /**
     * Returns the names of steps that have no dependencies (entry points).
     * These are the first steps that can be executed.
     */
    fun rootSteps(): Set<String> =
        steps.filter { it.dependencies.isEmpty() }
            .map { it.name }
            .toSet()

    /**
     * Returns the names of steps that depend on the given [stepName].
     * Useful for determining which steps can proceed after a step completes.
     */
    fun dependentsOf(stepName: String): Set<String> =
        steps.filter { stepName in it.dependencies }
            .map { it.name }
            .toSet()

    // -- Validation ----------------------------------------------------------

    private fun validate() {
        validateName()
        validateVersion()
        validateStepsNotEmpty()
        validateUniqueStepNames()
        validateDependencyReferences()
        validateNoSelfDependency()
    }

    private fun validateName() {
        if (name.isBlank()) {
            throw WorkflowValidationException("Workflow name must not be blank")
        }
    }

    private fun validateVersion() {
        if (version <= 0) {
            throw WorkflowValidationException(
                "Workflow '$name' version must be positive, got $version"
            )
        }
    }

    private fun validateStepsNotEmpty() {
        if (steps.isEmpty()) {
            throw WorkflowValidationException(
                "Workflow '$name' must have at least one step"
            )
        }
    }

    private fun validateUniqueStepNames() {
        val duplicates = steps.groupBy { it.name }
            .filter { it.value.size > 1 }
            .keys
        if (duplicates.isNotEmpty()) {
            throw DuplicateStepException(
                "Workflow '$name' contains duplicate step names: $duplicates"
            )
        }
    }

    private fun validateDependencyReferences() {
        val validNames = stepIndex.keys
        steps.forEach { step ->
            val missingDeps = step.dependencies - validNames
            if (missingDeps.isNotEmpty()) {
                throw StepNotFoundException(
                    "Step '${step.name}' in workflow '$name' depends on " +
                            "non-existent steps: $missingDeps. Available steps: $validNames"
                )
            }
        }
    }

    private fun validateNoSelfDependency() {
        steps.forEach { step ->
            if (step.name in step.dependencies) {
                throw WorkflowValidationException(
                    "Step '${step.name}' in workflow '$name' depends on itself"
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkflowDefinition) return false
        return name == other.name && version == other.version
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version
        return result
    }

    override fun toString(): String =
        "WorkflowDefinition(name='$name', version=$version, steps=${steps.map { it.name }})"
}