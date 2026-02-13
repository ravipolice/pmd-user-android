package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.policemobiledirectory.model.UnitModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUnitDialog(
    unit: UnitModel,
    allDistricts: List<String>,
    allBattalions: List<String>,
    allRanks: List<String>,
    onDismiss: () -> Unit,
    onSave: (UnitModel) -> Unit
) {
    // Local State
    var currentUnit by remember { mutableStateOf(unit) }
    var showMappedDistrictsDialog by remember { mutableStateOf(false) }
    var showRanksDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Unit: ${unit.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 1. UNIT IDENTITY (Read-only Name, Keyword)
                    item {
                        UnitSectionHeader("1. UNIT IDENTITY")
                        OutlinedTextField(
                            value = currentUnit.name,
                            onValueChange = { }, // Read-only
                            label = { Text("Unit Name (Read-only)") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = currentUnit.stationKeyword,
                            onValueChange = { currentUnit = currentUnit.copy(stationKeyword = it) },
                            label = { Text("Station Keyword Filter (Optional)") },
                            placeholder = { Text("e.g. DCRB") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("If set, only stations containing this keyword will be shown.") }
                        )
                    }

                    // 2. UNIT STATUS
                    item {
                        UnitSectionHeader("2. UNIT STATUS")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             RadioButton(
                                 selected = currentUnit.isActive,
                                 onClick = { currentUnit = currentUnit.copy(isActive = true) }
                             )
                             Text("Active", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                             Spacer(Modifier.width(16.dp))
                             RadioButton(
                                 selected = !currentUnit.isActive,
                                 onClick = { currentUnit = currentUnit.copy(isActive = false) }
                             )
                             Text("Inactive", color = Color.Gray)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = currentUnit.hideFromRegistration,
                                    onValueChange = { currentUnit = currentUnit.copy(hideFromRegistration = it) },
                                    role = Role.Checkbox
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = currentUnit.hideFromRegistration,
                                onCheckedChange = null // Handled by toggleable
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Hide from Registration Form", style = MaterialTheme.typography.bodyLarge)
                                Text("If checked, this unit will not appear in the registration form dropdown", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    // 3. UNIT SCOPE
                    item {
                        UnitSectionHeader("3. UNIT SCOPE")
                        val scopes = currentUnit.scopes.toMutableSet()
                        
                        ScopeCheckbox(
                            label = "HQ Level",
                            description = "Enables sections for this unit.",
                            checked = scopes.contains("hq"),
                            onCheckedChange = { checked ->
                                if (checked) scopes.add("hq") else scopes.remove("hq")
                                currentUnit = currentUnit.copy(scopes = scopes.toList())
                            }
                        )
                        ScopeCheckbox(
                            label = "District HQ",
                            description = "Maps to District HQ (No Stations). Enables sections management.",
                            checked = scopes.contains("district_hq"),
                            onCheckedChange = { checked ->
                                if (checked) scopes.add("district_hq") else scopes.remove("district_hq")
                                currentUnit = currentUnit.copy(scopes = scopes.toList())
                            }
                        )
                        ScopeCheckbox(
                            label = "Districts",
                            description = "Maps to Districts (Shows Stations).",
                            checked = scopes.contains("district"),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    scopes.add("district")
                                    // Set mapped area type to DISTRICT if not already set or competing
                                    currentUnit = currentUnit.copy(mappedAreaType = "DISTRICT")
                                } else {
                                    scopes.remove("district")
                                }
                                currentUnit = currentUnit.copy(scopes = scopes.toList())
                            }
                        )
                        ScopeCheckbox(
                            label = "Battalion",
                            description = "Maps to Battalions.",
                            checked = scopes.contains("battalion"),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    scopes.add("battalion")
                                    currentUnit = currentUnit.copy(mappedAreaType = "BATTALION")
                                } else {
                                    scopes.remove("battalion")
                                }
                                currentUnit = currentUnit.copy(scopes = scopes.toList())
                            }
                        )
                         ScopeCheckbox(
                            label = "Commissionerate",
                            description = "",
                            checked = scopes.contains("commissionerate"),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    scopes.add("commissionerate")
                                    currentUnit = currentUnit.copy(mappedAreaType = "COMMISSIONERATE") // Or ZONE
                                } else {
                                    scopes.remove("commissionerate")
                                }
                                currentUnit = currentUnit.copy(scopes = scopes.toList())
                            }
                        )
                    }

                    // 4. MAPPED AREAS (Conditional)
                    if (currentUnit.scopes.any { it in listOf("district", "battalion", "commissionerate") }) {
                        item {
                            UnitSectionHeader("4. MAPPED AREAS")
                            // Determine list based on mappedAreaType or scopes
                            val listToUse = when (currentUnit.mappedAreaType) {
                                "BATTALION" -> allBattalions
                                else -> allDistricts // Default to districts
                            }
                            
                            val label = when (currentUnit.mappedAreaType) {
                                "BATTALION" -> "Mapped Battalions"
                                else -> "Mapped Districts/Areas"
                            }
                            
                            OutlinedTextField(
                                value = if (currentUnit.mappedDistricts.isEmpty()) "None Selected" else "${currentUnit.mappedDistricts.size} Areas Selected",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(label) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMappedDistrictsDialog = true },
                                enabled = false, // Click handled by modifier
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            
                             if (showMappedDistrictsDialog) {
                                MultiSelectDialog(
                                    title = "Select $label",
                                    items = listToUse,
                                    selectedItems = currentUnit.mappedDistricts,
                                    onDismiss = { showMappedDistrictsDialog = false },
                                    onConfirm = { selected ->
                                        currentUnit = currentUnit.copy(mappedDistricts = selected)
                                        showMappedDistrictsDialog = false
                                    }
                                )
                            }
                        }
                    }

                    // 5. UNIT RANKS
                    item {
                         UnitSectionHeader("5. UNIT RANKS")
                         OutlinedTextField(
                                value = "${currentUnit.applicableRanks.size} Ranks Selected",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Applicable Ranks") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRanksDialog = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                    }
                }

                // Foooter Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(currentUnit) }) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
    
    // Ranks Dialog
    if (showRanksDialog) {
         MultiSelectDialog(
            title = "Select Applicable Ranks",
            items = allRanks,
            selectedItems = currentUnit.applicableRanks,
            onDismiss = { showRanksDialog = false },
            onConfirm = { selected ->
                currentUnit = currentUnit.copy(applicableRanks = selected)
                showRanksDialog = false
            }
        )
    }
}

@Composable
private fun UnitSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ScopeCheckbox(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (description.isNotEmpty()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MultiSelectDialog(
    title: String,
    items: List<String>,
    selectedItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val tempSelected = remember { mutableStateListOf<String>().apply { addAll(selectedItems) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                // Select All / Deselect All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { tempSelected.clear(); tempSelected.addAll(items) }) {
                         Text("Select All")
                    }
                    TextButton(onClick = { tempSelected.clear() }) {
                         Text("Deselect All")
                    }
                }
                HorizontalDivider()
                LazyColumn {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (tempSelected.contains(item)) tempSelected.remove(item)
                                    else tempSelected.add(item)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = tempSelected.contains(item), onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(item)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelected.toList()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
