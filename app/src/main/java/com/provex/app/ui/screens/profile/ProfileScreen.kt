package com.provex.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.WorkMode
import com.provex.app.domain.usecase.SkillCatalog
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexTextField
import com.provex.app.ui.theme.BorderMedium
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.DarkGreenBg
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
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
                text = "Career Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure your target role and skills you report knowing.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            NoticeBanner(
                text = "Important: Profile skills are self-reported claims. " +
                    "PROVEX never treats self-selected skills as verified until supported by analyzed code evidence.",
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "General Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        label = "Full name",
                        placeholder = "e.g. Alex Chen",
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.targetRole,
                        onValueChange = viewModel::onTargetRoleChanged,
                        label = "Target role",
                        placeholder = "e.g. Android Engineer Intern / Junior Developer",
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.level,
                        onValueChange = viewModel::onLevelChanged,
                        label = "Education / Experience level",
                        placeholder = "e.g. 3rd-year CS Undergraduate, Master's, Bootcamp",
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexTextField(
                        value = state.location,
                        onValueChange = viewModel::onLocationChanged,
                        label = "Location preference",
                        placeholder = "e.g. San Francisco, New York, Remote",
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Work arrangement preference",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WorkMode.entries.forEach { mode ->
                            val isSelected = state.workMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.onWorkModeChanged(mode) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NavyPrimary else SurfaceAlt,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) SurfaceWhite else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Programming Languages Section
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Programming Languages",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Select the languages you have studied or used in projects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkillCatalog.languageNames.forEach { lang ->
                            val selected = lang in state.selectedLanguages
                            SkillChip(
                                label = lang,
                                selected = selected,
                                onClick = { viewModel.toggleLanguage(lang) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frameworks & Databases Section
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Frameworks & Databases",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Select frameworks and database systems you are familiar with.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkillCatalog.frameworkNames.forEach { fw ->
                            val selected = fw in state.selectedFrameworks
                            SkillChip(
                                label = fw,
                                selected = selected,
                                onClick = { viewModel.toggleFramework(fw) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tools & Architecture Section
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Testing, Cloud & Architecture",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkillCatalog.generalNames.forEach { sk ->
                            val selected = sk in state.selectedOtherSkills
                            SkillChip(
                                label = sk,
                                selected = selected,
                                onClick = { viewModel.toggleOtherSkill(sk) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProvexButton(
                text = if (state.isSaved) "✓ Profile Saved" else "Save Career Profile",
                onClick = viewModel::saveProfile,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SkillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) NavyPrimary else SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) NavyPrimary else BorderMedium,
        ),
    ) {
        Text(
            text = (if (selected) "✓ " else "+ ") + label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) SurfaceWhite else TextPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
