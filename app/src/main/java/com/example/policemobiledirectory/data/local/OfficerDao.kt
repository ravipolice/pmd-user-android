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

    // ---------- SMART RANKED SEARCH ----------
    @Query("""
        SELECT * FROM officers
        WHERE searchBlob LIKE '%' || :query || '%'
        ORDER BY
        CASE
            WHEN LOWER(name) = :query THEN 1                   -- Exact Name Match
            WHEN LOWER(name) LIKE :query || '%' THEN 2         -- Name Starts With
            WHEN LOWER(rank) LIKE '%' || :query || '%' THEN 3  -- Rank Contains
            WHEN LOWER(station) LIKE '%' || :query || '%' THEN 4 -- Station Contains
            ELSE 5                                             -- General Blob Match
        END ASC,
        name ASC
        LIMIT 100
    """)
    fun smartSearch(query: String): Flow<List<OfficerEntity>>

    @Query("SELECT * FROM officers WHERE searchBlob LIKE :query ORDER BY name ASC LIMIT 100")
    fun searchByBlob(query: String): Flow<List<OfficerEntity>>
}
