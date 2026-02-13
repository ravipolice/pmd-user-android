package com.example.policemobiledirectory.model

/**
 * Hybrid Unit-District Mapping Model
 * Used for dynamic UI filtering based on selected unit
 */
data class UnitMapping(
    val unitName: String,
    val mappingType: String = "all", // "all", "subset", "single", "none"
    val mappedDistricts: List<String> = emptyList(),
    val isDistrictLevel: Boolean = false,
    val isHqLevel: Boolean = false,
    val scopes: List<String> = emptyList(),
    val applicableRanks: List<String> = emptyList(),
    val stationKeyword: String? = null,
    val mappedAreaType: String? = null,
    val hideFromRegistration: Boolean = false
)
