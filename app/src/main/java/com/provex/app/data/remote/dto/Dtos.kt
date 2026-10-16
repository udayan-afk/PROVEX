package com.provex.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire contracts for the future PROVEX FastAPI backend. Subject to change with the backend.

@Serializable
data class CredentialsDto(val email: String, val password: String)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    val email: String? = null,
)

@Serializable
data class JobSubmissionDto(
    @SerialName("job_text") val jobText: String,
    @SerialName("job_url") val jobUrl: String? = null,
    @SerialName("profile_skills") val profileSkills: List<String> = emptyList(),
    @SerialName("repository_ids") val repositoryIds: List<String> = emptyList(),
)

@Serializable
data class RequirementDto(
    val name: String,
    val category: String,
    val importance: String,
    @SerialName("source_text") val sourceText: String? = null,
)

@Serializable
data class AnalysisDto(
    val id: String,
    val title: String? = null,
    val company: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val requirements: List<RequirementDto> = emptyList(),
)

@Serializable
data class CitationDto(
    val repository: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("start_line") val startLine: Int,
    @SerialName("end_line") val endLine: Int,
    val snippet: String,
    val commit: String,
    /** True only if the backend verified the snippet against the analyzed revision. */
    val validated: Boolean = false,
)

@Serializable
data class SkillEvidenceDto(
    val skill: String,
    val status: String,
    val explanation: String = "",
    val limitations: List<String> = emptyList(),
    val confidence: String? = null,
    val citations: List<CitationDto> = emptyList(),
)

@Serializable
data class EvidenceResponseDto(
    val revision: String? = null,
    val skills: List<SkillEvidenceDto> = emptyList(),
)

@Serializable
data class RepositoryDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val language: String? = null,
    @SerialName("last_analyzed_revision") val lastAnalyzedRevision: String? = null,
)

@Serializable
data class GithubAuthorizationDto(
    /** URL on github.com the user should open in the browser to authorize PROVEX. */
    @SerialName("authorization_url") val authorizationUrl: String,
)

@Serializable
data class ResourceDto(val title: String, val url: String, val type: String)

@Serializable
data class RecommendationDto(val skill: String, val resources: List<ResourceDto> = emptyList())
