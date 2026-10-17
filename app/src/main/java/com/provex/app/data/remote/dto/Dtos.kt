package com.provex.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire contracts for the future PROVEX FastAPI backend. Subject to change with the backend.

@Serializable
data class CredentialsDto(val email: String, val password: String)

@Serializable
data class RegisterRequestDto(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val password: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String = "bearer",
    val email: String? = null,
    val user: UserDto? = null,
)

@Serializable
data class RefreshTokenDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
data class CareerProfileDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val level: String = "",
    @SerialName("target_role") val targetRole: String = "",
    val location: String = "",
    @SerialName("work_mode") val workMode: String = "NO_PREFERENCE",
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
)

@Serializable
data class CareerProfileUpdateDto(
    val level: String = "",
    @SerialName("target_role") val targetRole: String = "",
    val location: String = "",
    @SerialName("work_mode") val workMode: String = "NO_PREFERENCE",
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
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
    val state: String? = null,
)

@Serializable
data class GitHubStatusDto(
    val connected: Boolean,
    @SerialName("github_username") val githubUsername: String? = null,
    @SerialName("connected_at") val connectedAt: String? = null,
    val scopes: String? = null,
)

@Serializable
data class GitHubRepoDto(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: String,
    val description: String? = null,
    val language: String? = null,
    val private: Boolean = false,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ResourceDto(val title: String, val url: String, val type: String)

@Serializable
data class RecommendationDto(val skill: String, val resources: List<ResourceDto> = emptyList())
