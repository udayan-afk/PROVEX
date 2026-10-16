package com.provex.app.domain.usecase

import com.provex.app.domain.model.Citation
import com.provex.app.domain.model.Confidence
import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.GithubEvidenceState
import com.provex.app.domain.model.Importance
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.SkillAssessment
import com.provex.app.domain.model.UserProfile

/** Evidence for one skill as reported by the analysis backend. */
data class SkillEvidence(
    val skill: String,
    val status: EvidenceStatus,
    val explanation: String,
    val limitations: List<String>,
    val confidence: Confidence?,
    val citations: List<Citation>,
)

/** What was analyzed. When [analyzed] is false no repository analysis exists at all. */
data class EvidenceBundle(
    val analyzed: Boolean,
    val revision: String?,
    val bySkill: Map<String, SkillEvidence>,
) {
    companion object {
        val NONE = EvidenceBundle(analyzed = false, revision = null, bySkill = emptyMap())
    }
}

/**
 * Combines three independent sources: job requirements, the user's self-reported profile and
 * GitHub evidence. Rules:
 *  - A profile selection never counts as verification.
 *  - Missing evidence is "not demonstrated in analyzed scope", never proof of lacking the skill.
 *  - STRONG/PARTIAL claims without a validated citation are downgraded to "Needs verification".
 */
object AssessmentEngine {

    private const val ABSENCE_NOTE =
        "Missing evidence in analyzed repositories does not prove you lack this skill; the work may live elsewhere."

    fun assess(analysis: JobAnalysis, profile: UserProfile, evidence: EvidenceBundle): List<SkillAssessment> =
        analysis.comparableRequirements.map { req ->
            val inProfile = profile.claims(req.name)
            val ev = evidence.bySkill[req.name.lowercase()]
            when {
                !evidence.analyzed -> SkillAssessment(
                    skill = req.name, category = req.category, importance = req.importance, inProfile = inProfile,
                    github = GithubEvidenceState.NOT_ANALYZED,
                    status = if (inProfile) EvidenceStatus.NEEDS_VERIFICATION else EvidenceStatus.NOT_DEMONSTRATED,
                    explanation = if (inProfile) {
                        "You listed this skill, but no repositories have been analyzed yet, so it is unverified. " +
                            "A self-selected skill is a claim, not evidence."
                    } else {
                        "No repositories have been analyzed and this skill is not in your profile. " +
                            "That says nothing about whether you have it."
                    },
                    limitations = listOf(
                        "No repository analysis has been run (GitHub analysis requires the PROVEX backend).",
                        ABSENCE_NOTE,
                    ),
                )

                ev == null -> SkillAssessment(
                    skill = req.name, category = req.category, importance = req.importance, inProfile = inProfile,
                    github = GithubEvidenceState.NO_EVIDENCE_FOUND,
                    status = if (inProfile) EvidenceStatus.NEEDS_VERIFICATION else EvidenceStatus.NOT_DEMONSTRATED,
                    explanation = if (inProfile) {
                        "You listed this skill, but the analyzed repositories show no evidence of it."
                    } else {
                        "The analyzed repositories show no evidence of this skill."
                    },
                    limitations = listOf(ABSENCE_NOTE),
                )

                else -> {
                    val validated = ev.citations.filter { it.validated }
                    val claimsEvidence = ev.status == EvidenceStatus.STRONG || ev.status == EvidenceStatus.PARTIAL
                    val downgraded = claimsEvidence && validated.isEmpty()
                    val status = if (downgraded) EvidenceStatus.NEEDS_VERIFICATION else ev.status
                    SkillAssessment(
                        skill = req.name, category = req.category, importance = req.importance, inProfile = inProfile,
                        github = if (claimsEvidence && !downgraded) GithubEvidenceState.EVIDENCE_FOUND
                        else GithubEvidenceState.NO_EVIDENCE_FOUND,
                        status = status,
                        explanation = if (downgraded) {
                            "The backend reported evidence but supplied no validated code citation, so PROVEX treats this as unverified."
                        } else ev.explanation,
                        limitations = ev.limitations + if (downgraded) listOf("No validated citation supplied.") else emptyList(),
                        confidence = ev.confidence,
                        citations = validated,
                    )
                }
            }
        }.sortedWith(compareBy({ it.importance.ordinal }, { it.status.ordinal }, { it.skill }))

    fun summarize(assessments: List<SkillAssessment>): Map<EvidenceStatus, Int> =
        EvidenceStatus.entries.associateWith { s -> assessments.count { it.status == s } }

    fun isGap(a: SkillAssessment) = a.status != EvidenceStatus.STRONG

    fun importanceRank(i: Importance) = i.ordinal
}
