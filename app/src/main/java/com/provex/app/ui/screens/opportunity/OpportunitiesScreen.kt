package com.provex.app.ui.screens.opportunity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.AssistantChatMessage
import com.provex.app.domain.model.AssistantOpportunityCard
import com.provex.app.domain.model.AssistantPersonalizedGuidance
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexSecondaryButton
import com.provex.app.ui.theme.Amber
import com.provex.app.ui.theme.AmberBg
import com.provex.app.ui.theme.AmberDark
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.DarkGreenBg
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun OpportunitiesScreen(
    viewModel: OpportunitiesViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                OpportunitiesNavigationEvent.NavigateToHome -> onNavigateToHome()
                OpportunitiesNavigationEvent.NavigateToProfile -> onNavigateToProfile()
            }
        }
    }

    LaunchedEffect(state.messages.size, state.isGenerating) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Career Opportunity Assistant",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary,
                    )
                    Text(
                        text = "Find roles that fit your goals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                if (state.messages.isNotEmpty()) {
                    TextButton(onClick = viewModel::onClearConversation) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }

        // Main Conversation or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.messages.isEmpty() && !state.isGenerating) {
                AssistantEmptyState(
                    suggestedPrompts = state.suggestedPrompts,
                    onSelectPrompt = viewModel::onSelectPrompt,
                    onNavigateToProfile = viewModel::onNavigateToProfile,
                    profileHasSkills = state.profile.claimedSkills.isNotEmpty(),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(state.messages, key = { it.id }) { msg ->
                        if (msg.isUser) {
                            UserMessageBubble(text = msg.text)
                        } else {
                            AssistantMessageCard(
                                guidance = msg.guidance,
                                onAnalyzeRole = viewModel::onAnalyzeRole,
                                onNavigateToProfile = viewModel::onNavigateToProfile,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (state.isGenerating) {
                        item {
                            AssistantLoadingIndicator()
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Quick prompt chips bar if chat is active
        if (state.messages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.suggestedPrompts) { prompt ->
                    Surface(
                        color = SurfaceAlt,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable { viewModel.onSelectPrompt(prompt) },
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        // Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            tonalElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.inputQuery,
                    onValueChange = viewModel::onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Ask about a role (e.g. Android Intern)...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceAlt,
                        unfocusedContainerColor = SurfaceAlt,
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { viewModel.onSendQuery() },
                    ),
                    enabled = !state.isGenerating,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !state.isGenerating && state.inputQuery.isNotBlank()) {
                            viewModel.onSendQuery()
                        },
                    color = if (state.inputQuery.isNotBlank() && !state.isGenerating) NavyPrimary else TextMuted,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "↑",
                            color = SurfaceWhite,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantEmptyState(
    suggestedPrompts: List<String>,
    onSelectPrompt: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    profileHasSkills: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = NavyPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "✦",
                    color = MintGreen,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Explore Target Roles",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter any developer role or internship you are interested in. The assistant analyzes commonly requested skills, compares them with your profile, and outlines concrete preparation steps.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        if (!profileHasSkills) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = AmberBg,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToProfile),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "💡", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set up your skills in Career Profile for personalized gap analysis.",
                        style = MaterialTheme.typography.labelSmall.copy(color = AmberDark),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Edit →",
                        style = MaterialTheme.typography.labelSmall.copy(color = AmberDark, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SUGGESTED ROLES",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestedPrompts.forEach { prompt ->
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onSelectPrompt(prompt) },
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NavyPrimary,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            color = NavyPrimary,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(color = SurfaceWhite),
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantMessageCard(
    guidance: AssistantPersonalizedGuidance?,
    onAnalyzeRole: (String, String?) -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    if (guidance == null) return

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MintGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ROLE INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }

                Text(
                    text = "Guidance Archetype",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = guidance.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )

            if (guidance.profileMissingPrompt != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = AmberBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToProfile),
                ) {
                    Text(
                        text = guidance.profileMissingPrompt,
                        style = MaterialTheme.typography.labelSmall.copy(color = AmberDark),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skills Breakdown (Matched vs Strengthen)
            Text(
                text = "SKILLS OVERVIEW",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (guidance.matchedProfileSkills.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Claimed in profile:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = DarkGreen,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    guidance.matchedProfileSkills.forEach { skill ->
                        Surface(
                            color = DarkGreenBg,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "✓ $skill",
                                style = MaterialTheme.typography.labelSmall.copy(color = DarkGreen, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (guidance.skillsToStrengthen.isNotEmpty()) {
                Text(
                    text = "Recommended to strengthen:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = AmberDark,
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    guidance.skillsToStrengthen.forEach { skill ->
                        Surface(
                            color = AmberBg,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "+ $skill",
                                style = MaterialTheme.typography.labelSmall.copy(color = AmberDark, fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            // Opportunity Cards
            Text(
                text = "RECOMMENDED ROLE ARCHETYPES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            guidance.opportunityCards.forEach { card ->
                OpportunityCardItem(
                    card = card,
                    onAnalyze = { onAnalyzeRole(card.roleTitle, card.sampleJobDescription) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Project Ideas & Preparation
            Text(
                text = "SUGGESTED PROJECT EVIDENCE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))

            guidance.projectIdeas.forEach { idea ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "•", color = NavyPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = idea,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Honest Search Notice
            NoticeBanner(
                text = guidance.searchOptionsExplanation,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Action Button
            ProvexButton(
                text = "Analyze a Job Description in PROVEX",
                onClick = {
                    val defaultCard = guidance.opportunityCards.firstOrNull()
                    onAnalyzeRole(
                        defaultCard?.roleTitle ?: guidance.roleQuery,
                        defaultCard?.sampleJobDescription,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpportunityCardItem(
    card: AssistantOpportunityCard,
    onAnalyze: () -> Unit,
) {
    Surface(
        color = SurfaceAlt,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        color = SurfaceWhite,
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.sourceLabel,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp),
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

@Composable
private fun AssistantLoadingIndicator() {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = NavyPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Analyzing career role requirements & profile fit...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
