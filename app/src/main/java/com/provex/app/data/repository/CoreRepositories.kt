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
import com.provex.app.domain.model.WorkMode
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
    is HttpException -> {
        val serverDetail = try {
            response()?.errorBody()?.string()?.let { bodyStr ->
                val regex = """"detail"\s*:\s*"([^"]+)"""".toRegex()
                regex.find(bodyStr)?.groupValues?.get(1)
            }
        } catch (_: Exception) {
            null
        }
        when {
            !serverDetail.isNullOrBlank() -> serverDetail
            code() == 401 -> "Incorrect email or password."
            code() == 409 -> "An account with that email already exists."
            code() == 503 -> "The PROVEX backend GitHub service is not configured or unavailable."
            else -> "The PROVEX backend returned an error (HTTP ${code()})."
        }
    }
    is IOException -> "Can't reach the PROVEX backend. Check your connection and try again."
    else -> message ?: "Something went wrong."
}

class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val apiProvider: () -> ProvexApi?,
) : ProfileRepository {
    override val profile: Flow<UserProfile> = dao.observe().map { e ->
        e?.let { runCatching { json.decodeFromString<UserProfile>(it.json) }.getOrNull() } ?: UserProfile()
    }

    private fun parseWorkMode(modeStr: String, default: WorkMode): WorkMode {
        return try {
            WorkMode.valueOf(modeStr)
        } catch (_: Exception) {
            default
        }
    }

    override suspend fun save(profile: UserProfile): Result<Unit> {
        // Save locally first for responsiveness
        dao.upsert(ProfileEntity(0, json.encodeToString(profile)))

        val api = apiProvider() ?: return Result.success(Unit) // Offline / demo mode succeeds locally
        return try {
            val body = com.provex.app.data.remote.dto.CareerProfileUpdateDto(
                level = profile.level,
                targetRole = profile.targetRole,
                location = profile.location,
                workMode = profile.workMode.name,
                skills = profile.skills,
                languages = profile.languages,
                frameworks = profile.frameworks,
                interests = profile.interests,
            )
            val updated = api.updateProfile(body)
            val merged = profile.copy(
                name = updated.name?.takeIf { it.isNotBlank() } ?: profile.name,
                level = updated.level,
                targetRole = updated.targetRole,
                location = updated.location,
                workMode = parseWorkMode(updated.workMode, profile.workMode),
                skills = updated.skills,
                languages = updated.languages,
                frameworks = updated.frameworks,
                interests = updated.interests,
            )
            dao.upsert(ProfileEntity(0, json.encodeToString(merged)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncFromRemote(): Result<UserProfile> {
        val api = apiProvider()
        if (api == null) {
            val local = profile.first()
            return Result.success(local)
        }

        return try {
            val remote = api.getProfile()
            val local = profile.first()
            val merged = local.copy(
                name = remote.name?.takeIf { it.isNotBlank() } ?: local.name,
                level = remote.level,
                targetRole = remote.targetRole,
                location = remote.location,
                workMode = parseWorkMode(remote.workMode, local.workMode),
                skills = remote.skills,
                languages = remote.languages,
                frameworks = remote.frameworks,
                interests = remote.interests,
            )
            dao.upsert(ProfileEntity(0, json.encodeToString(merged)))
            Result.success(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // Profile doesn't exist on server yet, return current local
                Result.success(profile.first())
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLocal() {
        dao.clear()
    }
}

class AuthRepositoryImpl(
    private val prefs: PreferencesStore,
    private val secureStorage: com.provex.app.data.local.SecureSessionStorage,
    private val api: ProvexApi?,
    private val tokens: SessionTokenHolder,
    private val profileRepo: ProfileRepository,
    private val githubRepo: com.provex.app.domain.repository.GithubRepository? = null,
) : AuthRepository {
    override val authState: Flow<AuthState> = prefs.authState
    override val rememberMe: Flow<Boolean> = prefs.rememberMe
    override val backendConfigured: Boolean get() = api != null

    private val notConfigured = Result.failure<Unit>(
        IllegalStateException(
            "PROVEX account sign-in needs the PROVEX backend, which isn't configured in this build. " +
                "Use demo mode to explore the app with local data only."
        )
    )

    override suspend fun signIn(email: String, password: String, rememberMe: Boolean): Result<Unit> {
        val api = api ?: return notConfigured
        return try {
            val body = CredentialsDto(email.trim(), password)
            val res = api.login(body)
            handleSuccessfulAuth(res, email.trim(), rememberMe)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.userMessage()))
        }
    }

    override suspend fun register(fullName: String, email: String, password: String): Result<Unit> {
        val api = api ?: return notConfigured
        return try {
            val body = com.provex.app.data.remote.dto.RegisterRequestDto(
                fullName = fullName.trim(),
                email = email.trim(),
                password = password,
            )
            val res = api.register(body)
            handleSuccessfulAuth(res, email.trim(), rememberMe = true)
            // Pre-seed profile with name
            val current = profileRepo.profile.first()
            profileRepo.save(current.copy(name = fullName.trim()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.userMessage()))
        }
    }

    private suspend fun handleSuccessfulAuth(res: com.provex.app.data.remote.dto.AuthResponseDto, fallbackEmail: String, rememberMe: Boolean) {
        tokens.token = res.accessToken
        val email = res.user?.email ?: res.email ?: fallbackEmail
        if (rememberMe) {
            secureStorage.saveTokens(
                accessToken = res.accessToken,
                refreshToken = res.refreshToken,
                email = email,
            )
            prefs.setAccount(email, remember = true)
        } else {
            secureStorage.clear()
            prefs.setAccount(email, remember = false)
        }
        githubRepo?.refresh()
    }

    override suspend fun restoreSession(): Result<AuthState> {
        val savedEmail = secureStorage.getUserEmail()
        val savedAccess = secureStorage.getAccessToken()
        val savedRefresh = secureStorage.getRefreshToken()

        if (savedEmail.isNullOrBlank() || (savedAccess.isNullOrBlank() && savedRefresh.isNullOrBlank())) {
            return Result.success(AuthState.SignedOut)
        }

        val api = api
        if (api == null) {
            // Offline or backend unconfigured: if we have a saved email, return account state
            return Result.success(AuthState.Account(savedEmail))
        }

        // Try validating access token via /me
        if (!savedAccess.isNullOrBlank()) {
            tokens.token = savedAccess
            try {
                val me = api.getMe()
                prefs.setAccount(me.email, remember = true)
                githubRepo?.refresh()
                return Result.success(AuthState.Account(me.email))
            } catch (e: Exception) {
                // Access token expired, attempt refresh
            }
        }

        // Attempt refresh token rotation
        if (!savedRefresh.isNullOrBlank()) {
            try {
                val refreshRes = api.refreshToken(com.provex.app.data.remote.dto.RefreshTokenDto(savedRefresh))
                handleSuccessfulAuth(refreshRes, savedEmail, rememberMe = true)
                val finalEmail = refreshRes.user?.email ?: refreshRes.email ?: savedEmail
                return Result.success(AuthState.Account(finalEmail))
            } catch (e: Exception) {
                // Refresh token invalid or revoked
                tokens.token = null
                secureStorage.clear()
                prefs.clearAuth()
                prefs.clearUserScopedData()
                profileRepo.clearLocal()
                githubRepo?.reset()
                return Result.success(AuthState.SignedOut)
            }
        }

        return Result.success(AuthState.SignedOut)
    }

    override suspend fun enterDemoMode(name: String?, email: String?) {
        tokens.token = null
        secureStorage.clear()
        prefs.clearUserScopedData()
        profileRepo.clearLocal()
        githubRepo?.reset()
        prefs.setDemoMode(name, email)
        if (!name.isNullOrBlank()) {
            val current = profileRepo.profile.first()
            if (current.name.isBlank()) {
                profileRepo.save(current.copy(name = name.trim()))
            }
        }
    }

    override suspend fun setRememberMe(remember: Boolean) {
        prefs.setRememberMe(remember)
    }

    override suspend fun signOut() {
        val refreshToken = secureStorage.getRefreshToken()
        tokens.token = null
        secureStorage.clear()
        prefs.clearAuth()
        prefs.clearUserScopedData()
        profileRepo.clearLocal()
        githubRepo?.reset()

        // Asynchronously notify backend to revoke refresh token
        api?.let { backend ->
            try {
                backend.logout(com.provex.app.data.remote.dto.LogoutRequestDto(refreshToken = refreshToken))
            } catch (_: Exception) {
                // Best-effort remote revocation; local session is already purged
            }
        }
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
