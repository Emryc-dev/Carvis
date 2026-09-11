package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Vehicle
import com.example.ui.components.CarVisionBottomBar
import com.example.ui.components.CarVisionTopBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun DashboardScreen(
    userName: String,
    avatarUrl: String,
    vehicles: List<Vehicle>,
    latestVehicle: Vehicle?,
    onNavigate: (String) -> Unit,
    onSelectVehicle: (Vehicle) -> Unit,
    onScanPhotoPicked: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let(onScanPhotoPicked)
            }
        }
    }

    Scaffold(
        topBar = { CarVisionTopBar(avatarUrl = avatarUrl, onProfileClick = { onNavigate(Screen.Garage.route) }) },
        bottomBar = { CarVisionBottomBar(Screen.Dashboard.route, onNavigate) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (userName.isBlank()) "Bienvenue" else "Bienvenue, $userName",
                    color = TextWhite,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Prenez ou importez une photo pour identifier un véhicule.",
                    color = TextMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onNavigate(Screen.Scanner.route) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
            ) {
                Icon(Icons.Default.PhotoCamera, null)
                Text("  Prendre une photo", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Text("  Importer une photo")
            }

            Spacer(Modifier.height(8.dp))
            Text("Dernière analyse", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            if (latestVehicle == null) {
                Text("Aucune analyse récente", color = TextMuted, fontSize = 15.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(latestVehicle.brand + " " + latestVehicle.model, color = TextWhite, fontWeight = FontWeight.Medium)
                        val details = listOfNotNull(
                            latestVehicle.year.takeIf { it > 0 }?.toString(),
                            latestVehicle.generation.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (details.isNotBlank()) Text(details, color = TextMuted)
                    }
                    OutlinedButton(onClick = {
                        onSelectVehicle(latestVehicle)
                        onNavigate(Screen.VehicleResult.route)
                    }) { Text("Voir") }
                }
            }
        }
    }
}

