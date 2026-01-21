package com.example.policemobiledirectory.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _ranks = MutableStateFlow(constantsRepository.getRanks())
    val ranks: StateFlow<List<String>> = _ranks.asStateFlow()

    private val _districts = MutableStateFlow<List<String>>(emptyList())
    val districts: StateFlow<List<String>> = _districts.asStateFlow()

    private val _units = MutableStateFlow<List<String>>(emptyList())
    val units: StateFlow<List<String>> = _units.asStateFlow()

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
    private fun refreshConstants() {
        _districts.value = constantsRepository.getDistricts()
        _units.value = constantsRepository.getUnits()
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

    // =================================================================================
    // NEW: ViewModel Wrappers for Manage Resources
    // =================================================================================

    fun addDistrict(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.addDistrict(name)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to add district: ${it.message}")
            }
        }
    }

    fun deleteDistrict(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.deleteDistrict(name)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to delete district: ${it.message}")
            }
        }
    }

    fun addStation(district: String, name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.addStation(district, name)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to add station: ${it.message}")
            }
        }
    }

    fun deleteStation(district: String, name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.deleteStation(district, name)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to delete station: ${it.message}")
            }
        }
    }

    fun addUnit(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.addUnit(name)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to add unit: ${it.message}")
            }
        }
    }

    fun deleteUnit(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.deleteUnit(name)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to delete unit: ${it.message}")
            }
        }
    }

    // --- UPDATE (RENAME) WRAPPERS ---

    fun updateDistrict(oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.updateDistrict(oldName, newName)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to rename district: ${it.message}")
            }
        }
    }

    fun updateStation(district: String, oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.updateStation(district, oldName, newName)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to rename station: ${it.message}")
            }
        }
    }

    fun updateUnit(oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = constantsRepository.updateUnit(oldName, newName)
            result.onSuccess {
                refreshConstants()
                _refreshStatus.value = OperationStatus.Success(it)
            }.onFailure {
                _refreshStatus.value = OperationStatus.Error("Failed to rename unit: ${it.message}")
            }
        }
    }
}


