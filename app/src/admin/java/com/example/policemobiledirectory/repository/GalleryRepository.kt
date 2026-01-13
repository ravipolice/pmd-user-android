package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApiService,
    private val securityConfig: SecurityConfig
) {
    private fun token() = securityConfig.getSecretToken()
    private val gson = Gson()

    suspend fun fetchGalleryImages(): List<GalleryImage> {
        android.util.Log.d("GalleryRepository", "🔄 fetchGalleryImages() called")
        val response: Response<ResponseBody> = api.getGalleryImagesRaw(token = token())
        
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            android.util.Log.e("GalleryRepository", "❌ HTTP ${response.code()}: $errorBody")
            throw IllegalStateException("HTTP ${response.code()}: $errorBody")
        }
        
        val bodyStr = response.body()?.string()
            ?: throw IllegalStateException("Empty gallery response")

        // Log raw response for debugging (first 500 chars)
        val preview = if (bodyStr.length > 500) bodyStr.substring(0, 500) + "..." else bodyStr
        android.util.Log.d("GalleryRepository", "📥 Raw API response (preview): $preview")

        // Try parse as array
        try {
            val listType = object : TypeToken<List<GalleryImage>>() {}.type
            val images = gson.fromJson<List<GalleryImage>>(bodyStr, listType)
            android.util.Log.d("GalleryRepository", "📥 Parsed ${images.size} images as array")
            
            // Log first image structure for debugging
            if (images.isNotEmpty()) {
                val first = images.first()
                android.util.Log.d("GalleryRepository", "📋 First image: title='${first.title}', titleLower='${first.titleLower}', resolvedTitle='${first.resolvedTitle}'")
                android.util.Log.d("GalleryRepository", "📋 First image: url='${first.url}', urlLower='${first.urlLower}', urlMixed='${first.urlMixed}', resolvedUrl='${first.resolvedUrl}'")
                android.util.Log.d("GalleryRepository", "📋 First image: isValid=${first.isValid}, displayUrl='${first.displayUrl}'")
            }
            
            // ✅ Filter out invalid images (those without valid URLs)
            val validImages = images.filter { it.isValid }
            android.util.Log.d("GalleryRepository", "✅ Returning ${validImages.size} valid images (filtered from ${images.size} total)")
            
            if (validImages.isEmpty() && images.isNotEmpty()) {
                android.util.Log.w("GalleryRepository", "⚠️ WARNING: Received ${images.size} images but all were filtered out")
                // Log why each image was filtered
                images.forEachIndexed { index, image ->
                    android.util.Log.w("GalleryRepository", "   Image $index: title='${image.resolvedTitle}', url='${image.resolvedUrl}', isValid=${image.isValid}")
                }
            }
            
            return validImages
        } catch (e: Exception) {
            android.util.Log.w("GalleryRepository", "⚠️ Failed to parse as array: ${e.message}")
            android.util.Log.w("GalleryRepository", "⚠️ Exception type: ${e.javaClass.simpleName}")
            // try object with data/error
        }

        data class GalleryApiResponse(
            val success: Boolean? = null,
            val data: List<GalleryImage>? = null,
            val error: String? = null,
            val message: String? = null
        )

        val obj = try {
            gson.fromJson(bodyStr, GalleryApiResponse::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Unable to parse gallery response: ${e.message}")
        }

        // ✅ Check if response indicates an error
        if (obj.success == false || (obj.error != null || obj.message != null)) {
            val err = obj.error ?: obj.message ?: "Gallery load failed"
            throw IllegalStateException(err)
        }

        obj.data?.let { images ->
            android.util.Log.d("GalleryRepository", "📥 Received ${images.size} images from API response object")
            
            // Log first image structure for debugging
            if (images.isNotEmpty()) {
                val first = images.first()
                android.util.Log.d("GalleryRepository", "📋 First image: title='${first.title}', titleLower='${first.titleLower}', resolvedTitle='${first.resolvedTitle}'")
                android.util.Log.d("GalleryRepository", "📋 First image: url='${first.url}', urlLower='${first.urlLower}', urlMixed='${first.urlMixed}', resolvedUrl='${first.resolvedUrl}'")
                android.util.Log.d("GalleryRepository", "📋 First image: isValid=${first.isValid}, displayUrl='${first.displayUrl}'")
            }
            
            // ✅ Filter out invalid images (those without valid URLs)
            val validImages = images.filter { it.isValid }
            android.util.Log.d("GalleryRepository", "✅ Returning ${validImages.size} valid images (filtered from ${images.size} total)")
            
            if (validImages.isEmpty() && images.isNotEmpty()) {
                android.util.Log.w("GalleryRepository", "⚠️ WARNING: Received ${images.size} images but all were filtered out")
                // Log why each image was filtered
                images.forEachIndexed { index, image ->
                    android.util.Log.w("GalleryRepository", "   Image $index: title='${image.resolvedTitle}', url='${image.resolvedUrl}', isValid=${image.isValid}")
                }
            }
            
            return validImages
        }

        val err = obj.error ?: obj.message ?: "Gallery load failed"
        android.util.Log.e("GalleryRepository", "❌ API error: $err")
        throw IllegalStateException(err)
    }

    suspend fun uploadGalleryImage(request: GalleryUploadRequest) =
        api.uploadGalleryImage(token = token(), request = request)

    suspend fun deleteGalleryImage(request: GalleryDeleteRequest) =
        api.deleteGalleryImage(token = token(), request = request)
}


