package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.policemobiledirectory.ui.components.DashboardActionCard
import com.example.policemobiledirectory.ui.components.DashboardStatCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.ui.theme.*
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
fun StaffListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel(),
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    // Refresh data on entry
    LaunchedEffect(Unit) {
        viewModel.refreshEmployees()
        viewModel.refreshOfficers()
    }

    StaffListContent(
        viewModel = viewModel,
        constantsViewModel = constantsViewModel,
        onAddOfficer = { navController.navigate(Routes.ADD_OFFICER) },
        onEditOfficer = { officerId -> navController.navigate("${Routes.ADD_OFFICER}?officerId=$officerId") },
        onDeleteOfficer = { officerId -> viewModel.deleteOfficer(officerId) },
        onBack = { navController.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StaffListContent(
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
    onAddOfficer: () -> Unit,
    onEditOfficer: (String) -> Unit,
    onDeleteOfficer: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Collect Filter Data from ConstantsViewModel
    val unitsList by constantsViewModel.units.collectAsStateWithLifecycle(initialValue = emptyList())
    val districtsList by constantsViewModel.districts.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Collect Search/Filter State from EmployeeViewModel
    val selectedUnitState by viewModel.selectedUnit.collectAsStateWithLifecycle(initialValue = "All")
    val selectedDistrictState by viewModel.selectedDistrict.collectAsStateWithLifecycle(initialValue = "All")
    val selectedStationState by viewModel.selectedStation.collectAsStateWithLifecycle(initialValue = "All")
    val selectedRankState by viewModel.selectedRank.collectAsStateWithLifecycle(initialValue = "All")
    val searchQueryState by viewModel.searchQuery.collectAsStateWithLifecycle(initialValue = "")
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle(initialValue = SearchFilter.NAME)
    val searchFields = SearchFilter.values().toList()
    
    // Collect Filtered Results (Unified Contacts)
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val stationsForDistrict by viewModel.stationsForSelectedDistrict.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle(initialValue = false)
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle(initialValue = 1.0f)
    val cardStyle by viewModel.currentCardStyle.collectAsStateWithLifecycle(initialValue = CardStyle.Vibrant)

    // Local state for dropdown expansion
    var unitExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }

    var localSearchQuery by remember { mutableStateOf(searchQueryState) }

    // Sync local search query with ViewModel
    LaunchedEffect(localSearchQuery) {
        viewModel.updateSearchQuery(localSearchQuery)
    }
    
    LaunchedEffect(searchQueryState) {
        if (localSearchQuery != searchQueryState) {
            localSearchQuery = searchQueryState
        }
    }

    // --- 📋 UNIFIED LIST VIEW ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Staff Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${filteredContacts.size} found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 🔹 ROW 1: UNIT & DISTRICT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // UNIT Dropdown
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnitState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        leadingIcon = { Icon(Icons.Default.Groups, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
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

                // DISTRICT Dropdown
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDistrictState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        (listOf("All") + districtsList).forEach { district ->
                            DropdownMenuItem(text = { Text(district) }, onClick = {
                                viewModel.updateSelectedDistrict(district)
                                districtExpanded = false
                            })
                        }
                    }
                }
            }

            // 🔹 SEARCH BAR
            OutlinedTextField(
                value = localSearchQuery,
                onValueChange = { localSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by ${searchFilter.name.lowercase()}...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTeal) },
                trailingIcon = {
                    if (localSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { localSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 🔹 FILTER CHIPS
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                searchFields.forEach { filter ->
                    FilterChip(
                        selected = searchFilter == filter,
                        onClick = { viewModel.updateSearchFilter(filter) },
                        label = { Text(filter.name.lowercase().capitalize(), fontSize = 11.sp) }
                    )
                }
            }

            // 🔹 CONTACTS LIST
            if (filteredContacts.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No staff found")
                        TextButton(onClick = { viewModel.clearFilters() }) { Text("Clear All Filters") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        if (contact.employee != null) {
                            com.example.policemobiledirectory.ui.theme.components.EmployeeCardAdmin(
                                employee = contact.employee,
                                isAdmin = isAdmin,
                                fontScale = fontScale,
                                navController = NavController(context), // Dummy for now or pass actual
                                onDelete = { /* Handle delete */ },
                                context = context,
                                cardStyle = cardStyle
                            )
                        } else if (contact.officer != null) {
                            ContactCard(
                                officer = contact.officer,
                                fontScale = fontScale,
                                isAdmin = isAdmin,
                                onEdit = { onEditOfficer(contact.id) },
                                onDelete = { onDeleteOfficer(contact.id) }
                            )
                        }
                    }
                }
            }
        }

        // FAB for adding
        FloatingActionButton(
            onClick = onAddOfficer,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Add, "Add Staff")
        }
    }
}
