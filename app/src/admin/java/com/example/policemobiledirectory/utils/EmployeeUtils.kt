package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee
import androidx.compose.ui.graphics.Color

// Function to filter employees
fun filterEmployees(
    employees: List<Employee>,
    query: String,
    field: SearchFilter
): List<Employee> {
    if (query.isBlank()) return employees

    val lowerQuery = query.lowercase()

    return employees.filter { emp ->
        when (field) {
            SearchFilter.ALL -> {
                emp.name.lowercase().contains(lowerQuery) ||
                emp.kgid.lowercase().contains(lowerQuery) ||
                emp.station?.lowercase()?.contains(lowerQuery) == true ||
                emp.rank?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile1?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile2?.lowercase()?.contains(lowerQuery) == true
            }
            SearchFilter.NAME -> emp.name.lowercase().contains(lowerQuery)
            SearchFilter.KGID -> emp.kgid.lowercase().contains(lowerQuery)
            SearchFilter.STATION -> emp.station?.lowercase()?.contains(lowerQuery) == true
            SearchFilter.RANK -> emp.rank?.lowercase()?.contains(lowerQuery) == true
            SearchFilter.MOBILE -> {
                emp.mobile1?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile2?.lowercase()?.contains(lowerQuery) == true
            }
            SearchFilter.BLOOD_GROUP -> {
                getFormattedBloodGroup(emp.bloodGroup).lowercase().contains(lowerQuery) ||
                (emp.bloodGroup ?: "").lowercase().contains(lowerQuery)
            }
        }
    }
}

/**
 * Normalizes blood group string for display and matching.
 * E.g., "O Positive" -> "O+", "o-" -> "O–"
 */
fun getFormattedBloodGroup(bloodGroup: String?): String {
    val bg = bloodGroup ?: return "??"
    if (bg.trim() == "??" || bg.isBlank()) return "??"
    
    return bg.uppercase()
        .replace("POSITIVE", "+")
        .replace("NEGATIVE", "–")
        .replace("VE", "")
        .replace("(", "")
        .replace(")", "")
        .trim()
        .let { clean ->
            when (clean) {
                "A" -> "A+"
                "B" -> "B+"
                "O" -> "O+"
                "AB" -> "AB+"
                "A-" -> "A–"
                "B-" -> "B–"
                "O-" -> "O–"
                "AB-" -> "AB–"
                else -> clean
            }
        }
}

/**
 * Returns color based on blood group priority/rarity.
 */
fun getBloodGroupColor(bloodGroup: String?): Color {
    val formatted = getFormattedBloodGroup(bloodGroup)
    return when (formatted) {
        "O–" -> Color(0xFFD32F2F)    // Red
        "AB–" -> Color(0xFFF57C00)   // Deep Orange
        "A–", "B–" -> Color(0xFFFB8C00) // Orange
        "??", "?" -> Color(0xFF9E9E9E)  // Grey
        else -> {
            if (formatted.contains("+")) {
                Color(0xFF1976D2) // Blue for all positive groups
            } else {
                Color(0xFF9E9E9E) // Default Grey
            }
        }
    }
}
