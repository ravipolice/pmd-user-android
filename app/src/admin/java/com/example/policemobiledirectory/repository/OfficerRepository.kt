package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.model.Officer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfficerRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "OfficerRepository"
    private val officersCollection = firestore.collection("officers")

    /**
     * Get all officers from Firestore
     */
    /**
     * Get all officers from Firestore
     */
    fun getOfficers(): Flow<RepoResult<List<Officer>>> = flow {
        emit(RepoResult.Loading)
        try {
            val snapshot = officersCollection.get().await()
            val officers = snapshot.documents.mapNotNull { doc ->
                safeOfficerFromDoc(doc)
            }
            emit(RepoResult.Success(officers))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching officers: ${e.message}", e)
            emit(RepoResult.Error(e, "Failed to fetch officers: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Search officers by query and filter
     */
    fun searchOfficers(query: String, filter: String): Flow<RepoResult<List<Officer>>> = flow {
        emit(RepoResult.Loading)
        try {
            val snapshot = officersCollection.get().await()
            val allOfficers = snapshot.documents.mapNotNull { doc ->
                safeOfficerFromDoc(doc)
            }
            
            val filtered = allOfficers.filter { it.matches(query, filter) }
            emit(RepoResult.Success(filtered))
        } catch (e: Exception) {
            Log.e(TAG, "Error searching officers: ${e.message}", e)
            emit(RepoResult.Error(e, "Failed to search officers: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Add or Update Officer
     * - Uses set(..., SetOptions.merge()) to create or update
     */
    suspend fun addOrUpdateOfficer(officer: Officer): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            // Use existing agid or generate a new one (though typically AGID comes from external source, 
            // for app-created/edited ones we can use existing ID or a temp one if creating new)
            val docId = officer.agid.takeIf { it.isNotBlank() } ?: officersCollection.document().id
            
            // Ensure the officer object has the ID set
            val officerToSave = officer.copy(agid = docId)

            officersCollection.document(docId).set(officerToSave).await()
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving officer: ${e.message}", e)
            emit(RepoResult.Error(e, "Failed to save officer: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Delete Officer
     */
    suspend fun deleteOfficer(officerId: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            officersCollection.document(officerId).delete().await()
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting officer: ${e.message}", e)
            emit(RepoResult.Error(e, "Failed to delete officer: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Robust parsing of Officer document
     * Handles type mismatches (e.g., Number vs String) gracefully
     */
    private fun safeOfficerFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): Officer? {
        // 1. Try standard parsing first
        try {
            val officer = doc.toObject(Officer::class.java)
            if (officer != null) {
                // Filter hidden
                return if (officer.isHidden) null else officer.copy(agid = doc.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard parsing failed for ${doc.id}, attempting manual fallback: ${e.message}")
        }

        // 2. Manual fallback for type mismatches (e.g. Number -> String)
        try {
            val data = doc.data ?: return null

            // Helper to safely get string even if field is Number/Boolean
            fun getString(key: String): String? {
                val value = data[key] ?: return null
                return value.toString().trim()
            }
            
            // Helper for boolean
            fun getBoolean(key: String, default: Boolean): Boolean {
                 val value = data[key] ?: return default
                 return when(value) {
                     is Boolean -> value
                     is String -> value.toBoolean()
                     else -> default
                 }
            }

            // Note: If 'isHidden' is true (or string "true"), skip it
            val isHidden = getBoolean("isHidden", false)
            if (isHidden) return null

            return Officer(
                agid = doc.id,
                name = getString("name") ?: "",
                email = getString("email"),
                bloodGroup = getString("bloodGroup"),
                mobile = getString("mobile"), 
                landline = getString("landline"),
                rank = getString("rank"),
                station = getString("station"),
                district = getString("district"),
                photoUrl = getString("photoUrl"),
                unit = getString("unit"),
                isHidden = isHidden
            )

        } catch (e: Exception) {
            Log.e(TAG, "Manual parsing failed completely for ${doc.id}: ${e.message}")
            return null
        }
    }
}

