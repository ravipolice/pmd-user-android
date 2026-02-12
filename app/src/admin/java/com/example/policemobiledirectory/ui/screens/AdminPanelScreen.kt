package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.components.DashboardActionCardLarge
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
    var showDashboard by remember { mutableStateOf(true) }
    
    // Collect specific lists to get counts
    val employees by viewModel.employees.collectAsState()
    val officers by viewModel.officers.collectAsState()
    val pendingRegistrations by viewModel.pendingRegistrations.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    
    val totalEmployeesCount = employees.size
    val totalOfficersCount = officers.size
    val approvedCount = employees.count { it.isApproved }
    val unviewedPendingCount by viewModel.unviewedPendingCount.collectAsState()
    val pendingCount = unviewedPendingCount
    
    // Breakdown Stats
    val empDistricts by viewModel.employeesByDistrict.collectAsState()
    val empRanks by viewModel.employeesByRank.collectAsState()
    val offDistricts by viewModel.officersByDistrict.collectAsState()
    val offRanks by viewModel.officersByRank.collectAsState()
    
    // Constants status for District and Station counts
    val districts by constantsViewModel.districts.collectAsState()
    val stationsMap by constantsViewModel.stationsByDistrict.collectAsState()
    val totalStationsCount = remember(stationsMap) { 
        stationsMap.values.sumOf { it.size }
    }
    
    // Status collection
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val firestoreToSheetStatus by viewModel.firestoreToSheetStatus.collectAsState()
    val sheetToFirestoreStatus by viewModel.sheetToFirestoreStatus.collectAsState()
    val officersSyncStatus by viewModel.officersSyncStatus.collectAsState()
    val constantsRefreshStatus by constantsViewModel.refreshStatus.collectAsState()

    // 🔹 Load admin data
    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            viewModel.refreshEmployees()
            viewModel.refreshOfficers()
            viewModel.refreshPendingRegistrations()
        }
    }

    // Handle toast messages for operations
    LaunchedEffect(employeeStatus, firestoreToSheetStatus, sheetToFirestoreStatus, officersSyncStatus, constantsRefreshStatus) {
        val statuses = listOf(
            employeeStatus to null,
            firestoreToSheetStatus to { viewModel.resetFirestoreToSheetStatus() },
            sheetToFirestoreStatus to { viewModel.resetSheetToFirestoreStatus() },
            officersSyncStatus to { viewModel.resetOfficersSyncStatus() },
            constantsRefreshStatus to { constantsViewModel.resetRefreshStatus() }
        )
        
        statuses.forEach { (status, reset) ->
            if (status is OperationStatus.Success<*>) {
                val message = (status as? OperationStatus.Success<*>)?.data as? String
                if (message != null) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    reset?.invoke()
                }
            } else if (status is OperationStatus.Error) {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = !showDashboard) {
        showDashboard = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { 
                    val staffType by viewModel.selectedStaffType.collectAsState(EmployeeViewModel.StaffType.ALL)
                    val dynamicTitle = when (staffType) {
                        EmployeeViewModel.StaffType.EMPLOYEE -> "Employees"
                        EmployeeViewModel.StaffType.OFFICER -> "Officers"
                        else -> "Staff List"
                    }
                    Column {
                        Text(
                            text = if (showDashboard) "Dashboard" else dynamicTitle,
                            fontWeight = FontWeight.Bold
                        )
                        if (showDashboard) {
                            Text(
                                text = "Welcome back! Here's your overview.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (!showDashboard) showDashboard = true 
                        else navController.navigateUp() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showDashboard) {
                        IconButton(onClick = { 
                            viewModel.refreshEmployees()
                            viewModel.refreshOfficers()
                            viewModel.refreshPendingRegistrations()
                            constantsViewModel.forceRefresh()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showDashboard) {
                AdminUnifiedDashboard(
                    navController = navController,
                    viewModel = viewModel,
                    constantsViewModel = constantsViewModel,
                    stats = DashboardSummaries(
                        totalEmployees = totalEmployeesCount,
                        totalOfficers = totalOfficersCount,
                        approved = approvedCount,
                        pending = pendingCount,
                        districts = districts.size,
                        stations = totalStationsCount
                    ),
                    breakdowns = DashboardBreakdowns(
                        empDistricts = empDistricts,
                        empRanks = empRanks,
                        offDistricts = offDistricts,
                        offRanks = offRanks
                    ),
                    onViewAll = { showDashboard = false }
                )
            } else {
                StaffListContent(
                    viewModel = viewModel,
                    constantsViewModel = constantsViewModel,
                    onAddOfficer = { navController.navigate(Routes.ADD_OFFICER) },
                    onEditOfficer = { id -> navController.navigate("${Routes.ADD_OFFICER}?officerId=$id") },
                    onDeleteOfficer = { id -> viewModel.deleteOfficer(id) },
                    onBack = { showDashboard = true }
                )
            }
        }
    }
}

data class DashboardSummaries(
    val totalEmployees: Int,
    val totalOfficers: Int,
    val approved: Int,
    val pending: Int,
    val districts: Int,
    val stations: Int
)

data class DashboardBreakdowns(
    val empDistricts: Map<String, Int>,
    val empRanks: Map<String, Int>,
    val offDistricts: Map<String, Int>,
    val offRanks: Map<String, Int>
)

@Composable
fun AdminUnifiedDashboard(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
    stats: DashboardSummaries,
    breakdowns: DashboardBreakdowns,
    onViewAll: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 1. TOP STATS ROW ---
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "Total Employees",
                    count = stats.totalEmployees.toString(),
                    icon = Icons.Outlined.People,
                    colorStart = Color(0xFF2196F3),
                    colorEnd = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f).clickable {
                        viewModel.clearFilters()
                        viewModel.updateStaffType(EmployeeViewModel.StaffType.EMPLOYEE)
                        onViewAll()
                    }
                )
                DashboardStatCard(
                    title = "Total Officers",
                    count = stats.totalOfficers.toString(),
                    icon = Icons.Outlined.Badge,
                    colorStart = Color(0xFF673AB7),
                    colorEnd = Color(0xFF7E57C2),
                    modifier = Modifier.weight(1f).clickable {
                        viewModel.clearFilters()
                        viewModel.updateStaffType(EmployeeViewModel.StaffType.OFFICER)
                        onViewAll()
                    }
                )
            }
        }

        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard("Approved", stats.approved.toString(), Icons.Default.CheckCircle, Color(0xFF4CAF50), Modifier.weight(1f))
                MiniStatCard("Pending", stats.pending.toString(), Icons.Default.Pending, Color(0xFFFF9800), Modifier.weight(1f)) {
                    navController.navigate(Routes.PENDING_APPROVALS)
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard("Districts", stats.districts.toString(), Icons.Default.LocationOn, Color(0xFF9C27B0), Modifier.weight(1f))
                MiniStatCard("Stations", stats.stations.toString(), Icons.Default.Business, Color(0xFF03A9F4), Modifier.weight(1f))
            }
        }

        // --- 2. OVERVIEW TILES ---
        item(span = { GridItemSpan(2) }) {
            SectionHeader("Statistics Overviews")
        }
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionCardLarge(
                    title = "Employees\nOverview",
                    icon = Icons.Default.Analytics,
                    colorStart = Color(0xFF009688),
                    colorEnd = Color(0xFF4DB6AC),
                    onClick = { navController.navigate(Routes.EMPLOYEE_STATS) },
                    modifier = Modifier.weight(1f)
                )
                DashboardActionCardLarge(
                    title = "Officers\nOverview",
                    icon = Icons.Default.InsertChart,
                    colorStart = Color(0xFF607D8B),
                    colorEnd = Color(0xFF90A4AE),
                    onClick = { navController.navigate(Routes.OFFICER_STATS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 3. QUICK ACTIONS ---
        item(span = { GridItemSpan(2) }) {
            SectionHeader("Management Actions")
        }
        
        item {
            DashboardActionCardLarge(
                title = "Add Officer",
                icon = Icons.Default.PersonAdd,
                colorStart = Color(0xFF4CAF50),
                colorEnd = Color(0xFF81C784),
                onClick = { navController.navigate(Routes.ADD_OFFICER) }
            )
        }
        item {
            DashboardActionCardLarge(
                title = "Sync Data",
                icon = Icons.Default.Sync,
                colorStart = Color(0xFF2196F3),
                colorEnd = Color(0xFF64B5F6),
                onClick = { viewModel.syncOfficersSheetToFirebase() }
            )
        }
        item {
            DashboardActionCardLarge(
                title = "Manage Resources",
                icon = Icons.Default.Category,
                colorStart = Color(0xFFFF9800),
                colorEnd = Color(0xFFFFB74D),
                onClick = { navController.navigate(Routes.MANAGE_CONSTANTS) }
            )
        }
        item {
            DashboardActionCardLarge(
                title = "Push Notification",
                icon = Icons.Default.Notifications,
                colorStart = Color(0xFFE91E63),
                colorEnd = Color(0xFFF06292),
                onClick = { navController.navigate(Routes.SEND_NOTIFICATION) }
            )
        }

        // --- 4. APP RESOURCES ---
        item(span = { GridItemSpan(2) }) {
            SectionHeader("App Resources")
        }
        item {
            DashboardActionCardLarge(
                title = "Gallery",
                icon = Icons.Default.PhotoLibrary,
                colorStart = Color(0xFF3F51B5),
                colorEnd = Color(0xFF5C6BC0),
                onClick = { navController.navigate(Routes.GALLERY_SCREEN) }
            )
        }
        item {
            DashboardActionCardLarge(
                title = "Documents",
                icon = Icons.Default.Description,
                colorStart = Color(0xFF795548),
                colorEnd = Color(0xFF8D6E63),
                onClick = { navController.navigate(Routes.DOCUMENTS) }
            )
        }
        item(span = { GridItemSpan(2) }) {
            DashboardActionCardLarge(
                title = "Useful Links",
                icon = Icons.Default.Link,
                colorStart = Color(0xFF00BCD4),
                colorEnd = Color(0xFF4DD0E1),
                onClick = { navController.navigate(Routes.USEFUL_LINKS) }
            )
        }

        // Footer version info
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Dashboard v2.5 • Premium Experience",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp)
            )
        }
    }
}

@Composable
fun MiniStatCard(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier
            .height(70.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(count, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun BreakdownCard(title: String, data: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No data", style = MaterialTheme.typography.labelSmall) }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    data.forEach { (label, count) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(count.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}


