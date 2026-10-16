package com.provex.app.data.repository

import com.provex.app.data.local.AnalysisDao
import com.provex.app.data.local.AnalysisEntity
import com.provex.app.data.local.PreferencesStore
import com.provex.app.data.local.ProfileDao
import com.provex.app.data.local.ProfileEntity
import com.provex.app.data.local.SnapshotDao
import com.provex.app.data.local.SnapshotEntity
import com.provex.app.data.remote.NetworkModule
import com.provex.app.data.remote.ProvexApi
import com.provex.app.data.remote.SessionTokenHolder
import com.provex.app.data.remote.dto.CredentialsDto
import com.provex.app.data.remote.dto.JobSubmissionDto
import com.provex.app.data.remote.toBundle
import com.provex.app.data.remote.toDomain
import com.provex.app.domain.model.AssessmentSnapshot
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.JobRequirement
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.model.DataOrigin
import com.provex.app.domain.repository.AnalysisOutcome
import com.provex.app.domain.repository.AnalysisRepository
import com.provex.app.domain.repository.AssessmentResult
import com.provex.app.domain.repository.AuthRepository
import com.provex.app.domain.repository.ProfileRepository
import com.provex.app.domain.usecase.AssessmentEngine
import com.provex.app.domain.usecase.EvidenceBundle
import com.provex.app.domain.usecase.RequirementExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

private val json = NetworkModule.json

fun Throwable.userMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Incorrect email or password."
        409 -> "An account with that email already exists."
        else -> "The PROVEX backend returned an error (HTTP ${code()})."
    }
    is IOException -> "Can't reach the PROVEX backend. Check your connection and try again."
    else -> message ?: "Something went wrong."
}

class ProfileRepositoryImpl(private val dao: ProfileDao) : ProfileRepository {
    override val profile: Flow<UserProfile> = dao.observe().map { e ->
        e?.let { runCatching { json.decodeFromString<UserProfile>(it.json) }.getOrNull() } ?: UserProfile()
    }

    override suspend fun save(profile: UserProfile) {
        dao.upsert(ProfileEntity(0, json.encodeToString(profile)))
    }
}

class AuthRepositoryImpl(
    private val prefs: PreferencesStore,
    private val api: ProvexApi?,
    private val tokens: SessionTokenHolder,
) : AuthRepository {
    override val authState: Flow<AuthState> = prefs.authState
    override val backendConfigured: Boolean get() = api != null

    private val notConfigured = Result.failure<Unit>(
        IllegalStateException(
            "PROVEX account sign-in needs the PROVEX backend, which isn't configured in this build. " +
                "Use demo mode to explore the app with local data only."
        )
    )

    override suspend fun signIn(email: String, password: String): Result<Unit> = authenticate(email, password, register = false)
    override suspend fun register(email: String, password: String): Result<Unit> = authenticate(email, password, register = true)

    private suspend fun authenticate(email: String, password: String, register: Boolean): Result<Unit> {
        val api = api ?: return notConfigured
        return try {
            val body = CredentialsDto(email.trim(), password)
            val res = if (register) api.register(body) else api.login(body)
            tokens.token = res.accessToken
            prefs.setAccount(res.email ?: email.trim())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.userMessage()))
        }
    }

    override suspend fun enterDemoMode() = prefs.setDemoMode().let { }

    override suspend fun signOut() {
        tokens.token = null
        prefs.clearAuth()
    }
}

class AnalysisRepositoryImpl(
    private val analysisDao: AnalysisDao,
    private val snapshotDao: SnapshotDao,
    private val prefs: PreferencesStore,
    private val profileRepo: ProfileRepository,
    private val api: ProvexApi?,
) : AnalysisRepository {

    override val analyses: Flow<List<JobAnalysis>> = analysisDao.observeAll().map { rows ->
        rows.mapNotNull { runCatching { json.decodeFromString<JobAnalysis>(it.json) }.getOrNull() }
    }

    override val snapshots: Flow<List<AssessmentSnapshot>> = snapshotDao.observeAll().map { rows ->
        rows.mapNotNull { runCatching { json.decodeFromString<AssessmentSnapshot>(it.json) }.getOrNull() }
    }

    override fun observeAnalysis(id: String): Flow<JobAnalysis?> = analysisDao.observe(id).map { e ->
        e?.let { runCatching { json.decodeFromString<JobAnalysis>(it.json) }.getOrNull() }
    }

    override suspend fun analyze(jobText: String, jobUrl: String?): Result<AnalysisOutcome> {
        val text = jobText.trim()
        val url = jobUrl?.trim()?.takeIf { it.isNotEmpty() }
        val now = System.currentTimeMillis()
        var notice: String? = null

        var analysis: JobAnalysis? = null
        if (api != null) {
            try {
                val profile = profileRepo.profile.first()
                val repoIds = prefs.selectedRepos.first().filterNot { it.startsWith("sample-") }
                analysis = api.submitJob(JobSubmissionDto(text, url, profile.claimedSkills, repoIds))
                    .toDomain(now, text).copy(sourceUrl = url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = "${e.userMessage()} Used on-device keyword extraction instead."
            }
        } else {
            notice = "No PROVEX backend configured. Requirements were found by on-device keyword matching only; " +
                "please review them."
        }

        if (analysis == null) {
            val extraction = RequirementExtractor.extract(text)
            if (extraction.requirements.isEmpty()) {
                return Result.failure(
                    IllegalStateException(
                        "No recognizable skills or requirements were found in that text. " +
                            "Try pasting the full requirements section, or add requirements manually after review."
                    )
                )
            }
            analysis = JobAnalysis(
                id = UUID.randomUUID().toString(),
                createdAt = now,
                title = extraction.title,
                company = extraction.company,
                sourceUrl = url,
                rawText = text,
                requirements = extraction.requirements,
                origin = DataOrigin.ON_DEVICE,
            )
        }
        analysisDao.upsert(AnalysisEntity(analysis.id, analysis.createdAt, json.encodeToString(analysis)))
        saveSnapshot(analysis, assess(analysis), "Initial analysis")
        return Result.success(AnalysisOutcome(analysis, notice))
    }

    override suspend fun saveEdits(id: String, title: String?, company: String?, requirements: List<JobRequirement>) {
        val current = analysisDao.get(id)?.let { json.decodeFromString<JobAnalysis>(it.json) } ?: return
        val updated = current.copy(
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            company = company?.trim()?.takeIf { it.isNotEmpty() },
            requirements = requirements,
            userEdited = true,
        )
        analysisDao.upsert(AnalysisEntity(id, updated.createdAt, json.encodeToString(updated)))
        saveSnapshot(updated, assess(updated), "Requirements edited by you")
    }

    override suspend fun assess(analysis: JobAnalysis): AssessmentResult {
        val profile = profileRepo.profile.first()
        var bundle = EvidenceBundle.NONE
        var notice: String? = null
        val selected = prefs.selectedRepos.first().filterNot { it.startsWith("sample-") }
        if (api == null) {
            notice = "GitHub evidence needs the PROVEX backend, which isn't configured. " +
                "Skills are compared against your profile only."
        } else if (selected.isEmpty()) {
            notice = "No repositories selected for analysis. Select repositories to look for code evidence."
        } else if (analysis.origin == DataOrigin.BACKEND) {
            try {
                bundle = api.getEvidence(analysis.id).toBundle()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = "Couldn't retrieve GitHub evidence. ${e.userMessage()}"
            }
        } else {
            notice = "This analysis was created on-device, so the backend can't attach repository evidence to it."
        }
        return AssessmentResult(AssessmentEngine.assess(analysis, profile, bundle), bundle.revision, notice)
    }

    override suspend fun saveSnapshot(analysis: JobAnalysis, result: AssessmentResult, trigger: String) {
        val snapshot = AssessmentSnapshot(
            id = UUID.randomUUID().toString(),
            analysisId = analysis.id,
            createdAt = System.currentTimeMillis(),
            jobTitle = analysis.displayTitle,
            trigger = trigger,
            revision = result.revision,
            changedFiles = emptyList(),
            assessments = result.assessments,
        )
        snapshotDao.insert(SnapshotEntity(snapshot.id, snapshot.analysisId, snapshot.createdAt, json.encodeToString(snapshot)))
    }
}
