package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.Vehicle
import com.example.data.model.ScanTelemetry
import com.example.data.model.VehicleRarity

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
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
    val factoryPackages: String, // comma separated
    val imageUrl: String,
    val isSavedInGarage: Boolean,
    val lastScannedTimestamp: Long
) {
    fun toVehicle(): Vehicle = Vehicle(
        id = id,
        brand = brand,
        model = model,
        generation = generation,
        year = year,
        category = category,
        engine = engine,
        horsepower = horsepower,
        torque = torque,
        transmission = transmission,
        drivetrain = drivetrain,
        fuel = fuel,
        fuelConsumption = fuelConsumption,
        zeroToSixty = zeroToSixty,
        topSpeedMph = topSpeedMph,
        estimatedPrice = estimatedPrice,
        newPrice = newPrice,
        usedPrice = usedPrice,
        curbWeightLbs = curbWeightLbs,
        reliabilityScore = reliabilityScore,
        description = description,
        colorName = colorName,
        chassisCode = chassisCode,
        factoryPackages = factoryPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        imageUrl = imageUrl,
        rarity = VehicleRarity.COMMON,
        baseXp = 50,
        collectionXp = 50,
        isSavedInGarage = isSavedInGarage,
        lastScannedTimestamp = lastScannedTimestamp
    )

    companion object {
        fun fromVehicle(v: Vehicle): VehicleEntity = VehicleEntity(
            id = v.id,
            brand = v.brand,
            model = v.model,
            generation = v.generation,
            year = v.year,
            category = v.category,
            engine = v.engine,
            horsepower = v.horsepower,
            torque = v.torque,
            transmission = v.transmission,
            drivetrain = v.drivetrain,
            fuel = v.fuel,
            fuelConsumption = v.fuelConsumption,
            zeroToSixty = v.zeroToSixty,
            topSpeedMph = v.topSpeedMph,
            estimatedPrice = v.estimatedPrice,
            newPrice = v.newPrice,
            usedPrice = v.usedPrice,
            curbWeightLbs = v.curbWeightLbs,
            reliabilityScore = v.reliabilityScore,
            description = v.description,
            colorName = v.colorName,
            chassisCode = v.chassisCode,
            factoryPackages = v.factoryPackages.joinToString(","),
            imageUrl = v.imageUrl,
            isSavedInGarage = v.isSavedInGarage,
            lastScannedTimestamp = v.lastScannedTimestamp
        )
    }
}

@Entity(tableName = "scan_telemetry")
data class ScanTelemetryEntity(
    @PrimaryKey val scanId: String,
    val timestamp: Long,
    val vehicleName: String,
    val confidence: Double,
    val latencyMs: Long,
    val lidarDepth: String,
    val trimVerified: String,
    val imageUrl: String,
    val status: String
) {
    fun toScanTelemetry(): ScanTelemetry = ScanTelemetry(
        scanId = scanId,
        timestamp = timestamp,
        vehicleName = vehicleName,
        confidence = confidence,
        latencyMs = latencyMs,
        lidarDepth = lidarDepth,
        trimVerified = trimVerified,
        imageUrl = imageUrl,
        status = status
    )

    companion object {
        fun fromScanTelemetry(st: ScanTelemetry): ScanTelemetryEntity = ScanTelemetryEntity(
            scanId = st.scanId,
            timestamp = st.timestamp,
            vehicleName = st.vehicleName,
            confidence = st.confidence,
            latencyMs = st.latencyMs,
            lidarDepth = st.lidarDepth,
            trimVerified = st.trimVerified,
            imageUrl = st.imageUrl,
            status = st.status
        )
    }
}
