package com.provex.app.ui.screens.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexSecondaryButton
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.ErrorRed
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToRepositories: () -> Unit,
    onSignOut: () -> Unit,
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
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage account, notifications, and on-device storage.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account status card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Account & Authentication",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (val a = state.authState) {
                            is AuthState.Account -> "Signed in as: ${a.email}"
                            is AuthState.Demo -> "Operating in Local Demo Mode (No account required)"
                            AuthState.SignedOut -> "Signed out"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = NavyPrimary,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexSecondaryButton(
                        text = "Edit Career Profile",
                        onClick = onNavigateToProfile,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connected GitHub
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Connected GitHub",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (state.githubState.status) {
                            RepoConnectionStatus.CONNECTED -> if (state.githubState.isSample) "Demo Repositories Enabled" else "Connected"
                            RepoConnectionStatus.CONNECTING -> "Connecting..."
                            RepoConnectionStatus.NOT_CONNECTED -> "No GitHub account connected"
                            RepoConnectionStatus.ERROR -> "Connection Error"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.githubState.status == RepoConnectionStatus.CONNECTED) DarkGreen else TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProvexSecondaryButton(
                        text = "Manage Repositories",
                        onClick = onNavigateToRepositories,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification preferences
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Notifications & Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "ProofLoop Commit Alerts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(text = "Notify when a push affects your evaluated skills", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = state.notifyProofLoop,
                            onCheckedChange = viewModel::onToggleNotifyProofLoop,
                            colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = NavyPrimary),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "ProofPath Reminders", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(text = "Periodic reminders to work on practical project tasks", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = state.notifyLearning,
                            onCheckedChange = viewModel::onToggleNotifyLearning,
                            colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = NavyPrimary),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Management Card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reset all saved profiles, preferences, and cached analyses stored locally on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ProvexSecondaryButton(
                        text = "Reset Local App Data",
                        onClick = { viewModel.onClearAllData(onSignOut) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProvexButton(
                text = "Sign Out",
                onClick = { viewModel.onSignOut(onSignOut) },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
