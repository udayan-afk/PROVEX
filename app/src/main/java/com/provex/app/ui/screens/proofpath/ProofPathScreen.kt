package com.provex.app.ui.screens.proofpath

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.ui.components.EmptyStateView
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.SampleBadge
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.DarkGreenBg
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun ProofPathScreen(
    viewModel: ProofPathViewModel,
    onNavigateToProofLoop: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (state.analysis == null) {
        EmptyStateView(
            title = "Loading ProofPath...",
            description = "Generating actionable learning paths for identified evidence gaps.",
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
            Text(
                text = "ProofPath Learning Plan",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Target: ${state.analysis!!.displayTitle}",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Critical rule notice
            NoticeBanner(
                text = "ProofPath Principle:\n" +
                    "Completing tutorials, reading documentation, or checking off criteria will NOT automatically raise your skill rating. " +
                    "To upgrade your rating, commit working code to a connected repository so ProofLoop can verify it.",
            )

            if (state.notice != null) {
                Spacer(modifier = Modifier.height(10.dp))
                NoticeBanner(text = state.notice!!, isWarning = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Actionable Gap Remediation (${state.items.size} skills)",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.items.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No evidence gaps found!",
                    description = "All required skills for this opportunity have strong evidence.",
                )
            }
        } else {
            items(state.items) { item ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 14.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.skill,
                                style = MaterialTheme.typography.titleLarge,
                                color = NavyPrimary,
                            )
                            if (item.illustrative) {
                                SampleBadge(text = "Template Task")
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.whyItMatters,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Learning Resources
                        Text(
                            text = "RECOMMENDED RESOURCES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        item.resources.forEach { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = res.url != null) {
                                        if (res.url != null) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                                            context.startActivity(intent)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = "• ${res.title}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (res.url != null) NavyPrimary else TextSecondary,
                                            fontWeight = if (res.url != null) FontWeight.Medium else FontWeight.Normal,
                                        ),
                                    )
                                    if (!res.verified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(illustrative)",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp),
                                        )
                                    }
                                }
                                if (res.url != null) {
                                    Text(
                                        text = "Open ↗",
                                        style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Practical Repository-Specific Task
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceAlt,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "PROJECT TASK: ${item.taskTitle}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = NavyPrimary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.taskDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Acceptance Criteria for Evidence:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                item.acceptanceCriteria.forEachIndexed { idx, criterion ->
                                    val key = "${state.analysisId}:${item.skill}:$idx"
                                    val isChecked = key in state.completedCriteria

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleCriterion(item.skill, idx) }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { viewModel.toggleCriterion(item.skill, idx) },
                                            colors = CheckboxDefaults.colors(checkedColor = DarkGreen),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = criterion,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isChecked) TextMuted else TextPrimary,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            ProvexButton(
                text = "Check Commit Audit in ProofLoop →",
                onClick = onNavigateToProofLoop,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
