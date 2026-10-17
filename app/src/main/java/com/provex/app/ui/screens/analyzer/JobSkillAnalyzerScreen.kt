package com.provex.app.ui.screens.analyzer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.ExtractedSkillModel
import com.provex.app.domain.model.GitHubMatchResult
import com.provex.app.domain.model.JobAnalysisResult
import com.provex.app.domain.model.ProfileComparisonModel
import com.provex.app.domain.model.SkillMatchItem
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.theme.Amber
import com.provex.app.ui.theme.AmberBg
import com.provex.app.ui.theme.AmberDark
import com.provex.app.ui.theme.BlueAccent
import com.provex.app.ui.theme.BorderLight
import com.provex.app.ui.theme.BorderMedium
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.DarkGreenBg
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun JobSkillAnalyzerScreen(
    viewModel: JobSkillAnalyzerViewModel,
    onBack: (() -> Unit)? = null,
    onConnectGitHub: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.copiedMessage) {
        if (state.copiedMessage) {
            Toast.makeText(context, "Extracted skills copied to clipboard", Toast.LENGTH_SHORT).show()
            viewModel.onDismissCopiedMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Job Skill Analyzer",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extract required skills, qualifications, and resume keywords directly from any role.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text("Close", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (state.result == null) {
            // INPUT FORM SECTION
            item {
                ProvexCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Paste Job or Internship Description",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NavyPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.descriptionText,
                            onValueChange = viewModel::onDescriptionChanged,
                            placeholder = {
                                Text(
                                    "Paste the full role description, responsibilities, and requirements...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                            ),
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Minimum 30 characters",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                            Text(
                                text = "${state.descriptionText.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                            Text(
                                text = " OR ",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Paste Job Posting URL",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NavyPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.urlText,
                            onValueChange = viewModel::onUrlChanged,
                            placeholder = {
                                Text(
                                    "https://example.com/careers/software-intern",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                            ),
                        )

                        if (!state.inputNotice.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            NoticeBanner(text = state.inputNotice!!)
                        }

                        if (!state.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = state.errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer),
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = viewModel::onLoadExample) {
                                Text(
                                    text = "✦ Load Example Role",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = NavyPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }

                            if (state.descriptionText.isNotBlank() || state.urlText.isNotBlank()) {
                                TextButton(onClick = viewModel::onResetForm) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = viewModel::onAnalyzeClicked,
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                contentColor = SurfaceWhite,
                            ),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MintGreen,
                                    strokeWidth = 2.5.dp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analyzing Requirements...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Analyze Role Requirements", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                NoticeBanner(
                    text = "Grounded Intelligence: PROVEX strictly extracts skills mentioned in the job posting text. If URL extraction is blocked by website verification, paste the job text directly.",
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            // RESULTS SECTION
            val analysis = state.result!!

            item {
                RoleHeaderCard(analysis = analysis)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (analysis.githubMatching != null && analysis.githubMatching.connected) {
                item {
                    GitHubEvidenceDashboardCard(
                        githubMatching = analysis.githubMatching,
                        isRefreshing = state.isMatchingGithub,
                        onRefresh = viewModel::onMatchGithubClicked,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (analysis.githubMatching.skillsToStrengthen.isNotEmpty()) {
                    item {
                        SkillsToStrengthenCard(skills = analysis.githubMatching.skillsToStrengthen)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                item {
                    GitHubConnectPromptCard(onConnectGitHub = onConnectGitHub)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (analysis.profileComparison != null) {
                item {
                    ProfileComparisonCard(comparison = analysis.profileComparison!!)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                CategoryOverviewCard(skillCounts = analysis.skillCountsByCategory)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                FilterTabRow(
                    selectedFilter = state.selectedFilter,
                    totalCount = analysis.skills.size,
                    requiredCount = analysis.skills.count { it.importance.equals("Required", ignoreCase = true) },
                    preferredCount = analysis.skills.count { it.importance.equals("Preferred", ignoreCase = true) },
                    onSelect = viewModel::onFilterSelected,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(state.filteredSkills, key = { it.name + it.importance }) { skill ->
                val matchItem = analysis.githubMatching?.matches?.find {
                    it.skill.equals(skill.name, ignoreCase = true)
                }
                ExtractedSkillCard(skill = skill, matchItem = matchItem)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (analysis.responsibilities.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionBulletsCard(
                        title = "Key Responsibilities",
                        bullets = analysis.responsibilities,
                    )
                }
            }

            if (analysis.qualifications.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionBulletsCard(
                        title = "Qualifications & Criteria",
                        bullets = analysis.qualifications,
                    )
                }
            }

            if (analysis.resumeKeywords.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    ResumeKeywordsCard(keywords = analysis.resumeKeywords)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::onEditDescription,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Edit Input")
                    }

                    Button(
                        onClick = { viewModel.onCopySkills(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = SurfaceWhite,
                        ),
                    ) {
                        Text("Copy Skills")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = viewModel::onResetForm,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Analyze Another Role", color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun RoleHeaderCard(analysis: JobAnalysisResult) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = analysis.jobTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = NavyPrimary,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = if (analysis.sourceType == "url_extracted") "URL EXTRACTED" else "PASTED TEXT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MintGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            if (!analysis.company.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Company: ${analysis.company}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )

            if (analysis.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                analysis.warnings.forEach { warn ->
                    Text(
                        text = "ℹ $warn",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileComparisonCard(comparison: ProfileComparisonModel) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Career Profile Alignment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary,
                    )
                    Text(
                        text = if (comparison.userHasProfile) "Compared with your profile skills" else "No profile configured yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Surface(
                    color = if (comparison.matchPercentage >= 70) MintGreen else NavyPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "${comparison.matchPercentage}% Match",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (comparison.matchPercentage >= 70) NavyDark else SurfaceWhite,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (comparison.matchingSkills.isNotEmpty()) {
                Text(
                    text = "Skills In Your Profile (${comparison.matchingSkills.size}):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    comparison.matchingSkills.forEach { skill ->
                        Surface(
                            color = MintGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MintGreen),
                        ) {
                            Text(
                                text = "✓ $skill",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NavyDark,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (comparison.missingSkills.isNotEmpty()) {
                Text(
                    text = "Skills Missing From Profile (${comparison.missingSkills.size}):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    comparison.missingSkills.take(8).forEach { skill ->
                        Surface(
                            color = SurfaceAlt,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = "+ $skill",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Notice: ${comparison.disclaimer}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryOverviewCard(skillCounts: Map<String, Int>) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Skill Distribution",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                skillCounts.forEach { (category, count) ->
                    Surface(
                        color = SurfaceAlt,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            text = "$category ($count)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabRow(
    selectedFilter: String,
    totalCount: Int,
    requiredCount: Int,
    preferredCount: Int,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            "ALL" to "All ($totalCount)",
            "REQUIRED" to "Required ($requiredCount)",
            "PREFERRED" to "Preferred ($preferredCount)",
        ).forEach { (filterKey, label) ->
            val selected = selectedFilter == filterKey
            Surface(
                color = if (selected) NavyPrimary else SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (selected) NavyPrimary else MaterialTheme.colorScheme.outline),
                modifier = Modifier.clickable { onSelect(filterKey) },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (selected) SurfaceWhite else TextPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ExtractedSkillCard(
    skill: ExtractedSkillModel,
    matchItem: SkillMatchItem? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    val importanceColor = when (skill.importance.lowercase()) {
        "required" -> NavyPrimary
        "preferred" -> MintGreen
        else -> TextSecondary
    }

    val badgeTextColor = when (skill.importance.lowercase()) {
        "required" -> SurfaceWhite
        "preferred" -> NavyDark
        else -> SurfaceWhite
    }

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (matchItem?.matchStatus == "Strong Match") MintGreen.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (matchItem != null) {
                        val symbolBg = when (matchItem.matchStatus) {
                            "Strong Match" -> DarkGreenBg
                            "Partial Match" -> AmberBg
                            "Preferred Match" -> DarkGreenBg
                            else -> SurfaceAlt
                        }
                        val symbolColor = when (matchItem.matchStatus) {
                            "Strong Match" -> DarkGreen
                            "Partial Match" -> AmberDark
                            "Preferred Match" -> DarkGreen
                            else -> TextSecondary
                        }
                        Surface(
                            color = symbolBg,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, symbolColor.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = " ${matchItem.matchSymbol} ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = symbolColor,
                                modifier = Modifier.padding(2.dp),
                            )
                        }
                    }

                    Column {
                        Text(
                            text = skill.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NavyPrimary,
                        )
                        Text(
                            text = skill.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (matchItem != null) {
                        val matchBadgeColor = when (matchItem.matchStatus) {
                            "Strong Match" -> DarkGreenBg
                            "Partial Match" -> AmberBg
                            "Preferred Match" -> DarkGreenBg
                            else -> SurfaceAlt
                        }
                        val matchTextColor = when (matchItem.matchStatus) {
                            "Strong Match" -> DarkGreen
                            "Partial Match" -> AmberDark
                            "Preferred Match" -> DarkGreen
                            else -> TextSecondary
                        }
                        Surface(
                            color = matchBadgeColor,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = matchItem.matchStatus.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = matchTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Surface(
                        color = importanceColor,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = skill.importance.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }

            if (matchItem != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (matchItem.evidenceCredit > 0f) DarkGreenBg.copy(alpha = 0.5f) else SurfaceAlt,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GitHub Evidence:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (matchItem.evidenceCredit > 0f) DarkGreen else TextSecondary,
                            )
                            if (!matchItem.evidenceSource.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = NavyPrimary,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = matchItem.evidenceSource,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MintGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = matchItem.evidenceReason,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextPrimary,
                        )
                    }
                }
            }

            if (expanded && skill.evidence.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = SurfaceAlt,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Extracted from Job Description:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "“${skill.evidence}”",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = TextPrimary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubEvidenceDashboardCard(
    githubMatching: GitHubMatchResult,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Repository Evidence Match",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary,
                    )
                    Text(
                        text = "Real deterministic evaluation of public GitHub evidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NavyPrimary)
                    } else {
                        Text("↻ Sync", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CIRCULAR MATCH METER
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val score = githubMatching.matchScore
                val animatedProgress by animateFloatAsState(
                    targetValue = if (score != null) (score / 100f).coerceIn(0f, 1f) else 0f,
                    label = "match_progress",
                )

                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        drawCircle(
                            color = BorderLight,
                            radius = (size.minDimension - strokeWidth) / 2,
                            style = Stroke(width = strokeWidth),
                        )
                        if (score != null) {
                            drawArc(
                                color = MintGreen,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (score != null) {
                            Text(
                                text = "${score.roundToInt()}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                ),
                                color = NavyPrimary,
                            )
                        } else {
                            Text(
                                text = "N/A",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                ),
                            )
                        }
                        Text(
                            text = "MATCH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                            ),
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SCORE LABEL BADGE
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = NavyPrimary,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = if (githubMatching.matchScore != null) {
                            "Repository Evidence Match: ${githubMatching.matchScore.roundToInt()}%"
                        } else {
                            "Repository Evidence Match: Not enough data"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MintGreen,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 SUMMARY CARDS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    symbol = "✓",
                    count = githubMatching.strongMatchesCount,
                    label = "Strong Match",
                    containerColor = DarkGreenBg,
                    contentColor = DarkGreen,
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    symbol = "◐",
                    count = githubMatching.partialMatchesCount,
                    label = "Partial Match",
                    containerColor = AmberBg,
                    contentColor = AmberDark,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    symbol = "★",
                    count = githubMatching.preferredMatchesCount,
                    label = "Preferred",
                    containerColor = SoftBlue,
                    contentColor = BlueAccent,
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    symbol = "○",
                    count = githubMatching.notFoundCount,
                    label = "Not Found",
                    containerColor = SurfaceAlt,
                    contentColor = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // HORIZONTAL DISTRIBUTION BAR CHART
            val totalSkills = (githubMatching.strongMatchesCount + githubMatching.partialMatchesCount + githubMatching.notFoundCount).coerceAtLeast(1)
            val strongWeight = (githubMatching.strongMatchesCount.toFloat() / totalSkills).coerceAtLeast(0.01f)
            val partialWeight = (githubMatching.partialMatchesCount.toFloat() / totalSkills).coerceAtLeast(0.01f)
            val notFoundWeight = (githubMatching.notFoundCount.toFloat() / totalSkills).coerceAtLeast(0.01f)

            Text(
                text = "Skill Distribution in Repositories",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(BorderLight, RoundedCornerShape(6.dp))
                    .padding(1.dp),
            ) {
                if (githubMatching.strongMatchesCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(strongWeight)
                            .fillMaxSize()
                            .background(MintGreen, RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)),
                    )
                }
                if (githubMatching.partialMatchesCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(partialWeight)
                            .fillMaxSize()
                            .background(Amber),
                    )
                }
                if (githubMatching.notFoundCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(notFoundWeight)
                            .fillMaxSize()
                            .background(BorderMedium, RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(MintGreen, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "✓ Strong (${githubMatching.strongMatchesCount})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Amber, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "◐ Partial (${githubMatching.partialMatchesCount})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(BorderMedium, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "○ Not Found (${githubMatching.notFoundCount})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // REPOSITORIES ANALYZED & DISCLAIMER
            Surface(
                color = SurfaceAlt,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📁 ",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${githubMatching.repositoriesAnalyzed} repositories analyzed for @${githubMatching.githubUsername ?: "account"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NavyPrimary,
                        )
                    }
                    if (githubMatching.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        githubMatching.warnings.forEach { warn ->
                            Text(
                                text = "⚠ $warn",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = AmberDark),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notice: ${githubMatching.disclaimer}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    symbol: String,
    count: Int,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = contentColor,
            )
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = contentColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsToStrengthenCard(skills: List<String>) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Skills to Demonstrate or Strengthen",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "These requested skills had no detected code artifacts in your public repositories. Prioritize demonstrating them with a repository or portfolio project:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                skills.forEach { skill ->
                    Surface(
                        color = AmberBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = "○ $skill",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberDark,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubConnectPromptCard(onConnectGitHub: (() -> Unit)?) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "GitHub Evidence Matching",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary,
                )
                Surface(
                    color = AmberBg,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "NOT CONNECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connect your GitHub account to compare these role requirements with your actual repositories and generate a verified Repository Evidence Match score.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (onConnectGitHub != null) {
                Button(
                    onClick = onConnectGitHub,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyPrimary,
                        contentColor = SurfaceWhite,
                    ),
                ) {
                    Text("Connect GitHub in Profile", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = "Note: Missing repository evidence does not necessarily mean you lack a skill; it indicates no public code artifacts were found.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextMuted),
            )
        }
    }
}

@Composable
private fun SectionBulletsCard(title: String, bullets: List<String>) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary,
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumeKeywordsCard(keywords: List<String>) {
    ProvexCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Recommended Resume Keywords",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Incorporate these terms when tailoring your CV or project descriptions:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                keywords.forEach { kw ->
                    Surface(
                        color = NavyPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.2f)),
                    ) {
                        Text(
                            text = kw,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
