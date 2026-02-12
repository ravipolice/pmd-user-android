package com.example.policemobiledirectory.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.ui.theme.ChipSelectedStart
import com.example.policemobiledirectory.ui.theme.PrimaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBar(
    // Data Sources
    units: List<String>,
    districts: List<String>,
    stations: List<String>,
    ranks: List<String>,
    
    // Selected Values
    selectedUnit: String,
    selectedDistrict: String,
    selectedStation: String,
    selectedRank: String,
    
    // Callbacks
    onUnitChange: (String) -> Unit,
    onDistrictChange: (String) -> Unit,
    onStationChange: (String) -> Unit,
    onRankChange: (String) -> Unit,
    
    // Search Query
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    
    // Search Filter Type
    searchFilter: SearchFilter,
    onSearchFilterChange: (SearchFilter) -> Unit,
    
    // Config
    isDistrictLevelUnit: Boolean,
    isAdmin: Boolean,
    districtLabel: String = "District / HQ",
    stationLabel: String = "Station / Section",
    modifier: Modifier = Modifier
) {
    // UI State for Dropdowns (Internal)
    var unitExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }

    val searchFields = SearchFilter.values().toList()

    Column(
        modifier = modifier.fillMaxWidth(),
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
                        value = selectedUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
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
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    onUnitChange(unit)
                                    unitExpanded = false
                                }
                            )
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
                        value = selectedDistrict,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(districtLabel) },
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
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        districts.forEach { district ->
                            DropdownMenuItem(
                                text = { Text(district) },
                                onClick = {
                                    onDistrictChange(district)
                                    districtExpanded = false
                                }
                            )
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
            // STATION Dropdown (Hidden for District Level Units)
            if (!isDistrictLevelUnit) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .zIndex(9f)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = stationExpanded,
                        onExpandedChange = {
                            if ((selectedDistrict != "All" || stations.isNotEmpty())) stationExpanded = !stationExpanded
                        },
                    ) {
                        OutlinedTextField(
                            value = selectedStation,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stationLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = selectedDistrict != "All" || stations.isNotEmpty(),
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
                        ExposedDropdownMenu(
                            expanded = stationExpanded,
                            onDismissRequest = { stationExpanded = false }
                        ) {
                            stations.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station) },
                                    onClick = {
                                        onStationChange(station)
                                        stationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                 // Filler to keep Rank aligned or just let Rank take full width?
                 // Original logic let rank take 0.4f or 1f.
                 // We can handle this logic in the Row weight.
            }

            // RANK Dropdown
            Box(
                modifier = Modifier
                    .weight(if (isDistrictLevelUnit) 1f else 0.4f)
                    .zIndex(9f)
            ) {
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedRank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank") },
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
                    ExposedDropdownMenu(
                        expanded = rankExpanded,
                        onDismissRequest = { rankExpanded = false }
                    ) {
                        ranks.forEach { rank ->
                            DropdownMenuItem(
                                text = { Text(rank) },
                                onClick = {
                                    onRankChange(rank)
                                    rankExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 🔹 ROW 3: SEARCH BAR
        val searchLabel = when (searchFilter) {
            SearchFilter.ALL -> "Power Search"
            SearchFilter.RANK -> "Rank"
            SearchFilter.NAME -> "Name"
            SearchFilter.BLOOD_GROUP -> "Blood"
            else -> searchFilter.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        val placeholderText = if (searchFilter == SearchFilter.ALL) {
            "Search by Name, Mobile, Rank, Station, Blood..."
        } else {
            "Search by $searchLabel"
        }

        val keyboardType = when (searchFilter) {
            SearchFilter.MOBILE, SearchFilter.METAL_NUMBER -> KeyboardType.Number
            else -> KeyboardType.Text
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(placeholderText) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTeal) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PrimaryTeal)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = PrimaryTeal,
                unfocusedLabelColor = PrimaryTeal
            )
        )

        // 🔹 FILTER CHIPS
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(searchFields) { filter ->
                if (filter == SearchFilter.KGID && !isAdmin) return@items
                if (filter == SearchFilter.RANK) return@items

                FilterChip(
                    selected = searchFilter == filter,
                    onClick = { onSearchFilterChange(filter) },
                    label = {
                        Text(
                            when (filter) {
                                SearchFilter.METAL_NUMBER -> "Metal"
                                SearchFilter.KGID -> "KGID"
                                SearchFilter.BLOOD_GROUP -> "Blood"
                                else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChipSelectedStart,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}
