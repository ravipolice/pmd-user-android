package com.example.policemobiledirectory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.repository.DocumentsRepository
import com.example.policemobiledirectory.utils.ErrorHandler
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import kotlinx.coroutines.flow.first

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // State management with OperationStatus
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _documentsStatus = MutableStateFlow<OperationStatus<List<Document>>>(OperationStatus.Idle)
    val documentsStatus: StateFlow<OperationStatus<List<Document>>> = _documentsStatus.asStateFlow()



    // In-memory cache with timestamp
    private var cachedDocuments: List<Document>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Computed properties for convenience
    val isLoading: Boolean get() = _documentsStatus.value is OperationStatus.Loading
    val error: String? get() = (_documentsStatus.value as? OperationStatus.Error)?.message

    fun clearStatus() {
        // Only document load status clearing if needed
    }

    /**
     * Fetch documents with caching, error handling, and performance tracking
     */
    fun fetchDocuments(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Return cached data if available and not expired
            if (!forceRefresh && cachedDocuments != null && 
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS) {
                // Apply local delete filter to cached data too
                val filteredCache = cachedDocuments!!
                _documents.value = filteredCache
                _documentsStatus.value = OperationStatus.Success(filteredCache)
                return@launch
            }

            _documentsStatus.value = OperationStatus.Loading
            
            try {
                val docs = PerformanceLogger.measureNetworkOperation("documents", "GET") {
                    repository.fetchDocuments()
                }
                
                val docList = docs ?: emptyList()
                
                // Update cache
                cachedDocuments = docList
                cacheTimestamp = System.currentTimeMillis()
                
                // ✅ Filter out locally deleted docs
                val filteredList = docList
                
                _documents.value = filteredList
                _documentsStatus.value = OperationStatus.Success(filteredList)
                
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "DocumentsViewModel.fetchDocuments")
                
                // Return cached data if available, even if expired
                if (cachedDocuments != null) {
                    val filteredCache = cachedDocuments!!
                    _documents.value = filteredCache
                    _documentsStatus.value = OperationStatus.Error(
                        "Using cached data. ${errorInfo.userFriendlyMessage}"
                    )
                } else {
                    _documentsStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
                }
                
                // Retry if error is retryable
                if (errorInfo.shouldRetry) {
                    delay(errorInfo.retryDelay)
                    fetchDocuments(forceRefresh = true)
                }
            }
        }
    }


}
