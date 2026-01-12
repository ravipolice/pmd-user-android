package com.example.policemobiledirectory.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.policemobiledirectory.ui.screens.*
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch
import com.example.policemobiledirectory.ui.viewmodel.DocumentsViewModel
import com.example.policemobiledirectory.ui.screens.DocumentsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    employeeViewModel: EmployeeViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    // ✅ 1. Accept the Google Sign-In callback from MainActivity
    onGoogleSignInClicked: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBars = currentRoute in listOf(
        Routes.SPLASH,
        Routes.LOGIN,
        Routes.USER_REGISTRATION,
        Routes.FORGOT_PIN
    )

    // ✅ Global Navigation Drawer (only wraps if not hidden)
    if (!hideBars) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawer(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    viewModel = employeeViewModel
                )
            }
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        drawerState = drawerState, scope = scope // Removed drawerState and scope as they are not needed in the BottomBar
                    )
                }
            ) { innerPadding ->
                AppNavHostContent(
                    navController = navController,
                    employeeViewModel = employeeViewModel,
                    onThemeToggle = onThemeToggle,
                    // ✅ 2. Pass the callback down to the content host
                    onGoogleSignInClicked = onGoogleSignInClicked,
                    startDestination = startDestination,
                )
            }
        }
    } else {
        AppNavHostContent(
            navController = navController,
            employeeViewModel = employeeViewModel,
            onThemeToggle = onThemeToggle,
            // ✅ 3. Also pass it here for the initial composition
            onGoogleSignInClicked = onGoogleSignInClicked,
            startDestination = startDestination
        )
    }
}
@Composable
private fun AppNavHostContent(
    navController: NavHostController,
    employeeViewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    startDestination: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val isAdmin by employeeViewModel.isAdmin.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // --- SPLASH ---
        composable(Routes.SPLASH) {
            SplashVideoScreen(
                navController = navController,
                viewModel = employeeViewModel
            )
        }

        // --- LOGIN ---
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = employeeViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.EMPLOYEE_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterNewUser = { email ->
                    navController.navigate("${Routes.USER_REGISTRATION}?email=$email")
                },
                onForgotPinClicked = {
                    navController.navigate(Routes.FORGOT_PIN)
                },
                onGoogleSignInClicked = onGoogleSignInClicked,
                onThemeToggle = onThemeToggle
            )
        }

        // --- USER REGISTRATION ---
        composable(
            route = "${Routes.USER_REGISTRATION}?email={email}",
            arguments = listOf(
                androidx.navigation.navArgument("email") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            UserRegistrationScreen(
                navController = navController,
                viewModel = employeeViewModel,
                initialEmail = email
            )
        }

        // --- DOCUMENTS ---
        composable(Routes.DOCUMENTS) {
            val viewModel: DocumentsViewModel = hiltViewModel()
            DocumentsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }


        // --- FORGOT PIN ---
        composable(Routes.FORGOT_PIN) {
            ForgotPinScreen(
                viewModel = employeeViewModel,
                onPinResetSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.FORGOT_PIN) { inclusive = true }
                    }
                }
            )
        }



        // --- EMPLOYEE LIST (HOME) ---
        composable(Routes.EMPLOYEE_LIST) {
            EmployeeListScreen(
                navController = navController,
                viewModel = employeeViewModel,
                onThemeToggle = onThemeToggle
            )
        }

        // --- ABOUT ---
        composable(Routes.ABOUT) {
            AboutScreen(navController = navController)
        }

        // --- NUDI CONVERTER ---
        composable(Routes.NUDI_CONVERTER) {
            NudiConverterScreen(navController = navController)
        }

        // --- MY PROFILE ---
        composable(Routes.MY_PROFILE) {
            MyProfileEditScreen(navController = navController)
        }

        // --- NOTIFICATIONS ---
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(navController = navController, viewModel = employeeViewModel)
        }

        // --- Gallery Screen ---
        composable(Routes.GALLERY_SCREEN) {
            val galleryViewModel: com.example.policemobiledirectory.ui.viewmodel.GalleryViewModel = hiltViewModel()
            GalleryScreen(
                navController = navController,
                viewModel = galleryViewModel
            )
        }

        // --- Terms & Conditions ---
        composable(Routes.TERMS_AND_CONDITIONS) {
            TermsAndConditionsScreen(navController = navController)
        }

        // --- USEFUL LINKS ---
        composable(Routes.USEFUL_LINKS) {
            UsefulLinksScreen(navController = navController, viewModel = employeeViewModel)
        }

    }
}
