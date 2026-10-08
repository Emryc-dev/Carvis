package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.createCameraUri
import com.example.data.model.Vehicle
import com.example.ui.components.CarVisionTopBar
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.CarVisionViewModel

@Composable
fun CaptureScreen(viewModel: CarVisionViewModel, onBackClick: () -> Unit, onScanFinished: (Vehicle) -> Unit) {
    val context = LocalContext.current
    val state by viewModel.scanWorkflow.collectAsState()
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var sourceError by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.analyzePhotoUri(captureUri, onScanFinished)
        else sourceError = "La photo n’a pas été enregistrée. Réessayez."
    }
    fun launchCamera() {
        sourceError = null
        runCatching { createCameraUri(context) }
            .onSuccess { uri -> captureUri = uri; camera.launch(uri) }
            .onFailure { sourceError = "Impossible d’ouvrir la caméra sur cet appareil." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else sourceError = "Autorisez l’accès à la caméra dans les réglages pour prendre une photo."
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { sourceError = null; viewModel.analyzePhotoUri(uri, onScanFinished) }
    }
    val openCamera = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    Scaffold(topBar = { CarVisionTopBar(title = "Capture", showBackButton = true, onBackClick = onBackClick) }, containerColor = SurfaceDark) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.radialGradient(listOf(CyberCyan.copy(alpha = .10f), Color.Transparent), radius = 720f)
            )
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Quel véhicule avez-vous trouvé ?", color = TextWhite, fontSize = 29.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Une photo nette suffit. Aucun véhicule n’est ajouté au Garage sans votre accord.", color = TextMuted, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.weight(1f))
                if (state.isScanning) {
                    CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(56.dp))
                    Text("Identification en cours…", color = TextWhite, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 18.dp))
                    Text("Envoi sécurisé de la photo", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                } else {
                    Surface(modifier = Modifier.size(118.dp).clickable(onClick = openCamera), shape = CircleShape, color = CyberCyan, contentColor = CyanDark, shadowElevation = 18.dp) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, contentDescription = "Prendre une photo", modifier = Modifier.size(43.dp)) }
                    }
                    Text("Appuyez pour prendre une photo", color = TextWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                    Surface(modifier = Modifier.fillMaxWidth().padding(top = 28.dp).clickable { gallery.launch("image/*") }, shape = RoundedCornerShape(16.dp), color = SurfaceContainerLow) {
                        Box(Modifier.height(56.dp), contentAlignment = Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CyberCyan)
                                Text("Choisir dans la galerie", color = TextWhite, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp)) }
                sourceError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp)) }
                Spacer(Modifier.weight(1f))
                Text("JPEG, PNG ou WebP · 10 Mo maximum", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}
