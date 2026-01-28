package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import com.example.policemobiledirectory.viewmodel.AddEditOfficerViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun AddEditOfficerScreen(
    officerId: String?,
    navController: NavController,
    viewModel: AddEditOfficerViewModel = hiltViewModel(),
    employeeViewModel: EmployeeViewModel = hiltViewModel() // to refresh list
) {
    val context = LocalContext.current
    val officer by viewModel.officer.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val isNewOfficer = officerId.isNullOrBlank()
    val isSaving = saveStatus is RepoResult.Loading

    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is RepoResult.Success -> {
                Toast.makeText(
                    context,
                    if (isNewOfficer) "Officer saved successfully" else "Officer updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                employeeViewModel.refreshOfficers() // Refresh list
                viewModel.resetSaveStatus()
                navController.popBackStack()
            }
            is RepoResult.Error -> {
                Toast.makeText(
                    context,
                    status.message ?: "Failed to save officer",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetSaveStatus()
            }
            else -> Unit
        }
    }

    // Convert Officer to Employee (for Form)
    // We map Officer fields to Employee fields. 
    // AGID -> KGID
    val initialEmployee = officer?.let { off ->
        Employee(
            kgid = off.agid,
            name = off.name,
            email = off.email ?: "",
            mobile1 = off.mobile ?: "",
            landline = off.landline,
            // landline2 not in officer yet, but good to have if we update officer model
            rank = off.rank ?: "",
            district = off.district ?: "",
            station = off.station ?: "",
            unit = off.unit,
            photoUrl = off.photoUrl
        )
    }

    CommonEmployeeForm(
        isAdmin = true,
        isSelfEdit = false,
        isRegistration = false,
        isOfficer = true, // ✅ Officer Mode
        initialEmployee = initialEmployee,
        initialKgid = officerId,
        onSubmit = { emp, photo ->
            // Convert back to Officer
            val officerToSave = officer?.copy(
                name = emp.name,
                email = emp.email.takeIf { it.isNotBlank() },
                mobile = emp.mobile1?.takeIf { it.isNotBlank() },
                landline = emp.landline?.takeIf { it.isNotBlank() },
                rank = emp.rank,
                district = emp.district,
                station = emp.station,
                unit = emp.unit,
                // Do not overwrite photoUrl here, it's handled by upload within VM if new photo provided
                // or kept same if null. 
                // However, updatedOfficer in VM uses this object. 
                // The VM handles photoUrl update logic.
                // We just pass current photoUrl from form input if it was not changed? 
                // Actually emp.photoUrl comes from initialEmployee if not changed?
                // Let's rely on the VM logic: it takes 'officer' + 'newPhotoUri'.
                // If newPhotoUri is null, it should keep existing photoUrl.
                // We need to ensure we pass the *existing* photoUrl in the officer object we send.
                photoUrl = emp.photoUrl 
            ) ?: Officer( // New Officer
                agid = emp.kgid, // Use KGID input as AGID
                name = emp.name,
                email = emp.email.takeIf { it.isNotBlank() },
                mobile = emp.mobile1?.takeIf { it.isNotBlank() },
                landline = emp.landline?.takeIf { it.isNotBlank() },
                rank = emp.rank,
                district = emp.district,
                station = emp.station,
                unit = emp.unit
            )

            viewModel.saveOfficer(officerToSave, photo)
        },
        isLoading = isSaving
    )
}
