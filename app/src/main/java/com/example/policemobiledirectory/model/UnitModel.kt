package com.example.policemobiledirectory.model

/**
 * UnitModel
 * Represents a Unit configuration with its scope and mapping rules.
 * Renamed from 'Unit' to avoid conflict with Kotlin's Unit type.
 */
data class UnitModel(
    val id: String = "",
    val name: String = "",
    val isActive: Boolean = true,
    val mappingType: String = "all", // "all", "state", "single", "subset", "commissionerate", "none"
    val mappedDistricts: List<String> = emptyList(),
    val isDistrictLevel: Boolean = false,
    val isHqLevel: Boolean = false,
    val scopes: List<String> = emptyList(), // Legacy support
    val applicableRanks: List<String> = emptyList(),
    val stationKeyword: String = "" // For dynamic filtering (e.g. "DCRB", "ESCOM")
)
