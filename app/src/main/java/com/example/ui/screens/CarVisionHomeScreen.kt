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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
fun CarVisionHomeScreen(
    userName: String, avatarUrl: String, latestVehicle: Vehicle?, scanState: ScanWorkflowState,
    onNavigate: (String) -> Unit, onSelectVehicle: (Vehicle) -> Unit, onImportPhoto: (Uri) -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onImportPhoto) }
    Scaffold(
        topBar = { CarVisionTopBar(avatarUrl = avatarUrl, onProfileClick = { onNavigate(Screen.Garage.route) }) },
        bottomBar = { CarVisionBottomBar(Screen.Dashboard.route, onNavigate) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (userName.isBlank()) "Bienvenue" else "Bienvenue, ${userName.substringBefore(' ')}", color = TextWhite, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("Quelle voiture allons-nous identifier ?", color = TextMuted, fontSize = 14.sp)
                }
                Surface(modifier = Modifier.clickable { onNavigate(Screen.Compare.route) }, color = SurfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CompareArrows, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Text("Comparer", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onNavigate(Screen.Scanner.route) }, enabled = !scanState.isScanning,
                modifier = Modifier.fillMaxWidth().height(70.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
            ) {
                Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(25.dp))
                Text("  SCANNER UN VÉHICULE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = !scanState.isScanning) { picker.launch("image/*") }, color = SurfaceContainerLow, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (scanState.isScanning) CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.PhotoLibrary, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    Text(if (scanState.isScanning) "  Analyse en cours…" else "  Importer depuis la galerie", color = TextWhite, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Dernière identification", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Historique", color = CyberCyan, fontSize = 12.sp, modifier = Modifier.clickable { onNavigate(Screen.History.route) })
            }
            if (latestVehicle == null) {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Aucune identification récente", color = TextWhite, fontWeight = FontWeight.Medium)
                    Text("Votre prochain résultat apparaîtra ici.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth().clickable { onSelectVehicle(latestVehicle); onNavigate(Screen.VehicleResult.route) }, color = SurfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AsyncImage(model = latestVehicle.imageUrl, contentDescription = "${latestVehicle.brand} ${latestVehicle.model}", modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)).background(CyanDark), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(latestVehicle.brand, color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(latestVehicle.model, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(latestVehicle.year.takeIf { it > 0 }?.toString(), latestVehicle.generation.takeIf { it.isNotBlank() }).joinToString(" · "), color = TextMuted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ArrowForward, "Voir", tint = TextMuted)
                    }
                }
            }
        }
    }
}
