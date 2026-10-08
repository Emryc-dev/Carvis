package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Vehicle
import com.example.ui.components.CarVisionBottomBar
import com.example.ui.components.CarVisionTopBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.ScanWorkflowState

@Composable
fun HomeScreen(
    userName: String,
    avatarUrl: String,
    latestVehicle: Vehicle?,
    scanState: ScanWorkflowState,
    onNavigate: (String) -> Unit,
    onSelectVehicle: (Vehicle) -> Unit,
    onImportPhoto: (Uri) -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onImportPhoto) }
    Scaffold(
        topBar = { CarVisionTopBar(avatarUrl = avatarUrl, onProfileClick = { onNavigate(Screen.Garage.route) }) },
        bottomBar = { CarVisionBottomBar(Screen.Dashboard.route, onNavigate) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (userName.isBlank()) "Bonjour" else "Bonjour, ${userName.substringBefore(' ')}", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Reconnaissez la voiture.\nConstruisez votre collection.", color = TextWhite, fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(210.dp).clickable(enabled = !scanState.isScanning) { onNavigate(Screen.Scanner.route) },
                shape = RoundedCornerShape(28.dp), color = CyanDark
            ) {
                Box(Modifier.background(Brush.linearGradient(listOf(CyberCyan.copy(alpha = .22f), Color.Transparent)))) {
                    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Surface(shape = CircleShape, color = CyberCyan, contentColor = CyanDark, modifier = Modifier.size(58.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, contentDescription = null) }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Scanner un véhicule", color = TextWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("Prenez une photo pour obtenir son identité et sa rareté.", color = TextWhite.copy(alpha = .72f), lineHeight = 20.sp)
                        }
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CyberCyan, modifier = Modifier.align(Alignment.TopEnd).padding(24.dp))
                }
            }

            Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = !scanState.isScanning) { picker.launch("image/*") }, color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    if (scanState.isScanning) CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CyberCyan)
                    Column {
                        Text(if (scanState.isScanning) "Analyse en cours…" else "Importer une photo", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text("Depuis votre galerie", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            scanState.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 13.sp) }

            Text("Dernière découverte", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (latestVehicle == null) {
                Text("Votre prochaine découverte apparaîtra ici.", color = TextMuted)
            } else {
                Row(
                    Modifier.fillMaxWidth().clickable { onSelectVehicle(latestVehicle); onNavigate(Screen.VehicleResult.route) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AsyncImage(model = latestVehicle.imageUrl, contentDescription = "${latestVehicle.brand} ${latestVehicle.model}", modifier = Modifier.size(82.dp).clip(RoundedCornerShape(17.dp)).background(SurfaceContainerLow), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${latestVehicle.brand} ${latestVehicle.model}", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(latestVehicle.year.takeIf { it > 0 }?.toString(), latestVehicle.generation.takeIf { it.isNotBlank() }).joinToString(" · "), color = TextMuted, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = "Voir", tint = CyberCyan)
                }
            }
        }
    }
}
