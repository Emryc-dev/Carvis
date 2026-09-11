package com.example.data.remote

import android.graphics.Bitmap
import com.example.BuildConfig
import com.example.data.model.ScanTelemetry
import com.example.data.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class RemoteProfile(val name: String, val avatarUrl: String)
data class RemoteSnapshot(
    val profile: RemoteProfile,
    val vehicles: List<Vehicle>,
    val favorites: List<Vehicle>,
    val scans: List<ScanTelemetry>
)

object CarVisionApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    @Volatile private var accessToken: String? = null

    private val apiBase get() = BuildConfig.API_BASE_URL.trimEnd('/')
    private val supabaseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    private fun configured(): Boolean =
        apiBase.startsWith("http") &&
            supabaseUrl.startsWith("https://") &&
            !supabaseUrl.contains("PROJECT_REF") &&
            publishableKey.isNotBlank() &&
            !publishableKey.contains("REPLACE_ME")

    suspend fun signIn(email: String, password: String): RemoteSnapshot = withContext(Dispatchers.IO) {
        if (!configured()) error("Configuration mobile manquante. Renseignez API_BASE_URL, SUPABASE_URL et SUPABASE_PUBLISHABLE_KEY.")
        val body = JSONObject().put("email", email).put("password", password).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=password")
            .header("apikey", publishableKey)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error(JSONObject(text).optString("msg", "Connexion refusée."))
            accessToken = JSONObject(text).getString("access_token")
        }
        snapshot()
    }

    suspend fun signUp(email: String, password: String, name: String): RemoteSnapshot? = withContext(Dispatchers.IO) {
        if (!configured()) error("Configuration mobile manquante. Renseignez API_BASE_URL, SUPABASE_URL et SUPABASE_PUBLISHABLE_KEY.")
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("data", JSONObject().put("full_name", name))
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/signup")
            .header("apikey", publishableKey)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) error(json.optString("msg", "Impossible de créer le compte."))
            val token = json.optString("access_token")
            if (token.isBlank()) return@withContext null
            accessToken = token
        }
        snapshot()
    }

    suspend fun snapshot(): RemoteSnapshot = withContext(Dispatchers.IO) {
        val profileJson = get("/users/me")
        val vehiclesJson = get("/vehicles?limit=100")
        val favoritesJson = getArray("/favorites")
        val scansJson = get("/scans?limit=100")
        RemoteSnapshot(
            profile = RemoteProfile(profileJson.optString("name"), profileJson.optString("avatar_url")),
            vehicles = parseVehicles(vehiclesJson.optJSONArray("items") ?: JSONArray()),
            favorites = parseVehicles(favoritesJson),
            scans = parseScans(scansJson.optJSONArray("items") ?: JSONArray())
        )
    }

    suspend fun scan(bitmap: Bitmap): Pair<Vehicle, ScanTelemetry> = withContext(Dispatchers.IO) {
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", "vehicle.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val json = execute(Request.Builder().url("$apiBase/scans").header("Authorization", bearer()).post(multipart).build())
        val vehicleJson = json.optJSONObject("vehicle")
            ?: error("Le véhicule n’a pas pu être identifié.")
        val vehicle = parseVehicle(vehicleJson)
        val telemetry = parseScan(json)
        vehicle to telemetry
    }

    suspend fun setFavorite(vehicleId: String, save: Boolean) = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url("$apiBase/favorites/$vehicleId").header("Authorization", bearer())
        execute(if (save) builder.put(ByteArray(0).toRequestBody(null)).build() else builder.delete().build(), allowEmpty = true)
    }

    fun signOut() { accessToken = null }

    private fun bearer(): String = "Bearer " + (accessToken ?: error("Session expirée. Reconnectez-vous."))

    private fun getArray(path: String): JSONArray {
        val request = Request.Builder().url(apiBase + path).header("Authorization", bearer()).get().build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401) error("Session expirée. Reconnectez-vous.")
            if (!response.isSuccessful) error("Impossible de charger les données.")
            return JSONArray(text)
        }
    }

    private fun get(path: String): JSONObject =
        execute(Request.Builder().url(apiBase + path).header("Authorization", bearer()).get().build())

    private fun execute(request: Request, allowEmpty: Boolean = false): JSONObject {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401) error("Session expirée. Reconnectez-vous.")
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                error(message?.takeIf { it.isNotBlank() } ?: "Impossible de charger les données.")
            }
            return if (text.isBlank() && allowEmpty) JSONObject() else JSONObject(text)
        }
    }

    private fun parseVehicles(array: JSONArray): List<Vehicle> =
        (0 until array.length()).map { parseVehicle(array.getJSONObject(it)) }

    private fun parseVehicle(json: JSONObject): Vehicle {
        val specs = json.optJSONArray("specifications")?.optJSONObject(0)
        fun s(key: String) = specs?.optString(key).orEmpty()
        return Vehicle(
            id = json.getString("id"),
            brand = json.getString("brand"),
            model = json.getString("model"),
            generation = json.optString("generation"),
            year = json.optInt("year"),
            category = json.optString("vehicle_type"),
            engine = s("engine"),
            horsepower = specs?.optInt("horsepower") ?: 0,
            torque = s("torque"),
            transmission = s("transmission"),
            drivetrain = s("drivetrain"),
            fuel = s("fuel_type"),
            fuelConsumption = s("consumption"),
            zeroToSixty = specs?.optDouble("zero_to_sixty") ?: 0.0,
            topSpeedMph = specs?.optInt("top_speed_mph") ?: 0,
            estimatedPrice = 0,
            newPrice = 0,
            usedPrice = 0,
            curbWeightLbs = specs?.optInt("curb_weight_lbs") ?: 0,
            reliabilityScore = 0,
            description = "",
            colorName = "",
            chassisCode = "",
            factoryPackages = emptyList(),
            imageUrl = json.optString("image_url"),
            isSavedInGarage = false,
            lastScannedTimestamp = 0
        )
    }

    private fun parseScans(array: JSONArray): List<ScanTelemetry> =
        (0 until array.length()).map { parseScan(array.getJSONObject(it)) }

    private fun parseScan(json: JSONObject): ScanTelemetry {
        val vehicle = json.optJSONObject("vehicle")
        return ScanTelemetry(
            scanId = json.getString("id"),
            timestamp = 0,
            vehicleName = vehicle?.let { it.optString("brand") + " " + it.optString("model") }?.trim().orEmpty(),
            confidence = json.optDouble("confidence"),
            latencyMs = 0,
            lidarDepth = "",
            trimVerified = "",
            imageUrl = json.optString("image_url"),
            status = json.optString("status")
        )
    }
}



