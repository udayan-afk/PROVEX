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
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.ui.components.EvidenceStatusBadge
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexTextField
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
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
    onNavigateToOpportunities: () -> Unit = {},
    onNavigateToRoleAdvisor: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.checkDraft()
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

            // Greeting & Demo Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = state.profile.name.trim().ifBlank {
                        if (state.authState is AuthState.Demo && !(state.authState as AuthState.Demo).name.isNullOrBlank()) {
                            (state.authState as AuthState.Demo).name!!
                        } else {
                            "Engineer"
                        }
                    }
                    Text(
                        text = "Hello, $displayName 👋",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        text = "Turn code into credibility with evidence-based intelligence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                if (state.authState is AuthState.Demo) {
                    Surface(
                        color = NavyDark,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MintGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Demo Mode",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MintGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (state.notice != null) {
                NoticeBanner(
                    text = state.notice!!,
                    isWarning = true,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            // Primary Feature: "Check your fit" Card (Front and Center)
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Check your fit",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                            )
                            Text(
                                text = "Compare job requirements with your skills and code evidence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = viewModel::loadSampleJob,
                            modifier = Modifier.height(28.dp),
                        ) {
                            Text(
                                text = "Load sample job",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    ProvexTextField(
                        value = state.jobText,
                        onValueChange = viewModel::onJobTextChanged,
                        label = "Job or internship description",
                        placeholder = "Paste the complete requirements section or entire job posting text here...",
                        minLines = 4,
                        maxLines = 8,
                        errorText = state.jobTextError,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProvexTextField(
                        value = state.jobUrl,
                        onValueChange = viewModel::onJobUrlChanged,
                        label = "Job posting link (optional)",
                        placeholder = "https://company.com/jobs/role-123",
                        errorText = state.jobUrlError,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ProvexButton(
                        text = "Analyze Opportunity",
                        onClick = viewModel::onAnalyze,
                        isLoading = state.isAnalyzing,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Status Strip: Profile + GitHub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Profile summary card
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
                            else state.profile.targetRole.ifBlank { "Profile active" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NavyPrimary,
                            maxLines = 1,
                        )
                        Text(
                            text = "${state.profile.claimedSkills.size} skills claimed",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                    }
                }

                // GitHub connection summary card
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
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NavyPrimary,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = "${state.githubState.repos.count { it.selected }} repos selected",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact Role Advisor Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToRoleAdvisor),
                color = NavyDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen.copy(alpha = 0.2f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "✦",
                        color = MintGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Role Advisor",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite,
                            ),
                        )
                        Text(
                            text = "Find roles matching your code and prepare targeted proof.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MintGreen.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                    Text(
                        text = "Ask Advisor →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Skills Needing Attention
        if (state.attentionSkills.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Skills needing attention",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    val latest = state.recentAnalyses.firstOrNull()
                    if (latest != null) {
                        TextButton(onClick = { onNavigateToProofPath(latest.id) }) {
                            Text(
                                text = "View ProofPath →",
                                style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
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
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                            )
                            Text(
                                text = if (skill.inProfile) "Claimed in profile, but unverified in code."
                                else "Not found in profile or analyzed repos.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                            )
                        }
                        EvidenceStatusBadge(status = skill.status)
                    }
                }
            }
        }

        // Recent Analyses Section
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Recent assessments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (state.recentAnalyses.isNotEmpty()) {
            items(state.recentAnalyses.take(3)) { analysis ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { onNavigateToComparison(analysis.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = analysis.displayTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NavyPrimary,
                                maxLines = 1,
                            )
                            if (!analysis.company.isNullOrBlank()) {
                                Text(
                                    text = analysis.company!!,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                )
                            }
                        }
                        Text(
                            text = "${analysis.requirements.size} reqs →",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No assessments recorded yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                        )
                        Text(
                            text = "Paste any job or internship description above to check your code evidence match.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
