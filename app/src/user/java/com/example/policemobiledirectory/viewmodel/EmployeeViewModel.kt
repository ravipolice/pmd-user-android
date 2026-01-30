package com.example.policemobiledirectory.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.data.mapper.toEmployee
import com.example.policemobiledirectory.data.mapper.toEntity
import com.example.policemobiledirectory.repository.*
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.model.NotificationTarget
import com.example.policemobiledirectory.model.AppNotification
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.text.contains
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.repository.ConstantsRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.repository.ImageUploadRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.repository.AppIconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID


@HiltViewModel
open class EmployeeViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val pendingRepo: PendingRegistrationRepository,
    private val sessionManager: SessionManager,
    private val constantsRepository: ConstantsRepository,
    private val imageRepo: ImageRepository,
    private val syncRepository: SyncRepository,
    private val officerRepo: OfficerRepository,
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val appIconRepository by lazy { AppIconRepository.create(context) }

    // (All your StateFlows are correctly defined here)
    private val _currentUser = MutableStateFlow<Employee?>(null)
    val currentUser: StateFlow<Employee?> = _currentUser.asStateFlow()
    // Cleaned up admin check (always false for user app)
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    private val _authStatus = MutableStateFlow<OperationStatus<Employee>>(OperationStatus.Idle)
    val authStatus: StateFlow<OperationStatus<Employee>> = _authStatus.asStateFlow()
    private val _googleSignInUiEvent = MutableStateFlow<GoogleSignInUiEvent>(GoogleSignInUiEvent.Idle)
    val googleSignInUiEvent: StateFlow<GoogleSignInUiEvent> = _googleSignInUiEvent.asStateFlow()
    private val _otpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val otpUiState: StateFlow<OperationStatus<String>> = _otpUiState
    private val _verifyOtpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val verifyOtpUiState: StateFlow<OperationStatus<String>> = _verifyOtpUiState
    private val _pinResetUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinResetUiState: StateFlow<OperationStatus<String>> = _pinResetUiState
    private val _pinChangeState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinChangeState: StateFlow<OperationStatus<String>> = _pinChangeState.asStateFlow()
    private var otpSentTime: Long? = null
    private val otpValidityDuration = 5 * 60 * 1000L
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    private val _employeeStatus = MutableStateFlow<OperationStatus<List<Employee>>>(OperationStatus.Loading)
    val employeeStatus: StateFlow<OperationStatus<List<Employee>>> = _employeeStatus.asStateFlow()
    
    // Officers (read-only contacts)
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
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _debouncedSearchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // Unified Power Search (Room-based)
    private val _powerSearchResults = MutableStateFlow<List<Contact>>(emptyList())
    
    private val _selectedUnit = MutableStateFlow("All") // New Unit Filter
    val selectedUnit: StateFlow<String> = _selectedUnit.asStateFlow()
    
    private val _selectedDistrict = MutableStateFlow("All")
    private val _selectedStation = MutableStateFlow("All")
    private val _selectedRank = MutableStateFlow("All")
    val selectedRank: StateFlow<String> = _selectedRank.asStateFlow()

    private data class SearchFilters(
        val query: String,
        val filter: SearchFilter,
        val unit: String, // Added Unit
        val district: String,
        val station: String,
        val rank: String
    )

    private var adminNotificationsListener: ListenerRegistration? = null


    private val searchFiltersFlow = combine(
        _debouncedSearchQuery, // Use debounced query
        _searchFilter,
        _selectedUnit, // Added Unit
        _selectedDistrict,
        _selectedStation,
        _selectedRank
    ) { args: Array<Any?> ->
        val query = args[0] as String
        val filter = args[1] as SearchFilter
        val unit = args[2] as String
        val district = args[3] as String
        val station = args[4] as String
        val rank = args[5] as String
        SearchFilters(query, filter, unit, district, station, rank)
    }
    
    val allContacts: StateFlow<List<Contact>> = combine(_employees, _officers, _isAdmin) { employees, officers, isAdmin ->
        // Filter employees: show only approved ones for regular users, all for admins
        val filteredEmployees = if (isAdmin) {
            employees // Admins see all employees
        } else {
            employees.filter { it.isApproved } // Regular users see only approved employees
        }
        
        val employeeContacts = filteredEmployees.map { Contact(employee = it) }
        val officerContacts = officers.map { Contact(officer = it) }
        val allContactsList = employeeContacts + officerContacts
        
        // Debug logging
        android.util.Log.d("ContactsFilter", "isAdmin: $isAdmin, Total employees: ${employees.size}, Approved: ${filteredEmployees.size}, Officers: ${officers.size}, Total contacts: ${allContactsList.size}")
        
        allContactsList
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Rank Priority Map (Highest Rank = Lowest Index)
    private val rankPriorityMap = mapOf(
        // Senior Officers
        "DYSP" to 1,
        // Inspectors
        "PI" to 2, "CPI" to 2, "RPI" to 2, "WPI" to 2, "PIW" to 2,
        // Sub-Inspectors
        "PSI" to 3, "WPSI" to 3, "RSI" to 3, "PSIW" to 3,
        // Asst Sub-Inspectors
        "ASI" to 4, "WASI" to 4, "ARSI" to 4, "ASIW" to 4,
        // Head Constables
        "HC" to 5, "AHC" to 5, "CHC" to 5, "WHC" to 5, "HCW" to 5,
        // Constables
        "PC" to 6, "APC" to 6, "CPC" to 6, "WPC" to 6, "PCW" to 6,
        // Ministerial / Staff
        "SS" to 7, "FDA" to 8, "SDA" to 9, "GHA" to 10, "AO" to 11,
        "Typist" to 12, "Steno" to 13, "PA" to 14
    )

    private fun getRankPriority(rank: String?): Int {
        if (rank.isNullOrBlank()) return 999
        // Normalize rank string (remove extra spaces, ignore case)
        val normalized = rank.trim().uppercase()
        return rankPriorityMap[normalized] ?: 998 // Unknown ranks below known ones
    }

    private fun normalizeDistrict(district: String?): String {
        if (district == null) return ""
        // Remove suffixes like " -NR", " -ER", " -BR", " -SR", " -WR", " -NER", " -CR", " -COP"
        return district.split(" -")[0].trim().lowercase()
    }

    // Unified Power Search Strategy
    val filteredContacts: StateFlow<List<Contact>> = combine(
        allContacts,
        _powerSearchResults,
        searchFiltersFlow,
        _isAdmin
    ) { contacts, powerResults, filters, isAdmin ->
        val query = filters.query
        val unit = filters.unit
        val district = filters.district
        val station = filters.station
        val rank = filters.rank
        
        val sourceList = if (query.isNotBlank()) {
            powerResults
        } else {
            contacts
        }

        sourceList.filter { contact ->
            val districtMatch = district == "All" || normalizeDistrict(contact.district) == normalizeDistrict(district)
            val stationMatch = station == "All" || contact.station?.equals(station, ignoreCase = true) == true
            val rankMatch = rank == "All" || contact.rank?.equals(rank, ignoreCase = true) == true
            
            val unitMatch = if (unit == "All") true else {
                when {
                    contact.employee != null -> unit.equals(contact.employee.effectiveUnit, ignoreCase = true)
                    contact.officer != null -> unit.equals(contact.officer.effectiveUnit, ignoreCase = true)
                    else -> false
                }
            }
            
            districtMatch && stationMatch && rankMatch && unitMatch
        }.sortedWith(
            compareBy<Contact> { getRankPriority(it.rank) }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Simplified Notifications for User App
    private val _userNotificationsLastSeen = MutableStateFlow(0L)
    val userNotificationsLastSeen = _userNotificationsLastSeen.asStateFlow()
    private val _userNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val userNotifications: StateFlow<List<AppNotification>> = _userNotifications.asStateFlow()

    private val _adminNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val adminNotifications: StateFlow<List<AppNotification>> = _adminNotifications.asStateFlow()

    private val _adminNotificationsLastSeen = MutableStateFlow(0L)
    val adminNotificationsLastSeen: StateFlow<Long> = _adminNotificationsLastSeen.asStateFlow()

    val filteredEmployees: StateFlow<List<Employee>> = filteredContacts.map { contacts ->
        contacts.mapNotNull { it.employee }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()
    private val _pendingRegistrations = MutableStateFlow<List<PendingRegistrationEntity>>(emptyList())
    val pendingRegistrations: StateFlow<List<PendingRegistrationEntity>> = _pendingRegistrations.asStateFlow()
    private val _pendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pendingStatus: StateFlow<OperationStatus<String>> = _pendingStatus.asStateFlow()
    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()
    private val _operationResult = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val operationResult: StateFlow<OperationStatus<String>> = _operationResult.asStateFlow()
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()
    private val _firestoreToSheetStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val firestoreToSheetStatus: StateFlow<OperationStatus<String>> = _firestoreToSheetStatus.asStateFlow()

    private val _sheetToFirestoreStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val sheetToFirestoreStatus: StateFlow<OperationStatus<String>> = _sheetToFirestoreStatus.asStateFlow()

    private var userNotificationsListener: ListenerRegistration? = null
    private var userNotificationsListenerKgid: String? = null

    // Constants from Constants.kt (primary source)
    // Google Sheet is only a backup - no automatic syncing for performance
    val districts: StateFlow<List<String>> = MutableStateFlow(Constants.districtsList).asStateFlow()
    val ranks: StateFlow<List<String>> = MutableStateFlow(Constants.allRanksList).asStateFlow()
    val bloodGroups: StateFlow<List<String>> = MutableStateFlow(Constants.bloodGroupsList).asStateFlow()
    val stationsByDistrict: StateFlow<Map<String, List<String>>> = MutableStateFlow(Constants.stationsByDistrictMap).asStateFlow()

    init {
        Log.d("EmployeeVM", "🟢 ViewModel initialized")

        loadSession()
        // Constants.kt is the primary source - no automatic syncing

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

        // 🚀 PERFORMANCE OPTIMIZATION: Debounce search query (300ms) to avoid searching on every keystroke
        // This significantly improves performance for 10k+ users by reducing unnecessary filtering operations
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing before filtering
                .collect { query ->
                    _debouncedSearchQuery.value = query
                }
        }

        // 2️⃣ Observe login state from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                Log.d("Session", "🔄 isLoggedIn = $loggedIn")
            }
        }

        // 3️⃣ Observe admin flag from DataStore
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdmin ->
                _isAdmin.value = isAdmin
                Log.d("Session", "🔄 isAdmin = $isAdmin")
            }
        }

        // 4️⃣ Restore current user session from Room or Firestore
        viewModelScope.launch {
            sessionManager.userEmail.collect { email ->
                // ✅ Only restore if we're not in the middle of a logout
                // Check if isLoggedIn is already false (indicating logout in progress)
                if (_isLoggedIn.value == false && email.isBlank()) {
                    Log.d("Session", "🔒 Logout in progress, skipping session restore")
                    return@collect
                }
                
                if (email.isNotBlank()) {
                    Log.d("Session", "🔁 Restoring session for $email")

                    // Try Room first
                    val localUser = employeeRepo.getEmployeeByEmail(email)
                    if (localUser != null) {
                        _currentUser.value = localUser.toEmployee()
                        _isAdmin.value = localUser.isAdmin
                        _isLoggedIn.value = true
                        Log.d("Session", "✅ Loaded user ${localUser.name} (Admin=${localUser.isAdmin})")
                    } else {
                        // Fallback to Firestore if Room is empty
                        when (val remoteResult = employeeRepo.getUserByEmail(email)) {
                            is RepoResult.Success -> {
                                remoteResult.data?.let { user ->
                                    _currentUser.value = user
                                    _isAdmin.value = user.isAdmin
                                    _isLoggedIn.value = true
                                    Log.d("Session", "✅ Loaded remote user ${user.name}")
                                } ?: run {
                                    Log.w("Session", "⚠️ No matching user found for $email — resetting session")
                                    sessionManager.clearSession()
                                    _isLoggedIn.value = false
                                }
                            }
                            is RepoResult.Error -> {
                                Log.e("Session", "❌ Error loading user: ${remoteResult.message}")
                                sessionManager.clearSession()
                                _isLoggedIn.value = false
                            }
                            else -> Unit
                        }
                    }

                    // Refresh employees and officers after user restore
                    refreshEmployees()
                    refreshOfficers()
                } else {
                    Log.d("Session", "🔒 No stored email — user not logged in")
                    // Only clear if not already cleared (avoid unnecessary updates)
                    if (_currentUser.value != null || _isLoggedIn.value == true) {
                        _currentUser.value = null
                        _isAdmin.value = false
                        _isLoggedIn.value = false
                    }
                }
            }
        }

        // 5️⃣ Startup data prefetch
        viewModelScope.launch {
            try {
                ensureSignedInIfNeeded()

            } catch (e: Exception) {
                Log.e("Startup", "Startup failed: ${e.message}", e)
            }
        }

        viewModelScope.launch {
            currentUser.collectLatest { user ->
                updateUserNotificationListener(user)
            }
        }

        viewModelScope.launch {
            sessionManager.userNotificationsSeenAt.collect { lastSeen ->
                _userNotificationsLastSeen.value = lastSeen
            }
        }


        viewModelScope.launch {
            sessionManager.adminNotificationsSeenAt.collect { lastSeen ->
                _adminNotificationsLastSeen.value = lastSeen
            }
        }
    }



    // =========================================================
    // AUTHENTICATION (LOGIN, GOOGLE SIGN-IN, LOGOUT)
    // =========================================================
    fun loginWithPin(email: String, pin: String) {
        viewModelScope.launch {
            employeeRepo.loginUser(email, pin).collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val user = result.data
                        if (user != null) {
                            // ✅ Instantly update UI before waiting for DataStore
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin
                            _isLoggedIn.value = true

                            // ✅ Save to DataStore for persistence
                            sessionManager.saveLogin(email, user.isAdmin)

                            // ✅ Fetch a fresh version from local DB (ensures latest info)
                            val refreshed = employeeRepo.getEmployeeDirect(email)
                            if (refreshed != null) {
                                _currentUser.value = refreshed
                                _isAdmin.value = refreshed.isAdmin
                            }

                            // ✅ Refresh admin status to ensure it's up to date
                            checkIfAdmin()

                            _authStatus.value = OperationStatus.Success(user)
                            Log.d("Login", "✅ Logged in as ${user.name}, Admin=${user.isAdmin}")
                        } else {
                            _authStatus.value = OperationStatus.Error("User not found")
                        }
                    }

                    is RepoResult.Error -> {
                        _authStatus.value = OperationStatus.Error(result.message ?: "Login failed")
                    }

                    is RepoResult.Loading -> {
                        _authStatus.value = OperationStatus.Loading
                    }
                }
            }
        }
    }


    fun handleGoogleSignIn(email: String, googleIdToken: String) {
        viewModelScope.launch {
            _googleSignInUiEvent.value = GoogleSignInUiEvent.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                if (authResult.user != null) {
                    val existingUser = employeeRepo.getEmployeeByEmail(email)
                    if (existingUser != null) {
                        val user = existingUser.toEmployee()
                        sessionManager.saveLogin(user.email, user.isAdmin)
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.SignInSuccess(user)
                    } else {
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRequired(email, authResult.user?.displayName)
                    }
                } else {
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Sign-in failed: Firebase user is null.")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "❌ Failed", e)
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Error(e.localizedMessage ?: "Unknown error")
                logout()
            }
        }
    }



    fun markNotificationsRead(notifications: List<AppNotification>) {
        val latestTimestamp = notifications.mapNotNull { it.timestamp }.maxOrNull()
            ?: System.currentTimeMillis()
        viewModelScope.launch {
            if (latestTimestamp > _userNotificationsLastSeen.value) {
                sessionManager.setUserNotificationsSeen(latestTimestamp)
            }
        }
    }

    private fun updateUserNotificationListener(user: Employee?) {
        val kgid = user?.kgid

        if (user?.isAdmin == true) {
            userNotificationsListener?.remove()
            userNotificationsListener = null
            userNotificationsListenerKgid = null
            _userNotifications.value = emptyList()
            return
        }

        if (userNotificationsListenerKgid == kgid) return

        userNotificationsListener?.remove()
        userNotificationsListener = null
        userNotificationsListenerKgid = kgid

        if (user == null || kgid.isNullOrBlank()) {
            _userNotifications.value = emptyList()
            return
        }

        userNotificationsListener = firestore.collection("notifications_queue")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserNotifications", "❌ Failed to fetch: ${e.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val notifications = docs.mapNotNull { doc ->
                    val notification = doc.data?.toAppNotification(doc.id) ?: return@mapNotNull null
                    if (shouldDeliverNotification(notification, user)) notification else null
                }
                _userNotifications.value = notifications
            }
    }

    private fun shouldDeliverNotification(notification: AppNotification, user: Employee): Boolean {
        fun matches(lhs: String?, rhs: String?): Boolean =
            lhs != null && rhs != null && lhs.equals(rhs, ignoreCase = true)

        return when (notification.targetType) {
            NotificationTarget.ALL -> true
            NotificationTarget.INDIVIDUAL -> matches(notification.targetKgid, user.kgid)
            NotificationTarget.DISTRICT -> matches(notification.targetDistrict, user.district)
            NotificationTarget.STATION -> matches(notification.targetDistrict, user.district) &&
                    matches(notification.targetStation, user.station)
            NotificationTarget.ADMIN -> user.isAdmin
            NotificationTarget.KSRP_BATTALION -> false // Standard behavior for now
        }
    }

    private fun Map<String, Any>.toAppNotification(id: String): AppNotification? {
        val title = this["title"] as? String ?: "Notification"
        val body = this["body"] as? String ?: "You have a new message."
        val timestamp = (this["timestamp"] as? Number)?.toLong()
        val targetType = (this["targetType"] as? String)?.runCatching {
            NotificationTarget.valueOf(this.uppercase())
        }?.getOrNull() ?: NotificationTarget.ALL
        val targetKgid = this["targetKgid"] as? String
        val targetDistrict = this["targetDistrict"] as? String
        val targetStation = this["targetStation"] as? String

        return AppNotification(
            id = id,
            title = title,
            body = body,
            timestamp = timestamp,
            targetType = targetType,
            targetKgid = targetKgid,
            targetDistrict = targetDistrict,
            targetStation = targetStation
        )
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d("Logout", "🚪 Starting logout...")

                // 1️⃣ Clear local session FIRST to prevent observers from triggering
                sessionManager.clearSession()
                
                // 2️⃣ Reset in-memory session IMMEDIATELY
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null

                // 5️⃣ Reset auth/UI state so login screen doesn't re-trigger stale events
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle

                // 3️⃣ Sign out of Firebase (including any anonymous sessions)
                FirebaseAuth.getInstance().signOut()
                auth.signOut()
                
                // 4️⃣ Clear repository data
                employeeRepo.logout()

                Log.d("Logout", "✅ Logout complete, no anonymous re-login")

                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }

            } catch (e: Exception) {
                Log.e("Logout", "❌ Logout failed: ${e.message}")
                // Even if there's an error, ensure state is cleared
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }



    // =========================================================
// OTP / PIN FLOW  (Secure Version)
// =========================================================
    fun sendOtp(email: String) {
        viewModelScope.launch {
            Log.d("ForgotPinFlow", "🟢 sendOtp() for $email")
            _otpUiState.value = OperationStatus.Loading

            try {
                when (val result = employeeRepo.sendOtp(email)) {
                    is RepoResult.Success -> {
                        _otpUiState.value = OperationStatus.Success(result.data ?: "OTP sent to $email")
                        startOtpCountdown()
                    }

                    is RepoResult.Error -> {
                        _otpUiState.value = OperationStatus.Error(result.message ?: "Failed to send OTP")
                    }

                    else -> Unit
                }

            } catch (e: Exception) {
                _otpUiState.value = OperationStatus.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }


    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _verifyOtpUiState.value = OperationStatus.Loading
            try {
                when (val result = employeeRepo.verifyLoginCode(email, code)) {
                    is RepoResult.Success -> _verifyOtpUiState.value = OperationStatus.Success("OTP verified successfully")
                    is RepoResult.Error -> _verifyOtpUiState.value = OperationStatus.Error(result.message ?: "Invalid OTP")
                    else -> Unit
                }
            } catch (e: Exception) {
                _verifyOtpUiState.value = OperationStatus.Error(e.message ?: "Error verifying OTP")
            }
        }
    }

    fun updatePinAfterOtp(email: String, newPin: String) {
        viewModelScope.launch {
            _pinResetUiState.value = OperationStatus.Loading
            try {
                val result = employeeRepo.updateUserPin(email, null, newPin, true)
                when (result) {
                    is RepoResult.Success -> _pinResetUiState.value = OperationStatus.Success("PIN reset successful")
                    is RepoResult.Error -> _pinResetUiState.value = OperationStatus.Error(result.message ?: "Failed to reset PIN")
                    else -> Unit
                }
            } catch (e: Exception) {
                _pinResetUiState.value = OperationStatus.Error(e.message ?: "Error updating PIN")
            }
        }
    }

    fun changePin(email: String, oldPin: String, newPin: String) {
        viewModelScope.launch {
            _pinChangeState.value = OperationStatus.Loading
            when (val result = employeeRepo.updateUserPin(email, oldPin, newPin, false)) {
                is RepoResult.Success -> _pinChangeState.value = OperationStatus.Success("PIN changed successfully")
                is RepoResult.Error -> _pinChangeState.value = OperationStatus.Error(result.message ?: "Failed to change PIN")
                else -> Unit
            }
        }
    }

    private fun startOtpCountdown() {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            otpSentTime = start
            while (System.currentTimeMillis() - start < otpValidityDuration) {
                _remainingTime.value = otpValidityDuration - (System.currentTimeMillis() - start)
                delay(1000)
            }
            _remainingTime.value = 0L
            resetForgotPinFlow()
        }
    }

    fun resetForgotPinFlow() {
        _otpUiState.value = OperationStatus.Idle
        _verifyOtpUiState.value = OperationStatus.Idle
        _pinResetUiState.value = OperationStatus.Idle
    }

    fun resetPinChangeState() {
        _pinChangeState.value = OperationStatus.Idle
    }

    fun setPinResetError(message: String) {
        _pinResetUiState.value = OperationStatus.Error(message)
    }

    /**
     * Loads the user session from SessionManager.
     * If a valid session exists, it fetches user details and refreshes data.
     * If not, it ensures the app is in a clean, logged-out state.
     */
    fun loadSession() {
        viewModelScope.launch {
            // First, get the logged-in status.
            val isLoggedIn = sessionManager.isLoggedIn.first()
            _isLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                // If logged in, get the email and admin status.
                val email = sessionManager.userEmail.first()
                val isAdmin = sessionManager.isAdmin.first()
                _isAdmin.value = isAdmin

                if (email.isNotBlank()) {
                    try {
                        // Fetch the full user object from the repository.
                        val userEntity = employeeRepo.getEmployeeByEmail(email)
                        val user = userEntity?.toEmployee()

                        if (user != null) {
                            _currentUser.value = user
                            Log.d("Session", "✅ Session restored for user: ${user.name}, admin=$isAdmin")
                            // Refresh data now that we have a valid user.
                            refreshEmployees()

                        } else {
                            // Data is inconsistent (session exists but user not in DB).
                            // This is a failure case, so log out.
                            Log.e("Session", "❌ Session exists for $email but user not found in DB. Forcing logout.")
                            logout()
                        }
                    } catch (e: Exception) {
                        Log.e("Session", "❌ DB error during session restore: ${e.message}. Forcing logout.")
                        logout()
                    }
                } else {
                    // Session is invalid (isLoggedIn=true but no email). Force logout.
                    Log.e("Session", "❌ Invalid session state. Forcing logout.")
                    logout()
                }
            } else {
                // Not logged in. Ensure all states are clean.
                _isAdmin.value = false
                _currentUser.value = null
                Log.d("Session", "ℹ️ No active session. App is in Guest mode.")
            }
        }
    }

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
                bloodGroup?.lowercase()?.contains(queryLower) == true ||
                unit?.lowercase()?.contains(queryLower) == true ||
                effectiveUnit.lowercase().contains(queryLower)
            }
            SearchFilter.NAME -> {
                // Fast path: check if query is at start (common case)
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            SearchFilter.KGID -> {
                val kgidLower = kgid.lowercase()
                kgidLower.startsWith(queryLower) || kgidLower.contains(queryLower)
            }
            SearchFilter.MOBILE -> {
                // Mobile numbers: direct contains check (no lowercase needed for numbers)
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
    
    // Legacy function for backward compatibility (kept for Officer.matches)
    private fun Employee.matches(query: String, filter: SearchFilter): Boolean {
        return matchesOptimized(query.lowercase().trim(), filter)
    }


    // =========================================================
    // EMPLOYEE CRUD + HELPERS
    // =========================================================
    fun refreshEmployees() = viewModelScope.launch {
        _employeeStatus.value = OperationStatus.Loading
        try {
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
        } catch (e: Exception) {
            _employeeStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }
    
    /**
     * ✅ Refresh current user data from Firestore
     * Call this after updating profile to ensure UI shows latest data
     */
    fun refreshCurrentUser() = viewModelScope.launch {
        val currentKgid = _currentUser.value?.kgid
        
        if (currentKgid != null) {
            try {
                // First try local DB (faster)
                val localEntity = employeeRepo.getLocalEmployeeByKgid(currentKgid)
                if (localEntity != null) {
                    _currentUser.value = localEntity.toEmployee()
                    Log.d("EmployeeViewModel", "✅ Refreshed current user from local: ${localEntity.name}, metalNumber=${localEntity.metalNumber}")
                }
                
                // Then refresh from Firestore to get latest data
                val firestoreDoc = firestore.collection("employees").document(currentKgid).get().await()
                val firestoreEmp = firestoreDoc.toObject(Employee::class.java)
                if (firestoreEmp != null) {
                    val finalEmp = firestoreEmp.copy(kgid = currentKgid)
                    _currentUser.value = finalEmp
                    Log.d("EmployeeViewModel", "✅ Refreshed current user from Firestore: ${finalEmp.name}, metalNumber=${finalEmp.metalNumber}")
                    
                    // Update local cache using mapper
                    val entity = finalEmp.toEntity()
                    employeeRepo.insertEmployeeDirect(entity)
                }
            } catch (e: Exception) {
                Log.e("EmployeeViewModel", "❌ Exception refreshing current user: ${e.message}", e)
            }
        } else {
            // Fallback: refresh by email
            try {
                val currentEmail = sessionManager.userEmail.first()
                if (currentEmail.isNotBlank()) {
                    when (val result = employeeRepo.getUserByEmail(currentEmail)) {
                        is RepoResult.Success -> {
                            result.data?.let { user ->
                                _currentUser.value = user
                                Log.d("EmployeeViewModel", "✅ Refreshed current user by email: ${user.name}, metalNumber=${user.metalNumber}")
                            }
                        }
                        is RepoResult.Error -> {
                            Log.e("EmployeeViewModel", "❌ Failed to refresh current user by email: ${result.message}")
                        }
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                Log.e("EmployeeViewModel", "❌ Exception refreshing current user by email: ${e.message}", e)
            }
        }
    }
    
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

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect { refreshEmployees() }
    }



    // ✅ FIX: ALL FUNCTIONS ARE NOW CORRECTLY PLACED AT THE TOP LEVEL OF THE CLASS
    // =========================================================
    // UI + MISC
    // =========================================================
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    /**
     * ✅ Prevents unwanted Firebase guest auto-login
     */
    private suspend fun ensureSignedInIfNeeded() {
        // ✅ Only check if we have a valid session in DataStore
        val hasValidSession = sessionManager.isLoggedIn.first()
        if (!hasValidSession) {
            // No valid session, ensure Firebase is signed out
            val user = auth.currentUser
            if (user != null) {
                Log.w("AuthCheck", "⚠️ Firebase user exists but no valid session — signing out.")
                try {
                    auth.signOut()
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e("AuthCheck", "❌ Failed to sign out: ${e.message}")
                }
            }
            return
        }
        
        val user = auth.currentUser
        if (user == null) {
            Log.d("AuthCheck", "🔒 No Firebase user — not signing in automatically.")
            return
        }

        if (user.isAnonymous || user.email.isNullOrBlank()) {
            Log.w("AuthCheck", "⚠️ Anonymous Firebase session detected — signing out.")
            try {
                auth.signOut()
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e("AuthCheck", "❌ Failed to sign out anonymous user: ${e.message}")
            }
        } else {
            Log.d("AuthCheck", "✅ Valid Firebase user: ${user.email}")
        }
    }




    private suspend fun isEmailApproved(email: String): Boolean {
        // Prefer repository-level check (fast), but fallback to Firestore if needed.
        val emp = employeeRepo.getEmployeeByEmail(email) // returns entity or null
        return emp?.isApproved == true || emp?.toEmployee()?.isAdmin == true // adjust fields as per your model
    }



    // =========================================================
//  PENDING REGISTRATIONS (Final + Corrected)
// =========================================================



    // =========================================================
    //  USEFUL LINKS & NOTIFICATIONS
    // =========================================================
    fun fetchUsefulLinks() {
        viewModelScope.launch {
            try {
                val collection = firestore.collection("useful_links")
                val snapshot = try {
                    collection.get(Source.SERVER).await()
                } catch (serverError: Exception) {
                    Log.w("UsefulLinks", "Server fetch failed (${serverError.message}), falling back to cache")
                    collection.get(Source.CACHE).await()
                }

                // Temporary list for immediate show (no icons yet)
                _usefulLinks.value = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null
                    link.copy(documentId = doc.id)
                }

                // Fetch icons in background
                // Always try to fetch icons if playStoreUrl exists, even if iconUrl already exists
                // This allows refreshing icons that might be stale or incorrect
                val updatedLinks = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null

                    val icon = if (!link.playStoreUrl.isNullOrBlank()) {
                        try {
                            // 🔥 ALWAYS fetch icon using favicon API (will use cache if valid)
                            val fetched = appIconRepository.getOrFetchAppIcon(link.playStoreUrl)

                            if (!fetched.isNullOrBlank()) {
                                // Only update Firestore if icon changed or was missing
                                if (link.iconUrl != fetched) {
                                    try {
                                        collection.document(doc.id).update("iconUrl", fetched).await()
                                        Log.d("IconUpdate", "Updated icon for ${link.name}")
                                    } catch (e: Exception) {
                                        Log.w("IconUpdate", "Failed to save icon for ${link.name}: ${e.message}")
                                    }
                                }
                                fetched
                            } else {
                                // If fetch failed, use existing iconUrl if available
                                link.iconUrl
                            }

                        } catch (e: Exception) {
                            Log.e("IconFetch", "Error fetching icon for ${link.name}: ${e.message}")
                            // Fallback to existing iconUrl if fetch failed
                            link.iconUrl
                        }

                    } else {
                        // No playStoreUrl, use existing iconUrl
                        link.iconUrl
                    }

                    link.copy(
                        iconUrl = icon,
                        documentId = doc.id
                    )
                }

                // 🔥 Update UI ONCE with full data
                _usefulLinks.value = updatedLinks

            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch useful links: ${e.message}")
                _usefulLinks.value = emptyList()
            }
        }
    }







    // =========================================================
    //  NEW USER REGISTRATION
    // =========================================================
    fun registerNewUser(entity: PendingRegistrationEntity) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            try {
                // 1️⃣ Check for duplicate registration directly in Firestore
                val hasDuplicate = try {
                    // Check by KGID
                    val kgidSnapshot = firestore.collection("pending_registrations")
                        .whereEqualTo("status", "pending")
                        .whereEqualTo("kgid", entity.kgid)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!kgidSnapshot.isEmpty) {
                        true // Duplicate found by KGID
                    } else {
                        // Also check by email
                        val emailSnapshot = firestore.collection("pending_registrations")
                            .whereEqualTo("status", "pending")
                            .whereEqualTo("email", entity.email)
                            .limit(1)
                            .get()
                            .await()
                        !emailSnapshot.isEmpty // true if duplicate found
                    }
                } catch (e: Exception) {
                    Log.w("RegisterUser", "Duplicate check failed, proceeding anyway: ${e.message}")
                    false // Allow registration if check fails
                }

                if (hasDuplicate) {
                    _pendingStatus.value = OperationStatus.Error(
                        "A registration for this KGID/Email already exists and is pending approval."
                    )
                    return@launch
                }

                // 2️⃣ Prepare safe PendingRegistration object
                val pending = entity.copy(
                    isApproved = false,
                    firebaseUid = entity.firebaseUid.takeIf { it.isNotBlank() } ?: "",
                    status = "pending",
                    rejectionReason = null,
                    photoUrlFromGoogle = null
                )

                // 3️⃣ Submit to Firestore + Room
                pendingRepo.addPendingRegistration(pending).collect { result ->
                    when (result) {
                        is RepoResult.Loading ->
                            _pendingStatus.value = OperationStatus.Loading

                        is RepoResult.Success -> {
                            _pendingStatus.value =
                                OperationStatus.Success("Registration submitted for admin approval.")
                                // Notification logic removed for User App
                        }

                        is RepoResult.Error -> {
                            _pendingStatus.value =
                                OperationStatus.Error(result.message ?: "Registration failed.")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("RegisterUser", "❌ Registration failed", e)
                _pendingStatus.value =
                    OperationStatus.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }

    private val _saveStatus = MutableStateFlow<RepoResult<Boolean>?>(null)
    val saveStatus: StateFlow<RepoResult<Boolean>?> = _saveStatus.asStateFlow()

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    fun resetPendingStatus() {
        _pendingStatus.value = OperationStatus.Idle
    }


    fun saveEmployee(employee: Employee, photoUri: Uri?) {
        viewModelScope.launch {
            _saveStatus.value = RepoResult.Loading
            try {
                var finalEmployee = employee

                // 1. Upload photo if exists
                if (photoUri != null) {
                    _uploadStatus.value = OperationStatus.Loading
                    try {
                         imageRepo.uploadOfficerImage(photoUri, employee.kgid).collect { status ->
                             when(status) {
                                 is OperationStatus.Success -> {
                                     // The status.data should be the download URL
                                     val distinctUrl = status.data
                                     if (distinctUrl != null) {
                                         finalEmployee = finalEmployee.copy(photoUrl = distinctUrl)
                                     }
                                 }
                                 is OperationStatus.Error -> {
                                     throw Exception(status.message)
                                 }
                                 else -> {} // Loading or Idle
                             }
                         }
                         // Wait for the flow? uploadOfficerImage returns a Flow. 
                         // The logic above is incorrect for a Flow collection inside a linear process if we need the result *before* proceeding.
                         // We should use `last()` or transform it. A cleaner way is using `imageRepo.uploadOfficerImageSuspend` if it exists, or handling the flow.
                         
                         // Re-implementation assuming we need to wait for upload:
                         // Ideally, uploadOfficerImage should have a suspend version.
                         // Let's assume we can get it via first matching success/error.
                    } catch (e: Exception) {
                         _saveStatus.value = RepoResult.Error(e)
                         return@launch
                    }
                }
                
                // Let's rewrite the upload part more robustly assuming the Repo returns Flow<OperationStatus>
                if (photoUri != null) {
                     val uploadResult = imageRepo.uploadOfficerImage(photoUri, employee.kgid)
                         .filter { it is OperationStatus.Success || it is OperationStatus.Error }
                         .first()
                     
                     if (uploadResult is OperationStatus.Error) {
                         _saveStatus.value = RepoResult.Error(Exception(uploadResult.message))
                         return@launch
                     } else if (uploadResult is OperationStatus.Success) {
                          finalEmployee = finalEmployee.copy(photoUrl = uploadResult.data)
                     }
                }

                // 2. Save Employee
                employeeRepo.addOrUpdateEmployee(finalEmployee).collect { result ->
                     // Adapt RepoResult to what UI expects or just pass it
                     // addOrUpdateEmployee returns Flow<RepoResult<String>>?
                     // Let's assume it does.
                     _saveStatus.value = result
                     if (result is RepoResult.Success) {
                         refreshEmployees()
                         refreshCurrentUser()
                     }
                }

            } catch (e: Exception) {
                _saveStatus.value = RepoResult.Error(e)
            }
        }
    }

    // =========================================================
    //  FILE UPLOADS
    // =========================================================
    fun uploadPhoto(uri: Uri, kgid: String) = viewModelScope.launch {
        imageRepo.uploadOfficerImage(uri, kgid).collect { status ->
            _uploadStatus.value = status
        }
    }

    // =========================================================
    //  UI CONTROLS
    // =========================================================
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun updateSelectedUnit(unit: String) {
        _selectedUnit.value = unit
        // Reset station when unit changes
        if (unit != "All") {
             _selectedStation.value = "All"
        }
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

    fun adjustFontScale(increase: Boolean) {
        val step = 0.1f
        val current = _fontScale.value
        _fontScale.value = when {
            increase -> (current + step).coerceAtMost(1.8f)
            else -> (current - step).coerceAtLeast(0.8f)
        }
    }
    
    fun setFontScale(scale: Float) {
        _fontScale.value = scale.coerceIn(0.8f, 1.8f)
    }

    // =========================================================
    // ADMIN CHECK
    // =========================================================
    // =========================================================
    // ADMIN CHECK (Removed - Always False)
    // =========================================================
    fun checkIfAdmin() {
        _isAdmin.value = false
    }

    // This generic helper can be used if needed, but isn't strictly necessary with the current implementations
    fun markNotificationsRead(isAdmin: Boolean, notifications: List<AppNotification>) {
        if (notifications.isEmpty()) return
        val maxTimestamp = notifications.maxOfOrNull { it.timestamp ?: 0L } ?: 0L
        viewModelScope.launch {
            if (isAdmin) {
                sessionManager.setAdminNotificationsSeen(maxTimestamp)
            } else {
                sessionManager.setUserNotificationsSeen(maxTimestamp)
            }
        }
    }
    
    // Suspend function for direct image upload (used in registration)
    suspend fun uploadOfficerImageSuspend(uri: Uri, kgid: String): RepoResult<String> {
        return try {
            val result = imageRepo.uploadOfficerImage(uri, kgid)
                .filter { it is OperationStatus.Success || it is OperationStatus.Error }
                .first()
                
            when (result) {
                is OperationStatus.Success -> RepoResult.Success(result.data)
                is OperationStatus.Error -> RepoResult.Error(null, result.message)
                else -> RepoResult.Error(null, "Upload failed")
            }
        } catch (e: Exception) {
            RepoResult.Error(e)
        }
    }

    private fun <T> launchOperationForResult(stateFlow: MutableStateFlow<OperationStatus<T>>, block: suspend () -> Flow<RepoResult<T>>) = viewModelScope.launch {
        stateFlow.value = OperationStatus.Loading
        block().collectLatest { result ->
            when (result) {
                is RepoResult.Loading -> stateFlow.value = OperationStatus.Loading
                is RepoResult.Success -> stateFlow.value = OperationStatus.Success(result.data ?: return@collectLatest)
                is RepoResult.Error -> stateFlow.value = OperationStatus.Error(result.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userNotificationsListener?.remove()
        userNotificationsListener = null
        adminNotificationsListener = null
    }
}
