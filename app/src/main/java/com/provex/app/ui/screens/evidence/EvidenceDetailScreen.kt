package com.provex.app.ui.screens.evidence

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.ui.components.EmptyStateView
import com.provex.app.ui.components.EvidenceStatusBadge
import com.provex.app.ui.components.ImportanceBadge
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.theme.BorderMedium
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun EvidenceDetailScreen(
    viewModel: EvidenceDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val assessment = state.assessment

    if (assessment == null) {
        EmptyStateView(
            title = "Evidence not found",
            description = "No evaluation details were found for ${state.skillName}.",
            actionButtonText = "Back",
            onActionClick = onBack,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = assessment.skill,
                        style = MaterialTheme.typography.headlineMedium,
                        color = NavyPrimary,
                    )
                    Text(
                        text = "Category: ${assessment.category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                EvidenceStatusBadge(status = assessment.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Assessment Explanation Card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Assessment Explanation",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = assessment.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )

                    if (assessment.confidence != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Assessment Confidence: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextSecondary,
                            )
                            Text(
                                text = assessment.confidence.label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Limitations and Uncertainty
            if (assessment.limitations.isNotEmpty()) {
                ProvexCard(backgroundColor = SoftBlue) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Limitations & Scope Boundaries",
                            style = MaterialTheme.typography.titleMedium,
                            color = NavyPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        assessment.limitations.forEach { limitation ->
                            Text(
                                text = "• $limitation",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Validated Code Citations (${assessment.citations.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Citations are only shown when validated by the analysis service against repository revisions. PROVEX never fabricates code snippets.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (assessment.citations.isEmpty()) {
            item {
                NoticeBanner(
                    text = "No validated code citations were found in the analyzed repository scope for ${assessment.skill}. " +
                        "If you have implemented this in another repository, connect that project or add a feature in your learning plan.",
                )
            }
        } else {
            items(assessment.citations) { citation ->
                val codeScroll = rememberScrollState()
                ProvexCard(
                    modifier = Modifier.padding(bottom = 12.dp),
                    backgroundColor = SurfaceWhite,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = citation.repository,
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary,
                            )
                            Text(
                                text = "Rev: ${citation.commit.take(7)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        Text(
                            text = "${citation.filePath}:${citation.startLine}-${citation.endLine}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = DarkGreen,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(codeScroll),
                            shape = RoundedCornerShape(6.dp),
                            color = NavyDark,
                        ) {
                            Text(
                                text = citation.snippet,
                                color = SurfaceWhite,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                ),
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
