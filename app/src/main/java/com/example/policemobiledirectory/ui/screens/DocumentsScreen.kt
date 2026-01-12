@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.model.Document
import com.example.policemobiledirectory.ui.viewmodel.DocumentsViewModel
import com.example.policemobiledirectory.utils.OperationStatus
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.TopAppBar

@Composable
fun DocumentsScreen(
    navController: NavController,
    viewModel: DocumentsViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val documents by viewModel.documents.collectAsState()
    val documentsStatus by viewModel.documentsStatus.collectAsState()

    // 🔍 Preview state
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewMimeType by remember { mutableStateOf<String?>(null) }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) {
        // Only refresh if we're on the documents screen
        if (currentRoute == com.example.policemobiledirectory.navigation.Routes.DOCUMENTS) {
            viewModel.fetchDocuments()
        }
    }

    val filteredDocs = remember(searchQuery, documents) {
        documents.filter { doc ->
            doc.isValid && (
                doc.resolvedTitle.contains(searchQuery, ignoreCase = true) ||
                doc.resolvedCategory?.contains(searchQuery, ignoreCase = true) == true
            )
        }
    }

    // Handle delete status
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.fetchDocuments(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 🔍 Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by title or category") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                val currentStatus = documentsStatus
                when (currentStatus) {
                    is OperationStatus.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    is OperationStatus.Error -> ErrorSection(
                        currentStatus.message,
                        onRetry = { viewModel.fetchDocuments(forceRefresh = true) }
                    )

                    is OperationStatus.Success -> {
                        if (filteredDocs.isEmpty()) {
                            EmptySection(message = "No documents found")
                        } else {

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredDocs) { doc ->
                                    DocumentItem(
                                        doc = doc,
                                        onViewClick = {
                                            val url = doc.resolvedUrl
                                            previewUrl = url

                                            // Smarter MIME detection
                                            previewMimeType = when {
                                                url?.contains(".pdf", ignoreCase = true) == true ||
                                                        url?.contains("drive.google.com", ignoreCase = true) == true -> "application/pdf"

                                                url?.contains(".jpg", ignoreCase = true) == true ||
                                                        url?.contains(".jpeg", ignoreCase = true) == true -> "image/jpeg"

                                                url?.contains(".png", ignoreCase = true) == true -> "image/png"

                                                url?.contains(".doc", ignoreCase = true) == true ||
                                                        url?.contains(".docx", ignoreCase = true) == true -> "application/msword"

                                                url?.contains(".xls", ignoreCase = true) == true ||
                                                        url?.contains(".xlsx", ignoreCase = true) == true -> "application/vnd.ms-excel"

                                                url?.contains(".ppt", ignoreCase = true) == true ||
                                                        url?.contains(".pptx", ignoreCase = true) == true -> "application/vnd.ms-powerpoint"

                                                else -> "application/octet-stream"
                                            }
                                        },

                                        onDownloadClick = { 
                                            scope.launch { 
                                                val url = doc.resolvedUrl
                                                val title = doc.resolvedTitle
                                                if (url != null) {
                                                    downloadFile(context, url, title)
                                                }
                                            } 
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is OperationStatus.Idle -> EmptySection(message = "No documents loaded")
                }
            }



            // 👀 Fullscreen preview dialog
            if (previewUrl != null && previewMimeType != null) {
                FullscreenPreviewDialog(
                    url = previewUrl!!,
                    mimeType = previewMimeType!!,
                    onDismiss = {
                        previewUrl = null
                        previewMimeType = null
                    }
                )
            }
        }
    }
}


@Composable
fun DocumentItem(
    doc: Document,
    onViewClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        doc.resolvedTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    fun Modifier.compactIcon() = size(36.dp)
                    IconButton(onClick = onViewClick, modifier = Modifier.compactIcon()) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDownloadClick, modifier = Modifier.compactIcon()) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            doc.resolvedCategory?.let {
                Text("Category: $it", style = MaterialTheme.typography.bodySmall)
            }
            doc.resolvedDescription?.let {
                Text("Description: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FullscreenPreviewDialog(
    url: String,
    mimeType: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Auto-hide loader after 10 seconds (fallback for stuck Google Docs viewer)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10_000)
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                // ✅ Top Bar
                CenterAlignedTopAppBar(
                    title = { Text("Preview") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { downloadFile(context, url, "Document") }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Download")
                        }
                    }
                )

                // ✅ Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        hasError -> Text(
                            text = "⚠️ Unable to preview this file.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // ✅ PDF, Drive & Office Docs
                        mimeType.contains("pdf", ignoreCase = true) ||
                                url.contains("drive.google.com", ignoreCase = true) ||
                                mimeType.contains("word", ignoreCase = true) ||
                                mimeType.contains("excel", ignoreCase = true) ||
                                mimeType.contains("powerpoint", ignoreCase = true) -> {

                            AndroidView(factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            errorCode: Int,
                                            description: String?,
                                            failingUrl: String?
                                        ) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }

                                    // ✅ Smarter PDF/Drive Viewer Handling
                                    val viewerUrl = when {
                                        url.contains("drive.google.com") ->
                                            url.replace("view?usp=sharing", "preview")
                                        url.endsWith(".pdf", true) ->
                                            url // Direct PDF links
                                        else ->
                                            "https://docs.google.com/gview?embedded=true&url=$url"
                                    }

                                    loadUrl(viewerUrl)
                                }
                            }, modifier = Modifier.fillMaxSize())
                        }

                        // ✅ Images
                        mimeType.startsWith("image/") -> {
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = "Preview Image",
                                modifier = Modifier.fillMaxSize()
                            )
                            isLoading = false
                        }

                        // ❌ Other file types
                        else -> {
                            Text(
                                text = "Preview not available for this file type.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            isLoading = false
                        }
                    }

                    // ✅ Loader overlay
                    if (isLoading && !hasError) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Loading preview...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun openDocument(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
    }
}

fun downloadFile(context: Context, url: String, title: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription("Downloading file...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading $title...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
    }
}



