package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.ui.components.ContactCard
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun OfficerListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel(),
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    // Refresh data on entry
    LaunchedEffect(Unit) {
        viewModel.refreshOfficers()
    }

    OfficerListContent(
        viewModel = viewModel,
        constantsViewModel = constantsViewModel,
        onAddOfficer = { navController.navigate(Routes.ADD_OFFICER) },
        onEditOfficer = { officerId -> navController.navigate(Routes.EDIT_OFFICER + "/$officerId") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerListContent(
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
    onAddOfficer: () -> Unit,
    onEditOfficer: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Collect specific officer data for operations
    val officerStatus by viewModel.officerStatus.collectAsStateWithLifecycle()
    val pendingStatus by viewModel.officerPendingStatus.collectAsStateWithLifecycle()

    // Collect Filter Data from ConstantsViewModel
    val unitsList by constantsViewModel.units.collectAsStateWithLifecycle()
    val districtsList by constantsViewModel.districts.collectAsStateWithLifecycle()
    val ranksList by constantsViewModel.ranks.collectAsStateWithLifecycle()
    
    // Collect Search/Filter State from EmployeeViewModel
    val selectedUnitState by viewModel.selectedUnit.collectAsStateWithLifecycle(initialValue = "All")
    val selectedDistrictState by viewModel.selectedDistrict.collectAsStateWithLifecycle(initialValue = "All")
    val selectedStationState by viewModel.selectedStation.collectAsStateWithLifecycle(initialValue = "All")
    val selectedRankState by viewModel.selectedRank.collectAsStateWithLifecycle(initialValue = "All")
    val searchQueryState by viewModel.searchQuery.collectAsStateWithLifecycle(initialValue = "")
    
    // Collect Filtered Results (Unified Contacts)
    // We should filter explicitly for officers OR rely on VM
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()

    // Collect Stations reactive to selected District (and Unit) from VM
    val stationsForDistrict by viewModel.stationsForSelectedDistrict.collectAsStateWithLifecycle()

    // Local state for dropdown expansion
    var unitExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }

    var officerToDelete by remember { mutableStateOf<String?>(null) }
    var localSearchQuery by remember { mutableStateOf(searchQueryState) }

    // Sync local search query with ViewModel (debounce handled in VM)
    LaunchedEffect(localSearchQuery) {
        viewModel.updateSearchQuery(localSearchQuery)
    }
    
    // Sync external changes to search query
    LaunchedEffect(searchQueryState) {
        if (localSearchQuery != searchQueryState) {
            localSearchQuery = searchQueryState
        }
    }

    // Handle delete status
    LaunchedEffect(pendingStatus) {
        when (val status = pendingStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, status.data ?: "Operation successful", Toast.LENGTH_SHORT).show()
                viewModel.resetOfficerPendingStatus()
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.resetOfficerPendingStatus()
            }
            else -> {}
        }
    }

    // Filter to show ONLY Officers from the unified list
    // This allows using the same sophisticated filtering logic (Unit, District, Station, Rank, Search) from EmployeeViewModel
    val finalOfficersList = remember(filteredContacts) {
        filteredContacts.mapNotNull { it.officer }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 🔹 ROW 1: UNIT & DISTRICT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // UNIT
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnitState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Groups, null, Modifier.size(16.dp), PrimaryTeal); Spacer(Modifier.width(4.dp)); Text("Unit") } },
                        leadingIcon = { Icon(Icons.Default.Groups, null, tint = PrimaryTeal) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Red,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        (listOf("All") + unitsList).forEach { unit ->
                            DropdownMenuItem(text = { Text(unit) }, onClick = {
                                viewModel.updateSelectedUnit(unit)
                                unitExpanded = false
                            })
                        }
                    }
                }

                // DISTRICT
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDistrictState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), PrimaryTeal); Spacer(Modifier.width(4.dp)); Text("District") } },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = PrimaryTeal) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Red,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        (listOf("All") + districtsList).forEach { district ->
                            DropdownMenuItem(text = { Text(district) }, onClick = {
                                viewModel.updateSelectedDistrict(district)
                                viewModel.updateSelectedStation("All")
                                districtExpanded = false
                            })
                        }
                    }
                }
            }

            // 🔹 ROW 2: STATION & RANK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // STATION
                ExposedDropdownMenuBox(
                    expanded = stationExpanded,
                    onExpandedChange = { if (selectedDistrictState != "All") stationExpanded = !stationExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedStationState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Business, null, Modifier.size(16.dp), PrimaryTeal); Spacer(Modifier.width(4.dp)); Text("Station") } },
                        leadingIcon = { Icon(Icons.Default.Business, null, tint = PrimaryTeal) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = selectedDistrictState != "All",
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Red,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                        // stationsForDistrict from VM already includes "All" logic and Unit filter
                        stationsForDistrict.forEach { station ->
                            DropdownMenuItem(text = { Text(station) }, onClick = {
                                viewModel.updateSelectedStation(station)
                                stationExpanded = false
                            })
                        }
                    }
                }

                // RANK
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedRankState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MilitaryTech, null, Modifier.size(16.dp), PrimaryTeal); Spacer(Modifier.width(4.dp)); Text("Rank") } },
                        leadingIcon = { Icon(Icons.Default.MilitaryTech, null, tint = PrimaryTeal) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Red,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                        (listOf("All") + ranksList).forEach { rank ->
                            DropdownMenuItem(text = { Text(rank) }, onClick = {
                                viewModel.updateSelectedRank(rank)
                                rankExpanded = false
                            })
                        }
                    }
                }
            }

            // 🔹 ROW 3: SEARCH BAR
            OutlinedTextField(
                value = localSearchQuery,
                onValueChange = { localSearchQuery = it },
                label = { Text("Search by Name, Rank, Station...") },
                placeholder = { Text("Type to search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryTeal) },
                trailingIcon = {
                    if (localSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { localSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red, // Red Border like EmployeeList
                    unfocusedBorderColor = Color.Red,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = PrimaryTeal
                )
            )

            // 🔹 LIST Content
            if (officerStatus is OperationStatus.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (finalOfficersList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No officers found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(finalOfficersList) { officer ->
                        ContactCard(
                            officer = officer,
                            isAdmin = true,
                            onEdit = { onEditOfficer(officer.agid) },
                            onDelete = { officerToDelete = officer.agid }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddOfficer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = PrimaryTeal
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Officer", tint = Color.White)
        }
    }
    
    // Delete Confirmation Dialog
    if (officerToDelete != null) {
        AlertDialog(
            onDismissRequest = { officerToDelete = null },
            title = { Text("Delete Officer") },
            text = { Text("Are you sure you want to delete this officer? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    officerToDelete?.let { viewModel.deleteOfficer(it) }
                    officerToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { officerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
