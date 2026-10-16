package com.provex.app.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { target ->
            when (target) {
                SplashNavigationTarget.Home -> onNavigateToHome()
                SplashNavigationTarget.Login -> onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            // Distinct developer-tool monogram
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(18.dp),
                color = NavyDark,
                shadowElevation = 8.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "</>",
                        color = MintGreen,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prominent PROVEX wordmark
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PROVEX",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontSize = 32.sp,
                    ),
                    color = SurfaceWhite,
                )
                Text(
                    text = ".",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    color = MintGreen,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Turn Code Into Credibility.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                ),
                color = MintGreen,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Subtle, lightweight progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MintGreen,
                strokeWidth = 2.dp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verifying session...",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}
