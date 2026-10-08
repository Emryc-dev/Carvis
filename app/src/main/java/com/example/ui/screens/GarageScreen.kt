package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarageEntry
import com.example.data.model.GarageStats
import com.example.ui.components.CarVisionBottomBar
import com.example.ui.components.GarageVehicleCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.GarageUiState

@Composable
fun GarageScreen(
    entries: List<GarageEntry>, stats: GarageStats, state: GarageUiState,
    onRetry: () -> Unit, onScan: () -> Unit, onSelect: (GarageEntry) -> Unit, onNavigate: (String) -> Unit
) {
    Scaffold(bottomBar = { CarVisionBottomBar(Screen.Garage.route, onNavigate) }, containerColor = SurfaceDark) { padding ->
        when {
            state.isLoading && entries.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = CyberCyan)
                Text("Chargement de votre Garage…", color = TextMuted, modifier = Modifier.padding(top = 16.dp))
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 22.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        Text("CARVIS GARAGE", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Stat("COLLECTION", "${stats.carsCollected} voitures")
                                    Stat("NIVEAU", stats.level.toString())
                                    Stat("XP", stats.totalXp.toString())
                                }
                                LinearProgressIndicator(progress = { stats.progressToNextLevel }, modifier = Modifier.fillMaxWidth(), color = CyberCyan)
                            }
                        }
                    }
                }
                if (entries.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 70.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Votre Garage est vide", color = TextWhite, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            Text("Scannez une voiture et ajoutez-la à votre collection.", color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onScan, colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)) { Text("SCANNER") }
                                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLow, contentColor = TextWhite)) { Text("ACTUALISER") }
                            }
                        }
                    }
                } else items(entries, key = { it.id }) { entry -> GarageVehicleCard(entry) { onSelect(entry) } }
            }
        }
    }
}

@Composable private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(label, color = TextMuted, fontSize = 9.sp); Text(value, color = TextWhite, fontWeight = FontWeight.Bold) }
}
