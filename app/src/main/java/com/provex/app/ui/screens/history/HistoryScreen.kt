package com.provex.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToComparison: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        item {
            Text(
                text = "Career Time Machine",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Historical timeline of assessment snapshots and evidence progression.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            NoticeBanner(
                text = "Preserved Evidence History:\n" +
                    "PROVEX keeps an immutable record of each evaluation and edit. " +
                    "Snapshots are never overwritten, allowing you to see exactly how new commits moved the needle over time.",
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Assessment Snapshots (${state.snapshots.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.snapshots.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No snapshots recorded yet",
                    description = "Analyze an opportunity or edit requirements to create historical assessment snapshots.",
                )
            }
        } else {
            items(state.snapshots) { snapshot ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = { onNavigateToComparison(snapshot.analysisId) },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = snapshot.jobTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = NavyPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "View ↗",
                                style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = dateFormat.format(Date(snapshot.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            Text(
                                text = "Trigger: ${snapshot.trigger}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = TextSecondary,
                            )
                        }

                        if (snapshot.revision != null) {
                            Text(
                                text = "Repo Revision: ${snapshot.revision}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = DarkGreen,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Assessed Skills Summary (${snapshot.assessments.size} skills):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val strongCount = snapshot.assessments.count { it.status == com.provex.app.domain.model.EvidenceStatus.STRONG }
                            val partialCount = snapshot.assessments.count { it.status == com.provex.app.domain.model.EvidenceStatus.PARTIAL }
                            val unverifiedCount = snapshot.assessments.count { it.status == com.provex.app.domain.model.EvidenceStatus.NEEDS_VERIFICATION }

                            Text(
                                text = "$strongCount Strong",
                                style = MaterialTheme.typography.labelSmall.copy(color = DarkGreen, fontWeight = FontWeight.SemiBold),
                            )
                            Text(text = "•", color = TextMuted)
                            Text(
                                text = "$partialCount Partial",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            )
                            Text(text = "•", color = TextMuted)
                            Text(
                                text = "$unverifiedCount Unverified",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
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
