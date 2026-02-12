package com.example.policemobiledirectory

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.example.policemobiledirectory.utils.ToastUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.policemobiledirectory.navigation.AppNavGraph
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.services.MyFirebaseMessagingService
import com.example.policemobiledirectory.ui.theme.PMDTheme
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import androidx.activity.result.ActivityResultLauncher
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EmployeeViewModel by viewModels()
    private var wasLoggedOut = false

    // ✅ Permission launcher for notifications (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("Permission", "POST_NOTIFICATIONS granted = $isGranted")
    }

    // ✅ Legacy Google Sign-In Launcher (Fallback)
    private lateinit var googleSignInClient: GoogleSignInClient
    private val legacyGoogleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleLegacySignInResult(task)
        } else {
            Log.e("Auth", "❌ Legacy Sign-In cancelled or failed (code: ${result.resultCode})")
            ToastUtil.showToast(this, "Legacy Sign-In Cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // ✅ TEST LOG - This should ALWAYS appear when app starts
        Log.e("TEST_LOG", "═══════════════════════════════════════")
        Log.e("TEST_LOG", "🚀🚀🚀 MAINACTIVITY ONCREATE CALLED 🚀🚀🚀")
        Log.e("TEST_LOG", "═══════════════════════════════════════")
        android.util.Log.e("TEST_LOG2", "Android Log test - MainActivity started")
        System.out.println("SYSOUT: MainActivity onCreate called")

        // ✅ Initialize Legacy Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("603972083927-rog2v7ucndnu1399fugu3pemrjchov7t.apps.googleusercontent.com")
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 🚫 1️⃣ Clean up any leftover anonymous Firebase sessions
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null && (currentUser.isAnonymous || currentUser.email.isNullOrBlank())) {
            Log.w("StartupAuth", "⚠️ Clearing anonymous Firebase session on startup")
            auth.signOut()
        }

        // ✅ 2️⃣ Continue normal setup
        setupContent()
        askNotificationPermission()
        observeUserLoginForFCM()
    }

    /**
     * ✅ Launch Google Sign-In (only called if user selects Google login)
     */
    private suspend fun launchGoogleSignIn() {
        ToastUtil.showToast(this, "DEBUG: Starting Google Sign-In...")
        Log.d("Auth", "DEBUG: Starting Google Sign-In process")

        val credentialManager = CredentialManager.create(this)
        val googleIdOption = GetGoogleIdOption.Builder()
            // ✅ FIXED: Using correct Web Client ID from google-services.json for com.pmd.userapp
            .setServerClientId("603972083927-rog2v7ucndnu1399fugu3pemrjchov7t.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            Log.d("Auth", "DEBUG: Requesting credential...")
            ToastUtil.showToast(this, "DEBUG: Requesting Accounts...")

            // Reduced timeout to 2s to trigger fallback quickly
            val result: GetCredentialResponse = kotlinx.coroutines.withTimeout(2000L) {
                 Log.d("Auth", "DEBUG: calling credentialManager.getCredential")
                 credentialManager.getCredential(this@MainActivity, request)
            }
            Log.d("Auth", "DEBUG: credentialManager.getCredential returned")
            val credential = result.credential
            
            // Extract Google ID Token using library helper
            val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            val email = googleIdTokenCredential.id
            
            Log.d("Auth", "✅ Google Sign-In success for email: $email")
            ToastUtil.showToast(this, "DEBUG: Account selected: $email")
            Log.v("Auth", "Token: ${googleIdToken.take(10)}...")

            if (googleIdToken.isNotEmpty()) {
                viewModel.handleGoogleSignIn(email, googleIdToken)
                wasLoggedOut = false
            } else {
                Log.e("Auth", "❌ No ID token found in credential data")
                ToastUtil.showToast(this, "DEBUG: Error: No ID Token retrieved.")
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d("Auth", "⚠️ Sign-In cancelled by user")
            ToastUtil.showToast(this, "DEBUG: Sign-In Cancelled by User")
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Log.e("Auth", "❌ No credentials available: ${e.message}")
            ToastUtil.showToast(this, "DEBUG: No Google Accounts Found\n(Make sure you are logged into Google on this device)", Toast.LENGTH_LONG)
        } catch (e: androidx.credentials.exceptions.GetCredentialProviderConfigurationException) {
            Log.e("Auth", "❌ Provider configuration error (SHA-1/Package mismatch?): ${e.message}")
            ToastUtil.showToast(this, "DEBUG: CONFIG ERROR\n${e.message}\n(May be SHA-1 mismatch)", Toast.LENGTH_LONG)
        } catch (e: androidx.credentials.exceptions.GetCredentialUnknownException) {
            Log.e("Auth", "❌ Unknown credential error: ${e.message}")
            ToastUtil.showToast(this, "DEBUG: Unknown Auth Error: ${e.message}", Toast.LENGTH_LONG)
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            Log.e("Auth", "❌ Google Sign-In failed: ${e.type} - ${e.message}", e)
            ToastUtil.showToast(this, "DEBUG: Sign-In Error: ${e.type}\n${e.message}", Toast.LENGTH_LONG)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
             Log.e("Auth", "❌ Google Sign-In timed out (30s) - Triggering Legacy Fallback")
             ToastUtil.showToast(this, "Still waiting? Falling back to Legacy Sign-In...", Toast.LENGTH_LONG)
             launchLegacyGoogleSignIn()
        } catch (e: Exception) {
             Log.e("Auth", "❌ Google Sign-In unexpected error: ${e.javaClass.simpleName}", e)
             ToastUtil.showToast(this, "DEBUG: Unexpected Error: ${e.javaClass.simpleName}\n${e.localizedMessage}", Toast.LENGTH_LONG)
        } finally {
             Log.d("Auth", "DEBUG: Google Sign-In flow completed")
        }
    }

    private fun launchLegacyGoogleSignIn() {
        Log.d("Auth", "🚀 Launching Legacy Google Sign-In flow")
        val signInIntent = googleSignInClient.signInIntent
        legacyGoogleSignInLauncher.launch(signInIntent)
    }

    private fun handleLegacySignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            val email = account.email
            
            if (idToken != null && email != null) {
                Log.d("Auth", "✅ Legacy Google Sign-In success: $email")
                ToastUtil.showToast(this, "Legacy Success: $email")
                viewModel.handleGoogleSignIn(email, idToken)
                wasLoggedOut = false
            } else {
                Log.e("Auth", "❌ Legacy Sign-In: ID Token or Email is null")
                ToastUtil.showToast(this, "Legacy Error: Missing account info")
            }
        } catch (e: ApiException) {
            Log.e("Auth", "❌ Legacy Sign-In failed with code: ${e.statusCode}")
            ToastUtil.showToast(this, "Legacy Sign-In Error (Code: ${e.statusCode})")
        }
    }

    /**
     * ✅ Sets up Compose UI with automatic navigation based on login session.
     */
    private fun setupContent() {
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            PMDTheme(darkTheme = isDarkTheme) {

                // ✅ Always start with splash video
                val startDestination = Routes.SPLASH

                // ✅ Define logout action (manual only)
                val logoutAction: () -> Unit = {
                    scope.launch {
                        viewModel.logout {

                        ToastUtil.showToast(
                            this@MainActivity,
                            "Logged out successfully"
                        )

                            // ✅ Navigate to login only after clearing session
                            wasLoggedOut = true
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // ✅ Observe session changes for navigation
                LaunchedEffect(isLoggedIn, viewModel.currentUser.collectAsState().value) {
                    val currentUser = viewModel.currentUser.value
                    val currentRoute = navController.currentDestination?.route

                    // 1. Skip if still on Splash (Splash handles initial routing)
                    if (currentRoute == Routes.SPLASH) return@LaunchedEffect

                    // 2. Main Logic: Auto-Login Trigger
                    if (isLoggedIn && currentUser != null) {
                        // We only auto-navigate if:
                        // - We are on LOGIN screen (meaning a successful login just happened or was restored)
                        // - AND we haven't JUST clicked logout (wasLoggedOut check)
                        if (currentRoute == Routes.LOGIN && !wasLoggedOut) {
                            Log.d("MainActivity", "🏠 Session detected, auto-navigating to home")
                            navController.navigate(Routes.EMPLOYEE_LIST) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    } 
                    
                    // 3. Reset wasLoggedOut flag if we see a valid session start
                    if (isLoggedIn && currentUser != null) {
                        wasLoggedOut = false
                    }
                }

                // ✅ App Navigation
                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    employeeViewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onGoogleSignInClicked = { lifecycleScope.launch { launchGoogleSignIn() } },
                    onThemeToggle = { viewModel.toggleTheme() }
                )
            }
        }
    }

    /**
     * ✅ Ask permission for notifications (Android 13+)
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> Log.d("Permission", "Granted")

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * ✅ Sync FCM Token whenever a new user logs in
     */
    private fun observeUserLoginForFCM() {
        var previousUserUid: String? = viewModel.currentUser.value?.firebaseUid
        lifecycleScope.launch {
            viewModel.currentUser.collectLatest { employeeUser ->
                val currentUserUid = employeeUser?.firebaseUid
                if (currentUserUid != null && currentUserUid != previousUserUid) {
                    MyFirebaseMessagingService.syncPendingToken(this@MainActivity)
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                            return@addOnCompleteListener
                        }
                        val token = task.result
                        MyFirebaseMessagingService.sendRegistrationToServer(token)
                    }
                }
                previousUserUid = currentUserUid
            }
        }
    }
}
