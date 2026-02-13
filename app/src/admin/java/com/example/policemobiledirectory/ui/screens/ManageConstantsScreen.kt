package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close // Added Close icon
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.model.UnitModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageConstantsScreen(
    navController: NavController,
    viewModel: ConstantsViewModel,
    initialTab: Int = -1
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Districts", "Stations", "Units", "Ranks") // Added Ranks

    // State collection
    val districts by viewModel.districts.collectAsState()
    val stationsByDistrict by viewModel.stationsByDistrict.collectAsState()
    val units by viewModel.fullUnits.collectAsState() // Changed to fullUnits
    val ranks by viewModel.ranks.collectAsState() // Collect ranks
    val ksrpBattalions by viewModel.ksrpBattalions.collectAsState() // Collect Battalions
    val refreshStatus by viewModel.refreshStatus.collectAsState()

    // Dialog State for Edit
    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf("") }
    var originalItemName by remember { mutableStateOf("") }
    
    // Dialog State for Advanced Unit Edit
    var showEditUnitDialog by remember { mutableStateOf(false) }
    var unitToEdit by remember { mutableStateOf<com.example.policemobiledirectory.model.UnitModel?>(null) }

    // Dialog State for Add
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var selectedDistrictForStation by remember { mutableStateOf("") }
    var expandedDistrictDropdown by remember { mutableStateOf(false) }

    // Dialog State for Sections
    var showSectionsDialog by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }
    val currentSections by viewModel.currentUnitSections.collectAsState()

    LaunchedEffect(refreshStatus) {
        when (refreshStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, (refreshStatus as OperationStatus.Success<String>).data, Toast.LENGTH_SHORT).show()
                viewModel.resetRefreshStatus()
                showAddDialog = false
                showEditDialog = false
                // Don't dismiss sections dialog on success, just clear input
                newSectionName = ""
                newItemName = ""
                // itemToEdit = "" // Don't clear itemToEdit as it tracks the current unit
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, (refreshStatus as OperationStatus.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetRefreshStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("Manage Resources") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Custom Grid Tile Composable
            @Composable
            fun DashboardTile(
                title: String,
                icon: androidx.compose.ui.graphics.vector.ImageVector,
                color: Color,
                onClick: () -> Unit
            ) {
                Card(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = CardDefaults.cardColors(containerColor = color),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Icon (Large, faded)
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(80.dp)
                                .offset(x = 10.dp, y = 10.dp)
                        )
                        
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Handle Back Press to go to Grid
            androidx.activity.compose.BackHandler(enabled = selectedTabIndex != -1) {
                selectedTabIndex = -1
            }

            if (selectedTabIndex == -1) {
                // DASHBOARD GRID VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            DashboardTile(
                                title = "Districts",
                                icon = Icons.Default.Map,
                                color = Color(0xFF43A047), // Green 600
                                onClick = { selectedTabIndex = 0 }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            DashboardTile(
                                title = "Stations",
                                icon = Icons.Default.Apartment,
                                color = Color(0xFF1E88E5), // Blue 600
                                onClick = { selectedTabIndex = 1 }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            DashboardTile(
                                title = "Units",
                                icon = Icons.Default.Groups,
                                color = Color(0xFFFB8C00), // Orange 600
                                onClick = { selectedTabIndex = 2 }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            DashboardTile(
                                title = "Ranks",
                                icon = Icons.Default.Stars,
                                color = Color(0xFFE91E63), // Pink 500
                                onClick = { selectedTabIndex = 3 }
                            )
                        }
                    }
                }
            } else {
                // DETAIL HEADER (Optional, to show where we are)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                         Text(
                            text = tabs[selectedTabIndex],
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            when (selectedTabIndex) {
                0 -> { // Districts
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(districts) { district ->
                            ResourceItem(
                                name = district,
                                onEdit = {
                                    itemToEdit = district
                                    originalItemName = district
                                    showEditDialog = true
                                },
                                onDelete = { viewModel.deleteDistrict(district) }
                            )
                        }
                    }
                }
                1 -> { // Stations
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stationsByDistrict.forEach { (district, stations) ->
                            item {
                                Text(
                                    text = district,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(stations) { station ->
                                ResourceItem(
                                    name = station,
                                    onEdit = {
                                        itemToEdit = station
                                        originalItemName = station
                                        selectedDistrictForStation = district // Track which district this station belongs to
                                        showEditDialog = true
                                    },
                                    onDelete = { viewModel.deleteStation(district, station) }
                                )
                            }
                        }
                    }
                }
                2 -> { // Units
                     LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(units) { unit ->
                            // Custom Unit Item with "Manage Sections" button
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = unit.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            // Active/Inactive Badge
                                            Surface(
                                                color = if (unit.isActive) Color.Green.copy(alpha=0.2f) else Color.Red.copy(alpha=0.2f),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = if (unit.isActive) "Active" else "Inactive",
                                                    color = if (unit.isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(4.dp))
                                        
                                        // Scopes
                                        if (unit.scopes.isNotEmpty()) {
                                            Text(
                                                text = "Scopes: ${unit.scopes.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        
                                        // Hide from Reg
                                        if (unit.hideFromRegistration) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Hidden from Registration",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFEF6C00) // Orange
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        TextButton(
                                            onClick = {
                                                itemToEdit = unit.name // Use name string for legacy compatibility in dialogs
                                                viewModel.loadSectionsForUnit(unit.name)
                                                showSectionsDialog = true
                                                Toast.makeText(context, "Managing sections for ${unit.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(
                                                text = "Manage Sections",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            unitToEdit = unit
                                            showEditUnitDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.deleteUnit(unit.name) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // Ranks
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ranks) { rank ->
                            ResourceItem(
                                name = rank,
                                onEdit = {
                                    itemToEdit = rank
                                    originalItemName = rank
                                    showEditDialog = true
                                },
                                onDelete = { viewModel.deleteRank(rank) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ADD DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New ${tabs[selectedTabIndex].dropLast(1)}") },
            text = {
                Column {
                    if (selectedTabIndex == 1) { // Adding Station requires selecting District
                        ExposedDropdownMenuBox(
                            expanded = expandedDistrictDropdown,
                            onExpandedChange = { expandedDistrictDropdown = !expandedDistrictDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedDistrictForStation.ifEmpty { "Select District" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistrictDropdown) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDistrictDropdown,
                                onDismissRequest = { expandedDistrictDropdown = false }
                            ) {
                                districts.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district) },
                                        onClick = {
                                            selectedDistrictForStation = district
                                            expandedDistrictDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            when (selectedTabIndex) {
                                0 -> viewModel.addDistrict(newItemName)
                                1 -> {
                                    if (selectedDistrictForStation.isNotBlank()) {
                                        viewModel.addStation(selectedDistrictForStation, newItemName)
                                    } else {
                                        Toast.makeText(context, "Select a district", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                2 -> viewModel.addUnit(newItemName)
                                3 -> viewModel.addRank(newItemName) // Add Rank
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT DIALOG
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit ${tabs[selectedTabIndex].dropLast(1)}") },
            text = {
                Column {
                    Text(
                        "Note: Renaming will technically delete the old item and create a new one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemToEdit,
                        onValueChange = { itemToEdit = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemToEdit.isNotBlank() && itemToEdit != originalItemName) {
                            when (selectedTabIndex) {
                                0 -> viewModel.updateDistrict(originalItemName, itemToEdit)
                                1 -> viewModel.updateStation(selectedDistrictForStation, originalItemName, itemToEdit)
                                2 -> viewModel.updateUnit(originalItemName, itemToEdit)
                                3 -> viewModel.updateRank(originalItemName, itemToEdit) // Update Rank
                            }
                        } else if (itemToEdit == originalItemName) {
                            showEditDialog = false // No change
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADVANCED EDIT UNIT DIALOG
    if (showEditUnitDialog && unitToEdit != null) {
        EditUnitDialog(
            unit = unitToEdit!!,
            allDistricts = districts,
            allBattalions = ksrpBattalions, // Pass Battalions
            allRanks = ranks,
            onDismiss = { showEditUnitDialog = false; unitToEdit = null },
            onSave = { updatedUnit ->
                viewModel.updateUnitDetails(updatedUnit)
                showEditUnitDialog = false
                unitToEdit = null
            }
        )
    }
    // SECTIONS DIALOG
    if (showSectionsDialog) {
        AlertDialog(
            onDismissRequest = { showSectionsDialog = false },
            title = { Text("Manage Sections for $itemToEdit") },
            text = {
                Column {
                    // List existing sections
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp), // Limit height
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentSections.isEmpty()) {
                            item { Text("No sections added.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                        }
                        items(currentSections) { section ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(section, style = MaterialTheme.typography.bodyMedium)
                                IconButton(
                                    onClick = { viewModel.deleteSection(itemToEdit, section) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Add new section
                    Text("Add New Section", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSectionName,
                            onValueChange = { newSectionName = it },
                            placeholder = { Text("Section Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSectionName.isNotBlank()) {
                                    viewModel.addSection(itemToEdit, newSectionName)
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSectionsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ResourceItem(name: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
