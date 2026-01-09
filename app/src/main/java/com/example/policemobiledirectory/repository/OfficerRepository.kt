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
    fun getOfficers(): Flow<RepoResult<List<Officer>>> = flow {
        emit(RepoResult.Loading)
        try {
            val snapshot = officersCollection.get().await()
            val officers = snapshot.documents.mapNotNull { doc ->
                try {
                    val off = doc.toObject(Officer::class.java)?.copy(agid = doc.id)
                    // Filter hidden officers
                    if (off?.isHidden == true) null else off
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing officer ${doc.id}: ${e.message}")
                    null
                }
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
    /**
     * Search officers by query and filter
     */
    fun searchOfficers(query: String, filter: String): Flow<RepoResult<List<Officer>>> = flow {
        emit(RepoResult.Loading)
        try {
            val snapshot = officersCollection.get().await()
            val allOfficers = snapshot.documents.mapNotNull { doc ->
                try {
                    val off = doc.toObject(Officer::class.java)?.copy(agid = doc.id)
                    if (off?.isHidden == true) null else off
                } catch (e: Exception) {
                    null
                }
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
}

