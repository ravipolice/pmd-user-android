package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class GalleryImage(
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

    @SerializedName("Uploaded By")
    val uploadedBy: String? = null,
    
    @SerializedName("UploadedBy")
    val uploadedByNoSpace: String? = null,
    
    @SerializedName("uploadedBy")
    val uploadedByLower: String? = null,

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
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    
    @SerializedName("thumbnail_url")
    val thumbnailUrlUnderscore: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null
) {
    // Computed properties with fallbacks to handle field name variations
    val resolvedTitle: String
        get() = (title ?: titleLower)?.takeIf { it.isNotBlank() } ?: "Untitled"
    
    val resolvedUrl: String?
        get() = (url ?: urlLower ?: urlMixed ?: imageUrl)?.takeIf { it.isNotBlank() }
    
    val resolvedCategory: String?
        get() = (category ?: categoryLower)?.takeIf { it.isNotBlank() }
    
    val resolvedUploadedBy: String?
        get() = (uploadedBy ?: uploadedByNoSpace ?: uploadedByLower)?.takeIf { it.isNotBlank() }
    
    val resolvedUploadedDate: String?
        get() = (uploadedDate ?: uploadedDateNoSpace ?: uploadedDateLower)?.takeIf { it.isNotBlank() }
    
    val resolvedDescription: String?
        get() = (description ?: descriptionLower)?.takeIf { it.isNotBlank() }
    
    val resolvedThumbnailUrl: String?
        get() = (thumbnailUrl ?: thumbnailUrlUnderscore)?.takeIf { it.isNotBlank() }
    
    /**
     * Provides a displayable URL for image loaders like Coil.
     * It prioritizes the lightweight thumbnail and falls back to the full URL.
     * Returns null only if both thumbnailUrl and url are null or blank.
     */
    val displayUrl: String?
        get() {
            val thumb = resolvedThumbnailUrl
            val imgUrl = resolvedUrl
            return thumb ?: imgUrl
        }
    
    /**
     * Check if this image has valid data for display
     */
    val isValid: Boolean
        get() = !resolvedUrl.isNullOrBlank()
}


