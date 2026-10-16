package com.provex.app.ui.screens.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.ui.components.NoticeBanner
import com.provex.app.ui.components.ProvexButton
import com.provex.app.ui.components.ProvexCard
import com.provex.app.ui.components.ProvexSecondaryButton
import com.provex.app.ui.components.ProvexTextField
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                LoginNavigationEvent.NavigateToHome -> onNavigateToHome()
                LoginNavigationEvent.NavigateToRegister -> onNavigateToRegister()
            }
        }
    }

    if (state.showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissForgotPasswordDialog,
            title = {
                Text(text = "Password Recovery", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = "Password reset requires an active PROVEX backend authentication service. " +
                        "In local/offline mode, no user credentials are stored on a remote server.\n\n" +
                        "You can use Demo Mode to explore all evaluation workflows locally on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDismissForgotPasswordDialog) {
                    Text(text = "Understood", color = NavyPrimary)
                }
            },
        )
    }

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

        // PROVEX Identity Monogram
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = NavyPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "</>",
                    color = MintGreen,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "PROVEX",
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 1.sp),
            color = NavyPrimary,
        )

        Text(
            text = "Turn Code Into Credibility.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = DarkGreen,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Login Card
        ProvexCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    text = "Continue building your career credibility.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )

                // Email Field
                ProvexTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Email address",
                    placeholder = "student@university.edu",
                    errorText = state.emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !state.isLoading,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field with Show/Hide toggle
                ProvexTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Password",
                    placeholder = "Enter your password",
                    errorText = state.passwordError,
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !state.isLoading,
                    trailingIcon = {
                        Text(
                            text = if (state.isPasswordVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier
                                .clickable(onClick = viewModel::onTogglePasswordVisibility)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Remember me & Forgot password row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.onRememberMeChanged(!state.rememberMe) },
                    ) {
                        Checkbox(
                            checked = state.rememberMe,
                            onCheckedChange = viewModel::onRememberMeChanged,
                            colors = CheckboxDefaults.colors(checkedColor = NavyPrimary),
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                        )
                    }

                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NavyPrimary,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier
                            .clickable(onClick = viewModel::onForgotPasswordClicked)
                            .padding(vertical = 4.dp),
                    )
                }

                if (state.generalError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    NoticeBanner(text = state.generalError!!, isWarning = true)
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProvexButton(
                    text = "Sign In",
                    onClick = viewModel::onSignIn,
                    isLoading = state.isLoading,
                    enabled = !state.isLoading,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Demo Mode Option
        ProvexSecondaryButton(
            text = "Continue in Demo Mode (On-Device)",
            onClick = viewModel::onEnterDemoMode,
            enabled = !state.isLoading,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Explore all features locally with zero setup. No backend credentials required.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Create Account Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Create an account",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .clickable(onClick = viewModel::onNavigateToRegister)
                    .padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
