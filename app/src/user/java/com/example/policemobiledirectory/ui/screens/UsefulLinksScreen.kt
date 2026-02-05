package com.example.policemobiledirectory.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsefulLinksScreen(
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    val usefulLinks by viewModel.usefulLinks.collectAsState()
    // isAdmin and deleteDialog state removed


    val context = LocalContext.current
    val pendingStatus by viewModel.pendingStatus.collectAsState()

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Fetch links when screen loads and when coming back
    LaunchedEffect(currentRoute) {
        // Only refresh if we're on the useful links screen
        if (currentRoute == com.example.policemobiledirectory.navigation.Routes.USEFUL_LINKS) {
            viewModel.fetchUsefulLinks()
        }
    }

    // Show toast for delete status
    LaunchedEffect(pendingStatus) {
        val status = pendingStatus
        when (status) {
            is com.example.policemobiledirectory.utils.OperationStatus.Success<*> -> {
                val data = status.data.toString()
                Toast.makeText(context, data, Toast.LENGTH_SHORT).show()
                viewModel.resetPendingStatus()
            }
            is com.example.policemobiledirectory.utils.OperationStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                viewModel.resetPendingStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("Useful Links") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchUsefulLinks() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (usefulLinks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No links available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(usefulLinks) { link ->
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(90.dp)
                                    .clickable {
                                        handleLinkClick(context, link.playStoreUrl, link.apkUrl, link.name)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val iconModel = remember(link.iconUrl, link.playStoreUrl) {
                                    when {
                                        !link.iconUrl.isNullOrBlank() -> link.iconUrl
                                        !link.playStoreUrl.isNullOrBlank() -> {
                                            val packageName = getPackageNameFromPlayUrl(link.playStoreUrl)
                                            if (!packageName.isNullOrBlank() && packageName != link.playStoreUrl) {
                                                "https://www.google.com/s2/favicons?sz=128&domain_url=https://play.google.com/store/apps/details?id=$packageName"
                                            } else {
                                                "https://www.google.com/s2/favicons?sz=128&domain_url=play.google.com"
                                            }
                                        }
                                        else -> null
                                    }
                                }

                                val painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data(iconModel)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = null,
                                    error = null
                                )

                                if (iconModel != null) {
                                    Image(
                                        painter = painter,
                                        contentDescription = link.name,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                            contentDescription = link.name,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = link.name,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------
   ✅ Utility functions (unchanged)
---------------------------------------------- */

fun handleLinkClick(context: Context, playStoreUrl: String?, apkUrl: String?, appName: String) {
    try {
        when {
            !playStoreUrl.isNullOrEmpty() && playStoreUrl.contains("id=") -> {
                openAppOrStore(context, playStoreUrl)
            }
            !apkUrl.isNullOrEmpty() && apkUrl.contains("firebasestorage.googleapis.com") -> {
                downloadAndInstallApk(context, apkUrl, appName)
            }
            !playStoreUrl.isNullOrEmpty() -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                context.startActivity(intent)
            }
            else -> Toast.makeText(context, "No valid link found.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun openAppOrStore(context: Context, playStoreUrl: String) {
    val packageName = getPackageNameFromPlayUrl(playStoreUrl)
    if (packageName != null) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) context.startActivity(launchIntent)
        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
    } else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
}

fun downloadAndInstallApk(context: Context, apkUrl: String, appName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(apkUrl), "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
    }
}

fun getPackageNameFromPlayUrl(url: String): String? =
    try { Uri.parse(url).getQueryParameter("id") } catch (_: Exception) { null }
