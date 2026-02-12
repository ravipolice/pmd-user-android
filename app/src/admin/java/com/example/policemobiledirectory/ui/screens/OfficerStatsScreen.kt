package com.example.policemobiledirectory.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerStatsScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val allOfficers by viewModel.officers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshOfficers() }

    // --- Filters ---
    var selectedDistrict by remember { mutableStateOf("All") }
    var districtExpanded by remember { mutableStateOf(false) }
    val districtOptions = remember(districts) { listOf("All") + districts }

    var selectedStation by remember { mutableStateOf("All") }
    var stationExpanded by remember { mutableStateOf(false) }
    val stationOptions = remember(selectedDistrict, allOfficers, stationsByDistrict) {
        if (selectedDistrict == "All") {
            listOf("All") + allOfficers.mapNotNull { it.station?.trim() }.distinct().sorted()
        } else {
            val stations = stationsByDistrict[selectedDistrict]
                ?: stationsByDistrict.keys.find { it.equals(selectedDistrict, ignoreCase = true) }?.let { stationsByDistrict[it] }
                ?: emptyList()
            listOf("All") + stations
        }
    }

    var selectedRank by remember { mutableStateOf("All") }
    var rankExpanded by remember { mutableStateOf(false) }
    val allRanksOptions = remember(ranks) { listOf("All") + ranks }

    // --- Filtered officers ---
    val filteredOfficers by remember(allOfficers, selectedDistrict, selectedStation, selectedRank) {
        derivedStateOf {
            allOfficers
                .filter { selectedDistrict == "All" || it.district == selectedDistrict }
                .filter { selectedStation == "All" || it.station == selectedStation }
                .filter { selectedRank == "All" || it.rank == selectedRank }
        }
    }

    // --- Statistics ---
    val officersByDistrictStats = filteredOfficers
        .groupingBy { it.district?.trim()?.ifEmpty { "N/A" } ?: "N/A" }
        .eachCount().toList().sortedByDescending { it.second }

    val officersByStationStats = filteredOfficers
        .groupingBy { it.station?.trim()?.ifEmpty { "N/A" } ?: "N/A" }
        .eachCount().toList().sortedByDescending { it.second }

    val officersByRankStats = filteredOfficers
        .groupingBy { it.rank?.trim()?.ifEmpty { "N/A" } ?: "N/A" }
        .eachCount().toList().sortedByDescending { it.second }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("Officer Statistics") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
        ) {
            // Reusing existing FilterDropdown (private in EmployeeStatsScreen, so I'll need to copy or make it public)
            // For now, I'll define it locally in this file too
            
            FilterDropdownLocal("District/Unit", selectedDistrict, districtOptions, districtExpanded, { districtExpanded = it }) { newDistrict ->
                selectedDistrict = newDistrict
                if (newDistrict != "All") selectedStation = "All"
            }
            Spacer(Modifier.height(8.dp))
            FilterDropdownLocal("Station/Unit", selectedStation, stationOptions, stationExpanded, { stationExpanded = it }) { selectedStation = it }
            Spacer(Modifier.height(8.dp))
            FilterDropdownLocal("Rank", selectedRank, allRanksOptions, rankExpanded, { rankExpanded = it }) { selectedRank = it }
            Spacer(Modifier.height(16.dp))

            when {
                allOfficers.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No officer data available.")
                }
                filteredOfficers.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No officers match current filters.")
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { StatsSectionOfficer("Officers by District", officersByDistrictStats) }
                    item { StatsSectionOfficer("Officers by Station/Unit", officersByStationStats) }
                    item { StatsSectionOfficer("Officers by Rank", officersByRankStats) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdownLocal(
    label: String,
    selected: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatsSectionOfficer(
    title: String,
    data: List<Pair<String, Int>>
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty()) Text("No data for this category.")
            else data.forEachIndexed { index, (item, count) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                if (index < data.size - 1) HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
