package com.provex.app.domain.usecase

import com.provex.app.domain.model.Importance
import com.provex.app.domain.model.JobRequirement
import com.provex.app.domain.model.RequirementCategory

data class ExtractionResult(
    val title: String?,
    val company: String?,
    val requirements: List<JobRequirement>,
)

/**
 * Deterministic on-device keyword extraction used when no backend is available.
 * It only reports requirements whose text literally appears in the posting; it never infers
 * or invents requirements.
 */
object RequirementExtractor {

    private val preferredWords = listOf(
        "preferred", "nice to have", "nice-to-have", "bonus", "good to have", "a plus", "desirable", "advantageous",
    )
    private val requiredWords = listOf(
        "required", "must have", "must", "minimum", "essential", "proficiency in", "proficient",
    )
    private val requiredHeaders = listOf(
        "requirements", "required", "qualifications", "must have", "what you'll need", "what you need",
        "what we're looking for", "minimum",
    )
    private val otherHeaders = listOf(
        "responsibilities", "what you'll do", "about", "benefits", "perks", "we offer", "overview",
    )

    private val titleRegex = Regex("^(?:job\\s+title|position|role|title)\\s*[:\\-–]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val companyRegex =
        Regex("^(?:company(?:\\s+name)?|organi[sz]ation|employer)\\s*[:\\-–]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val yearsRegex = Regex("\\b\\d+\\+?\\s*(?:-\\s*\\d+\\s*)?(?:years?|yrs?)\\b", RegexOption.IGNORE_CASE)
    private val qualificationRegex = Regex(
        "\\b(?:bachelor|master|b\\.?tech|b\\.?sc|m\\.?sc|degree|currently\\s+(?:pursuing|enrolled)|final[- ]year|graduat\\w*)",
        RegexOption.IGNORE_CASE,
    )

    fun extract(rawText: String): ExtractionResult {
        val lines = rawText.lines().map { it.trim().trimStart('-', '*', '•', '·', '–').trim() }.filter { it.isNotEmpty() }

        var title: String? = null
        var company: String? = null
        for (line in lines) {
            if (title == null) titleRegex.find(line)?.let { title = it.groupValues[1].trim().take(100) }
            if (company == null) companyRegex.find(line)?.let { company = it.groupValues[1].trim().take(100) }
        }
        if (title == null) {
            val first = lines.firstOrNull()
            if (first != null && first.length <= 80 && !first.endsWith(".") && !isHeader(first) &&
                !companyRegex.matches(first)
            ) title = first
        }

        var sectionImportance = Importance.MENTIONED
        val found = LinkedHashMap<String, JobRequirement>()

        for (line in lines) {
            val lower = line.lowercase()
            if (isHeader(line)) {
                sectionImportance = when {
                    preferredWords.any { lower.contains(it) } -> Importance.PREFERRED
                    requiredHeaders.any { lower.contains(it) } -> Importance.REQUIRED
                    otherHeaders.any { lower.contains(it) } -> Importance.MENTIONED
                    else -> sectionImportance
                }
                continue
            }
            val lineImportance = when {
                preferredWords.any { lower.contains(it) } -> Importance.PREFERRED
                requiredWords.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) } -> Importance.REQUIRED
                else -> sectionImportance
            }

            for (skill in SkillCatalog.all) {
                if (skill.regex.containsMatchIn(line)) {
                    val key = "${skill.category}:${skill.name.lowercase()}"
                    val existing = found[key]
                    if (existing == null || rank(lineImportance) < rank(existing.importance)) {
                        found[key] = JobRequirement(key, skill.name, skill.category, lineImportance, line.take(200))
                    }
                }
            }

            if (yearsRegex.containsMatchIn(line) || qualificationRegex.containsMatchIn(line)) {
                val key = "${RequirementCategory.EXPERIENCE}:${lower.take(120)}"
                if (key !in found && found.count { it.value.category == RequirementCategory.EXPERIENCE } < 6) {
                    found[key] = JobRequirement(key, line.take(160), RequirementCategory.EXPERIENCE, lineImportance, line.take(200))
                }
            }
        }
        return ExtractionResult(title, company, found.values.toList())
    }

    private fun rank(i: Importance) = when (i) {
        Importance.REQUIRED -> 0
        Importance.PREFERRED -> 1
        Importance.MENTIONED -> 2
    }

    private fun isHeader(line: String): Boolean {
        if (line.length > 60) return false
        val lower = line.lowercase()
        val keyword = (requiredHeaders + preferredWords + otherHeaders).any { lower.contains(it) }
        return keyword && (line.endsWith(":") || line.split(' ').size <= 5)
    }
}
