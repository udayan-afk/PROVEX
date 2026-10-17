package com.provex.app.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.AssistantOpportunityCard
import com.provex.app.domain.model.Opportunity
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.SampleBadge
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRoleAdvisor: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                ExploreNavigationEvent.NavigateToHome -> onNavigateToHome()
                is ExploreNavigationEvent.NavigateToRoleAdvisor -> onNavigateToRoleAdvisor(event.initialPrompt)
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

            Text(
                text = "Explore Opportunities",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Career tracks, role archetypes, and market expectations.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(14.dp))

            NoticeBanner(
                text = "Data Integrity Policy: PROVEX never fabricates employers, live vacancies, salaries, or deadlines. Browse curated career archetypes below to discover market expectations and compare your code evidence.",
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.availableCategories) { cat ->
                    val selected = state.selectedCategory == cat
                    Surface(
                        color = if (selected) NavyPrimary else SurfaceWhite,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) NavyPrimary else MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.clickable { viewModel.onCategorySelected(cat) },
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selected) SurfaceWhite else TextPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "CURATED ROLE ARCHETYPES (${state.roleArchetypes.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(state.roleArchetypes, key = { it.id }) { card ->
            ExploreRoleCard(
                card = card,
                onAnalyze = { viewModel.onAnalyzeRole(card.roleTitle, card.sampleJobDescription) },
                onConsultAdvisor = { viewModel.onConsultAdvisor(card.roleTitle) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Sample Opportunities Toggle Section
        item {
            Spacer(modifier = Modifier.height(14.dp))

            ProvexCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sample Vacancy Previews",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Text(
                            text = "Show illustrative job postings to preview evaluation workflows.",
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

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (state.sampleEnabled && state.sampleOpportunities.isNotEmpty()) {
            items(state.sampleOpportunities) { opp ->
                ProvexCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = opp.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary,
                            )
                            SampleBadge()
                        }
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
            // Extra bottom spacing to ensure comfortable scrolling above the floating bubble & bottom bar
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreRoleCard(
    card: AssistantOpportunityCard,
    onAnalyze: () -> Unit,
    onConsultAdvisor: () -> Unit,
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.roleTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    color = NavyPrimary,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = card.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MintGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            if (!card.location.isNullOrBlank() || !card.workMode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(card.location, card.workMode).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = card.relevanceExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                card.relevantSkills.forEach { skill ->
                    Surface(
                        color = SurfaceAlt,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            text = skill,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✦ Ask Advisor",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NavyPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .clickable(onClick = onConsultAdvisor)
                        .padding(vertical = 4.dp),
                )

                Text(
                    text = "Analyze in PROVEX →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier
                        .clickable(onClick = onAnalyze)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
