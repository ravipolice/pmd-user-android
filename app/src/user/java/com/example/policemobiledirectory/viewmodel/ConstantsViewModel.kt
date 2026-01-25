package com.example.policemobiledirectory.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.policemobiledirectory.repository.ConstantsRepository
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.utils.OperationStatus

/**
 * ViewModel to expose constants from ConstantsRepository
 * This allows UI components to reactively observe constants changes
 */
@HiltViewModel
class ConstantsViewModel @Inject constructor(
    private val constantsRepository: ConstantsRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("constants_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val UNITS_CACHE_KEY = "units_cache"

    private val _ranks = MutableStateFlow(constantsRepository.getRanks())
    val ranks: StateFlow<List<String>> = _ranks.asStateFlow()

    private val _districts = MutableStateFlow<List<String>>(emptyList())
    val districts: StateFlow<List<String>> = _districts.asStateFlow()

    private val _units = MutableStateFlow<List<String>>(emptyList())
    val units: StateFlow<List<String>> = _units.asStateFlow()

    private val _fullUnits = MutableStateFlow<List<com.example.policemobiledirectory.model.UnitModel>>(emptyList())
    val fullUnits: StateFlow<List<com.example.policemobiledirectory.model.UnitModel>> = _fullUnits.asStateFlow()

    private val _stationsByDistrict = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val stationsByDistrict: StateFlow<Map<String, List<String>>> = _stationsByDistrict.asStateFlow()

    private val _bloodGroups = MutableStateFlow(constantsRepository.getBloodGroups())
    val bloodGroups: StateFlow<List<String>> = _bloodGroups.asStateFlow()

    private val _ranksRequiringMetalNumber = MutableStateFlow(constantsRepository.getRanksRequiringMetalNumber())
    val ranksRequiringMetalNumber: StateFlow<List<String>> = _ranksRequiringMetalNumber.asStateFlow()

    private val _ministerialRanks = MutableStateFlow(Constants.ministerialRanks.toList())
    val ministerialRanks: StateFlow<List<String>> = _ministerialRanks.asStateFlow()

    private val _policeStationRanks = MutableStateFlow(Constants.policeStationRanks)
    val policeStationRanks: StateFlow<Set<String>> = _policeStationRanks.asStateFlow()

    private val _highRankingOfficers = MutableStateFlow(Constants.highRankingOfficers)
    val highRankingOfficers: StateFlow<Set<String>> = _highRankingOfficers.asStateFlow()

    private val _ksrpBattalions = MutableStateFlow(Constants.ksrpBattalions)
    val ksrpBattalions: StateFlow<List<String>> = _ksrpBattalions.asStateFlow()

    // Loading and error states for refresh operation
    private val _refreshStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val refreshStatus: StateFlow<OperationStatus<String>> = _refreshStatus.asStateFlow()

    init {
        // Refresh constants if cache is expired
        viewModelScope.launch {
            if (constantsRepository.shouldRefreshCache()) {
                constantsRepository.refreshConstants()
            }
            // Update StateFlows after potential refresh
            refreshConstants()
        }
    }

    /**
     * Check if a unit is District Level (No Station Required)
     */
    suspend fun isDistrictLevelUnit(unitName: String): Boolean {
        return constantsRepository.isDistrictLevelUnit(unitName)
    }

    /**
     * Load constants and check version
     * Shows Toast if server version doesn't match local version
     */
    fun loadConstants() {
        viewModelScope.launch {
            val response = constantsRepository.fetchConstants()
            if (response != null) {
                // Version check - show Toast if mismatch
                if (response.version != Constants.LOCAL_CONSTANTS_VERSION) {
                    Toast.makeText(
                        appContext,
                        "New constants available. Please Sync.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                // Update StateFlows with fetched data (cache was updated by fetchConstants)
                refreshConstants()
            } else {
                // If fetch failed, use cached data
                refreshConstants()
            }
        }
    }
    
    /**
     * Refresh constants from repository and update StateFlows
     */
    /**
     * Refresh constants from repository and update StateFlows
     */
    private suspend fun refreshConstants() {
        _districts.value = constantsRepository.getDistricts()
        _units.value = constantsRepository.getUnits()
        _fullUnits.value = constantsRepository.getFullUnits()
        _stationsByDistrict.value = constantsRepository.getStationsByDistrict()
        _ranks.value = constantsRepository.getRanks()
        _bloodGroups.value = constantsRepository.getBloodGroups()
        _ranksRequiringMetalNumber.value = constantsRepository.getRanksRequiringMetalNumber()
    }

    /**
     * Force refresh constants from API with loading/error states
     */
    fun forceRefresh() {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            try {
                val success = constantsRepository.refreshConstants()
                if (success) {
                    refreshConstants() // Update StateFlows with new data
                    val stationsCount = _stationsByDistrict.value.values.sumOf { it.size }
                    _refreshStatus.value = OperationStatus.Success("Constants refreshed successfully. Total stations: $stationsCount")
                } else {
                    _refreshStatus.value = OperationStatus.Error("Failed to refresh constants. Using cached data.")
                }
            } catch (e: Exception) {
                _refreshStatus.value = OperationStatus.Error("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Clear cache and force refresh from API (bypasses 30-day cache expiration)
     */
    fun clearCacheAndRefresh() {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            try {
                constantsRepository.clearCache() // Clear cache first
                val success = constantsRepository.refreshConstants() // Force fetch from API
                if (success) {
                    refreshConstants() // Update StateFlows with new data
                    val stationsCount = _stationsByDistrict.value.values.sumOf { it.size }
                    _refreshStatus.value = OperationStatus.Success("Cache cleared and constants refreshed. Total stations: $stationsCount")
                } else {
                    _refreshStatus.value = OperationStatus.Error("Failed to refresh constants from API.")
                }
            } catch (e: Exception) {
                _refreshStatus.value = OperationStatus.Error("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Reset refresh status to Idle
     */
    fun resetRefreshStatus() {
        _refreshStatus.value = OperationStatus.Idle
    }

    /**
     * Force refresh units only from Firestore (bypasses cache)
     */
    fun forceRefreshUnits() {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            try {
                constantsRepository.clearCache() // Simple way to force refresh from Firestore on next pull
                val success = constantsRepository.refreshConstants()
                if (success) {
                    refreshConstants()
                    _refreshStatus.value = OperationStatus.Success("Units refreshed successfully.")
                } else {
                    _refreshStatus.value = OperationStatus.Error("Failed to refresh units.")
                }
            } catch (e: Exception) {
                _refreshStatus.value = OperationStatus.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get districts for a specific unit (Hybrid Strategy)
     */
    fun getDistrictsForUnit(unitName: String): List<String> {
        return constantsRepository.getDistrictsForUnit(unitName)
    }

    /**
     * Get sections for a specific unit (e.g. State INT)
     */
    suspend fun getSectionsForUnit(unitName: String): List<String> {
        return constantsRepository.getUnitSections(unitName)
    }
}




