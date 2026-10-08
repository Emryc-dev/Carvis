package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object Scanner : Screen("scanner")
    object VehicleResult : Screen("vehicle_result")
    object Compare : Screen("compare")
    object Garage : Screen("garage")
    object GarageDetail : Screen("garage_detail")
    object Admin : Screen("admin")
}


