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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.provex.app.R
import com.provex.app.domain.model.RepoConnectionStatus
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
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRepositories: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDisconnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.githubAuthUrlToLaunch) {
        state.githubAuthUrlToLaunch?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.clearGithubAuthUrl()
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = {
                Text(
                    text = "Disconnect GitHub?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to disconnect @${state.githubState.accountLogin ?: "GitHub"}? Your stored credentials will be securely revoked and removed from the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        viewModel.onDisconnectGithub()
                    }
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisconnectDialog = false }
                ) {
                    Text("Cancel", color = NavyPrimary)
                }
            },
        )
    }

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

            // GitHub Connection Card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NavyPrimary,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_github),
                                        contentDescription = "GitHub",
                                        tint = SurfaceWhite,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "GitHub Account",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                )
                                Text(
                                    text = when (state.githubState.status) {
                                        RepoConnectionStatus.CONNECTED -> "Connected as @${state.githubState.accountLogin ?: "user"}"
                                        RepoConnectionStatus.CONNECTING -> "Connecting..."
                                        RepoConnectionStatus.ERROR -> "Connection Error"
                                        RepoConnectionStatus.NOT_CONNECTED -> "Not connected"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (state.githubState.status) {
                                        RepoConnectionStatus.CONNECTED -> DarkGreen
                                        RepoConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                        else -> TextSecondary
                                    },
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (state.githubState.status) {
                                RepoConnectionStatus.CONNECTED -> DarkGreenBg
                                RepoConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                                else -> SurfaceAlt
                            },
                        ) {
                            Text(
                                text = when (state.githubState.status) {
                                    RepoConnectionStatus.CONNECTED -> "Active"
                                    RepoConnectionStatus.CONNECTING -> "Pending"
                                    RepoConnectionStatus.ERROR -> "Error"
                                    RepoConnectionStatus.NOT_CONNECTED -> "Disconnected"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = when (state.githubState.status) {
                                    RepoConnectionStatus.CONNECTED -> DarkGreen
                                    RepoConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                    else -> TextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (state.githubActionMessage != null || state.githubState.message != null) {
                        val msg = state.githubActionMessage ?: state.githubState.message.orEmpty()
                        if (msg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            NoticeBanner(
                                text = msg,
                                isWarning = state.githubState.status == RepoConnectionStatus.ERROR,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (state.githubState.status) {
                        RepoConnectionStatus.CONNECTED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProvexButton(
                                    text = "View Connected Repositories (${state.githubState.repos.size})",
                                    onClick = onNavigateToRepositories,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ProvexButton(
                                        text = "Refresh",
                                        onClick = viewModel::onRefreshGithub,
                                        modifier = Modifier.weight(1f),
                                    )
                                    ProvexButton(
                                        text = "Disconnect",
                                        onClick = { showDisconnectDialog = true },
                                        modifier = Modifier.weight(1f),
                                        isLoading = state.isDisconnectingGithub,
                                    )
                                }
                            }
                        }
                        RepoConnectionStatus.CONNECTING -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = NavyPrimary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Authorizing with GitHub...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                        RepoConnectionStatus.ERROR -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ProvexButton(
                                    text = "Retry Connection",
                                    onClick = viewModel::onConnectGithub,
                                    isLoading = state.isConnectingGithub,
                                    modifier = Modifier.weight(1f),
                                )
                                ProvexButton(
                                    text = "Refresh",
                                    onClick = viewModel::onRefreshGithub,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        RepoConnectionStatus.NOT_CONNECTED -> {
                            ProvexButton(
                                text = "Connect GitHub",
                                onClick = viewModel::onConnectGithub,
                                isLoading = state.isConnectingGithub,
                            )
                        }
                    }
                }
            }

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

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                NoticeBanner(
                    text = state.errorMessage!!,
                    isWarning = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                com.provex.app.ui.components.ProvexSecondaryButton(
                    text = "Retry Sync with Server",
                    onClick = viewModel::syncProfile,
                    enabled = !state.isSyncing,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProvexButton(
                text = if (state.isSaved) "✓ Profile Saved" else "Save Career Profile",
                onClick = viewModel::saveProfile,
                isLoading = state.isLoading,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ACCOUNT & SECONDARY FEATURES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToRepositories),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Connected Repositories",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NavyPrimary,
                        )
                        Text(
                            text = "Manage GitHub account and select code repositories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Text(text = "⌥", fontSize = 18.sp, color = NavyPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSettings),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Settings & Privacy",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NavyPrimary,
                        )
                        Text(
                            text = "Session state, notifications, and local data controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Text(text = "⚙", fontSize = 18.sp, color = NavyPrimary)
                }
            }

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
