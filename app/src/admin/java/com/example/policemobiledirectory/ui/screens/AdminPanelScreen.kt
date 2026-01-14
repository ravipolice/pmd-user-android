package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.components.DashboardActionCard
import com.example.policemobiledirectory.ui.components.DashboardStatCard
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel(),
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Officers")
    
    // Collect specific lists to get counts
    // Use the correct StateFlows from EmployeeViewModel
    val employees by viewModel.employees.collectAsState()
    val pendingRegistrations by viewModel.pendingRegistrations.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    
    val totalEmployeesCount = employees.size
    val pendingCount = pendingRegistrations.size
    
    // Status collection
    // Renamed refreshStatus to employeeStatus to match ViewModel
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val firestoreToSheetStatus by viewModel.firestoreToSheetStatus.collectAsState()
    val sheetToFirestoreStatus by viewModel.sheetToFirestoreStatus.collectAsState()
    val officersSyncStatus by viewModel.officersSyncStatus.collectAsState()
    
    // Constants status
    val constantsRefreshStatus by constantsViewModel.refreshStatus.collectAsState()

    // 🔹 Load admin data
    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            viewModel.refreshEmployees()
            viewModel.refreshPendingRegistrations()
        }
    }

    // Handle toast messages for operations
    LaunchedEffect(employeeStatus) {
        if (employeeStatus is OperationStatus.Error) {
             Toast.makeText(context, (employeeStatus as OperationStatus.Error).message, Toast.LENGTH_SHORT).show()
        }
        // No reset for employeeStatus available/needed for now
    }
    
    LaunchedEffect(firestoreToSheetStatus) {
        if (firestoreToSheetStatus is OperationStatus.Success) {
            Toast.makeText(context, (firestoreToSheetStatus as OperationStatus.Success<String>).data, Toast.LENGTH_SHORT).show()
            viewModel.resetFirestoreToSheetStatus()
        }
    }
    
    LaunchedEffect(sheetToFirestoreStatus) {
        if (sheetToFirestoreStatus is OperationStatus.Success) {
            Toast.makeText(context, (sheetToFirestoreStatus as OperationStatus.Success<String>).data, Toast.LENGTH_SHORT).show()
            viewModel.resetSheetToFirestoreStatus()
        }
    }
    
    LaunchedEffect(officersSyncStatus) {
        if (officersSyncStatus is OperationStatus.Success) {
            Toast.makeText(context, (officersSyncStatus as OperationStatus.Success<String>).data, Toast.LENGTH_SHORT).show()
            viewModel.resetOfficersSyncStatus()
        }
    }

    LaunchedEffect(constantsRefreshStatus) {
        if (constantsRefreshStatus is OperationStatus.Success) {
            Toast.makeText(context, (constantsRefreshStatus as OperationStatus.Success<String>).data, Toast.LENGTH_SHORT).show()
            constantsViewModel.resetRefreshStatus()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = { 
                        Column {
                            Text(
                                text = "Admin Dashboard",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Police Mobile Directory",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> AdminDashboardGrid(
                    navController = navController,
                    viewModel = viewModel,
                    constantsViewModel = constantsViewModel,
                    totalEmployeesCount = totalEmployeesCount,
                    pendingCount = pendingCount
                )
                1 -> OfficerListContent(
                    viewModel = viewModel,
                    constantsViewModel = constantsViewModel,
                    onAddOfficer = { navController.navigate(Routes.ADD_OFFICER) },
                    onEditOfficer = { id -> navController.navigate("${Routes.EDIT_OFFICER}/$id") }
                )
            }
        }
    }
}

@Composable
fun AdminDashboardGrid(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
    totalEmployeesCount: Int,
    pendingCount: Int
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        
        // --- SECTION 1: STATISTICS ---
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item(span = { GridItemSpan(2) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardStatCard(
                    title = "Total Employees",
                    count = totalEmployeesCount.toString(),
                    icon = Icons.Outlined.People,
                    colorStart = Color(0xFF2196F3), // Blue
                    colorEnd = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f).clickable { 
                        navController.navigate(Routes.EMPLOYEE_STATS) 
                    }
                )
                
                DashboardStatCard(
                    title = "Pending Approvals",
                    count = pendingCount.toString(),
                    icon = Icons.Outlined.PendingActions,
                    colorStart = Color(0xFF009688), // Teal
                    colorEnd = Color(0xFF4DB6AC),
                    modifier = Modifier.weight(1f).clickable {
                        navController.navigate(Routes.PENDING_APPROVALS)
                    }
                )
            }
        }
        
        // --- SECTION 2: MANAGEMENT ---
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Row 1
        item {
            DashboardActionCard(
                title = "Send Notification",
                icon = Icons.Outlined.NotificationsActive,
                color = Color(0xFFE91E63), // Pink
                onClick = { navController.navigate(Routes.SEND_NOTIFICATION) }
            )
        }
        item {
            DashboardActionCard(
                title = "Upload Database",
                icon = Icons.Outlined.CloudUpload,
                color = Color(0xFF3F51B5), // Indigo
                onClick = { navController.navigate(Routes.UPLOAD_CSV) }
            )
        }

        // Row 2
        item {
            DashboardActionCard(
                title = "Manage Resources",
                icon = Icons.Outlined.Category, // Category/Folder
                color = Color(0xFFFF9800), // Orange
                onClick = { navController.navigate(Routes.MANAGE_CONSTANTS) }
            )
        }
        item {
            DashboardActionCard(
                title = "Add Useful Link",
                icon = Icons.Outlined.Link,
                color = Color(0xFF673AB7), // Deep Purple
                onClick = { navController.navigate(Routes.ADD_USEFUL_LINK) }
            )
        }

        // Row 3
        item {
            DashboardActionCard(
                title = "Upload Document",
                icon = Icons.Outlined.Description,
                color = Color(0xFF00BCD4), // Cyan
                onClick = { navController.navigate(Routes.UPLOAD_DOCUMENT) }
            )
        }
        item {
            DashboardActionCard(
                title = "Sync Firestore \u2192 Sheet",
                icon = Icons.Default.CloudDownload,
                color = Color(0xFF4CAF50), // Green
                onClick = { viewModel.syncFirebaseToSheet() }
            )
        }

        // Row 4
        item {
            DashboardActionCard(
                title = "Sync Sheet \u2192 Firestore",
                icon = Icons.Default.CloudUpload,
                color = Color(0xFF2196F3), // Blue
                onClick = { viewModel.syncSheetToFirebase() }
            )
        }
        item {
            DashboardActionCard(
                title = "Sync Officers (Sheet)",
                icon = Icons.Default.Badge,
                color = Color(0xFF9C27B0), // Purple
                onClick = { viewModel.syncOfficersSheetToFirebase() }
            )
        }
        
        // Footer Action
        item(span = { GridItemSpan(2) }) {
            Button(
                onClick = { constantsViewModel.clearCacheAndRefresh() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh System Constants (Clear Cache)")
            }
        }
        
        // Version Info
        item(span = { GridItemSpan(2) }) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Admin Panel v2.0 • Modern Grid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
