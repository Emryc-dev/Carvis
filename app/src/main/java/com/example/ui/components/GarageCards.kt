package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GarageEntry
import com.example.data.model.Vehicle
import com.example.data.model.VehicleRarity
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

fun rarityColor(rarity: VehicleRarity): Color = when (rarity) {
    VehicleRarity.COMMON -> Color(0xFF9AA4AE)
    VehicleRarity.UNCOMMON -> Color(0xFF58B879)
    VehicleRarity.RARE -> Color(0xFF5594E8)
    VehicleRarity.EPIC -> Color(0xFFA678D8)
    VehicleRarity.MYTHIC -> Color(0xFFE06078)
}

@Composable
fun RarityLabel(rarity: VehicleRarity) {
    val color = rarityColor(rarity)
    Text(
        rarity.name,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color.copy(alpha = .13f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
fun GarageVehicleCard(entry: GarageEntry, onClick: () -> Unit) {
    val accent = rarityColor(entry.rarity)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).animateContentSize(),
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Column {
            Box {
                AsyncImage(
                    model = entry.capturedImageUrl.ifBlank { entry.vehicle.imageUrl },
                    contentDescription = "${entry.vehicle.brand} ${entry.vehicle.model}",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.22f).background(accent.copy(alpha = .08f)),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.align(Alignment.TopEnd).padding(9.dp)) { RarityLabel(entry.rarity) }
            }
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(entry.vehicle.brand, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(entry.vehicle.model, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("+${entry.xpEarned} XP", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(entry.capturedAt.take(10), color = TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun VehicleDiscoveryCard(
    vehicle: Vehicle,
    isAdding: Boolean,
    feedback: String?,
    onAdd: () -> Unit,
    onNotNow: () -> Unit,
    imageModel: Any? = vehicle.imageUrl,
    capturedAt: String? = null,
    secondaryLabel: String = "PAS MAINTENANT",
) {
    val accent = rarityColor(vehicle.rarity)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
            Column {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "${vehicle.brand} ${vehicle.model}",
                    modifier = Modifier.fillMaxWidth().height(230.dp).background(accent.copy(alpha = .08f)),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        RarityLabel(vehicle.rarity)
                        Text("+${vehicle.collectionXp} XP", color = accent, fontWeight = FontWeight.Bold)
                    }
                    Text(vehicle.brand.uppercase(), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(vehicle.model, color = TextWhite, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    val subtitle = listOfNotNull(vehicle.year.takeIf { it > 0 }?.toString(), vehicle.generation.takeIf { it.isNotBlank() }).joinToString(" / ")
                    if (subtitle.isNotBlank()) Text(subtitle, color = TextMuted)
                    capturedAt?.takeIf { it.isNotBlank() }?.let { Text("CAPTURED ${it.take(10)}", color = TextMuted, fontSize = 11.sp) }
                }
            }
        }
        if (feedback != null) Text(
            feedback,
            color = if (feedback.contains("ADDED TO GARAGE")) CyberCyan else TextMuted,
            fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = onAdd,
            enabled = !isAdding && !vehicle.isSavedInGarage,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
        ) { Text(if (vehicle.isSavedInGarage) "DÉJÀ DANS LE GARAGE" else if (isAdding) "AJOUT..." else "AJOUTER AU GARAGE", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text(secondaryLabel) }
    }
}
