package com.example.data.model

data class Vehicle(
    val id: String,
    val brand: String,
    val model: String,
    val generation: String,
    val year: Int,
    val category: String,
    val engine: String,
    val horsepower: Int,
    val torque: String,
    val transmission: String,
    val drivetrain: String,
    val fuel: String,
    val fuelConsumption: String,
    val zeroToSixty: Double,
    val topSpeedMph: Int,
    val estimatedPrice: Int,
    val newPrice: Int,
    val usedPrice: Int,
    val curbWeightLbs: Int,
    val reliabilityScore: Int,
    val description: String,
    val colorName: String,
    val chassisCode: String,
    val factoryPackages: List<String>,
    val imageUrl: String,
    val rarity: VehicleRarity = VehicleRarity.COMMON,
    val baseXp: Int = 50,
    val collectionXp: Int = baseXp,
    val isSavedInGarage: Boolean = false,
    val lastScannedTimestamp: Long = System.currentTimeMillis()
)

enum class VehicleRarity { COMMON, UNCOMMON, RARE, EPIC, MYTHIC }

data class GarageEntry(
    val id: String,
    val vehicle: Vehicle,
    val rarity: VehicleRarity,
    val xpEarned: Int,
    val capturedAt: String,
    val capturedImageUrl: String,
    val newlyAwardedXp: Int = 0,
    val alreadyCollected: Boolean = false
)

data class GarageStats(
    val carsCollected: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val nextLevelXp: Int? = 500,
    val progressToNextLevel: Float = 0f
)

data class ScanTelemetry(
    val scanId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val vehicleName: String,
    val confidence: Double,
    val latencyMs: Long,
    val lidarDepth: String,
    val trimVerified: String,
    val imageUrl: String,
    val status: String = "VERIFIED"
)
