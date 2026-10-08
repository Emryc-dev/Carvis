package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.components.InAppNotification
import com.example.ui.components.InAppNotificationHost
import com.example.ui.screens.AdminMonitoringScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.CarVisionHomeScreen
import com.example.ui.screens.CarVisionOnboardingScreen
import com.example.ui.screens.GarageProfileScreen
import com.example.ui.screens.GarageTab
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.GarageDetailScreen
import com.example.ui.screens.ScanExperienceScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VehicleCompareScreen
import com.example.ui.screens.VehicleResultScreen
import com.example.ui.theme.CarVisionTheme
import com.example.ui.viewmodel.CarVisionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CarVisionTheme { CarVisionApp() } }
    }
}

@Composable
fun CarVisionApp(viewModel: CarVisionViewModel = viewModel()) {
    val nav = rememberNavController()
    val vehicles by viewModel.allVehicles.collectAsState()
    val garage by viewModel.garageEntries.collectAsState()
    val garageStats by viewModel.garageStats.collectAsState()
    val garageState by viewModel.garageUiState.collectAsState()
    val selectedGarageEntry by viewModel.selectedGarageEntry.collectAsState()
    val history by viewModel.scanHistory.collectAsState()
    val selected by viewModel.selectedVehicle.collectAsState()
    val left by viewModel.compareVehicleLeft.collectAsState()
    val right by viewModel.compareVehicleRight.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val authState by viewModel.authUiState.collectAsState()
    val scanState by viewModel.scanWorkflow.collectAsState()
    var notification by remember { mutableStateOf<InAppNotification?>(null) }
    val showError: (String) -> Unit = { message ->
        if (message.isNotBlank()) notification = InAppNotification(message = message)
    }

    LaunchedEffect(authState.error) {
        authState.error?.let {
            showError(it)
            viewModel.consumeAuthError()
        }
    }
    LaunchedEffect(garageState.error) {
        garageState.error?.let {
            showError(it)
            viewModel.consumeGarageError()
        }
    }
    LaunchedEffect(scanState.error) {
        scanState.error?.let {
            showError(it)
            viewModel.consumeScanError()
        }
    }

    Box(Modifier.fillMaxSize()) {
    NavHost(navController = nav, startDestination = Screen.Splash.route, modifier = Modifier.fillMaxSize()) {
        composable(Screen.Splash.route) {
            LaunchedEffect(Unit) {
                viewModel.restoreSession { restored ->
                    nav.navigate(if (restored) Screen.Dashboard.route else Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            SplashScreen()
        }
        composable(Screen.Onboarding.route) {
            CarVisionOnboardingScreen(
                onComplete = { nav.navigate(Screen.Auth.route) },
                onSignIn = { nav.navigate(Screen.Auth.route) }
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                state = authState,
                googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                onSignIn = { email, password ->
                    viewModel.signIn(email, password) {
                        nav.navigate(Screen.Dashboard.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
                    }
                },
                onSignUp = { email, password, name ->
                    viewModel.signUp(email, password, name) {
                        nav.navigate(Screen.Dashboard.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
                    }
                },
                onGoogleSignIn = { idToken, nonce ->
                    viewModel.signInWithGoogle(idToken, nonce) {
                        nav.navigate(Screen.Dashboard.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
                    }
                },
                onBack = { nav.popBackStack() },
                onModeChange = viewModel::clearAuthMessage,
                onError = showError,
            )
        }
        composable(Screen.Dashboard.route) {
            CarVisionHomeScreen(
                userName = profile.name,
                avatarUrl = profile.avatarUrl,
                latestVehicle = selected,
                scanState = scanState,
                onNavigate = { route -> if (route != Screen.Dashboard.route) nav.navigate(route) },
                onSelectVehicle = viewModel::selectVehicle,
                onImportPhoto = { uri ->
                    viewModel.analyzePhotoUri(uri)
                    nav.navigate(Screen.Scanner.route)
                }
            )
        }
        composable(Screen.Scanner.route) {
            ScanExperienceScreen(
                viewModel = viewModel,
                onError = showError,
                onBack = {
                    viewModel.resetScan()
                    nav.popBackStack()
                },
                onViewGarage = {
                    nav.navigate(Screen.Garage.route) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.VehicleResult.route) {
            VehicleResultScreen(
                vehicle = selected,
                garageState = garageState,
                onBackClick = { nav.popBackStack() },
                onCompareClick = { vehicle ->
                    vehicles.firstOrNull { it.id != vehicle.id }?.let {
                        viewModel.setComparisonVehicles(vehicle, it)
                        nav.navigate(Screen.Compare.route)
                    }
                },
                onAddToGarage = viewModel::addToGarage,
                onNotNow = { nav.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = false } } }
            )
        }
        composable(Screen.Compare.route) {
            VehicleCompareScreen(
                vehicleLeft = left,
                vehicleRight = right,
                allVehicles = vehicles,
                onBackClick = { nav.popBackStack() },
                onSwapVehicles = viewModel::swapComparisonVehicles,
                onSelectLeftVehicle = { vehicle -> right?.let { viewModel.setComparisonVehicles(vehicle, it) } },
                onSelectRightVehicle = { vehicle -> left?.let { viewModel.setComparisonVehicles(it, vehicle) } },
                onNavigate = { route -> if (route != Screen.Compare.route) nav.navigate(route) }
            )
        }
        composable(Screen.Garage.route) {
            GarageScreen(
                entries = garage,
                stats = garageStats,
                state = garageState,
                onRetry = viewModel::refreshGarage,
                onScan = { nav.navigate(Screen.Scanner.route) },
                onSelect = { entry -> viewModel.selectGarageEntry(entry); nav.navigate(Screen.GarageDetail.route) },
                onNavigate = { route -> if (route != Screen.Garage.route) nav.navigate(route) }
            )
        }
        composable(Screen.GarageDetail.route) {
            GarageDetailScreen(
                entry = selectedGarageEntry,
                onBack = { nav.popBackStack() },
                onRemove = { entry -> viewModel.removeFromGarage(entry); nav.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(scans = history, onNavigate = { route -> if (route != Screen.History.route) nav.navigate(route) })
        }
        composable(Screen.Admin.route) { AdminMonitoringScreen { nav.popBackStack() } }
    }
        InAppNotificationHost(
            notification = notification,
            onDismiss = { notification = null },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )
    }
}







