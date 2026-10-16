package com.provex.app.data.remote

import com.provex.app.data.remote.dto.AnalysisDto
import com.provex.app.data.remote.dto.EvidenceResponseDto
import com.provex.app.data.remote.dto.RepositoryDto
import com.provex.app.domain.model.Citation
import com.provex.app.domain.model.Confidence
import com.provex.app.domain.model.DataOrigin
import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.GithubRepo
import com.provex.app.domain.model.Importance
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.JobRequirement
import com.provex.app.domain.model.RequirementCategory
import com.provex.app.domain.usecase.EvidenceBundle
import com.provex.app.domain.usecase.SkillEvidence

private inline fun <reified E : Enum<E>> enumOrNull(value: String?): E? =
    enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) }

fun AnalysisDto.toDomain(now: Long, rawText: String): JobAnalysis = JobAnalysis(
    id = id,
    createdAt = now,
    title = title,
    company = company,
    sourceUrl = sourceUrl,
    rawText = rawText,
    requirements = requirements.mapIndexed { i, r ->
        JobRequirement(
            id = "r$i:${r.name.lowercase()}",
            name = r.name,
            category = enumOrNull<RequirementCategory>(r.category) ?: RequirementCategory.GENERAL,
            importance = enumOrNull<Importance>(r.importance) ?: Importance.MENTIONED,
            sourceText = r.sourceText,
        )
    },
    origin = DataOrigin.BACKEND,
)

fun EvidenceResponseDto.toBundle(): EvidenceBundle = EvidenceBundle(
    analyzed = true,
    revision = revision,
    bySkill = skills.associate { s ->
        s.skill.lowercase() to SkillEvidence(
            skill = s.skill,
            status = enumOrNull<EvidenceStatus>(s.status) ?: EvidenceStatus.NEEDS_VERIFICATION,
            explanation = s.explanation,
            limitations = s.limitations,
            confidence = enumOrNull<Confidence>(s.confidence),
            citations = s.citations.map {
                Citation(it.repository, it.filePath, it.startLine, it.endLine, it.snippet, it.commit, it.validated)
            },
        )
    },
)

fun RepositoryDto.toDomain(selected: Boolean) =
    GithubRepo(id, fullName, language, lastAnalyzedRevision, selected, isSample = false)
