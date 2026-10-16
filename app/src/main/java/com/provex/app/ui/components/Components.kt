package com.provex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.Importance
import com.provex.app.ui.theme.Amber
import com.provex.app.ui.theme.AmberBg
import com.provex.app.ui.theme.AmberDark
import com.provex.app.ui.theme.BorderLight
import com.provex.app.ui.theme.BorderMedium
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.DarkGreenBg
import com.provex.app.ui.theme.ErrorRed
import com.provex.app.ui.theme.ErrorRedBg
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextMuted
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

@Composable
fun ProvexButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NavyPrimary,
            contentColor = SurfaceWhite,
            disabledContainerColor = BorderMedium,
            disabledContentColor = TextMuted,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = SurfaceWhite,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = SurfaceWhite),
            )
        }
    }
}

@Composable
fun ProvexSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderMedium),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = NavyPrimary,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = NavyPrimary),
        )
    }
}

@Composable
fun ProvexCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceWhite,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 1.dp,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ProvexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    maxLines: Int = 1,
    errorText: String? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(text = placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
                }
            },
            minLines = minLines,
            maxLines = maxLines,
            isError = errorText != null,
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = NavyPrimary,
                unfocusedBorderColor = BorderMedium,
                errorBorderColor = ErrorRed,
            ),
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

@Composable
fun EvidenceStatusBadge(status: EvidenceStatus, modifier: Modifier = Modifier) {
    val (bg, fg, border) = when (status) {
        EvidenceStatus.STRONG -> Triple(DarkGreenBg, DarkGreen, DarkGreen)
        EvidenceStatus.PARTIAL -> Triple(AmberBg, AmberDark, AmberDark)
        EvidenceStatus.NOT_DEMONSTRATED -> Triple(SurfaceAlt, TextSecondary, BorderMedium)
        EvidenceStatus.NEEDS_VERIFICATION -> Triple(SoftBlue, NavyPrimary, BorderMedium)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
fun ImportanceBadge(importance: Importance, modifier: Modifier = Modifier) {
    val (bg, fg) = when (importance) {
        Importance.REQUIRED -> Pair(NavyPrimary, SurfaceWhite)
        Importance.PREFERRED -> Pair(SoftBlue, NavyPrimary)
        Importance.MENTIONED -> Pair(SurfaceAlt, TextSecondary)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bg,
    ) {
        Text(
            text = importance.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = fg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun SampleBadge(modifier: Modifier = Modifier, text: String = "Sample Demo Data") {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = AmberBg,
        border = BorderStroke(1.dp, AmberDark.copy(alpha = 0.4f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = AmberDark,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun NoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isWarning) AmberBg else SoftBlue,
        border = BorderStroke(1.dp, if (isWarning) AmberDark.copy(alpha = 0.3f) else BorderLight),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = if (isWarning) "ℹ" else "✦",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isWarning) AmberDark else NavyPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isWarning) AmberDark else NavyPrimary,
                    lineHeight = 16.sp,
                ),
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = SoftBlue,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "◆",
                    style = MaterialTheme.typography.headlineMedium.copy(color = NavyPrimary),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            ProvexSecondaryButton(
                text = actionButtonText,
                onClick = onActionClick,
                modifier = Modifier.width(200.dp),
            )
        }
    }
}
