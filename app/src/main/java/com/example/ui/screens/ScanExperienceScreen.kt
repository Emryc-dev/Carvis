package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.createCameraUri
import com.example.ui.components.CarVisionTopBar
import com.example.ui.components.VehicleDiscoveryCard
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.CarVisionViewModel
import com.example.ui.viewmodel.ScanErrorType
import com.example.ui.viewmodel.ScanInputSource
import com.example.ui.viewmodel.ScanStage
import com.example.ui.viewmodel.ScanWorkflowState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanExperienceScreen(
    viewModel: CarVisionViewModel,
    onBack: () -> Unit,
    onViewGarage: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.scanWorkflow.collectAsState()
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.analyzePhotoUri(captureUri, ScanInputSource.CAMERA)
        else onError("La photo n’a pas été enregistrée. Réessayez.")
    }
    fun launchCamera() {
        runCatching { createCameraUri(context) }
            .onSuccess { captureUri = it; camera.launch(it) }
            .onFailure { onError("Impossible d’ouvrir l’appareil photo.") }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) launchCamera() else onError("Autorisez l’accès à la caméra dans les paramètres Android.")
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) viewModel.analyzePhotoUri(it, ScanInputSource.GALLERY)
    }
    val openCamera: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }
    BackHandler(state.stage != ScanStage.IDLE) { viewModel.resetScan() }
    Scaffold(
        topBar = { CarVisionTopBar(title = if (state.stage == ScanStage.IDLE) "Scanner" else "Vehicle discovery", showBackButton = true, onBackClick = { if (state.stage == ScanStage.IDLE) onBack() else viewModel.resetScan() }) },
        containerColor = SurfaceDark,
    ) { padding ->
        AnimatedContent(state.stage, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) }, label = "scan-stage", modifier = Modifier.fillMaxSize().padding(padding)) { stage ->
            when (stage) {
                ScanStage.IDLE -> InputChooser(openCamera, { gallery.launch("image/*") })
                ScanStage.IMAGE_CAPTURED, ScanStage.ANALYSING -> VehicleAnalysisOverlay(state)
                ScanStage.RESULT_READY, ScanStage.ADDING_TO_GARAGE, ScanStage.ADDED_TO_GARAGE -> DiscoveryView(state, viewModel::addDiscoveryToGarage, onViewGarage, viewModel::resetScan)
                ScanStage.ERROR -> ErrorView(state, viewModel::retryScan, { gallery.launch("image/*") }, openCamera)
            }
        }
    }
}

@Composable
private fun InputChooser(camera: () -> Unit, gallery: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Discover a vehicle", color = TextWhite, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text("Capture the whole car clearly, or choose an existing photo.", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 9.dp))
        Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier.size(116.dp).clickable(onClick = camera),
            shape = CircleShape,
            color = CyberCyan,
            contentColor = CyanDark,
            shadowElevation = 14.dp,
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, "Capture vehicle", Modifier.size(42.dp)) }
        }
        Text("Take a photo", color = TextWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
        OutlinedButton(gallery, Modifier.fillMaxWidth().padding(top = 30.dp).height(54.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.PhotoLibrary, "Import vehicle photo"); Text("  Choose from gallery")
        }
        Spacer(Modifier.weight(1f)); Text("JPEG, PNG or WebP, 10 MB maximum", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
fun VehicleAnalysisOverlay(state: ScanWorkflowState) {
    Box(Modifier.fillMaxSize().background(Color(0xFF090C12))) {
        AsyncImage(state.imageUri, "Vehicle being analyzed", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)))
        ScannerBrackets(Modifier.fillMaxWidth().aspectRatio(.82f).align(Alignment.Center).padding(24.dp))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xE6141820)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            if (state.inputSource == ScanInputSource.GALLERY) {
                CircularAnalysisView(Modifier.align(Alignment.CenterHorizontally))
            }
            Text("ANALYSING VEHICLE", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            AnalysisStepIndicator("Image received", true, false)
            AnalysisStepIndicator("Identifying vehicle", false, true)
            AnalysisStepIndicator("Checking vehicle data", false, false)
            AnalysisStepIndicator("Reading rarity and Garage reward", false, false)
        }
    }
}

@Composable
fun CircularAnalysisView(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        color = CyberCyan,
        strokeWidth = 3.dp,
        modifier = modifier.size(42.dp),
    )
}

@Composable
fun AnalysisStepIndicator(label: String, complete: Boolean, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when { complete -> Icon(Icons.Default.CheckCircle, "Completed", tint = CyberCyan, modifier = Modifier.size(18.dp)); active -> CircularProgressIndicator(color = CyberCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)); else -> Icon(Icons.Outlined.RadioButtonUnchecked, "Waiting", tint = TextMuted, modifier = Modifier.size(18.dp)) }
        Text(label, color = if (complete || active) TextWhite else TextMuted, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ScannerBrackets(modifier: Modifier) = Canvas(modifier) {
    val length = size.minDimension * .13f; val stroke = 3.dp.toPx(); val color = CyberCyan.copy(alpha = .72f)
    listOf(Offset(0f, 0f) to listOf(Offset(length, 0f), Offset(0f, length)), Offset(size.width, 0f) to listOf(Offset(size.width - length, 0f), Offset(size.width, length)), Offset(0f, size.height) to listOf(Offset(length, size.height), Offset(0f, size.height - length)), Offset(size.width, size.height) to listOf(Offset(size.width - length, size.height), Offset(size.width, size.height - length))).forEach { (corner, ends) -> ends.forEach { drawLine(color, corner, it, stroke, StrokeCap.Square) } }
}

@Composable
private fun DiscoveryView(state: ScanWorkflowState, add: () -> Unit, garage: () -> Unit, continueScanning: () -> Unit) {
    val vehicle = state.activeVehicle ?: return
    val collected = state.stage == ScanStage.ADDED_TO_GARAGE || state.alreadyInGarage
    val feedback = when {
        state.awardedXp != null -> "ADDED TO GARAGE  +${state.awardedXp} XP"
        state.alreadyInGarage || vehicle.isSavedInGarage -> "ALREADY IN GARAGE"
        else -> null
    }
    val captured = state.latestTelemetry?.timestamp?.takeIf { it > 0 }?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VehicleDiscoveryCard(vehicle, state.stage == ScanStage.ADDING_TO_GARAGE, feedback, add, continueScanning, state.imageUri ?: state.latestTelemetry?.imageUrl ?: vehicle.imageUrl, captured, if (collected) "CONTINUE SCANNING" else "NOT NOW")
        if (collected) Button(garage, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLow, contentColor = TextWhite)) { Text("VIEW GARAGE", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ErrorView(state: ScanWorkflowState, retry: () -> Unit, gallery: () -> Unit, camera: () -> Unit) {
    val title = when (state.errorType) { ScanErrorType.UPLOAD -> "UPLOAD FAILED"; ScanErrorType.IDENTIFICATION -> "VEHICLE NOT IDENTIFIED"; ScanErrorType.VEHICLE_DATA -> "VEHICLE DATA UNAVAILABLE"; else -> "SCAN COULD NOT FINISH" }
    Box(Modifier.fillMaxSize()) {
        state.imageUri?.let { AsyncImage(it, "Vehicle scan failed", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .68f)))
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = TextWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Réessayez avec une photo nette montrant clairement le véhicule.", color = TextMuted, textAlign = TextAlign.Center)
            Button(retry, Modifier.fillMaxWidth().height(52.dp)) { Text("TRY AGAIN") }
            OutlinedButton(gallery, Modifier.fillMaxWidth().height(50.dp)) { Text("CHOOSE ANOTHER IMAGE") }
            OutlinedButton(camera, Modifier.fillMaxWidth().height(50.dp)) { Text("TAKE ANOTHER PHOTO") }
        }
    }
}
