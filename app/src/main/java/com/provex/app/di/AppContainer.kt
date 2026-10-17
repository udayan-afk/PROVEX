package com.provex.app.di

import android.content.Context
import androidx.room.Room
import com.provex.app.data.local.PreferencesStore
import com.provex.app.data.local.ProvexDatabase
import com.provex.app.data.remote.NetworkModule
import com.provex.app.data.remote.SessionTokenHolder
import com.provex.app.data.repository.AnalysisRepositoryImpl
import com.provex.app.data.repository.AuthRepositoryImpl
import com.provex.app.data.repository.GithubRepositoryImpl
import com.provex.app.data.repository.InsightsRepositoryImpl
import com.provex.app.data.repository.LearningRepositoryImpl
import com.provex.app.data.repository.ProfileRepositoryImpl
import com.provex.app.data.repository.SettingsRepositoryImpl
import com.provex.app.domain.repository.AnalysisRepository
import com.provex.app.domain.repository.AuthRepository
import com.provex.app.domain.repository.GithubRepository
import com.provex.app.domain.repository.InsightsRepository
import com.provex.app.domain.repository.LearningRepository
import com.provex.app.domain.repository.ProfileRepository
import com.provex.app.domain.repository.SettingsRepository

class JobDraftHolder {
    var draftJobText: String? = null
    var draftJobUrl: String? = null

    fun consumeDraft(): Pair<String?, String?> {
        val text = draftJobText
        val url = draftJobUrl
        draftJobText = null
        draftJobUrl = null
        return Pair(text, url)
    }

    fun setDraft(text: String, url: String? = null) {
        draftJobText = text
        draftJobUrl = url
    }
}

interface AppContainer {
    val authRepository: AuthRepository
    val profileRepository: ProfileRepository
    val analysisRepository: AnalysisRepository
    val githubRepository: GithubRepository
    val learningRepository: LearningRepository
    val insightsRepository: InsightsRepository
    val settingsRepository: SettingsRepository
    val jobDraftHolder: JobDraftHolder
    val jobAnalyzerRepository: com.provex.app.domain.repository.JobAnalyzerRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val jobDraftHolder: JobDraftHolder by lazy {
        JobDraftHolder()
    }

    private val database: ProvexDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            ProvexDatabase::class.java,
            "provex.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private val preferencesStore: PreferencesStore by lazy {
        PreferencesStore(context.applicationContext)
    }

    private val secureSessionStorage: com.provex.app.data.local.SecureSessionStorage by lazy {
        com.provex.app.data.local.SecureSessionStorage(context.applicationContext)
    }

    private val tokenHolder = SessionTokenHolder()

    private val api by lazy {
        NetworkModule.createApi(tokenHolder)
    }

    override val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(
            dao = database.profileDao(),
            apiProvider = { api },
        )
    }

    override val githubRepository: GithubRepository by lazy {
        GithubRepositoryImpl(preferencesStore, api, tokenHolder)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            prefs = preferencesStore,
            secureStorage = secureSessionStorage,
            api = api,
            tokens = tokenHolder,
            profileRepo = profileRepository,
            githubRepo = githubRepository,
        )
    }

    override val analysisRepository: AnalysisRepository by lazy {
        AnalysisRepositoryImpl(
            analysisDao = database.analysisDao(),
            snapshotDao = database.snapshotDao(),
            prefs = preferencesStore,
            profileRepo = profileRepository,
            api = api,
        )
    }

    override val learningRepository: LearningRepository by lazy {
        LearningRepositoryImpl(preferencesStore)
    }

    override val insightsRepository: InsightsRepository by lazy {
        InsightsRepositoryImpl(preferencesStore, api)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesStore, authRepository)
    }

    override val jobAnalyzerRepository: com.provex.app.domain.repository.JobAnalyzerRepository by lazy {
        com.provex.app.data.repository.JobAnalyzerRepositoryImpl(api)
    }
}
