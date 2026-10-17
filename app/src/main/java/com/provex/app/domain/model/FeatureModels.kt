package com.provex.app.domain.model

import kotlinx.serialization.Serializable

enum class RepoConnectionStatus(val label: String) {
    NOT_CONNECTED("Not connected"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    ERROR("Connection problem"),
}

data class GithubRepo(
    val id: String,
    val fullName: String,
    val language: String?,
    val lastAnalyzedRevision: String?,
    val selected: Boolean,
    val isSample: Boolean,
    val name: String = fullName.substringAfterLast('/'),
    val owner: String = fullName.substringBefore('/', ""),
    val description: String? = null,
    val isPrivate: Boolean = false,
    val htmlUrl: String? = null,
    val defaultBranch: String? = "main",
    val updatedAt: String? = null,
)

data class GithubState(
    val status: RepoConnectionStatus,
    val accountLogin: String?,
    val repos: List<GithubRepo>,
    /** True if the repository list is illustrative data rather than from GitHub. */
    val isSample: Boolean,
    val message: String?,
)

enum class ResourceType(val label: String) {
    TUTORIAL("Tutorial"), DOCS("Official documentation"), COURSE("Course"), CERT("Certification")
}

@Serializable
data class LearningResource(
    val title: String,
    val url: String?,
    val type: ResourceType,
    /** True only when the URL came from verified backend data. */
    val verified: Boolean,
)

@Serializable
data class LearningPlanItem(
    val skill: String,
    val whyItMatters: String,
    val resources: List<LearningResource>,
    val taskTitle: String,
    val taskDescription: String,
    val acceptanceCriteria: List<String>,
    /** Task text is generated from templates, not repository analysis. */
    val illustrative: Boolean,
)

data class SkepticReview(
    val skill: String,
    val initialStatus: EvidenceStatus,
    val initialClaim: String,
    val challenge: String,
    val supportingEvidence: String,
    val validatedStatus: EvidenceStatus,
    val explanation: String,
    val isSample: Boolean,
)

enum class ProcessingState(val label: String) {
    NOT_CONNECTED("No repository connected"),
    IDLE("Idle"),
    PROCESSING("Processing"),
    COMPLETE("Complete"),
}

data class ProofLoopSkillChange(
    val skill: String,
    val reassessed: Boolean,
    val previous: EvidenceStatus,
    val current: EvidenceStatus,
    val note: String,
)

data class ProofLoopState(
    val repository: String?,
    val latestCommit: String?,
    val changedFiles: List<String>,
    val skillChanges: List<ProofLoopSkillChange>,
    val assessedAt: Long?,
    val processing: ProcessingState,
    val isSample: Boolean,
)

data class Opportunity(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val url: String?,
    val isSample: Boolean,
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data class Demo(val name: String? = null, val email: String? = null) : AuthState
    data class Account(val email: String) : AuthState
}
