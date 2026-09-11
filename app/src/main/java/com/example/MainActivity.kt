package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdminMonitoringScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GarageProfileScreen
import com.example.ui.screens.GarageTab
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VehicleCompareScreen
import com.example.ui.screens.VehicleResultScreen
import com.example.ui.theme.CarVisionTheme
import com.example.ui.viewmodel.CarVisionViewModel
import kotlinx.coroutines.delay

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
    val garage by viewModel.garageVehicles.collectAsState()
    val history by viewModel.scanHistory.collectAsState()
    val selected by viewModel.selectedVehicle.collectAsState()
    val left by viewModel.compareVehicleLeft.collectAsState()
    val right by viewModel.compareVehicleRight.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val authState by viewModel.authUiState.collectAsState()

    NavHost(navController = nav, startDestination = Screen.Splash.route, modifier = Modifier.fillMaxSize()) {
        composable(Screen.Splash.route) {
            LaunchedEffect(Unit) {
                delay(1100)
                nav.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            SplashScreen()
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onGetStarted = { nav.navigate(Screen.Auth.route) },
                onSignIn = { nav.navigate(Screen.Auth.route) }
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                state = authState,
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
                onBack = { nav.popBackStack() },
                onModeChange = viewModel::clearAuthMessage
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                userName = profile.name,
                avatarUrl = profile.avatarUrl,
                vehicles = vehicles,
                latestVehicle = selected,
                onNavigate = { route -> if (route != Screen.Dashboard.route) nav.navigate(route) },
                onSelectVehicle = viewModel::selectVehicle,
                onScanPhotoPicked = { bitmap ->
                    viewModel.analyzePhoto(bitmap) {
                        nav.navigate(Screen.VehicleResult.route)
                    }
                }
            )
        }
        composable(Screen.Scanner.route) {
            ScannerScreen(
                viewModel = viewModel,
                onBackClick = { nav.popBackStack() },
                onScanFinished = { vehicle ->
                    viewModel.selectVehicle(vehicle)
                    nav.navigate(Screen.VehicleResult.route) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.VehicleResult.route) {
            VehicleResultScreen(
                vehicle = selected,
                onBackClick = { nav.popBackStack() },
                onCompareClick = { vehicle ->
                    vehicles.firstOrNull { it.id != vehicle.id }?.let {
                        viewModel.setComparisonVehicles(vehicle, it)
                        nav.navigate(Screen.Compare.route)
                    }
                },
                onToggleGarage = viewModel::toggleGarage
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
            GarageProfileScreen(
                viewModel = viewModel,
                garageVehicles = garage,
                scanHistory = history,
                onSelectVehicle = { vehicle ->
                    viewModel.selectVehicle(vehicle)
                    nav.navigate(Screen.VehicleResult.route)
                },
                onNavigate = { route -> if (route != Screen.Garage.route) nav.navigate(route) },
                onSignOut = {
                    viewModel.signOut()
                    nav.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Screen.History.route) {
            GarageProfileScreen(
                initialTab = GarageTab.History,
                viewModel = viewModel,
                garageVehicles = garage,
                scanHistory = history,
                onSelectVehicle = { vehicle ->
                    viewModel.selectVehicle(vehicle)
                    nav.navigate(Screen.VehicleResult.route)
                },
                onNavigate = { route -> if (route != Screen.History.route) nav.navigate(route) },
                onSignOut = {
                    viewModel.signOut()
                    nav.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Screen.Admin.route) { AdminMonitoringScreen { nav.popBackStack() } }
    }
}







