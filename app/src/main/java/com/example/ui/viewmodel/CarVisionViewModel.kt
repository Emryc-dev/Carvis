package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CarVisionDatabase
import com.example.data.local.decodeVehiclePhoto
import com.example.data.model.GarageEntry
import com.example.data.model.ScanTelemetry
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanWorkflowState(val isScanning: Boolean = false, val activeVehicle: Vehicle? = null, val latestTelemetry: ScanTelemetry? = null, val error: String? = null)
data class UserProfileState(val isLoggedIn: Boolean = false, val name: String = "", val username: String = "", val avatarUrl: String = "")
data class AuthUiState(val isLoading: Boolean = false, val error: String? = null, val notice: String? = null)
data class GarageUiState(val isLoading: Boolean = false, val error: String? = null, val awardedXp: Int? = null, val alreadyCollected: Boolean = false)

class CarVisionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VehicleRepository(CarVisionDatabase.getDatabase(application))
    val allVehicles = repository.getAllVehicles()
    val garageEntries = repository.getGarageEntries()
    val garageStats = repository.getGarageStats()
    val scanHistory = repository.getAllScans()

    private val _scanWorkflow = MutableStateFlow(ScanWorkflowState())
    val scanWorkflow: StateFlow<ScanWorkflowState> = _scanWorkflow.asStateFlow()
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()
    private val _compareVehicleLeft = MutableStateFlow<Vehicle?>(null)
    val compareVehicleLeft: StateFlow<Vehicle?> = _compareVehicleLeft.asStateFlow()
    private val _compareVehicleRight = MutableStateFlow<Vehicle?>(null)
    val compareVehicleRight: StateFlow<Vehicle?> = _compareVehicleRight.asStateFlow()
    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()
    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()
    private val _garageUiState = MutableStateFlow(GarageUiState())
    val garageUiState: StateFlow<GarageUiState> = _garageUiState.asStateFlow()
    private val _selectedGarageEntry = MutableStateFlow<GarageEntry?>(null)
    val selectedGarageEntry: StateFlow<GarageEntry?> = _selectedGarageEntry.asStateFlow()

    fun selectVehicle(vehicle: Vehicle) { _selectedVehicle.value = vehicle }
    fun selectGarageEntry(entry: GarageEntry) { _selectedGarageEntry.value = entry }

    fun addToGarage(vehicle: Vehicle) {
        viewModelScope.launch {
            if (vehicle.isSavedInGarage) { _garageUiState.value = GarageUiState(alreadyCollected = true); return@launch }
            _garageUiState.value = GarageUiState(isLoading = true)
            runCatching { repository.addToGarage(vehicle.id, _scanWorkflow.value.latestTelemetry?.scanId) }
                .onSuccess { entry ->
                    _selectedVehicle.value = vehicle.copy(isSavedInGarage = true)
                    _garageUiState.value = GarageUiState(awardedXp = entry.newlyAwardedXp.takeIf { it > 0 }, alreadyCollected = entry.alreadyCollected)
                }
                .onFailure { _garageUiState.value = GarageUiState(error = it.message ?: "Impossible d’ajouter ce véhicule au Garage.") }
        }
    }

    fun removeFromGarage(entry: GarageEntry) {
        viewModelScope.launch {
            _garageUiState.value = GarageUiState(isLoading = true)
            runCatching { repository.removeFromGarage(entry.id) }
                .onSuccess { _garageUiState.value = GarageUiState() }
                .onFailure { _garageUiState.value = GarageUiState(error = it.message ?: "Impossible de retirer ce véhicule.") }
        }
    }

    fun refreshGarage() {
        viewModelScope.launch {
            _garageUiState.value = GarageUiState(isLoading = true)
            runCatching { repository.refresh() }
                .onSuccess { _garageUiState.value = GarageUiState() }
                .onFailure { _garageUiState.value = GarageUiState(error = it.message ?: "Impossible de charger votre Garage.") }
        }
    }

    fun clearGarageFeedback() { _garageUiState.value = GarageUiState() }
    fun setComparisonVehicles(left: Vehicle, right: Vehicle) { _compareVehicleLeft.value = left; _compareVehicleRight.value = right }
    fun swapComparisonVehicles() { val left = _compareVehicleLeft.value; _compareVehicleLeft.value = _compareVehicleRight.value; _compareVehicleRight.value = left }

    fun analyzePhoto(bitmap: Bitmap?, onScanFinished: (Vehicle) -> Unit) {
        if (bitmap == null) { _scanWorkflow.value = ScanWorkflowState(error = "Ajoutez une photo pour démarrer l’analyse."); return }
        viewModelScope.launch {
            _scanWorkflow.value = ScanWorkflowState(isScanning = true)
            _garageUiState.value = GarageUiState()
            runCatching { repository.processImageScan(bitmap) }
                .onSuccess { (vehicle, telemetry) ->
                    _selectedVehicle.value = vehicle
                    _scanWorkflow.value = ScanWorkflowState(activeVehicle = vehicle, latestTelemetry = telemetry)
                    onScanFinished(vehicle)
                }
                .onFailure { _scanWorkflow.value = ScanWorkflowState(error = it.message ?: "Impossible d’analyser cette photo.") }
        }
    }

    fun analyzePhotoUri(uri: Uri?, onScanFinished: (Vehicle) -> Unit) {
        if (uri == null) {
            _scanWorkflow.value = ScanWorkflowState(error = "Aucune photo sélectionnée.")
            return
        }
        viewModelScope.launch {
            _scanWorkflow.value = ScanWorkflowState(isScanning = true)
            _garageUiState.value = GarageUiState()
            runCatching {
                val bitmap = withContext(Dispatchers.IO) { decodeVehiclePhoto(getApplication(), uri) }
                    ?: error("Cette image ne peut pas être lue. Essayez une photo JPEG, PNG ou WebP.")
                repository.processImageScan(bitmap)
            }.onSuccess { (vehicle, telemetry) ->
                _selectedVehicle.value = vehicle
                _scanWorkflow.value = ScanWorkflowState(activeVehicle = vehicle, latestTelemetry = telemetry)
                onScanFinished(vehicle)
            }.onFailure {
                _scanWorkflow.value = ScanWorkflowState(error = it.message ?: "Impossible d’analyser cette photo.")
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            runCatching { repository.signIn(email, password) }
                .onSuccess { profile -> _userProfile.value = UserProfileState(true, profile.name, email, profile.avatarUrl); _authUiState.value = AuthUiState(); onSuccess() }
                .onFailure { _authUiState.value = AuthUiState(error = it.message ?: "Connexion impossible.") }
        }
    }

    fun signInWithGoogle(idToken: String, nonce: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            runCatching { repository.signInWithGoogle(idToken, nonce) }
                .onSuccess { profile -> _userProfile.value = UserProfileState(true, profile.name, avatarUrl = profile.avatarUrl); _authUiState.value = AuthUiState(); onSuccess() }
                .onFailure { _authUiState.value = AuthUiState(error = it.message ?: "Connexion Google impossible.") }
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            runCatching { repository.signUp(email, password, name) }
                .onSuccess { profile ->
                    if (profile == null) _authUiState.value = AuthUiState(notice = "Compte créé. Confirmez votre adresse e-mail, puis connectez-vous.")
                    else { _userProfile.value = UserProfileState(true, profile.name, email, profile.avatarUrl); _authUiState.value = AuthUiState(); onSuccess() }
                }
                .onFailure { _authUiState.value = AuthUiState(error = it.message ?: "Inscription impossible.") }
        }
    }

    fun clearAuthMessage() { _authUiState.value = AuthUiState() }
    fun signOut() { repository.signOut(); _selectedVehicle.value = null; _userProfile.value = UserProfileState(); _authUiState.value = AuthUiState(); _garageUiState.value = GarageUiState() }
}

