package com.example.policemobiledirectory.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    // ---------- BASIC CRUD ----------
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)
    @Query("UPDATE employees SET pin = :newPin WHERE email = :email")

    suspend fun updatePinByEmail(email: String, newPin: String)

    @Query("DELETE FROM employees")
    suspend fun clearEmployees()

    @Query("DELETE FROM employees WHERE kgid = :kgid")
    suspend fun deleteByKgid(kgid: String)

    @Query("SELECT * FROM employees WHERE kgid = :kgid LIMIT 1")
    suspend fun getEmployeeByKgid(kgid: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE email = :email LIMIT 1")
    suspend fun getEmployeeByEmail(email: String): EmployeeEntity?

    // ---------- SMART RANKED SEARCH ----------
    @Query("""
        SELECT * FROM employees
        WHERE searchBlob LIKE '%' || :query || '%'
        ORDER BY
        CASE
            WHEN LOWER(name) = :query THEN 1                   -- Exact Name Match
            WHEN LOWER(name) LIKE :query || '%' THEN 2         -- Name Starts With
            WHEN LOWER(kgid) LIKE :query || '%' THEN 3         -- KGID Starts With
            WHEN LOWER(rank) LIKE '%' || :query || '%' THEN 4  -- Rank Contains
            WHEN LOWER(station) LIKE '%' || :query || '%' THEN 5 -- Station Contains
            ELSE 6                                             -- General Blob Match
        END ASC,
        name ASC
        LIMIT 100
    """)
    fun smartSearch(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE searchBlob LIKE :query ORDER BY name ASC LIMIT 100")
    fun searchByBlob(query: String): Flow<List<EmployeeEntity>>


    // ---------- FALLBACK SEARCH ----------
    @Query("""
        SELECT * FROM employees
        WHERE LOWER(name) LIKE :query
           OR LOWER(kgid) LIKE :query
           OR LOWER(station) LIKE :query
           OR LOWER(rank) LIKE :query
           OR LOWER(metalNumber) LIKE :query
           OR LOWER(bloodGroup) LIKE :query
           OR LOWER(district) LIKE :query
           OR LOWER(mobile1) LIKE :query
           OR LOWER(mobile2) LIKE :query
        ORDER BY name ASC
        LIMIT 100
    """)
    fun searchAllFields(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(name) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByName(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(kgid) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByKgid(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(mobile1) LIKE :query OR LOWER(mobile2) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByMobile(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(station) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByStation(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(rank) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByRank(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(metalNumber) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByMetalNumber(query: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(bloodGroup) LIKE :query ORDER BY name ASC LIMIT 50")
    fun searchByBloodGroup(query: String): Flow<List<EmployeeEntity>>
}
