package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.repository.OfficerRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditOfficerViewModel @Inject constructor(
    private val officerRepository: OfficerRepository,
    private val imageRepository: ImageRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _officer = MutableStateFlow<Officer?>(null)
    val officer = _officer.asStateFlow()

    private val _saveStatus = MutableStateFlow<RepoResult<Boolean>?>(null)
    val saveStatus = _saveStatus.asStateFlow()

    private val _photoUploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val photoUploadStatus = _photoUploadStatus.asStateFlow()

    init {
        savedStateHandle.get<String>("officerId")?.let { officerId ->
            if (officerId.isNotEmpty()) {
                viewModelScope.launch {
                    officerRepository.getOfficers().collect { result ->
                        if (result is RepoResult.Success) {
                            _officer.value = result.data?.find { it.agid == officerId }
                        }
                    }
                }
            }
        }
    }

    fun saveOfficer(officer: Officer, newPhotoUri: Uri?) {
        viewModelScope.launch {
            try {
                var updatedOfficer = officer
                var uploadFailed = false

                // Upload new photo if provided
                if (newPhotoUri != null) {
                    // Use AGID as the identifier for officer images too
                    val imageId = officer.agid.takeIf { it.isNotBlank() } ?: "temp_${System.currentTimeMillis()}"
                    
                    imageRepository.uploadOfficerImage(newPhotoUri, imageId).collect { status ->
                        _photoUploadStatus.value = status
                        when (status) {
                            is OperationStatus.Success -> {
                                status.data?.let { url ->
                                    updatedOfficer = updatedOfficer.copy(photoUrl = url)
                                }
                            }
                            is OperationStatus.Error -> {
                                _saveStatus.value = RepoResult.Error(message = status.message)
                                uploadFailed = true
                            }
                            else -> Unit
                        }
                    }
                }

                if (uploadFailed) {
                    _photoUploadStatus.value = OperationStatus.Idle
                    return@launch
                }
                
                _photoUploadStatus.value = OperationStatus.Idle

                // Save to Firestore
                _saveStatus.value = RepoResult.Loading
                officerRepository.addOrUpdateOfficer(updatedOfficer).collect { result ->
                    _saveStatus.value = result
                }

            } catch (e: Exception) {
                _saveStatus.value = RepoResult.Error(message = "Unexpected error: ${e.message}")
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = null
    }
}
