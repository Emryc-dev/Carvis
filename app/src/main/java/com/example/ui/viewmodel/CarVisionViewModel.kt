package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CarVisionDatabase
import com.example.data.model.ScanTelemetry
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanWorkflowState(
    val isScanning: Boolean = false,
    val activeVehicle: Vehicle? = null,
    val latestTelemetry: ScanTelemetry? = null,
    val error: String? = null
)

data class UserProfileState(
    val isLoggedIn: Boolean = false,
    val name: String = "",
    val username: String = "",
    val avatarUrl: String = ""
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class CarVisionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VehicleRepository(CarVisionDatabase.getDatabase(application))

    val allVehicles = repository.getAllVehicles()
    val garageVehicles = repository.getGarageVehicles()
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

    fun selectVehicle(vehicle: Vehicle) { _selectedVehicle.value = vehicle }

    fun toggleGarage(vehicle: Vehicle) {
        viewModelScope.launch {
            runCatching { repository.toggleGarageStatus(vehicle.id, vehicle.isSavedInGarage) }
                .onFailure { _scanWorkflow.value = _scanWorkflow.value.copy(error = it.message ?: "Impossible de mettre à jour le garage.") }
        }
    }

    fun setComparisonVehicles(left: Vehicle, right: Vehicle) {
        _compareVehicleLeft.value = left
        _compareVehicleRight.value = right
    }

    fun swapComparisonVehicles() {
        val left = _compareVehicleLeft.value
        _compareVehicleLeft.value = _compareVehicleRight.value
        _compareVehicleRight.value = left
    }

    fun analyzePhoto(bitmap: Bitmap?, onScanFinished: (Vehicle) -> Unit) {
        if (bitmap == null) {
            _scanWorkflow.value = ScanWorkflowState(error = "Ajoutez une photo pour démarrer l’analyse.")
            return
        }
        viewModelScope.launch {
            _scanWorkflow.value = ScanWorkflowState(isScanning = true)
            runCatching { repository.processImageScan(bitmap) }
                .onSuccess { (vehicle, telemetry) ->
                    _selectedVehicle.value = vehicle
                    _scanWorkflow.value = ScanWorkflowState(activeVehicle = vehicle, latestTelemetry = telemetry)
                    onScanFinished(vehicle)
                }
                .onFailure {
                    _scanWorkflow.value = ScanWorkflowState(error = it.message ?: "Impossible d’analyser cette photo.")
                }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            runCatching { repository.signIn(email, password) }
                .onSuccess { profile ->
                    _userProfile.value = UserProfileState(
                        isLoggedIn = true,
                        name = profile.name,
                        username = email,
                        avatarUrl = profile.avatarUrl
                    )
                    _authUiState.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { _authUiState.value = AuthUiState(error = it.message ?: "Connexion impossible.") }
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            runCatching { repository.signUp(email, password, name) }
                .onSuccess { profile ->
                    if (profile == null) {
                        _authUiState.value = AuthUiState(notice = "Compte créé. Consultez votre e-mail pour confirmer votre inscription, puis connectez-vous.")
                    } else {
                        _userProfile.value = UserProfileState(true, profile.name, email, profile.avatarUrl)
                        _authUiState.value = AuthUiState()
                        onSuccess()
                    }
                }
                .onFailure { _authUiState.value = AuthUiState(error = it.message ?: "Inscription impossible.") }
        }
    }

    fun clearAuthMessage() {
        _authUiState.value = AuthUiState()
    }

    fun signOut() {
        repository.signOut()
        _selectedVehicle.value = null
        _userProfile.value = UserProfileState()
        _authUiState.value = AuthUiState()
    }
}



