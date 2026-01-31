package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_registrations")
data class PendingRegistrationEntity(
    @PrimaryKey(autoGenerate = true) val roomId: Long = 0,
    val kgid: String = "",
    val firestoreId: String? = null,
    val name: String = "",
    val email: String = "",
    val pin: String = "",
    val mobile1: String = "",
    val mobile2: String? = null,
    val rank: String = "",
    val metalNumber: String? = null,
    val district: String = "",
    val station: String = "",
    val unit: String? = null,
    val bloodGroup: String? = null,
    val photoUrl: String? = null,
    val firebaseUid: String = "",
    val isApproved: Boolean = false,
    val status: String = "pending", // pending, approved, rejected
    val rejectionReason: String? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val viewedByAdmin: Boolean = false,
    val photoUrlFromGoogle: String? = null,
    val landline: String? = null,
    val landline2: String? = null,
    val createdAt: java.util.Date? = null,
    val isManualStation: Boolean = false
)
