package com.provex.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkMode(val label: String) {
    NO_PREFERENCE("No preference"),
    REMOTE("Remote"),
    HYBRID("Hybrid"),
    ON_SITE("On-site"),
}

/** Skills the user *claims* to know. This is self-reported and never treated as verified. */
@Serializable
data class UserProfile(
    val name: String = "",
    val level: String = "",
    val targetRole: String = "",
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val location: String = "",
    val workMode: WorkMode = WorkMode.NO_PREFERENCE,
    val interests: List<String> = emptyList(),
) {
    val claimedSkills: List<String>
        get() = (skills + languages + frameworks).distinctBy { it.lowercase() }

    val isEmpty: Boolean
        get() = name.isBlank() && targetRole.isBlank() && claimedSkills.isEmpty()

    fun claims(skill: String): Boolean = claimedSkills.any { it.equals(skill, ignoreCase = true) }
}

@Serializable
enum class RequirementCategory(val label: String) {
    LANGUAGE("Programming languages"),
    FRAMEWORK("Frameworks"),
    DATABASE("Databases"),
    TESTING("Testing"),
    CLOUD_DEPLOY("Deployment & cloud"),
    GENERAL("Other technical skills"),
    EXPERIENCE("Experience & qualifications"),
}

@Serializable
enum class Importance(val label: String) {
    REQUIRED("Required"),
    PREFERRED("Preferred"),
    MENTIONED("Mentioned"),
}

@Serializable
data class JobRequirement(
    val id: String,
    val name: String,
    val category: RequirementCategory,
    val importance: Importance,
    /** The text from the posting this requirement was derived from, when known. */
    val sourceText: String? = null,
)

@Serializable
enum class DataOrigin(val label: String) {
    ON_DEVICE("On-device keyword extraction"),
    BACKEND("PROVEX backend analysis"),
}

@Serializable
data class JobAnalysis(
    val id: String,
    val createdAt: Long,
    val title: String? = null,
    val company: String? = null,
    val sourceUrl: String? = null,
    val rawText: String,
    val requirements: List<JobRequirement>,
    val origin: DataOrigin = DataOrigin.ON_DEVICE,
    val userEdited: Boolean = false,
) {
    val displayTitle: String get() = title?.takeIf { it.isNotBlank() } ?: "Untitled opportunity"
    val comparableRequirements: List<JobRequirement>
        get() = requirements.filter { it.category != RequirementCategory.EXPERIENCE }
}

@Serializable
enum class EvidenceStatus(val label: String) {
    STRONG("Strong evidence"),
    PARTIAL("Partial evidence"),
    NOT_DEMONSTRATED("Not demonstrated in analyzed scope"),
    NEEDS_VERIFICATION("Needs verification"),
}

@Serializable
enum class GithubEvidenceState(val label: String) {
    NOT_ANALYZED("No repository analysis yet"),
    NO_EVIDENCE_FOUND("No evidence found in analyzed scope"),
    EVIDENCE_FOUND("Evidence found"),
}

@Serializable
enum class Confidence(val label: String) { LOW("Low"), MEDIUM("Medium"), HIGH("High") }

/** A code citation. Only ever created from backend data with [validated] == true. */
@Serializable
data class Citation(
    val repository: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val snippet: String,
    val commit: String,
    val validated: Boolean,
)

@Serializable
data class SkillAssessment(
    val skill: String,
    val category: RequirementCategory,
    val importance: Importance,
    val inProfile: Boolean,
    val github: GithubEvidenceState,
    val status: EvidenceStatus,
    val explanation: String,
    val limitations: List<String> = emptyList(),
    val confidence: Confidence? = null,
    val citations: List<Citation> = emptyList(),
)

@Serializable
data class AssessmentSnapshot(
    val id: String,
    val analysisId: String,
    val createdAt: Long,
    val jobTitle: String,
    val trigger: String,
    val revision: String? = null,
    val changedFiles: List<String> = emptyList(),
    val assessments: List<SkillAssessment>,
)

data class SnapshotChange(
    val skill: String,
    val previous: EvidenceStatus?,
    val current: EvidenceStatus?,
)

/** Differences between two snapshots of the same analysis. */
fun AssessmentSnapshot.changesFrom(previous: AssessmentSnapshot?): List<SnapshotChange> {
    if (previous == null) return emptyList()
    val before = previous.assessments.associateBy { it.skill.lowercase() }
    val after = assessments.associateBy { it.skill.lowercase() }
    return (before.keys + after.keys).mapNotNull { key ->
        val p = before[key]?.status
        val c = after[key]?.status
        if (p != c) SnapshotChange((after[key] ?: before[key])!!.skill, p, c) else null
    }
}
