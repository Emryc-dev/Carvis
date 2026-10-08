package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GarageEntry
import com.example.ui.components.CarVisionTopBar
import com.example.ui.components.RarityLabel
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun GarageDetailScreen(entry: GarageEntry?, onBack: () -> Unit, onRemove: (GarageEntry) -> Unit) {
    Scaffold(topBar = { CarVisionTopBar(title = "Collection", showBackButton = true, onBackClick = onBack) }, containerColor = SurfaceDark) { padding ->
        if (entry == null) Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text("Véhicule introuvable", color = TextWhite) }
        else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            AsyncImage(model = entry.capturedImageUrl.ifBlank { entry.vehicle.imageUrl }, contentDescription = "${entry.vehicle.brand} ${entry.vehicle.model}", modifier = Modifier.fillMaxWidth().height(280.dp), contentScale = ContentScale.Crop)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RarityLabel(entry.rarity)
                Text("${entry.vehicle.brand} ${entry.vehicle.model}", color = TextWhite, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                val details = listOfNotNull(entry.vehicle.year.takeIf { it > 0 }?.toString(), entry.vehicle.generation.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (details.isNotBlank()) Text(details, color = TextMuted)
                Text("+${entry.xpEarned} XP gagnés", color = TextWhite, fontWeight = FontWeight.SemiBold)
                Text("Ajouté le ${entry.capturedAt.take(10)}", color = TextMuted)
                OutlinedButton(onClick = { onRemove(entry) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Retirer du Garage") }
            }
        }
    }
}
