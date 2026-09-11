package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun SystemStatusLine(
    modifier: Modifier = Modifier,
    statusText: String = "",
    coreActiveText: String = ""
) {
    if (statusText.isNotBlank()) {
        Text(statusText, color = TextMuted, fontSize = 12.sp, modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp))
    }
}

@Composable
fun CarVisionTopBar(
    title: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    avatarUrl: String = "",
    onAdminBadgeClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(SurfaceContainerLowest).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = TextWhite)
                }
            }
            Text(title ?: "CarVision", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (title == null) Text(" AI", color = CyberCyan, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        if (!showBackButton) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.Transparent, CircleShape).clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, "Profil", tint = TextMuted, modifier = Modifier.size(28.dp))
            }
        }
    }
}

