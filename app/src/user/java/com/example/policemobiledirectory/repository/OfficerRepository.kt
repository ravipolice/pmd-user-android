package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.data.local.OfficerDao
import com.example.policemobiledirectory.data.local.OfficerEntity
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.utils.SearchUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfficerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val officerDao: OfficerDao
) {
    private val TAG = "OfficerRepository"
    private val officersCollection = firestore.collection("officers")
    private val ioDispatcher = Dispatchers.IO

    /**
     * Get all officers (prioritizes local Room data)
     */
    fun getOfficers(): Flow<RepoResult<List<Officer>>> = officerDao.getAllOfficers()
        .map { entities -> 
            RepoResult.Success(entities.map { it.toOfficer() })
        }
        .flowOn(ioDispatcher)

    /**
     * Sync all officers from Firestore to Room
     */
    suspend fun syncAllOfficers(): RepoResult<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "🔄 Syncing Officers from Firestore to Room...")
            val snapshot = officersCollection.get().await()
            val entities = snapshot.documents.mapNotNull { doc ->
                try {
                    val off = doc.toObject(Officer::class.java)?.copy(agid = doc.id)
                    if (off != null && off.isHidden != true) {
                        off.toEntity()
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing officer ${doc.id}: ${e.message}")
                    null
                }
            }
            
            officerDao.clearOfficers()
            if (entities.isNotEmpty()) {
                officerDao.insertOfficers(entities)
                Log.d(TAG, "✅ Synced ${entities.size} officers to Room")
            }
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing officers: ${e.message}", e)
            RepoResult.Error(e, "Failed to sync officers")
        }
    }

    fun searchByBlob(query: String): Flow<RepoResult<List<Officer>>> {
        val normalizedQuery = query.trim().lowercase()
        return officerDao.smartSearch(normalizedQuery)
            .map { entities -> RepoResult.Success(entities.map { it.toOfficer() }) }
            .flowOn(ioDispatcher)
    }

    /**
     * Legacy Search: Search officers by query and filter (Hits Room via searchBlob fallback or just stay Room-based)
     * For now, we redirect text search to Room.
     */
    fun searchOfficers(query: String, filter: String): Flow<RepoResult<List<Officer>>> = searchByBlob(query)

    suspend fun addOrUpdateOfficer(officer: Officer): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val docId = officer.agid.takeIf { it.isNotBlank() } ?: officersCollection.document().id
            val officerToSave = officer.copy(agid = docId)

            officersCollection.document(docId).set(officerToSave).await()
            
            // Immediately update local cache
            officerDao.insertOfficer(officerToSave.toEntity())
            
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving officer: ${e.message}", e)
            emit(RepoResult.Error(e, "Failed to save officer: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // MAPPERS
    // -------------------------------------------------------------------

    private fun Officer.toEntity(): OfficerEntity {
        val blob = SearchUtils.generateSearchBlob(
            agid, name, mobile, rank, unit, district, station, email, bloodGroup
        )
        return OfficerEntity(
            agid = agid,
            name = name,
            email = email,
            rank = rank,
            mobile = mobile,
            landline = landline,
            station = station,
            district = district,
            unit = unit,
            photoUrl = photoUrl,
            bloodGroup = bloodGroup,
            isHidden = isHidden ?: false,
            searchBlob = blob
        )
    }

    private fun OfficerEntity.toOfficer(): Officer {
        return Officer(
            agid = agid,
            name = name,
            email = email,
            rank = rank,
            mobile = mobile,
            landline = landline,
            station = station,
            district = district,
            unit = unit,
            photoUrl = photoUrl,
            bloodGroup = bloodGroup,
            isHidden = isHidden,
            searchBlob = searchBlob
        )
    }
}

