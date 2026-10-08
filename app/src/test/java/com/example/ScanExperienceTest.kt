package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.mutableStateOf
import com.example.data.model.Vehicle
import com.example.data.model.VehicleRarity
import com.example.ui.components.VehicleDiscoveryCard
import com.example.ui.components.InAppNotification
import com.example.ui.components.InAppNotificationHost
import com.example.ui.theme.CarVisionTheme
import com.example.ui.viewmodel.ScanInputSource
import com.example.ui.viewmodel.ScanStage
import com.example.ui.viewmodel.ScanWorkflowState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ScanExperienceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun scanState_onlyReportsScanningForCapturedAndAnalysisStages() {
        assertFalse(ScanWorkflowState().isScanning)
        assertTrue(ScanWorkflowState(stage = ScanStage.IMAGE_CAPTURED, inputSource = ScanInputSource.CAMERA).isScanning)
        assertTrue(ScanWorkflowState(stage = ScanStage.ANALYSING, inputSource = ScanInputSource.GALLERY).isScanning)
        assertFalse(ScanWorkflowState(stage = ScanStage.RESULT_READY).isScanning)
        assertFalse(ScanWorkflowState(stage = ScanStage.ERROR).isScanning)
    }

    @Test
    fun discoveryCardDisplaysBackendVehicleRarityAndXp() {
        compose.setContent {
            CarVisionTheme {
                VehicleDiscoveryCard(
                    vehicle = vehicle(),
                    isAdding = false,
                    feedback = null,
                    onAdd = {},
                    onNotNow = {},
                    imageModel = null,
                    capturedAt = "2026-10-08",
                )
            }
        }

        compose.onNodeWithText("PORSCHE").assertIsDisplayed()
        compose.onNodeWithText("911 GT3").assertIsDisplayed()
        compose.onNodeWithText("MYTHIC").assertIsDisplayed()
        compose.onNodeWithText("+1180 XP").assertIsDisplayed()
        compose.onNodeWithText("CAPTURED 2026-10-08").assertIsDisplayed()
        compose.onNodeWithText("AJOUTER AU GARAGE").assertIsDisplayed()
    }

    @Test
    fun discoveryCardDisablesCollectionForDuplicateVehicle() {
        compose.setContent {
            CarVisionTheme {
                VehicleDiscoveryCard(
                    vehicle = vehicle().copy(isSavedInGarage = true),
                    isAdding = false,
                    feedback = "ALREADY IN GARAGE",
                    onAdd = {},
                    onNotNow = {},
                    imageModel = null,
                )
            }
        }

        compose.onNodeWithText("DÉJÀ DANS LE GARAGE").assertIsNotEnabled()
        compose.onNodeWithText("ALREADY IN GARAGE").assertIsDisplayed()
    }

    @Test
    fun inAppErrorNotificationDismissesItself() {
        val current = mutableStateOf<InAppNotification?>(
            InAppNotification(id = 1L, message = "Impossible de charger les données."),
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CarVisionTheme {
                InAppNotificationHost(
                    notification = current.value,
                    onDismiss = { current.value = null },
                )
            }
        }

        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Impossible de charger les données.").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(4_100)
        compose.mainClock.advanceTimeBy(400)
        compose.onAllNodesWithText("Impossible de charger les données.").assertCountEquals(0)
    }

    private fun vehicle() = Vehicle(
        id = "vehicle-1",
        brand = "Porsche",
        model = "911 GT3",
        generation = "992",
        year = 2024,
        category = "Sports car",
        engine = "4.0L flat-six",
        horsepower = 502,
        torque = "470 Nm",
        transmission = "PDK",
        drivetrain = "RWD",
        fuel = "Petrol",
        fuelConsumption = "",
        zeroToSixty = 3.2,
        topSpeedMph = 197,
        estimatedPrice = 0,
        newPrice = 0,
        usedPrice = 0,
        curbWeightLbs = 0,
        reliabilityScore = 0,
        description = "",
        colorName = "",
        chassisCode = "",
        factoryPackages = emptyList(),
        imageUrl = "",
        rarity = VehicleRarity.MYTHIC,
        baseXp = 1000,
        collectionXp = 1180,
        lastScannedTimestamp = 0,
    )
}
