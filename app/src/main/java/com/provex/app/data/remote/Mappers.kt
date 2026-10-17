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

fun com.provex.app.data.remote.dto.GitHubRepoDto.toDomain(selected: Boolean) = GithubRepo(
    id = id.toString(),
    fullName = fullName,
    language = language,
    lastAnalyzedRevision = null,
    selected = selected,
    isSample = false,
    name = name,
    owner = owner,
    description = description,
    isPrivate = private,
    htmlUrl = htmlUrl,
    defaultBranch = defaultBranch,
    updatedAt = updatedAt,
)

fun com.provex.app.data.remote.dto.ExtractedSkillDto.toDomain() =
    com.provex.app.domain.model.ExtractedSkillModel(
        name = name,
        category = category,
        importance = importance,
        evidence = evidence,
        confidence = confidence,
    )

fun com.provex.app.data.remote.dto.ProfileComparisonDto.toDomain() =
    com.provex.app.domain.model.ProfileComparisonModel(
        userHasProfile = userHasProfile,
        matchingSkills = matchingSkills,
        missingSkills = missingSkills,
        nextSkillsToLearn = nextSkillsToLearn,
        matchPercentage = matchPercentage,
        disclaimer = disclaimer,
    )

fun com.provex.app.data.remote.dto.SkillMatchItemDto.toDomain() =
    com.provex.app.domain.model.SkillMatchItem(
        skill = skill,
        category = category,
        importance = importance,
        matchStatus = matchStatus,
        matchSymbol = matchSymbol,
        evidenceCredit = evidenceCredit,
        requirementWeight = requirementWeight,
        evidenceSource = evidenceSource,
        evidenceReason = evidenceReason,
    )

fun com.provex.app.data.remote.dto.GitHubMatchResultDto.toDomain() =
    com.provex.app.domain.model.GitHubMatchResult(
        connected = connected,
        githubUsername = githubUsername,
        repositoriesAnalyzed = repositoriesAnalyzed,
        matchScore = matchScore,
        scoreLabel = scoreLabel,
        strongMatchesCount = strongMatchesCount,
        partialMatchesCount = partialMatchesCount,
        notFoundCount = notFoundCount,
        preferredMatchesCount = preferredMatchesCount,
        matches = matches.map { it.toDomain() },
        skillsToStrengthen = skillsToStrengthen,
        distribution = distribution,
        warnings = warnings,
        disclaimer = disclaimer,
    )

fun com.provex.app.data.remote.dto.JobAnalysisResponseDto.toDomain() =
    com.provex.app.domain.model.JobAnalysisResult(
        jobTitle = jobTitle ?: "Job Role",
        company = company,
        sourceType = sourceType,
        sourceUrl = sourceUrl,
        summary = summary,
        skills = skills.map { it.toDomain() },
        skillCountsByCategory = skillCountsByCategory,
        responsibilities = responsibilities,
        qualifications = qualifications,
        educationRequirements = educationRequirements,
        experienceRequirements = experienceRequirements,
        resumeKeywords = resumeKeywords,
        profileComparison = profileComparison?.toDomain(),
        githubMatching = githubMatching?.toDomain(),
        extractionMethod = extractionMethod,
        warnings = warnings,
    )
