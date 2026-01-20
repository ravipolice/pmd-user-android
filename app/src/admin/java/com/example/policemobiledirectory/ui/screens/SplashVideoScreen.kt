package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashVideoScreen(
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 second delay
        val target = if (viewModel.isLoggedIn.value) Routes.EMPLOYEE_LIST else Routes.LOGIN
        navController.navigate(target) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A3C)), // Dark navy blue background
        contentAlignment = Alignment.Center
    ) {
        // Display App Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(200.dp)
        )
    }
}

