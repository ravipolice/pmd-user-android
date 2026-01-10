package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.utils.OperationStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel(),
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val employeesList by viewModel.employees.collectAsState()
    val pendingRegistrationsList by viewModel.pendingRegistrations.collectAsState()
    val firestoreToSheetStatus by viewModel.firestoreToSheetStatus.collectAsState()
    val sheetToFirestoreStatus by viewModel.sheetToFirestoreStatus.collectAsState()
    val officersSyncStatus by viewModel.officersSyncStatus.collectAsState()
    val constantsRefreshStatus by constantsViewModel.refreshStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle back button to navigate to home screen
    BackHandler {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    val employeesCount = employeesList.size
    val pendingRegistrationsCount = pendingRegistrationsList.size

    // 🔹 Load admin data
    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            viewModel.refreshEmployees()
            viewModel.refreshPendingRegistrations()
        }
    }

    LaunchedEffect(firestoreToSheetStatus) {
        when (val status = firestoreToSheetStatus) {
            is OperationStatus.Success -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.data) }
                viewModel.resetFirestoreToSheetStatus()
            }
            is OperationStatus.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(status.message) }
                viewModel.resetFirestoreToSheetStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(sheetToFirestoreStatus) {
        when (val status = sheetToFirestoreStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                viewModel.resetSheetToFirestoreStatus()
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.resetSheetToFirestoreStatus()
            }
            else -> Unit
        }
    }
    
    LaunchedEffect(officersSyncStatus) {
        when (val status = officersSyncStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                viewModel.resetOfficersSyncStatus()
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.resetOfficersSyncStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(constantsRefreshStatus) {
        when (val status = constantsRefreshStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                constantsViewModel.resetRefreshStatus()
            }
            is OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                constantsViewModel.resetRefreshStatus()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = padding.calculateTopPadding() + 16.dp, 
                bottom = 24.dp
            ),
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
                        count = totalEmployees.toString(),
                        icon = Icons.Outlined.People,
                        colorStart = Color(0xFF2196F3), // Blue
                        colorEnd = Color(0xFF64B5F6),
                        modifier = Modifier.weight(1f).clickable { 
                            navController.navigate(Routes.EMPLOYEE_STATS) 
                        }
                    )
                    
                    )

                    ButtonRow(
                        icon = Icons.Filled.Refresh,
                        text = "Refresh Constants (Clear Cache)",
                        enabled = constantsRefreshStatus !is OperationStatus.Loading,
                        isLoading = constantsRefreshStatus is OperationStatus.Loading,
                        onClick = { constantsViewModel.clearCacheAndRefresh() }
                    )

                } else {
                    // 🔸 Non-admin users — fallback message + redirect
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "You do not have access to this page.\nRedirecting...",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 🔹 Redirect after showing message
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.EMPLOYEE_LIST) {
                            popUpTo(Routes.ADMIN_PANEL) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}
