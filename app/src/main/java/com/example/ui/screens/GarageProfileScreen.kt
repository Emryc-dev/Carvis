package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanTelemetry
import com.example.data.model.Vehicle
import com.example.ui.components.CarVisionBottomBar
import com.example.ui.components.CarVisionTopBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.CarVisionViewModel

enum class GarageTab { Garage, History }

@Composable
fun GarageProfileScreen(
    initialTab: GarageTab = GarageTab.Garage,
    viewModel: CarVisionViewModel,
    garageVehicles: List<Vehicle>,
    scanHistory: List<ScanTelemetry>,
    onSelectVehicle: (Vehicle) -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val currentRoute = if (selectedTab == GarageTab.History) Screen.History.route else Screen.Garage.route

    Scaffold(
        topBar = { CarVisionTopBar(title = if (selectedTab == GarageTab.History) "Historique" else "Mon garage") },
        bottomBar = { CarVisionBottomBar(currentRoute, onNavigate) },
        containerColor = SurfaceDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (profile.name.isBlank()) "Votre espace" else profile.name, color = TextWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    if (profile.username.isNotBlank()) Text(profile.username, color = TextMuted, fontSize = 14.sp)
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabButton("MON GARAGE", selectedTab == GarageTab.Garage, Modifier.weight(1f)) {
                        selectedTab = GarageTab.Garage
                    }
                    TabButton("HISTORIQUE", selectedTab == GarageTab.History, Modifier.weight(1f)) {
                        selectedTab = GarageTab.History
                    }
                }
            }

            if (selectedTab == GarageTab.Garage) {
                item { Text("Véhicules enregistrés", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                if (garageVehicles.isEmpty()) {
                    item { Text("Votre garage est vide.", color = TextMuted) }
                } else {
                    items(garageVehicles, key = { it.id }) { vehicle ->
                        VehicleLine(vehicle, onSelectVehicle)
                    }
                }
            } else {
                item { Text("Analyses enregistrées", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                if (scanHistory.isEmpty()) {
                    item { Text("Aucune analyse enregistrée.", color = TextMuted) }
                } else {
                    items(scanHistory, key = { it.scanId }) { scan -> ScanLine(scan) }
                }
            }

            item {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp)) {
                    Text("Se déconnecter")
                }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
        ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp)) {
            Text(label, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VehicleLine(vehicle: Vehicle, onSelectVehicle: (Vehicle) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelectVehicle(vehicle) }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(vehicle.brand + " " + vehicle.model, color = TextWhite, fontWeight = FontWeight.Medium)
            val details = listOfNotNull(vehicle.year.takeIf { it > 0 }?.toString(), vehicle.generation.takeIf { it.isNotBlank() }).joinToString(" · ")
            if (details.isNotBlank()) Text(details, color = TextMuted, fontSize = 13.sp)
        }
        Text("Voir", color = CyberCyan)
    }
    HorizontalDivider()
}

@Composable
private fun ScanLine(scan: ScanTelemetry) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(scan.vehicleName.ifBlank { "Véhicule non identifié" }, color = TextWhite, fontWeight = FontWeight.Medium)
        if (scan.status.isNotBlank()) Text(scan.status, color = TextMuted, fontSize = 13.sp)
    }
    HorizontalDivider()
}
