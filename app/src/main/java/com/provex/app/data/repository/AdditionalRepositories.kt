package com.provex.app.data.repository

import com.provex.app.data.local.PreferencesStore
import com.provex.app.data.remote.ProvexApi
import com.provex.app.data.remote.toDomain
import com.provex.app.domain.model.GithubRepo
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.LearningPlanItem
import com.provex.app.domain.model.Opportunity
import com.provex.app.domain.model.ProcessingState
import com.provex.app.domain.model.ProofLoopState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.domain.model.SkepticReview
import com.provex.app.domain.model.SkillAssessment
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.repository.GithubRepository
import com.provex.app.domain.repository.InsightsRepository
import com.provex.app.domain.repository.LearningPlanResult
import com.provex.app.domain.repository.LearningRepository
import com.provex.app.domain.repository.SettingsRepository
import com.provex.app.domain.usecase.LearningPlanBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

import com.provex.app.data.remote.SessionTokenHolder
import com.provex.app.domain.model.AuthState

class GithubRepositoryImpl(
    private val prefs: PreferencesStore,
    private val api: ProvexApi?,
    private val tokens: SessionTokenHolder? = null,
) : GithubRepository {

    private val refreshTrigger = MutableStateFlow(0)

    override val state: Flow<GithubState> = combine(
        prefs.authState,
        prefs.sampleRepos,
        prefs.selectedRepos,
        refreshTrigger,
    ) { authState, sampleEnabled, selected, _ ->
        when (authState) {
            is AuthState.SignedOut -> {
                GithubState(
                    status = RepoConnectionStatus.NOT_CONNECTED,
                    accountLogin = null,
                    repos = emptyList(),
                    isSample = false,
                    message = null,
                )
            }
            is AuthState.Demo -> {
                if (sampleEnabled) {
                    GithubState(
                        status = RepoConnectionStatus.CONNECTED,
                        accountLogin = "sample-user (demo preview)",
                        repos = SampleData.repositories(selected),
                        isSample = true,
                        message = "Showing illustrative repositories. No live GitHub account is connected.",
                    )
                } else {
                    GithubState(
                        status = RepoConnectionStatus.NOT_CONNECTED,
                        accountLogin = null,
                        repos = emptyList(),
                        isSample = false,
                        message = null,
                    )
                }
            }
            is AuthState.Account -> {
                when {
                    api == null -> {
                        GithubState(
                            status = RepoConnectionStatus.NOT_CONNECTED,
                            accountLogin = null,
                            repos = emptyList(),
                            isSample = false,
                            message = "GitHub connection requires the PROVEX backend, which is not configured in this build.",
                        )
                    }
                    tokens != null && tokens.token.isNullOrBlank() -> {
                        GithubState(
                            status = RepoConnectionStatus.NOT_CONNECTED,
                            accountLogin = null,
                            repos = emptyList(),
                            isSample = false,
                            message = null,
                        )
                    }
                    else -> {
                        try {
                            val status = api.getGithubStatus()
                            if (status.connected) {
                                val remoteRepos = try {
                                    api.getRepositories().map { it.toDomain(it.id.toString() in selected) }
                                } catch (e: Exception) {
                                    emptyList()
                                }
                                GithubState(
                                    status = RepoConnectionStatus.CONNECTED,
                                    accountLogin = status.githubUsername ?: "Connected user",
                                    repos = remoteRepos,
                                    isSample = false,
                                    message = null,
                                )
                            } else {
                                GithubState(
                                    status = RepoConnectionStatus.NOT_CONNECTED,
                                    accountLogin = null,
                                    repos = emptyList(),
                                    isSample = false,
                                    message = null,
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            if (e is retrofit2.HttpException && e.code() == 401) {
                                GithubState(
                                    status = RepoConnectionStatus.NOT_CONNECTED,
                                    accountLogin = null,
                                    repos = emptyList(),
                                    isSample = false,
                                    message = null,
                                )
                            } else {
                                GithubState(
                                    status = RepoConnectionStatus.ERROR,
                                    accountLogin = null,
                                    repos = emptyList(),
                                    isSample = false,
                                    message = "Could not load GitHub connection: ${e.userMessage()}",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    override suspend fun reset() {
        refreshTrigger.update { it + 1 }
    }

    override suspend fun beginAuthorization(): Result<String> {
        val api = api ?: return Result.failure(
            IllegalStateException(
                "GitHub authorization requires the PROVEX backend service to safely exchange OAuth tokens. " +
                    "The Android client never embeds GitHub client secrets."
            )
        )
        return try {
            val response = api.beginGithubAuthorization()
            Result.success(response.authorizationUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.userMessage()))
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        val api = api ?: return Result.failure(
            IllegalStateException("PROVEX backend not configured.")
        )
        return try {
            api.disconnectGithub()
            refresh()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.userMessage()))
        }
    }

    override suspend fun toggleSelected(id: String) {
        prefs.toggleRepo(id)
    }

    override suspend fun setSampleEnabled(enabled: Boolean) {
        prefs.setSampleRepos(enabled)
    }
}

class LearningRepositoryImpl(
    private val prefs: PreferencesStore,
) : LearningRepository {

    override val completedCriteria: Flow<Set<String>> = prefs.completedCriteria

    override suspend fun plan(
        analysis: JobAnalysis,
        assessments: List<SkillAssessment>,
    ): LearningPlanResult {
        val items = LearningPlanBuilder.build(analysis.displayTitle, assessments)
        val notice = if (items.any { it.illustrative }) {
            "Learning resources and tasks are generated from standard curriculum templates. " +
                "Repository-specific tasks and verified links will be provided once connected to the PROVEX backend."
        } else null
        return LearningPlanResult(items, notice)
    }

    override suspend fun toggleCriterion(key: String) {
        prefs.toggleCriterion(key)
    }
}

class InsightsRepositoryImpl(
    private val prefs: PreferencesStore,
    private val api: ProvexApi?,
) : InsightsRepository {

    override suspend fun skepticReviews(): List<SkepticReview> = emptyList()

    override fun sampleSkepticReviews(): List<SkepticReview> = SampleData.skepticReviews

    override suspend fun proofLoop(): ProofLoopState = ProofLoopState(
        repository = null,
        latestCommit = null,
        changedFiles = emptyList(),
        skillChanges = emptyList(),
        assessedAt = null,
        processing = ProcessingState.NOT_CONNECTED,
        isSample = false,
    )

    override fun sampleProofLoop(): ProofLoopState = SampleData.proofLoop()

    override val sampleOpportunitiesEnabled: Flow<Boolean> = prefs.sampleOpportunities

    override suspend fun setSampleOpportunities(enabled: Boolean) {
        prefs.setSampleOpportunities(enabled)
    }

    override suspend fun realOpportunities(): List<Opportunity> = emptyList()

    override fun sampleOpportunities(profile: UserProfile): List<Opportunity> =
        SampleData.opportunities(profile.targetRole)
}

class SettingsRepositoryImpl(
    private val prefs: PreferencesStore,
    private val authRepo: com.provex.app.domain.repository.AuthRepository,
) : SettingsRepository {

    override val notifyProofLoop: Flow<Boolean> = prefs.notifyProofLoop
    override val notifyLearning: Flow<Boolean> = prefs.notifyLearning

    override suspend fun setNotifyProofLoop(enabled: Boolean) {
        prefs.setNotifyProofLoop(enabled)
    }

    override suspend fun setNotifyLearning(enabled: Boolean) {
        prefs.setNotifyLearning(enabled)
    }

    override suspend fun deleteAllLocalData() {
        prefs.clearAll()
        authRepo.signOut()
    }
}

class JobAnalyzerRepositoryImpl(
    private val api: ProvexApi?,
) : com.provex.app.domain.repository.JobAnalyzerRepository {
    override suspend fun analyzeJob(
        description: String?,
        url: String?,
    ): Result<com.provex.app.domain.model.JobAnalysisResult> {
        val client = api ?: return Result.failure(
            IllegalStateException("Job Skill Analyzer requires the PROVEX backend, which is not configured in this build.")
        )
        return try {
            val req = com.provex.app.data.remote.dto.JobAnalysisRequestDto(
                description = description?.trim()?.ifBlank { null },
                url = url?.trim()?.ifBlank { null },
            )
            val resp = client.analyzeJob(req)
            Result.success(resp.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val message = try {
                if (!errorBody.isNullOrBlank()) {
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(errorBody)
                    if (jsonObj is kotlinx.serialization.json.JsonObject && jsonObj.containsKey("detail")) {
                        jsonObj["detail"]?.toString()?.trim('"') ?: errorBody
                    } else errorBody
                } else "HTTP ${e.code()}"
            } catch (_: Exception) {
                errorBody ?: "HTTP ${e.code()}"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun matchJobGithub(
        skills: List<com.provex.app.domain.model.ExtractedSkillModel>,
    ): Result<com.provex.app.domain.model.GitHubMatchResult> {
        val client = api ?: return Result.failure(
            IllegalStateException("GitHub matching requires the PROVEX backend, which is not configured in this build.")
        )
        return try {
            val req = com.provex.app.data.remote.dto.GitHubMatchRequestDto(
                skills = skills.map {
                    com.provex.app.data.remote.dto.ExtractedSkillDto(
                        name = it.name,
                        category = it.category,
                        importance = it.importance,
                        evidence = it.evidence,
                        confidence = it.confidence,
                    )
                }
            )
            val resp = client.matchJobGithub(req)
            Result.success(resp.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val message = try {
                if (!errorBody.isNullOrBlank()) {
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(errorBody)
                    if (jsonObj is kotlinx.serialization.json.JsonObject && jsonObj.containsKey("detail")) {
                        jsonObj["detail"]?.toString()?.trim('"') ?: errorBody
                    } else errorBody
                } else "HTTP ${e.code()}"
            } catch (_: Exception) {
                errorBody ?: "HTTP ${e.code()}"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
