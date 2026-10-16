package com.provex.app.data.repository

import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.GithubRepo
import com.provex.app.domain.model.Opportunity
import com.provex.app.domain.model.ProcessingState
import com.provex.app.domain.model.ProofLoopSkillChange
import com.provex.app.domain.model.ProofLoopState
import com.provex.app.domain.model.SkepticReview

/**
 * Clearly-labelled illustrative data used to preview screens until the backend exists.
 * Every item produced here carries `isSample = true` and the UI must surface that.
 * Nothing here is ever fed into assessments or history.
 */
object SampleData {

    fun repositories(selected: Set<String>): List<GithubRepo> = listOf(
        GithubRepo("sample-1", "sample-user/notes-app", "Kotlin", "a1b2c3d", "sample-1" in selected, true),
        GithubRepo("sample-2", "sample-user/api-service", "Python", null, "sample-2" in selected, true),
        GithubRepo("sample-3", "sample-user/portfolio-site", "TypeScript", null, "sample-3" in selected, true),
    )

    val skepticReviews: List<SkepticReview> = listOf(
        SkepticReview(
            skill = "Docker",
            initialStatus = EvidenceStatus.STRONG,
            initialClaim = "The repository contains a Dockerfile, so the candidate can containerize and deploy applications.",
            challenge = "A Dockerfile existing does not show the image builds, runs, or was ever deployed.",
            supportingEvidence = "Only the Dockerfile itself was observed. No CI build, compose file, or deployment " +
                "configuration was found in the analyzed scope.",
            validatedStatus = EvidenceStatus.PARTIAL,
            explanation = "Downgraded from strong to partial: the file proves familiarity with the syntax, not successful " +
                "deployment. The challenge was weighed against the evidence rather than accepted automatically.",
            isSample = true,
        ),
        SkepticReview(
            skill = "Unit testing",
            initialStatus = EvidenceStatus.PARTIAL,
            initialClaim = "Test files exist, suggesting the candidate writes unit tests.",
            challenge = "Test files might be empty or contain no assertions.",
            supportingEvidence = "The analyzed test file contains several assertions exercising repository logic.",
            validatedStatus = EvidenceStatus.PARTIAL,
            explanation = "Conclusion unchanged: the challenge was checked and not supported, but a single test file is " +
                "still limited coverage, so the assessment stays partial.",
            isSample = true,
        ),
    )

    fun proofLoop(): ProofLoopState = ProofLoopState(
        repository = "sample-user/notes-app",
        latestCommit = "a1b2c3d",
        changedFiles = listOf("README.md", "app/src/main/java/NotesViewModel.kt"),
        skillChanges = listOf(
            ProofLoopSkillChange(
                skill = "Kotlin", reassessed = true,
                previous = EvidenceStatus.PARTIAL, current = EvidenceStatus.STRONG,
                note = "Reassessed because NotesViewModel.kt changed.",
            ),
            ProofLoopSkillChange(
                skill = "Unit testing", reassessed = false,
                previous = EvidenceStatus.PARTIAL, current = EvidenceStatus.PARTIAL,
                note = "Left unchanged: no test files were touched. A README-only change never upgrades a technical skill.",
            ),
        ),
        assessedAt = null,
        processing = ProcessingState.COMPLETE,
        isSample = true,
    )

    fun opportunities(targetRole: String): List<Opportunity> {
        val role = targetRole.ifBlank { "Software Engineering Intern" }
        return listOf(
            Opportunity("sample-o1", "$role (sample)", "Sample Company", "Location not specified", null, true),
            Opportunity("sample-o2", "Junior $role (sample)", "Sample Company", "Location not specified", null, true),
        )
    }
}
