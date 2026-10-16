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

interface AppContainer {
    val authRepository: AuthRepository
    val profileRepository: ProfileRepository
    val analysisRepository: AnalysisRepository
    val githubRepository: GithubRepository
    val learningRepository: LearningRepository
    val insightsRepository: InsightsRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: ProvexDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            ProvexDatabase::class.java,
            "provex.db",
        ).fallbackToDestructiveMigration().build()
    }

    private val preferencesStore: PreferencesStore by lazy {
        PreferencesStore(context.applicationContext)
    }

    private val tokenHolder = SessionTokenHolder()

    private val api by lazy {
        NetworkModule.createApi(tokenHolder)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(preferencesStore, api, tokenHolder)
    }

    override val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(database.profileDao())
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

    override val githubRepository: GithubRepository by lazy {
        GithubRepositoryImpl(preferencesStore, api)
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
}
