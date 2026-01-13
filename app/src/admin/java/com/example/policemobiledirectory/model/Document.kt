package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class Document(
    @SerializedName("Title")
    val title: String? = null,
    
    @SerializedName("title")
    val titleLower: String? = null,

    @SerializedName("URL")
    val url: String? = null,
    
    @SerializedName("url")
    val urlLower: String? = null,
    
    @SerializedName("Url")
    val urlMixed: String? = null,

    @SerializedName("Category")
    val category: String? = null,
    
    @SerializedName("category")
    val categoryLower: String? = null,

    // ✅ Renamed field, mapped to "Uploaded By" in JSON
    @SerializedName("Uploaded By")
    val uploadedBy: String? = null,
    
    @SerializedName("UploadedBy")
    val uploadedByNoSpace: String? = null,
    
    @SerializedName("uploadedBy")
    val uploadedByLower: String? = null,

    // ✅ Renamed field, mapped to "Uploaded Date" in JSON
    @SerializedName("Uploaded Date")
    val uploadedDate: String? = null,
    
    @SerializedName("UploadedDate")
    val uploadedDateNoSpace: String? = null,
    
    @SerializedName("uploadedDate")
    val uploadedDateLower: String? = null,

    @SerializedName("Description")
    val description: String? = null,
    
    @SerializedName("description")
    val descriptionLower: String? = null,

    @SerializedName("Delete")
    val delete: String? = null,
    
    @SerializedName("delete")
    val deleteLower: String? = null,
    
    val fileId: String? = null,
    val fileType: String? = null,
    val thumbnailUrl: String? = null
) {
    // Computed properties with fallbacks to handle field name variations
    val resolvedTitle: String
        get() = (title ?: titleLower)?.takeIf { it.isNotBlank() } ?: "Untitled Document"
    
    val resolvedUrl: String?
        get() = (url ?: urlLower ?: urlMixed)?.takeIf { it.isNotBlank() }
    
    val resolvedCategory: String?
        get() = (category ?: categoryLower)?.takeIf { it.isNotBlank() }
    
    val resolvedUploadedBy: String?
        get() = (uploadedBy ?: uploadedByNoSpace ?: uploadedByLower)?.takeIf { it.isNotBlank() }
    
    val resolvedUploadedDate: String?
        get() = (uploadedDate ?: uploadedDateNoSpace ?: uploadedDateLower)?.takeIf { it.isNotBlank() }
    
    val resolvedDescription: String?
        get() = (description ?: descriptionLower)?.takeIf { it.isNotBlank() }
    
    /**
     * Check if this document has valid data for display
     */
    val isValid: Boolean
        get() = !resolvedUrl.isNullOrBlank()
}
