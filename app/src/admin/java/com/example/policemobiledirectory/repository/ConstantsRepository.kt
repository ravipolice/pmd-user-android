package com.example.policemobiledirectory.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.api.ConstantsData
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import com.example.policemobiledirectory.model.UnitModel

/**
 * ConstantsRepository - Manages dynamic constants synchronization
 * 
 * Features:
 * - Fetches constants from Google Sheets via Apps Script API
 * - Fetches units dynamically from Firestore
 * - Caches constants locally in SharedPreferences
 * - 30-day cache expiration (configurable)
 * - Automatic fallback to hardcoded Constants if cache is empty or expired
 * - Non-blocking background refresh
 */
@Singleton
class ConstantsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ConstantsApiService,
    private val securityConfig: SecurityConfig,
    private val firestore: FirebaseFirestore
) {

    private val prefs = context.getSharedPreferences("constants_cache", Context.MODE_PRIVATE)
    
    // Cache expiration: 1 hour (reduced from 30 days to ensure updates appear)
    private val CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1)
    private val CACHE_KEY = "remote_constants"
    private val CACHE_TIMESTAMP_KEY = "cache_timestamp"
    private val UNITS_CACHE_KEY = "units_cache"
    private val FULL_UNITS_CACHE_KEY = "full_units_cache"
    private val RANKS_CACHE_KEY = "ranks_cache"
    private val STATIONS_CACHE_KEY = "stations_cache"
    private val DISTRICTS_CACHE_KEY = "districts_cache"

    /**
     * Check if cache needs refresh (expired or doesn't exist)
     */
    fun shouldRefreshCache(): Boolean {
        // 1. Version Check - Invalidate cache if app update changed constants
        val cachedVersion = prefs.getInt("local_constants_version", 0)
        if (cachedVersion != Constants.LOCAL_CONSTANTS_VERSION) {
            Log.d("ConstantsRepository", "⚠️ Local constants version mismatch (Cached: $cachedVersion, Current: ${Constants.LOCAL_CONSTANTS_VERSION}). Invalidating cache.")
            clearCache()
            prefs.edit().putInt("local_constants_version", Constants.LOCAL_CONSTANTS_VERSION).apply()
            return true
        }

        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return true // No cache exists
        
        // Force refresh if Stations cache is missing (new feature)
        if (!prefs.contains(STATIONS_CACHE_KEY)) return true
        
        val now = System.currentTimeMillis()
        val age = now - timestamp
        return age >= CACHE_EXPIRY_MS
    }

    /**
     * Clear the cache (force next refresh to fetch from API)
     */
    fun clearCache() {
        prefs.edit()
            .remove(CACHE_KEY)
            .remove(CACHE_TIMESTAMP_KEY)
            .remove(UNITS_CACHE_KEY)
            .remove(FULL_UNITS_CACHE_KEY)
            .remove(RANKS_CACHE_KEY)
            .remove(STATIONS_CACHE_KEY)
            .remove(DISTRICTS_CACHE_KEY)
            .apply()
        Log.d("ConstantsRepository", "✅ Cache cleared - next refresh will fetch from API")
    }

    /**
     * Fetch remote constants and cache locally
     * Returns true if successful, false otherwise
     * Checks version and shows Toast if server version doesn't match local version
     */
    suspend fun refreshConstants(): Boolean = withContext(Dispatchers.IO) {
        var firestoreSuccess = false
        
        try {
            // 1. Fetch Google Sheet Constants (LEGACY - DISABLED)
            /*
            try {
                val response = apiService.getConstants(token = securityConfig.getSecretToken())
                
                if (response.success && response.data != null) {
                    // Check version
                    val serverVersion = response.data.version
                    if (serverVersion != Constants.LOCAL_CONSTANTS_VERSION) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "New constant update available. Please Sync.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Log.d("ConstantsRepository", "⚠️ Version mismatch: Server=$serverVersion, Local=${Constants.LOCAL_CONSTANTS_VERSION}")
                    }
                    
                    val json = Gson().toJson(response.data)
                    val now = System.currentTimeMillis()
                    
                    prefs.edit()
                        .putString(CACHE_KEY, json)
                        .putLong(CACHE_TIMESTAMP_KEY, now)
                        .apply()
                    
                    Log.d("ConstantsRepository", "✅ Constants refreshed from Google Sheet. Last updated: ${response.data.lastupdated}")
                    sheetSuccess = true
                } else {
                    Log.e("ConstantsRepository", "❌ API returned success=false or null data")
                }
            } catch (e: Exception) {
                Log.e("ConstantsRepository", "⚠️ Google Sheet sync failed (non-fatal): ${e.message}")
            }
            */
            Log.d("ConstantsRepository", "ℹ️ Google Sheets sync disabled. Fetching from Firestore only.")

            // 2. Fetch Units from Firestore
            fetchUnitsFromFirestore()
            
            // 3. Fetch Ranks from Firestore
            fetchRanksFromFirestore()

            // 4. Fetch Districts from Firestore
            // 4. Fetch Districts from Firestore
            fetchDistrictsFromFirestore()
            
            // 5. Fetch Stations from Firestore
            fetchStationsFromFirestore()
            
            // If we reached here without crashing, Firestore operations likely succeeded
            firestoreSuccess = true

        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch constants: ${e.message}", e)
            return@withContext false
        }
        
        // Return true if Firestore succeeded
        return@withContext firestoreSuccess
    }

    /**
     * Fetch units from Firestore "units" collection
     */
    /**
     * Fetch units from Firestore "units" collection
     */
    private suspend fun fetchUnitsFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching units from Firestore...")
            val snapshot = firestore.collection("units")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            // Map to UnitModel
            val unitModels = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name")
                if (name != null) {
                    UnitModel(
                        id = doc.id,
                        name = name,
                        isActive = doc.getBoolean("isActive") ?: true,
                        mappingType = doc.getString("mappingType") ?: "all",
                        mappedDistricts = (doc.get("mappedDistricts") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        isDistrictLevel = doc.getBoolean("isDistrictLevel") ?: false,
                        scopes = (doc.get("scopes") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    )
                } else null
            }.sortedBy { it.name }

            if (unitModels.isNotEmpty()) {
                val json = Gson().toJson(unitModels)
                prefs.edit().putString(FULL_UNITS_CACHE_KEY, json).apply()
                
                // Also cache legacy string list for compatibility
                val names = unitModels.map { it.name }.distinct()
                val namesJson = Gson().toJson(names)
                prefs.edit().putString(UNITS_CACHE_KEY, namesJson).apply()
                
                Log.d("ConstantsRepository", "✅ Fetched ${unitModels.size} units from Firestore")
            } else {
                Log.w("ConstantsRepository", "⚠️ No active units found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch units from Firestore", e)
        }
    }

    /**
     * Fetch Ranks from Firestore "rankMaster" collection
     */
    private suspend fun fetchRanksFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching ranks from Firestore...")
            val snapshot = firestore.collection("rankMaster")
                .whereEqualTo("isActive", true)
                //.orderBy("seniority_order") // Requires index, do client-side sort if needed
                .get()
                .await()

            // Map documents to Rank objects to sort
            val ranks = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("rank_id") ?: doc.id
                val order = doc.getLong("seniority_order")?.toInt() ?: 999
                val label = doc.getString("rank_label") ?: id
                Triple(id, order, label)
            }
            
            // Sort by seniority order
            val sortedRankIds = ranks.sortedBy { it.second }.map { it.first }

            if (sortedRankIds.isNotEmpty()) {
                val json = Gson().toJson(sortedRankIds)
                prefs.edit().putString(RANKS_CACHE_KEY, json).apply()
                Log.d("ConstantsRepository", "✅ Fetched ${sortedRankIds.size} ranks from Firestore")
                // Log top 5 for debug
                Log.d("ConstantsRepository", "   Top 5 ranks: ${sortedRankIds.take(5).joinToString(", ")}")
            } else {
                Log.w("ConstantsRepository", "⚠️ No active ranks found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch ranks from Firestore", e)
        }
    }

    /**
     * Fetch districts from Firestore "districts" collection
     */
    private suspend fun fetchDistrictsFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching districts from Firestore...")
            val snapshot = firestore.collection("districts")
                //.whereEqualTo("isActive", true)
                .get()
                .await()

            val districtNames = snapshot.documents
                .mapNotNull { it.getString("name") }
                .distinct()
                .sorted()

            if (districtNames.isNotEmpty()) {
                val json = Gson().toJson(districtNames)
                prefs.edit().putString(DISTRICTS_CACHE_KEY, json).apply()
                Log.d("ConstantsRepository", "✅ Fetched ${districtNames.size} districts from Firestore")
            } else {
                Log.w("ConstantsRepository", "⚠️ No districts found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch districts from Firestore", e)
        }
    }
    /**
     * Fetch stations from Firestore "stations" collection
     */
    private suspend fun fetchStationsFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching stations from Firestore...")
            val snapshot = firestore.collection("stations")
                .get()
                .await()

            // Group by district
            val stationMap = mutableMapOf<String, MutableList<String>>()
            
            snapshot.documents.forEach { doc ->
                val name = doc.getString("name")
                val district = doc.getString("district")
                
                if (!name.isNullOrBlank() && !district.isNullOrBlank()) {
                    val list = stationMap.getOrPut(district) { mutableListOf() }
                    list.add(name)
                }
            }

            if (stationMap.isNotEmpty()) {
                val json = Gson().toJson(stationMap)
                prefs.edit().putString(STATIONS_CACHE_KEY, json).apply()
                Log.d("ConstantsRepository", "✅ Fetched stations for ${stationMap.size} districts from Firestore")
            } else {
                Log.w("ConstantsRepository", "⚠️ No stations found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch stations from Firestore", e)
        }
    }
    
    /**
     * Fetch constants from API (alias for refreshConstants with explicit version checking)
     * This method is kept for backward compatibility and explicit version checking
     */
    suspend fun fetchConstants(): ConstantsData? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConstants(token = securityConfig.getSecretToken())
            
            if (response.success && response.data != null) {
                val serverVersion = response.data.version
                if (serverVersion != Constants.LOCAL_CONSTANTS_VERSION) {
                    Log.d("ConstantsRepository", "⚠️ Version mismatch: Server=$serverVersion, Local=${Constants.LOCAL_CONSTANTS_VERSION}")
                }
                // Also trigger unit & rank fetch in background when this is called
                fetchUnitsFromFirestore()
                fetchRanksFromFirestore()
                fetchStationsFromFirestore()
                
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch constants: ${e.message}", e)
            null
        }
    }

    /**
     * Get cached constants data, or null if cache is empty/invalid
     */
    private fun getCachedData(): ConstantsData? {
        val json = prefs.getString(CACHE_KEY, null)
        if (json.isNullOrEmpty()) return null

        return try {
            Gson().fromJson(json, ConstantsData::class.java)
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "Failed to parse cached constants", e)
            null
        }
    }

    /**
     * Generic helper to parse cached lists
     */
    private fun <T> getCachedList(key: String, typeToken: TypeToken<List<T>>): List<T> {
        val json = prefs.getString(key, null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            Gson().fromJson(json, typeToken.type)
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "Failed to parse cached list for key: $key", e)
            emptyList()
        }
    }

    /**
     * Get ranks from local Constants + Firestore + Google Sheets
     */
    /**
     * Get ranks from local Constants + Firestore + Google Sheets
     */
    fun getRanks(): List<String> {
        val cached = getCachedData()
        val sheetRanks = cached?.ranks ?: emptyList()
        val hardcodedRanks = Constants.allRanksList
        
        // Get Firestore ranks
        val firestoreRanks = getCachedList(RANKS_CACHE_KEY, object : TypeToken<List<String>>() {})

        // Merge: Firestore > Sheet > Hardcoded
        val combined = (firestoreRanks + sheetRanks + hardcodedRanks).distinct()
        return if (combined.isNotEmpty()) combined else hardcodedRanks
    }

    /**
     * Get districts from local Constants + Firestore + Google Sheets
     */
    /**
     * Get districts from local Constants + Firestore + Google Sheets
     */
    fun getDistricts(): List<String> {
        val cached = getCachedData()
        val sheetDistricts = cached?.districts ?: emptyList()
        val hardcodedDistricts = Constants.districtsList
        
        // Get Firestore districts
        val firestoreDistricts = getCachedList(DISTRICTS_CACHE_KEY, object : TypeToken<List<String>>() {})

        // Merge: Firestore > Sheet > Hardcoded
        return (firestoreDistricts + sheetDistricts + hardcodedDistricts).distinct().sorted()
    }

    /**
     * Get units with fallback to hardcoded constants
     * Now attempts to load from Firestore cache first
     */
    /**
     * Get full Unit models
     */
    /**
     * Get full Unit models
     */
    fun getFullUnits(): List<UnitModel> {
        val type = object : TypeToken<List<UnitModel>>() {}.type
        return getCachedList(FULL_UNITS_CACHE_KEY, object : TypeToken<List<UnitModel>>() {})
    }

    /**
     * Get units with fallback to hardcoded constants
     * Now attempts to load from Full Unit Model Cache first
     */
    fun getUnits(): List<String> {
        // Try full models first
        val full = getFullUnits()
        if (full.isNotEmpty()) return full.map { it.name }

        // Fallback to legacy string cache
        val cachedUnits = getCachedList(UNITS_CACHE_KEY, object : TypeToken<List<String>>() {})
        if (cachedUnits.isNotEmpty()) return cachedUnits
        
        return Constants.defaultUnitsList
    }

    fun isDistrictLevelUnit(unitName: String): Boolean {
        // Use full units cache to check district level status
        val fullUnits = getFullUnits()
        return fullUnits.find { it.name == unitName }?.isDistrictLevel == true
    }

    /**
     * Get stations by district with fallback to hardcoded constants
     * ✅ CRITICAL FIX: Always use hardcoded stations (which includes all common units) as the base.
     * If API/cached data exists, merge it with hardcoded to add any new stations, but always keep
     * all hardcoded stations (including common units) in the result.
     * ✅ FIXED: Case-insensitive district matching to handle mismatches between API and hardcoded names.
     */
    /**
     * Get stations by district with robust merging (Hardcoded + API + Firestore)
     * Strategy: Collect all -> Normalize keys -> Merge sets -> Build final map
     */
    /**
     * Get stations by district with robust merging (Hardcoded + API + Firestore)
     * Strategy: 3-Pass Merge (Gather -> Normalize -> Build)
     */
    fun getStationsByDistrict(): Map<String, List<String>> {
        // --- PASS 1: GATHER ALL DATA ---
        val hardcodedMap = Constants.stationsByDistrictMap
        val sheetsMap = getCachedData()?.stationsbydistrict ?: emptyMap()
        val firestoreMap = getFirestoreStationsMap()

        // --- PASS 2: MERGE INTO A NORMALIZED, CASE-INSENSITIVE MAP ---
        // Key: Normalized District Name (lowercase, trimmed), Value: Set of Stations
        val mergedStations = mutableMapOf<String, MutableSet<String>>()

        val allMaps = listOf(hardcodedMap, sheetsMap, firestoreMap)
        for (sourceMap in allMaps) {
            sourceMap.forEach { (district, stations) ->
                val normalizedKey = district.trim().lowercase()
                mergedStations.getOrPut(normalizedKey) { mutableSetOf() }.addAll(stations)
            }
        }

        // --- PASS 3: BUILD THE FINAL, CASE-SENSITIVE MAP ---
        // Use definitive district names from all sources + Constants.districtsList
        val allDistrictNames = (Constants.districtsList + hardcodedMap.keys + sheetsMap.keys + firestoreMap.keys)
            .filter { it.isNotBlank() }
            .distinctBy { it.trim().lowercase() }
            .sorted()

        val finalMap = mutableMapOf<String, List<String>>()

        allDistrictNames.forEach { districtName ->
            val normalizedKey = districtName.trim().lowercase()
            val stations = mergedStations[normalizedKey] ?: emptySet()
            // Ensure the district name itself is always an option and sort the final list
            val cleanedStations = (stations + districtName).filter { it.isNotBlank() }.distinct().sorted()
            
            finalMap[districtName] = cleanedStations
        }
        
        Log.d("ConstantsRepository", "📋 Final merged map has ${finalMap.size} districts")
        return finalMap
    }

    /**
     * Helper to parse Firestore stations from cache
     */
    private fun getFirestoreStationsMap(): Map<String, List<String>> {
        val json = prefs.getString(STATIONS_CACHE_KEY, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "Failed to parse Firestore stations cache", e)
            emptyMap()
        }
    }

    /**
     * Get blood groups with fallback to hardcoded constants
     */
    fun getBloodGroups(): List<String> {
        val cached = getCachedData()
        return cached?.bloodgroups?.sorted() ?: Constants.bloodGroupsList
    }

    /**
     * Get ranks requiring metal number (still uses hardcoded list)
     * This could be extended to support dynamic configuration in the future
     */
    fun getRanksRequiringMetalNumber(): List<String> {
        return Constants.ranksRequiringMetalNumber.toList()
    }

    /**
     * Get cache age in days, or null if no cache exists
     */
    fun getCacheAgeDays(): Long? {
        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return null
        
        val age = System.currentTimeMillis() - timestamp
        return TimeUnit.MILLISECONDS.toDays(age)
    }

    // =================================================================================
    // NEW: CRUD Operations for Managing Resources (Districts, Stations, Units)
    // These methods interact directly with Firestore and invalidate the cache
    // =================================================================================

    // --- DISTRICTS ---
    suspend fun addDistrict(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val district = mapOf("name" to name.trim())
            // Use same ID as name for easy deduplication
            firestore.collection("districts").document(name.trim())
                .set(district)
                .await()
            clearCache() // Force refresh
            Result.success("District '$name' added successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDistrict(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("districts").document(name.trim())
                .delete()
                .await()
            clearCache() // Force refresh
            fetchDistrictsFromFirestore()
            Result.success("District '$name' deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- STATIONS ---
    suspend fun addStation(district: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val station = mapOf(
                "name" to name.trim(),
                "district" to district.trim()
            )
            // Composite ID: "District_StationName"
            val id = "${district.trim()}_${name.trim()}"
            firestore.collection("stations").document(id)
                .set(station)
                .await()
            clearCache() // Force refresh
            fetchStationsFromFirestore() // IMPORTANT: Refill cache immediately so UI update works
            Result.success("Station '$name' added to $district")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStation(district: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val id = "${district.trim()}_${name.trim()}"
            firestore.collection("stations").document(id)
                .delete()
                .await()
            clearCache() // Force refresh
            fetchStationsFromFirestore() // IMPORTANT: Refill cache immediately
            Result.success("Station '$name' deleted")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- UNITS ---
    suspend fun addUnit(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val unit = mapOf(
                "name" to name.trim(),
                "isActive" to true
            )
            firestore.collection("units").document(name.trim())
                .set(unit)
                .await()
            clearCache() // Force refresh
            // Also update local units cache immediately
            fetchUnitsFromFirestore() 
            Result.success("Unit '$name' added successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUnit(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("units").document(name.trim())
                .delete()
                .await()
            clearCache()
            fetchUnitsFromFirestore()
            Result.success("Unit '$name' deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // --- UPDATE OPERATIONS (Rename) ---
    /*
     * Since Firestore IDs are the names themselves (or composites of names),
     * renaming effectively means creating a new document and deleting the old one.
     * This changes the ID, so external references would break if they rely on ID.
     * Assuming these are just string constants used in dropdowns, this is acceptable.
     */

    suspend fun updateDistrict(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        if (oldName == newName) return@withContext Result.success("No change")
        try {
            // 1. Create new
            addDistrict(newName).getOrThrow()
            // 2. Delete old
            deleteDistrict(oldName).getOrThrow()
            
            Result.success("Renamed district '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStation(district: String, oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        if (oldName == newName) return@withContext Result.success("No change")
        try {
            // 1. Create new
            addStation(district, newName).getOrThrow()
            // 2. Delete old
            deleteStation(district, oldName).getOrThrow()
            
            Result.success("Renamed station '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUnit(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
         if (oldName == newName) return@withContext Result.success("No change")
        try {
            // 1. Create new
            addUnit(newName).getOrThrow()
            // 2. Delete old
            deleteUnit(oldName).getOrThrow()
            
            Result.success("Renamed unit '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sections for a specific unit (e.g. "State INT")
     * Fixed to match Web Admin schema: Root collection "unit_sections" -> doc(unitName) -> field "sections" (array)
     */
    suspend fun getUnitSections(unitName: String): List<String> = withContext(Dispatchers.IO) {
        try {
            // Check cache or fetch from Firestore "unit_sections" collection
            val docSnapshot = firestore.collection("unit_sections")
                .document(unitName)
                .get()
                .await()
            
            if (docSnapshot.exists()) {
                val sections = docSnapshot.get("sections") as? List<String>
                val sortedSections = sections?.filter { it.isNotBlank() }?.sorted() ?: emptyList()
                Log.d("ConstantsRepository", "Found ${sortedSections.size} sections for unit $unitName from unit_sections")
                sortedSections
            } else {
                Log.d("ConstantsRepository", "No sections found for unit $unitName")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "Error fetching sections for $unitName", e)
            emptyList()
        }
    }
}
