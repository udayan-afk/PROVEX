package com.provex.app.ui.screens.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexSecondaryButton
import com.provex.app.ui.components.ProvexTextField
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.DarkGreenBg
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // PROVEX Identity
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = NavyPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "</>",
                    color = MintGreen,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PROVEX",
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 1.sp),
            color = NavyPrimary,
        )

        Text(
            text = "Turn Code Into Credibility.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = DarkGreen,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bridge the gap between self-selected profile claims and code evidence discovered in your repositories.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Clarification notice: Account auth vs GitHub auth
        NoticeBanner(
            text = "PROVEX account authentication is separate from GitHub repository authorization. " +
                "Connecting GitHub grants read access to analyze your repositories for evidence.",
            modifier = Modifier.padding(bottom = 20.dp),
        )

        // Main auth card
        ProvexCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (state.isRegistering) "Create your PROVEX account" else "Sign in to PROVEX",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                ProvexTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Email address",
                    placeholder = "student@university.edu",
                    errorText = state.emailError,
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProvexTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Password",
                    placeholder = "Minimum 8 characters",
                    errorText = state.passwordError,
                )

                if (state.generalError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NoticeBanner(text = state.generalError!!, isWarning = true)
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProvexButton(
                    text = if (state.isRegistering) "Create account" else "Sign in",
                    onClick = viewModel::onSubmit,
                    isLoading = state.isLoading,
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = viewModel::onToggleMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.isRegistering) "Already have an account? Sign in"
                        else "New to PROVEX? Create an account",
                        style = MaterialTheme.typography.bodyMedium.copy(color = NavyPrimary),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Demo Mode Section (Truthful, honest fallback when backend is not configured)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProvexSecondaryButton(
                text = "Continue in Demo Mode (On-Device)",
                onClick = {
                    viewModel.onEnterDemo()
                    onAuthSuccess()
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Demo mode stores your profile and analyses locally on this device. " +
                    "No cloud account or backend is required.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
