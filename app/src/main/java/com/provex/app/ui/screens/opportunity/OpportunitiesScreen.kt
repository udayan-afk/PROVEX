package com.provex.app.ui.screens.opportunity

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.provex.app.ui.components.EmptyStateView
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.SampleBadge
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun OpportunitiesScreen(
    viewModel: OpportunitiesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        item {
            Text(
                text = "Target Opportunities",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Job and internship openings matching your profile and verified skills.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            NoticeBanner(
                text = "Opportunity Data Integrity:\n" +
                    "PROVEX never fabricates companies, active vacancies, salaries, deadlines, or application links. " +
                    "Live opportunities appear here when connected to partner job boards via the PROVEX backend.",
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProvexCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sample Opportunities Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Show illustrative roles to preview the interface.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = state.sampleEnabled,
                        onCheckedChange = viewModel::onToggleSample,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = NavyPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Available Opportunities (${state.opportunities.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.opportunities.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No active opportunities",
                    description = "Enable the sample preview above or connect to the PROVEX backend for live feeds.",
                )
            }
        } else {
            items(state.opportunities) { opp ->
                ProvexCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = opp.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = NavyPrimary,
                            )
                            if (opp.isSample) {
                                SampleBadge()
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${opp.company} • ${opp.location}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
