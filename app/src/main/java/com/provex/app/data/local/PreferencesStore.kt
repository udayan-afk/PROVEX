package com.provex.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.provex.app.domain.model.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "provex_prefs")

/** Non-sensitive preferences only. Never store tokens or secrets here. */
class PreferencesStore(private val context: Context) {

    private object Keys {
        val authMode = stringPreferencesKey("auth_mode") // "demo" | "account"
        val accountEmail = stringPreferencesKey("account_email")
        val demoName = stringPreferencesKey("demo_name")
        val demoEmail = stringPreferencesKey("demo_email")
        val rememberMe = booleanPreferencesKey("remember_me")
        val notifyProofLoop = booleanPreferencesKey("notify_proofloop")
        val notifyLearning = booleanPreferencesKey("notify_learning")
        val sampleRepos = booleanPreferencesKey("sample_repos")
        val sampleOpportunities = booleanPreferencesKey("sample_opportunities")
        val selectedRepos = stringSetPreferencesKey("selected_repos")
        val completedCriteria = stringSetPreferencesKey("completed_criteria")
    }

    private val data: Flow<Preferences> =
        context.dataStore.data.catch { emit(emptyPreferences()) }

    val authState: Flow<AuthState> = data.map {
        when (it[Keys.authMode]) {
            "demo" -> AuthState.Demo(it[Keys.demoName], it[Keys.demoEmail])
            "account" -> AuthState.Account(it[Keys.accountEmail].orEmpty())
            else -> AuthState.SignedOut
        }
    }
    val rememberMe: Flow<Boolean> = data.map { it[Keys.rememberMe] ?: true }
    val notifyProofLoop: Flow<Boolean> = data.map { it[Keys.notifyProofLoop] ?: false }
    val notifyLearning: Flow<Boolean> = data.map { it[Keys.notifyLearning] ?: false }
    val sampleRepos: Flow<Boolean> = data.map { it[Keys.sampleRepos] ?: false }
    val sampleOpportunities: Flow<Boolean> = data.map { it[Keys.sampleOpportunities] ?: false }
    val selectedRepos: Flow<Set<String>> = data.map { it[Keys.selectedRepos].orEmpty() }
    val completedCriteria: Flow<Set<String>> = data.map { it[Keys.completedCriteria].orEmpty() }

    suspend fun setDemoMode(name: String? = null, email: String? = null) = context.dataStore.edit {
        it[Keys.authMode] = "demo"
        if (name != null) it[Keys.demoName] = name else it.remove(Keys.demoName)
        if (email != null) it[Keys.demoEmail] = email else it.remove(Keys.demoEmail)
        it.remove(Keys.accountEmail)
    }

    suspend fun setAccount(email: String, remember: Boolean = true) = context.dataStore.edit {
        it[Keys.authMode] = "account"
        it[Keys.accountEmail] = email
        it[Keys.rememberMe] = remember
    }

    suspend fun setRememberMe(remember: Boolean) = context.dataStore.edit {
        it[Keys.rememberMe] = remember
    }

    suspend fun clearAuth() = context.dataStore.edit {
        it.remove(Keys.authMode)
        it.remove(Keys.accountEmail)
        it.remove(Keys.demoName)
        it.remove(Keys.demoEmail)
    }

    suspend fun setNotifyProofLoop(v: Boolean) = context.dataStore.edit { it[Keys.notifyProofLoop] = v }
    suspend fun setNotifyLearning(v: Boolean) = context.dataStore.edit { it[Keys.notifyLearning] = v }
    suspend fun setSampleRepos(v: Boolean) = context.dataStore.edit { it[Keys.sampleRepos] = v }
    suspend fun setSampleOpportunities(v: Boolean) = context.dataStore.edit { it[Keys.sampleOpportunities] = v }

    suspend fun toggleRepo(id: String) = context.dataStore.edit {
        val cur = it[Keys.selectedRepos].orEmpty()
        it[Keys.selectedRepos] = if (id in cur) cur - id else cur + id
    }

    suspend fun toggleCriterion(key: String) = context.dataStore.edit {
        val cur = it[Keys.completedCriteria].orEmpty()
        it[Keys.completedCriteria] = if (key in cur) cur - key else cur + key
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
