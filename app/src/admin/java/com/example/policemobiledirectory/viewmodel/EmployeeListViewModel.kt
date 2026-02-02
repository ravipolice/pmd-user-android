package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.OfficerRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import com.example.policemobiledirectory.utils.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for employee and officer list operations:
 * - Employee CRUD operations
 * - Employee search and filtering
 * - Officer list management
 * - Combined contacts (employees + officers)
 */
@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val officerRepo: OfficerRepository
) : ViewModel() {

    // Employee State
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _employeeStatus = MutableStateFlow<OperationStatus<List<Employee>>>(OperationStatus.Loading)
    val employeeStatus: StateFlow<OperationStatus<List<Employee>>> = _employeeStatus.asStateFlow()

    // Officers State (read-only contacts)
    private val _officers = MutableStateFlow<List<Officer>>(emptyList())
    val officers: StateFlow<List<Officer>> = _officers.asStateFlow()

    private val _officerStatus = MutableStateFlow<OperationStatus<List<Officer>>>(OperationStatus.Loading)
    val officerStatus: StateFlow<OperationStatus<List<Officer>>> = _officerStatus.asStateFlow()

    // Combined contacts (employees + officers) for unified search
    data class Contact(
        val employee: Employee? = null,
        val officer: Officer? = null
    ) {
        val name: String get() = employee?.name ?: officer?.name ?: ""
        val id: String get() = employee?.kgid ?: officer?.agid ?: ""
        val rank: String? get() = employee?.rank ?: officer?.rank
        val station: String? get() = employee?.station ?: officer?.station
        val district: String? get() = employee?.district ?: officer?.district
        val mobile1: String? get() = employee?.mobile1 ?: officer?.primaryPhone
        val photoUrl: String? get() = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    }

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    private val _debouncedSearchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _selectedDistrict = MutableStateFlow("All")
    private val _selectedStation = MutableStateFlow("All")
    private val _selectedRank = MutableStateFlow("All")
    val selectedRank: StateFlow<String> = _selectedRank.asStateFlow()

    private data class SearchFilters(
        val query: String,
        val filter: SearchFilter,
        val district: String,
        val station: String,
        val rank: String
    )

    private val searchFiltersFlow = combine(
        _debouncedSearchQuery,
        _searchFilter,
        _selectedDistrict,
        _selectedStation,
        _selectedRank
    ) { query, filter, district, station, rank ->
        SearchFilters(query, filter, district, station, rank)
    }

    // Admin state (needed for filtering approved employees)
    private val _isAdmin = MutableStateFlow(false)
    fun setIsAdmin(isAdmin: Boolean) {
        _isAdmin.value = isAdmin
    }

    // Unified Power Search (Room-based)
    private val _powerSearchResults = MutableStateFlow<List<Contact>>(emptyList())

    // Combined contacts with admin-aware filtering
    val allContacts: StateFlow<List<Contact>> = combine(_employees, _officers, _isAdmin) { employees, officers, isAdmin ->
        val filteredEmployees = if (isAdmin) employees else employees.filter { it.isApproved }
        val employeeContacts = filteredEmployees.map { Contact(employee = it) }
        val officerContacts = officers.map { Contact(officer = it) }
        employeeContacts + officerContacts
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * ✅ Unified Power Search Strategy:
     * 1. If query is blank: Use allContacts + dropdown filters.
     * 2. If query present: Search Room (both tables), combine, then apply dropdown filters.
     */
    val filteredContacts: StateFlow<List<Contact>> = combine(
        allContacts,
        _powerSearchResults,
        searchFiltersFlow,
        _isAdmin
    ) { contacts, powerResults, filters, isAdmin ->
        val query = filters.query
        val district = filters.district
        val station = filters.station
        val rank = filters.rank
        
        val sourceList = if (query.isNotBlank()) {
            powerResults
        } else {
            contacts
        }

        if (sourceList.isEmpty() && query.isNotBlank()) {
            // Fallback while Power Search is loading or if it returned nothing
        }

        sourceList.filter { contact ->
            // if Global Search (query present), ignore dropdown filters and search whole DB
            val isGlobalSearch = query.isNotBlank()

            val districtMatch = isGlobalSearch || district == "All" || contact.district?.equals(district, ignoreCase = true) == true
            
            val stationMatch = if (isGlobalSearch || station == "All") {
                true
            } else {
                val isPS = station.endsWith(" PS", ignoreCase = true)
                val stripped = if (isPS) station.dropLast(3).trim() else station
                val circleVariant = "$stripped Circle"
                val contactStation = contact.station?.trim()
                (contactStation?.equals(station, ignoreCase = true) == true) ||
                (isPS && (contactStation?.equals(circleVariant, ignoreCase = true) == true || 
                         contactStation?.equals(stripped, ignoreCase = true) == true)) ||
                ((contactStation.isNullOrBlank() || contactStation.equals("Others", ignoreCase = true)) && 
                 contact.name.contains(stripped, ignoreCase = true))
            }

            val rankMatch = isGlobalSearch || rank == "All" || contact.rank?.equals(rank, ignoreCase = true) == true
            
            districtMatch && stationMatch && rankMatch
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Legacy support for filteredEmployees if still used elsewhere
    val filteredEmployees: StateFlow<List<Employee>> = filteredContacts.map { contacts ->
        contacts.mapNotNull { it.employee }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Power Search Logic
        viewModelScope.launch {
            _debouncedSearchQuery.collect { query ->
                if (query.isBlank()) {
                    _powerSearchResults.value = emptyList()
                    return@collect
                }
                
                // Unified Room Search
                combine(
                    employeeRepo.searchByBlob(query),
                    officerRepo.searchByBlob(query)
                ) { empResult, offResult ->
                    val emps = (empResult as? RepoResult.Success)?.data ?: emptyList()
                    val offs = (offResult as? RepoResult.Success)?.data ?: emptyList()
                    
                    val isAdmin = _isAdmin.value
                    val filteredEmps = if (isAdmin) emps else emps.filter { it.isApproved }
                    
                    val employeeContacts = filteredEmps.map { Contact(employee = it) }
                    val officerContacts = offs.map { Contact(officer = it) }
                    
                    employeeContacts + officerContacts
                }.collect { combined ->
                    _powerSearchResults.value = combined
                }
            }
        }

        // Debounce search query (300ms) to avoid searching on every keystroke
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    _debouncedSearchQuery.value = query
                }
        }
        
        // Initial setup
        refreshEmployees()
        refreshOfficers()
    }

    // =========================================================
    // EMPLOYEE OPERATIONS
    // =========================================================

    fun refreshEmployees() = viewModelScope.launch {
        _employeeStatus.value = OperationStatus.Loading
        try {
            PerformanceLogger.measureDatabaseOperation("employees", "refresh") {
                employeeRepo.refreshEmployees()
                val result = employeeRepo.getEmployees()
                    .filterNot { it is RepoResult.Loading }
                    .firstOrNull()
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _employees.value = list
                        _employeeStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> _employeeStatus.value = OperationStatus.Error(result.message ?: "Failed to load employees")
                    else -> _employeeStatus.value = OperationStatus.Error("Failed to load employees")
                }
            }
        } catch (e: Exception) {
            _employeeStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect {
            refreshEmployees()
        }
    }

    fun deleteEmployee(kgid: String) = viewModelScope.launch {
        Log.d("DeleteEmployee", "Deleting employee $kgid...")
        employeeRepo.deleteEmployee(kgid).collect {
            refreshEmployees()
        }
    }

    // =========================================================
    // OFFICER OPERATIONS
    // =========================================================

    fun refreshOfficers() = viewModelScope.launch {
        _officerStatus.value = OperationStatus.Loading
        try {
            // First sync from Firebase to Room
            officerRepo.syncAllOfficers()
            
            // Then observe Room via Repo
            officerRepo.getOfficers().collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _officers.value = list
                        _officerStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> {
                        _officerStatus.value = OperationStatus.Error(result.message ?: "Failed to load officers")
                    }
                    is RepoResult.Loading -> {
                        _officerStatus.value = OperationStatus.Loading
                    }
                }
            }
        } catch (e: Exception) {
            _officerStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    // =========================================================
    // SEARCH AND FILTER OPERATIONS
    // =========================================================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun updateSelectedDistrict(district: String) {
        _selectedDistrict.value = district
    }

    fun updateSelectedStation(station: String) {
        _selectedStation.value = station
    }

    fun updateSelectedRank(rank: String) {
        _selectedRank.value = rank
    }

    // =========================================================
    // HELPER FUNCTIONS
    // =========================================================

    // Optimized matching function (query is already lowercase)
    private fun Employee.matchesOptimized(queryLower: String, filter: SearchFilter): Boolean {
        return when (filter) {
            SearchFilter.ALL -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower) ||
                kgid.lowercase().contains(queryLower) ||
                mobile1?.contains(queryLower) == true || mobile2?.contains(queryLower) == true ||
                station?.lowercase()?.contains(queryLower) == true ||
                rank?.lowercase()?.contains(queryLower) == true ||
                metalNumber?.lowercase()?.contains(queryLower) == true ||
                bloodGroup?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.NAME -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            SearchFilter.KGID -> {
                val kgidLower = kgid.lowercase()
                kgidLower.startsWith(queryLower) || kgidLower.contains(queryLower)
            }
            SearchFilter.MOBILE -> {
                mobile1?.contains(queryLower) == true || mobile2?.contains(queryLower) == true
            }
            SearchFilter.STATION -> {
                station?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.RANK -> {
                rank?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.METAL_NUMBER -> {
                metalNumber?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.BLOOD_GROUP -> {
                bloodGroup?.lowercase()?.contains(queryLower) == true
            }
        }
    }

}
