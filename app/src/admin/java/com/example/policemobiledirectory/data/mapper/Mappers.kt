package com.example.policemobiledirectory.data.mapper

import com.example.policemobiledirectory.data.local.EmployeeEntity
import com.example.policemobiledirectory.data.local.OfficerEntity
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer

fun EmployeeEntity.toEmployee(): Employee = Employee(
    kgid = kgid,
    name = name,
    email = email,
    pin = pin,
    mobile1 = mobile1,
    mobile2 = mobile2,
    rank = rank,
    metalNumber = metalNumber,
    district = district,
    station = station,
    bloodGroup = bloodGroup,
    photoUrl = photoUrl,
    photoUrlFromGoogle = photoUrlFromGoogle,
    fcmToken = fcmToken,
    firebaseUid = firebaseUid,
    isAdmin = isAdmin,
    isApproved = isApproved,
    createdAt = createdAt,
    updatedAt = updatedAt,
    unit = unit,
    searchBlob = searchBlob
)

fun Employee.toEntity(): EmployeeEntity = EmployeeEntity(
    kgid = kgid,
    name = name,
    email = email,
    pin = pin,
    mobile1 = mobile1,
    mobile2 = mobile2,
    rank = rank,
    metalNumber = metalNumber,
    district = district,
    station = station,
    bloodGroup = bloodGroup,
    photoUrl = photoUrl,
    photoUrlFromGoogle = photoUrlFromGoogle,
    fcmToken = fcmToken,
    firebaseUid = firebaseUid,
    isAdmin = isAdmin,
    isApproved = isApproved,
    createdAt = createdAt,
    updatedAt = updatedAt,
    unit = unit,
    searchBlob = searchBlob
)

fun OfficerEntity.toOfficer(): Officer = Officer(
    agid = agid,
    name = name,
    email = email,
    bloodGroup = bloodGroup,
    mobile = mobile,
    landline = landline,
    rank = rank,
    station = station,
    district = district,
    photoUrl = photoUrl,
    unit = unit,
    isHidden = isHidden,
    searchBlob = searchBlob
)

fun Officer.toEntity(searchBlob: String = ""): OfficerEntity = OfficerEntity(
    agid = agid,
    name = name,
    email = email,
    bloodGroup = bloodGroup,
    mobile = mobile,
    landline = landline,
    rank = rank,
    station = station,
    district = district,
    photoUrl = photoUrl,
    unit = unit,
    isHidden = isHidden,
    searchBlob = searchBlob
)
