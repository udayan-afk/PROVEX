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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                RegisterNavigationEvent.NavigateToHome -> onNavigateToHome()
                RegisterNavigationEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
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
        Spacer(modifier = Modifier.height(16.dp))

        // PROVEX Identity Monogram
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = NavyPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "</>",
                    color = MintGreen,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PROVEX",
            style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 1.sp),
            color = NavyPrimary,
        )

        Text(
            text = "Turn Code Into Credibility.",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = DarkGreen,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Registration Card
        ProvexCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Create your account",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    text = "Start turning your code into credible career evidence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )

                // Full Name Field
                ProvexTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onFullNameChanged,
                    label = "Full name",
                    placeholder = "Alex Chen",
                    errorText = state.fullNameError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                    ),
                    enabled = !state.isLoading,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email Field
                ProvexTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Email address",
                    placeholder = "alex@university.edu",
                    errorText = state.emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !state.isLoading,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                ProvexTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Password (minimum 8 characters)",
                    placeholder = "Create a password",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Password Field
                ProvexTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    label = "Confirm password",
                    placeholder = "Repeat your password",
                    errorText = state.confirmPasswordError,
                    visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !state.isLoading,
                    trailingIcon = {
                        Text(
                            text = if (state.isConfirmPasswordVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier
                                .clickable(onClick = viewModel::onToggleConfirmPasswordVisibility)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    },
                )

                if (state.generalError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    NoticeBanner(text = state.generalError!!, isWarning = true)
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProvexButton(
                    text = "Create Account",
                    onClick = viewModel::onCreateAccount,
                    isLoading = state.isLoading,
                    enabled = !state.isLoading,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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
            onClick = viewModel::onContinueDemoMode,
            enabled = !state.isLoading,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Explores all features locally. Pre-seeds your name in your local profile without requiring server accounts.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Already have an account row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .clickable(onClick = viewModel::onNavigateToLogin)
                    .padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
