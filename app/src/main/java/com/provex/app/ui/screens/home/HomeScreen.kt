package com.provex.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.ui.components.EvidenceStatusBadge
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexTextField
import com.provex.app.ui.components.SampleBadge
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToExtraction: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToRepositories: () -> Unit,
    onNavigateToComparison: (String) -> Unit,
    onNavigateToProofPath: (String) -> Unit,
    onNavigateToOpportunities: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToExtraction -> onNavigateToExtraction(event.analysisId)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Heading & Subtitle
            Text(
                text = "Check your fit for any opportunity",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Understand what a job requires and how your skills compare.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.authState is com.provex.app.domain.model.AuthState.Demo) {
                val demoState = state.authState as com.provex.app.domain.model.AuthState.Demo
                val label = if (!demoState.name.isNullOrBlank()) {
                    "Demo Session • ${demoState.name} (Local On-Device)"
                } else {
                    "Demo Session (Local On-Device Mode)"
                }
                Surface(
                    color = NavyDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MintGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MintGreen,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }

            if (state.notice != null) {
                NoticeBanner(text = state.notice!!, isWarning = true, modifier = Modifier.padding(bottom = 12.dp))
            }

            // Quick Status Strip: Profile + GitHub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Profile summary chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToProfile),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CAREER PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.profile.isEmpty) "Incomplete"
                            else state.profile.targetRole.ifBlank { "Profile set" },
                            style = MaterialTheme.typography.titleMedium,
                            color = NavyPrimary,
                            maxLines = 1,
                        )
                        Text(
                            text = "${state.profile.claimedSkills.size} skills claimed",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                // GitHub connection summary
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToRepositories),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "GITHUB EVIDENCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (state.githubState.status == RepoConnectionStatus.CONNECTED) DarkGreen
                                        else TextMuted,
                                        CircleShape,
                                    ),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.githubState.status == RepoConnectionStatus.CONNECTED) {
                                    if (state.githubState.isSample) "Demo Repos" else "Connected"
                                } else "Not connected",
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = "${state.githubState.repos.count { it.selected }} repos selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action Card: Job Description Input
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Paste opportunity description",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                        TextButton(onClick = viewModel::loadSampleJob) {
                            Text(
                                text = "Load sample",
                                style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary, fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ProvexTextField(
                        value = state.jobText,
                        onValueChange = viewModel::onJobTextChanged,
                        label = "Job or internship description",
                        placeholder = "Paste the complete requirements section or entire job posting text here...",
                        minLines = 5,
                        maxLines = 10,
                        errorText = state.jobTextError,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.jobUrl,
                        onValueChange = viewModel::onJobUrlChanged,
                        label = "Job posting link (optional)",
                        placeholder = "https://company.com/jobs/role-123",
                        errorText = state.jobUrlError,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProvexButton(
                        text = "Analyze Opportunity",
                        onClick = viewModel::onAnalyze,
                        isLoading = state.isAnalyzing,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Skills Needing Attention (if any)
        if (state.attentionSkills.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Skills needing attention",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    val latest = state.recentAnalyses.firstOrNull()
                    if (latest != null) {
                        TextButton(onClick = { onNavigateToProofPath(latest.id) }) {
                            Text(
                                text = "View ProofPath",
                                style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.attentionSkills) { skill ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    backgroundColor = SurfaceAlt,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = skill.skill,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (skill.inProfile) "Claimed in profile, but unverified in code."
                                else "Not in profile or analyzed repos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        EvidenceStatusBadge(status = skill.status)
                    }
                }
            }
        }

        // Recent Analyses section
        if (state.recentAnalyses.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recent analyses",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.recentAnalyses.take(3)) { analysis ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { onNavigateToComparison(analysis.id) },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = analysis.displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${analysis.requirements.size} requirements",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        if (!analysis.company.isNullOrBlank()) {
                            Text(
                                text = analysis.company!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        // Personalized Opportunity Recommendations (Sample badge enabled)
        if (state.opportunities.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target opportunities",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SampleBadge(text = "Sample")
                    }
                    TextButton(onClick = onNavigateToOpportunities) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.opportunities.take(2)) { opp ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    backgroundColor = SurfaceWhite,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = opp.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = NavyPrimary,
                        )
                        Text(
                            text = "${opp.company} • ${opp.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
