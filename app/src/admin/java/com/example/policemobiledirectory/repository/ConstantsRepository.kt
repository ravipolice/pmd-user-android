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
import com.example.policemobiledirectory.model.UnitMapping

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
    private val UNIT_MAPPINGS_CACHE_KEY = "unit_mappings_cache"

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

    // =================================================================================
    // NEW: Hybrid Unit-District Mapping Strategy
    // =================================================================================

    // UnitMapping is now imported from com.example.policemobiledirectory.model

    /**
     * Fetch units from Firestore "units" collection
     * Now fetches extended fields: mappingType, mappedDistricts
     */
    private suspend fun fetchUnitsFromFirestore() {
        try {
            Log.d("ConstantsRepository", "🔄 Fetching units (hybrid) from Firestore...")
            val snapshot = firestore.collection("units")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            // 1. Plain list of names for the Unit Dropdown
            val unitNames: List<String> = snapshot.documents
                .mapNotNull { doc -> doc.getString("name") }
                .distinct()
                .sorted()

            // 2. Full Mappings for the District Resolution
            val mappings: Map<String, UnitMapping> = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name")
                if (name != null) {
                    val type = doc.getString("mappingType") ?: "all"
                    
                    // Handle "mappedDistricts" (Legacy)
                    val districtsObj = doc.get("mappedDistricts")
                    val legacyDistricts = when (districtsObj) {
                        is List<*> -> districtsObj.mapNotNull { it?.toString() }
                        else -> emptyList<String>()
                    }

                    // Handle "mappedAreaIds" (New)
                    val areaIdsObj = doc.get("mappedAreaIds")
                    val areaIds = when (areaIdsObj) {
                        is List<*> -> areaIdsObj.mapNotNull { it?.toString() }
                        else -> emptyList<String>()
                    }

                    // Merge both lists
                    val mergedDistricts = (legacyDistricts + areaIds).distinct()

                    val isDistrictLevel = doc.getBoolean("isDistrictLevel") ?: false
                    val isHqLevel = doc.getBoolean("isHqLevel") ?: false
                    
                    val scopesObj = doc.get("scopes")
                    val scopes = when (scopesObj) {
                        is List<*> -> scopesObj.mapNotNull { it?.toString() }
                        else -> emptyList<String>()
                    }

                    val ranksObj = doc.get("applicableRanks")
                    val applicableRanks = when (ranksObj) {
                        is List<*> -> ranksObj.mapNotNull { it?.toString() }
                        else -> emptyList<String>()
                    }

                    val stationKeyword = doc.getString("stationKeyword")
                    val mappedAreaType = doc.getString("mappedAreaType")

                    UnitMapping(name, type, mergedDistricts, isDistrictLevel, isHqLevel, scopes, applicableRanks, stationKeyword, mappedAreaType)
                } else null
            }.associateBy { it.unitName }

            if (unitNames.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val gson = Gson()
                
                // Map to UnitModel for full cache
                val unitModels: List<UnitModel> = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    if (name != null) {
                        val legacyDistricts = (doc.get("mappedDistricts") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        val areaIds = (doc.get("mappedAreaIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        
                        val stationKeyword = doc.getString("stationKeyword") ?: ""

                        UnitModel(
                            id = doc.id,
                            name = name,
                            isActive = doc.getBoolean("isActive") ?: true,
                            mappingType = doc.getString("mappingType") ?: "all",
                            mappedDistricts = (legacyDistricts + areaIds).distinct(),
                            isDistrictLevel = doc.getBoolean("isDistrictLevel") ?: false,
                            isHqLevel = doc.getBoolean("isHqLevel") ?: false,
                            scopes = (doc.get("scopes") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            applicableRanks = (doc.get("applicableRanks") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            stationKeyword = stationKeyword
                        )
                    } else null
                }.sortedBy { it.name }

                // Save plain list
                prefs.edit()
                    .putString(UNITS_CACHE_KEY, gson.toJson(unitNames))
                    .putString(FULL_UNITS_CACHE_KEY, gson.toJson(unitModels))
                    .putString(UNIT_MAPPINGS_CACHE_KEY, gson.toJson(mappings)) // Save mappings
                    .apply()
                
                Log.d("ConstantsRepository", "✅ Fetched ${unitNames.size} units & ${mappings.size} mappings")
            } else {
                Log.w("ConstantsRepository", "⚠️ No active units found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch units from Firestore", e)
        }
    }

    /**
     * Get districts for a specific unit using Hybrid Strategy:
     * 1. Dynamic (Cache)
     * 2. Hardcoded Fallback (Safety Net)
     */
    fun getDistrictsForUnit(unitName: String): List<String> {
        val startTime = System.currentTimeMillis()
        var source = "Unknown"
        var districts: List<String> = emptyList()

        // SCRB Hotfix Removed - Relying on Generic Mapping

        // Step 1: Check Cached Dynamic Mappings (Contains scopes and mapped areas)
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<Map<String, UnitMapping>>() {}.type
                val cachedMappings: Map<String, UnitMapping> = Gson().fromJson(json, type)
                
                val mapping = cachedMappings[unitName]
                if (mapping != null) {
                    source = "Firestore Cache" 
                    
                    // CRITICAL FIX: If unit is HQ-only (no district/battalion/commissionerate scopes),
                    // return all districts + HQ, ignoring any stale mappedDistricts
                    val hasAreaScope = mapping.scopes.contains("district") || 
                                       mapping.scopes.contains("battalion") || 
                                       mapping.scopes.contains("commissionerate") || 
                                       mapping.scopes.contains("district_stations")
                    
                    if (mapping.isHqLevel && !hasAreaScope) {
                        // HQ-only unit: Return all districts + HQ
                        districts = getDistricts() + "HQ"
                        source = "Firestore Cache (HQ-Only)"
                    } else {
                        // A. Start with explicit mapping
                        districts = when (mapping.mappingType) {
                            "subset", "single" -> mapping.mappedDistricts
                            "none" -> listOf("No District Required")
                            "state" -> getDistricts()
                            "all" -> getDistricts()
                            else -> getDistricts()
                        }

                        // B. If it's a State-level unit (HQ or State scope), ensure "HQ" is included
                        if (mapping.scopes.contains("state") || mapping.scopes.contains("hq") || mapping.isHqLevel) {
                            districts = (districts + "HQ").distinct()
                        }
                    }
                } else {
                    source = "Generic Fallback (Cache Miss)"
                    districts = getDistricts() + "HQ"
                }
            } catch (e: Exception) {
                Log.e("ConstantsRepository", "Failed to parse unit mappings", e)
                source = "Generic Fallback (Error)"
                districts = getDistricts() + "HQ"
            }
        } else {
            source = "Generic Fallback (No Cache)"
            districts = getDistricts() + "HQ"
        }

        // Always sort with HQ priority
        val result = sortDistricts(districts.distinct())
        
        Log.d("ConstantsRepository", "🔍 Resolved districts for unit '$unitName': Count=${result.size}, Source=$source, Time=${System.currentTimeMillis() - startTime}ms")
        return result
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
     * PRIORITY: Firestore > Sheet > Hardcoded
     */
    fun getRanks(): List<String> {
        // 1. Firestore
        val firestoreRanks = getCachedList(RANKS_CACHE_KEY, object : TypeToken<List<String>>() {})
        if (firestoreRanks.isNotEmpty()) return firestoreRanks

        // 2. Sheets (Legacy)
        val cached = getCachedData()
        val sheetRanks = cached?.ranks ?: emptyList()
        if (sheetRanks.isNotEmpty()) return sheetRanks

        // 3. Hardcoded Fallback
        return Constants.allRanksList
    }

    /**
     * Get districts from local Constants + Firestore + Google Sheets
     */
    /**
     * Get districts from local Constants + Firestore + Google Sheets
     * PRIORITY: Firestore > Sheet > Hardcoded
     */
    fun getDistricts(): List<String> {
        // 1. Firestore
        val firestoreDistricts = getCachedList(DISTRICTS_CACHE_KEY, object : TypeToken<List<String>>() {})
        val baseList = if (firestoreDistricts.isNotEmpty()) {
            firestoreDistricts
        } else {
            // 2. Sheets (Legacy)
            val cached = getCachedData()
            val sheetDistricts = cached?.districts ?: emptyList()
            if (sheetDistricts.isNotEmpty()) sheetDistricts else Constants.districtsList
        }
        
        // Add HQ and sort with HQ priority
        return sortDistricts((baseList + "HQ").distinct())
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
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<Map<String, UnitMapping>>() {}.type
                val cachedMappings: Map<String, UnitMapping> = Gson().fromJson(json, type)
                return cachedMappings[unitName]?.isDistrictLevel == true
            } catch (e: Exception) {
                Log.e("ConstantsRepository", "Failed to parse unit mappings for check", e)
            }
        }
        return false
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
     * Strategy: Per-District Override (Firestore > Sheets > Hardcoded)
     * If a district exists in Firestore, we use ONLY that source for that district.
     */
    fun getStationsByDistrict(): Map<String, List<String>> {
        val mergedMap = mutableMapOf<String, List<String>>()

        // Sources (Low to High Priority)
        val hardcodedMap = Constants.stationsByDistrictMap
        val sheetsMap = getCachedData()?.stationsbydistrict ?: emptyMap()
        val firestoreMap = getFirestoreStationsMap()

        // 1. Start with Hardcoded
        hardcodedMap.forEach { (k, v) -> mergedMap[k] = v }

        // 2. Override with Sheets (if key exists)
        sheetsMap.forEach { (k, v) -> 
            if (v.isNotEmpty()) mergedMap[k] = v 
        }

        // 3. Override with Firestore (Highest Priority)
        firestoreMap.forEach { (k, v) -> 
            if (v.isNotEmpty()) mergedMap[k] = v 
        }

        return mergedMap
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
    // Helper Methods
    // =================================================================================

    /**
     * Sort districts with HQ priority
     * HQ / Unit HQ comes first, rest alphabetical
     */
    private fun sortDistricts(list: List<String>): List<String> {
        val hqItems = list.filter { it.equals("HQ", ignoreCase = true) || it.equals("UNIT_HQ", ignoreCase = true) }
        val otherItems = list.filterNot { it.equals("HQ", ignoreCase = true) || it.equals("UNIT_HQ", ignoreCase = true) }.sorted()
        return if (hqItems.isNotEmpty()) {
            listOf("HQ") + otherItems
        } else {
            otherItems
        }
    }

    /**
     * Get unit mappings from cache
     */
    private fun getUnitMappings(): Map<String, UnitMapping> {
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null)
        return if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<Map<String, UnitMapping>>() {}.type
                Gson().fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                Log.e("ConstantsRepository", "Error parsing unit mappings", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
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
    suspend fun getSectionsForUnit(unitName: String): List<String> = withContext(Dispatchers.IO) {
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

    /**
     * Get applicable ranks for unit
     */
    fun getApplicableRanksForUnit(unitName: String): List<String> {
        val mapping = getUnitMappings()[unitName]
        return mapping?.applicableRanks ?: emptyList()
    }

    /**
     * Centralized Station Resolution Logic
     * Filters a list of stations based on unit-specific metadata (keywords, scopes)
     */
    fun getStationsForUnit(
        unitName: String,
        baseStations: List<String>
    ): List<String> {
        val mapping = getUnitMappings()[unitName] ?: return baseStations
        
        // Priority: If unit has district scope, show ALL stations for the district (no keyword filter)
        val hasDistrictScope = mapping.scopes.contains("district") ||
                               mapping.scopes.contains("district_stations") ||
                               mapping.isDistrictLevel

        val stationKeyword = mapping.stationKeyword
        if (!stationKeyword.isNullOrBlank() && !hasDistrictScope) {
            val keywords = stationKeyword.split(",").map { it.trim() }.filter { it.isNotBlank() }
            return baseStations.filter { station ->
                keywords.any { k -> station.contains(k, ignoreCase = true) }
            }
        }

        return baseStations
    }
}
