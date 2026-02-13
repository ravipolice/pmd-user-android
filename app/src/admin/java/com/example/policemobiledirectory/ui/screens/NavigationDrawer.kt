package com.example.policemobiledirectory.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Badge // Or Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawer(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentRoute = navController.currentDestination?.route

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .navigationBarsPadding() // ✅ Fix: Account for system navigation bar to prevent overlap
        ) {

            // ============================================================
            // 🔹 TOP SECTION: PROFILE CARD
            // ============================================================
            Surface(
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image with Blood Group in top right corner
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        val painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(currentUser?.photoUrl)
                                .placeholder(R.drawable.officer)
                                .error(R.drawable.officer)
                                .crossfade(true)
                                .scale(Scale.FILL)
                                .build()
                        )

                        Image(
                            painter = painter,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                        )
                        
                        // Blood Group badge in top right corner
                        currentUser?.bloodGroup?.takeIf { it.isNotBlank() }?.let { bg ->
                            // Format blood group: show "??" as-is, otherwise format normally
                            val formattedBg = if (bg.trim() == "??") {
                                "??"
                            } else {
                                bg.uppercase()
                                    .replace("POSITIVE", "+")
                                    .replace("NEGATIVE", "–")
                                    .replace("VE", "")
                                    .replace("(", "")
                                    .replace(")", "")
                                    .trim()
                                    .let { clean ->
                                        when (clean) {
                                            "A" -> "A+"
                                            "B" -> "B+"
                                            "O" -> "O+"
                                            "AB" -> "AB+"
                                            "A-" -> "A–"
                                            "B-" -> "B–"
                                            "O-" -> "O–"
                                            "AB-" -> "AB–"
                                            else -> clean
                                        }
                                    }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(28.dp)
                                    .offset(x = 4.dp, y = (-4).dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = formattedBg,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Name (bold, prominent) + rank (smaller, no brackets)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser?.name ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        currentUser?.displayRank?.takeIf { it.isNotBlank() }?.let { rank ->
                            Text(
                                text = rank,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                    
                    // KGID (larger)
                    Text(
                        text = currentUser?.kgid?.let { "KGID: $it" } ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 16.sp
                        )
                    )
                    
                    // Station (larger)
                    currentUser?.station?.takeIf { it.isNotBlank() }?.let { station ->
                        Text(
                            text = station,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp
                            )
                        )
                    }
                    
                    // Email (larger)
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Divider(thickness = 1.dp)

            // ============================================================
            // 🔹 MIDDLE SECTION: MENU ITEMS
            // ============================================================
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState()) // Enable scrolling for many items
            ) {
                // 1. Dashboard
                DrawerItem(
                    icon = Icons.Default.Dashboard,
                    text = "Dashboard",
                    selected = currentRoute == Routes.ADMIN_PANEL,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.ADMIN_PANEL) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                            }
                        }
                    }
                )

                // 2. Employees (Home)
                DrawerItem(
                    icon = Icons.Default.Badge, // Using Badge or Person
                    text = "Employees",
                    selected = currentRoute == Routes.EMPLOYEE_LIST,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.EMPLOYEE_LIST) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(0) // Go to root
                            }
                        }
                    }
                )
                
                // 3. Pending Approvals
                DrawerItem(
                    icon = Icons.Default.VerifiedUser,
                    text = "Pending Approvals",
                    selected = currentRoute == Routes.PENDING_APPROVALS,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.PENDING_APPROVALS)
                        }
                    }
                )

               /*
                // 4. Officers (Merged into Employees generally, but kept if distinct route desired)
                DrawerItem(
                    icon = Icons.Default.LocalPolice,
                    text = "Officers",
                    selected = false, // Add specific route check if separate screen exists
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            // Logic to filter for officers or nav to separate screen
                            navController.navigate(Routes.EMPLOYEE_LIST) // Placeholder
                        }
                    }
                )
                */

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                // --- MASTER DATA ---
                Text(
                    text = "Master Data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                )

                // Ranks
                DrawerItem(
                    icon = Icons.Default.Stars,
                    text = "Ranks",
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("${Routes.MANAGE_CONSTANTS}?initialTab=3") // Tab 3 = Ranks
                        }
                    },
                    isSubItem = true
                )

                // Districts
                DrawerItem(
                    icon = Icons.Default.Map,
                    text = "Districts",
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("${Routes.MANAGE_CONSTANTS}?initialTab=0") // Tab 0 = Districts
                        }
                    },
                    isSubItem = true

                )

                // Stations
                DrawerItem(
                    icon = Icons.Default.Apartment,
                    text = "Stations",
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("${Routes.MANAGE_CONSTANTS}?initialTab=1") // Tab 1 = Stations
                        }
                    },
                    isSubItem = true

                )

                // Units
                DrawerItem(
                    icon = Icons.Default.Groups,
                    text = "Units",
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("${Routes.MANAGE_CONSTANTS}?initialTab=2") // Tab 2 = Units
                        }
                    },
                    isSubItem = true

                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 5. Notifications
                DrawerItem(
                    icon = Icons.Default.Notifications,
                    text = "Notifications",
                    selected = currentRoute == Routes.NOTIFICATIONS,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.NOTIFICATIONS)
                        }
                    }
                )

                // 6. Documents
                DrawerItem(
                    icon = Icons.Default.Description,
                    text = "Documents",
                    selected = currentRoute == Routes.DOCUMENTS,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.DOCUMENTS)
                        }
                    }
                )

                // 7. Gallery
                DrawerItem(
                    icon = Icons.Default.PhotoLibrary,
                    text = "Gallery",
                    selected = currentRoute == Routes.GALLERY_SCREEN,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.GALLERY_SCREEN)
                        }
                    }
                )
                
                // 8. Useful Links
                DrawerItem(
                    icon = Icons.Default.Link,
                    text = "Useful Links",
                    selected = currentRoute == Routes.USEFUL_LINKS,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.USEFUL_LINKS)
                        }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 9. App Management (General)
                
                // 10. Doc Converter
                DrawerItem(
                    icon = Icons.Default.Translate,
                    text = "Nudi Converter",
                    selected = currentRoute == Routes.NUDI_CONVERTER,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.NUDI_CONVERTER)
                        }
                    }
                )
                
                // 11. CSV Upload
                DrawerItem(
                    icon = Icons.Default.UploadFile,
                    text = "CSV Upload",
                    selected = currentRoute == Routes.UPLOAD_CSV,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.UPLOAD_CSV)
                        }
                    }
                )
            }

            Divider(thickness = 1.dp)

            // ============================================================
            // 🔹 BOTTOM SECTION: LOGOUT + CONTACT
            // ============================================================
            Column(modifier = Modifier.padding(16.dp)) {
                var showLogoutDialog by remember { mutableStateOf(false) }
                var isLoggingOut by remember { mutableStateOf(false) }
                var showSupportDialog by remember { mutableStateOf(false) }
                val clipboardManager = LocalClipboardManager.current

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
                        confirmButton = {
                            TextButton(
                                enabled = !isLoggingOut,
                                onClick = {
                                    isLoggingOut = true
                                    scope.launch {
                                        drawerState.close()
                                        viewModel.logout {
                                            isLoggingOut = false
                                            showLogoutDialog = false
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            ) {
                                if (isLoggingOut) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Logout", color = Color.Red)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !isLoggingOut,
                                onClick = { showLogoutDialog = false }
                            ) { Text("Cancel") }
                        },
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        title = { Text("Confirm Logout") },
                        text = { Text("Are you sure you want to log out?") }
                    )
                }

                if (showSupportDialog) {
                    AlertDialog(
                        onDismissRequest = { showSupportDialog = false },
                        icon = { Icon(Icons.Default.Email, contentDescription = null) },
                        title = { Text("Contact Support") },
                        text = { Text("Email: noreply.pmdapp@gmail.com\nWe usually respond quickly.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:noreply.pmdapp@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                }
                                showSupportDialog = false
                            }) {
                                Text("Email")
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString("noreply.pmdapp@gmail.com"))
                                    Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                                }) { Text("Copy") }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { showSupportDialog = false }) { Text("Close") }
                            }
                        }
                    )
                }

                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    text = "Logout",
                    textColor = Color.Red,
                    onClick = { showLogoutDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DrawerItem(
                    icon = Icons.Default.Email,
                    text = "Contact Support",
                    onClick = { showSupportDialog = true }
                )
            }
        }
    }
}

// ============================================================
// 🔹 Reusable Drawer Item Composable
// ============================================================
@Composable
fun DrawerItem(
    icon: ImageVector,
    text: String,
    selected: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isSubItem: Boolean = false, // ✅ New parameter
    onClick: () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        Color.Transparent

    val paddingStart = if (isSubItem) 40.dp else 20.dp // Indent subitems

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(start = paddingStart, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if(isSubItem) 20.dp else 24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (selected) MaterialTheme.colorScheme.primary else textColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
