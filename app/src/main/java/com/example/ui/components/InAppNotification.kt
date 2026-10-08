package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

data class InAppNotification(
    val id: Long = System.nanoTime(),
    val message: String,
)

@Composable
fun InAppNotificationHost(
    notification: InAppNotification?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(4_000)
            onDismiss()
        }
    }

    val smoothEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    AnimatedVisibility(
        visible = notification != null,
        modifier = modifier
            .zIndex(20f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        enter = slideInVertically(
            initialOffsetY = { -it - 24 },
            animationSpec = tween(250, easing = smoothEaseOut),
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 24 },
            animationSpec = tween(350, easing = smoothEaseOut),
        ) + fadeOut(tween(180)),
    ) {
        notification?.let { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Assertive }
                    .clickable(onClick = onDismiss),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainer,
                border = BorderStroke(1.dp, ErrorAccent.copy(alpha = .52f)),
                shadowElevation = 12.dp,
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 13.dp, bottom = 13.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorAccent,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = item.message,
                        color = TextWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer la notification", tint = TextWhite)
                    }
                }
            }
        }
    }
}

private val ErrorAccent = Color(0xFFFF7B86)
