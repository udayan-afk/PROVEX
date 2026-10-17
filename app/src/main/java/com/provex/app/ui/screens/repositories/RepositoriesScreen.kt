package com.provex.app.ui.screens.repositories

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.ui.components.EmptyStateView
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.SampleBadge
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

@Composable
fun RepositoriesScreen(
    viewModel: RepositoriesViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val gh = state.githubState
    val context = LocalContext.current
    var showDisconnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.authUrlToLaunch) {
        state.authUrlToLaunch?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.clearAuthUrl()
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
                    text = "Are you sure you want to disconnect @${gh.accountLogin ?: "GitHub"}? Your stored credentials will be securely wiped from the PROVEX server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        viewModel.onDisconnect()
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
                text = "GitHub Repositories",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect repositories to find evidence for required job skills.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Card: Why access is needed & security
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Why PROVEX Needs Repository Access",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PROVEX inspects code structures, dependencies, test assertions, and commits in repositories you choose. " +
                            "This turns self-selected skills into independently verified evidence.\n\n" +
                            "Security Guarantee:\n" +
                            "• Never paste Personal Access Tokens into the app.\n" +
                            "• PROVEX uses secure OAuth flows negotiated through the PROVEX backend.\n" +
                            "• Read-only access to selected repositories only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection status card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Connection Status",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            when (gh.status) {
                                                RepoConnectionStatus.CONNECTED -> DarkGreen
                                                RepoConnectionStatus.CONNECTING -> SoftBlue
                                                RepoConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                                RepoConnectionStatus.NOT_CONNECTED -> TextMuted
                                            },
                                            CircleShape,
                                        ),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (gh.status) {
                                        RepoConnectionStatus.CONNECTED -> if (gh.isSample) "Demo Mode (Sample)" else "Connected"
                                        RepoConnectionStatus.CONNECTING -> "Connecting..."
                                        RepoConnectionStatus.ERROR -> "Connection Error"
                                        RepoConnectionStatus.NOT_CONNECTED -> "Not Connected"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = when (gh.status) {
                                        RepoConnectionStatus.CONNECTED -> DarkGreen
                                        RepoConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                        else -> TextSecondary
                                    },
                                )
                            }
                        }

                        if (gh.status == RepoConnectionStatus.CONNECTED && gh.isSample) {
                            SampleBadge()
                        }
                    }

                    if (gh.accountLogin != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Account: @${gh.accountLogin}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = NavyPrimary,
                        )
                    }

                    if (state.feedbackMessage != null || gh.message != null) {
                        val msg = state.feedbackMessage ?: gh.message.orEmpty()
                        if (msg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            NoticeBanner(
                                text = msg,
                                isWarning = gh.status == RepoConnectionStatus.ERROR,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (gh.status == RepoConnectionStatus.CONNECTED && !gh.isSample) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ProvexButton(
                                text = "Sync Repos",
                                onClick = viewModel::onRefresh,
                                modifier = Modifier.weight(1f),
                            )
                            ProvexButton(
                                text = "Disconnect",
                                onClick = { showDisconnectDialog = true },
                                modifier = Modifier.weight(1f),
                                isLoading = state.isDisconnecting,
                            )
                        }
                    } else if (gh.status == RepoConnectionStatus.ERROR) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ProvexButton(
                                text = "Retry Connection",
                                onClick = viewModel::onConnectClicked,
                                isLoading = state.isConnecting,
                                modifier = Modifier.weight(1f),
                            )
                            ProvexButton(
                                text = "Refresh Status",
                                onClick = viewModel::onRefresh,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        ProvexButton(
                            text = "Connect GitHub Account",
                            onClick = viewModel::onConnectClicked,
                            isLoading = state.isConnecting,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Demo / Sample repositories toggle
            ProvexCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Sample Repositories",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Explore repository selection and audit workflows without a live backend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = gh.isSample,
                        onCheckedChange = viewModel::onToggleSampleRepos,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = NavyPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Select repositories for analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Only selected repositories are scanned for code evidence during job fit analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (gh.repos.isEmpty()) {
            item {
                EmptyStateView(
                    title = if (gh.status == RepoConnectionStatus.CONNECTED) "No repositories found" else "No repositories available",
                    description = if (gh.status == RepoConnectionStatus.CONNECTED)
                        "Your connected GitHub account doesn't have any accessible repositories yet."
                    else
                        "Connect your GitHub account or enable sample repositories above to select projects for analysis.",
                )
            }
        } else {
            items(gh.repos) { repo ->
                ProvexCard(
                    modifier = Modifier.padding(bottom = 10.dp),
                    onClick = { viewModel.onToggleRepoSelection(repo.id) },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = repo.selected,
                                onCheckedChange = { viewModel.onToggleRepoSelection(repo.id) },
                                colors = CheckboxDefaults.colors(checkedColor = NavyPrimary),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = repo.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                    )
                                    if (repo.isSample) {
                                        SampleBadge()
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (repo.isPrivate) SurfaceAlt else DarkGreenBg,
                                    ) {
                                        Text(
                                            text = if (repo.isPrivate) "Private" else "Public",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (repo.isPrivate) TextSecondary else DarkGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                if (repo.owner.isNotEmpty()) {
                                    Text(
                                        text = "owner: ${repo.owner}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                        }

                        if (!repo.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = repo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 40.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (repo.language != null) {
                                Text(
                                    text = "• ${repo.language}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = NavyPrimary,
                                )
                            }
                            if (repo.defaultBranch != null) {
                                Text(
                                    text = "branch: ${repo.defaultBranch}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                            if (repo.updatedAt != null) {
                                val dateStr = repo.updatedAt.take(10)
                                Text(
                                    text = "Updated: $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                        }

                        if (repo.htmlUrl != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = repo.htmlUrl,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SoftBlue,
                                modifier = Modifier
                                    .padding(start = 40.dp)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))
                                        context.startActivity(intent)
                                    },
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
