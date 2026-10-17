package com.provex.app

import com.provex.app.data.remote.dto.ExtractedSkillDto
import com.provex.app.data.remote.dto.JobAnalysisResponseDto
import com.provex.app.data.remote.dto.ProfileComparisonDto
import com.provex.app.data.remote.toDomain
import com.provex.app.domain.model.JobAnalysisResult
import com.provex.app.domain.repository.JobAnalyzerRepository
import com.provex.app.ui.screens.analyzer.JobSkillAnalyzerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobSkillAnalyzerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeJobAnalyzerRepository(
        private val resultToReturn: Result<JobAnalysisResult>,
    ) : JobAnalyzerRepository {
        var lastDescription: String? = null
        var lastUrl: String? = null

        override suspend fun analyzeJob(
            description: String?,
            url: String?,
        ): Result<JobAnalysisResult> {
            lastDescription = description
            lastUrl = url
            return resultToReturn
        }

        override suspend fun matchJobGithub(
            skills: List<com.provex.app.domain.model.ExtractedSkillModel>,
        ): Result<com.provex.app.domain.model.GitHubMatchResult> {
            return Result.success(
                com.provex.app.domain.model.GitHubMatchResult(
                    connected = true,
                    githubUsername = "octocat",
                    repositoriesAnalyzed = 10,
                    matchScore = 83.3f,
                    scoreLabel = "Repository Evidence Match",
                    strongMatchesCount = 2,
                    partialMatchesCount = 1,
                    notFoundCount = 1,
                    preferredMatchesCount = 1,
                    matches = listOf(
                        com.provex.app.domain.model.SkillMatchItem(
                            skill = "Kotlin",
                            category = "Language",
                            importance = "Required",
                            matchStatus = "Strong Match",
                            matchSymbol = "✓",
                            evidenceCredit = 1.0f,
                            requirementWeight = 3,
                            evidenceSource = "provex-mobile",
                            evidenceReason = "Repository uses Kotlin",
                        ),
                    ),
                    skillsToStrengthen = listOf("Docker"),
                )
            )
        }
    }

    @Test
    fun `github matching dto mapping and onMatchGithubClicked flow work accurately`() = runTest {
        val matchItemDto = com.provex.app.data.remote.dto.SkillMatchItemDto(
            skill = "Kotlin",
            category = "Language",
            importance = "Required",
            matchStatus = "Strong Match",
            matchSymbol = "✓",
            evidenceCredit = 1.0f,
            requirementWeight = 3,
            evidenceSource = "my-repo",
            evidenceReason = "Contains Kotlin code",
        )
        val matchResultDto = com.provex.app.data.remote.dto.GitHubMatchResultDto(
            connected = true,
            githubUsername = "alexdev",
            repositoriesAnalyzed = 12,
            matchScore = 87.5f,
            scoreLabel = "Repository Evidence Match",
            strongMatchesCount = 3,
            partialMatchesCount = 1,
            notFoundCount = 1,
            preferredMatchesCount = 1,
            matches = listOf(matchItemDto),
            skillsToStrengthen = listOf("Kubernetes"),
            distribution = mapOf("Strong" to 3, "Partial" to 1, "Not Found" to 1),
            warnings = emptyList(),
        )

        val domainMatch = matchResultDto.toDomain()
        assertTrue(domainMatch.connected)
        assertEquals("alexdev", domainMatch.githubUsername)
        assertEquals(12, domainMatch.repositoriesAnalyzed)
        assertEquals(87.5f, domainMatch.matchScore)
        assertEquals("Repository Evidence Match", domainMatch.scoreLabel)
        assertEquals(1, domainMatch.matches.size)
        assertEquals("Kotlin", domainMatch.matches[0].skill)
        assertEquals("Strong Match", domainMatch.matches[0].matchStatus)
        assertEquals("my-repo", domainMatch.matches[0].evidenceSource)
        assertEquals(listOf("Kubernetes"), domainMatch.skillsToStrengthen)

        val fakeRepo = FakeJobAnalyzerRepository(
            Result.success(
                JobAnalysisResult(
                    jobTitle = "Android Dev",
                    skills = listOf(
                        com.provex.app.domain.model.ExtractedSkillModel("Kotlin", "Language", "Required", "Evidence"),
                    ),
                )
            )
        )
        val viewModel = JobSkillAnalyzerViewModel(fakeRepo)
        viewModel.onDescriptionChanged("Valid job description for Android role requiring Kotlin")
        viewModel.onAnalyzeClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.result)
        viewModel.onMatchGithubClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isMatchingGithub)
        assertNotNull(finalState.result?.githubMatching)
        assertEquals(true, finalState.result?.githubMatching?.connected)
        assertEquals("octocat", finalState.result?.githubMatching?.githubUsername)
        assertEquals(83.3f, finalState.result?.githubMatching?.matchScore)
    }

    @Test
    fun `empty inputs trigger validation error without calling repository`() = runTest {
        val fakeRepo = FakeJobAnalyzerRepository(Result.success(JobAnalysisResult()))
        val viewModel = JobSkillAnalyzerViewModel(fakeRepo)

        viewModel.onAnalyzeClicked()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Please paste a job description or provide a job posting URL"))
        assertNull(fakeRepo.lastDescription)
        assertNull(fakeRepo.lastUrl)
    }

    @Test
    fun `brief description triggers too brief validation error`() = runTest {
        val fakeRepo = FakeJobAnalyzerRepository(Result.success(JobAnalysisResult()))
        val viewModel = JobSkillAnalyzerViewModel(fakeRepo)

        viewModel.onDescriptionChanged("Short text")
        viewModel.onAnalyzeClicked()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("too brief"))
        assertNull(fakeRepo.lastDescription)
    }

    @Test
    fun `both description and url trigger input notice`() {
        val fakeRepo = FakeJobAnalyzerRepository(Result.success(JobAnalysisResult()))
        val viewModel = JobSkillAnalyzerViewModel(fakeRepo)

        viewModel.onDescriptionChanged("Valid long description with Kotlin and Android SDK...")
        assertNull(viewModel.uiState.value.inputNotice)

        viewModel.onUrlChanged("https://example.com/job")
        assertNotNull(viewModel.uiState.value.inputNotice)
        assertTrue(viewModel.uiState.value.inputNotice!!.contains("Both description and URL provided"))
    }

    @Test
    fun `successful analysis updates result and loading state`() = runTest {
        val expectedResult = JobAnalysisResult(
            jobTitle = "Android Engineer",
            company = "TestCorp",
            skills = listOf(
                com.provex.app.domain.model.ExtractedSkillModel("Kotlin", "Language", "Required", "Evidence"),
                com.provex.app.domain.model.ExtractedSkillModel("Python", "Language", "Preferred", "Evidence"),
            ),
        )
        val fakeRepo = FakeJobAnalyzerRepository(Result.success(expectedResult))
        val viewModel = JobSkillAnalyzerViewModel(fakeRepo)

        viewModel.onDescriptionChanged("We are seeking an Android engineer with Kotlin and Python experience...")
        viewModel.onAnalyzeClicked()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.result)
        assertEquals("Android Engineer", state.result?.jobTitle)
        assertEquals(2, state.result?.skills?.size)
    }

    @Test
    fun `skill filtering by importance works accurately`() {
        val testResult = JobAnalysisResult(
            jobTitle = "Engineer",
            skills = listOf(
                com.provex.app.domain.model.ExtractedSkillModel("Kotlin", "Language", "Required", "Evidence"),
                com.provex.app.domain.model.ExtractedSkillModel("Compose", "Framework", "Required", "Evidence"),
                com.provex.app.domain.model.ExtractedSkillModel("Figma", "Design", "Preferred", "Evidence"),
                com.provex.app.domain.model.ExtractedSkillModel("Agile", "Process", "Mentioned", "Evidence"),
            ),
        )
        val fakeRepo = FakeJobAnalyzerRepository(Result.success(testResult))
        val viewModel = JobSkillAnalyzerViewModel(fakeRepo)

        viewModel.onLoadExample()
        // Simulate result present
        val stateWithResult = viewModel.uiState.value.copy(result = testResult)
        assertEquals(4, stateWithResult.filteredSkills.size)

        val requiredState = stateWithResult.copy(selectedFilter = "REQUIRED")
        assertEquals(2, requiredState.filteredSkills.size)
        assertTrue(requiredState.filteredSkills.all { it.importance == "Required" })

        val preferredState = stateWithResult.copy(selectedFilter = "PREFERRED")
        assertEquals(1, preferredState.filteredSkills.size)
        assertEquals("Figma", preferredState.filteredSkills.first().name)
    }

    @Test
    fun `dto to domain mapping accurately transforms all fields`() {
        val dto = JobAnalysisResponseDto(
            jobTitle = "Backend Intern",
            company = "StartupXYZ",
            sourceType = "url_extracted",
            sourceUrl = "https://example.com/jobs/backend",
            summary = "Summary of the role",
            skills = listOf(
                ExtractedSkillDto("Python", "Programming Language", "Required", "Must know Python", 0.98f),
                ExtractedSkillDto("FastAPI", "Backend Framework", "Preferred", "FastAPI is a plus", 0.95f),
            ),
            skillCountsByCategory = mapOf("Programming Language" to 1, "Backend Framework" to 1),
            responsibilities = listOf("Write clean APIs", "Collaborate with team"),
            qualifications = listOf("Pursuing BS in Computer Science"),
            educationRequirements = listOf("Bachelor's degree"),
            experienceRequirements = listOf("1+ years experience"),
            resumeKeywords = listOf("FastAPI", "Python", "REST API"),
            profileComparison = ProfileComparisonDto(
                userHasProfile = true,
                matchingSkills = listOf("Python"),
                missingSkills = listOf("FastAPI"),
                nextSkillsToLearn = listOf("FastAPI"),
                matchPercentage = 50,
                disclaimer = "Test disclaimer",
            ),
            warnings = listOf("Warning 1"),
        )

        val domain = dto.toDomain()
        assertEquals("Backend Intern", domain.jobTitle)
        assertEquals("StartupXYZ", domain.company)
        assertEquals("url_extracted", domain.sourceType)
        assertEquals("https://example.com/jobs/backend", domain.sourceUrl)
        assertEquals(2, domain.skills.size)
        assertEquals("Python", domain.skills[0].name)
        assertEquals("Required", domain.skills[0].importance)
        assertEquals("Must know Python", domain.skills[0].evidence)
        assertEquals(true, domain.profileComparison?.userHasProfile)
        assertEquals(50, domain.profileComparison?.matchPercentage)
        assertEquals(listOf("Python"), domain.profileComparison?.matchingSkills)
        assertEquals(listOf("FastAPI"), domain.profileComparison?.missingSkills)
    }
}
