package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageConstantsScreen(
    navController: NavController,
    viewModel: ConstantsViewModel
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Districts", "Stations", "Units")

    // State collection
    val districts by viewModel.districts.collectAsState()
    val stationsByDistrict by viewModel.stationsByDistrict.collectAsState()
    val units by viewModel.units.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()

    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var selectedDistrictForStation by remember { mutableStateOf("") }

    // Constants for dropdown
    var expandedDistrictDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(refreshStatus) {
        when (refreshStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, (refreshStatus as OperationStatus.Success<String>).data, Toast.LENGTH_SHORT).show()
                viewModel.resetRefreshStatus()
                showAddDialog = false
                newItemName = ""
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, (refreshStatus as OperationStatus.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetRefreshStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
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
                                onDelete = { viewModel.deleteDistrict(district) }
                            )
                        }
                    }
                }
                1 -> { // Stations
                    // Flatten the map for display or show grouped. Let's flatten for now with header.
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
                            ResourceItem(
                                name = unit,
                                onDelete = { viewModel.deleteUnit(unit) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New ${tabs[selectedTabIndex].dropLast(1)}") }, // "Add New District"
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
}

@Composable
fun ResourceItem(name: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
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
