package com.provex.app.ui.screens.opportunity

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
import com.provex.app.domain.model.DataOrigin
import com.provex.app.domain.model.Importance
import com.provex.app.domain.model.RequirementCategory
import com.provex.app.ui.components.EmptyStateView
import com.provex.app.ui.components.ImportanceBadge
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexTextField
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.ErrorRed
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun ExtractionScreen(
    viewModel: ExtractionViewModel,
    onNavigateToComparison: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val analysis = state.analysis

    if (analysis == null) {
        EmptyStateView(
            title = "Loading analysis...",
            description = "Extracting job requirements and qualifications.",
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
                text = "Job Requirement Extraction",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Review, correct, and verify requirements extracted from the posting.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            NoticeBanner(
                text = if (analysis.origin == DataOrigin.ON_DEVICE) {
                    "Extracted on-device using literal keyword matching. " +
                        "No assumptions or unmentioned skills have been added. You can adjust importance or add skills below."
                } else {
                    "Extracted by the PROVEX backend service. Review requirements for accuracy."
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Identified metadata card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Opportunity Metadata",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChanged,
                        label = "Job title",
                        placeholder = "e.g. Android Engineer Intern",
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.company,
                        onValueChange = viewModel::onCompanyChanged,
                        label = "Company name",
                        placeholder = "e.g. Acme Corporation",
                    )

                    if (!analysis.sourceUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Source Link: ${analysis.sourceUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NavyPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Extracted Requirements (${state.requirements.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Text(
                    text = "Tap badge to change",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Group requirements by Category
        RequirementCategory.entries.forEach { category ->
            val inCat = state.requirements.filter { it.category == category }
            if (inCat.isNotEmpty()) {
                item {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NavyPrimary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                }

                items(inCat) { req ->
                    ProvexCard(
                        modifier = Modifier.padding(bottom = 6.dp),
                        backgroundColor = SurfaceWhite,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                if (req.sourceText != null) {
                                    Text(
                                        text = "\"${req.sourceText}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary,
                                        maxLines = 2,
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.clickable { viewModel.toggleImportance(req.id) },
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    ImportanceBadge(importance = req.importance)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "✕",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .clickable { viewModel.removeRequirement(req.id) }
                                        .padding(4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add custom requirement card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Add missing requirement",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProvexTextField(
                            value = state.newSkillName,
                            onValueChange = viewModel::onNewSkillNameChanged,
                            label = "",
                            placeholder = "e.g. Jetpack Compose, GraphQL, Git",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = viewModel::addCustomRequirement) {
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelLarge.copy(color = NavyPrimary),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProvexButton(
                text = "Proceed to Skill Comparison →",
                onClick = {
                    viewModel.saveAndProceed {
                        onNavigateToComparison(analysis.id)
                    }
                },
                isLoading = state.isSaving,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
