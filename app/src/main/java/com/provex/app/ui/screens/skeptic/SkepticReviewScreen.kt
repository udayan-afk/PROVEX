package com.provex.app.ui.screens.skeptic

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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.provex.app.data.repository.SampleData
import com.provex.app.ui.components.EvidenceStatusBadge
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.SampleBadge
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.ErrorRed
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun SkepticReviewScreen(
    onBack: () -> Unit,
) {
    val reviews = remember { SampleData.skepticReviews }

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
                    text = "Skeptic Review",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NavyPrimary,
                )
                SampleBadge(text = "Sample Cases")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Adversarial second-pass verification that prevents superficial claims.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            NoticeBanner(
                text = "Why Skeptic Review exists:\n" +
                    "A naive AI might see a Dockerfile and claim 'Student knows container deployment'. " +
                    "The Skeptic Review challenges this: Was the image tested? Did CI build it? Was it deployed?\n" +
                    "The second review is weighed critically against evidence—never blindly accepted.",
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        items(reviews) { item ->
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EvidenceStatusBadge(status = item.initialStatus)
                            Text(text = " → ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            EvidenceStatusBadge(status = item.validatedStatus)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Initial Claim
                    Text(
                        text = "INITIAL AI CLAIM",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                    )
                    Text(
                        text = item.initialClaim,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Challenged limitation
                    Text(
                        text = "SKEPTIC CHALLENGE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ErrorRed),
                    )
                    Text(
                        text = item.challenge,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Observed Supporting Evidence
                    Text(
                        text = "OBSERVED EVIDENCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                    )
                    Text(
                        text = item.supportingEvidence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Validated Conclusion
                    ProvexCard(backgroundColor = SurfaceAlt) {
                        Column {
                            Text(
                                text = "VALIDATED REASONING",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DarkGreen),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
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
