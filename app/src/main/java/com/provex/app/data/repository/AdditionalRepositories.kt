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

class GithubRepositoryImpl(
    private val prefs: PreferencesStore,
    private val api: ProvexApi?,
) : GithubRepository {

    private val refreshTrigger = MutableStateFlow(0)

    override val state: Flow<GithubState> = combine(
        prefs.sampleRepos,
        prefs.selectedRepos,
        refreshTrigger,
    ) { sampleEnabled, selected, _ ->
        when {
            api == null -> {
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
                        message = "GitHub connection requires the PROVEX backend, which is not configured in this build.",
                    )
                }
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
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

    override suspend fun refresh() {
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
