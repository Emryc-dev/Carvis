package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.local.CarVisionDatabase
import com.example.data.model.ScanTelemetry
import com.example.data.model.Vehicle
import com.example.data.remote.CarVisionApi
import com.example.data.remote.RemoteProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VehicleRepository(@Suppress("UNUSED_PARAMETER") database: CarVisionDatabase) {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    private val favorites = MutableStateFlow<List<Vehicle>>(emptyList())
    private val scans = MutableStateFlow<List<ScanTelemetry>>(emptyList())

    fun getAllVehicles(): StateFlow<List<Vehicle>> = vehicles
    fun getGarageVehicles(): StateFlow<List<Vehicle>> = favorites
    fun getAllScans(): StateFlow<List<ScanTelemetry>> = scans
    suspend fun getVehicleById(id: String): Vehicle? = vehicles.value.firstOrNull { it.id == id }

    suspend fun signIn(email: String, password: String): RemoteProfile {
        val snapshot = CarVisionApi.signIn(email, password)
        applySnapshot(snapshot)
        return snapshot.profile
    }

    suspend fun signUp(email: String, password: String, name: String): RemoteProfile? {
        val snapshot = CarVisionApi.signUp(email, password, name) ?: return null
        applySnapshot(snapshot)
        return snapshot.profile
    }

    suspend fun refresh(): RemoteProfile {
        val snapshot = CarVisionApi.snapshot()
        applySnapshot(snapshot)
        return snapshot.profile
    }

    suspend fun toggleGarageStatus(vehicleId: String, currentStatus: Boolean) {
        CarVisionApi.setFavorite(vehicleId, !currentStatus)
        val snapshot = CarVisionApi.snapshot()
        applySnapshot(snapshot)
    }

    suspend fun processImageScan(bitmap: Bitmap): Pair<Vehicle, ScanTelemetry> {
        val result = CarVisionApi.scan(bitmap)
        val snapshot = CarVisionApi.snapshot()
        applySnapshot(snapshot)
        return result
    }

    fun signOut() {
        CarVisionApi.signOut()
        vehicles.value = emptyList()
        favorites.value = emptyList()
        scans.value = emptyList()
    }

    private fun applySnapshot(snapshot: com.example.data.remote.RemoteSnapshot) {
        val favoriteIds = snapshot.favorites.mapTo(hashSetOf()) { it.id }
        vehicles.value = snapshot.vehicles.map { it.copy(isSavedInGarage = it.id in favoriteIds) }
        favorites.value = snapshot.favorites.map { it.copy(isSavedInGarage = true) }
        scans.value = snapshot.scans
    }
}


