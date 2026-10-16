package com.provex.app.ui.screens.assessment

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.GithubEvidenceState
import com.provex.app.ui.components.EmptyStateView
import com.provex.app.ui.components.EvidenceStatusBadge
import com.provex.app.ui.components.ImportanceBadge
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.theme.AmberDark
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun ComparisonScreen(
    viewModel: ComparisonViewModel,
    onNavigateToEvidenceDetail: (String, String) -> Unit,
    onNavigateToProofPath: (String) -> Unit,
    onNavigateToSkeptic: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val analysis = state.analysis

    if (analysis == null) {
        EmptyStateView(
            title = "Loading comparison...",
            description = "Comparing job requirements with profile claims and repository evidence.",
        )
        return
    }

    val filteredList = if (state.filterStatus == null) state.assessments
    else state.assessments.filter { it.status == state.filterStatus }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        item {
            Text(
                text = "Skill Comparison",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = analysis.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Three-source comparison explanation banner
            NoticeBanner(
                text = "PROVEX compares three independent sources:\n" +
                    "1. Job requirements (from the posting)\n" +
                    "2. Profile claims (skills you selected)\n" +
                    "3. GitHub code evidence (commits, implementations, tests in connected repos)\n" +
                    "Absence of evidence does not mean you lack the skill; it simply means it wasn't demonstrated in the analyzed scope.",
            )

            if (state.notice != null) {
                Spacer(modifier = Modifier.height(10.dp))
                NoticeBanner(text = state.notice!!, isWarning = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EvidenceStatus.entries.forEach { status ->
                    val count = state.assessments.count { it.status == status }
                    val isSelected = state.filterStatus == status
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.setFilter(if (isSelected) null else status)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NavyPrimary else SurfaceWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = if (isSelected) SurfaceWhite else NavyPrimary,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = when (status) {
                                    EvidenceStatus.STRONG -> "Strong"
                                    EvidenceStatus.PARTIAL -> "Partial"
                                    EvidenceStatus.NOT_DEMONSTRATED -> "Missing"
                                    EvidenceStatus.NEEDS_VERIFICATION -> "Unverified"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) SurfaceWhite else TextSecondary,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Skills Assessed (${filteredList.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                TextButton(onClick = onNavigateToSkeptic) {
                    Text(
                        text = "Skeptic Review ↗",
                        style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(filteredList) { item ->
            ProvexCard(
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header: Skill name + Importance + Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = item.skill,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ImportanceBadge(importance = item.importance)
                        }
                        EvidenceStatusBadge(status = item.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Independent Source States
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Profile Claim State
                        Column {
                            Text(
                                text = "PROFILE CLAIM",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = TextMuted,
                            )
                            Text(
                                text = if (item.inProfile) "✓ Claimed by you" else "— Not listed in profile",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (item.inProfile) NavyPrimary else TextSecondary,
                                    fontWeight = if (item.inProfile) FontWeight.Medium else FontWeight.Normal,
                                ),
                            )
                        }

                        // GitHub Evidence State
                        Column {
                            Text(
                                text = "GITHUB CODE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = TextMuted,
                            )
                            Text(
                                text = when (item.github) {
                                    GithubEvidenceState.EVIDENCE_FOUND -> "✓ Verified in code"
                                    GithubEvidenceState.NO_EVIDENCE_FOUND -> "— None in analyzed scope"
                                    GithubEvidenceState.NOT_ANALYZED -> "⏳ No repos analyzed"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (item.github == GithubEvidenceState.EVIDENCE_FOUND) DarkGreen else TextSecondary,
                                    fontWeight = if (item.github == GithubEvidenceState.EVIDENCE_FOUND) FontWeight.Medium else FontWeight.Normal,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explanation
                    Text(
                        text = item.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (item.citations.isNotEmpty() || item.github == GithubEvidenceState.EVIDENCE_FOUND) {
                            TextButton(
                                onClick = { onNavigateToEvidenceDetail(analysis.id, item.skill) },
                            ) {
                                Text(
                                    text = "View code citations (${item.citations.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            ProvexButton(
                text = "Generate ProofPath Learning Plan →",
                onClick = { onNavigateToProofPath(analysis.id) },
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
