package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Vehicle
import com.example.ui.components.CarVisionTopBar
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.CarVisionViewModel

@Composable
fun ScannerScreen(
    viewModel: CarVisionViewModel,
    onBackClick: () -> Unit,
    onScanFinished: (Vehicle) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.scanWorkflow.collectAsState()
    val analyze: (android.graphics.Bitmap) -> Unit = { bitmap ->
        viewModel.analyzePhoto(bitmap, onScanFinished)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { it?.let(analyze) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let(analyze)
        } }
    }

    Scaffold(
        topBar = { CarVisionTopBar(title = "Analyser un véhicule", showBackButton = true, onBackClick = onBackClick) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ajoutez une photo nette du véhicule.", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Cadrez le véhicule en entier et utilisez une image bien éclairée.", color = TextMuted, fontSize = 16.sp, lineHeight = 23.sp)

            if (state.isScanning) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 40.dp))
                Text("Analyse en cours…", color = TextMuted)
            } else {
                Button(onClick = { camera.launch(null) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Text("  Prendre une photo")
                }
                OutlinedButton(
                    onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Text("  Importer une photo")
                }
            }

            state.error?.let {
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { camera.launch(null) }) { Text("Réessayer") }
            }
        }
    }
}


