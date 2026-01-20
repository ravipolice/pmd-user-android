@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel

import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.ui.theme.components.ContactCard
import com.example.policemobiledirectory.ui.theme.components.EmployeeCardAdmin
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.ui.theme.CardStyle
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsState()
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val stationsForDistrict by viewModel.stationsForSelectedDistrict.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    
    // Notification counts
    val userNotifications by viewModel.userNotifications.collectAsState()
    val adminNotifications by viewModel.adminNotifications.collectAsState()
    val userNotificationsSeenAt by viewModel.userNotificationsLastSeen.collectAsState()
    val adminNotificationsSeenAt by viewModel.adminNotificationsLastSeen.collectAsState()
    val notificationCount = if (isAdmin) {
        adminNotifications.count { (it.timestamp ?: 0L) > adminNotificationsSeenAt }
    } else {
        userNotifications.count { (it.timestamp ?: 0L) > userNotificationsSeenAt }
    }

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
            constantsViewModel.forceRefresh()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp), // ✅ Fix: Avoid double status bar padding
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
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
                            // Notification badge
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .offset(x = 12.dp, y = (-12).dp)
                                        .background(
                                            color = SecondaryYellow,
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.White,
                                            shape = CircleShape
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
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                        constantsViewModel.forceRefresh()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    // Font Size Selector
                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale: Float -> viewModel.setFontScale(scale) },
                        onFontScaleToggle = {
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { kotlin.math.abs(it - current) < 0.05f }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) currentIndex + 1 else 0
                            viewModel.setFontScale(presets[nextIndex])
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    // Style Selector
                    CardStyleSelectorButton(
                        currentStyle = viewModel.currentCardStyle.collectAsState().value,
                        onStyleSelected = { viewModel.updateCardStyle(it) }
                    )

                    IconButton(onClick = onThemeToggle) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                Box(
                    modifier = Modifier.shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = FABColor.copy(alpha = 0.5f),
                        ambientColor = FABColor.copy(alpha = 0.3f)
                    )
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=") },
                        containerColor = FABColor,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Employee",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = BackgroundLight
        ) {
            EmployeeListContent(
                navController = navController,
                viewModel = viewModel,
                constantsViewModel = constantsViewModel,
                context = context,
                isAdmin = isAdmin,
                fontScale = fontScale,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun CardStyleSelectorButton(
    currentStyle: CardStyle,
    onStyleSelected: (CardStyle) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    IconButton(onClick = { showMenu = true }) {
        Icon(Icons.Default.Palette, contentDescription = "Card Style")
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Vibrant (Default)") },
            onClick = { onStyleSelected(CardStyle.Vibrant); showMenu = false },
            leadingIcon = if (currentStyle is CardStyle.Vibrant) { { Icon(Icons.Default.Check, null) } } else null
        )
        DropdownMenuItem(
            text = { Text("Classic (Navy)") },
            onClick = { onStyleSelected(CardStyle.Classic); showMenu = false },
            leadingIcon = if (currentStyle is CardStyle.Classic) { { Icon(Icons.Default.Check, null) } } else null
        )
        DropdownMenuItem(
            text = { Text("Modern (Minimal)") },
            onClick = { onStyleSelected(CardStyle.Modern); showMenu = false },
            leadingIcon = if (currentStyle is CardStyle.Modern) { { Icon(Icons.Default.Check, null) } } else null
        )
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
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val officerStatus by viewModel.officerStatus.collectAsState()
    val searchParams by viewModel.searchParams.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val cardStyle by viewModel.currentCardStyle.collectAsState()

    // Get constants from ViewModel
    val districts by constantsViewModel.districts.collectAsState()
    val units by constantsViewModel.units.collectAsState()
    val ranks by constantsViewModel.ranks.collectAsState()
    
    // State for dropdown expansion
    var unitExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }
    
    // Derived UI-specific lists
    // val unitsList by constantsViewModel.units.collectAsState() // Removed duplicate
    val stationsForDistrict by viewModel.stationsForSelectedDistrict.collectAsState()
    val allRanks = remember(ranks) { listOf("All") + ranks }


    val searchFields = SearchFilter.values().toList()
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // 🔹 ROW 1: UNIT & DISTRICT (Primary Filters)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // UNIT Dropdown
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .zIndex(10f)
            ) {
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                ) {
                    OutlinedTextField(
                        value = searchParams.unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        leadingIcon = null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit) }, onClick = { unitExpanded = false; viewModel.updateSelectedUnit(unit) })
                        }
                    }
                }
            }

            // DISTRICT Dropdown
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .zIndex(10f)
            ) {
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                ) {
                    OutlinedTextField(
                        value = searchParams.district,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District") },
                        leadingIcon = null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        districts.forEach { district ->
                            DropdownMenuItem(text = { Text(district) }, onClick = { districtExpanded = false; viewModel.updateSelectedDistrict(district); viewModel.updateSelectedStation("All") })
                        }
                    }
                }
            }
        }

        // 🔹 ROW 2: STATION & RANK (Secondary Filters)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // STATION Dropdown
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .zIndex(9f)
            ) {
                ExposedDropdownMenuBox(
                    expanded = stationExpanded,
                    onExpandedChange = {
                        if (searchParams.district != "All") stationExpanded = !stationExpanded
                    },
                ) {
                    OutlinedTextField(
                        value = searchParams.station,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Station") },
                        leadingIcon = null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = searchParams.district != "All",
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                        stationsForDistrict.forEach { station ->
                            DropdownMenuItem(text = { Text(station) }, onClick = { stationExpanded = false; viewModel.updateSelectedStation(station) })
                        }
                    }
                }
            }
            
            // RANK Dropdown
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .zIndex(9f)
            ) {
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                ) {
                    OutlinedTextField(
                        value = searchParams.rank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank") },
                        leadingIcon = null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = PrimaryTeal
                        )
                    )
                    ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                        allRanks.forEach { rank ->
                            DropdownMenuItem(text = { Text(rank) }, onClick = { rankExpanded = false; viewModel.updateSelectedRank(rank) })
                        }
                    }
                }
            }
        }

        // 🔹 ROW 3: SEARCH BAR

        val searchLabel = when (searchParams.filter) {
            SearchFilter.METAL_NUMBER -> "Metal"
            SearchFilter.KGID -> "KGID"
            SearchFilter.MOBILE -> "Mobile"
            SearchFilter.STATION -> "Station"
            SearchFilter.RANK -> "Rank"
            SearchFilter.NAME -> "Name"
            SearchFilter.BLOOD_GROUP -> "Blood"
        }

        // Determine keyboard type based on selected filter
        val keyboardType = when (searchParams.filter) {
            SearchFilter.MOBILE, SearchFilter.METAL_NUMBER -> KeyboardType.Number
            else -> KeyboardType.Text
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(15.dp))
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(15.dp))
        ) {
            OutlinedTextField(
                value = searchParams.query,
                onValueChange = {
                    viewModel.updateSearchQuery(it)
                },
                placeholder = { Text("Search by $searchLabel") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryTeal)
                },
                trailingIcon = {
                    if (searchParams.query.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PrimaryTeal)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = PrimaryTeal
                )
            )
        }

        // 🔹 FILTER CHIPS
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        ) {
            items(SearchFilter.values().toList()) { filter ->
                if (filter == SearchFilter.KGID && !isAdmin) return@items
                if (filter == SearchFilter.RANK) return@items

                val selected = searchParams.filter == filter
                val labelText = when (filter) {
                    SearchFilter.METAL_NUMBER -> "Metal"
                    SearchFilter.KGID -> "KGID"
                    SearchFilter.BLOOD_GROUP -> "Blood"
                    else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                }

                Box(modifier = Modifier.shadow(elevation = if (selected) 4.dp else 0.dp, shape = RoundedCornerShape(20.dp))) {
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateSearchFilter(filter) },
                        enabled = true,
                        label = { Text(labelText, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (selected) Color.Transparent else ChipUnselected,
                            selectedLabelColor = Color.White,
                            containerColor = ChipUnselected,
                            labelColor = if (selected) Color.White else PrimaryTeal
                        ),
                        modifier = if (selected) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    listOf(ChipSelectedStart, ChipSelectedEnd)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }

        // 🔹 UNIFIED CONTACTS LIST
        Box(modifier = Modifier.weight(1f)) {
            when {
                employeeStatus is OperationStatus.Loading || officerStatus is OperationStatus.Loading -> {
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
                             Text("No contacts found", fontSize = 16.sp)
                             if (searchParams.query.isNotEmpty() || searchParams.district != "All" || searchParams.unit != "All") {
                                 TextButton(onClick = { 
                                     viewModel.clearFilters()
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
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            if (isAdmin && contact.employee != null) {
                                // 🧑‍💼 Show admin version for employees only
                                EmployeeCardAdmin(
                                    employee = contact.employee,
                                    isAdmin = true,
                                    fontScale = fontScale,
                                    navController = navController,
                                    onDelete = { employeeToDelete = contact.employee },
                                    context = context,
                                    cardStyle = cardStyle
                                )
                            } else {
                                // 👮‍♂️ Show unified contact card (works for both Employee and Officer)
                                ContactCard(
                                    officer = contact.officer,
                                    fontScale = fontScale,
                                    isAdmin = isAdmin,
                                    onEdit = { 
                                        navController.navigate("${Routes.ADD_OFFICER}?officerId=${contact.officer?.agid ?: ""}")
                                    },
                                    onClick = {},
                                    cardStyle = cardStyle
                                )
                            }
                        }
                    }
                }
            }
        }
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


