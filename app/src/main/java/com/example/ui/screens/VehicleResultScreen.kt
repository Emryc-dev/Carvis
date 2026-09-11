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
import com.example.ui.components.CarVisionTopBar
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun VehicleResultScreen(
    vehicle: Vehicle?, onBackClick: () -> Unit,
    onCompareClick: (Vehicle) -> Unit, onToggleGarage: (Vehicle) -> Unit
) {
    Scaffold(
        topBar = { CarVisionTopBar(title = "Résultat", showBackButton = true, onBackClick = onBackClick) },
        bottomBar = {
            vehicle?.let {
                Surface(color = SurfaceContainerLowest, shadowElevation = 12.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = { onCompareClick(it) }, modifier = Modifier.weight(1f).height(50.dp)) {
                            Text("Comparer")
                        }
                        Button(
                            onClick = { onToggleGarage(it) }, modifier = Modifier.weight(1.35f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
                        ) { Text(if (it.isSavedInGarage) "Retirer du garage" else "Ajouter au garage", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        },
        containerColor = SurfaceDark
    ) { padding ->
        if (vehicle == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Aucun résultat disponible", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("L’analyse n’a retourné aucun véhicule.", color = TextMuted)
                OutlinedButton(onClick = onBackClick) { Text("Retour") }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (vehicle.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = vehicle.imageUrl,
                        contentDescription = vehicle.brand + " " + vehicle.model,
                        modifier = Modifier.fillMaxWidth().height(225.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(vehicle.brand + " " + vehicle.model, color = TextWhite, fontSize = 29.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                        val subtitle = listOfNotNull(
                            vehicle.year.takeIf { it > 0 }?.toString(),
                            vehicle.generation.takeIf { it.isNotBlank() },
                            vehicle.category.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (subtitle.isNotBlank()) Text(subtitle, color = CyberCyan, fontSize = 14.sp)
                    }
                    if (vehicle.description.isNotBlank()) Text(vehicle.description, color = TextMuted, lineHeight = 22.sp)
                    Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            Text("Caractéristiques", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Detail("Moteur", vehicle.engine)
                            Detail("Puissance", vehicle.horsepower.takeIf { it > 0 }?.let { "$it ch" }.orEmpty())
                            Detail("Transmission", vehicle.transmission)
                            Detail("Carburant", vehicle.fuel)
                            Detail("Roues motrices", vehicle.drivetrain)
                            Detail("0 à 100 km/h", vehicle.zeroToSixty.takeIf { it > 0 }?.let { "$it s" }.orEmpty())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextMuted, modifier = Modifier.weight(1f))
            Text(value, color = TextWhite, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        }
    }
}
