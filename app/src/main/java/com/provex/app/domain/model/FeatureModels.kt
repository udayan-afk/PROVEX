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

data class ExtractedSkillModel(
    val name: String,
    val category: String,
    val importance: String = "Mentioned",
    val evidence: String = "",
    val confidence: Float? = null,
)

data class ProfileComparisonModel(
    val userHasProfile: Boolean = false,
    val matchingSkills: List<String> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val nextSkillsToLearn: List<String> = emptyList(),
    val matchPercentage: Int = 0,
    val disclaimer: String = "",
)

data class JobAnalysisResult(
    val jobTitle: String = "Job Role",
    val company: String? = null,
    val sourceType: String = "pasted_description",
    val sourceUrl: String? = null,
    val summary: String = "",
    val skills: List<ExtractedSkillModel> = emptyList(),
    val skillCountsByCategory: Map<String, Int> = emptyMap(),
    val responsibilities: List<String> = emptyList(),
    val qualifications: List<String> = emptyList(),
    val educationRequirements: List<String> = emptyList(),
    val experienceRequirements: List<String> = emptyList(),
    val resumeKeywords: List<String> = emptyList(),
    val profileComparison: ProfileComparisonModel? = null,
    val githubMatching: GitHubMatchResult? = null,
    val extractionMethod: String = "deterministic_rules",
    val warnings: List<String> = emptyList(),
)

data class SkillMatchItem(
    val skill: String,
    val category: String,
    val importance: String = "Mentioned",
    val matchStatus: String = "Not Found",
    val matchSymbol: String = "○",
    val evidenceCredit: Float = 0.0f,
    val requirementWeight: Int = 1,
    val evidenceSource: String? = null,
    val evidenceReason: String = "",
)

data class GitHubMatchResult(
    val connected: Boolean = false,
    val githubUsername: String? = null,
    val repositoriesAnalyzed: Int = 0,
    val matchScore: Float? = null,
    val scoreLabel: String = "Repository Evidence Match",
    val strongMatchesCount: Int = 0,
    val partialMatchesCount: Int = 0,
    val notFoundCount: Int = 0,
    val preferredMatchesCount: Int = 0,
    val matches: List<SkillMatchItem> = emptyList(),
    val skillsToStrengthen: List<String> = emptyList(),
    val distribution: Map<String, Int> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val disclaimer: String = "Missing repository evidence does not necessarily mean you lack the skill; it indicates no public code artifacts were found in analyzed repositories.",
)
