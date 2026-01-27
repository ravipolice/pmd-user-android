package com.example.policemobiledirectory.model

import com.google.firebase.firestore.PropertyName

/**
 * Officer - Read-only contact information for police officers
 * Admin-managed, no authentication required
 * Separate from Employee collection
 */
data class Officer(
    val agid: String = "",           // Auto-generated ID (from Apps Script)
    val name: String = "",
    val email: String? = null,
    val bloodGroup: String? = null,
    val mobile: String? = null,
    val landline: String? = null,
    val rank: String? = null,
    val station: String? = null,
    val district: String? = null,
    val photoUrl: String? = null,
    val unit: String? = null,
    @get:PropertyName("isHidden")
    val isHidden: Boolean = false
) {
    /**
     * ✅ Effective Unit: Hybrid Strategy
     */
    val effectiveUnit: String
        get() {
            if (!unit.isNullOrBlank()) return unit
            
            val stationName = station ?: ""
            return when {
                listOf("Traffic").any { stationName.contains(it, ignoreCase = true) } -> "Traffic"
                listOf("Control Room").any { stationName.contains(it, ignoreCase = true) } -> "Control Room"
                listOf("CEN", "Cyber").any { stationName.contains(it, ignoreCase = true) } -> "CEN Crime / Cyber"
                listOf("Women").any { stationName.contains(it, ignoreCase = true) } -> "Women Police"
                listOf("DPO", "Computer", "Admin", "Office").any { stationName.contains(it, ignoreCase = true) } -> "DPO / Admin"
                listOf("DAR").any { stationName.contains(it, ignoreCase = true) } -> "DAR"
                listOf("DCRB").any { stationName.contains(it, ignoreCase = true) } -> "DCRB"
                listOf("DSB", "Intelligence", "INT").any { stationName.contains(it, ignoreCase = true) } -> "DSB / Intelligence"
                listOf("FPB", "MCU", "SMMC", "DCRE", "Lokayukta", "ESCOM").any { stationName.contains(it, ignoreCase = true) } -> "Special Units"
                else -> "Law & Order"
            }
        }

    val primaryPhone: String?
        get() = mobile ?: landline

    val secondaryPhone: String?
        get() = if (!mobile.isNullOrBlank() && !landline.isNullOrBlank()) landline else null

    /**
     * Optimized matching function (query is already lowercase)
     * Supports filters: name, agid, rank, mobile, district, station, email
     */
    fun matchesOptimized(queryLower: String, filter: String): Boolean {
        return when (filter.lowercase()) {
            "name" -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            "agid" -> {
                val agidLower = agid.lowercase()
                agidLower.startsWith(queryLower) || agidLower.contains(queryLower)
            }
            "rank" -> (rank ?: "").lowercase().contains(queryLower)
            "mobile" -> listOfNotNull(mobile, landline).any { it.lowercase().contains(queryLower) }
            "district" -> (district ?: "").lowercase().contains(queryLower)
            "station" -> (station ?: "").lowercase().contains(queryLower)
            "email" -> (email ?: "").lowercase().contains(queryLower)
            "blood" -> (bloodGroup ?: "").lowercase().contains(queryLower)
            else -> listOfNotNull(
                name, agid, rank, mobile, landline, district, station, email, bloodGroup, unit, effectiveUnit
            ).any { it.lowercase().contains(queryLower) }
        }
    }
    
    /**
     * Legacy function for backward compatibility
     */
    fun matches(query: String, filter: String): Boolean {
        return matchesOptimized(query.trim().lowercase(), filter)
    }
}
