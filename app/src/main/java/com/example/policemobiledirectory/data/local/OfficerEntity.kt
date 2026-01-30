package com.example.policemobiledirectory.data.local

import androidx.room.*

/**
 * OfficerEntity - Local storage for admin-managed Officers
 * Supports unified offline search via searchBlob
 */
@Entity(
    tableName = "officers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["district"]),
        Index(value = ["rank"]),
        Index(value = ["unit"]),
        Index(value = ["searchBlob"])
    ]
)
data class OfficerEntity(
    @PrimaryKey
    val agid: String = "",
    val name: String = "",
    val email: String? = null,
    val rank: String? = null,
    val mobile: String? = null,
    val landline: String? = null,
    val station: String? = null,
    val district: String? = null,
    val unit: String? = null,
    val photoUrl: String? = null,
    val bloodGroup: String? = null,
    val isHidden: Boolean = false,
    val searchBlob: String = ""
)
