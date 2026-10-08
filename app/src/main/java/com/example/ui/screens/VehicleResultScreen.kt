package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Vehicle
import com.example.ui.components.CarVisionTopBar
import com.example.ui.components.VehicleDiscoveryCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.GarageUiState

@Composable
fun VehicleResultScreen(
    vehicle: Vehicle?,
    garageState: GarageUiState,
    onBackClick: () -> Unit,
    onCompareClick: (Vehicle) -> Unit,
    onAddToGarage: (Vehicle) -> Unit,
    onNotNow: () -> Unit
) {
    Scaffold(
        topBar = { CarVisionTopBar(title = "Véhicule identifié", showBackButton = true, onBackClick = onBackClick) },
        containerColor = SurfaceDark
    ) { padding ->
        if (vehicle == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Aucun résultat disponible", color = TextWhite)
                Text("L’analyse n’a retourné aucun véhicule du catalogue.", color = TextMuted)
                OutlinedButton(onClick = onBackClick) { Text("Retour") }
            }
        } else {
            val visibleVehicle = if (garageState.awardedXp != null || garageState.alreadyCollected) vehicle.copy(isSavedInGarage = true) else vehicle
            val feedback = when {
                garageState.awardedXp != null -> "+${garageState.awardedXp} XP ajoutés à votre collection"
                garageState.alreadyCollected || visibleVehicle.isSavedInGarage -> "Déjà dans votre Garage"
                garageState.error != null -> garageState.error
                else -> null
            }
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VehicleDiscoveryCard(
                    vehicle = visibleVehicle,
                    isAdding = garageState.isLoading,
                    feedback = feedback,
                    onAdd = { onAddToGarage(vehicle) },
                    onNotNow = onNotNow
                )
                OutlinedButton(onClick = { onCompareClick(vehicle) }, modifier = Modifier.fillMaxWidth()) { Text("Comparer ce véhicule") }
            }
        }
    }
}
