package com.example.policemobiledirectory.model

data class Unit(
    val name: String = "",
    val isActive: Boolean = true,
    val applicableRanks: List<String> = emptyList(),
    val isHqLevel: Boolean = false
)
