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
import com.example.policemobiledirectory.data.local.EmployeeDao

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
    private val firestore: FirebaseFirestore,
    private val employeeDao: EmployeeDao
) {

    private val prefs = context.getSharedPreferences("constants_cache", Context.MODE_PRIVATE)
    
    // Cache expiration: 1 hour (reduced from 30 days to ensure updates appear)
    private val CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1)
    private val CACHE_KEY = "remote_constants"
    private val CACHE_TIMESTAMP_KEY = "cache_timestamp"
    private val UNITS_CACHE_KEY = "units_cache"
    private val DISTRICTS_CACHE_KEY = "districts_cache"
    private val RANKS_CACHE_KEY = "ranks_cache"

    /**
     * Check if cache needs refresh (expired or doesn't exist)
     */
    fun shouldRefreshCache(): Boolean {
        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return true // No cache exists
        
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
            .remove(RANKS_CACHE_KEY)
            .apply()
        Log.d("ConstantsRepository", "✅ Cache cleared - next refresh will fetch from API")
    }

    /**
     * Fetch remote constants and cache locally
     * Returns true if successful, false otherwise
     * Checks version and shows Toast if server version doesn't match local version
     */
    suspend fun refreshConstants(): Boolean = withContext(Dispatchers.IO) {
        var success = false
        try {
            // 1. Fetch Google Sheet Constants
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
                Log.d("ConstantsRepository", "Stations count by district: ${response.data.stationsbydistrict.mapValues { it.value.size }}")
                success = true
            } else {
                Log.e("ConstantsRepository", "❌ API returned success=false or null data")
            }

            // 2. Fetch Units from Firestore
            fetchUnitsFromFirestore()

            // 3. Fetch Districts from Firestore
            fetchDistrictsFromFirestore()

            // 4. Fetch Ranks from Firestore
            fetchRanksFromFirestore()

        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch constants: ${e.message}", e)
            success = false
        }
        return@withContext success
    }

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

            val unitNames = snapshot.documents
                .mapNotNull { it.getString("name") }
                .distinct()
                .sorted()

            if (unitNames.isNotEmpty()) {
                val json = Gson().toJson(unitNames)
                prefs.edit().putString(UNITS_CACHE_KEY, json).apply()
                Log.d("ConstantsRepository", "✅ Fetched ${unitNames.size} units from Firestore")
            } else {
                Log.w("ConstantsRepository", "⚠️ No active units found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch units from Firestore", e)
        }
    }

    /**
     * Fetch districts from Firestore "districts" collection
     */
    private suspend fun fetchDistrictsFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching districts from Firestore...")
            val snapshot = firestore.collection("districts")
                //.whereEqualTo("isActive", true) // Assuming all in collection are active unless flagged
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
     * Fetch ranks from Firestore "rankMaster" collection
     */
    private suspend fun fetchRanksFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching ranks from Firestore...")
            val snapshot = firestore.collection("rankMaster")
                .whereEqualTo("isActive", true)
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
            } else {
                Log.w("ConstantsRepository", "⚠️ No active ranks found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch ranks from Firestore", e)
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
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "New constant update available. Please Sync.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                // Also trigger unit & rank fetch in background when this is called
                fetchUnitsFromFirestore()
                fetchRanksFromFirestore()
                
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
     * Get ranks from local Constants + Firestore + Google Sheets
     */
    fun getRanks(): List<String> {
        val cached = getCachedData()
        val sheetRanks = cached?.ranks ?: emptyList()
        val hardcodedRanks = Constants.allRanksList
        
        // Get Firestore ranks
        val firestoreRanksJson = prefs.getString(RANKS_CACHE_KEY, null)
        val firestoreRanks = if (!firestoreRanksJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson<List<String>>(firestoreRanksJson, type)
            } catch (e: Exception) {
                emptyList<String>()
            }
        } else {
            emptyList()
        }

        // Merge: Firestore > Sheet > Hardcoded
        val combined = (firestoreRanks + sheetRanks + hardcodedRanks).distinct()
        return if (combined.isNotEmpty()) combined else hardcodedRanks
    }

    /**
     * Get districts from local Constants + Firestore + Google Sheets
     */
    fun getDistricts(): List<String> {
        val cached = getCachedData()
        val sheetDistricts = cached?.districts ?: emptyList()
        val hardcodedDistricts = Constants.districtsList
        
        // Get Firestore districts
        val firestoreDistrictsJson = prefs.getString(DISTRICTS_CACHE_KEY, null)
        val firestoreDistricts = if (!firestoreDistrictsJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson<List<String>>(firestoreDistrictsJson, type)
            } catch (e: Exception) {
                emptyList<String>()
            }
        } else {
            emptyList()
        }

        // Merge: Firestore > Sheet > Hardcoded
        return (firestoreDistricts + sheetDistricts + hardcodedDistricts).distinct().sorted()
    }

    /**
     * Get units with fallback to hardcoded constants
     * Now attempts to load from Firestore cache first
     */
    fun getUnits(): List<String> {
        val json = prefs.getString(UNITS_CACHE_KEY, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                val cachedUnits: List<String> = Gson().fromJson(json, type)
                if (cachedUnits.isNotEmpty()) {
                    return cachedUnits
                }
            } catch (e: Exception) {
                Log.e("ConstantsRepository", "Failed to parse cached units", e)
            }
        }
        return Constants.defaultUnitsList
    }

    /**
     * Get stations by district with fallback to hardcoded constants
     * ✅ CRITICAL FIX: Always use hardcoded stations (which includes all common units) as the base.
     * If API/cached data exists, merge it with hardcoded to add any new stations, but always keep
     * all hardcoded stations (including common units) in the result.
     * ✅ FIXED: Case-insensitive district matching to handle mismatches between API and hardcoded names.
     */
    fun getStationsByDistrict(): Map<String, List<String>> {
        val cached = getCachedData()
        val hardcodedStations = Constants.stationsByDistrictMap
        
        
        Log.d("ConstantsRepository", "🔍 DEBUG: Starting getStationsByDistrict with ${hardcodedStations.size} districts")

        
        // Create case-insensitive lookup for districts
        val districtLookup = hardcodedStations.keys.associateBy { it.lowercase() }
        
        // Always start with hardcoded stations as base (includes all common units for all districts)
        val resultMap = hardcodedStations.mapValues { (district, hardcodedList) ->
            // For each district, start with hardcoded list (includes common units)
            val baseStations = hardcodedList.toMutableList()
            
            // Find matching API district (case-insensitive)
            val matchingApiDistrict = cached?.stationsbydistrict?.keys?.find { 
                it.lowercase().trim() == district.lowercase().trim() 
            }
            
            // If API has data for this district, merge it in
            val apiStations = matchingApiDistrict?.let { 
                val stations = cached.stationsbydistrict[it] ?: emptyList()
                Log.d("ConstantsRepository", "District: $district -> API match: $it, Stations count: ${stations.size}")
                stations
            } ?: emptyList()
            
            if (apiStations.isNotEmpty()) {
                // Add API stations that aren't already in hardcoded list (case-insensitive check)
                apiStations.forEach { apiStation ->
                    val exists = baseStations.any { 
                        it.lowercase().trim() == apiStation.lowercase().trim() 
                    }
                    if (!exists) {
                        baseStations.add(apiStation)
                    }
                }
            } else if (cached != null) {
                // Debug: Check if district exists in API with different spelling
                val apiDistrictKeys = cached.stationsbydistrict.keys
                val possibleMatches = apiDistrictKeys.filter { 
                    it.lowercase().trim().contains(district.lowercase().trim()) || 
                    district.lowercase().trim().contains(it.lowercase().trim())
                }
                if (possibleMatches.isNotEmpty()) {
                    Log.w("ConstantsRepository", "⚠️ District '$district' not found in API, but found possible matches: $possibleMatches")
                }
            }
            

            
            // Ensure district name is included, remove duplicates, sort
            (baseStations + district).distinct().sorted()
        }.toMutableMap()
        
        // Also handle any districts that exist in API but not in hardcoded (case-insensitive check)
        cached?.stationsbydistrict?.forEach { (apiDistrict, apiStations) ->
            val exists = resultMap.keys.any { 
                it.lowercase().trim() == apiDistrict.lowercase().trim() 
            }
            if (!exists) {
                // District exists in API but not in hardcoded - use API data + district name
                resultMap[apiDistrict] = (apiStations + apiDistrict).distinct().sorted()
                Log.d("ConstantsRepository", "Added new district from API: $apiDistrict")
            }
        }
        
        // Final verification: Ensure all hardcoded districts have stations
        hardcodedStations.keys.forEach { district ->
            if (!resultMap.containsKey(district) || resultMap[district].isNullOrEmpty()) {
                Log.w("ConstantsRepository", "⚠️ WARNING: District '$district' has no stations in result! Using hardcoded fallback.")
                val fallbackStations = (hardcodedStations[district]?.toList() ?: emptyList<String>()) + district
                resultMap[district] = fallbackStations.distinct().sorted()
            }
        }
        
        // CRITICAL FIX: Normalize keys to match exactly with Constants.districtsList
        // Use districtsList as the source of truth for exact district names
        val normalizedMap = mutableMapOf<String, List<String>>()
        Constants.districtsList.forEach { exactDistrictName ->
            // Find matching entry in resultMap (case-insensitive)
            val matchingKey = resultMap.keys.find { it.equals(exactDistrictName, ignoreCase = true) }
            val stations = matchingKey?.let { resultMap[it] } 
                ?: (hardcodedStations[exactDistrictName]?.toList() ?: emptyList()) + exactDistrictName
            
            // Ensure we always have stations (at least district name + hardcoded stations)
            normalizedMap[exactDistrictName] = stations.distinct().sorted()
        }
        

        
        // Verify all districts from Constants.districtsList are in the map
        Constants.districtsList.forEach { district ->
            if (!normalizedMap.containsKey(district)) {
                Log.e("ConstantsRepository", "❌ CRITICAL: District '$district' from Constants.districtsList missing from map!")
            }
        }
        
        Log.d("ConstantsRepository", "📋 Final map has ${normalizedMap.size} districts")
        
        return normalizedMap
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
        return Constants.ranksRequiringMetalNumber
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
            fetchDistrictsFromFirestore() // Update local cache specifically for districts
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
}
