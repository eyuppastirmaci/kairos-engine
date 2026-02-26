package dev.kairos.engine.model

import java.time.Instant

/**
 * Non-functional metadata attached to a [WorkflowDefinition].
 *
 * Contains descriptive and administrative information that does not
 * affect execution behavior. Useful for registry listings, logging,
 * and operational dashboards.
 *
 * @property description Human-readable summary of what the workflow does.
 * @property createdAt    Timestamp when the definition was built. Defaults to now.
 */
data class WorkflowMetadata(
    val description: String = "",
    val createdAt: Instant = Instant.now()
)