package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.repository.AppIconRepository
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context

/**
 * ViewModel responsible for useful links operations:
 * - Fetching useful links
 * - Adding/deleting useful links
 * - Icon management
 */
@HiltViewModel
class UsefulLinksViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val firestoreInstance = FirebaseFirestore.getInstance()
    private val appIconRepository by lazy { AppIconRepository.create(context) }

    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()



    fun fetchUsefulLinks() {
        viewModelScope.launch {
            try {
                val collection = firestoreInstance.collection("useful_links")
                val snapshot = try {
                    collection.get(Source.SERVER).await()
                } catch (serverError: Exception) {
                    Log.w("UsefulLinks", "Server fetch failed (${serverError.message}), falling back to cache")
                    collection.get(Source.CACHE).await()
                }

                // Temporary list for immediate show (no icons yet)
                _usefulLinks.value = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null
                    link.copy(documentId = doc.id)
                }

                // Fetch icons in background
                val updatedLinks = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null

                    val icon = if (!link.playStoreUrl.isNullOrBlank()) {
                        try {
                            val fetched = appIconRepository.getOrFetchAppIcon(link.playStoreUrl)

                            if (!fetched.isNullOrBlank()) {
                                if (link.iconUrl != fetched) {
                                    try {
                                        collection.document(doc.id).update("iconUrl", fetched).await()
                                        Log.d("IconUpdate", "Updated icon for ${link.name}")
                                    } catch (e: Exception) {
                                        Log.w("IconUpdate", "Failed to save icon for ${link.name}: ${e.message}")
                                    }
                                }
                                fetched
                            } else {
                                link.iconUrl
                            }
                        } catch (e: Exception) {
                            Log.e("IconFetch", "Error fetching icon for ${link.name}: ${e.message}")
                            link.iconUrl
                        }
                    } else {
                        link.iconUrl
                    }

                    link.copy(
                        iconUrl = icon,
                        documentId = doc.id
                    )
                }

                _usefulLinks.value = updatedLinks

            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch useful links: ${e.message}")
                _usefulLinks.value = emptyList()
            }
        }
    }


}



