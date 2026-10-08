package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.local.CarVisionDatabase
import com.example.data.model.ScanTelemetry
import com.example.data.model.Vehicle
import com.example.data.model.GarageEntry
import com.example.data.model.GarageStats
import com.example.data.remote.CarVisionApi
import com.example.data.remote.RemoteProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VehicleRepository(@Suppress("UNUSED_PARAMETER") database: CarVisionDatabase) {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    private val garageEntries = MutableStateFlow<List<GarageEntry>>(emptyList())
    private val garageStats = MutableStateFlow(GarageStats())
    private val scans = MutableStateFlow<List<ScanTelemetry>>(emptyList())

    fun getAllVehicles(): StateFlow<List<Vehicle>> = vehicles
    fun getGarageEntries(): StateFlow<List<GarageEntry>> = garageEntries
    fun getGarageStats(): StateFlow<GarageStats> = garageStats
    fun getAllScans(): StateFlow<List<ScanTelemetry>> = scans
    suspend fun getVehicleById(id: String): Vehicle? = vehicles.value.firstOrNull { it.id == id }

    suspend fun signIn(email: String, password: String): RemoteProfile {
        val snapshot = CarVisionApi.signIn(email, password)
        applySnapshot(snapshot)
        return snapshot.profile
    }

    suspend fun signInWithGoogle(idToken: String, nonce: String): RemoteProfile {
        val snapshot = CarVisionApi.signInWithGoogle(idToken, nonce)
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

    suspend fun addToGarage(vehicleId: String, scanId: String?): GarageEntry {
        val result = CarVisionApi.addToGarage(vehicleId, scanId)
        val snapshot = CarVisionApi.snapshot()
        applySnapshot(snapshot)
        return result
    }

    suspend fun removeFromGarage(entryId: String) {
        CarVisionApi.removeFromGarage(entryId)
        applySnapshot(CarVisionApi.snapshot())
    }

    suspend fun processImageScan(bitmap: Bitmap): Pair<Vehicle, ScanTelemetry> {
        val (vehicle, telemetry) = CarVisionApi.scan(bitmap)
        val snapshot = CarVisionApi.snapshot()
        applySnapshot(snapshot)
        val collected = snapshot.garage.any { it.vehicle.id == vehicle.id }
        return vehicle.copy(isSavedInGarage = collected) to telemetry
    }

    fun signOut() {
        CarVisionApi.signOut()
        vehicles.value = emptyList()
        garageEntries.value = emptyList()
        garageStats.value = GarageStats()
        scans.value = emptyList()
    }

    private fun applySnapshot(snapshot: com.example.data.remote.RemoteSnapshot) {
        val garageIds = snapshot.garage.mapTo(hashSetOf()) { it.vehicle.id }
        vehicles.value = snapshot.vehicles.map { it.copy(isSavedInGarage = it.id in garageIds) }
        garageEntries.value = snapshot.garage
        garageStats.value = snapshot.garageStats
        scans.value = snapshot.scans
    }
}


