package com.provex.app.domain.repository

import com.provex.app.domain.model.AssessmentSnapshot
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.JobRequirement
import com.provex.app.domain.model.LearningPlanItem
import com.provex.app.domain.model.Opportunity
import com.provex.app.domain.model.ProofLoopState
import com.provex.app.domain.model.SkepticReview
import com.provex.app.domain.model.SkillAssessment
import com.provex.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun save(profile: UserProfile)
}

interface AuthRepository {
    val authState: Flow<AuthState>
    /** False when this build has no backend URL; account auth is then unavailable. */
    val backendConfigured: Boolean
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun enterDemoMode()
    suspend fun signOut()
}

data class AnalysisOutcome(val analysis: JobAnalysis, val notice: String?)

data class AssessmentResult(
    val assessments: List<SkillAssessment>,
    val revision: String?,
    val notice: String?,
)

interface AnalysisRepository {
    val analyses: Flow<List<JobAnalysis>>
    val snapshots: Flow<List<AssessmentSnapshot>>
    fun observeAnalysis(id: String): Flow<JobAnalysis?>
    suspend fun analyze(jobText: String, jobUrl: String?): Result<AnalysisOutcome>
    suspend fun saveEdits(id: String, title: String?, company: String?, requirements: List<JobRequirement>)
    suspend fun assess(analysis: JobAnalysis): AssessmentResult
    suspend fun saveSnapshot(analysis: JobAnalysis, result: AssessmentResult, trigger: String)
}

interface GithubRepository {
    val state: Flow<GithubState>
    suspend fun refresh()
    /** Returns a github.com URL to open in the browser, produced by the backend. */
    suspend fun beginAuthorization(): Result<String>
    suspend fun toggleSelected(id: String)
    suspend fun setSampleEnabled(enabled: Boolean)
}

data class LearningPlanResult(val items: List<LearningPlanItem>, val notice: String?)

interface LearningRepository {
    val completedCriteria: Flow<Set<String>>
    suspend fun plan(analysis: JobAnalysis, assessments: List<SkillAssessment>): LearningPlanResult
    suspend fun toggleCriterion(key: String)
}

interface InsightsRepository {
    /** Real reviews from the backend; empty until the backend supports skeptic review. */
    suspend fun skepticReviews(): List<SkepticReview>
    fun sampleSkepticReviews(): List<SkepticReview>
    suspend fun proofLoop(): ProofLoopState
    fun sampleProofLoop(): ProofLoopState
    val sampleOpportunitiesEnabled: Flow<Boolean>
    suspend fun setSampleOpportunities(enabled: Boolean)
    /** Real listings from the backend; empty until listings are supported. */
    suspend fun realOpportunities(): List<Opportunity>
    fun sampleOpportunities(profile: UserProfile): List<Opportunity>
}

interface SettingsRepository {
    val notifyProofLoop: Flow<Boolean>
    val notifyLearning: Flow<Boolean>
    suspend fun setNotifyProofLoop(enabled: Boolean)
    suspend fun setNotifyLearning(enabled: Boolean)
    suspend fun deleteAllLocalData()
}
