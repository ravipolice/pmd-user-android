package com.example.policemobiledirectory.repository

import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.data.local.EmployeeEntity
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.toEmployee
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.utils.PinHasher
import com.example.policemobiledirectory.api.EmployeeApiService
import com.example.policemobiledirectory.utils.SecurityConfig
import com.example.policemobiledirectory.utils.SearchUtils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.URL

import java.util.Date

// ------------------- REPOSITORY -------------------
open class EmployeeRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val employeeDao: EmployeeDao,
    private val firestore: FirebaseFirestore,
    private val apiService: EmployeeApiService,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val securityConfig: SecurityConfig
) {
    private val TAG = "EmployeeRepository"
    private val employeesCollection = firestore.collection("employees")
    private val storageRef = storage.reference
    private val metaDoc = firestore.collection("meta").document("idCounters")

    /**
     * Firestore rules require an authenticated user. If the app is logged out,
     * ensure we still have a lightweight anonymous Firebase session before
     * performing Firestore reads (e.g., login via email+PIN).
     */
    private suspend fun ensureFirestoreAuth() {
        if (auth.currentUser == null) {
            try {
                Log.d(TAG, "ensureFirestoreAuth: signing in anonymously for Firestore access")
                Firebase.auth.signInAnonymously().await()
            } catch (e: Exception) {
                Log.e(TAG, "ensureFirestoreAuth: anonymous sign-in failed", e)
                throw e
            }
        }
    }


    companion object {
        const val FIELD_EMAIL = "email"
        const val FIELD_FCM = "fcmToken"
        const val FIELD_KGID = "kgid"
        const val FIELD_PIN_HASH = "pin"              // pin hash stored in Firestore under "pin"
        const val RANK_PENDING = "Pending Verification"
    }

    // -------------------------------------------------------------------
    // SEARCH (Room backed) - Multi-keyword AND search with intelligent sorting
    // -------------------------------------------------------------------
    fun searchEmployees(query: String, filter: SearchFilter, currentUserDistrict: String? = null): Flow<List<EmployeeEntity>> {
        // For SearchFilter.ALL, use multi-keyword AND search
        if (filter == SearchFilter.ALL && query.trim().contains(" ")) {
            // Multi-keyword search: split by spaces and search for ALL keywords
            val keywords = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            
            if (keywords.isEmpty()) {
                return employeeDao.searchByName("%${query.trim().lowercase()}%")
            }
            
            // Get all employees and filter in memory for multi-keyword AND logic
            return employeeDao.getAllEmployees().map { employees ->
                val queryLower = query.trim().lowercase()
                val filtered = employees.filter { employee ->
                    // Check if ALL keywords are present in searchBlob
                    val searchBlob = employee.searchBlob.lowercase()
                    keywords.all { keyword -> searchBlob.contains(keyword) }
                }
                
                // Sort by relevance: exact match > same district > same range > others
                filtered.sortedWith(compareBy<EmployeeEntity> { employee ->
                    // Priority 1: Exact name match (lowest number = highest priority)
                    if (employee.name.lowercase() == queryLower) 0
                    else if (employee.name.lowercase().startsWith(queryLower)) 1
                    else 2
                }.thenBy { employee ->
                    // Priority 2: Same district as current user
                    if (currentUserDistrict != null && employee.district?.equals(currentUserDistrict, ignoreCase = true) == true) 0
                    else 1
                }.thenBy { employee ->
                    // Priority 3: Same range (extract range from district)
                    val userRange = extractRange(currentUserDistrict)
                    val employeeRange = extractRange(employee.district)
                    if (userRange != null && employeeRange != null && userRange == employeeRange) 0
                    else 1
                }.thenBy { employee ->
                    // Priority 4: Alphabetical by name
                    employee.name.lowercase()
                }).take(100) // Limit results
            }
        }
        
        // Single keyword or specific field search
        val searchQuery = "%${query.trim().lowercase()}%"
        val queryLower = query.trim().lowercase()
        
        return when (filter) {
            SearchFilter.ALL -> employeeDao.searchByName(searchQuery).map { employees ->
                // Sort single keyword searches too
                employees.sortedWith(compareBy<EmployeeEntity> { employee ->
                    // Exact match first
                    if (employee.name.lowercase() == queryLower) 0
                    else if (employee.name.lowercase().startsWith(queryLower)) 1
                    else 2
                }.thenBy { employee ->
                    // Same district
                    if (currentUserDistrict != null && employee.district?.equals(currentUserDistrict, ignoreCase = true) == true) 0
                    else 1
                }.thenBy { employee ->
                    // Same range
                    val userRange = extractRange(currentUserDistrict)
                    val employeeRange = extractRange(employee.district)
                    if (userRange != null && employeeRange != null && userRange == employeeRange) 0
                    else 1
                }.thenBy { employee ->
                    employee.name.lowercase()
                })
            }
            SearchFilter.NAME -> employeeDao.searchByName(searchQuery)
            SearchFilter.KGID -> employeeDao.searchByKgid(searchQuery)
            SearchFilter.MOBILE -> employeeDao.searchByMobile(searchQuery)
            SearchFilter.STATION -> employeeDao.searchByStation(searchQuery)
            SearchFilter.METAL_NUMBER -> employeeDao.searchByMetalNumber(searchQuery)
            SearchFilter.RANK -> employeeDao.searchByRank(searchQuery)
            SearchFilter.BLOOD_GROUP -> employeeDao.searchByBloodGroup(searchQuery)
        }
    }
    
    /**
     * Extract range from district name based on official Karnataka Police structure
     * 7 Ranges: Southern, Western, Eastern, Central, Northern, North Eastern, Ballari
     */
    private fun extractRange(district: String?): String? {
        if (district == null) return null
        
        // Direct range names
        if (district.contains("Range", ignoreCase = true)) {
            return district
        }
        
        // Map districts to their ranges (based on official Karnataka Police structure)
        return when {
            // Southern Range (Mysuru)
            district.contains("Mysuru", ignoreCase = true) -> "Southern Range"
            district.contains("Kodagu", ignoreCase = true) -> "Southern Range"
            district.contains("Mandya", ignoreCase = true) -> "Southern Range"
            district.contains("Hassan", ignoreCase = true) -> "Southern Range"
            district.contains("Chamarajanagar", ignoreCase = true) -> "Southern Range"
            
            // Western Range (Mangaluru)
            district.contains("Dakshina Kannada", ignoreCase = true) -> "Western Range"
            district.contains("Mangaluru", ignoreCase = true) -> "Western Range"
            district.contains("Udupi", ignoreCase = true) -> "Western Range"
            district.contains("Chikkamagaluru", ignoreCase = true) -> "Western Range"
            district.contains("Uttara Kannada", ignoreCase = true) -> "Western Range"
            
            // Eastern Range (Davangere)
            district.contains("Chitradurga", ignoreCase = true) -> "Eastern Range"
            district.contains("Davanagere", ignoreCase = true) -> "Eastern Range"
            district.contains("Davangere", ignoreCase = true) -> "Eastern Range"
            district.contains("Haveri", ignoreCase = true) -> "Eastern Range"
            district.contains("Shivamogga", ignoreCase = true) -> "Eastern Range"
            
            // Central Range (Bengaluru)
            district.contains("Bengaluru Rural", ignoreCase = true) -> "Central Range"
            district.contains("Bengaluru Urban", ignoreCase = true) -> "Central Range"
            district.contains("Bengaluru City", ignoreCase = true) -> "Central Range"
            district.contains("Bengaluru", ignoreCase = true) -> "Central Range"
            district.contains("Chikkaballapura", ignoreCase = true) -> "Central Range"
            district.contains("Kolar", ignoreCase = true) -> "Central Range"
            district.contains("Ramanagara", ignoreCase = true) -> "Central Range"
            district.contains("Tumakuru", ignoreCase = true) -> "Central Range"
            
            // Northern Range (Belagavi)
            district.contains("Bagalkote", ignoreCase = true) -> "Northern Range"
            district.contains("Bagalkot", ignoreCase = true) -> "Northern Range"
            district.contains("Belagavi", ignoreCase = true) -> "Northern Range"
            district.contains("Belgaum", ignoreCase = true) -> "Northern Range"
            district.contains("Dharwad", ignoreCase = true) -> "Northern Range"
            district.contains("Gadag", ignoreCase = true) -> "Northern Range"
            district.contains("Vijayapura", ignoreCase = true) -> "Northern Range"
            district.contains("Bijapur", ignoreCase = true) -> "Northern Range"
            
            // North Eastern Range (Kalaburagi)
            district.contains("Bidar", ignoreCase = true) -> "North Eastern Range"
            district.contains("Kalaburagi", ignoreCase = true) -> "North Eastern Range"
            district.contains("Gulbarga", ignoreCase = true) -> "North Eastern Range"
            district.contains("Yadgir", ignoreCase = true) -> "North Eastern Range"
            
            // Ballari Range (Ballari)
            district.contains("Ballari", ignoreCase = true) -> "Ballari Range"
            district.contains("Bellary", ignoreCase = true) -> "Ballari Range"
            district.contains("Koppal", ignoreCase = true) -> "Ballari Range"
            district.contains("Raichur", ignoreCase = true) -> "Ballari Range"
            district.contains("Raichuru", ignoreCase = true) -> "Ballari Range"
            district.contains("Vijayanagara", ignoreCase = true) -> "Ballari Range"
            
            else -> null
        }
    }


    // Unified Blob Search (Global Search)
    fun searchByBlob(query: String): Flow<RepoResult<List<Employee>>> = flow {
        emit(RepoResult.Loading)
        try {
            val normalizedQuery = "%${query.trim().lowercase().replace(Regex("[^a-z0-9\\s]"), "")}%"
            employeeDao.searchByBlob(normalizedQuery).collect { entities ->
                emit(RepoResult.Success(entities.map { it.toEmployee() }))
            }
        } catch (e: Exception) {
            emit(RepoResult.Error(e, "Search failed"))
        }
    }

    // -------------------------------------------------------------------
    // OFFLINE-FIRST LOGIN (Email + PIN hashed)
    // - 1) Try Room (offline)
    // - 2) If missing/mismatch, fallback to Firestore (online)
    // - 3) On successful online login, cache in Room
    // -------------------------------------------------------------------
    fun loginUserWithPin(email: String, pin: String): Flow<RepoResult<Employee>> = flow {
        emit(RepoResult.Loading)
        val normalizedEmail = email.trim().lowercase()
        try {
            ensureFirestoreAuth()

            // Step 1 — Local lookup (Room)
            val localEmployee = employeeDao.getEmployeeByEmail(normalizedEmail)
            if (localEmployee != null) {
                val storedHash = localEmployee.pin
                if (!storedHash.isNullOrEmpty() && PinHasher.verifyPin(pin, storedHash)) {
                    Log.d(TAG, "LoginFlow: offline login success for $email")
                    
                    // ✅ Sign in anonymously and update firebaseUid even for offline login
                    try {
                        val authResult = FirebaseAuth.getInstance().signInAnonymously().await()
                        val firebaseUid = authResult.user?.uid
                        if (firebaseUid != null) {
                            // Update firebaseUid in Firestore if different
                            val kgid = localEmployee.kgid.takeIf { !it.isNullOrBlank() }
                            if (kgid != null) {
                                try {
                                    val docRef = employeesCollection.document(kgid)
                                    val currentDoc = docRef.get().await()
                                    val currentFirebaseUid = currentDoc.getString("firebaseUid")
                                    if (currentFirebaseUid != firebaseUid) {
                                        docRef.update("firebaseUid", firebaseUid).await()
                                        Log.d(TAG, "✅ Updated firebaseUid in Firestore (offline login): $firebaseUid")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "⚠️ Failed to update firebaseUid in Firestore (offline): ${e.message}")
                                }
                            }
                            
                            // Update local cache with firebaseUid
                            val updatedEmployee = localEmployee.copy(firebaseUid = firebaseUid)
                            employeeDao.insertEmployee(updatedEmployee)
                            emit(RepoResult.Success(updatedEmployee.toEmployee()))
                        } else {
                            emit(RepoResult.Success(localEmployee.toEmployee()))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to sign in anonymously (offline login): ${e.message}")
                        // Continue with offline login anyway
                        emit(RepoResult.Success(localEmployee.toEmployee()))
                    }
                    return@flow
                }
            }

            // Step 2 — Firestore fallback
            var snapshot = employeesCollection
                .whereEqualTo(FIELD_EMAIL, normalizedEmail)
                .limit(1)
                .get()
                .await()

            // ✅ Fallback to admins collection
            var isAdminCollection = false
            if (snapshot.isEmpty) {
                val adminSnapshot = firestore.collection("admins")
                    .whereEqualTo("email", normalizedEmail)
                    .limit(1)
                    .get()
                    .await()
                
                if (!adminSnapshot.isEmpty) {
                    snapshot = adminSnapshot
                    isAdminCollection = true
                    Log.d(TAG, "LoginFlow: User found in 'admins' collection")
                }
            }

            if (snapshot.isEmpty) {
                emit(RepoResult.Error(null, "Invalid email or PIN"))
                return@flow
            }

            val doc = snapshot.documents.first()
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
            val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
            var remoteEmployee = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
            
            // ✅ Enforce isAdmin if from admins collection
            if (isAdminCollection) {
                remoteEmployee = remoteEmployee?.copy(isAdmin = true)
            }

            // read pin hash directly from document (defensive if Employee POJO doesn't contain it)
            val remotePinHash = doc.getString(FIELD_PIN_HASH).orEmpty()

            if (remotePinHash.isEmpty()) {
                emit(RepoResult.Error(null,"PIN not set for this account"))
                return@flow
            }

            // Step 3 — Verify entered PIN against stored hash (remote)
            if (!PinHasher.verifyPin(pin, remotePinHash)) {
                emit(RepoResult.Error(null,"Invalid email or PIN"))
                return@flow
            }

            try {
                val authResult = FirebaseAuth.getInstance().signInAnonymously().await()
                val firebaseUid = authResult.user?.uid
                Log.d(TAG, "🔥 Firebase anonymous login success (PIN login), UID: $firebaseUid")
                
                // ✅ Update firebaseUid in Firestore if it's different
                if (firebaseUid != null) {
                    val currentFirebaseUid = doc.getString("firebaseUid")
                    if (currentFirebaseUid != firebaseUid) {
                        try {
                            doc.reference.update("firebaseUid", firebaseUid).await()
                            Log.d(TAG, "✅ Updated firebaseUid in Firestore: $firebaseUid")
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Failed to update firebaseUid in Firestore: ${e.message}")
                            // Continue anyway - not critical for login
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase anonymous login failed (PIN login)", e)
                emit(RepoResult.Error(e, "Login failed. Try again."))
                return@flow
            }

            // Step 4 — Cache remote record locally for offline use
            // Build entity from remote Employee object (or from doc fields)
            val authUser = FirebaseAuth.getInstance().currentUser
            val entityToCache = buildEmployeeEntityFromDoc(doc, pinHash = remotePinHash).copy(
                firebaseUid = authUser?.uid ?: ""
            )
            employeeDao.insertEmployee(entityToCache)
            emit(RepoResult.Success(entityToCache.toEmployee()))
        } catch (e: Exception) {
            Log.e(TAG, "loginUserWithPin failed", e)
            emit(RepoResult.Error(null,"Login failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // APPLY NEW PIN AFTER RESET (atomic: update Firestore then Room)
    // This is used when user has reset via email and now supplies new PIN in-app.
    // -------------------------------------------------------------------
    suspend fun applyNewPinAfterReset(email: String, newPinPlain: String): RepoResult<String> = withContext(ioDispatcher) {
        try {
            val newPinHash = PinHasher.hashPassword(newPinPlain)

            // 1) Find user document
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            if (snapshot.isEmpty) return@withContext RepoResult.Error(null,"No employee found for $email")

            val doc = snapshot.documents.first()
            val docRef = doc.reference

            // 2) Update Firestore
            val updates = mapOf(FIELD_PIN_HASH to newPinHash)
            docRef.update(updates).await()

            // 3) Update Room (local cache)
            val localEmp = employeeDao.getEmployeeByEmail(email)
            if (localEmp != null) {
                val updatedLocal = localEmp.copy(pin = newPinHash)
                employeeDao.insertEmployee(updatedLocal)
            } else {
                // If local missing, fetch the remote full object and insert
                // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
                val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                val updatedRemote = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
                if (updatedRemote != null) {
                    val entity = buildEmployeeEntityFromDoc(doc, pinHash = newPinHash)
                    employeeDao.insertEmployee(entity)
                }
            }

            Log.i(TAG, "applyNewPinAfterReset: PIN applied for $email")
            RepoResult.Success("New PIN applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "applyNewPinAfterReset failed", e)
            RepoResult.Error(null,"Failed to apply new PIN: ${e.message}")
        }
    }

    private suspend fun verifyEmailBeforePinChange(email: String): Boolean {
        // Example — adjust based on your actual verification system
        val document = firestore.collection("employees").document(email).get().await()
        return document.exists()
    }

    private suspend fun updatePinByEmail(email: String, hashedPin: String) {
        try {
            firestore.collection("employees")
                .document(email)
                .update("pin", hashedPin)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to update PIN: ${e.message}")
        }
    }

    suspend fun deleteEmployee(kgid: String, photoUrl: String?) = withContext(ioDispatcher) {
        try {
            // ✅ 1️⃣ Delete from Google Sheet (via Apps Script API)
            // ✅ 1️⃣ Delete from Google Sheet (LEGACY - DISABLED)
            /*
            apiService.deleteEmployee(
                token = securityConfig.getSecretToken(),
                kgid = kgid
            )
            */

            // ✅ 2️⃣ Delete from Firestore
            firestore.collection("employees").document(kgid).delete().await()

            // ✅ 3️⃣ Delete from Room (local cache)
            employeeDao.deleteByKgid(kgid)

            // ✅ 4️⃣ Delete image from Google Drive (optional)
            photoUrl?.takeIf { it.contains("id=") }?.let { url ->
                val fileId = url.substringAfter("id=").substringBefore("&")
                deleteImageFromDrive(fileId)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun deleteImageFromDrive(fileId: String) {
        try {
            // ✅ FIX: Replaced khttp with a call to the Retrofit apiService
            val response = apiService.deleteImageFromDrive(
                token = securityConfig.getSecretToken(),
                fileId = fileId
            )
            if (response.isSuccessful) {
                Log.d(TAG, "deleteImageFromDrive: Successfully deleted image with fileId: $fileId")
            } else {
                Log.e(TAG, "deleteImageFromDrive: Failed to delete image. Server responded with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteImageFromDrive: Network request failed", e)
        }
    }

    // -------------------------------------------------------------------
    // UNIFIED PIN UPDATE (update both Firestore and Room)
    // Use this when admin or user changes PIN from inside app (online)
    // -------------------------------------------------------------------
    suspend fun updateUserPinBothOnlineAndOffline(email: String, newPinPlain: String): RepoResult<String> = withContext(ioDispatcher) {
        try {
            val hashedPin = PinHasher.hashPassword(newPinPlain)

            // Update Firestore document (if exists)
            val querySnapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            if (querySnapshot.isEmpty) {
                return@withContext RepoResult.Error(null,"User not found in Firebase")
            }

            val docRef = querySnapshot.documents.first().reference
            docRef.update(FIELD_PIN_HASH, hashedPin).await()

            // Update Room cache
            val localEmp = employeeDao.getEmployeeByEmail(email)
            if (localEmp != null) {
                employeeDao.insertEmployee(localEmp.copy(pin = hashedPin))
            } else {
                // If local missing, insert remote doc
                val remoteDoc = querySnapshot.documents.first()
                val entity = buildEmployeeEntityFromDoc(remoteDoc, pinHash = hashedPin)
                employeeDao.insertEmployee(entity)
            }

            Log.i(TAG, "updateUserPinBothOnlineAndOffline: PIN updated for $email")
            RepoResult.Success("PIN updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "updateUserPinBothOnlineAndOffline failed", e)
            RepoResult.Error(null,"Failed to update PIN: ${e.message}")
        }
    }

    private fun generateRandomCode(length: Int = 6): String {
        val chars = "0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    suspend fun sendOtp(email: String): RepoResult<String> {
        return try {
            Log.d(TAG, "📤 Attempting to send OTP to: $email")

            val data = mapOf("email" to email)

            // CRITICAL FIX: explicitly asking for the 'asia-south1' instance
            val result = FirebaseFunctions.getInstance("asia-south1")
                .getHttpsCallable("requestOtp")
                .call(data)
                .await()

            // Safely extract the response map
            val responseData = result.data as? Map<*, *>
            Log.d(TAG, "✅ Cloud Function response: $responseData")

            if (responseData == null || responseData["success"] != true) {
                val msg = responseData?.get("message")?.toString() ?: "Unknown error from server."
                Log.e(TAG, "❌ Cloud Function returned error: $msg")
                return RepoResult.Error(Exception(msg), msg)
            }

            Log.d(TAG, "📩 OTP email successfully requested for $email")

            RepoResult.Success(responseData["message"]?.toString() ?: "Verification code sent to your email.")

        } catch (e: Exception) {
            Log.e(TAG, "💥 OTP send failed: ${e.message}", e)
            // If e.message contains "404", it specifically means the function URL was not found.
            RepoResult.Error(e, e.message ?: "Failed to send verification code.")
        }
    }


    // 🔹 3. Verify OTP code
    suspend fun verifyLoginCode(email: String, code: String): RepoResult<Employee> {
        val normalizedEmail = email.trim().lowercase()

        Log.d(TAG, "verifyLoginCode() called for normalizedEmail=$normalizedEmail code=$code")
        Log.e("EMAIL_CHECK", "Email entering verifyOtp = '${email}'")

        return try {
            val data = mapOf(
                "email" to normalizedEmail,
                "code" to code.trim()
            )

            val result = FirebaseFunctions
                .getInstance("asia-south1")
                .getHttpsCallable("verifyOtpEmail")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            Log.d(TAG, "✅ verifyOtpEmail response: $response")

            if (response == null || response["success"] != true) {
                val msg = response?.get("message")?.toString() ?: "OTP verification failed."
                Log.e(TAG, "❌ OTP verification failed: $msg")
                return RepoResult.Error(null, msg)
            }

            val empData = response["employee"] as? Map<*, *>
            if (empData == null) {
                Log.e(TAG, "❌ No employee data returned for $normalizedEmail")
                return RepoResult.Error(null, "No employee found for this email")
            }

            val emp = mapEmployeeFromResponse(empData)
            
            try {
                val authResult = FirebaseAuth.getInstance().signInAnonymously().await()
                val firebaseUid = authResult.user?.uid
                Log.d(TAG, "🔥 Firebase anonymous login success (OTP flow), UID: $firebaseUid")
                
                // ✅ Update firebaseUid in Firestore if it's different
                if (firebaseUid != null) {
                    val kgid = emp.kgid.takeIf { !it.isNullOrBlank() } ?: throw IllegalStateException("Employee kgid is missing")
                    val docRef = employeesCollection.document(kgid)
                    try {
                        val currentDoc = docRef.get().await()
                        val currentFirebaseUid = currentDoc.getString("firebaseUid")
                        if (currentFirebaseUid != firebaseUid) {
                            docRef.update("firebaseUid", firebaseUid).await()
                            Log.d(TAG, "✅ Updated firebaseUid in Firestore: $firebaseUid")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to update firebaseUid in Firestore: ${e.message}")
                        // Continue anyway - not critical for login
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase anonymous login failed (OTP flow)", e)
                return RepoResult.Error(e, "Login failed. Please try again.")
            }

            // Update local cache with firebaseUid
            val authUser = FirebaseAuth.getInstance().currentUser
            val empWithUid = emp.copy(firebaseUid = authUser?.uid)
            employeeDao.insertEmployee(empWithUid.toEntity())
            
            Log.d(TAG, "✅ Employee cached for $normalizedEmail")
            RepoResult.Success(empWithUid)

        } catch (e: Exception) {
            Log.e(TAG, "💥 verifyLoginCode() failed", e)
            RepoResult.Error(e, "Failed to verify code: ${e.message}")
        }
    }


    // -------------------------------------------------------------------
    // FIRESTORE → ROOM SYNC (bulk)
    // -------------------------------------------------------------------
    suspend fun syncAllEmployees(): RepoResult<Unit> = withContext(ioDispatcher) {
        try {
            val snapshot = employeesCollection.get().await()
            
            val visibleEntities = ArrayList<EmployeeEntity>()
            val hiddenKgids = ArrayList<String>()

            snapshot.documents.forEach { doc ->
                val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                // toObject will now deserialise isHidden because we updated Employee.kt
                val emp = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)

                if (emp != null) {
                    if (emp.isHidden) {
                        if (docKgid.isNotBlank()) hiddenKgids.add(docKgid)
                    } else {
                        // Only add to list if NOT hidden
                        val entity = buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty())
                        visibleEntities.add(entity)
                    }
                }
            }

            // 1. Insert/Update visible employees
            if (visibleEntities.isNotEmpty()) {
                employeeDao.insertEmployees(visibleEntities)
            }
            
            // 2. Delete hidden employees from local DB
            if (hiddenKgids.isNotEmpty()) {
                Log.d(TAG, "Removing ${hiddenKgids.size} hidden employees from local DB")
                hiddenKgids.forEach { kgid ->
                    employeeDao.deleteByKgid(kgid)
                }
            }
            
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncAllEmployees failed", e)
            RepoResult.Error(null,"Failed to sync employees: ${e.message}")
        }
    }

    // -------------------------------------------------------------------
    // CRUD and helpers
    // -------------------------------------------------------------------
    fun addOrUpdateEmployee(emp: Employee): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val kgid = emp.kgid.takeIf { !it.isNullOrBlank() } ?: generateAutoId()
            
            // ✅ CRITICAL: Get current authenticated user's UID
            val currentUser = auth.currentUser
            val currentFirebaseUid = currentUser?.uid
            
            // ✅ Read existing document to get current firebaseUid
            val docRef = employeesCollection.document(kgid)
            val existingDoc = try {
                docRef.get().await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not read existing document: ${e.message}")
                null
            }
            
            val docExists = existingDoc?.exists() == true
            val existingFirebaseUid = existingDoc?.getString("firebaseUid")
            
            Log.d(TAG, "📋 Document check: exists=$docExists, existingFirebaseUid=$existingFirebaseUid, currentAuthUid=$currentFirebaseUid")
            
            // ✅ If firebaseUid is missing and we have a current user, set it first
            if ((existingFirebaseUid == null || existingFirebaseUid.isEmpty()) && currentFirebaseUid != null && docExists) {
                try {
                    docRef.update("firebaseUid", currentFirebaseUid).await()
                    Log.d(TAG, "✅ Set firebaseUid for first time: $currentFirebaseUid")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to set firebaseUid: ${e.message}")
                    // Continue anyway - might be permission issue, but we'll try the update
                }
            }
            
            // ✅ Ensure firebaseUid is included in the Employee object
            // Use existing firebaseUid if available, otherwise use current user's UID
            val firebaseUidToUse = if (docExists) {
                // For existing employees: use existing firebaseUid, or current user's UID if missing
                existingFirebaseUid?.takeIf { it.isNotBlank() } 
                    ?: currentFirebaseUid 
                    ?: emp.firebaseUid
            } else {
                // ✅ FIX: For NEW employees created by admin, don't use admin's UID
                // Leave firebaseUid null/empty - it will be set when employee first logs in
                emp.firebaseUid?.takeIf { it.isNotBlank() } ?: null
            }
            
            // ✅ CRITICAL FIX: Ensure kgid and firebaseUid are explicitly set, and email is normalized
            var finalEmp = emp.copy(
                kgid = kgid,
                email = emp.email!!.trim().lowercase(),
                firebaseUid = firebaseUidToUse
            )
            
            Log.d(TAG, "💾 Saving employee: kgid=$kgid, firebaseUid=$firebaseUidToUse, currentAuthUid=$currentFirebaseUid, docExists=$docExists")
            Log.d(TAG, "💾 Employee fields: name=${finalEmp.name}, email=${finalEmp.email}, metalNumber=${finalEmp.metalNumber}")
            
            // ✅ Use update() if document exists (for profile updates, document should always exist)
            // This ensures Firestore update rules are applied correctly
            if (docExists) {
                // Document exists - use update() to update fields
                // Build update map with only the fields we want to update (exclude protected fields)
                // Protected fields: email, kgid, isAdmin, pin, firebaseUid (handled separately)
                val updateMap = mutableMapOf<String, Any>()
                
                // ✅ REQUIRED FIELDS - Always include (even if empty, though they shouldn't be)
                updateMap["name"] = finalEmp.name
                
                // ✅ MOBILE FIELDS - Include if not null
                finalEmp.mobile1?.takeIf { it.isNotBlank() }?.let {
                    updateMap["mobile1"] = it
                }
                finalEmp.mobile2?.takeIf { it.isNotBlank() }?.let {
                    updateMap["mobile2"] = it
                }
                
                // ✅ RANK AND METAL NUMBER - Include if they have values
                finalEmp.rank?.takeIf { it.isNotBlank() }?.let {
                    updateMap["rank"] = it
                }
                finalEmp.metalNumber?.takeIf { it.isNotBlank() }?.let {
                    updateMap["metalNumber"] = it
                }
                
                // ✅ LOCATION FIELDS - Include if they have values
                // Note: District is read-only for self-edit in UI, but including it won't hurt if unchanged
                finalEmp.district?.takeIf { it.isNotBlank() }?.let {
                    updateMap["district"] = it
                }
                finalEmp.station?.takeIf { it.isNotBlank() }?.let {
                    updateMap["station"] = it
                }
                
                // ✅ BLOOD GROUP - Optional field
                finalEmp.bloodGroup?.takeIf { it.isNotBlank() }?.let {
                    updateMap["bloodGroup"] = it
                }
                
                // ✅ PHOTO FIELDS - Include if they have values
                finalEmp.photoUrl?.takeIf { it.isNotBlank() }?.let {
                    updateMap["photoUrl"] = it
                }
                finalEmp.photoUrlFromGoogle?.takeIf { it.isNotBlank() }?.let {
                    updateMap["photoUrlFromGoogle"] = it
                }
                
                // ✅ FCM TOKEN - Include if present (for notifications)
                finalEmp.fcmToken?.takeIf { it.isNotBlank() }?.let {
                    updateMap["fcmToken"] = it
                }

                // ✅ UNIT FIELD - Include if not null (Hybrid Strategy)
                finalEmp.unit?.let {
                    updateMap["unit"] = it
                }
                
                // Only update firebaseUid if it was missing
                if (firebaseUidToUse != null && (existingFirebaseUid == null || existingFirebaseUid.isEmpty())) {
                    updateMap["firebaseUid"] = firebaseUidToUse
                }
                
                // ✅ COMPREHENSIVE LOGGING
                Log.d(TAG, "═══════════════════════════════════════════════════════")
                Log.d(TAG, "📝 UPDATE MAP DETAILS:")
                Log.d(TAG, "   Fields in update map: ${updateMap.keys.joinToString(", ")}")
                Log.d(TAG, "   Total fields: ${updateMap.size}")
                Log.d(TAG, "📝 FINAL EMPLOYEE VALUES:")
                Log.d(TAG, "   name: '${finalEmp.name}'")
                Log.d(TAG, "   email: '${finalEmp.email}' (protected - not in update)")
                Log.d(TAG, "   mobile1: '${finalEmp.mobile1}' → ${if (updateMap.containsKey("mobile1")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   mobile2: '${finalEmp.mobile2}' → ${if (updateMap.containsKey("mobile2")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   rank: '${finalEmp.rank}' → ${if (updateMap.containsKey("rank")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   metalNumber: '${finalEmp.metalNumber}' → ${if (updateMap.containsKey("metalNumber")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   district: '${finalEmp.district}' → ${if (updateMap.containsKey("district")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   station: '${finalEmp.station}' → ${if (updateMap.containsKey("station")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   bloodGroup: '${finalEmp.bloodGroup}' → ${if (updateMap.containsKey("bloodGroup")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   photoUrl: '${finalEmp.photoUrl?.take(50)}...' → ${if (updateMap.containsKey("photoUrl")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "   photoUrlFromGoogle: '${finalEmp.photoUrlFromGoogle?.take(50)}...' → ${if (updateMap.containsKey("photoUrlFromGoogle")) "INCLUDED" else "EXCLUDED"}")
                Log.d(TAG, "═══════════════════════════════════════════════════════")
                
                if (updateMap.isEmpty()) {
                    Log.w(TAG, "⚠️ Update map is empty! Nothing to update.")
                    emit(RepoResult.Success(true))
                    return@flow
                }
                
                docRef.update(updateMap).await()
                Log.d(TAG, "✅ Successfully updated employee document with ${updateMap.size} fields")
            } else {
                // Document doesn't exist - creating new employee (admin add employee flow)
                Log.d(TAG, "📝 Creating NEW employee document for kgid=$kgid")
                Log.d(TAG, "📝 Admin creating employee - ensuring all required fields are set")
                
                // ✅ For new employee creation, ensure isApproved is set (defaults to false for new registrations)
                // Admin-created employees should be approved by default
                // ✅ firebaseUid: For new employees, this will be null until they first log in.
                //    When they log in (via PIN or OTP), Firebase Authentication will generate a UID
                //    and it will be automatically set in Firestore by the login flow.
                val employeeToCreate = finalEmp.copy(
                    isApproved = true,  // ✅ Admin-created employees are auto-approved
                    isAdmin = finalEmp.isAdmin ?: false,  // ✅ Preserve isAdmin if set, otherwise false
                    firebaseUid = finalEmp.firebaseUid?.takeIf { it.isNotBlank() } ?: null  // ✅ Will be set when employee first logs in
                )
                
                Log.d(TAG, "📝 Creating employee with isApproved=true, isAdmin=${employeeToCreate.isAdmin}, firebaseUid=${employeeToCreate.firebaseUid?.take(10) ?: "null (will be set on first login)"}...")
                
                try {
                    docRef.set(employeeToCreate, SetOptions.merge()).await()
                    Log.d(TAG, "✅ Successfully created new employee document")
                    
                    // ✅ Use employeeToCreate (with isApproved=true) for Room database
                    finalEmp = employeeToCreate
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to create employee document", e)
                    Log.e(TAG, "❌ Error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "❌ Error message: ${e.message}")
                    
                    // Check if it's a permission error
                    if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                        e.message?.contains("permission-denied", ignoreCase = true) == true) {
                        Log.e(TAG, "❌ PERMISSION DENIED - Admin status may not be recognized")
                        Log.e(TAG, "❌ Current user UID: ${auth.currentUser?.uid}")
                        Log.e(TAG, "❌ Current user email: ${auth.currentUser?.email}")
                        throw IllegalStateException("Permission denied: Admin access required. Please ensure your account has admin privileges in Firestore.")
                    }
                    throw e
                }
            }
            
            Log.d(TAG, "✅ Successfully saved employee: kgid=$kgid, isApproved=${finalEmp.isApproved}")
            Log.d(TAG, "✅ Employee details: name=${finalEmp.name}, email=${finalEmp.email}, isAdmin=${finalEmp.isAdmin}")
            
            // Insert/update into Room (use finalEmp which now has isApproved=true for new employees)
            val entity = buildEmployeeEntityFromPojo(finalEmp)
            Log.d(TAG, "✅ Saving to Room DB: kgid=${entity.kgid}, isApproved=${entity.isApproved}, name=${entity.name}")
            employeeDao.insertEmployee(entity)
            Log.d(TAG, "✅ Employee saved to Room DB successfully")
            
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "❌ addOrUpdateEmployee failed", e)
            Log.e(TAG, "❌ Error details: ${e.message}")
            e.printStackTrace()
            emit(RepoResult.Error(null,"Failed to add/update employee: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    fun deleteEmployee(kgid: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            employeesCollection.document(kgid).delete().await()
            employeeDao.deleteByKgid(kgid)
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "deleteEmployee failed", e)
            emit(RepoResult.Error(null,"Failed to delete employee: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // Delete employee by email (helper function)
    suspend fun deleteEmployeeByEmail(email: String): RepoResult<Boolean> = withContext(ioDispatcher) {
        try {
            // Find employee by email
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull()
            
            if (doc == null) {
                return@withContext RepoResult.Error(null, "Employee with email $email not found")
            }
            
            // ✅ CRITICAL FIX: Use document ID as kgid if field is missing
            val kgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
            val employee = doc.toObject(Employee::class.java)?.copy(kgid = kgid)
            
            // Delete from Firestore
            employeesCollection.document(kgid).delete().await()
            
            // Delete from Room (local cache)
            employeeDao.deleteByKgid(kgid)
            
            // Delete from Google Sheet if API is available
            // Delete from Google Sheet if API is available (LEGACY - DISABLED)
            /*
            try {
                apiService.deleteEmployee(
                    token = securityConfig.getSecretToken(),
                    kgid = kgid
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete from Google Sheet: ${e.message}")
            }
            */
            
            Log.i(TAG, "Successfully deleted employee: $kgid ($email)")
            RepoResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "deleteEmployeeByEmail failed", e)
            RepoResult.Error(null, "Failed to delete employee: ${e.message}")
        }
    }

    /**
     * Helper function to fetch ALL employees from Firestore using pagination
     * Firestore has a default limit of ~500 documents per query, so we need pagination
     */
    private suspend fun fetchAllEmployeesFromFirestore(): List<DocumentSnapshot> {
        val allDocuments = mutableListOf<DocumentSnapshot>()
        var lastDocument: DocumentSnapshot? = null
        val batchSize = 500L  // ✅ Firestore limit() expects Long, not Int
        var documents: List<DocumentSnapshot>
        
        do {
            val query = if (lastDocument == null) {
                employeesCollection.limit(batchSize)
            } else {
                employeesCollection.startAfter(lastDocument).limit(batchSize)
            }
            
            val snapshot = query.get().await()
            documents = snapshot.documents
            
            if (documents.isNotEmpty()) {
                allDocuments.addAll(documents)
                lastDocument = documents.last()
                Log.d(TAG, "📥 Fetched ${documents.size} employees (total so far: ${allDocuments.size})")
            }
            
        } while (documents.size == batchSize.toInt()) // Continue if we got a full batch (might be more)
        
        Log.d(TAG, "✅ Total employees fetched from Firestore: ${allDocuments.size}")
        return allDocuments
    }

    fun getEmployees(): Flow<RepoResult<List<Employee>>> = flow {
        emit(RepoResult.Loading)
        try {
            // getAllEmployees returns Flow<List<EmployeeEntity>> from DAO — use .first() to collect current
            var cached = employeeDao.getAllEmployees().first()
            if (cached.isEmpty()) {
                // ✅ FIX: Use pagination to fetch ALL employees (not just first 500)
                val allDocuments = fetchAllEmployeesFromFirestore()
                // ✅ CRITICAL FIX: Ensure kgid is set from document ID for all employees
                val entities = allDocuments.mapNotNull { doc ->
                    val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                    val emp = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
                    emp?.let { buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty()) }
                }
                employeeDao.insertEmployees(entities)
                cached = employeeDao.getAllEmployees().first()
            }
            emit(RepoResult.Success(cached.map { it.toEmployee() }))
        } catch (e: Exception) {
            Log.e(TAG, "getEmployees failed", e)
            emit(RepoResult.Error(null,"Failed to fetch employees: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // PROFILE PHOTO UPLOAD
    // -------------------------------------------------------------------
    fun uploadProfilePhoto(uri: Uri, kgid: String): Flow<RepoResult<String>> = flow {
        emit(RepoResult.Loading)
        try {
            val fileRef = storageRef.child("profile_photos/$kgid.jpg")
            fileRef.putFile(uri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            employeesCollection.document(kgid).update("photoUrl", downloadUrl).await()

            // Update local cache if present
            val local = employeeDao.getEmployeeByKgid(kgid)
            if (local != null) {
                employeeDao.insertEmployee(local.copy(photoUrl = downloadUrl))
            }

            emit(RepoResult.Success(downloadUrl))
        } catch (e: Exception) {
            Log.e(TAG, "uploadProfilePhoto failed", e)
            emit(RepoResult.Error(null,"Failed to upload photo: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // PENDING REGISTRATIONS
    // -------------------------------------------------------------------
    fun getPendingRegistrations(): Flow<List<PendingRegistrationEntity>> = flow {
        val snapshot = employeesCollection.whereEqualTo("rank", RANK_PENDING).get().await()
        val pending = snapshot.documents.mapNotNull { doc ->
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
            val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
            val emp = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
            emp?.let {
                if (it.kgid.isNullOrBlank() || it.email.isNullOrBlank()) return@mapNotNull null
                PendingRegistrationEntity(
                    kgid = it.kgid,
                    name = it.name ?: "",
                    email = it.email ?: "",
                    mobile1 = it.mobile1 ?: "",
                    mobile2 = it.mobile2 ?: "",
                    station = it.station ?: "",
                    district = it.district ?: "",
                    bloodGroup = it.bloodGroup ?: "",
                    photoUrl = it.photoUrl ?: "",
                    firebaseUid = it.firebaseUid ?: "",
                    submittedAt = it.createdAt?.time ?: System.currentTimeMillis(),
                    isManualStation = it.isManualStation
                )
            }
        }
        emit(pending)
    }.flowOn(ioDispatcher)

    fun approvePendingRegistration(entity: PendingRegistrationEntity): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            // ✅ CRITICAL FIX: Ensure kgid is set and not empty
            val kgid = entity.kgid.trim().takeIf { it.isNotBlank() } 
                ?: throw IllegalArgumentException("KGID is required for approval")
            val employee = entity.toEmployee().copy(
                rank = "Verified",
                kgid = kgid // ✅ Explicitly ensure kgid is set
            )
            val pendingQuery = employeesCollection.whereEqualTo("kgid", kgid).limit(1).get().await()
            val pendingDocId = pendingQuery.documents.firstOrNull()?.id
            // ✅ CRITICAL FIX: Use kgid as document ID and ensure kgid field is included
            val targetDocRef = employeesCollection.document(kgid)

            firestore.runBatch { batch ->
                // ✅ CRITICAL FIX: Ensure kgid field is explicitly included in document (already set in employee object)
                batch.set(targetDocRef, employee)
                pendingDocId?.let { id -> if (id != kgid) batch.delete(employeesCollection.document(id)) }
            }.await()

            employeeDao.insertEmployee(employee.toEntity())
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "approvePendingRegistration failed", e)
            emit(RepoResult.Error(null,"Approval failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    fun rejectPendingRegistration(entity: PendingRegistrationEntity, reason: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val pendingDoc = employeesCollection.whereEqualTo("kgid", entity.kgid).limit(1).get().await().documents.firstOrNull()
            pendingDoc?.reference?.delete()?.await()
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "rejectPendingRegistration failed", e)
            emit(RepoResult.Error(null,"Rejection failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // GOOGLE SIGN-IN (only for registration / admin flows)
    // -------------------------------------------------------------------
    fun signInWithGoogleIdToken(idToken: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw IllegalStateException("Firebase user is null")
            val email = user.email ?: throw IllegalStateException("Google account has no email")
            var snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            
            // ✅ Fallback to admins collection
            if (snapshot.isEmpty) {
                snapshot = firestore.collection("admins").whereEqualTo("email", email).limit(1).get().await()
            }
            
            if (snapshot.isEmpty) emit(RepoResult.Error(null,"User not found: $email"))
            else emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogleIdToken failed", e)
            emit(RepoResult.Error(null,e.message ?: "Google Sign-In failed"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------
    private suspend fun generateAutoId(): String = withContext(ioDispatcher) {
        firestore.runTransaction { tx: Transaction ->
            val snapshot = tx.get(metaDoc)
            val current = snapshot.getLong("contactsCounter") ?: 0
            val next = current + 1
            tx.update(metaDoc, "contactsCounter", next)
            "AGID${next.toString().padStart(4, '0')}"
        }.await()
    }

    suspend fun getEmployeeByEmail(email: String): EmployeeEntity? = withContext(ioDispatcher) {
        val normalizedEmail = email.trim().lowercase()
        val rawEmail = email.trim()

        // Primary: local Room (try normalized first)
        var local = employeeDao.getEmployeeByEmail(normalizedEmail)
        
        // If not found locally, try raw email (for legacy case-sensitive data)
        if (local == null && normalizedEmail != rawEmail) {
            local = employeeDao.getEmployeeByEmail(rawEmail)
        }
        
        if (local != null) return@withContext local

        // Fallback: Firestore (try normalized first)
        var snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, normalizedEmail).limit(1).get().await()
        
        // If not found in Firestore with lowercase, try raw email (legacy data support)
        if (snapshot.isEmpty && normalizedEmail != rawEmail) {
            Log.d(TAG, "getEmployeeByEmail: User not found with lowercase '$normalizedEmail', trying raw '$rawEmail'")
            snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, rawEmail).limit(1).get().await()
        }

        // ✅ Fallback to 'admins' collection if still not found
        var isAdminCollection = false
        if (snapshot.isEmpty) {
             Log.d(TAG, "getEmployeeByEmail: User not found in 'employees', checking 'admins'")
             snapshot = firestore.collection("admins").whereEqualTo("email", normalizedEmail).limit(1).get().await()
             if (!snapshot.isEmpty) {
                 isAdminCollection = true
             }
        }

        val doc = snapshot.documents.firstOrNull()
        // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
        val docKgid = doc?.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc?.id ?: ""
        
        var remote = doc?.toObject(Employee::class.java)?.copy(kgid = docKgid)
        if (isAdminCollection && remote != null) {
            remote = remote.copy(isAdmin = true)
        }

        if (remote != null && doc != null) {
            val entity = buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty())
            
            // If from admin collection, ensure entity is marked as admin
            val finalEntity = if (isAdminCollection) entity.copy(isAdmin = true) else entity
            
            employeeDao.insertEmployee(finalEntity)
            return@withContext finalEntity
        }
        return@withContext null
    }

    // Fetch current logged in user from Firebase and cache locally
    suspend fun getCurrentUserFromFirebase(): RepoResult<Employee?> = withContext(ioDispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext RepoResult.Error(null,"No user logged in")
            val email = user.email ?: return@withContext RepoResult.Error(null,"No email found")
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull()
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
            val docKgid = doc?.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc?.id ?: ""
            val employee = doc?.toObject(Employee::class.java)?.copy(kgid = docKgid)
            if (doc != null && employee != null) {
                val entity = buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty())
                employeeDao.insertEmployee(entity)
            }
            RepoResult.Success(employee)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserFromFirebase failed", e)
            RepoResult.Error(null,"Failed to get user: ${e.message}")
        }
    }

    suspend fun logout() = withContext(ioDispatcher) {
        auth.signOut()
        employeeDao.clearEmployees()
    }

    // -------------------------------------------------------------------
    // ENTITY MAPPERS (Room <-> Domain)
    // -------------------------------------------------------------------
    private fun EmployeeEntity.toEmployee(): Employee {
        return Employee(
            kgid = kgid,
            name = name,
            email = email,
            pin = pin,
            mobile1 = mobile1,
            mobile2 = mobile2,
            rank = rank,
            metalNumber = metalNumber,
            district = district,
            station = station,
            bloodGroup = bloodGroup,
            photoUrl = photoUrl,
            photoUrlFromGoogle = photoUrlFromGoogle,
            fcmToken = fcmToken,
            firebaseUid = firebaseUid,
            isAdmin = isAdmin,
            isApproved = isApproved,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // convert domain Employee -> entity (pin not available from domain POJO usually)
    private fun Employee.toEntity(): EmployeeEntity {
        val blob = SearchUtils.generateSearchBlob(
            kgid, name ?: "", mobile1 ?: "", mobile2 ?: "", rank ?: "", metalNumber ?: "",
            district ?: "", station ?: "", unit ?: "", bloodGroup ?: ""
        )
        return EmployeeEntity(
            kgid = kgid,
            name = name ?: "",
            email = email,
            pin = pin,
            mobile1 = mobile1 ?: "",
            mobile2 = mobile2 ?: "",
            rank = rank ?: "",
            metalNumber = metalNumber ?: "",
            district = district ?: "",
            station = station ?: "",
            bloodGroup = bloodGroup ?: "",
            photoUrl = photoUrl ?: "",
            photoUrlFromGoogle = photoUrlFromGoogle ?: "",
            fcmToken = fcmToken ?: "",
            firebaseUid = firebaseUid ?: "",
            isAdmin = isAdmin,
            isApproved = isApproved,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unit = unit,
            searchBlob = blob,
            isManualStation = isManualStation
        )
    }

    // build EmployeeEntity from Firestore document (defensive)
    private fun buildEmployeeEntityFromDoc(doc: DocumentSnapshot, pinHash: String = ""): EmployeeEntity {
        // ✅ CRITICAL FIX: Always use document ID as kgid if field is missing
        val docKgid = doc.id // Document ID (e.g., "1953036")
        
        val emp = doc.toObject(Employee::class.java)
        return if (emp != null) {
            // Ensure kgid is set - use document ID if field is missing/empty
            val finalKgid = if (emp.kgid.isNotBlank()) emp.kgid else docKgid
            val blob = SearchUtils.generateSearchBlob(
                finalKgid, emp.name, emp.mobile1, emp.mobile2, emp.rank, emp.metalNumber, 
                emp.district, emp.station, emp.unit, emp.bloodGroup
            )

            EmployeeEntity(
                kgid = finalKgid, // ✅ Always has a value (either from field or document ID)
                name = emp.name ?: "",
                email = emp.email ?: "",
                mobile1 = emp.mobile1 ?: "",
                mobile2 = emp.mobile2 ?: "",
                rank = emp.rank ?: "",
                metalNumber = emp.metalNumber ?: "",
                district = emp.district ?: "",
                station = emp.station ?: "",
                bloodGroup = emp.bloodGroup ?: "",
                photoUrl = emp.photoUrl ?: "",
                photoUrlFromGoogle = emp.photoUrlFromGoogle ?: "",
                fcmToken = emp.fcmToken ?: "",
                isAdmin = emp.isAdmin,
                isApproved = emp.isApproved ?: true, // Default to true if missing
                firebaseUid = emp.firebaseUid ?: "",
                createdAt = emp.createdAt ?: Date(),
                updatedAt = emp.updatedAt ?: Date(),
                pin = pinHash,
                unit = emp.unit,
                searchBlob = blob,
                isManualStation = emp.isManualStation
            )
        } else {
            // If doc couldn't be mapped to Employee, build from fields directly
            // ✅ CRITICAL FIX: Use document ID as kgid if field is missing
            val fieldKgid = doc.getString("kgid")
            val finalKgid = if (!fieldKgid.isNullOrBlank()) fieldKgid else docKgid
            val name = doc.getString("name") ?: ""
            val email = doc.getString("email") ?: ""
            val m1 = doc.getString("mobile1") ?: ""
            val m2 = doc.getString("mobile2") ?: ""
            val rank = doc.getString("rank") ?: ""
            val metal = doc.getString("metalNumber") ?: doc.getString("metal") ?: ""
            val dist = doc.getString("district") ?: ""
            val station = doc.getString("station") ?: ""
            val blood = doc.getString("bloodGroup") ?: ""
            val unit = doc.getString("unit") ?: ""

             val blob = SearchUtils.generateSearchBlob(
                finalKgid, name, m1, m2, rank, metal, dist, station, unit, blood
            )

            EmployeeEntity(
                kgid = finalKgid, // ✅ Always has a value (either from field or document ID)
                name = name,
                email = email,
                mobile1 = m1,
                mobile2 = m2,
                rank = rank,
                metalNumber = metal,
                district = dist,
                station = station,
                bloodGroup = blood,
                photoUrl = doc.getString("photoUrl") ?: "",
                photoUrlFromGoogle = doc.getString("photoUrlFromGoogle") ?: "",
                fcmToken = doc.getString("fcmToken") ?: "",
                isAdmin = doc.getBoolean("isAdmin") ?: false,
                isApproved = doc.getBoolean("isApproved") ?: true, // Default to true if missing
                firebaseUid = doc.getString("firebaseUid") ?: "",
                createdAt = doc.getDate("createdAt") ?: Date(),
                updatedAt = doc.getDate("updatedAt") ?: Date(),
                pin = pinHash,
                unit = unit,
                searchBlob = blob,
                isManualStation = doc.getBoolean("isManualStation") ?: false
            )
        }
    }

    private fun mapEmployeeFromResponse(data: Map<*, *>): Employee {
        fun str(key: String): String = data[key]?.toString().orEmpty()
        fun bool(key: String): Boolean = when (val value = data[key]) {
            is Boolean -> value
            else -> value?.toString()?.toBoolean() ?: false
        }

        return Employee(
            kgid = str("kgid"),
            name = str("name"),
            email = str("email"),
            pin = str("pin").ifBlank { null },
            mobile1 = str("mobile1").ifBlank { null },
            mobile2 = str("mobile2").ifBlank { null },
            rank = str("rank").ifBlank { null },
            metalNumber = str("metalNumber").ifBlank { null },
            district = str("district").ifBlank { null },
            station = str("station").ifBlank { null },
            bloodGroup = str("bloodGroup").ifBlank { null },
            photoUrl = str("photoUrl").ifBlank { null },
            photoUrlFromGoogle = str("photoUrlFromGoogle").ifBlank { null },
            fcmToken = str("fcmToken").ifBlank { null },
            firebaseUid = str("firebaseUid").ifBlank { null },
            isAdmin = bool("isAdmin"),
            isApproved = data["isApproved"] as? Boolean ?: true,
            unit = str("unit").ifBlank { null }
        )
    }

    // build EmployeeEntity from POJO (no pin)
    private fun buildEmployeeEntityFromPojo(emp: Employee, pinHash: String = ""): EmployeeEntity {
        val blob = SearchUtils.generateSearchBlob(
            emp.kgid, emp.name, emp.mobile1, emp.mobile2, emp.rank, emp.metalNumber,
            emp.district, emp.station, emp.unit, emp.bloodGroup
        )
        return EmployeeEntity(
            kgid = emp.kgid,
            name = emp.name ?: "",
            email = emp.email ?: "",
            mobile1 = emp.mobile1 ?: "",
            mobile2 = emp.mobile2 ?: "",
            rank = emp.rank ?: "",
            metalNumber = emp.metalNumber ?: "",
            district = emp.district ?: "",
            station = emp.station ?: "",
            bloodGroup = emp.bloodGroup ?: "",
            photoUrl = emp.photoUrl ?: "",
            photoUrlFromGoogle = emp.photoUrlFromGoogle ?: "",
            fcmToken = emp.fcmToken ?: "",
            isAdmin = emp.isAdmin,
            isApproved = emp.isApproved,
            firebaseUid = emp.firebaseUid ?: "",
            createdAt = emp.createdAt ?: Date(),
            updatedAt = emp.updatedAt ?: Date(),
            pin = pinHash,
            unit = emp.unit,
            searchBlob = blob,
            isManualStation = emp.isManualStation
        )
    }



    // -------------------------
    // Additional helpers expected by ViewModel / calling code
    // -------------------------
    // For compatibility with viewmodel usage in your error list
    fun loginUser(email: String, pin: String): Flow<RepoResult<Employee>> = loginUserWithPin(email, pin)

    suspend fun updateUserPin(
        email: String,
        oldPin: String? = null,
        newPin: String,
        isForgot: Boolean = false
    ): RepoResult<String> {
        return try {
            Log.d("UpdatePinFlow", "Updating PIN for $email (isForgot=$isForgot)")

            // Step 1: Prepare the request payload
            val hashedNewPin = PinHasher.hashPassword(newPin)
            val data = mutableMapOf(
                "email" to email,
                "newPinHash" to hashedNewPin,
                "isForgot" to isForgot
            )

            if (!isForgot && oldPin != null) {
                val hashedOldPin = PinHasher.hashPassword(oldPin)
                data["oldPinHash"] = hashedOldPin
            }

            // Step 2: Call Firebase Cloud Function (✅ correct function name)
            val result = functions
                .getHttpsCallable("updateUserPin")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            Log.d("UpdatePinFlow", "✅ updateUserPin response: $response")

            if (response != null && response["success"] == true) {
                // Step 3: Update local Room cache
                employeeDao.updatePinByEmail(email, hashedNewPin)

                Log.d("UpdatePinFlow", "🎯 PIN successfully updated for $email via Cloud Function")
                RepoResult.Success(response["message"].toString())
            } else {
                val msg = response?.get("message")?.toString() ?: "Failed to update PIN."
                RepoResult.Error(null, msg)
            }

        } catch (e: Exception) {
            Log.e("UpdatePinFlow", "❌ Failed to update PIN for $email", e)
            RepoResult.Error(e, "Failed to update PIN: ${e.message}")
        }
    }

    suspend fun refreshEmployees() = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "🔄 Starting refreshEmployees - clearing cache and reloading from Firestore")
            // Clear existing cache
            employeeDao.clearEmployees()
            
            // ✅ FIX: Use pagination to fetch ALL employees (not just first 500)
            val allDocuments = fetchAllEmployeesFromFirestore()
            Log.d(TAG, "📥 Loaded ${allDocuments.size} documents from Firestore")
            
            var processedCount = 0
            val entities = allDocuments.mapNotNull { doc ->
                try {
                    // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
                    val kgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                    val emp = doc.toObject(Employee::class.java)?.copy(kgid = kgid)
                    
                    // Skip deleted employees
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    if (isDeleted) {
                        Log.d(TAG, "Skipping deleted employee: $kgid")
                        return@mapNotNull null
                    }
                    
                    // ✅ Log employee details for debugging (only first 10 to avoid log spam)
                    if (processedCount < 10) {
                        val isApproved = doc.getBoolean("isApproved") ?: false
                        Log.d(TAG, "📋 Employee: kgid=$kgid, name=${emp?.name}, isApproved=$isApproved")
                    }
                    processedCount++
                    
                    emp?.let { 
                        buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty()) 
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing employee document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "✅ Parsed ${entities.size} employees from Firestore")
            if (entities.isNotEmpty()) {
                employeeDao.insertEmployees(entities)
                Log.d(TAG, "✅ Inserted ${entities.size} employees into Room database")
                Log.d(TAG, "✅ Refreshed ${entities.size} employees from Firestore")
            } else {
                Log.w(TAG, "⚠️ No employees found in Firestore after refresh")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to refresh employees from Firestore", e)
        }
    }


    // helper wrapper to get user by email (returns RepoResult for consistency)
    suspend fun getUserByEmail(email: String): RepoResult<Employee?> = withContext(ioDispatcher) {
        try {
            val entity = getEmployeeByEmail(email) // <- This should return EmployeeEntity?
            if (entity != null) {
                RepoResult.Success(entity.toEmployee())  // ✅ Convert Entity -> Model safely
            } else {
                RepoResult.Success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserByEmail failed", e)
            RepoResult.Error(null, "Failed to get user: ${e.message}")
        }
    }

    // check if employee exists by (KGID + email) in Firestore (for forgot PIN, login, etc.)
    suspend fun checkEmployeeExists(kgid: String? = null, email: String? = null): Boolean = withContext(ioDispatcher) {
        try {
            when {
                !kgid.isNullOrBlank() -> {
                    val doc = employeesCollection.document(kgid).get().await()
                    doc.exists()
                }
                !email.isNullOrBlank() -> {
                    val normalizedEmail = email.trim().lowercase()
                    val rawEmail = email.trim()

                    // Try lowercase first
                    var snapshot = employeesCollection
                        .whereEqualTo(FIELD_EMAIL, normalizedEmail)
                        .limit(1)
                        .get()
                        .await()
                    
                    // If not found, try raw email (legacy/case-sensitive fallback)
                    if (snapshot.isEmpty && normalizedEmail != rawEmail) {
                        Log.d(TAG, "checkEmployeeExists: User not found with lowercase, trying raw '$rawEmail'")
                        snapshot = employeesCollection
                            .whereEqualTo(FIELD_EMAIL, rawEmail)
                            .limit(1)
                            .get()
                            .await()
                    }
                    snapshot.documents.isNotEmpty()
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkEmployeeExists failed", e)
            false
        }
    }


    // helper to get employee by kgid locally (used above)
    private suspend fun EmployeeDao.getEmployeeByKgidSafe(kgid: String): EmployeeEntity? {
        // Provide a convenience extension in this file if DAO lacks a direct method; attempts getEmployeeByKgid if exists.
        // If your DAO already has getEmployeeByKgid, this extension won't be used; otherwise you can add it to DAO.
        return try {
            // try direct function if present
            val method = this::class.java.declaredMethods.firstOrNull { it.name == "getEmployeeByKgid" }
            if (method != null) {
                @Suppress("UNCHECKED_CAST")
                method.invoke(this, kgid) as? EmployeeEntity
            } else null
        } catch (t: Throwable) {
            null
        }
    }

    // Provide a typed call for local lookup by kgid (calls DAO method if present)
    suspend fun getLocalEmployeeByKgid(kgid: String): EmployeeEntity? = withContext(ioDispatcher) {
        // If DAO provides a getEmployeeByKgid suspend function (recommended), it will be used directly.
        // We attempt to call it reflectively to avoid compilation errors if your DAO defines it.
        return@withContext try {
            // simple attempt: many DAOs implement a suspend fun getEmployeeByKgid(kgid: String): EmployeeEntity?
            val method = employeeDao::class.java.methods.firstOrNull { it.name == "getEmployeeByKgid" }
            if (method != null) {
                @Suppress("UNCHECKED_CAST")
                method.invoke(employeeDao, kgid) as? EmployeeEntity
            } else null
        } catch (t: Throwable) {
            null
        }
    }
    suspend fun getEmployeeDirect(email: String): Employee? = withContext(ioDispatcher) {
        val entity = getEmployeeByEmail(email)
        entity?.toEmployee()
    }
    
    /**
     * ✅ Insert EmployeeEntity directly into local DB
     * Used for refreshing current user after profile update
     */
    suspend fun insertEmployeeDirect(entity: EmployeeEntity) = withContext(ioDispatcher) {
        try {
            employeeDao.insertEmployee(entity)
            Log.d(TAG, "✅ Inserted employee entity: kgid=${entity.kgid}, metalNumber=${entity.metalNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to insert employee entity: ${e.message}", e)
        }
    }

    /**
     * ✅ Update firebaseUid in Firestore for the current authenticated user
     * Used after Google Sign-In or when firebaseUid needs to be synced
     */
    suspend fun updateFirebaseUidForCurrentUser(email: String): RepoResult<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
            val firebaseUid = currentUser?.uid
            if (firebaseUid == null) {
                return@withContext RepoResult.Error(null, "No authenticated Firebase user")
            }

            // Find employee by email
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            if (snapshot.isEmpty) {
                return@withContext RepoResult.Error(null, "Employee not found for email: $email")
            }

            val doc = snapshot.documents.first()
            val currentFirebaseUid = doc.getString("firebaseUid")
            
            // Update if different
            if (currentFirebaseUid != firebaseUid) {
                doc.reference.update("firebaseUid", firebaseUid).await()
                Log.d(TAG, "✅ Updated firebaseUid in Firestore for $email: $firebaseUid")
                
                // Update local cache
                val kgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                val localEmp = employeeDao.getEmployeeByEmail(email)
                if (localEmp != null) {
                    employeeDao.insertEmployee(localEmp.copy(firebaseUid = firebaseUid))
                }
            }
            
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update firebaseUid: ${e.message}", e)
            RepoResult.Error(e, "Failed to update firebaseUid: ${e.message}")
        }
    }

}
