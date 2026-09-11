package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TextMuted

@Composable
fun CarVisionBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomCenter),
            color = SurfaceContainerLowest.copy(alpha = 0.97f),
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomDestination("Accueil", Icons.Default.Home, currentRoute == Screen.Dashboard.route) {
                    onNavigate(Screen.Dashboard.route)
                }
                BottomDestination("Historique", Icons.Default.ManageSearch, currentRoute == Screen.History.route) {
                    onNavigate(Screen.History.route)
                }
                Spacer(Modifier.size(58.dp))
                BottomDestination("Comparer", Icons.Default.CompareArrows, currentRoute == Screen.Compare.route) {
                    onNavigate(Screen.Compare.route)
                }
                BottomDestination("Garage", Icons.Default.DirectionsCar, currentRoute == Screen.Garage.route) {
                    onNavigate(Screen.Garage.route)
                }
            }
        }

        Surface(
            modifier = Modifier.size(58.dp).align(Alignment.TopCenter).offset(y = (-4).dp)
                .shadow(12.dp, CircleShape).clickable { onNavigate(Screen.Scanner.route) },
            color = CyberCyan,
            contentColor = CyanDark,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Analyser un véhicule", modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun BottomDestination(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) CyberCyan else TextMuted
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(21.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
