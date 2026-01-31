package com.example.policemobiledirectory.data.local

import com.example.policemobiledirectory.model.Employee
import java.util.Date

/**
 * Extension function to convert PendingRegistrationEntity to Employee.
 */
fun PendingRegistrationEntity.toEmployee(overridePhotoUrl: String? = null): Employee {
    return Employee(
        kgid = this.kgid,
        name = this.name,
        email = this.email,
        pin = this.pin,
        mobile1 = this.mobile1,
        mobile2 = this.mobile2,
        rank = this.rank,
        metalNumber = this.metalNumber,
        district = this.district,
        station = this.station,
        unit = this.unit,
        bloodGroup = this.bloodGroup,
        photoUrl = overridePhotoUrl ?: this.photoUrl,
        photoUrlFromGoogle = this.photoUrlFromGoogle,
        firebaseUid = this.firebaseUid,
        isApproved = true,
        isAdmin = false,
        createdAt = Date(this.submittedAt), // Convert Long to Date
        updatedAt = Date(), // Current time as updated
        landline = null, // Not present in registration
        landline2 = null,
        isManualStation = this.isManualStation
    )
}
