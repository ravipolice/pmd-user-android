package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun MyProfileEditScreen(
    navController: NavController? = null,
    employeeViewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentEmployee by employeeViewModel.currentUser.collectAsState()
    val saveStatus by employeeViewModel.saveStatus.collectAsState()
    val photoUploadStatus by employeeViewModel.uploadStatus.collectAsState()
    val isLoading = saveStatus is com.example.policemobiledirectory.repository.RepoResult.Loading || 
                    photoUploadStatus is com.example.policemobiledirectory.utils.OperationStatus.Loading

    // Handle back button to navigate to home screen
    BackHandler(enabled = navController != null) {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController?.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    // Show feedback messages
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is RepoResult.Success -> {
                Toast.makeText(
                    context,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                employeeViewModel.resetSaveStatus() // Reset status
                // ✅ Refresh current user to show updated data (including metal number)
                employeeViewModel.refreshCurrentUser()
                employeeViewModel.refreshEmployees()
                // ✅ Navigate back after successful save
                navController?.popBackStack()
            }
            is RepoResult.Error -> {
                Toast.makeText(
                    context,
                    status.message ?: "Failed to update profile",
                    Toast.LENGTH_LONG
                ).show()
                employeeViewModel.resetSaveStatus()
            }
            else -> {}
        }
    }
    
    // Also log photo upload status changes
    LaunchedEffect(photoUploadStatus) {
        android.util.Log.e("MyProfileScreen", "📸📸📸 photoUploadStatus CHANGED: $photoUploadStatus")
        android.util.Log.e("MyProfileScreen", "photoUploadStatus type: ${photoUploadStatus.javaClass.simpleName}")
    }

    // Check if profile is outdated (older than 90 days)
    val isProfileOutdated = androidx.compose.runtime.remember(currentEmployee) {
        val lastUpdate = currentEmployee?.updatedAt
        if (lastUpdate == null) {
            // If never updated (or new schema), prompt to update
            true
        } else {
            val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
            val diff = System.currentTimeMillis() - lastUpdate.time
            diff > ninetyDaysInMillis
        }
    }

    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        if (isProfileOutdated && currentEmployee != null) {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                ),
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material.icons.Icons.Default.Warning.let { icon ->
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = "Warning",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                    androidx.compose.material3.Text(
                        text = "Please verify your profile details. It has been over 3 months since the last update.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    
        CommonEmployeeForm(
            isAdmin = false,
            isSelfEdit = true,
            isRegistration = false,
            initialEmployee = currentEmployee,
            onNavigateToTerms = null,
            onSubmit = { emp: Employee, photo: Uri? ->
                try {
                    employeeViewModel.saveEmployee(emp, photo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onRegisterSubmit = null,
            isLoading = isLoading
        )
    }
}
