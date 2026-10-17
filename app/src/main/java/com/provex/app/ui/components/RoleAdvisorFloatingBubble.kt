package com.provex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.SurfaceWhite

/**
 * Premium floating "Role Advisor" bubble.
 * Provides a compact, non-intrusive floating pill above the bottom navigation
 * allowing candidates to consult the AI career role advisor at any time.
 */
@Composable
fun RoleAdvisorFloatingBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Open Role Advisor",
                onClick = onClick,
            )
            .semantics {
                contentDescription = "Role Advisor: Find roles by your code"
                role = Role.Button
            },
        shape = RoundedCornerShape(28.dp),
        color = NavyPrimary,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // Sparkle icon container
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = NavyDark,
                border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.25f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "✦",
                        color = MintGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Subtitle
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Role Advisor",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                        ),
                        color = SurfaceWhite,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Subtle mint status indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MintGreen, CircleShape)
                    )
                }

                if (isExpanded) {
                    Text(
                        text = "Find roles by your code",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = MintGreen.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}
