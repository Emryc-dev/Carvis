package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun VehicleCompareScreen(
    vehicleLeft: Vehicle?, vehicleRight: Vehicle?, allVehicles: List<Vehicle>,
    onBackClick: () -> Unit, onSwapVehicles: () -> Unit,
    onSelectLeftVehicle: (Vehicle) -> Unit, onSelectRightVehicle: (Vehicle) -> Unit,
    onNavigate: (String) -> Unit
) {
    val left = vehicleLeft
    val right = vehicleRight
    Scaffold(
        topBar = { CarVisionTopBar(title = "Comparer", showBackButton = true, onBackClick = onBackClick) },
        bottomBar = { CarVisionBottomBar(Screen.Compare.route, onNavigate) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Comparaison", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (left == null || right == null) {
                Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Sélectionnez deux véhicules à comparer.", color = TextMuted, lineHeight = 22.sp)
                        if (allVehicles.size >= 2) {
                            Button(
                                onClick = { onSelectLeftVehicle(allVehicles[0]); onSelectRightVehicle(allVehicles[1]) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
                            ) { Text("Sélectionner des véhicules") }
                        } else Text("Pas assez de véhicules disponibles.", color = TextMuted)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VehiclePanel(left, Modifier.weight(1f))
                    VehiclePanel(right, Modifier.weight(1f))
                }
                Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ComparisonRow("Année", value(left.year), value(right.year))
                        ComparisonRow("Moteur", left.engine, right.engine)
                        ComparisonRow("Puissance", power(left.horsepower), power(right.horsepower))
                        ComparisonRow("Transmission", left.transmission, right.transmission)
                        ComparisonRow("Carburant", left.fuel, right.fuel)
                    }
                }
                OutlinedButton(onClick = onSwapVehicles, modifier = Modifier.fillMaxWidth()) { Text("Inverser les véhicules") }
            }
        }
    }
}

@Composable
private fun VehiclePanel(vehicle: Vehicle, modifier: Modifier) {
    Surface(modifier = modifier, color = SurfaceContainer, shape = RoundedCornerShape(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (vehicle.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = vehicle.imageUrl, contentDescription = vehicle.brand + " " + vehicle.model,
                    modifier = Modifier.fillMaxWidth().height(105.dp), contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(vehicle.brand, color = TextMuted, fontSize = 12.sp)
                Text(vehicle.model, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (vehicle.year > 0) Text(vehicle.year.toString(), color = CyberCyan, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ComparisonRow(label: String, left: String, right: String) {
    if (left.isNotBlank() || right.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(left.ifBlank { "Non disponible" }, color = TextWhite, modifier = Modifier.weight(1f))
                Text(right.ifBlank { "Non disponible" }, color = TextWhite, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun value(year: Int) = year.takeIf { it > 0 }?.toString().orEmpty()
private fun power(horsepower: Int) = horsepower.takeIf { it > 0 }?.let { "$it ch" }.orEmpty()
