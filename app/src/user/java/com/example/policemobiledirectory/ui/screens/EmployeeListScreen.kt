@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.ui.theme.ErrorRed
import com.example.policemobiledirectory.ui.theme.CardShadow
import com.example.policemobiledirectory.ui.theme.BorderTeal
import com.example.policemobiledirectory.ui.theme.ChipSelectedStart
import com.example.policemobiledirectory.ui.theme.ChipSelectedEnd
import com.example.policemobiledirectory.ui.theme.ChipUnselected
import com.example.policemobiledirectory.ui.theme.BorderChipUnselected
import com.example.policemobiledirectory.ui.theme.GlassOpacity
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.ui.theme.PrimaryTealDark
import com.example.policemobiledirectory.ui.theme.FABColor
import com.example.policemobiledirectory.ui.theme.BorderTeal
import com.example.policemobiledirectory.ui.theme.BackgroundLight
import com.example.policemobiledirectory.ui.theme.SecondaryYellow
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel

import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch

import com.example.policemobiledirectory.ui.components.EmployeeCardUser
import com.example.policemobiledirectory.ui.components.ContactCard
import com.example.policemobiledirectory.ui.components.SearchFilterBar
import kotlinx.coroutines.CoroutineScope
import com.example.policemobiledirectory.navigation.Routes
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    constantsViewModel: ConstantsViewModel = hiltViewModel(),
    notificationsViewModel: com.example.policemobiledirectory.viewmodel.NotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    
    // Notification counts
    val notifications by notificationsViewModel.notifications.collectAsStateWithLifecycle()
    val notificationCount = notifications.count { !it.isRead }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) { 
        // Only refresh if we're on the employee list screen
        if (currentRoute == Routes.EMPLOYEE_LIST) {
            viewModel.checkIfAdmin()
            // Refresh data when screen comes back into focus
            viewModel.refreshEmployees()
            viewModel.refreshOfficers()
        }
        // Constants.kt is the primary source - no automatic syncing
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(

                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PMD Home")
                        Spacer(Modifier.width(6.dp))
                        Box {
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                            // Notification badge - circular with white border (Yellow theme)
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .offset(x = 12.dp, y = (-12).dp)
                                        .background(
                                            color = SecondaryYellow, // Yellow for notification badge
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.White,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Will use gradient background
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.background(
                    brush = Brush.linearGradient(
                        listOf(
                            PrimaryTeal.copy(alpha = GlassOpacity),
                            PrimaryTealDark.copy(alpha = GlassOpacity)
                        )
                    )
                ),
                actions = {
                    IconButton(onClick = { 
                        viewModel.refreshEmployees()
                        viewModel.refreshOfficers()
                        constantsViewModel.forceRefresh()  // ← Also refresh constants from Google Sheet
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    // Single Font Size Button with Dropdown Menu
                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale ->
                            viewModel.setFontScale(scale)
                        },
                        onFontScaleToggle = {
                            // Cycle through common presets: 0.8, 1.0, 1.2, 1.4, 1.6, 1.8
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { 
                                kotlin.math.abs(it - current) < 0.05f 
                            }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) {
                                currentIndex + 1
                            } else {
                                0 // Cycle back to first
                            }
                            viewModel.setFontScale(presets[nextIndex])
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    IconButton(onClick = onThemeToggle) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {}
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = BackgroundLight // Light off-white background
        ) {
            EmployeeListContent(
                navController = navController,
                viewModel = viewModel,
                constantsViewModel = constantsViewModel,
                context = context,
                isAdmin = isAdmin,
                fontScale = fontScale,
                snackbarHostState = snackbarHostState,

            )
        }
    }
}

@Composable
private fun EmployeeListContent(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
    context: Context,
    isAdmin: Boolean,
    fontScale: Float,
    snackbarHostState: SnackbarHostState,

) {
    val coroutineScope = rememberCoroutineScope()
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val officerStatus by viewModel.officerStatus.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Get constants from ViewModel
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val units by constantsViewModel.units.collectAsStateWithLifecycle()
    val fullUnits by constantsViewModel.fullUnits.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    // 🔹 UNIT SELECTION STATE
    var selectedUnit by remember { mutableStateOf("All") }
    // var unitExpanded removed
    val selectedUnitState by viewModel.selectedUnit.collectAsStateWithLifecycle()
    
    // Sync selectedUnit with ViewModel
    LaunchedEffect(selectedUnitState) {
        selectedUnit = selectedUnitState
    }

    val ksrpBattalions by constantsViewModel.ksrpBattalions.collectAsStateWithLifecycle()

    // 🔹 DYNAMIC SECTIONS STATE
    // Fetches sections for the selected unit (e.g. State INT -> Special Branch, etc.)
    val unitSections by produceState<List<String>>(initialValue = emptyList(), key1 = selectedUnit) {
        if (selectedUnit != "All" && selectedUnit.isNotBlank()) {
            value = constantsViewModel.getSectionsForUnit(selectedUnit)
        } else {
            value = emptyList()
        }
    }

    // Check if selected unit is District Level (hides Station)
    val isDistrictLevelUnit by produceState(initialValue = false, key1 = selectedUnit) {
        value = constantsViewModel.isDistrictLevelUnit(selectedUnit)
    }

    var selectedDistrict by remember { mutableStateOf("All") }
    // var districtExpanded removed

    // Filter Units based on selected District
    val filteredUnitNames = remember(fullUnits, units, selectedDistrict) {
        if (selectedDistrict == "All" || selectedDistrict.isBlank()) {
             units
        } else {
             if (fullUnits.isNotEmpty()) {
                 fullUnits.filter { u ->
                     when (u.mappingType) {
                         "all", "" -> true
                         "none" -> true
                         "state" -> selectedDistrict == "HQ"
                         "commissionerate", "single", "subset" -> u.mappedDistricts.any { it.equals(selectedDistrict, ignoreCase = true) }
                         else -> true 
                     }
                 }.map { it.name }.sorted()
             } else {
                 units
             }
        }
    }

    // Show "All" only for admins, regular users see only districts
    // LOGIC CHANGE: If Unit is KSRP, show KSRP Battalions instead of Districts
    // UNIT-TO-DISTRICT MAPPING LOGIC
    val districtsList = remember(isAdmin, districts, ksrpBattalions, selectedUnit, fullUnits) {
        // Find configuration for the selected unit
        val unitConfig = fullUnits.find { it.name == selectedUnit }
        
        val baseList = when {
            selectedUnit == "All" -> districts // Show all for "All"
            unitConfig != null -> {
                when (unitConfig.mappingType) {
                    "subset", "single", "commissionerate" -> {
                         // Use mapped districts from configuration
                         if (unitConfig.mappedDistricts.isNotEmpty()) {
                             unitConfig.mappedDistricts.sorted()
                         } else {
                             // Default to all districts excluding KSRP if mapping is missing
                             districts.filter { !ksrpBattalions.contains(it) }
                         }
                    }
                    "none" -> emptyList() // No districts for this unit (e.g. State Level)
                    "state" -> listOf("HQ") // Example for state level
                    else -> {
                        // "all" or unknown -> All districts, but strictly exclude KSRP battalions
                        districts.filter { !ksrpBattalions.contains(it) }
                    }
                }
            }
            // Fallback: If no config found, strictly exclude KSRP battalions (default behavior)
            // Removed hardcoded "KSRP" check; it must be configured in DB now.
            else -> districts.filter { !ksrpBattalions.contains(it) } 
        }
        
        if (isAdmin) {
            listOf("All") + baseList
        } else {
            baseList
        }
    }

    var selectedStation by remember { mutableStateOf("All") }
    // var stationExpanded removed
    
    // 🔹 FILTER STATIONS BY DISTRICT AND UNIT
    val stationsForDistrict = remember(selectedDistrict, selectedUnit, stationsByDistrict, fullUnits, unitSections) {
        // Find configuration for the selected unit to get stationKeyword
        val unitConfig = fullUnits.find { it.name == selectedUnit }

        // 1. Get stations for the selected district (or all districts if "All")
        val districtStations = if (selectedDistrict == "All") {
             listOf("All") 
        } else {
            val stations = stationsByDistrict[selectedDistrict]
                ?: stationsByDistrict.keys.find { it.equals(selectedDistrict, ignoreCase = true) }?.let { stationsByDistrict[it] }
                ?: emptyList()
            listOf("All") + stations
        }
        
         // 2. Apply unit-specific dynamic filtering
        if (unitSections.isNotEmpty()) {
             // If dynamic sections exist for this unit, use them as the "Station" list
             listOf("All") + unitSections
        } else if (selectedUnit == "All" || selectedUnit == "Law & Order") {
            // "Law & Order" / "All" shows everything in the district
            districtStations
        } else {
             // Dynamic Filtering using stationKeyword from DB
             val keywords = unitConfig?.stationKeyword
                 ?.split(",")
                 ?.map { it.trim() }
                 ?.filter { it.isNotEmpty() }
                 ?: listOf(selectedUnit) // Fallback to Unit Name if no keyword configured

             districtStations.filter { station -> 
                 station == "All" || keywords.any { station.contains(it, ignoreCase = true) }
             }
        }
    }

    var selectedRank by remember { mutableStateOf("All") }
    // var rankExpanded removed
    val allRanks = remember(ranks) { listOf("All") + ranks }
    val selectedRankState by viewModel.selectedRank.collectAsStateWithLifecycle()
    
    // Sync selectedRank with ViewModel
    LaunchedEffect(selectedRankState) {
        selectedRank = selectedRankState
    }

    // Initialize district to user's registered district when currentUser loads (for non-admins)
    // For admins, default to "All"; for regular users, default to their registered district
    LaunchedEffect(currentUser, isAdmin, districts) {
        if (isAdmin) {
            // Admin: default to "All" if not already set
            if (selectedDistrict == "All") {
                viewModel.updateSelectedDistrict("All")
            }
        } else {
            // Regular user: set to their registered district (if it exists in the list)
            val userDistrict = currentUser?.district?.takeIf { it.isNotBlank() }
            if (userDistrict != null && districts.contains(userDistrict)) {
                // User's district is valid and exists in the list
                selectedDistrict = userDistrict
                viewModel.updateSelectedDistrict(userDistrict)
            } else {
                // Fallback: use first district if user has no district set or district is invalid
                val fallbackDistrict = districts.firstOrNull() ?: "All"
                selectedDistrict = fallbackDistrict
                viewModel.updateSelectedDistrict(fallbackDistrict)
            }
        }
    }

    // val searchFields removed

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // 🔹 SEARCH & FILTER BAR
        SearchFilterBar(
            units = filteredUnitNames,
            districts = districtsList,
            stations = stationsForDistrict,
            ranks = allRanks,
            selectedUnit = selectedUnit,
            selectedDistrict = selectedDistrict,
            selectedStation = selectedStation,
            selectedRank = selectedRank,
            onUnitChange = { unit ->
                selectedUnit = unit
                viewModel.updateSelectedUnit(unit)
            },
            onDistrictChange = { district ->
                selectedDistrict = district
                selectedStation = "All"
                viewModel.updateSelectedDistrict(district)
                viewModel.updateSelectedStation("All")
            },
            onStationChange = { station ->
                selectedStation = station
                viewModel.updateSelectedStation(station)
            },
            onRankChange = { rank ->
                selectedRank = rank
                viewModel.updateSelectedRank(rank)
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            searchFilter = searchFilter,
            onSearchFilterChange = { viewModel.updateSearchFilter(it) },
            isDistrictLevelUnit = isDistrictLevelUnit,
            isAdmin = isAdmin,
            modifier = Modifier.padding(bottom = 8.dp)
        )


        // 🔹 UNIFIED CONTACTS LIST (Employees + Officers)
        Box(modifier = Modifier.weight(1f)) {
            when {
                // Show loading for Idle state too (initial launch) to prevent "No contacts" flash
                employeeStatus is OperationStatus.Loading || officerStatus is OperationStatus.Loading || 
                employeeStatus is OperationStatus.Idle || officerStatus is OperationStatus.Idle -> {
                     Box(Modifier.fillMaxSize(), Alignment.Center) {
                         CircularProgressIndicator(color = PrimaryTeal)
                     }
                }
                employeeStatus is OperationStatus.Error || officerStatus is OperationStatus.Error -> {
                    val errorMessage = (employeeStatus as? OperationStatus.Error)?.message 
                        ?: (officerStatus as? OperationStatus.Error)?.message 
                        ?: "Unknown Error"
                        
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refreshEmployees(); viewModel.refreshOfficers() }) {
                            Text("Retry")
                        }
                    }
                }
                filteredContacts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             verticalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             Text(
                                 text = "No contacts found.",
                                 style = MaterialTheme.typography.bodyLarge,
                                 color = Color.Gray,
                                 textAlign = TextAlign.Center
                             )
                             if (searchQuery.isNotEmpty() || selectedDistrict != "All" || selectedUnit != "All") {
                                    TextButton(onClick = {
                                        // Reset to "All" to show ALL contact cards
                                        // Update UI state explicitly
                                        selectedUnit = "All"
                                        selectedStation = "All"
                                        selectedRank = "All"

                                        if (isAdmin) {
                                            selectedDistrict = "All"
                                            viewModel.updateSelectedDistrict("All")
                                        } else {
                                            // Reset to user's registered district
                                            val userDistrict = currentUser?.district?.takeIf { it.isNotBlank() }
                                            if (userDistrict != null && districts.contains(userDistrict)) {
                                                selectedDistrict = userDistrict
                                                viewModel.updateSelectedDistrict(userDistrict)
                                            } else {
                                                selectedDistrict = "All"
                                                viewModel.updateSelectedDistrict("All")
                                            }
                                        }

                                        // Update ViewModel
                                        viewModel.updateSelectedUnit("All")
                                        viewModel.updateSelectedStation("All")
                                        viewModel.updateSelectedRank("All")
                                        viewModel.updateSearchQuery("")
                                    }) {
                                     Text("Reset All Filters")
                                 }
                             }
                         }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            ContactCard(
                                employee = contact.employee,
                                officer = contact.officer,
                                fontScale = fontScale,
                                isAdmin = false,
                                onEdit = null,
                                onClick = {
                                    /* No detailed view action yet */ 
                                }
                            )
                        }
                    }
                }
            }
        }


        // 🔹 DELETE CONFIRMATION + SNACKBAR (Improved with Auto Refresh)


    }
}

/**
 * Single Font Size Selector Button
 * - Click to cycle through common preset sizes (0.8, 1.0, 1.2, 1.4, 1.6, 1.8)
 * - Long press opens menu with all size options for precise selection
 */
@Composable
private fun FontSizeSelectorButton(
    currentFontScale: Float,
    onFontScaleSelected: (Float) -> Unit,
    onFontScaleToggle: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val presetSizes = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f)
    
    Box {
        // Single button with combined clickable for click and long press
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = onFontScaleToggle,
                    onLongClick = { showDropdownMenu = true }
                )
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${(currentFontScale * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Font Size (Click to cycle, Long press for menu)",
                    modifier = Modifier.size(18.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
        
        // Dropdown Menu
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            presetSizes.forEach { size ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${(size * 100).toInt()}%",
                            fontWeight = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onFontScaleSelected(size)
                        showDropdownMenu = false
                    },
                    leadingIcon = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Current size",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
