package com.example.policemobiledirectory.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficerDao {

    @Query("SELECT * FROM officers ORDER BY name ASC")
    fun getAllOfficers(): Flow<List<OfficerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfficer(officer: OfficerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfficers(officers: List<OfficerEntity>)

    @Query("DELETE FROM officers")
    suspend fun clearOfficers()

    @Query("DELETE FROM officers WHERE agid = :agid")
    suspend fun deleteByAgid(agid: String)

    @Query("SELECT * FROM officers WHERE searchBlob LIKE :query ORDER BY name ASC LIMIT 100")
    fun searchByBlob(query: String): Flow<List<OfficerEntity>>
}
