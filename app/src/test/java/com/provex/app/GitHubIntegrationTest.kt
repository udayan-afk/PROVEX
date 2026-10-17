package com.provex.app

import com.provex.app.data.remote.dto.GitHubRepoDto
import com.provex.app.data.remote.toDomain
import com.provex.app.domain.model.GithubRepo
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.repository.GithubRepository
import com.provex.app.domain.repository.ProfileRepository
import com.provex.app.ui.screens.profile.ProfileViewModel
import com.provex.app.ui.screens.repositories.RepositoriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import com.provex.app.data.repository.userMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.junit.Test

class FakeGithubRepository : GithubRepository {
    val stateFlow = MutableStateFlow(
        GithubState(
            status = RepoConnectionStatus.NOT_CONNECTED,
            accountLogin = null,
            repos = emptyList(),
            isSample = false,
            message = null,
        )
    )
    override val state: Flow<GithubState> = stateFlow

    var refreshCalled = false
    var disconnectCalled = false
    var toggleSelectedId: String? = null
    var authorizationResult: Result<String> = Result.success("https://github.com/login/oauth/authorize?client_id=xyz&state=123")

    override suspend fun refresh() {
        refreshCalled = true
    }

    override suspend fun beginAuthorization(): Result<String> = authorizationResult

    override suspend fun disconnect(): Result<Unit> {
        disconnectCalled = true
        stateFlow.value = GithubState(
            status = RepoConnectionStatus.NOT_CONNECTED,
            accountLogin = null,
            repos = emptyList(),
            isSample = false,
            message = null,
        )
        return Result.success(Unit)
    }

    override suspend fun toggleSelected(id: String) {
        toggleSelectedId = id
    }

    override suspend fun setSampleEnabled(enabled: Boolean) {
        // no-op for fake
    }
}

class FakeProfileRepository : ProfileRepository {
    override val profile: Flow<UserProfile> = flowOf(UserProfile(name = "Test Student"))
    override suspend fun save(profile: UserProfile): Result<Unit> = Result.success(Unit)
    override suspend fun syncFromRemote(): Result<UserProfile> = Result.success(UserProfile(name = "Test Student"))
}

@OptIn(ExperimentalCoroutinesApi::class)
class GitHubIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRepositoriesViewModel_initialStateNotConnected() = runTest {
        val fakeRepo = FakeGithubRepository()
        val viewModel = RepositoriesViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(RepoConnectionStatus.NOT_CONNECTED, uiState.githubState.status)
        assertNull(uiState.githubState.accountLogin)
        assertTrue(uiState.githubState.repos.isEmpty())
        assertFalse(uiState.isConnecting)
    }

    @Test
    fun testRepositoriesViewModel_connectedStateDisplaysUsernameAndRepos() = runTest {
        val fakeRepo = FakeGithubRepository()
        val repoList = listOf(
            GithubRepo(
                id = "101",
                fullName = "octocat/hello-world",
                language = "Kotlin",
                lastAnalyzedRevision = null,
                selected = true,
                isSample = false,
                name = "hello-world",
                owner = "octocat",
                description = "My first GitHub repository",
                isPrivate = false,
                htmlUrl = "https://github.com/octocat/hello-world",
                defaultBranch = "main",
                updatedAt = "2026-10-17T00:00:00Z",
            )
        )
        fakeRepo.stateFlow.value = GithubState(
            status = RepoConnectionStatus.CONNECTED,
            accountLogin = "octocat",
            repos = repoList,
            isSample = false,
            message = null,
        )

        val viewModel = RepositoriesViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(RepoConnectionStatus.CONNECTED, uiState.githubState.status)
        assertEquals("octocat", uiState.githubState.accountLogin)
        assertEquals(1, uiState.githubState.repos.size)
        assertEquals("hello-world", uiState.githubState.repos.first().name)
        assertEquals("Kotlin", uiState.githubState.repos.first().language)
    }

    @Test
    fun testRepositoriesViewModel_connectLaunchesAuthUrl() = runTest {
        val fakeRepo = FakeGithubRepository()
        val viewModel = RepositoriesViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onConnectClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.authUrlToLaunch)
        assertTrue(uiState.authUrlToLaunch!!.contains("github.com/login/oauth/authorize"))
        assertFalse(uiState.isConnecting)

        viewModel.clearAuthUrl()
        assertNull(viewModel.uiState.value.authUrlToLaunch)
    }

    @Test
    fun testRepositoriesViewModel_disconnectFlow() = runTest {
        val fakeRepo = FakeGithubRepository()
        fakeRepo.stateFlow.value = GithubState(
            status = RepoConnectionStatus.CONNECTED,
            accountLogin = "octocat",
            repos = emptyList(),
            isSample = false,
            message = null,
        )
        val viewModel = RepositoriesViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDisconnect()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepo.disconnectCalled)
        assertEquals(RepoConnectionStatus.NOT_CONNECTED, viewModel.uiState.value.githubState.status)
    }

    @Test
    fun testProfileViewModel_observesGitHubConnectionAndDisconnect() = runTest {
        val fakeProfileRepo = FakeProfileRepository()
        val fakeGithubRepo = FakeGithubRepository()
        fakeGithubRepo.stateFlow.value = GithubState(
            status = RepoConnectionStatus.CONNECTED,
            accountLogin = "octocat-dev",
            repos = emptyList(),
            isSample = false,
            message = null,
        )

        val profileVm = ProfileViewModel(fakeProfileRepo, fakeGithubRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(RepoConnectionStatus.CONNECTED, profileVm.uiState.value.githubState.status)
        assertEquals("octocat-dev", profileVm.uiState.value.githubState.accountLogin)

        profileVm.onDisconnectGithub()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeGithubRepo.disconnectCalled)
        assertEquals(RepoConnectionStatus.NOT_CONNECTED, profileVm.uiState.value.githubState.status)
    }

    @Test
    fun testGitHubRepoDto_mappingPreservesAllFields() {
        val dto = GitHubRepoDto(
            id = 42L,
            name = "provex-app",
            fullName = "udayan-afk/provex-app",
            owner = "udayan-afk",
            description = "AI Career Intelligence Mobile App",
            language = "Kotlin",
            private = true,
            htmlUrl = "https://github.com/udayan-afk/provex-app",
            defaultBranch = "master",
            updatedAt = "2026-10-17T06:00:00Z",
        )

        val domain = dto.toDomain(selected = true)
        assertEquals("42", domain.id)
        assertEquals("provex-app", domain.name)
        assertEquals("udayan-afk/provex-app", domain.fullName)
        assertEquals("udayan-afk", domain.owner)
        assertEquals("AI Career Intelligence Mobile App", domain.description)
        assertEquals("Kotlin", domain.language)
        assertTrue(domain.isPrivate)
        assertEquals("https://github.com/udayan-afk/provex-app", domain.htmlUrl)
        assertEquals("master", domain.defaultBranch)
        assertEquals("2026-10-17T06:00:00Z", domain.updatedAt)
        assertTrue(domain.selected)
        assertFalse(domain.isSample)
    }

    @Test
    fun testUserMessage_http503WithDetailExtractsServerMessage() {
        val jsonMediaType = "application/json".toMediaType()
        val errorBody = """{"detail":"GitHub OAuth client is not configured on the PROVEX backend. Set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET in backend/.env."}""".toResponseBody(jsonMediaType)
        val retrofitResponse = Response.error<Any>(503, errorBody)
        val exception = HttpException(retrofitResponse)

        val msg = exception.userMessage()
        assertEquals(
            "GitHub OAuth client is not configured on the PROVEX backend. Set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET in backend/.env.",
            msg
        )
    }

    @Test
    fun testUserMessage_http503WithoutDetailHasClearFallback() {
        val errorBody = "".toResponseBody(null)
        val retrofitResponse = Response.error<Any>(503, errorBody)
        val exception = HttpException(retrofitResponse)

        val msg = exception.userMessage()
        assertEquals("The PROVEX backend GitHub service is not configured or unavailable.", msg)
    }
}
