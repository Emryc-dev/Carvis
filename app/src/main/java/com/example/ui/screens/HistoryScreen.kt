package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ScanTelemetry
import com.example.ui.components.CarVisionBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun HistoryScreen(scans: List<ScanTelemetry>, onNavigate: (String) -> Unit) {
    Scaffold(bottomBar = { CarVisionBottomBar(Screen.History.route, onNavigate) }, containerColor = SurfaceDark) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.padding(top = 26.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Historique", color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Toutes les identifications effectuées, qu’elles aient rejoint votre Garage ou non.", color = TextMuted, lineHeight = 21.sp)
                }
            }
            if (scans.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 90.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = CircleShape, color = SurfaceContainerLow, modifier = Modifier.size(68.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, null, tint = CyberCyan) } }
                        Text("Aucune identification", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("Vos véhicules analysés apparaîtront ici.", color = TextMuted)
                    }
                }
            } else items(scans, key = { it.scanId }) { scan ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = scan.imageUrl, contentDescription = scan.vehicleName, modifier = Modifier.size(88.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceContainerLow), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(scan.vehicleName.ifBlank { "Véhicule non identifié" }, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (scan.status == "completed") "Identification terminée" else scan.status, color = TextMuted, fontSize = 12.sp)
                    }
                    Box(Modifier.size(8.dp).background(if (scan.status == "completed") CyberCyan else TextMuted, CircleShape))
                }
            }
        }
    }
}
