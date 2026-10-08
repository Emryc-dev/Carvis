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
import com.example.data.remote.CarVisionApi
import com.example.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

enum class ScanInputSource { CAMERA, GALLERY }
enum class ScanStage { IDLE, IMAGE_CAPTURED, ANALYSING, RESULT_READY, ADDING_TO_GARAGE, ADDED_TO_GARAGE, ERROR }
enum class ScanErrorType { UPLOAD, IDENTIFICATION, VEHICLE_DATA, UNKNOWN }

data class ScanWorkflowState(
    val stage: ScanStage = ScanStage.IDLE,
    val inputSource: ScanInputSource? = null,
    val imageUri: Uri? = null,
    val activeVehicle: Vehicle? = null,
    val latestTelemetry: ScanTelemetry? = null,
    val alreadyInGarage: Boolean = false,
    val awardedXp: Int? = null,
    val errorType: ScanErrorType? = null,
    val error: String? = null,
) {
    val isScanning: Boolean get() = stage == ScanStage.IMAGE_CAPTURED || stage == ScanStage.ANALYSING
}
data class UserProfileState(val isLoggedIn: Boolean = false, val name: String = "", val username: String = "", val avatarUrl: String = "")
data class AuthUiState(val isLoading: Boolean = false, val error: String? = null, val notice: String? = null)
data class GarageUiState(val isLoading: Boolean = false, val error: String? = null, val awardedXp: Int? = null, val alreadyCollected: Boolean = false)

class CarVisionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VehicleRepository(CarVisionDatabase.getDatabase(application))
    private var scanJob: Job? = null

    init { CarVisionApi.initialize(application) }
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
    fun consumeGarageError() {
        _garageUiState.value = _garageUiState.value.copy(error = null)
    }
    fun consumeScanError() {
        _scanWorkflow.value = _scanWorkflow.value.copy(error = null)
    }
    fun setComparisonVehicles(left: Vehicle, right: Vehicle) { _compareVehicleLeft.value = left; _compareVehicleRight.value = right }
    fun swapComparisonVehicles() { val left = _compareVehicleLeft.value; _compareVehicleLeft.value = _compareVehicleRight.value; _compareVehicleRight.value = left }

    fun analyzePhoto(bitmap: Bitmap?, onScanFinished: (Vehicle) -> Unit) {
        if (bitmap == null) { _scanWorkflow.value = ScanWorkflowState(stage = ScanStage.ERROR, errorType = ScanErrorType.UPLOAD, error = "Ajoutez une photo pour démarrer l’analyse."); return }
        viewModelScope.launch {
            _scanWorkflow.value = ScanWorkflowState(stage = ScanStage.ANALYSING, inputSource = ScanInputSource.CAMERA)
            _garageUiState.value = GarageUiState()
            runCatching { repository.processImageScan(bitmap) }
                .onSuccess { (vehicle, telemetry) ->
                    _selectedVehicle.value = vehicle
                    _scanWorkflow.value = ScanWorkflowState(stage = ScanStage.RESULT_READY, inputSource = ScanInputSource.CAMERA, activeVehicle = vehicle, latestTelemetry = telemetry, alreadyInGarage = vehicle.isSavedInGarage)
                    onScanFinished(vehicle)
                }
                .onFailure { _scanWorkflow.value = ScanWorkflowState(stage = ScanStage.ERROR, inputSource = ScanInputSource.CAMERA, errorType = ScanErrorType.UNKNOWN, error = it.message ?: "Impossible d’analyser cette photo.") }
        }
    }

    fun analyzePhotoUri(uri: Uri?, source: ScanInputSource = ScanInputSource.GALLERY) {
        if (uri == null) {
            _scanWorkflow.value = ScanWorkflowState(stage = ScanStage.ERROR, inputSource = source, errorType = ScanErrorType.UPLOAD, error = "Aucune photo sélectionnée.")
            return
        }
        _scanWorkflow.value = ScanWorkflowState(stage = ScanStage.IMAGE_CAPTURED, inputSource = source, imageUri = uri)
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanWorkflow.value = _scanWorkflow.value.copy(stage = ScanStage.ANALYSING, error = null, errorType = null)
            _garageUiState.value = GarageUiState()
            runCatching {
                val bitmap = withContext(Dispatchers.IO) { decodeVehiclePhoto(getApplication(), uri) }
                    ?: error("Cette image ne peut pas être lue. Essayez une photo JPEG, PNG ou WebP.")
                repository.processImageScan(bitmap)
            }.onSuccess { (vehicle, telemetry) ->
                _selectedVehicle.value = vehicle
                _scanWorkflow.value = _scanWorkflow.value.copy(
                    stage = ScanStage.RESULT_READY,
                    activeVehicle = vehicle,
                    latestTelemetry = telemetry,
                    alreadyInGarage = vehicle.isSavedInGarage,
                )
            }.onFailure {
                val message = it.message ?: "Impossible d’analyser cette photo."
                val type = when {
                    message.contains("stored", true) || message.contains("upload", true) || message.contains("stock", true) -> ScanErrorType.UPLOAD
                    message.contains("AI", true) || message.contains("recognition", true) || message.contains("identif", true) -> ScanErrorType.IDENTIFICATION
                    message.contains("catalog", true) || message.contains("vehicle", true) || message.contains("véhicule", true) -> ScanErrorType.VEHICLE_DATA
                    else -> ScanErrorType.UNKNOWN
                }
                _scanWorkflow.value = _scanWorkflow.value.copy(stage = ScanStage.ERROR, errorType = type, error = message)
            }
        }
    }

    fun retryScan() {
        val state = _scanWorkflow.value
        analyzePhotoUri(state.imageUri, state.inputSource ?: ScanInputSource.GALLERY)
    }

    fun resetScan() {
        scanJob?.cancel()
        scanJob = null
        _scanWorkflow.value = ScanWorkflowState()
        _garageUiState.value = GarageUiState()
    }

    fun addDiscoveryToGarage() {
        val state = _scanWorkflow.value
        val vehicle = state.activeVehicle ?: return
        if (state.alreadyInGarage || state.stage == ScanStage.ADDING_TO_GARAGE) return
        viewModelScope.launch {
            _scanWorkflow.value = state.copy(stage = ScanStage.ADDING_TO_GARAGE, error = null)
            runCatching { repository.addToGarage(vehicle.id, state.latestTelemetry?.scanId) }
                .onSuccess { entry ->
                    _selectedVehicle.value = vehicle.copy(isSavedInGarage = true)
                    _scanWorkflow.value = _scanWorkflow.value.copy(
                        stage = ScanStage.ADDED_TO_GARAGE,
                        activeVehicle = vehicle.copy(isSavedInGarage = true),
                        alreadyInGarage = entry.alreadyCollected,
                        awardedXp = entry.newlyAwardedXp.takeIf { it > 0 },
                    )
                }
                .onFailure { error ->
                    _scanWorkflow.value = _scanWorkflow.value.copy(
                        stage = ScanStage.RESULT_READY,
                        errorType = ScanErrorType.UNKNOWN,
                        error = error.message ?: "Impossible d’ajouter ce véhicule au Garage.",
                    )
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

    fun restoreSession(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.restoreSession() }
                .onSuccess { profile ->
                    if (profile == null) onComplete(false)
                    else {
                        _userProfile.value = UserProfileState(true, profile.name, avatarUrl = profile.avatarUrl)
                        onComplete(true)
                    }
                }
                .onFailure { onComplete(false) }
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
    fun consumeAuthError() {
        _authUiState.value = _authUiState.value.copy(error = null)
    }
    fun signOut() { repository.signOut(); _selectedVehicle.value = null; _userProfile.value = UserProfileState(); _authUiState.value = AuthUiState(); _garageUiState.value = GarageUiState() }
}

