package com.provex.app

import com.provex.app.domain.model.DataOrigin
import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.GithubEvidenceState
import com.provex.app.domain.model.Importance
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.JobRequirement
import com.provex.app.domain.model.RequirementCategory
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.usecase.AssessmentEngine
import com.provex.app.domain.usecase.EvidenceBundle
import com.provex.app.domain.usecase.RequirementExtractor
import com.provex.app.domain.usecase.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicTest {

    @Test
    fun testRequirementExtractor_identifiesKeywordsAndImportance() {
        val text = """
            Job Title: Mobile Software Engineer
            Company: TestCorp
            
            Requirements:
            - Must have Kotlin and Android SDK proficiency
            - Experience with Unit testing
            
            Preferred:
            - Familiarity with Docker and Jetpack Compose
        """.trimIndent()

        val result = RequirementExtractor.extract(text)
        assertEquals("Mobile Software Engineer", result.title)
        assertEquals("TestCorp", result.company)

        val skills = result.requirements.associateBy { it.name }
        assertTrue("Kotlin should be extracted", "Kotlin" in skills)
        assertTrue("Android should be extracted", "Android" in skills)
        assertTrue("Unit testing should be extracted", "Unit testing" in skills)
        assertTrue("Docker should be extracted", "Docker" in skills)

        assertEquals(Importance.REQUIRED, skills["Kotlin"]?.importance)
        assertEquals(Importance.PREFERRED, skills["Docker"]?.importance)
    }

    @Test
    fun testAssessmentEngine_profileClaimIsNotTreatedAsVerifiedEvidence() {
        val reqKotlin = JobRequirement(
            id = "r1",
            name = "Kotlin",
            category = RequirementCategory.LANGUAGE,
            importance = Importance.REQUIRED,
        )
        val analysis = JobAnalysis(
            id = "test-1",
            createdAt = 1000L,
            rawText = "Requirements: Kotlin",
            requirements = listOf(reqKotlin),
            origin = DataOrigin.ON_DEVICE,
        )
        val profileWithClaim = UserProfile(
            name = "Test Student",
            skills = listOf("Kotlin"),
        )

        // Case 1: No repos analyzed yet -> Profile claim should result in NEEDS_VERIFICATION, NOT STRONG
        val resultsNoRepo = AssessmentEngine.assess(analysis, profileWithClaim, EvidenceBundle.NONE)
        val assessment = resultsNoRepo.first()

        assertTrue(assessment.inProfile)
        assertEquals(GithubEvidenceState.NOT_ANALYZED, assessment.github)
        assertEquals(EvidenceStatus.NEEDS_VERIFICATION, assessment.status)
        assertTrue("Must not claim Strong evidence without code verification", assessment.status != EvidenceStatus.STRONG)
    }

    @Test
    fun testAssessmentEngine_missingEvidenceDoesNotClaimStudentLacksSkill() {
        val req = JobRequirement(
            id = "r2",
            name = "Rust",
            category = RequirementCategory.LANGUAGE,
            importance = Importance.REQUIRED,
        )
        val analysis = JobAnalysis(
            id = "test-2",
            createdAt = 1000L,
            rawText = "Requirements: Rust",
            requirements = listOf(req),
        )
        val emptyProfile = UserProfile()

        val results = AssessmentEngine.assess(analysis, emptyProfile, EvidenceBundle.NONE)
        val rustAssessment = results.first()

        assertEquals(EvidenceStatus.NOT_DEMONSTRATED, rustAssessment.status)
        assertTrue(
            rustAssessment.limitations.any { it.contains("does not prove you lack this skill") },
        )
    }

    @Test
    fun testValidators_validatesEmailAndPassword() {
        assertNotNull(Validators.emailError(""))
        assertNotNull(Validators.emailError("invalid-email"))
        assertNull(Validators.emailError("user@domain.com"))

        assertNotNull(Validators.passwordError("short"))
        assertNull(Validators.passwordError("securepassword123"))

        assertNotNull(Validators.loginPasswordError(""))
        assertNull(Validators.loginPasswordError("anypassword"))

        assertNotNull(Validators.nameError(""))
        assertNotNull(Validators.nameError("A"))
        assertNull(Validators.nameError("Alex Chen"))

        assertNotNull(Validators.confirmPasswordError("securePass123", ""))
        assertNotNull(Validators.confirmPasswordError("securePass123", "mismatchPass"))
        assertNull(Validators.confirmPasswordError("securePass123", "securePass123"))

        assertNotNull(Validators.jobTextError("too short"))
        assertNull(Validators.jobTextError("This is a sufficiently long job description text to satisfy validation requirements."))
    }
}
