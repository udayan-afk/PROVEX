package com.provex.app.ui.screens.proofloop

import androidx.compose.foundation.background
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.data.repository.SampleData
import com.provex.app.ui.components.EvidenceStatusBadge
import com.provex.app.ui.components.NoticeBanner
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
fun ProofLoopScreen(
    onBack: () -> Unit,
) {
    val proofLoop = remember { SampleData.proofLoop() }

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
                Text(
                    text = "ProofLoop Audit",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NavyPrimary,
                )
                SampleBadge(text = "Sample Audit")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Continuous skill tracking when new code is committed.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Critical rule explanation
            NoticeBanner(
                text = "ProofLoop Principle:\n" +
                    "A README-only or documentation commit will NOT upgrade a technical skill rating. " +
                    "Skills are only reassessed when code files directly relevant to that technology change.",
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Commit metadata card
            ProvexCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Latest Observed Commit",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(text = "Repository", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = proofLoop.repository ?: "No repository connected",
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Commit SHA", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = proofLoop.latestCommit ?: "—",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = DarkGreen,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Changed Files in Commit (${proofLoop.changedFiles.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    proofLoop.changedFiles.forEach { file ->
                        Text(
                            text = "• $file",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Skill Reassessment Audit",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Distinguishing re-evaluated skills from those untouched by this commit.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(proofLoop.skillChanges) { change ->
            ProvexCard(
                modifier = Modifier.padding(bottom = 10.dp),
                backgroundColor = if (change.reassessed) SurfaceWhite else SurfaceAlt,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (change.reassessed) DarkGreen else TextMuted, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = change.skill,
                                style = MaterialTheme.typography.titleLarge,
                                color = NavyPrimary,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (change.reassessed) DarkGreenBg else SurfaceAlt,
                        ) {
                            Text(
                                text = if (change.reassessed) "REASSESSED" else "UNCHANGED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (change.reassessed) DarkGreen else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Previous: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        EvidenceStatusBadge(status = change.previous)
                        Text(text = " → Current: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        EvidenceStatusBadge(status = change.current)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = change.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
