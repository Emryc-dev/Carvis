package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TelemetryBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TitaniumBorder

@Composable
fun HUDVisualReticle(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    lidarDepth: String = "",
    chassisCode: String = "G82 VERIFIED",
    colorIdentified: String = "Isle of Man Green",
    showWireframe: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, TitaniumBorder, RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val bracketLen = 28f
            val strokeWidth = 3f

            // 4 Corner Brackets in Cyber Cyan
            // Top Left
            drawLine(CyberCyan, Offset(24f, 24f), Offset(24f + bracketLen, 24f), strokeWidth)
            drawLine(CyberCyan, Offset(24f, 24f), Offset(24f, 24f + bracketLen), strokeWidth)

            // Top Right
            drawLine(CyberCyan, Offset(width - 24f, 24f), Offset(width - 24f - bracketLen, 24f), strokeWidth)
            drawLine(CyberCyan, Offset(width - 24f, 24f), Offset(width - 24f, 24f + bracketLen), strokeWidth)

            // Bottom Left
            drawLine(CyberCyan, Offset(24f, height - 24f), Offset(24f + bracketLen, height - 24f), strokeWidth)
            drawLine(CyberCyan, Offset(24f, height - 24f), Offset(24f, height - 24f - bracketLen), strokeWidth)

            // Bottom Right
            drawLine(CyberCyan, Offset(width - 24f, height - 24f), Offset(width - 24f - bracketLen, height - 24f), strokeWidth)
            drawLine(CyberCyan, Offset(width - 24f, height - 24f), Offset(width - 24f, height - 24f - bracketLen), strokeWidth)

            // Center crosshair
            val cx = width / 2
            val cy = height / 2
            drawLine(Color(0x5500F0FF), Offset(cx - 20, cy), Offset(cx + 20, cy), 1f)
            drawLine(Color(0x5500F0FF), Offset(cx, cy - 20), Offset(cx, cy + 20), 1f)

            if (showWireframe) {
                // Draw vehicle silhouette vector wireframe
                val path = Path().apply {
                    moveTo(width * 0.2f, height * 0.65f)
                    lineTo(width * 0.25f, height * 0.52f)
                    lineTo(width * 0.35f, height * 0.50f)
                    lineTo(width * 0.45f, height * 0.36f)
                    lineTo(width * 0.68f, height * 0.36f)
                    lineTo(width * 0.78f, height * 0.50f)
                    lineTo(width * 0.85f, height * 0.53f)
                    lineTo(width * 0.86f, height * 0.65f)
                }
                drawPath(path, TelemetryBlue, style = Stroke(width = 3f))

                // Wheels
                drawCircle(CyberCyan, radius = 18f, center = Offset(width * 0.32f, height * 0.68f), style = Stroke(3f))
                drawCircle(CyberCyan, radius = 6f, center = Offset(width * 0.32f, height * 0.68f))

                drawCircle(CyberCyan, radius = 18f, center = Offset(width * 0.75f, height * 0.68f), style = Stroke(3f))
                drawCircle(CyberCyan, radius = 6f, center = Offset(width * 0.75f, height * 0.68f))
            }

            if (isScanning) {
                // Moving Laser Scan Line
                val currentY = height * scanY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            CyberCyan.copy(alpha = 0.9f),
                            Color.White,
                            CyberCyan.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(24f, currentY),
                    end = Offset(width - 24f, currentY),
                    strokeWidth = 3f
                )
            }
        }

        // Top Left Telemetry Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, TitaniumBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(CyberCyan, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LiDAR Depth: $lidarDepth",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .background(SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "FOV: 84.6° // 4K RAW",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Mid Reticle Lock Indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp, top = 20.dp)
                .background(SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "[MATRIX LASER: MATCH]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        }

        // Bottom Right Verified Tag
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .background(SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "[AERO SPLITTER: CFR]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TelemetryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .background(SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, TitaniumBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "[CHASSIS: $chassisCode]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
